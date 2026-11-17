-- The rounds of each day's daily challenge, pinned the first time anyone asks for that date, so
-- a deploy during the day can't change the puzzle. No user data. Never deleted: a few hundred
-- bytes a day.
create table daily_challenge (
    challenge_date date primary key,
    rounds         text not null    -- JSON: [{"gameType": "TRIPOINT", "roundKey": "..."}, ...]
);
