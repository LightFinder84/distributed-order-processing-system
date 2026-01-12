package com.example.payment_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.payment_service.event.Event;
import com.example.payment_service.model.PaymentOutbox;
import com.example.payment_service.repository.PaymentOutboxRepository;

import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class EventProducer {

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Lazy
    private EventProducer self;

    @Value("${application.topic.payment}")
    private String paymentTopic;

    public void produce() {
        int count;
        do {
            count = self.produceBatch();
        } while (count > 0);
    }

    @Transactional
    public int produceBatch() {
        List<PaymentOutbox> pendingOutboxes = paymentOutboxRepository.findByStatus("pending");
        for (PaymentOutbox outbox : pendingOutboxes) {
            sendEvent(outbox);
        }
        return pendingOutboxes.size();
    }

    private void sendEvent(PaymentOutbox outbox) {
        Event event = outbox.getPayload();
        try {
            kafkaTemplate.send(paymentTopic, event.transactionId().toString(),
                    objectMapper.writeValueAsString(event)).get();
            outbox.setStatus("processed");
            paymentOutboxRepository.save(outbox);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
