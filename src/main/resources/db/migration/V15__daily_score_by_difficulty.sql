-- Running totals of each day's finished daily challenges, per difficulty league.
-- A new table, not a changed primary key: that isn't portable between H2 and Postgres, and the
-- previous version of the app, still running during a deploy, writes to daily_score.
create table daily_score_by_difficulty (
    challenge_date date        not null,
    difficulty     varchar(10) not null,  -- EASY, NORMAL or HARD; unknown games count as NORMAL
    players        integer not null default 0,  -- finished games counted
    score_total    integer not null default 0,  -- the sum of their scores
    bucket_0       integer not null default 0,  -- score below 10% of the maximum
    bucket_1       integer not null default 0,
    bucket_2       integer not null default 0,
    bucket_3       integer not null default 0,
    bucket_4       integer not null default 0,
    bucket_5       integer not null default 0,
    bucket_6       integer not null default 0,
    bucket_7       integer not null default 0,
    bucket_8       integer not null default 0,
    bucket_9       integer not null default 0,  -- 90% up to, not including, the maximum
    bucket_10      integer not null default 0,  -- a perfect score
    primary key (challenge_date, difficulty)
);

-- Totals from before the split were never divided by difficulty: they become the NORMAL league.
insert into daily_score_by_difficulty
    (challenge_date, difficulty, players, score_total, bucket_0, bucket_1, bucket_2, bucket_3,
     bucket_4, bucket_5, bucket_6, bucket_7, bucket_8, bucket_9, bucket_10)
select challenge_date, 'NORMAL', players, score_total, bucket_0, bucket_1, bucket_2, bucket_3,
       bucket_4, bucket_5, bucket_6, bucket_7, bucket_8, bucket_9, bucket_10
from daily_score;

-- For the owner, in the database console. Read-only.
drop view daily_score_history;

create view daily_score_history as
select challenge_date,
       difficulty,
       players,
       round(1.0 * score_total / nullif(players, 0), 1) as average_score,
       bucket_0, bucket_1, bucket_2, bucket_3, bucket_4, bucket_5,
       bucket_6, bucket_7, bucket_8, bucket_9, bucket_10
from daily_score_by_difficulty;
