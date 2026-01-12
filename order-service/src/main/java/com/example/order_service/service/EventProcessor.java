package com.example.order_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.order_service.event.Event;
import com.example.order_service.model.Order;
import com.example.order_service.model.OrderInbox;
import com.example.order_service.repository.OrderInboxRepository;
import com.example.order_service.repository.OrderRepository;

import jakarta.transaction.Transactional;

@Service
public class EventProcessor {

    @Autowired
    private OrderInboxRepository orderInboxRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    @Lazy
    private EventProcessor self;

    @Value("${application.batch-size}")
    private int batchSize;

    public void processEvents() {
        int count = self.processBatch();
        while (count != 0) {
            count = self.processBatch();
        }
    }

    @Transactional
    public int processBatch() {
        Pageable pageable = PageRequest.of(0, batchSize, Sort.by("createdAt").ascending());
        List<OrderInbox> pendingInboxes = orderInboxRepository.findByStatus("pending", pageable);
        for (OrderInbox inbox : pendingInboxes) {
            processEvent(inbox);
        }
        return pendingInboxes.size();
    }

    private void processEvent(OrderInbox inbox) {
        UUID transactionId = inbox.getTransactionId();
        Order order = orderRepository.findByTransactionId(transactionId);
        if (order == null) {
            inbox.markAsSkipped();
            orderInboxRepository.save(inbox);
            return;
        }
        Event event = inbox.getPayload();

        if (Event.Type.INVENTORY_FAILED == event.type()) {
            order.markAsCancelled();
        }
        if (Event.Type.PAYMENT_FAILED == event.type()) {
            order.markAsCancelled();
        }
        if (Event.Type.PAYMENT_SUCCESS == event.type()) {
            order.markAsCompleted();
        }
        orderRepository.save(order);

        inbox.markAsProcessed();
        orderInboxRepository.save(inbox);
    }
}
