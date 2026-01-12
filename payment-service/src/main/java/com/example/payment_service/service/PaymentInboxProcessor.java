package com.example.payment_service.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.payment_service.event.Event;
import com.example.payment_service.event.Event.Type;
import com.example.payment_service.event.Event.OrderItem;
import com.example.payment_service.model.Payment;
import com.example.payment_service.model.PaymentInbox;
import com.example.payment_service.repository.PaymentInboxRepository;
import com.example.payment_service.repository.PaymentRepository;

import jakarta.transaction.Transactional;

@Service
public class PaymentInboxProcessor {

    @Autowired
    private PaymentInboxRepository paymentInboxRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private Environment environment;

    @Autowired
    @Lazy
    private PaymentInboxProcessor self;

    @Value("${application.batch-size}")
    private int batchSize;

    private BigDecimal calcualteAmount(Event event) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : event.items()) {
            BigDecimal subTotal = item.priceAtPurchase().multiply(new BigDecimal(item.quantity()));
            total = total.add(subTotal);
        }
        return total;
    }

    public void processPaymentEvents() {
        int count;
        do {
            count = self.processBatch();
        } while (count > 0);
    }

    @Transactional
    public int processBatch() {
        Pageable pageable = PageRequest.of(0, batchSize, Sort.by("createdAt").ascending());
        List<PaymentInbox> pendingInboxes = paymentInboxRepository.findByStatus("pending", pageable);
        for (PaymentInbox inbox : pendingInboxes) {
            processPaymentEvent(inbox);
        }
        return pendingInboxes.size();
    }

    private void processPaymentEvent(PaymentInbox inbox) {
        if (inbox.getPayload().type() == Type.INVENTORY_RESERVED) {
            // store payment data
            Payment payment = new Payment();
            payment.setHookUrl(environment.getProperty("application.webhook-url"));
            payment.setTransactionId(inbox.getPayload().transactionId());
            payment.setAmount(calcualteAmount(inbox.getPayload()));
            payment.setLastUdatedAt(OffsetDateTime.now());
            paymentRepository.save(payment);
    
            // mark as processed
            inbox.setStatus("processed");
        } else {
            inbox.setStatus("skipped");
        }
        paymentInboxRepository.save(inbox);

    }
}
