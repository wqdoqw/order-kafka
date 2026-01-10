package com.company.order.kafka;

import com.company.order.domain.OrderCreatedEvent;
import com.company.order.domain.ProcessedEvent;
import com.company.order.domain.ProcessedEventRepository;
import com.company.order.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OrderEventConsumer {

    private final ProcessedEventRepository processedEventRepository;
    private final OrderService orderService;

    public OrderEventConsumer(ProcessedEventRepository processedEventRepository, OrderService orderService) {
        this.processedEventRepository = processedEventRepository;
        this.orderService = orderService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.order-created}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(
            OrderCreatedEvent event,
            @Header(name = KafkaHeaders.DELIVERY_ATTEMPT, required = false) Integer attempt,
            Acknowledgment acknowledgment
    ) {
        System.out.println("[DEBUG_LOG] Consumer received message: attempt=" + attempt + ", eventId=" + event.eventId() + ", orderId=" + event.orderId() + ", amount=" + event.amount());

        try {
            if (processedEventRepository.existsById(event.eventId())) {
                System.out.println("[DEBUG_LOG] Duplicate event detected and ignored. eventId=" + event.eventId());
                System.out.println("[Consumer] duplicated event ignored. eventId=" + event.eventId());
                acknowledgment.acknowledge(); // 중복 메시지도 처리 완료로 표시
                System.out.println("[DEBUG_LOG] Message acknowledged for duplicate event: " + event.eventId());
                return;
            }

            System.out.println("[DEBUG_LOG] Processing message with OrderService: eventId=" + event.eventId());
            // 여기서 999면 예외 발생 -> retry -> dlq
            try {
                orderService.handle(event);
                System.out.println("[DEBUG_LOG] OrderService successfully processed event: " + event.eventId());
            } catch (Exception e) {
                System.out.println("[DEBUG_LOG] OrderService failed to process event: " + event.eventId() + ", error: " + e.getMessage());
                throw e; // Re-throw to trigger retry
            }

            processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
            System.out.println("[DEBUG_LOG] Event marked as processed in repository: " + event.eventId());
            System.out.println("[Consumer] success eventId=" + event.eventId());

            // 성공적으로 처리된 경우에만 ack
            System.out.println("[DEBUG_LOG] Acknowledging message after successful processing: " + event.eventId());
            acknowledgment.acknowledge();
            System.out.println("[DEBUG_LOG] Message successfully acknowledged: " + event.eventId());
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Exception in consumer processing: " + e.getClass().getName() + ": " + e.getMessage());
            System.out.println("[DEBUG_LOG] Will rethrow exception to trigger error handler and retry mechanism");
            // 예외는 ErrorHandler에서 처리하도록 다시 throw
            throw e;
        }
    }
}
