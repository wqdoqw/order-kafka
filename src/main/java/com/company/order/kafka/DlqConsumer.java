package com.company.order.kafka;

import com.company.order.domain.OrderCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DlqConsumer {

    private final ObjectMapper objectMapper;

    public DlqConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.order-created-dlq}",
            containerFactory = "dlqKafkaListenerContainerFactory"
    )
    public void onDlq(String message, Acknowledgment acknowledgment) {
        System.out.println("[DEBUG_LOG] DLQ received message: " + message);

        try {
            // JSON 문자열을 OrderCreatedEvent로 변환
            System.out.println("[DEBUG_LOG] Attempting to parse DLQ message into OrderCreatedEvent");
            OrderCreatedEvent event = parseOrderCreatedEvent(message);

            System.out.println("[DEBUG_LOG] DLQ successfully parsed message: eventId=" + event.eventId() 
                    + ", orderId=" + event.orderId() 
                    + ", amount=" + event.amount() 
                    + ", createdAt=" + event.createdAt());

            System.out.println("[DLQ] received eventId=" + event.eventId()
                    + ", orderId=" + event.orderId()
                    + ", amount=" + event.amount());

            // DLQ 메시지 처리 완료 후 ack
            System.out.println("[DEBUG_LOG] Acknowledging DLQ message for eventId=" + event.eventId());
            acknowledgment.acknowledge();
            System.out.println("[DEBUG_LOG] DLQ message acknowledged for eventId=" + event.eventId());
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Error processing DLQ message: " + e.getClass().getName() + ": " + e.getMessage());
            System.err.println("[DLQ] Error processing message: " + e.getMessage());
            System.err.println("[DLQ] Original message: " + message);
            // 에러가 발생해도 DLQ에서는 ack (더 이상 재시도하지 않음)
            System.out.println("[DEBUG_LOG] Acknowledging DLQ message despite error");
            acknowledgment.acknowledge();
            System.out.println("[DEBUG_LOG] DLQ message acknowledged after error");
        }
    }

    private OrderCreatedEvent parseOrderCreatedEvent(String json) throws JsonProcessingException {
        System.out.println("[DEBUG_LOG] Parsing JSON message: " + json);

        try {
            // 직접 ObjectMapper를 사용하여 JSON 파싱
            System.out.println("[DEBUG_LOG] Attempting to parse directly to OrderCreatedEvent");
            OrderCreatedEvent event = objectMapper.readValue(json, OrderCreatedEvent.class);
            System.out.println("[DEBUG_LOG] Successfully parsed to OrderCreatedEvent directly");
            return event;
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Failed to parse directly to OrderCreatedEvent: " + e.getMessage());
            System.out.println("[DEBUG_LOG] Attempting fallback parsing with DlqOrderCreatedEventDto");

            // JSON에 createdAt이 timestamp로 되어 있을 경우 수동 변환
            DlqOrderCreatedEventDto dto = objectMapper.readValue(json, DlqOrderCreatedEventDto.class);
            System.out.println("[DEBUG_LOG] Successfully parsed to DlqOrderCreatedEventDto: eventId=" + dto.eventId() 
                    + ", orderId=" + dto.orderId() 
                    + ", amount=" + dto.amount() 
                    + ", createdAt(timestamp)=" + dto.createdAt());

            OrderCreatedEvent event = new OrderCreatedEvent(
                    dto.eventId(),
                    dto.orderId(),
                    dto.amount(),
                    Instant.ofEpochMilli(dto.createdAt())
            );
            System.out.println("[DEBUG_LOG] Successfully converted DlqOrderCreatedEventDto to OrderCreatedEvent");
            return event;
        }
    }

    // DLQ에서 받은 메시지 형식에 맞는 DTO
    private record DlqOrderCreatedEventDto(
            String eventId,
            String orderId,
            long amount,
            long createdAt
    ) {}
}
