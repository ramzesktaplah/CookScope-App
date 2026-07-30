// CookScope Edge Function: notify-user
// Sends Firebase Cloud Messaging (FCM) push notifications to users who have
// opted into the given notification category.
//
// This function is designed to be called by:
//  1. The daily trending-refresh cron job (for Popular Recipe pushes).
//  2. Scheduled pg_cron jobs for mealtime and weekly notifications.
//  3. Future event-based triggers (post-scan, post-save, etc.).
//
// Authentication: server-to-server via the `apikey` header (Supabase anon
// key), matching the trending-refresh pattern. The service role key is used
// internally to read all user preferences and FCM tokens (bypassing RLS).
//
// Request body:
//   {
//     "category": "popular_recipe" | "mealtime" | "scan_ai" | ...,
//     "title": "string",
//     "body": "string",
//     "deepLink": "string" (optional)
//   }
//
// Firebase setup required before this function can deliver pushes:
//   1. Create a Firebase project and add an Android app.
//   2. Download google-services.json and place it in the android/app folder.
//   3. Set the FIREBASE_SERVER_KEY env var (Project Settings → Cloud Messaging
//      → Server Key) OR a service account JSON in FIREBASE_SERVICE_ACCOUNT.
//   4. The Android app must register an FCM token and store it in the
//      profiles.fcm_token column (see NotificationPreferencesManager).
//
// Until Firebase is configured, this function returns a 503 with a clear
// message so the cron jobs don't fail silently.

import {
  preflight,
  json,
  safeError,
} from "../_shared/auth.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

interface NotifyRequest {
  category: string;
  title: string;
  body: string;
  deepLink?: string;
}

Deno.serve(async (req) => {
  const pre = preflight(req);
  if (pre) return pre;

  try {
    // Validate the Supabase publishable key (server-to-server auth).
    const expectedKey = Deno.env.get("SUPABASE_ANON_KEY");
    if (!expectedKey) {
      console.error("notify-user: SUPABASE_ANON_KEY not configured");
      return json({ error: "Service unavailable." }, 500);
    }
    const providedKey = req.headers.get("apikey") ?? "";
    if (providedKey !== expectedKey) {
      return json({ error: "Unauthorized." }, 401);
    }

    const payload: NotifyRequest = await req.json();
    if (!payload.category || !payload.title || !payload.body) {
      return json({ error: "Missing category, title, or body." }, 400);
    }

    // Map the category to the preferences column name.
    const categoryColumn = mapCategoryToColumn(payload.category);
    if (!categoryColumn) {
      return json({ error: `Unknown category: ${payload.category}` }, 400);
    }

    // Use the service role to bypass RLS and read all opted-in users.
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (!serviceKey) {
      console.error("notify-user: SUPABASE_SERVICE_ROLE_KEY not configured");
      return json({ error: "Service unavailable." }, 500);
    }

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      serviceKey,
    );

    // Fetch users who have this category enabled AND have an FCM token.
    const { data: prefs, error: prefsError } = await supabase
      .from("user_notification_preferences")
      .select(`user_id, ${categoryColumn}`)
      .eq(categoryColumn, true);

    if (prefsError) {
      console.error("notify-user: failed to fetch preferences:", prefsError);
      return json({ error: "Service unavailable." }, 500);
    }

    if (!prefs || prefs.length === 0) {
      return json({ sent: 0, message: "No users opted into this category." });
    }

    const userIds = prefs.map((p: { user_id: string }) => p.user_id);

    // Fetch FCM tokens for those users from the profiles table.
    const { data: profiles, error: profilesError } = await supabase
      .from("profiles")
      .select("id, fcm_token")
      .in("id", userIds)
      .not("fcm_token", "is", null);

    if (profilesError) {
      console.error("notify-user: failed to fetch profiles:", profilesError);
      return json({ error: "Service unavailable." }, 500);
    }

    const tokens = (profiles ?? [])
      .filter((p: { fcm_token: string | null }) => p.fcm_token)
      .map((p: { fcm_token: string | null }) => p.fcm_token as string);

    if (tokens.length === 0) {
      return json({ sent: 0, message: "No FCM tokens registered." });
    }

    // Send via Firebase Cloud Messaging.
    const serverKey = Deno.env.get("FIREBASE_SERVER_KEY");
    if (!serverKey) {
      console.error("notify-user: FIREBASE_SERVER_KEY not configured");
      return json({
        sent: 0,
        message: "Firebase not configured. Set FIREBASE_SERVER_KEY env var.",
      }, 503);
    }

    const fcmResponse = await fetch("https://fcm.googleapis.com/fcm/send", {
      method: "POST",
      headers: {
        "Authorization": `key=${serverKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        registration_ids: tokens,
        notification: {
          title: payload.title,
          body: payload.body,
          sound: "default",
        },
        data: {
          category: payload.category,
          deepLink: payload.deepLink ?? "",
        },
      }),
    });

    if (!fcmResponse.ok) {
      const errText = await fcmResponse.text();
      console.error("notify-user: FCM send failed:", errText);
      return json({ error: "Push delivery failed." }, 502);
    }

    const result = await fcmResponse.json();
    return json({
      sent: tokens.length,
      success: result.success ?? 0,
      failure: result.failure ?? 0,
    });
  } catch (err) {
    return safeError(err);
  }
});

/** Map a notification category to the preferences table column name. */
function mapCategoryToColumn(category: string): string | null {
  const map: Record<string, string> = {
    popular_recipe: "popular_recipe",
    mealtime: "mealtime",
    scan_ai: "scan_ai",
    saved_recipes: "saved_recipes",
    engagement: "engagement",
    achievements: "achievements",
    trending: "trending",
    premium: "premium",
    rate_limit_reset: "rate_limit_reset",
  };
  return map[category] ?? null;
}
