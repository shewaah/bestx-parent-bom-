/*
 * Copyright 1997- 2015 SoftSolutions! srl 
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
package it.softsolutions.bestx.connections.tradeweb;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnectionListener;
import it.softsolutions.bestx.connections.tradeweb.mds.MDSConnectionImpl;
import it.softsolutions.bestx.connections.tradeweb.tradexpress.TradeXpressConnection;
import it.softsolutions.bestx.connections.tradeweb.tradexpress.TradeXpressConnectionImpl;
import it.softsolutions.bestx.connections.tradeweb.tradexpress.TradeXpressConnectionListener;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.tradestac.api.ConnectionStatus;
import it.softsolutions.tradestac.api.TradeStacException;
import it.softsolutions.tradestac.client.TradeStacClient;
import it.softsolutions.tradestac.fix.field.PartyRole;
import it.softsolutions.tradestac.fix50.TSExecutionReport;
import it.softsolutions.tradestac.fix50.TSNoPartyID;
import quickfix.ConfigError;

/**
 *
 * Purpose: this class is mainly for ...
 *
 * Project Name : bestx-tradeweb-market First created by: william.younang
 * Creation date: 12/gen/2015
 * 
 **/
public class MyTradeStacTWClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyTradeStacTWClient.class);

    private MDSConnectionImpl mdsConnection;
    private TradeStacPreTradeConnectionListener mdsConnectionListener;

    private TradeXpressConnection tradeXpressConnection;
    private TradeXpressConnectionListener tradeXpressConnectionListener;

    private TradeStacClient tradestacClient = new TradeStacClient();

    public void setUpMdsConnection() throws TradeStacException, BestXException, ConfigError {
        
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("cs-spring.xml");
        mdsConnection = new MDSConnectionImpl();
        mdsConnection.setFixConfigFileName("fix-tradestac-mds.properties");
        mdsConnection.setTradeStacClientSession(tradestacClient);
        Executor executor = (Executor) context.getBean("threadPoolTradeweb");
        mdsConnectionListener = new TradeStacPreTradeConnectionListener() {

            @Override
            public void onMarketConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
                LOGGER.debug("[{}] {}", connectionName, connectionStatus);
            }

            @Override
            public void onClientConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
                LOGGER.debug("[{}] {}", connectionName, connectionStatus);
            }

            @Override
            public void onClassifiedProposal(Instrument instrument, ClassifiedProposal askClassifiedProposal, ClassifiedProposal bidClassifiedProposal) {
                LOGGER.debug("{}, {}, {}", instrument, askClassifiedProposal, bidClassifiedProposal);
            }

            @Override
            public void onSecurityListCompleted(Set<String> securityList) {
                // TODO Auto-generated method stub
                
            }
        };

        mdsConnection.setInstrumentFinder((InstrumentFinder) context.getBean("dbBasedInstrumentFinder"));
        mdsConnection.setExecutor(executor);
        mdsConnection.setTradeStacPreTradeConnectionListener(mdsConnectionListener);
        mdsConnection.init();
        mdsConnection.connect();
    }

    public void setUpTradeXpressConnection() {
        tradeXpressConnectionListener = new TradeXpressConnectionListenerImpl();
        tradeXpressConnection = new TradeXpressConnectionImpl();
        tradeXpressConnection.setTradeXpressConnectionListener(tradeXpressConnectionListener);
    }

    public void requestInstrumentStatus() throws BestXException, InterruptedException {
        mdsConnection.requestInstrumentStatus();
        // try {
        // Thread.sleep(5000);
        // } catch (InterruptedException e) {
        // }
    }

    public void instrumentPrice() throws BestXException, InterruptedException {
        String isin = "XS0129647813";
        List<String> marketMakerCodes = Arrays.asList(new String[] { "DLRX", "DLRY", "DLRZ", "DLRC" });

        Instrument instrument = new Instrument();
        instrument.setIsin(isin);
        mdsConnection.requestInstrumentPriceSnapshot(instrument, marketMakerCodes);
        // do {
        // try { Thread.sleep(5000); } catch (InterruptedException e) { }
        // } while (true);
    }

    private class TradeXpressConnectionListenerImpl implements TradeXpressConnectionListener {

        @Override
        public void onClientConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
            LOGGER.debug("connectionName = {}, connectionStatus = {}", connectionName, connectionStatus);
        }

        @Override
        public void onMarketConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
            LOGGER.debug("connectionName = {}, connectionStatus = {}", connectionName, connectionStatus);
        }

        @Override
        public void onOrderReject(String sessionId, String quoteReqId, String reason) {
            LOGGER.debug("sessionId = {}, quoteReqId = {}, reason = {}", sessionId, quoteReqId, reason);
        }

		@Override
        public void onOrderCancelReject(String sessionId, String clOrdId, String reason) {
	        // TODO Auto-generated method stub
        }

		@Override
		public void onExecutionReport(String sessionId, String clOrdId, TSExecutionReport tsExecutionReport) {
			List <TSNoPartyID> tsp = tsExecutionReport.getTSParties().getTSNoPartyIDsList();
			String execBroker = null;
			for(int i = 0; i < tsp.size(); i++) {
				if(tsp.get(i).getPartyRole().compareTo(PartyRole.ExecutingFirm) == 0)
					execBroker = tsp.get(i).getPartyID();
			}
			LOGGER.debug("sessionId = {}, clOrdId = {}, execType = {}, ordStatus = {}, text = {}, executionBroker = {}",
            		sessionId, clOrdId, tsExecutionReport.getExecType(), tsExecutionReport.getOrdStatus(),
            		tsExecutionReport.getText(), execBroker);
		}

    }

    public static void main(String[] args) throws Exception {
        MyTradeStacTWClient myTradeStacTWClient = new MyTradeStacTWClient();
        myTradeStacTWClient.setUpMdsConnection();
        myTradeStacTWClient.requestInstrumentStatus();
//        myTradeStacTWClient.setUpTradeXpressConnection();
//        myTradeStacTWClient.sendRfq();

    }

}
