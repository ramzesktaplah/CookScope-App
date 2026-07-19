// CookScope Edge Function: recipe-search
// Searches recipeapi.io by query string or by ingredient list. Requires a
// valid Supabase Auth session. The recipeapi.io key lives only in the backend.

import {
  requireAuth,
  preflight,
  json,
  safeError,
} from "../_shared/auth.ts";

const BASE_URL = "https://recipeapi.io/api/v1";

interface SearchRequest {
  query?: string;
  ingredients?: string[];
  page?: number;
  perPage?: number;
}

Deno.serve(async (req) => {
  const pre = preflight(req);
  if (pre) return pre;

  try {
    await requireAuth(req);

    let body: SearchRequest = {};
    if (req.method === "POST") {
      body = await req.json().catch(() => ({}));
    } else if (req.method === "GET") {
      const url = new URL(req.url);
      body = {
        query: url.searchParams.get("query") ?? undefined,
        ingredients: url.searchParams.get("ingredients")?.split(",").filter(Boolean) ?? undefined,
        page: Number(url.searchParams.get("page") ?? "1"),
        perPage: Number(url.searchParams.get("perPage") ?? "20"),
      };
    }

    const apiKey = Deno.env.get("RECIPEAPI_KEY");
    if (!apiKey) {
      return json({ error: "Service unavailable. Please try again later." }, 500);
    }

    const page = body.page ?? 1;
    const perPage = body.perPage ?? 20;
    let url: string;

    if (body.ingredients && body.ingredients.length > 0) {
      const ingredientsParam = body.ingredients
        .map((i) => encodeURIComponent(i.trim()))
        .join(",");
      url = `${BASE_URL}/recipes?ingredients=${ingredientsParam}&lang=en&page=${page}&per_page=${perPage}`;
    } else if (body.query && body.query.trim()) {
      const q = encodeURIComponent(body.query.trim());
      url = `${BASE_URL}/recipes?search=${q}&lang=en&page=${page}&per_page=${perPage}`;
    } else {
      return json({ error: "Provide a query or ingredients." }, 400);
    }

    const upstream = await fetch(url, {
      headers: {
        Authorization: `Bearer ${apiKey}`,
        Accept: "application/json",
      },
    });

    if (!upstream.ok) {
      console.error("recipe-search upstream status:", upstream.status);
      return json({ error: "Recipe search is unavailable right now." }, 502);
    }

    const data = await upstream.json();
    return json(data);
  } catch (err) {
    return safeError(err);
  }
});
