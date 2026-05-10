package hr.algebra.camelle4.model;

import java.time.Instant;

public class OrderEvent {

    private String orderId;
    private String customer;
    private double totalAmount;
    private String currency;
    private Instant createdAt;

    public OrderEvent() {}

    public OrderEvent(String orderId, String customer, double totalAmount, String currency, Instant createdAt) {
        this.orderId = orderId;
        this.customer = customer;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public String getOrderId()       { return orderId; }
    public void setOrderId(String v) { this.orderId = v; }

    public String getCustomer()       { return customer; }
    public void setCustomer(String v) { this.customer = v; }

    public double getTotalAmount()       { return totalAmount; }
    public void setTotalAmount(double v) { this.totalAmount = v; }

    public String getCurrency()       { return currency; }
    public void setCurrency(String v) { this.currency = v; }

    public Instant getCreatedAt()        { return createdAt; }
    public void setCreatedAt(Instant v)  { this.createdAt = v; }
}