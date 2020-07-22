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
package it.softsolutions.bestx.model;

import java.math.BigDecimal;
import java.util.Date;

import it.softsolutions.bestx.Operation;
import it.softsolutions.jsscommon.Money;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class LazyOrder {
    
    private Order order;
    private Operation operation;

    public LazyOrder(Operation operation) {
        this.operation = operation;
    }

    private void loadOrderIfNull() {
        if (order == null) {
            order = operation.getOrder();
        }
    }

    public String getCustomerOrderId() {
        loadOrderIfNull();
        return order.getCustomerOrderId();
    }

    public Money getLimit() {
        loadOrderIfNull();
        return order.getLimit();
    }

    public Order.OrderType getType() {
        loadOrderIfNull();
        return order.getType();
    }

    public Customer getCustomer() {
        loadOrderIfNull();
        return order.getCustomer();
    }

    public Date getFutSettDate() {
        loadOrderIfNull();
        return order.getFutSettDate();
    }

    public Instrument getInstrument() {
        loadOrderIfNull();
        return order.getInstrument();
    }

    public BigDecimal getQty() {
        loadOrderIfNull();
        return order.getQty();
    }

    public String getSecExchange() {
        loadOrderIfNull();
        return order.getSecExchange();
    }

    public String getSettlementType() {
        loadOrderIfNull();
        return order.getSettlementType();
    }

    public Rfq.OrderSide getSide() {
        loadOrderIfNull();
        return order.getSide();
    }

    public Date getTransactTime() {
        loadOrderIfNull();
        return order.getTransactTime();
    }

    public String getCurrency() {
        loadOrderIfNull();
        return order.getCurrency();
    }

    public String getFIXOrderId() {
        loadOrderIfNull();
        return order.getFixOrderId();
    }
}
