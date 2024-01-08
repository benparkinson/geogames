package com.parkinson.benjamin.geogames.model;

import java.util.List;

public record Game(List<GameRound> rounds, int totalRoundCount) {}
