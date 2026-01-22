package com.example.order_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.order_service.DTO.PlaceOrderRequest;
import com.example.order_service.DTO.PlaceOrderResponse;
import com.example.order_service.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "/v1/order")
public class OrderController {

    /**
     * Service for processing order placement.
     */
    @Autowired
    private OrderService orderService;

    /**
     * Place order endpoint.
     *
     * @param orderRequest
     * @return Http response entity.
     */
    @PostMapping
    public final ResponseEntity<PlaceOrderResponse> placeOrder(
            @Valid @RequestBody final PlaceOrderRequest orderRequest) {

        PlaceOrderResponse orderResponse = orderService.placeOrder(orderRequest);

        return new ResponseEntity<>(orderResponse, HttpStatus.CREATED);
    }
}
