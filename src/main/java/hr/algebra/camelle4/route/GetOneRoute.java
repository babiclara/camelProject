package hr.algebra.camelle4.route;

import hr.algebra.camelle4.config.AppConfig;
import hr.algebra.camelle4.model.OrderEvent;
import hr.algebra.camelle4.processor.ResponseProcessor;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.crypto.CryptoDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class GetOneRoute extends RouteBuilder {

    @Autowired private ResponseProcessor responseProcessor;
    @Autowired private MeterRegistry meterRegistry;
    @Autowired private CryptoDataFormat orderCryptoDataFormat;


    private final Long[] ids = {3L, 33L, 99L};
    private int index = 0;

    @Override
    public void configure() {
        errorHandler(RouteSupport.defaultErrorHandler());

        from("timer:get-one-category?period=15000")
                .routeId("get-one-category")
                .log("START: Fetching single category")
                .process(exchange -> {
                    Long id = ids[index % ids.length];
                    exchange.getIn().setHeader("currentId", id);
                    index++;
                })
                .setHeader("CamelHttpMethod", constant("GET"))
                .toD(AppConfig.BASE_URL + "/${header.currentId}?httpMethod=GET")
                .process(responseProcessor)
                .log("COMPLETED: Fetched category ${header.currentId}")
                .toD("file:" + AppConfig.OUTPUT_DIR + "?fileName=get-one-category-id${header.currentId}-${date:now:yyyyMMdd-HHmmss}.json")
                .process(exchange -> {
                    String body = exchange.getIn().getBody(String.class);
                    String name = extractField(body, "name");
                    OrderEvent order = new OrderEvent(
                            UUID.randomUUID().toString(), name, 19.99, "EUR", Instant.now());
                    exchange.getIn().setBody(order);
                })
                .to(AppConfig.DIRECT_PUBLISH_ORDER);

        from(AppConfig.DIRECT_PUBLISH_ORDER)
                .routeId("rabbit-orders-publisher")
                .marshal().json()
                .marshal(orderCryptoDataFormat)
                .log("Publishing ENCRYPTED order to RabbitMQ")
                .to("spring-rabbitmq:" + AppConfig.RABBIT_EXCHANGE
                        + "?routingKey=" + AppConfig.RABBIT_ROUTING_KEY
                        + "&autoDeclare=false");

//        from("spring-rabbitmq:" + AppConfig.RABBIT_EXCHANGE
//                + "?queues=" + AppConfig.RABBIT_QUEUE
//                + "&autoDeclare=false")
//                .routeId("rabbit-orders-consumer")
//                .unmarshal(orderCryptoDataFormat)
//                .unmarshal().json(OrderEvent.class)
//                .process(exchange -> {
//                    meterRegistry.counter(AppConfig.METRIC_ORDERS).increment();
//                    OrderEvent o = exchange.getIn().getBody(OrderEvent.class);
//                    log.info("Order processed: {} - {}", o.getOrderId(), o.getCustomer());
//                });
    }

    private String extractField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) return "unknown";
        start += key.length();
        return json.substring(start, json.indexOf("\"", start));
    }
}