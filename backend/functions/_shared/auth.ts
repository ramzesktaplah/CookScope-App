// Shared auth helper for CookScope Edge Functions.
// Native Supabase Auth mode — verifies the caller's JWT via supabase.auth.getUser().

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

export class AuthError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "AuthError";
  }
}

export interface AuthUser {
  userId: string;
  email?: string;
}

/**
 * Require a valid Supabase Auth session on the request. Throws AuthError
 * if the Authorization header is missing or the JWT does not resolve to a user.
 */
export async function requireAuth(req: Request): Promise<AuthUser> {
  const authHeader = req.headers.get("Authorization") ?? "";
  if (!authHeader.toLowerCase().startsWith("bearer ")) {
    throw new AuthError("Missing token");
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } },
  );

  const {
    data: { user },
    error,
  } = await supabase.auth.getUser();

  if (error || !user) {
    throw new AuthError("Unauthorized");
  }

  return {
    userId: user.id,
    email: user.email,
  };
}

/** CORS headers shared by every function. */
export const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
};

/** Build a JSON response with CORS headers. */
export function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

/** Handle OPTIONS preflight for every function. */
export function preflight(req: Request): Response | null {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  return null;
}

/** Map any upstream error to a safe user-facing message. Never leaks URLs/keys. */
export function safeError(err: unknown): Response {
  if (err instanceof AuthError) {
    return json({ error: "Unauthorized" }, 401);
  }
  // RateLimitError — return 429 with retry-after seconds.
  if (err && typeof err === "object" && err.name === "RateLimitError") {
    const rle = err as { retryAfterSeconds?: number; limit?: number };
    return json({
      error: "Rate limit reached. Please try again later.",
      retryAfterSeconds: rle.retryAfterSeconds ?? 0,
    }, 429);
  }
  console.error("edge function error:", err);
  return json({ error: "Service unavailable. Please try again later." }, 500);
}
