package hr.algebra.camelle4.config;

public final class AppConfig {

    public static final String BASE_URL   = "http://localhost:8080/api/categories";
    public static final String OUTPUT_DIR = "output/responses/";

    public static final String SID4 = "8894";

    public static final String RABBIT_EXCHANGE    = "orders-" + 8894 + "-exchange";
    public static final String RABBIT_QUEUE       = "orders-" + 8894 + "-queue";
    public static final String RABBIT_DLX         = "orders-" + 8894 + "-dlx";
    public static final String RABBIT_DLQ         = "orders-" + 8894 + "-dlq";
    public static final String RABBIT_ROUTING_KEY = "order.created";

    public static final String KAFKA_TOPIC_SENSORS  = "iot-sensors-" + 8894 + "-avro";
    public static final String KAFKA_TOPIC_PAYMENTS = "payments-" + 8894 + "-proto";
    public static final String KAFKA_TOPIC_DLQ      = "demo-dlq-" + 8894;

    public static final String KAFKA_GROUP_SENSORS  = "sensor-analytics-" + 8894;
    public static final String KAFKA_GROUP_PAYMENTS = "payment-ledger-" + 8894;

    public static final String DIRECT_PUBLISH_ORDER   = "direct:publishOrder";
    public static final String DIRECT_PUBLISH_SENSOR  = "direct:publishSensor";
    public static final String DIRECT_PUBLISH_PAYMENT = "direct:publishPayment";
    public static final String DIRECT_GLOBAL_DLQ      = "direct:globalDlq";

    public static final String METRIC_ORDERS     = "demo_orders_" + 8894 + "_processed_total";
    public static final String METRIC_SENSORS    = "demo_sensors_" + 8894 + "_processed_total";
    public static final String METRIC_PAYMENTS   = "demo_payments_" + 8894 + "_processed_total";
    public static final String METRIC_DUPLICATES = "demo_duplicates_" + 8894 + "_blocked_total";

    public static final String HEADER_PAYMENT_ID = "paymentId";
    public static final String HEADER_CURRENCY   = "currency";
    public static final String HEADER_LOCATION   = "location";
    public static final String HEADER_SOURCE     = "source";

    private AppConfig() {}
}