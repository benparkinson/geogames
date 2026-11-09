-- RIVERS_ON_MAP and RIVERS_BY_ENDS were removed from GameType. A game that still holds one of them
-- could no longer be read (the enum column would fail to load), so delete those games; their
-- rounds go with them (on delete cascade). The statistics tables keep the history as plain names.
delete from game_entity
where game_mode in ('RIVERS_ON_MAP', 'RIVERS_BY_ENDS')
   or id in (select game_id from game_round_entity
             where game_type in ('RIVERS_ON_MAP', 'RIVERS_BY_ENDS'));
