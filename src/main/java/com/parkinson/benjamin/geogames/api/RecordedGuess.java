package com.parkinson.benjamin.geogames.api;

/** A guess the server has stored for a round. */
public record RecordedGuess(String text, boolean correct) {}
