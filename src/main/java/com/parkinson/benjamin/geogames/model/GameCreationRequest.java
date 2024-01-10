package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.dao.GameType;

public record GameCreationRequest(String gameType, int numberOfRounds) {}
