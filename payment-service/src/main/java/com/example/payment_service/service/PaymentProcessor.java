package com.example.payment_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.payment_service.DTO.PaymentMockRequest;
import com.example.payment_service.model.Payment;
import com.example.payment_service.repository.PaymentRepository;

import jakarta.transaction.Transactional;

@Service
public class PaymentProcessor {
    
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RestClient restClient;

    @Autowired
    @Lazy
    private PaymentProcessor self;

    @Value("${application.batch-size}")
    private int batchSize;

    private String processPayment(Payment payment) {
        // send request to 3rd application
        try {
            PaymentMockRequest req = new PaymentMockRequest();
            req.setTransactionId(payment.getTransactionId().toString());
            req.setHookUrl(payment.getHookUrl());
            req.setAmount(payment.getAmount());
            
            String paymentSite = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(String.class);

            return paymentSite;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Scheduled(fixedRate = 5000)
    public void processPayments() {
        List<Payment> selectedPayments = self.fetchAndLockPayment();
        for (Payment payment : selectedPayments) {
            String paymentSite = processPayment(payment);
            if (paymentSite != null) {
                self.updatePaymentResult(payment, "pending", paymentSite);
            } else {
                self.updatePaymentResult(payment, "skipped", null);
            }
        }
    }

    @Transactional
    public List<Payment> fetchAndLockPayment() {
        Pageable pageable = PageRequest.of(0, batchSize, Sort.by("lastUdatedAt").ascending());
        List<Payment> createdPayments = paymentRepository.findByStatus("created", pageable);
        for (Payment payment : createdPayments) {
            payment.setStatus("processing");
        }
        paymentRepository.saveAll(createdPayments);
        return createdPayments;
    }

    @Transactional
    public void updatePaymentResult(Payment payment, String status, String paymentSite) {
        Payment dbPayment = paymentRepository.findByTransactionId(payment.getTransactionId());
        if (dbPayment != null) {
            dbPayment.setStatus(status);
            dbPayment.setPaymentSite(paymentSite);
        }
    }
}
