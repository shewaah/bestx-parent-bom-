/*
 * Copyright 1997-2013 SoftSolutions! srl 
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

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Order.OrderType;
import it.softsolutions.bestx.model.Order.TimeInForce;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: validate the order checking the TimeInForce field, taking as granted that it has always a value. We accept:
 * <ul>
 * <li>value 0 (Day or Session)</li>
 * <li>value 1 (Good Till Cancel)</li>
 * <li>if Good Till Cancel the OrdType must be Limit</li>
 * </ul>
 * 
 * 
 * 
 * 
 * Project Name : bestxengine-product First created by: ruggero.rizzo Creation date: 16/ott/2013
 * 
 **/
public class FormalTimeInForceValidator implements OrderValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FormalTimeInForceValidator.class);

    @Override
    public OrderResult validateOrder(Operation operation, Order order) {
        OrderResultBean result = new OrderResultBean();
        result.setOrder(order);
        result.setValid(true);
        TimeInForce timeInForce = order.getTimeInForce();
        if (timeInForce != TimeInForce.DAY_OR_SESSION && timeInForce != TimeInForce.GOOD_TILL_CANCEL) {
            LOGGER.info("Cannot accept order because TimeInForce value {} is not allowed.", timeInForce);
            result.setValid(false);
            result.setReason(Messages.getString("TimeInForceNotValid.0", timeInForce));
        }
        if (timeInForce == TimeInForce.GOOD_TILL_CANCEL){
            if (order.getType() != OrderType.LIMIT) {
                LOGGER.info("Cannot accept order because OrdType {} is not allowed for TimeInForce {}.", order.getType(), timeInForce);
                result.setValid(false);
                result.setReason(Messages.getString("TimeInForceNotValid.1", order.getType(), timeInForce));
            } else if (order.getLimit() != null && order.getLimit().getAmount() != null && order.getLimit().getAmount().compareTo(BigDecimal.ZERO) == 0){
                LOGGER.info("Cannot accept order because the limit price [{}] is not allowed for TimeInForce {}.", order.getLimit().getAmount(), timeInForce);
                result.setValid(false);
                result.setReason(Messages.getString("TimeInForceNotValid.2", order.getType(), order.getLimit().getAmount(), timeInForce));
            }
        }
        
        return result;
    }

    @Override
    public boolean isDbNeeded() {
        return false;
    }

    @Override
    public boolean isInstrumentDbCheck() {
        return false;
    }

}
