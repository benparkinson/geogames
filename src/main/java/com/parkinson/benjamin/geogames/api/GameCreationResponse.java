package com.parkinson.benjamin.geogames.api;

import java.util.UUID;

/** The id is the game's public id, a random UUID (see {@code GameEntity.getPublicId()}). */
public record GameCreationResponse(UUID id) {}
