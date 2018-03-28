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
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.dao.OperationStateAuditDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Order;

/**  
 *
 * Purpose:  This validation rule checks if the order already exists in active order list or in closed order in the database  
 *
 * Project Name : bestxengine-product 
 * First created by: marcello.oberti 
 * Creation date: 19-ott-2012 
 * 
 **/
public class AlreadyExistingOrderControl implements OrderValidator {

    private OperationStateAuditDao operationStateAuditDao; 

    //	@Override
    /* (non-Javadoc)
     * @param operation not used
     * @param order The order to be validated
     * @return object OrderResult containing the validation result
     */
    public OrderResult validateOrder(Operation operation, Order order) {
        checkPreRequisites();
        OrderResultBean result = new OrderResultBean();
        result.setOrder(order);
        result.setReason("");
        if (!operationStateAuditDao.usedOrderId(operation.getIdentifier(OperationIdType.ORDER_ID))) {
            result.setValid(true);
        }
        else { 
            result.setValid(false);
            result.setReason(Messages.getString("AlreadyExistingOrder.0"));
        }
        return result;
    }

    /**
     * Sets the operation state audit dao.
     *
     * @param operationStateAuditDao the new operation state audit dao
     */
    public void setOperationStateAuditDao(OperationStateAuditDao operationStateAuditDao) {
        this.operationStateAuditDao = operationStateAuditDao;
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (operationStateAuditDao == null) {
            throw new ObjectNotInitializedException("Operation State DAO not set");
        }
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
