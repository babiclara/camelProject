package hr.algebra.camelle4.route;

import hr.algebra.camelle4.config.AppConfig;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class DeadLetterRoute extends RouteBuilder {

    @Override
    public void configure() {
        from(AppConfig.DIRECT_GLOBAL_DLQ)
                .routeId("dead-letter-route")
                .log("DEAD LETTER: exhausted from ${routeId}")
                .setHeader(AppConfig.HEADER_SOURCE, simple("${routeId}"))
                .setBody(simple("DLQ | source=${header.source} | error=${exception.message} | body=${body}"))
                .to("kafka:" + AppConfig.KAFKA_TOPIC_DLQ);
    }
}