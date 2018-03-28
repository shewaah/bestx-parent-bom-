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

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class FormalSettlementTypeFilter implements OrderValidator {

    //	@Override
    /* (non-Javadoc)
     * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#validateOrder(it.softsolutions.bestx.Operation, it.softsolutions.bestx.model.Order)
     */
    public OrderResult validateOrder(Operation unused, Order order) {
        OrderResultBean result = new OrderResultBean();
        result.setOrder(order);
        result.setReason("");

        // [DR20120613] Added restricted check on the only supported SettlmntTyp (FUTURE)
        Integer settlementType = order.getSettlementType();
        //		result.setValid(settlementType != null && settlementType == Integer.parseInt("" + quickfix.field.SettlmntTyp.FUTURE));
        result.setValid(settlementType != null);

        if(!result.isValid()) {
            result.setReason(Messages.getString("FormalSettlementType.0"));
        }
        return result;
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
