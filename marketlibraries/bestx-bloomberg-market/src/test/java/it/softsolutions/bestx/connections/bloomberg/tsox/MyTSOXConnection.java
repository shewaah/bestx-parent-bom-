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
package it.softsolutions.bestx.connections.bloomberg.tsox;


import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.connections.ConnectionListener;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.tradestac.api.ConnectionStatus;
import it.softsolutions.tradestac.fix50.TSExecutionReport;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-bloomberg-market 
 * First created by: davide.rossoni 
 * Creation date: 16/lug/2013 
 * 
 **/
public class MyTSOXConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyTSOXConnection.class);

    private static TSOXConnection tsoxConnection;

    private void init() {
//    	Messages messages = new Messages();
//    	messages.setBundleName("messages");
//    	messages.setLanguage("it");
//    	messages.setCountry("IT");
		
        tsoxConnection = new RBLD_TSOXConnection();
        tsoxConnection.setTsoxConnectionListener(new InnerTSOXConnectionListener());
        
        try { Thread.sleep(5000); } catch (InterruptedException e) { }
    }

    private void run() throws BestXException {
        Instrument instrument = new Instrument();
        instrument.setIsin("AT0001929292");
        
        MarketOrder marketOrder = new MarketOrder();
        marketOrder.setInstrument(instrument);
        marketOrder.setSide(OrderSide.BUY);
        marketOrder.setQty(new BigDecimal(100000));
        marketOrder.setFutSettDate(DateUtils.addDays(new Date(), 3));
        marketOrder.setCurrency("EUR");
        MarketMarketMaker marketMarketMaker = new MarketMarketMaker();
        marketMarketMaker.setMarketSpecificCode("BARX");
        marketOrder.setMarketMarketMaker(marketMarketMaker);
        
        tsoxConnection.sendRfq(marketOrder);
    }

    public static void main(String[] args) {
        try {
            MyTSOXConnection myTSOXConnection = new MyTSOXConnection();
            myTSOXConnection.init();
            myTSOXConnection.run();
            
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
    
    private class InnerConnectionListener implements ConnectionListener {

        @Override
        public void onConnection(Connection source) {
            LOGGER.debug("{}", source);            
        }

        @Override
        public void onDisconnection(Connection source, String reason) {
            LOGGER.debug("{}, {}", source, reason);
            
        }
        
    }
 
    @SuppressWarnings("deprecated")
    private class InnerTSOXConnectionListener implements TSOXConnectionListener {

        @Override
        public void onClientConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
            LOGGER.debug("{}, {}", connectionName, connectionStatus);
            
        }

        @Override
        public void onMarketConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
            LOGGER.debug("{}, {}", connectionName, connectionStatus);
            
        }

        @Override
        public void onCounter(String sessionId, String quoteReqId, String quoteRespId, String quoteId, String marketMaker, BigDecimal price, BigDecimal qty, String currency, 
                        ProposalSide side, ProposalType type, Date futSettDate, int acknowledgeLevel, String onBehalfOfCompID) {
            LOGGER.debug("{}, {}", sessionId, marketMaker);
            
        }

        @Override
        public void onOrderReject(String sessionId, String quoteReqId, String reason) {
            LOGGER.debug("{}, {}, {}", sessionId, quoteReqId, reason);
            
        }

        @Override
        public void onExecutionReport(String sessionId, String clOrdId, TSExecutionReport tsExecutionReport) {
            LOGGER.debug("{}, {}, {}, {}", sessionId, clOrdId, tsExecutionReport.getExecType(), tsExecutionReport.getOrdStatus());
        }

        @Override
        public void onQuoteStatusTimeout(String sessionId, String quoteReqID, String quoteID, String dealer, String text) {
            LOGGER.debug("{}, {}, {}, {}, {}", sessionId, quoteReqID, quoteID, dealer, text);
        }

        @Override
        public void onQuoteStatusTradeEnded(String sessionId, String quoteReqID, String quoteID, String dealer, String text) {
            LOGGER.debug("{}, {}, {}, {}, {}", sessionId, quoteReqID, quoteID, dealer, text);
        }

        @Override
        public void onQuoteStatusExpired(String sessionId, String quoteReqID, String quoteID, String dealer) {
            LOGGER.debug("{}, {}, {}, {}", sessionId, quoteReqID, quoteID, dealer);
        }

		@Override
		public void onCancelReject(String sessionId, String quoteReqId, String reason) {
			
		}
        
    }
}
