package com.company.order.config;

import com.company.order.domain.OrderCreatedEvent;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler errorHandler(
            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.order-created-dlq}") String dlqTopic
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(dlqTopic, record.partition())
        );

        FixedBackOff backOff = new FixedBackOff(1000L, 3L); // 1초 간격 3번 재시도 (총 4회)

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        handler.setRetryListeners((record, ex, deliveryAttempt) -> {
            System.out.println("[DEBUG_LOG] RETRY ATTEMPT #" + deliveryAttempt +
                    ", topic=" + record.topic() +
                    ", partition=" + record.partition() +
                    ", offset=" + record.offset() +
                    ", key=" + record.key() +
                    ", exception=" + ex.getClass().getSimpleName() +
                    ", message=" + ex.getMessage());

            // Log additional information about the retry process
            System.out.println("[DEBUG_LOG] Retry details: ConsumerRecord=" + record);
            System.out.println("[DEBUG_LOG] Exception stack trace: " + ex);
        });

        return handler;
    }
}
