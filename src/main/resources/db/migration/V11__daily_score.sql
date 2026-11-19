-- Which daily challenge a game was made from. NULL for every other game, and for daily games
-- created before this column existed (those are never counted).
alter table game_entity add column daily_date date;

-- Running totals of each day's finished daily challenges: no per-player rows, so it never needs
-- cleaning up. One row per challenge date, never deleted (about 90 bytes a day). Kept apart from
-- daily_challenge, whose row may be deleted to mend a day.
create table daily_score (
    challenge_date date    primary key,
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
    bucket_10      integer not null default 0   -- a perfect score
);

-- For the owner, in the database console. Read-only.
create view daily_score_history as
select challenge_date,
       players,
       round(1.0 * score_total / nullif(players, 0), 1) as average_score,
       bucket_0, bucket_1, bucket_2, bucket_3, bucket_4, bucket_5,
       bucket_6, bucket_7, bucket_8, bucket_9, bucket_10
from daily_score;
