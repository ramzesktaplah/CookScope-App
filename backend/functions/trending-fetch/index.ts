// CookScope Edge Function: trending-fetch
// Returns today's trending recipes from the trending_recipes table.
// Auth-protected — requires a valid Supabase Auth session.

import {
  requireAuth,
  preflight,
  json,
  safeError,
} from "../_shared/auth.ts";
import { enforceRateLimit } from "../_shared/ratelimit.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (req) => {
  const pre = preflight(req);
  if (pre) return pre;

  try {
    const authUser = await requireAuth(req);
    await enforceRateLimit(authUser.userId, "trending_fetch");

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_ANON_KEY")!,
      { global: { headers: { Authorization: req.headers.get("Authorization")! } } },
    );

    const today = new Date().toISOString().slice(0, 10); // YYYY-MM-DD

    const { data, error } = await supabase
      .from("trending_recipes")
      .select("recipe_id,title,image_url,cuisine,difficulty,cook_time_min,recipe_data")
      .eq("for_date", today)
      .order("created_at", { ascending: true });

    if (error) {
      console.error("trending-fetch query error:", error.message);
      return json({ error: "Failed to fetch trending recipes." }, 500);
    }

    return json({ data: data ?? [] });
  } catch (err) {
    return safeError(err);
  }
});
