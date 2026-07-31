// CookScope Edge Function: recipe-random
// Returns up to `count` random recipes from recipeapi.io (the API returns one
// per call, so we fan out). Requires a valid Supabase Auth session.

import {
  requireAuth,
  preflight,
  json,
  safeError,
} from "../_shared/auth.ts";
import { enforceRateLimit } from "../_shared/ratelimit.ts";
import { verifyIntegrity } from "../_shared/integrity.ts";

const BASE_URL = "https://recipeapi.io/api/v1";

Deno.serve(async (req) => {
  const pre = preflight(req);
  if (pre) return pre;

  try {
    const authUser = await requireAuth(req);
    await enforceRateLimit(authUser.userId, "recipe_random");
    await verifyIntegrity(req, authUser.userId);

    let count = 6;
    if (req.method === "POST") {
      const body = await req.json().catch(() => ({}));
      if (typeof body.count === "number") count = Math.max(1, Math.min(10, body.count));
    } else if (req.method === "GET") {
      const n = Number(new URL(req.url).searchParams.get("count") ?? "6");
      if (!Number.isNaN(n)) count = Math.max(1, Math.min(10, n));
    }

    const apiKey = Deno.env.get("RECIPEAPI_KEY");
    if (!apiKey) {
      return json({ error: "Service unavailable. Please try again later." }, 500);
    }

    const recipes: unknown[] = [];
    await Promise.all(
      Array.from({ length: count }, async () => {
        try {
          const res = await fetch(`${BASE_URL}/recipes/random?lang=en`, {
            headers: {
              Authorization: `Bearer ${apiKey}`,
              Accept: "application/json",
            },
          });
          if (!res.ok) return;
          const jsonBody = await res.json();
          if (jsonBody?.data) recipes.push(jsonBody.data);
        } catch {
          // ignore individual failures
        }
      }),
    );

    if (recipes.length === 0) {
      return json({ error: "No recipes returned. Please try again." }, 502);
    }

    return json({ data: recipes });
  } catch (err) {
    return safeError(err);
  }
});
