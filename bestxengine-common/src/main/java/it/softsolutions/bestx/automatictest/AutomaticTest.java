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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CachedOperationRegistry;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.connections.fixgateway.FixGatewayConnector;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;
import it.softsolutions.xt2.protocol.XT2Msg;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-common First created by: davide.rossoni Creation date: 19/feb/2013
 * 
 **/
public class AutomaticTest implements AutomaticTestMBean{

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomaticTest.class);

   
   
	private FixGatewayConnector connector;
	
	private CachedOperationRegistry cachedOperationRegistry;

	public FixGatewayConnector getConnector() {
		return connector;
	}

	public CachedOperationRegistry getCachedOperationRegistry() {
		return cachedOperationRegistry;
	}

	public void setCachedOperationRegistry(CachedOperationRegistry cachedOperationRegistry) {
		this.cachedOperationRegistry = cachedOperationRegistry;
	}

	public void setConnector(FixGatewayConnector connector) {
		this.connector = connector;
	}
	
    @Override
    public String getSimpleOrderOperationById(String id) throws OperationNotExistingException, BestXException {
    	return cachedOperationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, id).toString();
    }


	public String createNewOrder(String isin, String date, String settlementDate) {
		XT2Msg msg = new XT2Msg();
	/*
	 * SourceMarketName=Oms1FixGateway
_Subject_=/FIX/ORDER/1561556666416_DE0001135275
Currency=EUR
HandlInst=1
UserSessionName=QSDBX
TimeInForce=0
Side=1
OrderQty=20000.0 // type = 3 (d)
ClOrdID=1561556666416
OrdType=1
SettlmntTyp=6
Symbol=DE0001135275
IDSource=4
Account=1994
$IBMessTy___=7 // type = 0 (i)
SecurityID=DE0001135275
FutSettDate=20190628
TransactTime=20190626-08:00:00.000
SessionId=FIX.4.2:SOFT1->OMS1	
	 */
		// msg.setValue(fieldName, value);
		msg.setSourceMarketName("Oms1FixGateway");
		long timestamp = System.currentTimeMillis();
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
		
		return Long.toString(timestamp);
	}
   
}
