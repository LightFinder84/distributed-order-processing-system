package com.example.inventory_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.inventory_service.event.Event;
import com.example.inventory_service.model.InventoryInbox;
import com.example.inventory_service.repository.InventoryInboxRepository;

import jakarta.transaction.Transactional;

@Service
public class EventConsumer {

    @Autowired
    private InventoryInboxRepository inboxRepository;

    @KafkaListener(topics = { "${application.topic.order}", "${application.topic.payment}" })
    @Transactional
    public void EventListener(Event event) {
        InventoryInbox inbox = new InventoryInbox();
        inbox.setTransactionId(event.transactionId());
        inbox.setType(event.type());
        inbox.setPayload(event);
        inboxRepository.save(inbox);
    }
}
