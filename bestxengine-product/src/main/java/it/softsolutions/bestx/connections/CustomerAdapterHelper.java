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
package it.softsolutions.bestx.connections;

import java.math.BigDecimal;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.Operation.RevocationState;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.model.Rfq;
import it.softsolutions.bestx.services.logutils.ApplicationStatisticsHelper;

/** 
*
* Purpose: this class is mainly for manage the messages exchange between BestX and the customer connection
*
* Project Name : bestxengine-product
* First created by: ruggero.rizzo
* Creation date: 18/giu/2012
*
**/
public class CustomerAdapterHelper implements CustomerConnection{
	Logger LOGGER = LoggerFactory.getLogger(CustomerAdapterHelper.class);
	private CustomerConnection customerConnection;
	private Map<String, CustomerConnection> adaptersMap;

	private void checkPreRequisites() throws ObjectNotInitializedException {
	}

	public void init() throws BestXException {
		checkPreRequisites();
	}
	@Override
	public void sendRfqResponse(Operation source, Rfq rfq, String rfqId, Quote quote) throws BestXException {
		throw new UnsupportedOperationException();
	}
	@Override
	public void sendRfqReject(Operation source, Rfq rfq, String rfqId, CustomerConnection.ErrorCode errorCode, String reason) throws BestXException {
		throw new UnsupportedOperationException();
	}
	@Override
	public void sendOrderReject(Operation source, Order order, String orderId, ExecutionReport executionReport, CustomerConnection.ErrorCode errorCode, String rejectReason) throws BestXException {
		// Send Fix SessionID
		LOGGER.debug("Send Order Reject for OrderId: {}", orderId);
				
		String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
		LOGGER.debug("Session ID : {}", fixSessionId);
		customerConnection = adaptersMap.get(fixSessionId);
		if(customerConnection != null)
			customerConnection.sendOrderReject(source, order, orderId, executionReport, errorCode, rejectReason);
		else LOGGER.warn("Unable to send order rejection of order {}, because the customerconnection is not available", orderId);
	}
	

	@Override
	public void sendPartialFillExecutionReport(Operation source, Quote quote, Order order, String orderId, Attempt attempt, ExecutionReport executionReport, BigDecimal cumQty) throws BestXException {
		// Send Fix SessionID
		LOGGER.debug("Send Execution Report for OrderId: {}", orderId);
		
		String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
		LOGGER.debug("Session ID : {}", fixSessionId);
		customerConnection = adaptersMap.get(fixSessionId);
		customerConnection.sendPartialFillExecutionReport(source, quote, order, orderId, attempt, executionReport, cumQty);    
	}

	/**
	 * Method used to send the closure message report, which is the execution report to be sent
	 * after all the partial fills
	 */
	public void sendClosureExecutionReport(Operation source, Quote quote, Order order, String orderId, Attempt attempt, ExecutionReport executionReport) throws BestXException {
		// Send Fix SessionID
		LOGGER.debug("Send Execution Report for OrderId: {}", orderId);
		
		String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
		LOGGER.debug("Session ID : {}", fixSessionId);
		customerConnection = adaptersMap.get(fixSessionId);
		customerConnection.sendExecutionReport(source, quote, order, orderId, attempt, executionReport);    
	}
	
	@Override
	public void sendExecutionReport(Operation source, Quote quote, Order order, String orderId, Attempt attempt, ExecutionReport executionReport) throws BestXException {
		// Send Fix SessionID
		LOGGER.debug("Send Execution Report for OrderId: {}", orderId);
		
		
		String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
		LOGGER.debug("Session ID : {}", fixSessionId);
		customerConnection = adaptersMap.get(fixSessionId);
		
		ApplicationStatisticsHelper.logStringAndUpdateOrderIds(order, "Order.ExecutionReport_" + customerConnection.getConnectionName()+ "." + order.getInstrument().getIsin(), this.getClass().getName());
		
        LOGGER.debug("[INT-TRACE] Sending execution report : {}", executionReport);
		customerConnection.sendExecutionReport(source, quote, order, orderId, attempt, executionReport);    
	}
	
