-- Optimistic locking for answers: two requests answering one round at the same moment can't both
-- write it. The default covers rounds that exist already and rows inserted by an older version of
-- the app during a deploy.
alter table game_round_entity add column version integer not null default 0;
