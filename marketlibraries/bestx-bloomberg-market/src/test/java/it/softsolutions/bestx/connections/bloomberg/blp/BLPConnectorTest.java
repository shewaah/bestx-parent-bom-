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
package it.softsolutions.bestx.connections.bloomberg.blp;

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
* Project Name : bestx-bloomberg-market 
* First created by: fabrizio.aponte 
* Creation date: 22/mag/2012 
* 
**/
public class BLPConnectorTest {
	
	private static final Logger logger = LoggerFactory.getLogger(BLPConnectorTest.class);

	private static BLPConnector blpConnector;
	
	private static  Semaphore semaphore = new Semaphore(0);
	@BeforeClass
	public static void setUp() throws Exception {
		SSLog.init("SSLog.properties");

		//final Semaphore semaphore = new Semaphore(0);

		// Initialize finders using Spring
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("cs-spring.xml");

		InstrumentFinder instrumentFinder = (InstrumentFinder) context.getBean("dbBasedInstrumentFinder");
		MarketMakerFinder marketMakerFinder = (MarketMakerFinder) context.getBean("dbBasedMarketMakerFinder");
		VenueFinder venueFinder = (VenueFinder) context.getBean("dbBasedVenueFinder");
		MarketFinder marketFinder = (MarketFinder) context.getBean("dbBasedMarketFinder");
		TradeStacClient tradestacClient = new TradeStacClient();

		TradeStacPreTradeConnectionListener blpConnectionListener = new TradeStacPreTradeConnectionListener() {
		   
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
				
//				try
//            {
//               proposals.put(askClassifiedProposal);
//               proposals.put(bidClassifiedProposal);
//            }
//            catch (InterruptedException e)
//            {
//               // TODO Auto-generated catch block
//               e.printStackTrace();
//            }
			}

			@Override
			public void onSecurityListCompleted(Set<String> securityList) {
			}
		};

		blpConnector = new BLPConnector();
		blpConnector.setFixConfigFileName("fix-tradestac-blp.properties");
		blpConnector.setTradeStacClientSession(tradestacClient);
		blpConnector.setInstrumentFinder(instrumentFinder);
		blpConnector.setMarketMakerFinder(marketMakerFinder);
		blpConnector.setVenueFinder(venueFinder);
		blpConnector.setMarketFinder(marketFinder);
		blpConnector.setTradeStacPreTradeConnectionListener(blpConnectionListener);
		blpConnector.init();
		blpConnector.connect();
		semaphore.acquire();
	}

	@Test
	public void oneMarketMaker() throws BestXException, InterruptedException {
		// IsinCode = {"XS0055498413", "FR0010428011", "DE0001134922", "DE0001134468", "AT000B048988", "IT0004759673"};		
		//String isin = "XS0055498413";
		String isin = "DE0001030500";
		
		// For BGN and CBBT the Venue.Market is null
//		String[] marketMakers = new String[] {"BGN", "CBBT", "SG", "DKFI", "LLOX", "ERGB", "MELI", "HMTF", "ETLX", "EMTS", "MILA"};
		String[] marketMakers = new String[] {"BGN", "CBBT", "HMTF", "ETLX", "EMTS"};
		
		Instrument instrument = new Instrument();
		instrument.setIsin(isin);
		List<String> marketMakerCodes = Arrays.asList(marketMakers);

		blpConnector.requestInstrumentPriceSnapshot(instrument, marketMakerCodes);


		do {
	        try { Thread.sleep(10000); } catch (InterruptedException e) { }
        } while (false);
	}
}
