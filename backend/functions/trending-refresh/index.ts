// CookScope Edge Function: trending-refresh
// Called daily by a pg_cron job (via pg_net http_post). Validates the
// Supabase publishable key from the `apikey` header, fetches 7 random
// recipes from recipeapi.io, and upserts them into the trending_recipes
// table with today's date. No user auth — this is a server-to-server cron
// trigger that uses the project's publishable key for authentication.

import {
  preflight,
  json,
  safeError,
  corsHeaders,
} from "../_shared/auth.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const BASE_URL = "https://recipeapi.io/api/v1";
const RECIPE_COUNT = 20;

Deno.serve(async (req) => {
  const pre = preflight(req);
  if (pre) return pre;

  try {
    // Validate the Supabase publishable key sent by the cron job.
    // The cron job stores this in vault and sends it as the `apikey`
    // header — the standard Supabase server-to-service pattern.
    const expectedKey = Deno.env.get("SUPABASE_ANON_KEY");
    if (!expectedKey) {
      console.error("trending-refresh: SUPABASE_ANON_KEY not configured");
      return json({ error: "Service unavailable." }, 500);
    }
    const providedKey = req.headers.get("apikey") ?? "";
    if (providedKey !== expectedKey) {
      return json({ error: "Unauthorized." }, 401);
    }

    const apiKey = Deno.env.get("RECIPEAPI_KEY");
    if (!apiKey) {
      console.error("trending-refresh: RECIPEAPI_KEY not configured");
      return json({ error: "Service unavailable." }, 500);
    }

    // Fetch 7 random recipes (recipeapi.io returns one per /random call).
    const recipes: Record<string, unknown>[] = [];
    await Promise.all(
      Array.from({ length: RECIPE_COUNT }, async () => {
        try {
          const res = await fetch(`${BASE_URL}/recipes/random?lang=en`, {
            headers: {
              Authorization: `Bearer ${apiKey}`,
              Accept: "application/json",
            },
          });
          if (!res.ok) return;
          const body = await res.json();
          if (body?.data) recipes.push(body.data as Record<string, unknown>);
        } catch {
          // ignore individual failures
        }
      }),
    );

    if (recipes.length === 0) {
      return json({ error: "No recipes returned from upstream." }, 502);
    }

    // Upsert into trending_recipes using the service role key (bypasses RLS).
    const supabaseAdmin = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
    );

    const today = new Date().toISOString().slice(0, 10); // YYYY-MM-DD

    const rows = recipes.map((r) => ({
      recipe_id: String(r.id ?? ""),
      title: String(r.name ?? r.title ?? "Untitled"),
      image_url: (r.image_url as string | null) ?? null,
      cuisine: (r.cuisine as string | null) ?? null,
      difficulty: (r.difficulty as string | null) ?? null,
      cook_time_min: (r.cook_time as number | null) ??
        ((r.cook_time as number | undefined) ?? 0),
      recipe_data: r,
      for_date: today,
    }));

    const { error } = await supabaseAdmin
      .from("trending_recipes")
      .upsert(rows, { onConflict: "recipe_id,for_date" });

    if (error) {
      console.error("trending-refresh upsert error:", error.message);
      return json({ error: "Failed to cache trending recipes." }, 500);
    }

    console.log(`trending-refresh: inserted ${rows.length} recipes for ${today}`);
    return json({ inserted: rows.length });
  } catch (err) {
    return safeError(err);
  }
});
