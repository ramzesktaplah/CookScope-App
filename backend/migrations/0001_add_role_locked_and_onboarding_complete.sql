-- CookScope auth/onboarding migration
-- Adds role_locked and onboarding_complete columns to profiles so the
-- permanent-role rule and onboarding-completion state are enforced at
-- the data layer.

alter table public.profiles
  add column if not exists role_locked boolean not null default false;

alter table public.profiles
  add column if not exists onboarding_complete boolean not null default false;

-- Backfill existing rows.
update public.profiles
  set role_locked = false,
      onboarding_complete = false
  where role_locked is null or onboarding_complete is null;
