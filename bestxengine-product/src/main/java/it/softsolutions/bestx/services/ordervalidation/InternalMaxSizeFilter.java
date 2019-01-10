/**
 * 
 */
package it.softsolutions.bestx.services.ordervalidation;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.dao.ExchangeRateDao;
import it.softsolutions.bestx.model.ExchangeRate;
import it.softsolutions.bestx.model.Order;

/**
 * @author Stefano
 * 
 */
public class InternalMaxSizeFilter implements OrderValidator {
	private static final Logger LOGGER = LoggerFactory.getLogger(InternalMaxSizeFilter.class);

	private ExchangeRateDao exchangeRateDao;
	private BigDecimal internalAuthThreshold;
	private String mainCurrency;
	
    public String getMainCurrency() {
		return mainCurrency;
	}


	public void setMainCurrency(String mainCurrency) {
		this.mainCurrency = mainCurrency;
	}


	public InternalMaxSizeFilter () {
	}

	@Override
	public OrderResult validateOrder(Operation unused, Order order) {
		OrderResultBean result = new OrderResultBean();
		result.setOrder(order);
		result.setReason("");

        if(mainCurrency == null) {
            result.setValid(false);
            LOGGER.info("Order rejected, mainCurrency not configured ");
            result.setReason("MainCurrency not configured");
        }

		String orderCurrency = order.getCurrency();

		BigDecimal orderQtyInEuro = BigDecimal.ZERO;
		if (orderCurrency.equalsIgnoreCase(mainCurrency)) { //$NON-NLS-1$
			orderQtyInEuro = order.getQty();
		} else {
			ExchangeRate exchangeRate = exchangeRateDao.getExchangeRate(orderCurrency);
			if (exchangeRate != null) {
				orderQtyInEuro = (convert(order.getQty(), exchangeRate.getExchangeRateAmount()));
			} else {
				result.setValid(false);
			}
		}
		result.setValid((orderQtyInEuro.compareTo(BigDecimal.ZERO) > 0)
				&& (orderQtyInEuro.compareTo(internalAuthThreshold) <= 0)
				);
		if (!result.isValid()) {
			String reason = "";
			if (orderQtyInEuro.compareTo(BigDecimal.ZERO) == 0) {
				reason += Messages.getString("ExchangeRateNotFound.0", order.getCurrency());
			} 
			else {
				if (reason.length() > 0)
				{
					reason += " - ";
				}
				if (orderQtyInEuro.compareTo(internalAuthThreshold) > 0)
				{
					reason += Messages.getString("MaxSizeFilter.3", internalAuthThreshold);
				}
			}
		}
		return result;
	}

	/**
	 * Tool method used to calculate quantity in Euro
	 * 
	 * @param qty
	 *            Order quantity
	 * @param exchangeRate
	 *            Exchange rate of the order currency
	 * 
	 * @return Order quantity in Euro
	 */
	public static BigDecimal convert(BigDecimal qty, BigDecimal exchangeRate) {
		if (exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		return (qty.divide(exchangeRate, 2, RoundingMode.HALF_UP));
	}

    @Override
	public boolean isDbNeeded() {
    	return false;
    }
    
    @Override
	public boolean isInstrumentDbCheck() {
    	return false;
    }

	public ExchangeRateDao getExchangeRateDao() {
		return exchangeRateDao;
	}

	public void setExchangeRateDao(ExchangeRateDao exchangeRateDao) {
		this.exchangeRateDao = exchangeRateDao;
	}

	public BigDecimal getInternalAuthThreshold() {
		return internalAuthThreshold;
	}

	public void setInternalAuthThreshold(BigDecimal internalAuthThreshold) {
		this.internalAuthThreshold = internalAuthThreshold;
	}
}
