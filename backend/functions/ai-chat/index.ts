// CookScope Edge Function: ai-chat
// Proxies chat completions to the Rork AI Gateway (toolkit.rork.com) so the
// Rork toolkit secret never reaches the Android client. Requires a valid
// Supabase Auth session. Supports both text and multimodal (image_url) messages.

import {
  requireAuth,
  preflight,
  json,
  safeError,
  corsHeaders,
} from "../_shared/auth.ts";
import { enforceRateLimit } from "../_shared/ratelimit.ts";

const TOOLKIT_URL = "https://toolkit.rork.com/v2/vercel/v1/chat/completions";

interface ChatMessage {
  role: string;
  content: string | unknown[];
}

interface ChatRequest {
  messages: ChatMessage[];
  model?: string;
  temperature?: number;
  maxTokens?: number;
  thinking?: boolean;
  reasoningEffort?: string;
}

Deno.serve(async (req) => {
  const pre = preflight(req);
  if (pre) return pre;

  try {
    const authUser = await requireAuth(req);
    await enforceRateLimit(authUser.userId, "ai_chat");

    const body = await req.json() as ChatRequest;
    if (!Array.isArray(body.messages) || body.messages.length === 0) {
      return json({ error: "Messages are required." }, 400);
    }

    const secretKey = Deno.env.get("RORK_TOOLKIT_SECRET_KEY");
    if (!secretKey) {
      return json({ error: "Service unavailable. Please try again later." }, 500);
    }

    const payload = {
      model: body.model ?? "openai/gpt-4o-mini",
      messages: body.messages,
      temperature: body.temperature ?? 0.7,
      top_p: 0.95,
      max_tokens: body.maxTokens ?? 4096,
      stream: false,
      ...(body.thinking
        ? {
            reasoning: {
              effort: body.reasoningEffort ?? "medium",
              include: true,
            },
          }
        : {}),
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
      console.error("ai-chat upstream status:", upstream.status);
      return json({ error: "AI service is busy. Please try again." }, 502);
    }

    const data = await upstream.json();
    const message = data?.choices?.[0]?.message ?? {};
    const content = message.content ?? "";
    const reasoning = message.reasoning ?? message.reasoning_content ?? null;

    return json({ content, reasoning });
  } catch (err) {
    return safeError(err);
  }
});
