package it.softsolutions.bestx.services.ordervalidation;

import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.jsscommon.exceptions.CurrencyMismatchException;

public class OrderValidatorPriceLimit implements OrderValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderValidatorPriceLimit.class);
    
    private static final ResourceBundle messages = ResourceBundle.getBundle("messages");

    public OrderResult validateOrder(Operation operation, Order order) {
        OrderResultBean result = new OrderResultBean();
        result.setValid(true);
        result.setOrder(order);
        if (order.getLimit() != null) {
            Quote quote = operation.getQuote();
            try {
                if (order.getSide() == OrderSide.BUY && order.getLimit().getAmount().compareTo(quote.getAskProposal().getPrice().getAmount()) < 0) {
                    result.setReason(messages.getString("ORDER_VALIDATOR_PRICE_LIMIT_BUY_PRICE_BELOW_QUOTE"));
                    result.setValid(false);
                } else if (order.getSide() == OrderSide.SELL && order.getLimit().getAmount().compareTo(quote.getBidProposal().getPrice().getAmount()) > 0) {
                    result.setReason(messages.getString("ORDER_VALIDATOR_PRICE_LIMIT_SELL_PRICE_ABOVE_QUOTE"));
                    result.setValid(false);
                }
            } catch (IllegalArgumentException e) {
                if (e.getCause() != null && e.getCause() instanceof CurrencyMismatchException) {
                    result.setReason(messages.getString("ORDER_VALIDATOR_PRICE_LIMIT_PRICE_CURRENCY_DIFF_QUOTE"));
                    result.setValid(false);
                } else {
                    LOGGER.error("An error occurred while comparing order price to quote", e);
                    result.setReason(messages.getString("ORDER_VALIDATOR_PRICE_LIMIT_COMPARISON_TO_QUOTE_ERROR"));
                    result.setValid(false);
                }
            }
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
