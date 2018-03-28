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

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Instrument;
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
public class NonRetailIsinFilter implements OrderValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(NonRetailIsinFilter.class);

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#validateOrder(it.softsolutions.bestx.Operation, it.softsolutions.bestx.model.Order)
     */
    public OrderResult validateOrder(Operation operation, Order order) {
        OrderResultBean result = new OrderResultBean();
        result.setOrder(order);
        result.setReason("");		

        // Filter is applied only to BUY orders
        if (order.getSide().equals(OrderSide.SELL)) {  //20081124 AMC corrected wrong order side
            result.setValid(true);
        } else {
            Instrument inst = order.getInstrument();
            if (!inst.isInInventory()) {  // 20081124 AMC corrected because it before told not being in the inventory the bond when it is
                LOGGER.info(inst.getIsin() + " is not in inventory.");
                result.setValid(false);
                result.setReason(Messages.getString("NonRetailIsinFilter.1"));
            } else if (inst.getInstrumentAttributes().isRetailCustomerDisabled()) {
                LOGGER.info(inst.getIsin() + " is not enabled for retail."); //$NON-NLS-1$
                result.setValid(false);
                result.setReason(Messages.getString("NonRetailIsinFilter.0"));
            } else { // is available for retail customer
                result.setValid(true);
            }
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
