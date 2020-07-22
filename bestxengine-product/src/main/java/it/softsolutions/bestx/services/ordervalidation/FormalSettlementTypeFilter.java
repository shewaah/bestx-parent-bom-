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
import quickfix.field.SettlType;

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
        result.setValid(true);

        String settlementType = order.getSettlementType();
        
        if (settlementType == null) {
           result.setValid(false);
           result.setReason(Messages.getString("FormalSettlementType.0"));
        } else if (settlementType.equals(SettlType.FUTURE) && order.getFutSettDate() == null) {
           if (order.getInstrument() == null) {
              result.setValid(false);
              result.setReason(Messages.getString("ExistInstrumentControl.0"));
           } else {
              if (order.getInstrument().getIssueDate() != null && DateService.newLocalDate().before(order.getInstrument().getIssueDate())) {
                 //Issue date in future
                 order.setSettlementType(SettlType.WHEN_AND_IF_ISSUED);
              } else {
                 //Issue date in past
                 if (order.getInstrument().getStdSettlementDays() == null) {
                    result.setValid(false);
                    result.setReason(Messages.getString("FormalSettlementType.1"));
                 } else {
                    switch (order.getInstrument().getStdSettlementDays()) {
                       case 0:
                          order.setSettlementType(SettlType.CASH);
                          break;
                       case 1:
                          order.setSettlementType(SettlType.NEXT_DAY);
                          break;
                       case 2:
                          order.setSettlementType(SettlType.T_PLUS_2);
                          break;
                       case 3:
                          order.setSettlementType(SettlType.T_PLUS_3);
                          break;
                       case 4:
                          order.setSettlementType(SettlType.T_PLUS_4);
                          break;
                       case 5:
                          order.setSettlementType(SettlType.T_PLUS_5);
                          break;
                    }
                 }
              }
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
