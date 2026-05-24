package hr.algebra.camelle4.security;

import hr.algebra.camelle4.config.AppConfig;
import org.apache.camel.CamelAuthorizationException;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.spring.security.SpringSecurityAuthorizationPolicy;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class AdminOrderRoute extends RouteBuilder {

    @Override
    public void configure() {

        AuthorityAuthorizationManager<Object> hasAdmin =
                AuthorityAuthorizationManager.hasRole("ADMIN");

        AuthorizationManager<Exchange> camelAuthManager =
                (Supplier<Authentication> authSupplier, Exchange exchange) ->
                        hasAdmin.check(authSupplier, exchange);

        SpringSecurityAuthorizationPolicy adminPolicy =
                new SpringSecurityAuthorizationPolicy();
        adminPolicy.setAuthorizationManager(camelAuthManager);

        onException(CamelAuthorizationException.class)
                .handled(true)
                .setHeader("CamelHttpResponseCode", constant(403))
                .setHeader("Content-Type", constant("application/json"))
                .setBody(constant("""
                        {
                          "error": "Forbidden",
                          "message": "ADMIN role required."
                        }"""));

        from("platform-http:/api/secure/admin-order?httpMethodRestrict=GET,POST")
                .routeId("admin-order-route")
                .policy(adminPolicy)
                .log("ADMIN access granted - forwarding to RabbitMQ")
                .setExchangePattern(org.apache.camel.ExchangePattern.InOnly)
                .to("spring-rabbitmq:" + AppConfig.RABBIT_EXCHANGE
                        + "?routingKey=" + AppConfig.RABBIT_ROUTING_KEY
                        + "&autoDeclare=false&disableReplyTo=true")
                .setExchangePattern(org.apache.camel.ExchangePattern.InOut)
                .setHeader("Content-Type", constant("application/json"))
                .setBody(constant("""
                        {"status": "accepted", "message": "Order queued."}"""));
    }
}