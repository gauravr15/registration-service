package com.odin.registrationservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic notificationTopic() {
        // name: notification-topic, partitions: 1, replication-factor: 1
        return new NewTopic("otp.notification.message", 1, (short) 1);
    }
}
