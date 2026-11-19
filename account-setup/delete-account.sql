-- Install in the intended AnDPoP Supabase project only.
-- The mobile app uses its signed-in user's access token.
create or replace function public.delete_my_andpop_account()
returns void
language plpgsql
security definer
set search_path = ''
as $function$
declare
    current_user_id uuid := auth.uid();
begin
    if current_user_id is null then
        raise exception 'Authentication required' using errcode = '42501';
    end if;
    delete from auth.users where id = current_user_id;
end;
$function$;

revoke all on function public.delete_my_andpop_account() from public, anon, authenticated;
grant execute on function public.delete_my_andpop_account() to authenticated;
