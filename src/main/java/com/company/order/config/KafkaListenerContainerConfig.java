package com.company.order.config;

import com.company.order.domain.OrderCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaListenerContainerConfig {

    // 메인 토픽용(재시도 + DLQ 전송)
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, OrderCreatedEvent> consumerFactory,
            CommonErrorHandler errorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);

        // attempt 헤더 활성화
        factory.getContainerProperties().setDeliveryAttemptHeader(true);

        // 오프셋 자동 커밋 비활성화 및 수동 ack 모드 설정
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }

    // DLQ 토픽용(에러핸들러 없음)
    @Bean(name = "dlqKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> dlqKafkaListenerContainerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId,
            CommonErrorHandler dlqLoggingErrorHandler
    ) {
        // DLQ용 ConsumerFactory는 String으로 받음
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        // String 타입으로 메시지를 받음
        ConsumerFactory<String, String> stringConsumerFactory = 
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer());

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringConsumerFactory);
        factory.setCommonErrorHandler(dlqLoggingErrorHandler);

        factory.getContainerProperties().setDeliveryAttemptHeader(true);

        // DLQ 리스너도 수동 ack 모드 설정
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
