package com.company.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaDlqConfig {

    /**
     * DLQ Consumer 전용 ErrorHandler
     * - 로그만 남기고 종료
     */
    @Bean
    public CommonErrorHandler dlqLoggingErrorHandler() {

        FixedBackOff noRetry = new FixedBackOff(0L, 0L);
        DefaultErrorHandler handler = new DefaultErrorHandler(noRetry);

        handler.setRetryListeners((record, ex, deliveryAttempt) -> {
            System.out.println("[DEBUG_LOG] DLQ error handler invoked: attempt=" + deliveryAttempt);
            System.out.println("[DEBUG_LOG] DLQ error details: topic=" + record.topic() 
                    + ", partition=" + record.partition() 
                    + ", offset=" + record.offset() 
                    + ", key=" + record.key());
            System.out.println("[DEBUG_LOG] DLQ exception: " + ex.getClass().getName() + ": " + ex.getMessage());
            System.out.println("[DEBUG_LOG] DLQ record: " + record);

            System.err.println(
                    "[DLQ ERROR] skipped permanently. record=" + record +
                            ", exception=" + ex.getMessage()
            );
        });

        return handler;
    }
}
