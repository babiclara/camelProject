package hr.algebra.camelle4.config;

import org.apache.camel.component.springrabbit.SpringRabbitMQComponent;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.spi.IdempotentRepository;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelConfig {

    @Bean("spring-rabbitmq")
    public SpringRabbitMQComponent springRabbitMQComponent(ConnectionFactory connectionFactory) {
        SpringRabbitMQComponent component = new SpringRabbitMQComponent();
        component.setConnectionFactory(connectionFactory);
        return component;
    }

    @Bean
    public IdempotentRepository paymentIdempotentRepository() {
        return new MemoryIdempotentRepository();
    }
}