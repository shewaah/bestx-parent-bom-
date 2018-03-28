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

import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Order;

/* 2009-09-25 Ruggero
 * This validator must check if the customer wants orders only in EUR.
 * If so we must check the instrument currency, or, if not available,
 * the order one. If it is EUR then it's ok, otherwise the order
 * is rejectable.
 */
/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by: ruggero.rizzo 
 * Creation date: 25-set-2009 
 * 
 **/
public class CustomerCurrencyValidator implements OrderValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerCurrencyValidator.class);

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

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#validateOrder(it.softsolutions.bestx.Operation, it.softsolutions.bestx.model.Order)
     */
    public OrderResult validateOrder(Operation operation, Order order) {
        OrderResultBean result = new OrderResultBean();
        result.setOrder(order);
        result.setReason("");
        Customer customer = operation.getOrder().getCustomer();
        String eurCurrency = null;
        try
        {
            eurCurrency = "EUR";
        } 
        catch (IllegalArgumentException iae)
        {
            LOGGER.error("Wrong currency code for the Euro value : EUR. Check the currencies!");
            result.setReason(Messages.getString("CustomerCurrencyValidator_WrongEuroCode.0"));
            result.setValid(false);
            return result;
        }

        String externalCurrency = order.getInstrument().getCurrency();
        if (externalCurrency == null)
        {
            LOGGER.debug("The instrument's currency is null, checking the order one.");
            externalCurrency = order.getCurrency();
            if (externalCurrency == null)
            {
                LOGGER.debug("The order's currency is null, unable to check the currency.");
                result.setReason(Messages.getString("CustomerCurrencyValidator_NoCurrencyFound.0"));
                result.setValid(false);
                return result;
            }
        }

        CustomerAttributes customerAttr = null;
        if(customer != null)
        	customerAttr = (CustomerAttributes)customer.getCustomerAttributes();
        if (customerAttr != null)
        {
            if (customerAttr.getOnlyEUROrders())
            {
                if (externalCurrency.equals(eurCurrency))
                {
                    LOGGER.debug("The currency is Euro, the order is valid.");
                    result.setValid(true);
                }
                else
                {
                    LOGGER.debug("The currency is not Euro, the order is rejectable. Currency: {}", externalCurrency);
                    result.setReason(Messages.getString("CustomerCurrencyValidator_DifferentCurrencies.0"));
                    result.setValid(false);
                }
            }
            else
            {
                LOGGER.debug("The customer {} accepts orders in every currency.", customer.getName());
                result.setValid(true);
            }
        }
        else
        {
            LOGGER.info(
                            "Order {}, no attributes found for the customer {}. Validation for orders only in EUR not applicable.",
                            order.getFixOrderId(), (customer != null ? customer.getName() : null));
            result.setValid(true);
        }

        return result;
    }

}
