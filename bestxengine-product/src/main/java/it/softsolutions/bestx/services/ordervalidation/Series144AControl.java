/**
 * 
 */
package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;

/**
 * @author Stefano
 * 
 */
public class Series144AControl implements OrderValidator {

	// @Override
	public OrderResult validateOrder(Operation unused, Order order) {
		OrderResultBean result = new OrderResultBean();
		result.setOrder(order);
		result.setValid(false);
		result.setReason("");
		if (order.getInstrument().getOfferingType() == null) {
			result.setValid(false);
			result.setReason(Messages.getString("Series144AControl.3"));
		} else {
			result.setValid(!("8".equalsIgnoreCase(order.getInstrument()
					.getOfferingType()) 
					|| "9".equalsIgnoreCase(order.getInstrument()
							.getOfferingType())));

			if (!result.isValid()) {
				result.setReason(Messages.getString("Series144AControl.2"));
			}
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
