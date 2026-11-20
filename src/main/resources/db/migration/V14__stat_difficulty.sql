-- The game's difficulty for the statistics, like hard_mode: the setting the player chose.
-- NULL means unknown (a game from before the setting, or a client that did not send it).
alter table stat_game add column difficulty varchar(10);

update stat_game set difficulty = case when hard_mode then 'HARD' else 'NORMAL' end
where hard_mode is not null;

drop view round_result;

create view round_result as
select g.played_on      as answered_on,
       g.play_id,
       m.name           as game_mode,
       g.rounds_in_game,
       r.round_number,
       t.name           as game_type,
       k.round_key,
       a.name           as answer_state,
       r.hints_used,
       r.wrong_guesses,
       t.hint_count,
       t.guess_limit,
       r.points,
       g.hard_mode,
       g.difficulty
from stat_round r
join stat_game g         on g.id = r.game_id
join stat_game_mode m    on m.id = g.game_mode_id
join stat_game_type t    on t.id = r.game_type_id
join stat_round_key k    on k.id = r.round_key_id
join stat_answer_state a on a.id = r.answer_state_id;
