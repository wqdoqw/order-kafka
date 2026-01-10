package com.company.order.service;

import com.company.order.domain.OrderCreatedEvent;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    public void handle(OrderCreatedEvent event) {
        System.out.println("[DEBUG_LOG] OrderService handling event: eventId=" + event.eventId() + ", orderId=" + event.orderId() + ", amount=" + event.amount());

        if (event.amount() == 999) {
            System.out.println("[DEBUG_LOG] Detected test condition (amount=999). Throwing simulated exception for eventId=" + event.eventId());
            throw new RuntimeException("Simulated failure for DLQ test. eventId=" + event.eventId());
        }

        System.out.println("[DEBUG_LOG] Successfully processed event: eventId=" + event.eventId());
        System.out.println("[OrderService] processed orderId=" + event.orderId() + ", amount=" + event.amount());
    }
}
