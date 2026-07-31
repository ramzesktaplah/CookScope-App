-- CookScope migration: secure authentication & authorization (M3 remediation)
--
-- 1. Enable RLS on profiles, saved_recipes, user_preferences (created via
--    dashboard, had NO RLS policies — any authenticated user could read/modify
--    every other user's data).
-- 2. Drop chat_messages table (no RLS, superseded by chat_conversations,
--    only referenced by dead code with userId="").
-- 3. Protect plan_type from privilege escalation — a trigger rejects any
--    UPDATE to plan_type unless the request uses the service role (which
--    bypasses RLS and triggers).
-- 4. Auto-update updated_at timestamps on all three base tables.
-- 5. Create user_rate_limits table for server-side per-user rate limiting
--    on edge functions.

-- ============================================================
-- 1a. RLS on profiles
-- ============================================================
alter table public.profiles enable row level security;

drop policy if exists "profiles_select_own" on public.profiles;
create policy "profiles_select_own"
    on public.profiles
    for select
    to authenticated
    using (id = auth.uid());

drop policy if exists "profiles_insert_own" on public.profiles;
create policy "profiles_insert_own"
    on public.profiles
    for insert
    to authenticated
    with check (id = auth.uid());

drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own"
    on public.profiles
    for update
    to authenticated
    using (id = auth.uid())
    with check (id = auth.uid());

drop policy if exists "profiles_delete_own" on public.profiles;
create policy "profiles_delete_own"
    on public.profiles
    for delete
    to authenticated
    using (id = auth.uid());

-- ============================================================
-- 1b. RLS on saved_recipes
-- ============================================================
alter table public.saved_recipes enable row level security;

drop policy if exists "saved_recipes_select_own" on public.saved_recipes;
create policy "saved_recipes_select_own"
    on public.saved_recipes
    for select
    to authenticated
    using (user_id = auth.uid());

drop policy if exists "saved_recipes_insert_own" on public.saved_recipes;
create policy "saved_recipes_insert_own"
    on public.saved_recipes
    for insert
    to authenticated
    with check (user_id = auth.uid());

drop policy if exists "saved_recipes_update_own" on public.saved_recipes;
create policy "saved_recipes_update_own"
    on public.saved_recipes
    for update
    to authenticated
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

drop policy if exists "saved_recipes_delete_own" on public.saved_recipes;
create policy "saved_recipes_delete_own"
    on public.saved_recipes
    for delete
    to authenticated
    using (user_id = auth.uid());

-- ============================================================
-- 1c. RLS on user_preferences
-- ============================================================
alter table public.user_preferences enable row level security;

drop policy if exists "user_preferences_select_own" on public.user_preferences;
create policy "user_preferences_select_own"
    on public.user_preferences
    for select
    to authenticated
    using (user_id = auth.uid());

drop policy if exists "user_preferences_insert_own" on public.user_preferences;
create policy "user_preferences_insert_own"
    on public.user_preferences
    for insert
    to authenticated
    with check (user_id = auth.uid());

drop policy if exists "user_preferences_update_own" on public.user_preferences;
create policy "user_preferences_update_own"
    on public.user_preferences
    for update
    to authenticated
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

drop policy if exists "user_preferences_delete_own" on public.user_preferences;
create policy "user_preferences_delete_own"
    on public.user_preferences
    for delete
    to authenticated
    using (user_id = auth.uid());

-- ============================================================
-- 2. Drop chat_messages table
-- ============================================================
drop table if exists public.chat_messages cascade;

-- ============================================================
-- 3. Protect plan_type from privilege escalation
--
-- A BEFORE UPDATE trigger that raises an exception if the caller is trying
-- to change plan_type and is NOT using the service role. The service role
-- bypasses RLS and triggers entirely, so RevenueCat webhooks / server-side
-- code can still update plan_type freely. Authenticated users with the anon
-- key are blocked from self-escalating to "premium".
-- ============================================================
create or replace function public.protect_plan_type()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    -- Only block when plan_type is actually being changed.
    if new.plan_type is distinct from old.plan_type then
        -- The service role sets a custom claim "role": "service_role".
        -- Authenticated users have "role": "authenticated" or "anon".
        -- If the request JWT's role is not service_role, reject the change.
        if coalesce(current_setting('request.jwt.claim.role', true), '') != 'service_role' then
            raise exception 'Not authorized to change plan_type'
                using errcode = '42501';
        end if;
    end if;
    return new;
end;
$$;

drop trigger if exists trg_protect_plan_type on public.profiles;
create trigger trg_protect_plan_type
    before update on public.profiles
    for each row
    execute function public.protect_plan_type();

-- ============================================================
-- 4. Auto-update updated_at triggers
-- ============================================================
create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists trg_profiles_updated_at on public.profiles;
create trigger trg_profiles_updated_at
    before update on public.profiles
    for each row
    execute function public.touch_updated_at();

drop trigger if exists trg_saved_recipes_updated_at on public.saved_recipes;
create trigger trg_saved_recipes_updated_at
    before update on public.saved_recipes
    for each row
    execute function public.touch_updated_at();

drop trigger if exists trg_user_preferences_updated_at on public.user_preferences;
create trigger trg_user_preferences_updated_at
    before update on public.user_preferences
    for each row
    execute function public.touch_updated_at();

-- ============================================================
-- 5. user_rate_limits table for server-side rate limiting
--
-- Service-role only — users cannot read or modify their own limits.
-- Edge functions check/increment this via the service-role client.
-- ============================================================
create table if not exists public.user_rate_limits (
    id bigint generated always as identity primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    action text not null,
    count int not null default 0,
    window_start timestamptz not null default now(),
    created_at timestamptz not null default now(),
    unique (user_id, action)
);

alter table public.user_rate_limits enable row level security;
-- No policies = fully locked for anon/authenticated. Service role bypasses RLS.

create index if not exists idx_rate_limits_user_action
    on public.user_rate_limits (user_id, action);
