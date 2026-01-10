package com.company.order.domain;

import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        String eventId,
        String orderId,
        long amount,
        Instant createdAt
) {
    public static OrderCreatedEvent of(String orderId, long amount) {
        return new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                orderId,
                amount,
                Instant.now()
        );
    }
}
