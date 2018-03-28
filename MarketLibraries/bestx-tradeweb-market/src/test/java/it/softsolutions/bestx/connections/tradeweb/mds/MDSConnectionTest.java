/*
 * Copyright 1997-2014 SoftSolutions! srl 
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
package it.softsolutions.bestx.connections.tradeweb.mds;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnectionListener;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.jsscommon.SSLog;
import it.softsolutions.tradestac.api.ConnectionStatus;
import it.softsolutions.tradestac.client.TradeStacClient;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestx-tradeweb-market 
* First created by: davide.rossoni 
* Creation date: 19/dec/2014 
* 
**/
public class MDSConnectionTest {
	
	private static final Logger logger = LoggerFactory.getLogger(MDSConnectionTest.class);

	private static MDSConnectionImpl mdsConnection;
	
	private static  Semaphore semaphore = new Semaphore(0);
	
	@BeforeClass
	public static void setUp() throws Exception {
    	Messages messages = new Messages();
		messages.setBundleName("messages");
		messages.setLanguage("it");
		messages.setCountry("IT");
		
		SSLog.init("SSLog.properties");

		//final Semaphore semaphore = new Semaphore(0);

		// Initialize finders using Spring
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("cs-spring.xml");

		InstrumentFinder instrumentFinder = (InstrumentFinder) context.getBean("dbBasedInstrumentFinder");
		MarketMakerFinder marketMakerFinder = (MarketMakerFinder) context.getBean("dbBasedMarketMakerFinder");
		VenueFinder venueFinder = (VenueFinder) context.getBean("dbBasedVenueFinder");
		MarketFinder marketFinder = (MarketFinder) context.getBean("dbBasedMarketFinder");
		TradeStacClient tradestacClient = new TradeStacClient();

		TradeStacPreTradeConnectionListener mdsConnectionListener = new TradeStacPreTradeConnectionListener() {
		   
			@Override
			public void onMarketConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
                logger.debug("[{}] {}", connectionName, connectionStatus);
				semaphore.release();
			}

			@Override
			public void onClientConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
				logger.debug("[{}] {}", connectionName, connectionStatus);
				//semaphore.release();
			}

			@Override
			public void onClassifiedProposal(Instrument instrument, ClassifiedProposal askClassifiedProposal, ClassifiedProposal bidClassifiedProposal) {
				logger.debug("{}, {}, {}", instrument, askClassifiedProposal, bidClassifiedProposal);
			}

            @Override
            public void onSecurityListCompleted(Set<String> securityList) {
                // TODO Auto-generated method stub
                
            }
		};

		mdsConnection = new MDSConnectionImpl();
		mdsConnection.setFixConfigFileName("fix-tradestac-mds.properties");
		mdsConnection.setTradeStacClientSession(tradestacClient);
		mdsConnection.setInstrumentFinder(instrumentFinder);
		mdsConnection.setMarketMakerFinder(marketMakerFinder);
		mdsConnection.setVenueFinder(venueFinder);
		mdsConnection.setMarketFinder(marketFinder);
		mdsConnection.setTradeStacPreTradeConnectionListener(mdsConnectionListener);
		mdsConnection.init();
		mdsConnection.connect();
		semaphore.acquire();
	}
	
	@Test
	public void instrumentStatus() throws BestXException, InterruptedException {
		System.out.println(" --- instrumentStatus --- ");
		mdsConnection.requestInstrumentStatus();
		System.out.println(" --- instrumentStatus 2. --- ");
//		do {
			try { Thread.sleep(5000); } catch (InterruptedException e) { }
//        } while (true);
	}

	@Test
	public void instrumentPrice() throws BestXException, InterruptedException {
        String isin = "XS0129647813";
		List<String> marketMakerCodes = Arrays.asList(new String[] { "DLRX", "DLRY", "DLRZ" });
		
		Instrument instrument = new Instrument();
		instrument.setIsin(isin);
		instrument.setCurrency("EUR");
		mdsConnection.requestInstrumentPriceSnapshot(instrument, marketMakerCodes);
		do {
			try { Thread.sleep(5000); } catch (InterruptedException e) { }
		} while (true);
	}
}
