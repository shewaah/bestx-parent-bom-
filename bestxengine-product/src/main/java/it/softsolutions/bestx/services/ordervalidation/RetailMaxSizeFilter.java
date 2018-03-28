/**
 * 
 */
package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author AMC
 * 
 */
public class RetailMaxSizeFilter implements OrderValidator {
	private static final Logger LOGGER = LoggerFactory.getLogger(RetailMaxSizeFilter.class);
	private final BigDecimal maxSize;
	

	public RetailMaxSizeFilter (BigDecimal retailMaxSize) {
		maxSize = retailMaxSize;
	}
	
	// @Override
	public OrderResult validateOrder(Operation unused, Order order) {
		OrderResultBean result = new OrderResultBean();
		result.setOrder(order);
		result.setReason("");

		// if the order is a sell one the filter passes
		// AMC 20101020 Requested by Tullio Grilli
		if(order.getSide().compareTo(OrderSide.SELL) == 0)
		{
		   result.setValid(true);
		   return result;
		}
		// END patch
		String orderCurrency = order.getCurrency();

		BigDecimal orderedQty;
		BigDecimal minimumSize;
		if (orderCurrency.equalsIgnoreCase("EUR")
				|| orderCurrency.equalsIgnoreCase("USD")
				|| orderCurrency.equalsIgnoreCase("GBP")) {
			orderedQty = order.getQty();
			minimumSize = order.getInstrument().getMinSize();
			LOGGER.debug("Ordered quantity is: "+orderedQty+".");
			if (orderedQty != null && orderedQty.compareTo(BigDecimal.ZERO) >= 0 && minimumSize != null) {
				if (orderedQty.compareTo(minimumSize) >= 0 &&
						minimumSize.compareTo(maxSize) > 0) {
					result.setValid(false);
				} else {
					result.setValid(true);
				}
			} else {
				result.setValid(false);
			}
		} else {
			result.setValid(true);
		}

		if (!result.isValid()) {
			result.setReason(Messages.getString("RetailMaxSizeFilter.0"));
		}
		return result;
	}
    public boolean isDbNeeded() {
    	return true;
    }
    
    public boolean isInstrumentDbCheck() {
    	return false;
    }
}
