package hr.algebra.camelle4.route;

import hr.algebra.camelle4.config.AppConfig;
import hr.algebra.camelle4.processor.ResponseProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Route 3 — Create a New Category (POST).
 * HTTP Method: POST
 * Endpoint: http://localhost:8080/api/categories
 * Timer Period: 30 seconds
 * Request Body: 3 different JSON payloads in round-robin rotation
 * Output: output/responses/create-category-TIMESTAMP.json
 */

@Component
public class CreateRoute extends RouteBuilder {

    @Autowired
    private ResponseProcessor responseProcessor;

    private final String[] payloads = {
            "{\"name\": \"Images\", \"description\": \"Image files such as PNG and JPEG\"}",
            "{\"name\": \"Documents\", \"description\": \"Office documents such as DOCX and PDF\"}",
            "{\"name\": \"Videos\", \"description\": \"Video files such as MP4 and AVI\"}"
    };
    private int index = 0;

    @Override
    public void configure() {
        onException(Exception.class)
                .handled(true)
                .log("ERROR on create-category: ${exception.message}")
                .setBody(simple("{\"error\": \"${exception.message}\"}"))
                .to("file:" + AppConfig.OUTPUT_DIR + "?fileName=create-category-error-${date:now:yyyyMMdd-HHmmss}.json");

        from("timer:create-category?period=30000")
                .routeId("create-category")
                .log("START: Creating new category")
                .process(exchange -> {
                    String payload = payloads[index % payloads.length];
                    exchange.getIn().setBody(payload);
                    index++;
                })
                .setHeader("CamelHttpMethod", constant("POST"))
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("EndpointURL", constant(AppConfig.BASE_URL))
                .to(AppConfig.BASE_URL + "?httpMethod=POST")
                .process(responseProcessor)
                .log("COMPLETED: Created new category")
                .to("file:" + AppConfig.OUTPUT_DIR + "?fileName=create-category-${date:now:yyyyMMdd-HHmmss}.json");
    }
}