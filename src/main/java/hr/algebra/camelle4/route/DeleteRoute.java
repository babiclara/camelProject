package hr.algebra.camelle4.route;

import hr.algebra.camelle4.config.AppConfig;
import hr.algebra.camelle4.processor.ResponseProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Route 5 — Delete a Category (DELETE).
 * HTTP Method: POST then DELETE (chained)
 * Endpoint: http://localhost:8080/api/categories
 * Timer Period: 60 seconds
 * Advanced Logic: Creates a temporary entity via POST, extracts the ID,
 *                 then deletes it via DELETE — two chained HTTP calls.
 * Output: output/responses/delete-category-idN-TIMESTAMP.json
 */
@Component
public class DeleteRoute extends RouteBuilder {

    @Autowired
    private ResponseProcessor responseProcessor;

    @Override
    public void configure() {
        onException(Exception.class)
                .handled(true)
                .log("ERROR on delete-category: ${exception.message}")
                .setBody(simple("{\"error\": \"${exception.message}\"}"))
                .to("file:" + AppConfig.OUTPUT_DIR + "?fileName=delete-category-error-${date:now:yyyyMMdd-HHmmss}.json");

        from("timer:delete-category?period=60000")
                .routeId("delete-category")
                .log("START: Create-then-delete category")

                .setBody(constant("{\"name\": \"Temporary\", \"description\": \"To be deleted\"}"))
                .setHeader("CamelHttpMethod", constant("POST"))
                .setHeader("Content-Type", constant("application/json"))
                .to(AppConfig.BASE_URL + "?httpMethod=POST")
                .log("Created temporary category for deletion")

                .process(exchange -> {
                    String responseBody = exchange.getIn().getBody(String.class);
                    int idStart = responseBody.indexOf("\"id\":") + 5;
                    int idEnd = responseBody.indexOf(",", idStart);
                    if (idEnd == -1) idEnd = responseBody.indexOf("}", idStart);
                    String idStr = responseBody.substring(idStart, idEnd).trim();
                    Long id = Long.parseLong(idStr);
                    exchange.getIn().setHeader("deleteId", id);
                })

                .setBody(constant(null))
                .setHeader("CamelHttpMethod", constant("DELETE"))
                .setHeader("EndpointURL", simple(AppConfig.BASE_URL + "/${header.deleteId}"))
                .toD(AppConfig.BASE_URL + "/${header.deleteId}?httpMethod=DELETE")
                .log("Deleted category with ID ${header.deleteId}")

                .process(responseProcessor)
                .log("COMPLETED: Create-then-delete category")
                .toD("file:" + AppConfig.OUTPUT_DIR + "?fileName=delete-category-id${header.deleteId}-${date:now:yyyyMMdd-HHmmss}.json");
    }
}