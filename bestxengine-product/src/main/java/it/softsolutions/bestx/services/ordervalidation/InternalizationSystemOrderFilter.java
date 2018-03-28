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

import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.CustomerAttributesIFC;
import it.softsolutions.bestx.model.Order;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: Implements a validator which returns always a valid OrderResult if the ordered instrument is in a portfolio listed in
 * portfolioList Otherwise returns the OrderValidator result
 * 
 * Project Name : bestxengine-product First created by: davide.rossoni Creation date: 19/ott/2012
 * 
 **/
public class InternalizationSystemOrderFilter implements OrderValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalizationSystemOrderFilter.class);

    protected List<String> portfolioList;

    public OrderResult validateOrder(Operation operation, Order order) {
        OrderResultBean result = new OrderResultBean();
        result.setOrder(order);

        /*
         * 20110617 - Ruggero if the order has not as the execution destination "AKIS" (isBestExecutionRequired returns true is the
         * execution destination is not set or is not AKIS) it is looking for a normal best execution, thus it should not go on an
         * internalized instrument. If this happens, go in warning.
         */
        if (order.isBestExecutionRequired()) {
            result.setValid(true);
            result.setReason("");
            if (order.getInstrument().getInstrumentAttributes() != null && order.getInstrument().getInstrumentAttributes().getPortfolio().isInternalizable()) {
                result.setValid(false);
                result.setReason(Messages.getString("InternalizationSystemOrderFilter.0"));
                LOGGER.info("Invalid best order because it is an internal product.");
            }
        } else {
            /*
             * 20110617 - Ruggero AKIS set as execution destination, here we must check if - the instrument is internalizable - the customer
             * has been enabled to the internalization
             * 
             * If we cannot find the customer attributes, we put the order in Warning for we cannot find out if this order could eventually
             * go to the internalizer.
             */
            LOGGER.debug("Order execution destination {}", order.getExecutionDestination());

            if (Order.IS_EXECUTION_DESTINATION.equals(order.getExecutionDestination())) {
                LOGGER.debug("Order execution destination is the IS one : {}", Order.IS_EXECUTION_DESTINATION);

                result.setReason(Messages.getString("InternalizationSystemOrderFilter.1"));
                result.setValid(false);
                if (order.getInstrument().getInstrumentAttributes() == null || order.getInstrument().getInstrumentAttributes().getPortfolio().isInternalizable()) {
                    result.setValid(true);
                    result.setReason("");
                    LOGGER.info("Valid placed order because it is an internal product.");
                }

                Customer customer = order.getCustomer();
                CustomerAttributesIFC custAttr = customer.getCustomerAttributes();
                if (custAttr != null) {
                    String errorMsg = Messages.getString("InternalizationSystemOrderFilter.2");

                    Boolean internalCust = ((CustomerAttributes) custAttr).getInternalCustomer();
                    if (internalCust != null && !internalCust) {
                        LOGGER.info("Order not valid, we have AKIS as execution destination, but the customer {}/{} is not allowed to use the internalization", customer.getName(), customer.getFixId());
                        String reason = result.getReason();
                        if (reason != null && reason.length() > 0) {
                            reason += " " + errorMsg;
                        } else {
                            reason = errorMsg;
                        }
                        result.setReason(reason);
                        result.setValid(false);
                    } else {
                        LOGGER.info("Order valid, we have AKIS as execution destination and the customer {}/{} is allowed to use the internalization", customer.getName(), customer.getFixId());
                    }
                } else {
                    LOGGER.info("The customer {}/{} has not the attributes set ... cannot find out if he has enabled the internalization.", customer.getName(), customer.getFixId());
                    result.setReason(Messages.getString("InternalizationSystemOrderFilter.3"));
                    result.setValid(false);
                }
            }
        }
        return result;
    }

    public boolean isDbNeeded() {
        return false;
    }

    public boolean isInstrumentDbCheck() {
        return false;
    }

    public List<String> getPortfolioList() {
        return this.portfolioList;
    }

    public void setPortfolioList(List<String> portfolioList) {
        this.portfolioList = portfolioList;
    }
}