	@Override
	public void sendOrderResponseAck(Operation source, Order order, String orderId) throws BestXException {
		// Send Fix SessionRID
		LOGGER.debug("Send Order Response Ack for OrderId: {}", orderId);
		
		String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
		LOGGER.debug("Session ID : {}", fixSessionId);
		if (adaptersMap.containsKey(fixSessionId)) {
			customerConnection = adaptersMap.get(fixSessionId);
			customerConnection.sendOrderResponseAck(source, order, orderId);
		} else {
			throw new BestXException("Unable to retrieve a valid customerConnection for fixSessionID [" + fixSessionId + "], please check configuration");
		}
	}
	@Override
	public void sendOrderResponseNack(Operation source, Order order, String orderId, String errorMsg) throws BestXException {
		// Send Fix SessionID
		LOGGER.info("Send Order Response Nack for OrderId: {}", orderId);
		
		String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
		LOGGER.debug("Session ID : {}", fixSessionId);
		customerConnection = adaptersMap.get(fixSessionId);
		
		if (customerConnection != null) {
			customerConnection.sendOrderResponseNack(source, order, orderId, errorMsg);
		} else {
			throw new BestXException("Unable to retrieve a valid customerConnection for orderId = " + orderId + ", fixSessionId = " + fixSessionId);
		}
	}
	@Override	
	public void sendRevokeAck(Operation source, String orderId, String comment) throws BestXException {
		String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
		LOGGER.debug("Session ID : {}", fixSessionId);
		customerConnection = adaptersMap.get(fixSessionId);
		customerConnection.sendRevokeAck(source, orderId, comment);
	}
	@Override
	public void sendRevokeNack(Operation source, Order order, String comment) throws BestXException {
		String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
		LOGGER.debug("Session ID : {}", fixSessionId);
		customerConnection = adaptersMap.get(fixSessionId);
		customerConnection.sendRevokeNack(source, order, comment);
	}
	@Override
	public void sendRevokeReport(Operation source, Order order, 
			RevocationState revocationState, String comment)
			throws BestXException {
		String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
		LOGGER.debug("Session ID : {}", fixSessionId);
		customerConnection = adaptersMap.get(fixSessionId);
		customerConnection.sendRevokeReport(source, order, revocationState, comment);
	}

	/**
	 * @return the adaptersMap
	 */
	public Map<String, CustomerConnection> getAdaptersMap() {
		return adaptersMap;
	}

	/**
	 * @param adaptersMap the adaptersMap to set
	 */
	public void setAdaptersMap(Map<String, CustomerConnection> adaptersMap) {
		this.adaptersMap = adaptersMap;
	}
	@Override
	public void connect() throws BestXException {
		
	}
	@Override
	public void disconnect() throws BestXException {
	}
	@Override
	public String getConnectionName() {
		return null;
	}
	@Override
	public boolean isConnected() {
		return false;
	}
	@Override
	public void setConnectionListener(ConnectionListener listener) {
		
	}
	@Override
   public boolean isFillsManager(Operation operation)
   {
      String fixSessionId = operation.getIdentifier(OperationIdType.FIX_SESSION);
      LOGGER.debug("Session ID : {}", fixSessionId);
      customerConnection = adaptersMap.get(fixSessionId);
      return customerConnection.isFillsManager(operation);
   }

	@Override
	public void sendOrderCancelReject(Operation source, String comment)
			throws BestXException {
		String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
		LOGGER.debug("Session ID : {}", fixSessionId);
		customerConnection = adaptersMap.get(fixSessionId);
		customerConnection.sendOrderCancelReject(source, comment);
	}

	@Override
	public String getChannelName() {
		return "CustomerAdapterHelper";
	}
}
