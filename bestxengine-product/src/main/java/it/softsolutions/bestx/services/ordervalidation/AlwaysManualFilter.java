/**
 * 
 */
package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.ordervalidation.OrderResultBean;


/**
 * This validation rule returns that the customer 
 * is set for manual management
 * 
 * @see ValidationRule
 * @author Stefano Pontillo
 */
public class AlwaysManualFilter implements OrderValidator {

	/* (non-Javadoc)
	 * @param operation not used
	 * @param order The order to be validated
	 * @return object OrderResult containing the validation result
	 */
	public OrderResult validateOrder(Operation operation, Order order) {
		OrderResultBean result = new OrderResultBean();
		result.setOrder(order);
		result.setReason(Messages.getString("AlwaysManualFilter.0"));
		result.setValid(false);
		return result;
	}

    public boolean isDbNeeded() {
    	return false;
    }
    
    public boolean isInstrumentDbCheck() {
    	return false;
    }
}
