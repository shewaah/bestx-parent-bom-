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

import quickfix.field.PriceType;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: Creation date: 19-ott-2012
 * 
 **/
public class FormalPriceFilter implements OrderValidator {

	@Override
	public OrderResult validateOrder(Operation unused, Order order) {
		
		OrderResultBean result = new OrderResultBean();
		result.setOrder(order);
		result.setReason("");
		result.setValid(true);
		
		if(PriceType.PERCENTAGE == order.getPriceType().intValue()) {
			if (order.getType() != null && order.getType() == Order.OrderType.LIMIT) {
				
				if (order.getLimit() == null || order.getLimit().getAmount().compareTo(BigDecimal.ZERO) == 0) {
					result.setValid(false);
					result.setReason(Messages.getString("FormalPrice.0"));
				} else if (order.getLimit().getAmount().compareTo(BigDecimal.ZERO) < 0) {
					result.setValid(false);
					result.setReason(Messages.getString("FormalPrice.1"));
				}
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
