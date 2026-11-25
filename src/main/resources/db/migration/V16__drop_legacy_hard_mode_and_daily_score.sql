-- The previous version of the app no longer runs, so nothing reads or writes these.
-- stat_game.hard_mode stays: it holds the historical statistics.
alter table game_entity drop column hard_mode;

-- Its rows were copied into daily_score_by_difficulty by V15; no view reads it any more.
drop table daily_score;
