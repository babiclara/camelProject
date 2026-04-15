package hr.algebra.camelle4.route;

import hr.algebra.camelle4.config.AppConfig;
import hr.algebra.camelle4.processor.ResponseProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Route 4 — Update an Existing Category (PUT).
 * HTTP Method: PUT
 * Endpoint: http://localhost:8080/api/categories/{id}
 * Timer Period: 45 seconds
 * Request Body: 3 different (id, payload) pairs in round-robin rotation
 * Output: output/responses/update-category-idN-TIMESTAMP.json
 */

@Component
public class UpdateRoute extends RouteBuilder {

    @Autowired
    private ResponseProcessor responseProcessor;

    private final Long[] ids = {3L, 33L, 99L};
    private final String[] payloads = {
            "{\"name\": \"Images Updated\", \"description\": \"Updated image category\"}",
            "{\"name\": \"Documents Updated\", \"description\": \"Updated document category\"}",
            "{\"name\": \"Videos Updated\", \"description\": \"Updated video category\"}"
    };
    private int index = 0;

    @Override
    public void configure() {
        onException(Exception.class)
                .handled(true)
                .log("ERROR on update-category: ${exception.message}")
                .setBody(simple("{\"error\": \"${exception.message}\"}"))
                .to("file:" + AppConfig.OUTPUT_DIR + "?fileName=update-category-error-${date:now:yyyyMMdd-HHmmss}.json");

        from("timer:update-category?period=45000")
                .routeId("update-category")
                .log("START: Updating category")
                .process(exchange -> {
                    int i = index % ids.length;
                    Long id = ids[i];
                    String payload = payloads[i];
                    exchange.getIn().setBody(payload);
                    exchange.getIn().setHeader("currentId", id);
                    index++;
                })
                .setHeader("CamelHttpMethod", constant("PUT"))
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("EndpointURL", simple(AppConfig.BASE_URL + "/${header.currentId}"))
                .toD(AppConfig.BASE_URL + "/${header.currentId}?httpMethod=PUT")
                .process(responseProcessor)
                .log("COMPLETED: Updated category with ID ${header.currentId}")
                .toD("file:" + AppConfig.OUTPUT_DIR + "?fileName=update-category-id${header.currentId}-${date:now:yyyyMMdd-HHmmss}.json");
    }
}