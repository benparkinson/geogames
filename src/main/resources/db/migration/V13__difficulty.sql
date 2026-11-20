-- The game's difficulty, chosen when it is created and never changed: EASY, NORMAL or HARD.
-- NULL means unknown (an old game, or a client that sent neither difficulty nor hardMode).
alter table game_entity add column difficulty varchar(10);

update game_entity set difficulty = case when hard_mode then 'HARD' else 'NORMAL' end
where hard_mode is not null;
