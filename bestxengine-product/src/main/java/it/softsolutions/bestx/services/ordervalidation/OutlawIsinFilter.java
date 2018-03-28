/**
 * 
 */
package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.finders.OutlawTickerFinder;
import it.softsolutions.bestx.model.Order;

/**
 * This validation rule check if the ticker of the instrument and the currency code 
 * of the order are inserted in OutlawRuleTable table
 * 
 * @author Stefano Pontillo
 */
public class OutlawIsinFilter implements OrderValidator {
//	@Override
	public OrderResult validateOrder(Operation unused, Order order) {
		OrderResultBean result = new OrderResultBean();
		result.setOrder(order);
		result.setValid(false);
		result.setReason("");
		if (order.getInstrument().getInstrumentAttributes() == null) {
			result.setValid(true);
			return result;
		}
		boolean isISINOutlaw = order.getInstrument().getInstrumentAttributes().isOutlaw();
		boolean isTickerOutlaw = getOutlawTickerFinder().isOutLaw(order.getInstrument().getRTFITicker(), order.getCurrency());
		result.setValid(!(isISINOutlaw || isTickerOutlaw));
		if(!result.isValid()) {
			result.setReason(Messages.getString("OutlawIsinFilter.0"));
		}
		return result;
	}
	private OutlawTickerFinder outlawTickerFinder;
	/**
	 * @return the outlawTickerDAO
	 */
	public OutlawTickerFinder getOutlawTickerFinder() {
		return outlawTickerFinder;
	}
	/**
	 * @param outlawTickerDAO the outlawTickerDAO to set
	 */
	public void setOutlawTickerFinder(OutlawTickerFinder outlawTickerFinder) {
		this.outlawTickerFinder = outlawTickerFinder;
	}
    public boolean isDbNeeded() {
    	return false;
    }
    
    public boolean isInstrumentDbCheck() {
    	return false;
    }
}
