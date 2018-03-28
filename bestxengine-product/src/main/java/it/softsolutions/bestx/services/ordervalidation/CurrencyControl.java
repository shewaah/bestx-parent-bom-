/**
 * 
 */
package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;


/**
 * This validation rule check if the order currency 
 * is the same of the instrument currency
 * 
 * @see ValidationRule
 * @author Stefano Pontillo
 */
public class CurrencyControl implements OrderValidator {

	/**
	 * Return true if the order currency code 
	 * is the same of its instrument currency code
	 * @param operation not used
	 * @param order the order to be validated
	 */
	@Override
	public OrderResult validateOrder(Operation operation, Order order){
		OrderResultBean result = new OrderResultBean();
		result.setOrder(order);
		result.setReason("");		
		if (order.getInstrument().getCurrency() == null || order.getCurrency().equals(order.getInstrument().getCurrency())){
			result.setValid(true);
		} else {
			result.setReason(Messages.getString("FormalCurrency.0", order.getCurrency(), order.getInstrument().getCurrency()));
			result.setValid(false);
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
