-- Hard mode is chosen when a game starts and never changes. NULL means unknown: a game created
-- before this column existed, or by a client that did not send the setting.
alter table game_entity add column hard_mode boolean;

-- The same value for statistics. Rows from before this migration stay NULL.
alter table stat_game add column hard_mode boolean;

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
       g.hard_mode
from stat_round r
join stat_game g         on g.id = r.game_id
join stat_game_mode m    on m.id = g.game_mode_id
join stat_game_type t    on t.id = r.game_type_id
join stat_round_key k    on k.id = r.round_key_id
join stat_answer_state a on a.id = r.answer_state_id;
