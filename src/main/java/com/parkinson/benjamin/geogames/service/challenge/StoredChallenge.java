package com.parkinson.benjamin.geogames.service.challenge;

/** A challenge with its primary key, which never leaves the server. */
public record StoredChallenge(long id, Challenge challenge) {}
