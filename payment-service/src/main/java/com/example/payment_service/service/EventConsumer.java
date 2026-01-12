package com.example.payment_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.payment_service.event.Event;
import com.example.payment_service.model.PaymentInbox;
import com.example.payment_service.repository.PaymentInboxRepository;

import jakarta.transaction.Transactional;

@Service
public class EventConsumer {

    @Autowired
    private PaymentInboxRepository paymentInboxRepository;

    @KafkaListener(topics = "${application.topic.inventory}")
    @Transactional
    public void listen(Event event) {
        if (!paymentInboxRepository.existsByTransactionIdAndType(event.transactionId(), event.type())) {
            PaymentInbox inbox = new PaymentInbox();
            inbox.setTransactionId(event.transactionId());
            inbox.setType(event.type());
            inbox.setPayload(event);
            paymentInboxRepository.save(inbox);
        }
    }
}
