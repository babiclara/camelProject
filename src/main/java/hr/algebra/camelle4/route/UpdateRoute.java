package hr.algebra.camelle4.route;

import hr.algebra.camelle4.config.AppConfig;
import hr.algebra.camelle4.processor.ResponseProcessor;
import hr.algebra.le5.s8894.proto.PaymentProto;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.protobuf.ProtobufDataFormat;
import org.apache.camel.spi.IdempotentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UpdateRoute extends RouteBuilder {

    @Autowired private ResponseProcessor responseProcessor;
    @Autowired private MeterRegistry meterRegistry;
    @Autowired private IdempotentRepository paymentIdempotentRepository;

    private final Long[]   ids      = {3L, 33L, 99L};
    private final String[] payloads = {
            "{\"name\": \"Images Updated\",    \"description\": \"Updated image category\"}",
            "{\"name\": \"Documents Updated\", \"description\": \"Updated document category\"}",
            "{\"name\": \"Videos Updated\",    \"description\": \"Updated video category\"}"
    };
    private int index = 0;

    @Override
    public void configure() {
        errorHandler(RouteSupport.defaultErrorHandler());

        ProtobufDataFormat protoFormat = new ProtobufDataFormat(
                PaymentProto.PaymentEvent.getDefaultInstance());

        from("timer:update-category?period=45000")
                .routeId("update-category")
                .log("START: Updating category")
                .process(exchange -> {
                    int i = index % ids.length;
                    exchange.getIn().setBody(payloads[i]);
                    exchange.getIn().setHeader("currentId", ids[i]);
                    index++;
                })
                .setHeader("CamelHttpMethod", constant("PUT"))
                .setHeader("Content-Type",   constant("application/json"))
                .toD(AppConfig.BASE_URL + "/${header.currentId}?httpMethod=PUT")
                .process(responseProcessor)
                .log("COMPLETED: Updated category ${header.currentId}")
                .toD("file:" + AppConfig.OUTPUT_DIR + "?fileName=update-category-id${header.currentId}-${date:now:yyyyMMdd-HHmmss}.json")
                .process(exchange -> {
                    String sharedId = UUID.randomUUID().toString();
                    exchange.getIn().setHeader(AppConfig.HEADER_PAYMENT_ID, sharedId);

                    PaymentProto.PaymentEvent payment = PaymentProto.PaymentEvent.newBuilder()
                            .setPaymentId(sharedId)
                            .setFromAccount("HR12-" + AppConfig.SID4)
                            .setToAccount("HR98-" + exchange.getIn().getHeader("currentId"))
                            .setAmount("9.99")
                            .setCurrency("EUR")
                            .setTimestamp(System.currentTimeMillis())
                            .setStatus(PaymentProto.PaymentStatus.PENDING)
                            .setCategoryDescription("Duplicate payment test")
                            .build();

                    exchange.getIn().setBody(payment);
                })
                .to(AppConfig.DIRECT_PUBLISH_PAYMENT)
                .to(AppConfig.DIRECT_PUBLISH_PAYMENT);

        from("kafka:" + AppConfig.KAFKA_TOPIC_PAYMENTS + "?groupId=" + AppConfig.KAFKA_GROUP_PAYMENTS + "-idempotent")
                .routeId("kafka-payments-idempotent-consumer")
                .unmarshal(protoFormat)
                .process(exchange -> {
                    PaymentProto.PaymentEvent p = exchange.getIn().getBody(PaymentProto.PaymentEvent.class);
                    exchange.getIn().setHeader(AppConfig.HEADER_PAYMENT_ID, p.getPaymentId());
                })
                .idempotentConsumer(header(AppConfig.HEADER_PAYMENT_ID), paymentIdempotentRepository)
                .skipDuplicate(false)
                .choice()
                .when(exchangeProperty(Exchange.DUPLICATE_MESSAGE).isEqualTo(true))
                .process(exchange -> {
                    meterRegistry.counter(AppConfig.METRIC_DUPLICATES).increment();
                    log.warn("Duplicate payment dropped: {}", exchange.getIn().getHeader(AppConfig.HEADER_PAYMENT_ID));
                })
                .otherwise()
                .process(exchange -> {
                    meterRegistry.counter(AppConfig.METRIC_PAYMENTS, AppConfig.HEADER_CURRENCY, "EUR").increment();
                    log.info("Payment processed: {}", exchange.getIn().getHeader(AppConfig.HEADER_PAYMENT_ID));
                })
                .endChoice();
    }
}