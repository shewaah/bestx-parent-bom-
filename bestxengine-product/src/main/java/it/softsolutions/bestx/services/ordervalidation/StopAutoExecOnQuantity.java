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
public class StopAutoExecOnQuantity implements OrderValidator {
	private BigDecimal noAutoExecSize;
	private ExchangeRateFinder exchangeRateFinder;
	public static String CURANDO_VAL = "This order won't auto execute. ";

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#validateOrder(it.softsolutions.bestx.Operation, it.softsolutions.bestx.model.Order)
	 */
	@Override
	public OrderResult validateOrder(Operation operation, Order order) {
		OrderResultBean result = new OrderResultBean();
		result.setOrder(order);
		result.setValid(false);
		result.setReason("");
		// comment following 3 rows if this filter applies to all orders
//		if(!operation.getOrder().isLimitFile()) {  // if this filter applies only to LimitFile orders
//			result.setValid(true);
//			return result;
//		}
		BigDecimal normalizedOrderSize;
		try {
			normalizedOrderSize = order.getQty().multiply(exchangeRateFinder.getExchangeRateByCurrency(order.getCurrency()).getExchangeRateAmount());
		} catch (BestXException e) {
			result.setReason(Messages.getString("StopAutoExecOnQuantity.0", order.getCurrency()));
			return result;
		}
      result.setValid(true);
		if(normalizedOrderSize.compareTo(noAutoExecSize) > 0) { //size is too high, return valid
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
	public BigDecimal getNoAutoExecSize() {
		return noAutoExecSize;
	}

	public void setNoAutoExecSize(BigDecimal noAutoExecSize) {
		this.noAutoExecSize = noAutoExecSize;
	}

	public ExchangeRateFinder getExchangeRateFinder() {
		return exchangeRateFinder;
	}

	public void setExchangeRateFinder(ExchangeRateFinder exchangeRateFinder) {
		this.exchangeRateFinder = exchangeRateFinder;
	}


}
