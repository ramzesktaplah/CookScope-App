// CookScope Edge Function: usda-search
// Proxies USDA FoodData Central API calls so the USDA API key never ships in
// the Android APK. Accepts either a search query or an FDC ID for detail
// lookup. Requires a valid Supabase Auth session. Rate-limited server-side.

import {
  requireAuth,
  preflight,
  json,
  safeError,
} from "../_shared/auth.ts";
import { enforceRateLimit } from "../_shared/ratelimit.ts";
import { verifyIntegrity } from "../_shared/integrity.ts";

const BASE_URL = "https://api.nal.usda.gov/fdc/v1";

interface UsdaSearchRequest {
  query?: string;
  pageSize?: number;
  fdcId?: number;
}

Deno.serve(async (req) => {
  const pre = preflight(req);
  if (pre) return pre;

  try {
    const authUser = await requireAuth(req);
    await enforceRateLimit(authUser.userId, "usda_search");
    await verifyIntegrity(req, authUser.userId);

    const body: UsdaSearchRequest = await req.json().catch(() => ({}));

    const apiKey = Deno.env.get("USDA_API_KEY");
    if (!apiKey) {
      return json({ error: "Service unavailable. Please try again later." }, 500);
    }

    // If fdcId is provided, fetch the food detail.
    if (body.fdcId && typeof body.fdcId === "number") {
      const url = `${BASE_URL}/food/${body.fdcId}?api_key=${apiKey}`;
      const upstream = await fetch(url, {
        headers: { Accept: "application/json" },
      });

      if (!upstream.ok) {
        console.error("usda-search detail upstream status:", upstream.status);
        return json({ error: "Food data unavailable right now." }, 502);
      }

      const data = await upstream.json();
      return json(data);
    }

    // Otherwise, search by query.
    if (!body.query || !body.query.trim()) {
      return json({ error: "Provide a query or fdcId." }, 400);
    }

    const pageSize = Math.max(1, Math.min(50, body.pageSize ?? 5));
    const searchBody = {
      query: body.query.trim(),
      pageSize,
      sortBy: "dataType",
      sortOrder: "asc",
    };

    const url = `${BASE_URL}/foods/search?api_key=${apiKey}`;
    const upstream = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(searchBody),
    });

    if (!upstream.ok) {
      console.error("usda-search upstream status:", upstream.status);
      return json({ error: "Food search is unavailable right now." }, 502);
    }

    const data = await upstream.json();
    return json(data);
  } catch (err) {
    return safeError(err);
  }
});
