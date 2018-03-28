/**
 * 
 */
package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.dao.ExchangeRateDao;
import it.softsolutions.bestx.model.ExchangeRate;
import it.softsolutions.bestx.model.Order;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stefano
 *
 */
/* 
 * isValid iff the instrument has a settlement date
 */
public class SettlementDateControl implements OrderValidator {
	private static final Logger LOGGER = LoggerFactory.getLogger(SettlementDateControl.class);

	private ExchangeRateDao exchangeRateDao;

	//	@Override
	public OrderResult validateOrder(Operation unused, Order order) {
		OrderResultBean result = new OrderResultBean();
		result.setOrder(order);
		result.setValid(true);
		result.setReason("");
		Date instSettDate = order.getInstrument().getDefaultSettlementDate();
		ExchangeRate exchangeRate = exchangeRateDao.getExchangeRate(order.getCurrency());

		if (instSettDate == null && (exchangeRate == null || exchangeRate.getSettlementDate() == null)) {
			LOGGER.error(Messages.getString("SettlementDateControl.4", order.getInstrument().getIsin()));
			result.setReason(Messages.getString("SettlementDateControl.4", order.getInstrument().getIsin()));		
			result.setValid(false);
		}
		return result;
	}

	public void setExchangeRateDao(ExchangeRateDao exchangeRateDao) {
		this.exchangeRateDao = exchangeRateDao;
	}
    public boolean isDbNeeded() {
    	return true;
    }
    
    public boolean isInstrumentDbCheck() {
    	return false;
    }
}
