-- Adds per-user notification category preferences and an FCM token column
-- on profiles for server-side push delivery.
--
-- The user_notification_preferences table stores opt-in/opt-out flags for
-- each notification category. Defaults are all true (opt-in) so users who
-- grant the system permission receive the full catalog until they toggle
-- categories off in Settings.
--
-- The fcm_token column on profiles is populated by the Android app after
-- the user grants POST_NOTIFICATIONS. The notify-user edge function reads
-- it to send Firebase Cloud Messaging pushes.

-- 1. FCM token column on profiles (nullable — empty until the app registers).
alter table public.profiles
    add column if not exists fcm_token text;

-- 2. Notification preferences table.
create table if not exists public.user_notification_preferences (
    id bigint generated always as identity primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    -- Category toggles (all default true = opt-in).
    popular_recipe boolean not null default true,
    mealtime boolean not null default true,
    scan_ai boolean not null default true,
    saved_recipes boolean not null default true,
    engagement boolean not null default true,
    achievements boolean not null default true,
    trending boolean not null default true,
    premium boolean not null default true,
    rate_limit_reset boolean not null default true,
    -- Timestamps.
    created_at timestamp with time zone default now(),
    updated_at timestamp with time zone default now(),
    unique (user_id)
);

-- Enable RLS.
alter table public.user_notification_preferences enable row level security;

-- Users can read and update their own preferences.
create policy "Allow authenticated read own notification prefs"
    on public.user_notification_preferences
    for select
    to authenticated
    using (user_id = auth.uid());

create policy "Allow authenticated upsert own notification prefs"
    on public.user_notification_preferences
    for insert
    to authenticated
    with check (user_id = auth.uid());

create policy "Allow authenticated update own notification prefs"
    on public.user_notification_preferences
    for update
    to authenticated
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

create policy "Allow authenticated delete own notification prefs"
    on public.user_notification_preferences
    for delete
    to authenticated
    using (user_id = auth.uid());

-- Allow the service role (used by notify-user edge function) to read all
-- preference rows so it can filter recipients before sending pushes.
-- The service role bypasses RLS by default, so no extra policy is needed.

-- Allow users to update their own fcm_token on profiles.
create policy "Allow authenticated update own fcm_token"
    on public.profiles
    for update
    to authenticated
    using (id = auth.uid())
    with check (id = auth.uid());

-- Index for quick lookup by user.
create index if not exists idx_notification_prefs_user_id
    on public.user_notification_preferences (user_id);
