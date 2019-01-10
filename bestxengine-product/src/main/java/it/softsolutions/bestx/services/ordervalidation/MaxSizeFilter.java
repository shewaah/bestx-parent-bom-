/**
 * 
 */
package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.dao.ExchangeRateDao;
import it.softsolutions.bestx.model.ExchangeRate;
import it.softsolutions.bestx.model.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stefano
 * 
 */
public class MaxSizeFilter implements OrderValidator {
	private static final Logger LOGGER = LoggerFactory.getLogger(MaxSizeFilter.class);

	private ExchangeRateDao exchangeRateDao;
	private BigDecimal internalAuthThreshold;

	private String mainCurrency;
	
    public String getMainCurrency() {
		return mainCurrency;
	}


	public void setMainCurrency(String mainCurrency) {
		this.mainCurrency = mainCurrency;
	}

	public MaxSizeFilter (ExchangeRateDao exchangeRateDao, BigDecimal internalAuthThreshold) {
		this.exchangeRateDao = exchangeRateDao;
		this.internalAuthThreshold = internalAuthThreshold;
	}
	
	// @Override
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

		BigDecimal orderQtyInEuro;
		if (orderCurrency.equalsIgnoreCase(mainCurrency)) {
			orderQtyInEuro = order.getQty();
		} else {
			ExchangeRate exchangeRate = exchangeRateDao.getExchangeRate(orderCurrency);
			if (exchangeRate != null) {
				orderQtyInEuro = (convert(order.getQty(), exchangeRate.getExchangeRateAmount()));
			} else {
				result.setValid(false);
				orderQtyInEuro = null;
			}
		}
		BigDecimal maxSize = order.getCustomer().getMaxOrderSize();
		if (maxSize == null) {
			LOGGER.error("No MaxSize has been set for customer "
					+ order.getCustomer().getName() + ".");
			result.setReason(Messages.getString("MaxSizeFilter.1", order
					.getCustomer().getName()));
		} else {
			if (orderQtyInEuro != null) {
			   //valid if orderQty greater or equal to 0, 
			   //lesser than maxSize and 
			   //lesser than internalAuthThreshold
			   result.setValid((orderQtyInEuro.compareTo(BigDecimal.ZERO) >= 0)
						&& (orderQtyInEuro.compareTo(maxSize) <= 0)
						&& (orderQtyInEuro.compareTo(internalAuthThreshold) <= 0));
			} else {
				result.setValid(false);
			}

			if (!result.isValid()) {
			   String reason = "";
            if (orderQtyInEuro != null && orderQtyInEuro.compareTo(internalAuthThreshold) > 0)
            {
               reason += Messages.getString("MaxSizeFilter.3", internalAuthThreshold);
            }
            if (reason.length() > 0)
            {
               reason += " - ";
            }
            if (orderQtyInEuro != null && orderQtyInEuro.compareTo(maxSize) > 0)
            {
			      reason += Messages.getString("MaxSizeFilter.2", maxSize);
            }
            result.setReason(reason);
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
		if (exchangeRate.compareTo(new BigDecimal(0.0)) == 0) {
			return new BigDecimal(0.0);
		}
		return (qty.divide(exchangeRate, 2, RoundingMode.HALF_UP));
	}

    public boolean isDbNeeded() {
    	return false;
    }
    
    public boolean isInstrumentDbCheck() {
    	return false;
    }
}
