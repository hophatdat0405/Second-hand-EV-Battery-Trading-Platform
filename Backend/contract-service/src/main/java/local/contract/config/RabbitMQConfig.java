package local.contract.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${mq.exchange:ev.exchange}") 
    private String exchangeName;

    @Value("${mq.queue.order-paid:order.paid.queue}")
    private String orderPaidQueueName;

    @Value("${mq.routing.order-paid:order.paid}")
    private String orderPaidRoutingKey;

    // ✅ Dùng DirectExchange (trùng với transaction-service)
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    // ✅ Khai báo Queue
    @Bean
    public Queue orderPaidQueue() {
        return new Queue(orderPaidQueueName, true);
    }

    // ✅ Binding queue với exchange theo routing key
    @Bean
    public Binding bindingOrderPaid(Queue orderPaidQueue, DirectExchange exchange) {
        return BindingBuilder
                .bind(orderPaidQueue)
                .to(exchange)
                .with(orderPaidRoutingKey);
    }

    // ✅ Dùng JSON converter để tránh lỗi CollSer
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
