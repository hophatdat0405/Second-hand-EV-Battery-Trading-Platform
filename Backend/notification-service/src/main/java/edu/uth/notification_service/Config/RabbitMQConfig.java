package edu.uth.notification_service.Config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;
    @Value("${app.rabbitmq.queue}")
    private String queueName;
    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    @Bean
    public Queue notificationQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public TopicExchange listingExchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(listingExchange())
                .with(routingKey);
    }

    /**
     * SỬA LỖI: Cấu hình MessageConverter
     * Chúng ta map ID "ListingEventDTO" (nhận từ 8080)
     * VÀ TÊN CLASS ĐẦY ĐỦ (edu.uth.listingservice.DTO.ListingEventDTO)
     * với Class tương ứng CỦA SERVICE 8085 NÀY.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(new ObjectMapper());

        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();

        // 1. Map ID ngắn "ListingEventDTO" (trường hợp lý tưởng)
        idClassMapping.put("ListingEventDTO", edu.uth.notification_service.DTO.ListingEventDTO.class);
        
        // 2. SỬA LỖI: Map tên class đầy đủ từ producer (trường hợp thực tế đang xảy ra)
        //    với class DTO của service này.
        idClassMapping.put("edu.uth.listingservice.DTO.ListingEventDTO", edu.uth.notification_service.DTO.ListingEventDTO.class);
        
        // Bạn cũng nên làm tương tự cho các DTO khác nếu có, ví dụ:
        // idClassMapping.put("edu.uth.listingservice.DTO.PricingRequestDTO", edu.uth.notification_service.DTO.PricingRequestDTO.class);


        classMapper.setIdClassMapping(idClassMapping);
        converter.setClassMapper(classMapper);

        return converter;
    }
}