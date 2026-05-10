package hr.algebra.camelle4.route;

import hr.algebra.camelle4.config.AppConfig;
import hr.algebra.camelle4.processor.ResponseProcessor;
import hr.algebra.le5.s8894.proto.PaymentProto;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.protobuf.ProtobufDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateRoute extends RouteBuilder {

    @Autowired private ResponseProcessor responseProcessor;
    @Autowired private MeterRegistry meterRegistry;

    private final String[] payloads = {
            "{\"name\": \"Images\",    \"description\": \"Image files such as PNG and JPEG\"}",
            "{\"name\": \"Documents\", \"description\": \"Office documents such as DOCX and PDF\"}",
            "{\"name\": \"Videos\",    \"description\": \"Video files such as MP4 and AVI\"}"
    };
    private final String[] currencies = {"EUR", "USD", "HRK"};
    private int index = 0;

    @Override
    public void configure() {
        errorHandler(RouteSupport.defaultErrorHandler());

        ProtobufDataFormat protoFormat = new ProtobufDataFormat(
                PaymentProto.PaymentEvent.getDefaultInstance());

        from("timer:create-category?period=30000")
                .routeId("create-category")
                .log("START: Creating new category")
                .process(exchange -> {
                    String payload  = payloads[index % payloads.length];
                    String currency = currencies[index % currencies.length];
                    String desc     = extractField(payload, "description");
                    exchange.getIn().setBody(payload);
                    exchange.getIn().setHeader(AppConfig.HEADER_CURRENCY, currency);
                    exchange.getIn().setHeader("categoryDescription", desc);
                    index++;
                })
                .setHeader("CamelHttpMethod", constant("POST"))
                .setHeader("Content-Type",   constant("application/json"))
                .to(AppConfig.BASE_URL + "?httpMethod=POST")
                .process(responseProcessor)
                .log("COMPLETED: Created category")
                .to("file:" + AppConfig.OUTPUT_DIR + "?fileName=create-category-${date:now:yyyyMMdd-HHmmss}.json")
                .process(exchange -> {
                    String body     = exchange.getIn().getBody(String.class);
                    String entityId = extractField(body, "id");
                    String currency = exchange.getIn().getHeader(AppConfig.HEADER_CURRENCY, String.class);
                    String desc     = exchange.getIn().getHeader("categoryDescription", String.class);

                    PaymentProto.PaymentEvent payment = PaymentProto.PaymentEvent.newBuilder()
                            .setPaymentId(UUID.randomUUID().toString())
                            .setFromAccount("HR12-" + AppConfig.SID4)
                            .setToAccount("HR98-" + entityId)
                            .setAmount("19.99")
                            .setCurrency(currency)
                            .setTimestamp(System.currentTimeMillis())
                            .setStatus(PaymentProto.PaymentStatus.PENDING)
                            .setCategoryDescription(desc)
                            .build();

                    exchange.getIn().setBody(payment);
                })
                .to(AppConfig.DIRECT_PUBLISH_PAYMENT);

        from(AppConfig.DIRECT_PUBLISH_PAYMENT)
                .routeId("kafka-payments-publisher")
                .marshal(protoFormat)
                .to("kafka:" + AppConfig.KAFKA_TOPIC_PAYMENTS);

        from("kafka:" + AppConfig.KAFKA_TOPIC_PAYMENTS + "?groupId=" + AppConfig.KAFKA_GROUP_PAYMENTS)
                .routeId("kafka-payments-consumer")
                .unmarshal(protoFormat)
                .process(exchange -> {
                    PaymentProto.PaymentEvent p = exchange.getIn().getBody(PaymentProto.PaymentEvent.class);
                    meterRegistry.counter(AppConfig.METRIC_PAYMENTS, AppConfig.HEADER_CURRENCY, p.getCurrency()).increment();
                    log.info("Payment processed: {} {} {}", p.getPaymentId(), p.getAmount(), p.getCurrency());
                });
    }

    private String extractField(String json, String field) {
        String key = "\"" + field + "\":";
        int start = json.indexOf(key);
        if (start < 0) return "unknown";
        start += key.length();
        if (json.charAt(start) == '"') {
            start++;
            return json.substring(start, json.indexOf("\"", start));
        }
        int end = json.indexOf(",", start);
        if (end < 0) end = json.indexOf("}", start);
        return json.substring(start, end).trim();
    }
}