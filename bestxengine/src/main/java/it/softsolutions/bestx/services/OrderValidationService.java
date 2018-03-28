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
package it.softsolutions.bestx.services;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.ordervalidation.OrderResult;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine First created by: davide.rossoni Creation date: 11/dic/2013
 * 
 */
public interface OrderValidationService {

	/**
	 * Returns a {@link OrderResult} object with validation information performing a pure formal validation
	 * 
	 * @param operation
	 *            The operation against which the order is to be validated
	 * @param order
	 *            The order to be validated
	 * @return An {@link OrderResult} object
	 * @see {@link OrderResult}
	 */
	public OrderResult validateOrderFormally(Operation operation, Order order);

	/**
	 * Returns a {@link OrderResult} object with validation information performing a validation based on the customer
	 * configuration and business related controls and filters
	 * 
	 * @param operation
	 *            The operation against which the order is to be validated
	 * @param order
	 *            The order to be validated
	 * @param customer
	 *            The ordering customer bearing a list of filters to be applied
	 * @return An {@link OrderResult} object
	 * @see {@link OrderResult}
	 */
	public OrderResult validateOrderByCustomer(Operation operation, Order order, Customer customer);

	/**
	 * Returns a {@link OrderResult} object with validation information performing a validation based on the configured punctual filters
	 * 
	 * @param operation
	 *            The operation against which the order is to be validated
	 * @param order
	 *            The order to be validated
	 * @return An {@link OrderResult} object
	 * @see {@link OrderResult}
	 */
	public abstract OrderResult validateOrderOnPunctualFilters(Operation operation,
			Order order);
}
