package com.example.order_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.order_service.event.Event;
import com.example.order_service.event.Event.Type;
import com.example.order_service.model.OrderInbox;
import com.example.order_service.repository.OrderInboxRepository;

import jakarta.transaction.Transactional;

@Service
public class EventConsumer {

    @Autowired
    private OrderInboxRepository orderInboxRepository;

    @KafkaListener(topics = {
            "${application.topic.payment}",
            "${application.topic.inventory}"
    })
    @Transactional
    public void eventListener(Event event) {
        if (event.type() == Type.INVENTORY_RESERVED) {
            return; // skip success event
        }
        if (orderInboxRepository.existsByTransactionIdAndType(event.transactionId(), event.type())) {
            return;
        }
        // store to database
        try {
            OrderInbox inbox = OrderInbox.builder().payload(event).transactionId(event.transactionId())
                    .type(event.type()).build();

            orderInboxRepository.save(inbox);
        } catch (DataIntegrityViolationException e) {
            System.out.println("Duplicate detected by DB constraint for: " + event.transactionId());
        }
    }
}
