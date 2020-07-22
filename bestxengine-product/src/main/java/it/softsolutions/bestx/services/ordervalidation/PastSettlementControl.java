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
import it.softsolutions.bestx.services.DateService;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class PastSettlementControl implements OrderValidator {

    @Override
    public OrderResult validateOrder(Operation unused, Order order) {
        OrderResultBean result = new OrderResultBean();
        result.setOrder(order);
        result.setReason("");
        result.setValid(true);
        if (order.getFutSettDate() != null) {
            result.setValid(DateService.newLocalDate().before(order.getFutSettDate()));
        }
        if (!result.isValid()) {
            result.setReason(Messages.getString("PastSettlement.0", order.getFutSettDate()));
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
