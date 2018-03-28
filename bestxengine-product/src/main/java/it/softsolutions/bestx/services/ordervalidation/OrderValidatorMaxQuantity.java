package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;

import java.math.BigDecimal;
import java.util.ResourceBundle;

public class OrderValidatorMaxQuantity implements OrderValidator {
    private static final ResourceBundle messages = ResourceBundle.getBundle("messages");
    private BigDecimal maxOrderQuantity;

    public void setMaxOrderQuantity(BigDecimal maxOrderQuantity) {
        this.maxOrderQuantity = maxOrderQuantity;
    }

    public OrderResult validateOrder(Operation notUsed, Order order) {
        return validateOrder(order);
    }

    public OrderResult validateOrder(Order order) {
        OrderResultBean result = new OrderResultBean();
        result.setOrder(order);
        if (maxOrderQuantity != null && order.getQty().compareTo(maxOrderQuantity) > 0) {
            result.setReason(messages.getString("ORDER_VALIDATOR_MAX_QUANTITY_GREATER") + ": " + maxOrderQuantity);
            result.setValid(false);
        } else {
            result.setValid(true);
        }
        return result;
    }

    public boolean isDbNeeded() {
        return false;
    }

    public boolean isInstrumentDbCheck() {
        return false;
    }
}
