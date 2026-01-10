package com.company.order.kafka;

import com.company.order.domain.OrderCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final String topic;

    public OrderEventProducer(KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate,
                              @Value("${app.kafka.topics.order-created}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(OrderCreatedEvent event) {
        kafkaTemplate.send(topic, event.orderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.out.println("[Producer] failed: " + ex.getMessage());
                    } else {
                        System.out.println("[Producer] sent topic=" + result.getRecordMetadata().topic()
                                + ", partition=" + result.getRecordMetadata().partition()
                                + ", offset=" + result.getRecordMetadata().offset()
                                + ", key=" + event.orderId()
                                + ", eventId=" + event.eventId());
                    }
                });
    }
}
