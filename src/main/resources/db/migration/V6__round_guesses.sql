-- The guesses made in a round, in order, so a refreshed page can restore them. Only known place
-- names are stored (the spelling from the name lists), never the text as typed.
create table game_round_guess (
    round_id bigint       not null references game_round_entity (id) on delete cascade,
    position int          not null,
    guess    varchar(255) not null,
    correct  boolean      not null,
    primary key (round_id, position)
);
