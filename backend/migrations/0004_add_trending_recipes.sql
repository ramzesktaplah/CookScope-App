-- Creates the trending_recipes table used by the daily trending cache.
-- The trending-refresh edge function is called daily by pg_cron to populate
-- this table with 20 recipeapi.io recipes, and the trending-fetch edge
-- function serves the rows to the Discover tab.

create table if not exists public.trending_recipes (
    id bigint generated always as identity primary key,
    recipe_id text not null,
    title text not null,
    image_url text,
    cuisine text,
    difficulty text,
    cook_time_min integer,
    recipe_data jsonb not null,
    for_date date not null,
    created_at timestamp with time zone default now(),
    updated_at timestamp with time zone default now(),
    unique (recipe_id, for_date)
);

-- Enable RLS so the table can be queried by authenticated users.
alter table public.trending_recipes enable row level security;

create policy "Allow authenticated read access to trending recipes"
    on public.trending_recipes
    for select
    to authenticated
    using (true);

-- Index for the daily lookup.
create index if not exists idx_trending_recipes_for_date
    on public.trending_recipes (for_date);

-- Refresh the materialized view (if any) on the columnar payload.
-- Note: the edge function upserts rows directly, so no pg_cron SQL body is needed here.
