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

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;

import java.math.BigDecimal;

/**
 * Purpose:
 * 
 * Project Name : bestxengine-product First created by: Creation date: 19-ott-2012.
 */
public class MinQuantityControl implements OrderValidator {

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.sabe.model.ValidationRule#validate()
     */
    public OrderResult validateOrder(Operation unused, Order order) {
        OrderResultBean result = new OrderResultBean();
        result.setOrder(order);
        result.setValid(false);
        result.setReason("");
        
        BigDecimal orderQty = order.getQty();
        BigDecimal instrumentMinQty = order.getInstrument().getMinSize();
        
        if (instrumentMinQty != null && orderQty != null) {
            // valid if orderQty >= instrumentMinQty
            // (compareTo returns -1 if orderQty is lesser than instrumentMinQty)
            result.setValid(orderQty.compareTo(instrumentMinQty) != -1);
        }
        
        if (!result.isValid()) {
            result.setReason(Messages.getString("MinQuantityControl.0", order.getQty(), (order.getInstrument().getMinSize() != null ? order.getInstrument().getMinSize() : "ND")));
        }

        return result;
    }

    public boolean isDbNeeded() {
        return true;
    }

    public boolean isInstrumentDbCheck() {
        return false;
    }
}
