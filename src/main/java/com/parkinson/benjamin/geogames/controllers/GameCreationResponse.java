package com.parkinson.benjamin.geogames.controllers;

import org.springframework.hateoas.Link;

public record GameCreationResponse(Link link, long id) {

}
