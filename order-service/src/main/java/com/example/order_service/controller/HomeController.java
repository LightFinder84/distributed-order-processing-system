package com.example.order_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/")
public class HomeController {

    /**
     * Application's health check endpoint.
     *
     * @return a verification message
     */
    @GetMapping
    public final String hello() {
        return "It's working!";
    }
}
