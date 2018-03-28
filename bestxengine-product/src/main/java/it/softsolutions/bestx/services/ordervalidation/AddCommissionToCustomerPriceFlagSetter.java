/*
 * Project Name : BestXEngine_Akros
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author: anna.cochetti $
 * Date         : $Date: 2009-06-10 08:27:44 $
 * Header       : $Id: AddCommissionToCustomerPriceFlagSetter.java,v 1.1 2009-06-10 08:27:44 anna.cochetti Exp $
 * Revision     : $Revision: 1.1 $
 * Source       : $Source: /root/scripts/BestXEngine_Akros/src/it/softsolutions/bestx/services/ordervalidation/AddCommissionToCustomerPriceFlagSetter.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_Akros
 */
package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;

public class AddCommissionToCustomerPriceFlagSetter implements OrderValidator {
	
	public boolean isDbNeeded() {
		return false;
	}

	public boolean isInstrumentDbCheck() {
		return false;
	}

	public OrderResult validateOrder(Operation operation, Order order) {
		OrderResultBean result = new OrderResultBean();
		result.setOrder(order);
		result.setReason("");
		result.setValid(true);
		CustomerAttributes customerAttributes = (CustomerAttributes) order.getCustomer().getCustomerAttributes();
		
		if (customerAttributes != null) {
			order.setAddCommissionToCustomerPrice(customerAttributes.getAddCommissionToCustomerPrice());
		} else {
			order.setAddCommissionToCustomerPrice(false);
		}

		return result;
	}
}
