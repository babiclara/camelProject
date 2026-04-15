package hr.algebra.camelle4.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ResponseProcessor implements Processor {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        Integer httpStatus = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        String routeId = exchange.getFromRouteId();
        String httpMethod = exchange.getIn().getHeader(Exchange.HTTP_METHOD, String.class);
        String endpoint = exchange.getIn().getHeader("EndpointURL", String.class);

        if (body == null || body.isBlank()) {
            Long deletedId = exchange.getIn().getHeader("deleteId", Long.class);
            if (deletedId != null) {
                body = String.format("{\"deleted\": true, \"deletedId\": %d}", deletedId);
            } else {
                body = "null";
            }
        }

        String preview = (body.length() > 200) ? body.substring(0, 200) + "..." : body;
        LOG.info("Route: {} | Method: {} | URL: {} | Status: {} | Preview: {}",
                routeId, httpMethod, endpoint, httpStatus, preview);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String wrapped = String.format("""
                {
                  "capturedAt": "%s",
                  "routeId": "%s",
                  "httpMethod": "%s",
                  "endpoint": "%s",
                  "httpStatus": %s,
                  "response": %s
                }""", timestamp, routeId, httpMethod, endpoint, httpStatus, body);

        exchange.getIn().setBody(wrapped);
    }
}