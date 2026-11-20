-- How many of a round's answers the player found. NULL means the round was answered before
-- partial credit existed (it scores 0 unless correct), the client did not report it, or the round
-- is not answered yet.
alter table game_round_entity add column answers_found smallint;
