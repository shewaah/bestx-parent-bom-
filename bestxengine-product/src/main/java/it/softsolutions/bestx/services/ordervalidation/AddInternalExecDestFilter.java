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
 * Date         : $Date: 2010-03-12 10:05:42 $
 * Header       : $Id: InternalizationSystemOrderFilter.java,v 1.1 2010-03-12 10:05:42 anna.cochetti Exp $
 * Revision     : $Revision: 1.1 $
 * Source       : $Source: /root/scripts/BestXEngine_Akros/src/it/softsolutions/bestx/services/ordervalidation/InternalizationSystemOrderFilter.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_Akros
 */
package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author anna.cochetti
 * 
 *         Implements a validator which changes the execution destination of the order if : - the customer is an internal one - the isin is
 *         internalizable
 */
public class AddInternalExecDestFilter implements OrderValidator {
	private static final Logger LOGGER = LoggerFactory.getLogger(AddInternalExecDestFilter.class);

	public OrderResult validateOrder(Operation operation, Order order) {
		OrderResultBean result = new OrderResultBean();
		result.setOrder(order);
		result.setValid(true);
		result.setReason("");
		Customer customer = order.getCustomer();
		CustomerAttributes customerAttributes = null;
		if (customer != null) {
			customerAttributes = (CustomerAttributes) customer.getCustomerAttributes();
		}

		if (order.isBestExecutionRequired()) {
			if (order.getInstrument().getInstrumentAttributes() != null && order.getInstrument().getInstrumentAttributes().getPortfolio().isInternalizable() && customerAttributes != null
			        && customerAttributes.getInternalCustomer()) {
				order.setExecutionDestination(Order.IS_EXECUTION_DESTINATION);
				LOGGER.info("The customer is an internal one, the isin is internalizable, forcing the execution destination to " + Order.IS_EXECUTION_DESTINATION);
			}
		}
		return result;
	}

	public boolean isDbNeeded() {
		return false;
	}

	public boolean isInstrumentDbCheck() {
		return false;
	}
}