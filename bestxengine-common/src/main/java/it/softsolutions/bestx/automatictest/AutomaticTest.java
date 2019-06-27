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
package it.softsolutions.bestx.automatictest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationRegistry;
import it.softsolutions.bestx.connections.fixgateway.FixGatewayConnector;
import it.softsolutions.xt2.protocol.XT2Msg;

/**
 * 
 * This class allows to test the system from a JMX interface by allowing to
 * create a new order and then retrieving its status.
 * 
 * @author Robert Gonzalez & Alberto Acquafresca
 *
 */
public class AutomaticTest implements AutomaticTestMBean{

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomaticTest.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    
	private FixGatewayConnector connector;
	
	private OperationRegistry operationRegistry;

    public FixGatewayConnector getConnector() {
		return connector;
	}

	public void setConnector(FixGatewayConnector connector) {
		this.connector = connector;
	}

	public OperationRegistry getOperationRegistry() {
		return operationRegistry;
	}

	public void setOperationRegistry(OperationRegistry operationRegistry) {
		this.operationRegistry = operationRegistry;
	}

	@Override
    public String getSimpleOrderOperationById(String id) {
		try {
		LOGGER.info("Getting information of order: " + id);
    	String ret = operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, id).toString();
    	LOGGER.info("Information from order " + id + ": " + ret);
    	return ret;
		} catch (Exception e) {
			LOGGER.error("Error getting information from order " + id + ": " + e.getMessage());
			e.printStackTrace();
			return "ERROR: " + e.getMessage();
		}
    }

	@Override
	public String createNewOrder(String isin) {
		LocalDate today = LocalDate.now();
		
		LocalDate todayPlusTwo = LocalDate.from(today);
		
	    int addedDays = 0;
	    while (addedDays < 2) {
	    	todayPlusTwo = todayPlusTwo.plusDays(1);
	    	// Add Italy holidays?
	        if (!(todayPlusTwo.getDayOfWeek() == DayOfWeek.SATURDAY ||
	        		todayPlusTwo.getDayOfWeek() == DayOfWeek.SUNDAY)) {
	            ++addedDays;
	        }
	    }
	    
		return this.createNewOrder(isin, formatter.format(today), formatter.format(todayPlusTwo));
	}
	
	public String createNewOrder(String isin, String date, String settlementDate) {
		LOGGER.info("Creating new order for ISIN: " + isin);
		XT2Msg msg = new XT2Msg();

		// UNIX Timestamp is used to create order ID
		long timestamp = System.currentTimeMillis();
		
		msg.setSourceMarketName("Oms1FixGateway");
		msg.setSubject("/FIX/ORDER/" + timestamp + "_" + isin);
		msg.setValue("Currency", "EUR");
		msg.setValue("HandlInst", "1");
		msg.setValue("UserSessionName", "QSDBX");
		msg.setValue("TimeInForce", "0");
		msg.setValue("Side", "1");
		msg.setValue("OrderQty", 20000.0);
		msg.setValue("ClOrdID", Long.toString(timestamp));
		msg.setValue("OrdType", "1");
		msg.setValue("SettlmntTyp", "6");
		msg.setValue("Symbol", isin);
		msg.setValue("IDSource", "4");
		msg.setValue("Account", "1994");
		msg.setValue("$IBMessTy___", 7);
		msg.setValue("SecurityID", isin);
		msg.setValue("FutSettDate", settlementDate);
		msg.setValue("TransactTime", date + "-08:00:00.000");
		msg.setValue("SessionId", "FIX.4.2:SOFT1->OMS1");
		msg.setName("ORDER");
		connector.onNotification(msg);
		
		LOGGER.info("New order created and sent: " + timestamp);
		return Long.toString(timestamp);
	}
   
}
