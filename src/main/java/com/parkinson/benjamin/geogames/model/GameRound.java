package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.dao.GameType;

public record GameRound(GameType gameType, String jsonBlob, int totalRoundCount) {}
