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

package it.softsolutions.bestx.services;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.ordervalidation.OrderResult;
import it.softsolutions.bestx.services.ordervalidation.OrderResultBean;
import it.softsolutions.bestx.services.ordervalidation.OrderValidator;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class CSOrderValidationService implements OrderValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSOrderValidationService.class);

    private List<OrderValidator> formalValidatorList = null;
    private List<OrderValidator> businessValidatorList = null;
    private List<OrderValidator> punctualValidatorList = null;

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.services.OrderValidationService#validateOrderByCustomer(it.softsolutions.bestx.Operation, it.softsolutions.bestx.model.Order, it.softsolutions.bestx.model.Customer)
     */
    public OrderResult validateOrderByCustomer(Operation operation, Order order, Customer customer) {
        checkRequisites();
        try {
            List<OrderValidator> orderValidatorList = mergeCustomerList(customer.getCustomerValidationRules());
            return validateOrder(orderValidatorList, operation, order);
        } catch (NullPointerException e) {
            operation.getState().setComment(Messages.getString("CSOrderValidationService.0"));
            throw e;
        }
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.services.OrderValidationService#validateOrderFormally(it.softsolutions.bestx.Operation, it.softsolutions.bestx.model.Order)
     */
    public OrderResult validateOrderFormally(Operation operation, Order order) {
        checkRequisites();
        return validateOrder(formalValidatorList, operation, order);
    }

    /**
     * The result string returned is appended for each result of the validation.
     *
     * @param operation the operation
     * @param order the order to be validated
     * @return an OrderResult containing the string of all the applied validation reasons and the validity of the order against the applied
     * validations
     */
    @Override
	public OrderResult validateOrderOnPunctualFilters(Operation operation, Order order) {
        OrderResultBean result = new OrderResultBean();
        result.setValid(true);
        result.setOrder(order);
        result.setReason("");
        StringBuffer reason = new StringBuffer();
        for (OrderValidator validator : punctualValidatorList) {
            OrderResult oResult = validator.validateOrder(operation, operation.getOrder());
            reason.append(reason.length() == 0 ? oResult.getReason() : "; " + oResult.getReason());
        }
        result.setReason(reason.toString());
        return result;
    }

    private OrderResult validateOrder(List<OrderValidator> validators, Operation operation, Order order) {
        OrderResultBean result = new OrderResultBean();
        result.setValid(true);
        result.setOrder(order);
        result.setReason("");
        boolean dbInstCheckOk = true;
        StringBuffer reason = new StringBuffer();
        for (OrderValidator validator : validators) {
            if (!dbInstCheckOk && validator.isDbNeeded()) {
                continue;
            }
            OrderResult oResult = validator.validateOrder(operation, operation.getOrder());
            if (!oResult.isValid()) {
                if (validator.isInstrumentDbCheck()) {
                    dbInstCheckOk = false;
                }
                result.setValid(false);
                // 20090617 appende la reason in ogni caso perche' alcuni controlli hanno reasona anche in caso di esito positivo
                // reason.append(reason.length() == 0 ? oResult.getReason() : " - " + oResult.getReason());
            }
            /*
             * 02-07-2009 Ruggero Tullio ha chiesto che non venga scritto mai Prodotto Interno
             */
            // if(oResult.getReason().length() > 0 && (!isProdottoInterno ||
            // !oResult.getReason().contains(FilterOnPortfolioBasis.Prodotto_Interno))) {
            if (oResult.getReason().length() > 0) {
                reason.append(reason.length() == 0 ? oResult.getReason() : " - " + oResult.getReason());
            }
        }
        result.setReason(reason.toString());
        LOGGER.info("Order validation: {} {} for orderId={}", result.isValid(), (result.isValid() ? "" : " (" + reason + ")"), order.getFixOrderId());

        return result;
    }

    private List<OrderValidator> mergeCustomerList(List<OrderValidator> customerList) {
        ArrayList<OrderValidator> result = new ArrayList<OrderValidator>(businessValidatorList);
        result.addAll(customerList);
        return result;
    }

    private boolean checkRequisites() {
        if (formalValidatorList == null) {
            LOGGER.error(Messages.getString("CSOrderValidationService.0"));
            return false;
        }
        if (businessValidatorList == null) {
            LOGGER.error(Messages.getString("CSOrderValidationService.1"));
            return false;
        }

        // [DR20120530] skip check on punctualValidatorList, it can be undefined
        // ...

        return true;
    }

    /**
     * Sets the formal validator list.
     *
     * @param formalValidatorList the formalValidatorList to set
     */
    public void setFormalValidatorList(List<OrderValidator> formalValidatorList) {
        this.formalValidatorList = formalValidatorList;
    }

    /**
     * Sets the punctual validator list.
     *
     * @param punctualValidatorList the punctualValidatorList to set
     */
    public void setPunctualValidatorList(List<OrderValidator> punctualValidatorList) {
        this.punctualValidatorList = punctualValidatorList;
    }

    /**
     * Sets the business validator list.
     *
     * @param businessValidatorList the businessValidatorList to set
     */
    public void setBusinessValidatorList(List<OrderValidator> businessValidatorList) {
        this.businessValidatorList = businessValidatorList;
    }
}
