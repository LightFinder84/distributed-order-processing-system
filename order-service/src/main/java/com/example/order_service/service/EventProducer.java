package com.example.order_service.service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.order_service.model.OrderOutbox;
import com.example.order_service.repository.OrderOutboxRepository;

import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class EventProducer {

    @Autowired
    private OrderOutboxRepository orderOutboxRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper jacksonJsonMapper;

    @Autowired
    @Lazy
    private EventProducer self;

    @Value("${application.batch-size}")
    private int batchSize;

    @Value("${application.topic.order}")
    private String orderTopic;

    public void processOutbox() {
        int count = self.processBatch();
        while (count != 0) {
            count = self.processBatch();
        }
    }

    @Transactional
    public int processBatch() {
        Pageable pageable = PageRequest.of(0, batchSize, Sort.by("createdAt").ascending());
        List<OrderOutbox> pendingPayloads = orderOutboxRepository.findByStatus("pending", pageable);
        for (OrderOutbox pendingOutbox : pendingPayloads) {
            try {
                kafkaTemplate
                        .send(orderTopic, pendingOutbox.getTransactionId().toString(),
                                jacksonJsonMapper.writeValueAsString(pendingOutbox.getPayload()))
                        .get(5, TimeUnit.SECONDS);
                pendingOutbox.markAsProcessed();
                orderOutboxRepository.save(pendingOutbox);
            } catch (TimeoutException | InterruptedException e1) {
                System.out.println(
                        "Send kafka message failed, skipped for transaction " + pendingOutbox.getTransactionId());
            } catch (Exception e) {
                pendingOutbox.markAsFailed();
                e.printStackTrace();
            }
        }
        return pendingPayloads.size();
    }
}
