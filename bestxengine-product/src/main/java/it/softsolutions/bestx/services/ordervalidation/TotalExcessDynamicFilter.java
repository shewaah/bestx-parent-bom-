/*
 * Copyright 1997-2012 SoftSolutions! srl 
 * All Rights Reserved. 
 * 
 * NOTICE: All information contained herein is, and remains the property of SoftSolutions! srl 
 * The intellectual and technical concepts contained herein are proprietary to SoftSolutions! srl and 
 * may be covered by EU, U.S. and other Foreign Patents, patents in process, and 
 * are protected by trade secret or copyright law. 
 * Dissemination of this information or reproduction of this material is strictly forbidden 
 * unless prior written permission is obtained from SoftSolutions! srl.
 * Any additional licenses, terms and conditions, if any, are defined in file 'LICENSE.txt', 
 * which may be part of this software package.
 */

package it.softsolutions.bestx.services.ordervalidation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.dao.ExcessDao;
import it.softsolutions.bestx.finders.ExchangeRateFinder;
import it.softsolutions.bestx.model.ExchangeRate;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class TotalExcessDynamicFilter implements OrderValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TotalExcessDynamicFilter.class);
    private double limit;
    private double sbilancio;
    private ExcessDao excessDao;
    private ExchangeRateFinder exchangeRateFinder;

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#validateOrder(it.softsolutions.bestx.Operation, it.softsolutions.bestx.model.Order)
     */
    public OrderResult validateOrder(Operation operation, Order order) {
        OrderResultBean result = new OrderResultBean();
        result.setOrder(order);
        result.setReason("");	
        result.setValid(false);
        double sbilancioOrdine = 0;

        if (order.getCustomer() == null) {
            result.setReason(Messages.getString("TotalExcessDynamicFilter.1"));
            return result;
        }
        if(order.getCurrency() == null) {
            result.setReason(Messages.getString("TotalExcessDynamicFilter.0", order.getCustomer().getFixId()));
            return result;
        }

        try {
            this.limit = excessDao.getLimit(order.getCustomer());
            this.sbilancio = excessDao.getCurrentExcess(order.getCustomer());
        } catch (Exception e) {
            result.setReason(Messages.getString("TotalExcessDynamicFilter.2", order.getCustomer().getFixId()));
            LOGGER.error("Unable to find limit in filter for customer: "+ order.getCustomer().getFixId() + "  on order:" + order.getFixOrderId());
        }

        ExchangeRate rate = null;
        String currency = order.getCurrency();
        try {
            rate = exchangeRateFinder.getExchangeRateByCurrency(currency);
        } catch (BestXException e) {
            LOGGER.error("Order {}, error getting the exchange rate for the currency: {}", operation.getOrder().getFixOrderId(), currency, e);
            result.setReason(Messages.getString("TotalExcessDynamicFilter.5", currency));
            return result;
        }

        if (Math.abs(this.sbilancio) > this.limit) {
            if (order.getSide() == OrderSide.BUY) {
                sbilancioOrdine = this.sbilancio + (order.getQty().doubleValue() / rate.getExchangeRateAmount().doubleValue());
            } else {
                sbilancioOrdine = this.sbilancio - (order.getQty().doubleValue() / rate.getExchangeRateAmount().doubleValue());
            }

            if (Math.abs(sbilancioOrdine) >= Math.abs(this.sbilancio)) {
                LOGGER.debug("Verifica sbilancio per l'ordine " + order.getCustomerOrderId() + 
                                " - Saldo acq/ven: " + this.sbilancio + " - Soglia: " + this.limit + " - FILTRO NON SUPERATO");
                result.setReason(Messages.getString("TotalExcessDynamicFilter.3",  this.sbilancio, this.limit));
                return result;
            }
        }

        LOGGER.debug("Verifica sbilancio per l'ordine " + order.getCustomerOrderId() + 
                        " - Saldo acq/ven: " + this.sbilancio + " - Soglia: " + this.limit + " - FILTRO SUPERATO");
        result.setValid(true);
        result.setReason(Messages.getString("TotalExcessDynamicFilter.4",  this.sbilancio, this.limit));
        return result;
    }

    /**
     * Sets the excess dao.
     *
     * @param excessDao the excessDao to set
     */
    public void setExcessDao(ExcessDao excessDao) {
        this.excessDao = excessDao;
    }

    /**
     * Sets the exchange rate finder.
     *
     * @param exchangeRateFinder the exchangeRateFinder to set
     */
    public void setExchangeRateFinder(ExchangeRateFinder exchangeRateFinder) {
        this.exchangeRateFinder = exchangeRateFinder;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#isDbNeeded()
     */
    public boolean isDbNeeded() {
        return false;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#isInstrumentDbCheck()
     */
    public boolean isInstrumentDbCheck() {
        return false;
    }
}
