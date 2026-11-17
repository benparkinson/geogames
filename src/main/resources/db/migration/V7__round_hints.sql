-- The hints taken in a round before it was answered, so a refreshed page can restore them.
-- guesses_before is how many guesses the round had when the hint was taken, which decides what
-- some hints show (Tripoint reveals a country not found yet).
create table game_round_hint (
    round_id       bigint not null references game_round_entity (id) on delete cascade,
    hint_index     int    not null,
    guesses_before int    not null,
    primary key (round_id, hint_index)
);
