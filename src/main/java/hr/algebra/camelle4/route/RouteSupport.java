package hr.algebra.camelle4.route;

import hr.algebra.camelle4.config.AppConfig;
import org.apache.camel.builder.DeadLetterChannelBuilder;

public final class RouteSupport {

    private RouteSupport() {}

    public static DeadLetterChannelBuilder defaultErrorHandler() {
        DeadLetterChannelBuilder builder = new DeadLetterChannelBuilder();
        builder.setDeadLetterUri(AppConfig.DIRECT_GLOBAL_DLQ);
        builder.maximumRedeliveries(3);
        builder.redeliveryDelay(1_000);
        builder.backOffMultiplier(2.0);
        builder.maximumRedeliveryDelay(30_000);
        builder.useExponentialBackOff();
        builder.logRetryAttempted(true);
        builder.logExhausted(true);
        return builder;
    }
}