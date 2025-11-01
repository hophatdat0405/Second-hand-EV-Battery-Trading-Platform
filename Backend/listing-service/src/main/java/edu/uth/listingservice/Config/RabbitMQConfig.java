// File: edu/uth/listingservice/Config/RabbitMQConfig.java
package edu.uth.listingservice.Config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Lấy tên từ application.properties/.yml
    @Value("${app.rabbitmq.ai.exchange}")
    private String aiExchangeName;

    @Value("${app.rabbitmq.ai.queue}")
    private String aiQueueName;

    @Value("${app.rabbitmq.ai.routing-key}")
    private String aiRoutingKey;

    @Bean
    public Queue aiRequestQueue() {
        // Tên queue phải khớp với Python: "ai.price.request.queue"
        return new Queue(aiQueueName);
    }

    @Bean
    public DirectExchange aiExchange() {
        return new DirectExchange(aiExchangeName);
    }

    @Bean
    public Binding aiBinding() {
        return BindingBuilder.bind(aiRequestQueue())
                             .to(aiExchange())
                             .with(aiRoutingKey);
    }
    
    // Lưu ý: Chúng ta không cần khai báo reply-queue
    // Spring sẽ tự động tạo một reply-queue tạm thời cho mỗi request
}