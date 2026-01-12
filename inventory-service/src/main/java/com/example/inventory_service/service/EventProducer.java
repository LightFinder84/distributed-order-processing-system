package com.example.inventory_service.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.example.inventory_service.event.Event;
import com.example.inventory_service.model.InventoryOutbox;
import com.example.inventory_service.repository.InventoryOutboxRepository;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import tools.jackson.databind.ObjectMapper;

@Service
public class EventProducer {

    @Autowired
    private InventoryOutboxRepository inventoryOutboxRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment environment;

    @Autowired
    @Lazy
    private EventProducer self;

    @Value("${application.batch-size}")
    private int batchSize;

    public void produce() {
        int count = self.produceBatch();
        while (count != 0) {
            count = self.produceBatch();
        }
    }

    @Transactional
    public int produceBatch() {
        Pageable pageable = PageRequest.of(0, batchSize, Sort.by("createdAt").ascending());
        List<InventoryOutbox> pendingOutbox = inventoryOutboxRepository.findByStatus("pending", pageable);
        for (InventoryOutbox outbox : pendingOutbox) {
            outbox.setStatus("processing");
            sendEventAsync(outbox);
        }
        inventoryOutboxRepository.saveAll(pendingOutbox);
        return pendingOutbox.size();
    }

    public void sendEventAsync(InventoryOutbox outbox) {
        try {
            Event event = outbox.getPayload();
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    environment.getProperty("application.topic.inventory"), event.transactionId().toString(),
                    objectMapper.writeValueAsString(event));
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    self.updateStatus(outbox, "processed");
                } else {
                    System.err.println(ex.getMessage());
                    self.updateStatus(outbox, "failed");
                }
            });
        } catch (Exception e) {
            self.updateStatus(outbox, "failed");
            e.printStackTrace();
        }
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public void updateStatus(InventoryOutbox outbox, String status) {
        inventoryOutboxRepository.findById(outbox.getId()).ifPresent(item -> {
            item.setStatus(status);
            inventoryOutboxRepository.save((item));
        });
    }
}
