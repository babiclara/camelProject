package hr.algebra.camelle4.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    @Bean
    public DirectExchange ordersExchange() {
        return ExchangeBuilder.directExchange(AppConfig.RABBIT_EXCHANGE).durable(true).build();
    }

    @Bean("dlxExchange")
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(AppConfig.RABBIT_DLX).durable(true).build();
    }

    @Bean
    public Queue ordersQueue() {
        return QueueBuilder.durable(AppConfig.RABBIT_QUEUE)
                .withArgument("x-dead-letter-exchange", AppConfig.RABBIT_DLX)
                .withArgument("x-dead-letter-routing-key", "dlq.order")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(AppConfig.RABBIT_DLQ).build();
    }

    @Bean
    public Binding ordersBinding(Queue ordersQueue, DirectExchange ordersExchange) {
        return BindingBuilder.bind(ordersQueue).to(ordersExchange).with(AppConfig.RABBIT_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, @Qualifier("dlxExchange") DirectExchange dlxExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(dlxExchange).with("dlq.order");
    }
}