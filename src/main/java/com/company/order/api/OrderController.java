package com.company.order.api;

import com.company.order.domain.OrderCreatedEvent;
import com.company.order.kafka.OrderEventProducer;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderEventProducer producer;

    public OrderController(OrderEventProducer producer) {
        this.producer = producer;
    }

    public record CreateOrderRequest(String orderId, long amount) { }

    @PostMapping
    public String create(@RequestBody CreateOrderRequest req) {
        OrderCreatedEvent event = OrderCreatedEvent.of(req.orderId(), req.amount());
        producer.publish(event);
        return "published eventId=" + event.eventId();
    }
}
