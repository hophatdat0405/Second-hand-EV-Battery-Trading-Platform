package edu.uth.listingservice.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.context.annotation.Primary; 
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// THÊM 2 IMPORT NÀY
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // ... (Các @Value của bạn) ...
    @Value("${app.rabbitmq.exchange}")
    private String listingEventsExchange;
    @Value("${app.rabbitmq.ai.exchange}")
    private String aiExchange;
    @Value("${app.rabbitmq.ai.queue}")
    private String aiRequestQueue;
    @Value("${app.rabbitmq.ai.routing-key}")
    private String aiRoutingKey; 
    @Value("${app.rabbitmq.user.exchange}") // Tên Exchange của User Service (từ application.properties)
    private String userEventsExchange;

    @Value("${app.rabbitmq.user.queue.listener}") // Queue mà Listing Service sẽ lắng nghe
    private String userEventsQueue;
    /**
     * ✅ SỬA LỖI: Cấu hình MessageConverter
     * Chúng ta map các Class sang một ID đơn giản (ví dụ: "ListingEventDTO").
     * Khi gửi, nó sẽ đính kèm __type__: "ListingEventDTO"
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(new ObjectMapper());
        
        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        
        // Map Class của service 8080 với một ID
        idClassMapping.put("ListingEventDTO", edu.uth.listingservice.DTO.ListingEventDTO.class);
        idClassMapping.put("PricingRequestDTO", edu.uth.listingservice.DTO.PricingRequestDTO.class);
        
        classMapper.setIdClassMapping(idClassMapping);
        converter.setClassMapper(classMapper);
        
        return converter;
    }

    /**
     * Cấu hình RabbitTemplate để SỬ DỤNG JSON
     */
    @Bean
    @Primary 
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter); 
        return rabbitTemplate;
    }

    // ... (Các @Bean Exchange, Queue, Binding của bạn giữ nguyên) ...
    @Bean
    public TopicExchange listingEventsExchangeName() {
        return new TopicExchange(listingEventsExchange);
    }
    
    @Bean
   public TopicExchange aiExchangeName() { // <-- Thay đổi ở đây
    return new TopicExchange(aiExchange);   // <-- Và ở đây
    }

    @Bean
    public Queue aiRequestQueueName() {
        return new Queue(aiRequestQueue, true); 
    }

    @Bean
    public Binding aiRequestBinding() {
        return BindingBuilder
                .bind(aiRequestQueueName())
                .to(aiExchangeName())
                .with(aiRoutingKey);
    }
    @Bean
    public Queue userEventsQueue() {
        return new Queue(userEventsQueue, true); 
    }

    /**
     * Lấy Exchange của User Service (đã định nghĩa ở User Service)
     */
    @Bean
    public TopicExchange userEventsExchangeName() {
        // Sử dụng lại exchangeName từ User Service: user.events.exchange
        return new TopicExchange(userEventsExchange); 
    }

    /**
     * Binding Queue của Listing Service vào Exchange của User Service
     * với Routing Key là "user.event.#"
     */
    @Bean
    public Binding userEventsBinding() {
        // Lắng nghe tất cả các sự kiện user (CREATED, UPDATED)
        return BindingBuilder
                .bind(userEventsQueue())
                .to(userEventsExchangeName())
                .with("user.event.#"); // Lắng nghe routing key: user.event.created, user.event.updated
    }
}