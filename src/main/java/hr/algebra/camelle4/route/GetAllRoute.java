package hr.algebra.camelle4.route;

import hr.algebra.camelle4.config.AppConfig;
import hr.algebra.camelle4.processor.ResponseProcessor;
import hr.algebra.le5.s8894.avro.SensorReading;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.avro.AvroDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;

@Component
public class GetAllRoute extends RouteBuilder {

    private static final Random RNG = new Random();

    @Autowired private ResponseProcessor responseProcessor;
    @Autowired private MeterRegistry meterRegistry;

    @Override
    public void configure() {
        errorHandler(RouteSupport.defaultErrorHandler());


        AvroDataFormat avroFormat = new AvroDataFormat();
        avroFormat.setInstanceClassName("hr.algebra.le5.s8894.avro.SensorReading");

        from("timer:get-all-category?period=10000")
                .routeId("get-all-category")
                .log("START: Fetching all categories")
                .setHeader("CamelHttpMethod", constant("GET"))
                .to(AppConfig.BASE_URL + "?httpMethod=GET")
                .process(responseProcessor)
                .log("COMPLETED: Fetching all categories")
                .to("file:" + AppConfig.OUTPUT_DIR + "?fileName=get-all-category-${date:now:yyyyMMdd-HHmmss}.json")
                .split().jsonpathWriteAsString("$[*]")
                .process(exchange -> {
                    String el       = exchange.getIn().getBody(String.class);
                    String entityId = extractField(el, "id");
                    String name     = extractField(el, "name");

                    SensorReading reading = SensorReading.newBuilder()
                            .setDeviceId("sensor" + AppConfig.SID4 + "-" + entityId)
                            .setLocation("Zagreb")
                            .setTemperature(15.0 + RNG.nextDouble() * 20.0)
                            .setHumidity(40.0 + RNG.nextDouble() * 40.0)
                            .setTimestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                            .setCategoryName(name)
                            .build();

                    exchange.getIn().setBody(reading);
                })
                .to(AppConfig.DIRECT_PUBLISH_SENSOR)
                .end();

        from(AppConfig.DIRECT_PUBLISH_SENSOR)
                .routeId("kafka-sensors-publisher")
                .marshal(avroFormat)
                .to("kafka:" + AppConfig.KAFKA_TOPIC_SENSORS);

        from("kafka:" + AppConfig.KAFKA_TOPIC_SENSORS + "?groupId=" + AppConfig.KAFKA_GROUP_SENSORS)
                .routeId("kafka-sensors-consumer")
                .unmarshal(avroFormat)
                .process(exchange -> {
                    SensorReading r = exchange.getIn().getBody(SensorReading.class);
                    meterRegistry.counter(AppConfig.METRIC_SENSORS, "location", r.getLocation().toString()).increment();
                    log.info("Sensor: {} temp={} category={}", r.getDeviceId(), r.getTemperature(), r.getCategoryName());
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