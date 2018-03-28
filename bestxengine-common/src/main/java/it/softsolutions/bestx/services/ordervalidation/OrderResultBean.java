package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.model.Order;

public class OrderResultBean implements OrderResult {
    private Order order;
    private String reason = "";
    private boolean valid;

    public void setOrder(Order order) {
        this.order = order;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public Order getOrder() {
        return order;
    }

    public String getReason() {
        return reason;
    }

    public boolean isValid() {
        return valid;
    }
}
