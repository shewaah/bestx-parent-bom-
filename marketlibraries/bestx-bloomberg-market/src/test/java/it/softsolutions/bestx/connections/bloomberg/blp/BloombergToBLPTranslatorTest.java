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

import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import it.softsolutions.bestx.ConnectionHelper;
import it.softsolutions.bestx.connections.bloomberg.BloombergConnection;
import it.softsolutions.bestx.connections.bloomberg.BloombergProposalInputLazyBean;
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnectionListener;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.tradestac.api.ConnectionStatus;
import it.softsolutions.xt2.protocol.XT2Msg;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestx-bloomberg-market 
* First created by: fabrizio.aponte 
* Creation date: 22/mag/2012 
* 
**/
public class BloombergToBLPTranslatorTest {
	
	private static final long NY_TIMEZONE = 21600000;

	private static BloombergToBLPTranslator bloombergToBLPTranslator;
	private static ClassPathXmlApplicationContext context;
	
	@BeforeClass
	public static void setUp() throws Exception {
		// Initialize finders using Spring
		context = new ClassPathXmlApplicationContext("cs-spring.xml");

		InstrumentFinder instrumentFinder = (InstrumentFinder) context.getBean("dbBasedInstrumentFinder");
		MarketMakerFinder marketMakerFinder = (MarketMakerFinder) context.getBean("dbBasedMarketMakerFinder");
		VenueFinder venueFinder = (VenueFinder) context.getBean("dbBasedVenueFinder");
		MarketFinder marketFinder = (MarketFinder) context.getBean("dbBasedMarketFinder");
		BloombergConnection bloombergConnection = (BloombergConnection) context.getBean("bloombergConnection");
		ConnectionHelper bloombergConnectionHelper = (ConnectionHelper) context.getBean("bloombergConnectionHelper");

		// initialize the blpConnectionListener
		TradeStacPreTradeConnectionListener blpConnectionListener = new TradeStacPreTradeConnectionListener() {
			
			@Override
			public void onMarketConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
			}
			
			@Override
			public void onClientConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
			}
			
			@Override
			public void onClassifiedProposal(Instrument instrument, ClassifiedProposal askClassifiedProposal, ClassifiedProposal bidClassifiedProposal) {
			}

			@Override
			public void onSecurityListCompleted(Set<String> securityList) {
			}
		};
		
		bloombergToBLPTranslator = new BloombergToBLPTranslator();
		bloombergToBLPTranslator.setTradeStacPreTradeConnectionListener(blpConnectionListener);
		bloombergToBLPTranslator.setInstrumentFinder(instrumentFinder);
		bloombergToBLPTranslator.setMarketFinder(marketFinder);
		bloombergToBLPTranslator.setMarketMakerFinder(marketMakerFinder);
		bloombergToBLPTranslator.setVenueFinder(venueFinder);
		
		bloombergToBLPTranslator.setBloombergConnection(bloombergConnection);
		bloombergToBLPTranslator.setBloombergConnectionHelper(bloombergConnectionHelper);
		
		bloombergToBLPTranslator.init();
	}

	@Test(expected = ObjectNotInitializedException.class)
	public void initNull() {
		BloombergToBLPTranslator invalidBloombergToBLPTranslator = new BloombergToBLPTranslator();
		invalidBloombergToBLPTranslator.init();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void onConnectionStatusChange() {
		bloombergToBLPTranslator.onConnectionStatusChange(true, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void onInstrumentPriceNull() {
		BloombergProposalInputLazyBean askBloomProposal = null;
		BloombergProposalInputLazyBean bidBloomProposal = null;

		bloombergToBLPTranslator.onInstrumentPrice(askBloomProposal, bidBloomProposal);
	}

	@Test(expected = IllegalArgumentException.class)
	public void onInstrumentPriceInvalid() {

		BloombergProposalInputLazyBean askBloomProposal = new BloombergProposalInputLazyBean(new XT2Msg(), null, NY_TIMEZONE);
		BloombergProposalInputLazyBean bidBloomProposal = new BloombergProposalInputLazyBean(new XT2Msg(), null, NY_TIMEZONE);

		bloombergToBLPTranslator.onInstrumentPrice(askBloomProposal, bidBloomProposal);
	}

//	@Test
	public void onInstrumentPriceValid() {
		BloombergProposalInputLazyBean askBloomProposal = null;
		BloombergProposalInputLazyBean bidBloomProposal = null;

		bloombergToBLPTranslator.onInstrumentPrice(askBloomProposal, bidBloomProposal);
	}

    
    @AfterClass
    public static void tearDown() throws Exception {
        context.close();
    }
	
}
