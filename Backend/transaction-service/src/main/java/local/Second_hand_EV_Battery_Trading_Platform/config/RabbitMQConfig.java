package local.Second_hand_EV_Battery_Trading_Platform.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ✅ Đổi sang DirectExchange để trùng với MQPublisher
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange("ev.exchange", true, false);
    }

    @Bean
    public Queue orderPaidQueue() {
        return new Queue("order.paid.queue", true);
    }

    @Bean
    public Queue orderFailedQueue() {
        return new Queue("order.failed.queue", true);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue("order.created.queue", true);
    }

    @Bean
    public Binding bindingPaid(Queue orderPaidQueue, DirectExchange exchange) {
        return BindingBuilder.bind(orderPaidQueue).to(exchange).with("order.paid");
    }

    @Bean
    public Binding bindingFailed(Queue orderFailedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(orderFailedQueue).to(exchange).with("order.failed");
    }

    @Bean
    public Binding bindingCreated(Queue orderCreatedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(exchange).with("order.created");
    }
}
