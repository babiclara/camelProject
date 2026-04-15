package hr.algebra.camelle4.route;

import hr.algebra.camelle4.config.AppConfig;
import hr.algebra.camelle4.processor.ResponseProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Route 1 — Retrieve All Categories (GET ALL).
 * HTTP Method: GET
 * Endpoint: http://localhost:8080/api/categories
 * Timer Period: 10 seconds
 * Output: output/responses/get-all-category-TIMESTAMP.json
 */

@Component
public class GetAllRoute extends RouteBuilder {

    @Autowired
    private ResponseProcessor responseProcessor;

    @Override
    public void configure() {
        onException(Exception.class)
                .handled(true)
                .log("ERROR on get-all-category: ${exception.message}")
                .setBody(simple("{\"error\": \"${exception.message}\"}"))
                .to("file:" + AppConfig.OUTPUT_DIR + "?fileName=get-all-category-error-${date:now:yyyyMMdd-HHmmss}.json");

        from("timer:get-all-category?period=10000")
                .routeId("get-all-category")
                .log("START: Fetching all categories")
                .setHeader("CamelHttpMethod", constant("GET"))
                .setHeader("EndpointURL", constant(AppConfig.BASE_URL))
                .to(AppConfig.BASE_URL + "?httpMethod=GET")
                .process(responseProcessor)
                .log("COMPLETED: Fetching all categories")
                .to("file:" + AppConfig.OUTPUT_DIR + "?fileName=get-all-category-${date:now:yyyyMMdd-HHmmss}.json");
    }
}