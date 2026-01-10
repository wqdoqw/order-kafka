package com.company.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic orderCreatedTopic(@Value("${app.kafka.topics.order-created}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderCreatedDlqTopic(@Value("${app.kafka.topics.order-created-dlq}") String dlqTopic) {
        return TopicBuilder.name(dlqTopic).partitions(3).replicas(1).build();
    }
}
