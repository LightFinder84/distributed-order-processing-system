package com.example.payment_simulator.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.payment_simulator.DTO.MockRequest;

@Controller
public class PaymentController {

    @Autowired
    private Map<String, MockRequest> requestStorage;
    
    @GetMapping(path = "/gui/{token}")
    public String processPayment(@PathVariable String token, Model model) {
        
        if (!requestStorage.containsKey(token)) {
            return "error";
        }

        MockRequest req = requestStorage.get(token);
        if (!req.getTransactionStatus().equals("pending")) {
            return "error";
        }

        model.addAttribute("transactionId", req.getTransactionId());
        model.addAttribute("amount", req.getAmount().toString());
        model.addAttribute("token", token);

        return "payment";
    }
}
