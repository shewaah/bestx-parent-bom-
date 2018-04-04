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
package it.softsolutions.bestx.connections.tradeweb.tradexpress;


import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.tradestac.api.ConnectionStatus;
import it.softsolutions.tradestac.client.TradeStacClient;
import it.softsolutions.tradestac.fix.field.ExecType;
import it.softsolutions.tradestac.fix.field.OrdStatus;
import it.softsolutions.tradestac.fix.field.PartyRole;
import it.softsolutions.tradestac.fix50.TSExecutionReport;
import it.softsolutions.tradestac.fix50.TSNoPartyID;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-tradeweb-market 
 * First created by: davide.rossoni 
 * Creation date: 16/lug/2013 
 * 
 **/
public class MyTradeXpressConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyTradeXpressConnection.class);

    private static TradeXpressConnectionImpl tradeXpressConnection;

    private void init() throws Exception{
    	Messages messages = new Messages();
		messages.setBundleName("messages");
		messages.setLanguage("it");
		messages.setCountry("IT");
		
		TradeStacClient tradestacClient = new TradeStacClient();
		
        tradeXpressConnection = new TradeXpressConnectionImpl();
       
        tradeXpressConnection.setFixConfigFileName("fix-tradestac-tradexpress.properties");
        tradeXpressConnection.setTradeStacClientSession(tradestacClient);
        tradeXpressConnection.setTradeXpressConnectionListener(new InnerTradeXpressConnectionListener());
        tradeXpressConnection.init();
        tradeXpressConnection.connect();
    }
    
    MarketOrder marketOrder;

    private void run() throws BestXException {
    	// DE000A1ALVC5@DLRZ [NEW] 1000000.0 - 96.491 || 96.516 - 500000.0
    	
//    	int[] orderQties = new int[]{ 450000, 452000, 453000, 454000, 455000, 12000000, 13000000, 14000000, 15000000, 16000000, 21000000, 24000000 };
    	int[] orderQties = new int[]{ 450000, 453000, 454000, 455000, 12000000, 15000000, 16000000, 24000000, 14000000 };
    	String[] behaviours = new String[]{ "Dealer takes +15 seconds to quote", 
    	"Dealer quote much better then comp but passes on the HIT\\LIFT as if price was subject", 
    	"Dealers Pass on the RFQ, no quotes", 
    	"Dealers all quote worse than composite", 
    	"Dealer will timeout before quoting", 
    	"Dealer will pass after acknowledge quote request", 
    	"Dealers will pass after acknowledge quote request", 
    	"Will quote, wait for customer accepting quote, then pass", 
    	"Dealers will timeout before quoting" };

    	for (int i = 0; i < behaviours.length; i++) {
    		int orderQty = orderQties[i];
	        String behaviour = behaviours[i];
    		
    		// XS0619547838, US40411EAB48, XS0546057570, XS0603832782, XS0576912124
    		String isin = "XS0619547838";
    		String marketSpecificCode = "DLRX";
    		String currency = "EUR";
    		String price = "102.53";
    		
    		Instrument instrument = new Instrument();
    		instrument.setIsin(isin);
    		
    		marketOrder = new MarketOrder();
    		marketOrder.setInstrument(instrument);
    		marketOrder.setSide(OrderSide.BUY);
    		marketOrder.setQty(new BigDecimal(orderQty));
    		marketOrder.setLimit(new Money(currency, new BigDecimal(price)));
    		marketOrder.setFutSettDate(DateUtils.addDays(new Date(), 2));
    		marketOrder.setCurrency("EUR");
    		String marketSessionId = "MS" + System.nanoTime();
    		marketOrder.setMarketSessionId(marketSessionId);
    		MarketMarketMaker marketMarketMaker = new MarketMarketMaker();
    		marketMarketMaker.setMarketSpecificCode(marketSpecificCode);
    		marketOrder.setMarketMarketMaker(marketMarketMaker);
    		
    		tradeXpressConnection.sendOrder(marketOrder);
    		Date now = new Date();
    		System.out.println(String.format("\n%s [%s] === %s ==================", now, marketSessionId, behaviour));
    		System.out.println(String.format("%s [%s] %s  - %s: %s %s", now, marketSessionId, isin, orderQty, marketSpecificCode, price));
    		
    		try { Thread.sleep(5000); } catch (InterruptedException e) { }
        }
        
        do {
            try { Thread.sleep(5000); } catch (InterruptedException e) { }
        } while (true);
    }

    public static void main(String[] args) {
        try {
            MyTradeXpressConnection myTradeXpressConnection = new MyTradeXpressConnection();
            myTradeXpressConnection.init();
            Thread.sleep(6000);
            myTradeXpressConnection.run();
            
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
    
  
    private class InnerTradeXpressConnectionListener implements TradeXpressConnectionListener {

        @Override
        public void onClientConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
        	LOGGER.debug("connectionName = {}, connectionStatus = {}", connectionName, connectionStatus);
            
        }

        @Override
        public void onMarketConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
            LOGGER.debug("connectionName = {}, connectionStatus = {}", connectionName, connectionStatus);
            
        }

        @Override
        public void onOrderReject(String sessionId, String clOrdId, String reason) {
            LOGGER.debug("sessionId = {}, clOrdId = {}, reason = {}", sessionId, clOrdId, reason);
         
            System.out.println(String.format("[%s] onOrderReject: %s", clOrdId, reason));
        }

        @Override
	    public void onExecutionReport(String sessionId, String clOrdId, TSExecutionReport tsExecutionReport) {

	    	ExecType execType = tsExecutionReport.getExecType();
	    	OrdStatus ordStatus = tsExecutionReport.getOrdStatus();
	    	BigDecimal accruedInterestAmount = tsExecutionReport.getAccruedInterestAmt() != null ? BigDecimal.valueOf(tsExecutionReport.getAccruedInterestAmt()) : BigDecimal.ZERO;
	    	BigDecimal accruedInterestRate = BigDecimal.ZERO;
	    	BigDecimal lastPrice = tsExecutionReport.getLastPx() != null ? BigDecimal.valueOf(tsExecutionReport.getLastPx()) : BigDecimal.ZERO;
	    	String contractNo = tsExecutionReport.getExecID();
	    	Date futSettDate = tsExecutionReport.getSettlDate();
	    	Date transactTime = tsExecutionReport.getTransactTime();
	    	String text = tsExecutionReport.getText();
        	String executionBroker = null;
        	for(TSNoPartyID party : tsExecutionReport.getTSParties().getTSNoPartyIDsList()) {
        		if(party.getPartyRole() == PartyRole.ExecutingFirm) {
        			executionBroker = party.getPartyID();
        			break;
        		}
        	}
        	String micCode = null;
        	LOGGER.debug(
                    "sessionId = {}, clOrdId = {}, execType = {}, ordStatus = {}, accruedInterestAmount = {}, accruedInterestRate = {}, lastPrice = {}, contractNo = {}, futSettDate = {}, transactTime = {}, text = {}, executionBroker = {}",
                    sessionId, clOrdId, execType, ordStatus, accruedInterestAmount, accruedInterestRate, lastPrice, contractNo, futSettDate, transactTime, text, executionBroker);
            
            LOGGER.debug("{}, {}, {}, {}", sessionId, clOrdId, execType, ordStatus);

            System.out.println(String.format("%s [%s] %s - %s: [%s] %s - %s", new Date(), clOrdId, execType, ordStatus, text == null ? "" : text, lastPrice, executionBroker != null ? executionBroker : ""));
            
//            try {
//	            tradeXpressConnection.cancelOrder(marketOrder);
//            } catch (BestXException e) {
//            	LOGGER.error("{}", e.getMessage(), e);
//	        }
        }

		@Override
        public void onOrderCancelReject(String sessionId, String clOrdId, String reason) {
			LOGGER.debug("sessionId = {}, clOrdId = {}, reason = {}", sessionId, clOrdId, reason);
	     
			System.out.println(String.format("[%s] onOrderCancelReject: %s", clOrdId, reason));
        }
        
    }
}
