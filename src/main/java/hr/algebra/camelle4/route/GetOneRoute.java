package hr.algebra.camelle4.route;

import hr.algebra.camelle4.config.AppConfig;
import hr.algebra.camelle4.processor.ResponseProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Route 2 — Retrieve a Single Category (GET ONE).
 * HTTP Method: GET
 * Endpoint: http://localhost:8080/api/categories/{id}
 * Timer Period: 15 seconds
 * Dynamic ID: Cycles through IDs 3, 33, 99
 * Output: output/responses/get-one-category-idN-TIMESTAMP.json
 */

@Component
public class GetOneRoute extends RouteBuilder {

    @Autowired
    private ResponseProcessor responseProcessor;

    private final Long[] ids = {3L, 33L, 99L};
    private int index = 0;

    @Override
    public void configure() {
        onException(Exception.class)
                .handled(true)
                .log("ERROR on get-one-category: ${exception.message}")
                .setBody(simple("{\"error\": \"${exception.message}\"}"))
                .to("file:" + AppConfig.OUTPUT_DIR + "?fileName=get-one-category-error-${date:now:yyyyMMdd-HHmmss}.json");

        from("timer:get-one-category?period=15000")
                .routeId("get-one-category")
                .log("START: Fetching single category")
                .process(exchange -> {
                    Long id = ids[index % ids.length];
                    exchange.getIn().setHeader("currentId", id);
                    index++;
                })
                .setHeader("CamelHttpMethod", constant("GET"))
                .setHeader("EndpointURL", simple(AppConfig.BASE_URL + "/${header.currentId}"))
                .toD(AppConfig.BASE_URL + "/${header.currentId}?httpMethod=GET")
                .process(responseProcessor)
                .log("COMPLETED: Fetched category with ID ${header.currentId}")
                .toD("file:" + AppConfig.OUTPUT_DIR + "?fileName=get-one-category-id${header.currentId}-${date:now:yyyyMMdd-HHmmss}.json");
    }
}