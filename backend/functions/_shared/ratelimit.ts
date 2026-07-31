// Server-side per-user rate limiting for CookScope Edge Functions.
//
// Enforces the same limits as the Android client-side usage managers but
// server-side, so they cannot be bypassed by calling the edge functions
// directly. Uses the service-role Supabase client to read/increment the
// user_rate_limits table (which has RLS locked to service-role only).

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

/** Error thrown when a user exceeds their rate limit. */
export class RateLimitError extends Error {
  retryAfterSeconds: number;
  limit: number;
  action: string;

  constructor(retryAfterSeconds: number, limit: number, action: string) {
    super("Rate limit exceeded");
    this.name = "RateLimitError";
    this.retryAfterSeconds = retryAfterSeconds;
    this.limit = limit;
    this.action = action;
  }
}

interface LimitSpec {
  free: number;
  premium: number;
  windowSeconds: number;
}

/** Per-action rate limit configuration. Matches the Android client spec. */
const LIMITS: Record<string, LimitSpec> = {
  ai_chat: { free: 5, premium: 50, windowSeconds: 3.5 * 60 * 60 },
  ai_scan: { free: 10, premium: 50, windowSeconds: 5 * 60 * 60 },
  recipe_search: { free: 20, premium: 52, windowSeconds: 24 * 60 * 60 },
  recipe_random: { free: 20, premium: 52, windowSeconds: 24 * 60 * 60 },
  recipe_detail: { free: 20, premium: 52, windowSeconds: 24 * 60 * 60 },
  trending_fetch: { free: 20, premium: 52, windowSeconds: 24 * 60 * 60 },
};

/**
 * Create a service-role Supabase client for internal operations.
 */
function serviceClient() {
  return createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );
}

/**
 * Look up the user's plan_type from profiles to determine their limit.
 * Returns "free" if not found or on error (fail-open to free tier).
 */
async function getPlanType(userId: string): Promise<string> {
  try {
    const supabase = serviceClient();
    const { data, error } = await supabase
      .from("profiles")
      .select("plan_type")
      .eq("id", userId)
      .single();
    if (error || !data) return "free";
    return data.plan_type ?? "free";
  } catch {
    return "free";
  }
}

/**
 * Enforce the rate limit for a given action. Must be called after
 * requireAuth() so userId is verified.
 *
 * Throws RateLimitError if the user has exceeded their limit.
 * Otherwise increments the count and returns silently.
 */
export async function enforceRateLimit(
  userId: string,
  action: string,
): Promise<void> {
  const spec = LIMITS[action];
  if (!spec) {
    // Unknown action — allow it (shouldn't happen in practice).
    return;
  }

  const planType = await getPlanType(userId);
  const limit = planType === "premium" ? spec.premium : spec.free;
  const now = new Date();

  const supabase = serviceClient();

  // Try to fetch the existing row.
  const { data, error } = await supabase
    .from("user_rate_limits")
    .select("id, count, window_start")
    .eq("user_id", userId)
    .eq("action", action)
    .single();

  if (error || !data) {
    // Row doesn't exist — create it with count=1.
    await supabase.from("user_rate_limits").insert({
      user_id: userId,
      action,
      count: 1,
      window_start: now.toISOString(),
    });
    return;
  }

  const windowStart = new Date(data.window_start);
  const elapsedSeconds = (now.getTime() - windowStart.getTime()) / 1000;

  if (elapsedSeconds >= spec.windowSeconds) {
    // Window expired — reset count and window_start.
    await supabase
      .from("user_rate_limits")
      .update({ count: 1, window_start: now.toISOString() })
      .eq("id", data.id);
    return;
  }

  if (data.count >= limit) {
    // Limit exceeded — compute seconds until window resets.
    const retryAfterSeconds = Math.ceil(
      spec.windowSeconds - elapsedSeconds,
    );
    throw new RateLimitError(retryAfterSeconds, limit, action);
  }

  // Increment count within current window.
  await supabase
    .from("user_rate_limits")
    .update({ count: data.count + 1 })
    .eq("id", data.id);
}
