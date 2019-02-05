/**
 * 
 */
package it.softsolutions.bestx.services.ordervalidation;

import java.math.BigDecimal;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.finders.ExchangeRateFinder;
import it.softsolutions.bestx.model.Order;

/**
 * @author anna.cochetti
 *
 */
public class StopAutoExecHandlInst implements OrderValidator {
	public static String CURANDO_VAL = "This order won't auto execute. ";

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#validateOrder(it.softsolutions.bestx.Operation, it.softsolutions.bestx.model.Order)
	 */
	@Override
	public OrderResult validateOrder(Operation operation, Order order) {
		OrderResultBean result = new OrderResultBean();
		result.setOrder(order);
		result.setValid(true);
		result.setReason("");

		if(order.getHandlInst().equalsIgnoreCase("3")) {
			result.setReason(CURANDO_VAL);
			operation.setNotAutoExecute(true); // AMC this is needed to avoid the order being automatically executed
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#isDbNeeded()
	 */
	@Override
	public boolean isDbNeeded() {
		return false;
	}

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#isInstrumentDbCheck()
	 */
	@Override
	public boolean isInstrumentDbCheck() {
		return false;
	}
}
