-- Every game has had a play id and a mode since V2, and games live for 24 hours.
-- A row without them could only be left over from before that, so it is removed.
delete from game_entity where play_id is null or game_mode is null;

alter table game_entity alter column play_id set not null;
alter table game_entity alter column game_mode set not null;
