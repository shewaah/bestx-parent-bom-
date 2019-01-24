/**
 * 
 */
package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;


/**
 * @author Stefano
 *
 */
public class ExistInstrumentControl implements OrderValidator {
	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#validateOrder(it.softsolutions.bestx.Operation, it.softsolutions.bestx.model.Order)
	 */
	public OrderResult validateOrder(Operation operation, Order order) {
		OrderResultBean result = new OrderResultBean();
		result.setOrder(order);
		result.setReason("");		
		Instrument instrument = order.getInstrument();
		String instrumentCode = order.getInstrumentCode();
		result.setValid(instrument != null && instrument.isInInventory());
		if(!result.isValid()) {
			result.setReason(Messages.getString("ExistInstrumentControl.0", instrumentCode));
		}	
		return result;
	}

    public boolean isDbNeeded() {
    	return false;
    }
    
    public boolean isInstrumentDbCheck() {
    	return true;
    }
}
