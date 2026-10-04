create table public.oak_operators (
 user_id uuid primary key references auth.users(id) on delete cascade,
 created_at timestamptz not null default now()
);
alter table public.oak_operators enable row level security;
grant select on public.oak_operators to authenticated;
create policy own_membership on public.oak_operators for select to authenticated using ((select auth.uid()) = user_id);
create table public.oak_requests (
 id uuid primary key,
 created_at timestamptz not null default now(),
 ready boolean not null default false,
 payload jsonb not null,
 status text not null default 'Nueva' check (status in ('Nueva','Por contactar','Esperando información','Presupuesto enviado','Aceptada','Cerrada')),
 notes text not null default '' check (length(notes) <= 10000),
 quote jsonb,
 visit jsonb,
 photos jsonb not null default '[]'::jsonb
);
alter table public.oak_requests enable row level security;
revoke all on public.oak_requests, public.oak_operators from anon;
grant select, update on public.oak_requests to authenticated;
create policy operator_read on public.oak_requests for select to authenticated using (exists(select 1 from public.oak_operators where user_id = (select auth.uid())));
create policy operator_update on public.oak_requests for update to authenticated using (exists(select 1 from public.oak_operators where user_id = (select auth.uid()))) with check (exists(select 1 from public.oak_operators where user_id = (select auth.uid())));
create table public.oak_intake_limits (id bigint generated always as identity primary key, fingerprint text not null, created_at timestamptz not null default now());
create index oak_limit_time on public.oak_intake_limits(created_at);
create index oak_limit_contact on public.oak_intake_limits(fingerprint,created_at);
alter table public.oak_intake_limits enable row level security;
revoke all on public.oak_intake_limits from anon, authenticated;
grant all on public.oak_requests, public.oak_intake_limits, public.oak_operators to service_role;
grant usage, select on sequence public.oak_intake_limits_id_seq to service_role;
create function public.oak_claim_intake(fingerprint text) returns boolean language plpgsql security invoker set search_path = '' as $$
begin
 perform pg_advisory_xact_lock(8173902);
 delete from public.oak_intake_limits where created_at < now() - interval '1 day';
 if (select count(*) from public.oak_intake_limits where created_at > now()-interval '1 hour') >= 100
 or (select count(*) from public.oak_intake_limits l where l.fingerprint = oak_claim_intake.fingerprint and created_at > now()-interval '1 hour') >= 5 then return false; end if;
 insert into public.oak_intake_limits(fingerprint) values (oak_claim_intake.fingerprint);
 return true;
end; $$;
revoke all on function public.oak_claim_intake(text) from public, anon, authenticated;
grant execute on function public.oak_claim_intake(text) to service_role;
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types) values ('oak-request-photos','oak-request-photos',false,8388608,array['image/jpeg','image/png','image/webp']);
create policy operator_photos on storage.objects for select to authenticated using (bucket_id = 'oak-request-photos' and exists(select 1 from public.oak_operators where user_id = (select auth.uid())));
