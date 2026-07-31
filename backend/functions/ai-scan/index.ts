// CookScope Edge Function: ai-scan
// Multimodal vision proxy for dish photo analysis. Accepts a base64 JPEG and
// returns the raw model content (caller parses JSON). Requires a valid
// Supabase Auth session.

import {
  requireAuth,
  preflight,
  json,
  safeError,
} from "../_shared/auth.ts";
import { enforceRateLimit } from "../_shared/ratelimit.ts";
import { verifyIntegrity } from "../_shared/integrity.ts";

const TOOLKIT_URL = "https://toolkit.rork.com/v2/vercel/v1/chat/completions";

interface ScanRequest {
  messages: Array<Record<string, unknown>>;
  model?: string;
  temperature?: number;
  maxTokens?: number;
}

Deno.serve(async (req) => {
  const pre = preflight(req);
  if (pre) return pre;

  try {
    const authUser = await requireAuth(req);
    await enforceRateLimit(authUser.userId, "ai_scan");
    await verifyIntegrity(req, authUser.userId);

    const body = await req.json() as ScanRequest;
    if (!Array.isArray(body.messages) || body.messages.length === 0) {
      return json({ error: "Messages are required." }, 400);
    }

    const secretKey = Deno.env.get("RORK_TOOLKIT_SECRET_KEY");
    if (!secretKey) {
      return json({ error: "Service unavailable. Please try again later." }, 500);
    }

    const payload = {
      model: body.model ?? "deepseek/deepseek-v4-flash",
      messages: body.messages,
      temperature: body.temperature ?? 0.3,
      top_p: 0.95,
      max_tokens: body.maxTokens ?? 4096,
      stream: false,
    };

    const upstream = await fetch(TOOLKIT_URL, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${secretKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (!upstream.ok) {
      console.error("ai-scan upstream status:", upstream.status);
      return json({ error: "Analysis failed. Please try again." }, 502);
    }

    const data = await upstream.json();
    const content = data?.choices?.[0]?.message?.content ?? "";

    return json({ content });
  } catch (err) {
    return safeError(err);
  }
});
