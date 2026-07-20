-- CookScope migration: remove account-type / role system
--
-- Drops the role, role_locked, stars_count, and stars_given columns
-- from the profiles table. The account-type (Chef/Teacher vs Learner)
-- system has been removed from the app; profiles now hold only
-- id, username, display_name, bio, avatar_url, plan_type,
-- onboarding_complete, created_at, updated_at.
--
-- Reverses the role_locked column added in 0001_add_role_locked_and_onboarding_complete.sql.

alter table public.profiles
  drop column if exists role_locked;

alter table public.profiles
  drop column if exists role;

alter table public.profiles
  drop column if exists stars_count;

alter table public.profiles
  drop column if exists stars_given;
