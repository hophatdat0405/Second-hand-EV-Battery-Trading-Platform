// File: edu/uth/example/review_service/Config/RabbitMQConfig.java
package edu.uth.example.review_service.Config;

// --- THÊM CÁC IMPORT NÀY ---
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // (Bean 'reviewExchange' cũ của bạn giữ nguyên)
    @Value("${app.rabbitmq.review-events.exchange}")
    private String reviewExchangeName;
    @Bean
    public Exchange reviewExchange() {
        return new TopicExchange(reviewExchangeName);
    }

    // --- THÊM CÁC BEAN MỚI ĐỂ LẮNG NGHE USER ---
    
    @Value("${app.rabbitmq.user.exchange}")
    private String userExchangeName;

    @Value("${app.rabbitmq.user.queue.listener}")
    private String userQueueName;

    @Value("${app.rabbitmq.user.routing-key}")
    private String userRoutingKey;

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(userExchangeName);
    }

    @Bean
    public Queue userSyncQueue() {
        return new Queue(userQueueName, true); // durable = true
    }

    @Bean
    public Binding userSyncBinding(Queue userSyncQueue, TopicExchange userExchange) {
        return BindingBuilder
                .bind(userSyncQueue)
                .to(userExchange)
                .with(userRoutingKey);
    }
    // --- KẾT THÚC THÊM BEAN MỚI ---


    // (Các Bean Converter và RabbitTemplate cũ giữ nguyên)
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter);
        return rabbitTemplate;
    }
}