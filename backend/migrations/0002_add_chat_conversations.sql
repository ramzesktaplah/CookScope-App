-- CookScope chat conversations sync
-- Stores per-user chat history so it survives reinstall and re-login.
-- The Android client upserts each conversation by (user_id, ext_id),
-- where ext_id is the locally-generated "chat_<ts>_<rand>" id.

create table if not exists public.chat_conversations (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references auth.users(id) on delete cascade,
  ext_id      text not null,
  title       text not null default 'New Chat',
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now(),
  messages    jsonb not null default '[]'::jsonb,
  unique (user_id, ext_id)
);

-- Newest-first lookup for the chat history drawer.
create index if not exists chat_conversations_user_updated_idx
  on public.chat_conversations (user_id, updated_at desc);

-- Row Level Security: a user can only see / modify their own conversations.
alter table public.chat_conversations enable row level security;

drop policy if exists "users select own conversations"
  on public.chat_conversations;
create policy "users select own conversations"
  on public.chat_conversations
  for select
  using (auth.uid() = user_id);

drop policy if exists "users insert own conversations"
  on public.chat_conversations;
create policy "users insert own conversations"
  on public.chat_conversations
  for insert
  with check (auth.uid() = user_id);

drop policy if exists "users update own conversations"
  on public.chat_conversations;
create policy "users update own conversations"
  on public.chat_conversations
  for update
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

drop policy if exists "users delete own conversations"
  on public.chat_conversations;
create policy "users delete own conversations"
  on public.chat_conversations
  for delete
  using (auth.uid() = user_id);
