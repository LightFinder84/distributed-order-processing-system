package com.example.payment_simulator.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.payment_simulator.DTO.MockRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "mock/pay")
public class HomeController {

    @Autowired
    private Environment environment;

    @Autowired
    private Map<String, MockRequest> requestStorage;
    
    @PostMapping
    public ResponseEntity<String> createTransaction(@Valid @RequestBody MockRequest req) {
        
        System.out.println(req);

        String mockToken = UUID.randomUUID().toString();
        requestStorage.put(mockToken, req);

        String url = environment.getProperty("server.url") + "gui/" + mockToken;
        return new ResponseEntity<>(url, HttpStatus.ACCEPTED);
    }
}
