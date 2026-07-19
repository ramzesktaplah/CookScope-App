// CookScope Edge Function: recipe-detail
// Fetches a single recipe by id from recipeapi.io. Requires a valid Supabase
// Auth session.

import {
  requireAuth,
  preflight,
  json,
  safeError,
} from "../_shared/auth.ts";

const BASE_URL = "https://recipeapi.io/api/v1";

Deno.serve(async (req) => {
  const pre = preflight(req);
  if (pre) return pre;

  try {
    await requireAuth(req);

    let id: string | undefined;
    if (req.method === "POST") {
      const body = await req.json().catch(() => ({}));
      id = body.id;
    } else if (req.method === "GET") {
      id = new URL(req.url).searchParams.get("id") ?? undefined;
    }

    if (!id) {
      return json({ error: "Recipe id is required." }, 400);
    }

    const apiKey = Deno.env.get("RECIPEAPI_KEY");
    if (!apiKey) {
      return json({ error: "Service unavailable. Please try again later." }, 500);
    }

    const url = `${BASE_URL}/recipes/${encodeURIComponent(id)}?lang=en`;
    const upstream = await fetch(url, {
      headers: {
        Authorization: `Bearer ${apiKey}`,
        Accept: "application/json",
      },
    });

    if (!upstream.ok) {
      console.error("recipe-detail upstream status:", upstream.status);
      return json({ error: "Recipe could not be loaded." }, 502);
    }

    const data = await upstream.json();
    return json(data);
  } catch (err) {
    return safeError(err);
  }
});
