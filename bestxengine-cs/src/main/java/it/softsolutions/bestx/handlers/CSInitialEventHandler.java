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
package it.softsolutions.bestx.handlers;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.connections.fixgateway.FixOrderInputLazyBean;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.logutils.ApplicationStatisticsHelper;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.OrderReceivedState;
import it.softsolutions.bestx.states.PriceDiscoveryState;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * Purpose: initial state event handler
 * 
 * Project Name : bestxengine-cs First created by: Ruggero Rizzo Creation date: 06-feb-2012
 * 
 **/
public class CSInitialEventHandler extends CSBaseOperationEventHandler {

	private static final long serialVersionUID = -5987404784119699672L;

	private static final Logger LOGGER = LoggerFactory.getLogger(CSInitialEventHandler.class);

	/**
	 * @param operation
	 */
	public CSInitialEventHandler(Operation operation) {
		super(operation);
	}

	@Override
	public void onCustomerOrder(CustomerConnection source, Order order) {
		LOGGER.debug("Get Order details from FIX message");
		//the incoming order is a FixOrderInputLazyBean which extends Order, we need the ISIN
		//for logging, here we can recover it without triggering a possible placeholder creation
		FixOrderInputLazyBean fixOrder = (FixOrderInputLazyBean) order;
		String instrumentIsin = fixOrder.getIsin();

		Order orderBean = new Order();
		orderBean.setValues(order);
		//[RR20130912] BMXNT-300: store the isin to have it independently from the existence of an Instrument object
		orderBean.setInstrumentCode(instrumentIsin);

		// [RR20121109] BXSUP-1623 : add 3 hours to avoid settlement date time set at midnight
		Date futSettDate = orderBean.getFutSettDate();
		if (futSettDate != null) {
			futSettDate = DateUtils.addHours(futSettDate, 3);
			orderBean.setFutSettDate(futSettDate);
		}

		ApplicationStatisticsHelper.logStringAndUpdateOrderIds(orderBean, "Order." + source.getConnectionName() + "." + instrumentIsin, this.getClass().getName());

		operation.setOrder(orderBean);

		operation.addAttempt();
		String text = orderBean.getText();
		if (text != null) {
			text = LimitFileHelper.getInstance().removePrefix(text);
			orderBean.setText(text);
		}       
		operation.setStateResilient(new OrderReceivedState(), ErrorState.class);
	}
	
	
	@Override
	public void onOperatorPriceDiscovery(OperatorConsoleConnection source) {
		LOGGER.debug("Price discovery required");
		operation.setStateResilient(new PriceDiscoveryState(), ErrorState.class);
	}
}
