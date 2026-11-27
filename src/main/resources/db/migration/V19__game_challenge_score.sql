-- The score to beat for a game started from a challenge, NULL for every other game.
alter table game_entity add column challenge_score integer;
