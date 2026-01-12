package com.example.inventory_service.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.inventory_service.event.Event;
import com.example.inventory_service.event.Event.Type;
import com.example.inventory_service.model.InventoryInbox;
import com.example.inventory_service.model.InventoryOutbox;
import com.example.inventory_service.repository.InventoryInboxRepository;
import com.example.inventory_service.repository.InventoryOutboxRepository;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@Service
public class InventoryProcessor {

    @Autowired
    private InventoryInboxRepository inboxRepository;

    @Autowired
    private InventoryOutboxRepository outboxRepository;

    @Autowired
    private StockService stockService;

    @Autowired
    @Lazy
    private InventoryProcessor self;

    @Value("${application.topic.order}")
    private String orderTopic;

    @Value("${application.topic.payment}")
    private String paymentTopic;

    @Value("${application.batch-size}")
    private int batchSize;

    public void processEvents() {
        int count = self.processBatch();
        while (count != 0) {
            count = self.processBatch();
        }
    }

    @Transactional // manage inbox
    public int processBatch() {
        Pageable pageable = PageRequest.of(0, batchSize, Sort.by("createdAt").ascending());
        List<InventoryInbox> pendingInboxes = inboxRepository.findByStatus("pending", pageable);
        for (InventoryInbox inbox : pendingInboxes) {
            self.processEvent(inbox.getPayload());
            inbox.setStatus("processed");
        }
        inboxRepository.saveAll(pendingInboxes);
        return pendingInboxes.size();
    }

    @Transactional(value = TxType.REQUIRES_NEW) // manage outbox
    public void processEvent(Event incoming) {
        if (incoming.type() == Type.ORDER_CREATED) {
            InventoryOutbox outbox = self.handleOrderCreatedEvent(incoming);
            outboxRepository.save(outbox);
        }

        if (incoming.type() == Type.PAYMENT_FAILED) {
            stockService.addStock(incoming);
        }
    }

    @Transactional(value = TxType.MANDATORY)
    public InventoryOutbox handleOrderCreatedEvent(Event incoming) {
        Event outgoing = null;
        InventoryOutbox outbox = new InventoryOutbox();
        outbox.setTransactionId(incoming.transactionId());
        // deduct stock
        try {
            stockService.deductStock(incoming);
            outgoing = new Event(incoming.transactionId(), Type.INVENTORY_RESERVED,
                    OffsetDateTime.now(), incoming.items());
        } catch (Exception e) {
            outgoing = new Event(incoming.transactionId(), Type.INVENTORY_FAILED,
                    OffsetDateTime.now(), null);
        }

        outbox.setType(outgoing.type());
        outbox.setPayload(outgoing);
        return outbox;
    }
}
