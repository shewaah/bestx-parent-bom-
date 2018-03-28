/**
 * 
 */
package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.finders.db.MapExchangeRateFinder;
import it.softsolutions.bestx.model.ExchangeRate;
import it.softsolutions.bestx.model.Order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stefano
 *
 */
public class ExchangeRateControl implements OrderValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeRateControl.class);
	private MapExchangeRateFinder finder;
	
    public OrderResult validateOrder(Operation operation, Order order) {
        OrderResultBean result = new OrderResultBean();
        result.setOrder(order);
        result.setReason("");       
        String currCode = order.getCurrency();
        
        ExchangeRate exchangeRate = null;
        try {
            exchangeRate = finder.getExchangeRateByCurrency(currCode);
        } catch (BestXException e) {
            // already managed below in the else branch
        }

        if (currCode.equals("EUR") || exchangeRate != null ){
            result.setValid(true);
        }else {
            result.setValid(false);
            LOGGER.info("Order rejected, currency {} not found", currCode);
            result.setReason(Messages.getString("ExchangeRateControl.0", currCode));
        }
        
        return result;
    }


	/**
	 * @param dao the dao to set
	 */
	public void setFinder(MapExchangeRateFinder finder) {
		this.finder = finder;
	}

    public boolean isDbNeeded() {
    	return true;
    }
    
    public boolean isInstrumentDbCheck() {
    	return false;
    }
}
