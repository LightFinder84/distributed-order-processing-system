package com.example.payment_simulator.controller;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import com.example.payment_simulator.DTO.ConfirmRequest;
import com.example.payment_simulator.DTO.MockRequest;
import com.example.payment_simulator.DTO.PaymentResponse;

import jakarta.validation.Valid;

@RestController
public class ConfirmController {

    @Value("${application.secret-key}")
    private String secretKey;

    private final RestClient restClient = RestClient.create();

    @Autowired
    private Map<String, MockRequest> requestStorage;

    @PostMapping(path = "mock/confirm")
    public void confirmPayment(@Valid @RequestBody ConfirmRequest req) {
        if (!requestStorage.containsKey(req.getToken())) {
            return;
        }
        MockRequest mockRequest = requestStorage.get(req.getToken());
        String hookUrl = mockRequest.getHookUrl();
        PaymentResponse myPayload = new PaymentResponse();
        myPayload.setTransactionId(mockRequest.getTransactionId());

        if (req.isSuccess()) {
            myPayload.setStatus("success");
        } else {
            myPayload.setStatus("failed");
        }
        myPayload.setAmount(mockRequest.getAmount());

        try {
            String data = mockRequest.getTransactionId() + mockRequest.getAmount();
            String signature = generateSignature(data, secretKey);
    
            restClient.post()
                .uri(hookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(myPayload)
                .header("X-Signature", signature)
                .retrieve()
                .body(String.class);
            
        } catch (Exception e) {
            System.out.println("Failed to generate signature.");
        }

    }

    public String generateSignature(String data, String secretKey) throws Exception {
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSha256.init(secretKeySpec);
        byte[] hash = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
