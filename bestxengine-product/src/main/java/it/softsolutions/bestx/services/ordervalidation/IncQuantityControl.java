/**
 * 
 */
package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.Messages;


import java.math.BigDecimal;

/**
 * @author Stefano
 *
 */
public class IncQuantityControl implements OrderValidator {

//	@Override
	public OrderResult validateOrder(Operation unused, Order order) {
		OrderResultBean result = new OrderResultBean();
		result.setOrder(order);
		result.setReason("");		
		BigDecimal incSize = order.getInstrument().getIncSize();
		if (incSize == null) {
			result.setValid(true);
			return result;
		}
		if(incSize.compareTo(BigDecimal.ZERO) == 0) {
			result.setValid(true);
			return result;
		}
		BigDecimal modulo = order.getQty().remainder(incSize);  // se != 0 non ho una quantita' consistente con incSize
		if (modulo.compareTo(BigDecimal.ZERO)== 0) {
			result.setValid(true);
		} else {
			result.setValid(false);
		}
		
		if(!result.isValid()) {
			result.setReason(Messages.getString("IncQuantityControl.0", incSize));
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
