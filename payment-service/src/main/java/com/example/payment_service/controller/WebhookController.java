package com.example.payment_service.controller;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.payment_service.DTO.WebhookRequest;
import com.example.payment_service.event.Event;
import com.example.payment_service.event.Event.Type;
import com.example.payment_service.model.Payment;
import com.example.payment_service.model.PaymentInbox;
import com.example.payment_service.model.PaymentOutbox;
import com.example.payment_service.repository.PaymentInboxRepository;
import com.example.payment_service.repository.PaymentOutboxRepository;
import com.example.payment_service.repository.PaymentRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@RestController
public class WebhookController {

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;

    @Autowired
    private PaymentInboxRepository paymentInboxRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${application.secret-key}")
    private String secretKey;

    @PostMapping(path = "${application.webhook-path}")
    @Transactional
    public void handlePaymentCallback(@Valid @RequestBody WebhookRequest req, @RequestHeader("X-Signature") String signature) {
        System.out.println("Processing webhook request");

        // validate signature
        String data = req.getTransactionId().toString() + req.getAmount();
        if (!isValidSignature(data, signature)) {
            System.err.println("Invalid signature");
            return;
        }

        // check payment
        Payment payment = paymentRepository.findByTransactionId(req.getTransactionId());
        if (!payment.getStatus().equals("pending")) {
            System.err.println("Payment not found");
            return;
        }

        // create event
        PaymentInbox inbox = paymentInboxRepository.findByTransactionId(payment.getTransactionId());
        if (inbox == null) {
            return;
        }
        Event inboxEvent = inbox.getPayload();
        Event event = null;

        // Update payment status
        if (req.getStatus().equals("success")) {
            payment.setStatus("completed");
            event = new Event(inboxEvent.transactionId(), Type.PAYMENT_SUCCESS, OffsetDateTime.now(), null);
        } else {
            payment.setStatus("failed");
            event = new Event(inboxEvent.transactionId(), Type.PAYMENT_FAILED, OffsetDateTime.now(), inboxEvent.items());
        }
        paymentRepository.save(payment);

        PaymentOutbox outbox = new PaymentOutbox();
        outbox.setTransactionId(event.transactionId());
        outbox.setType(event.type());
        outbox.setPayload(event);
        paymentOutboxRepository.save(outbox);
    }

    private boolean isValidSignature(String data, String receivedSignature) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);
            
            byte[] hash = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);

            return expectedSignature.equals(receivedSignature);
        } catch (Exception e) {
            return false;
        }
    }
}
