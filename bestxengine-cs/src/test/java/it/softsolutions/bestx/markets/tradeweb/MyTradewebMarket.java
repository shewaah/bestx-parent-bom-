/**
 * Copyright 1997-2015 SoftSolutions! srl 
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
package it.softsolutions.bestx.markets.tradeweb;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationRegistry;
import it.softsolutions.bestx.connections.MarketPriceConnection;
import it.softsolutions.bestx.connections.MarketPriceConnectionListener;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.Book;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.states.InitialState;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.jsscommon.SSLog;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-tradeweb-market
 * First created by: davide.rossoni
 * Creation date: 02/gen/2015
 * 
 */
public class MyTradewebMarket {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(MyTradewebMarket.class);

//	protected static final String ISIN = "XS0452418238";//"FR0010853226";
//    protected static final String ISIN = "FR0010379255";

    private VenueFinder venueFinder; 
	private TradewebMarket tradewebMarket;
	private MarketPriceConnectionListener marketPriceConnectionListener;
	private OperationRegistry operationRegistry;
	
	private void init() throws Exception {
//		Messages messages = new Messages();
//		messages.setBundleName("messages");
//		messages.setLanguage("it");
//		messages.setCountry("IT");

		SSLog.init("SSLog.properties");

		// final Semaphore semaphore = new Semaphore(0);

		// Initialize finders using Spring
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("cs-spring.xml");

		SessionFactory sessionFactory = (SessionFactory) context.getBean("sessionFactory");
		
		InitialState initialState = new InitialState();
		System.out.println("initialState > " + initialState);
		
//		HibernateVenueDao hibernateVenueDao = new HibernateVenueDao();
//		hibernateVenueDao.setSessionFactory(sessionFactory);
		
		operationRegistry = (OperationRegistry) context.getBean("defaultOperationRegistry"); 
		
		tradewebMarket = (TradewebMarket) context.getBean("tradewebMarket");
		
		System.out.println("tradewebMarket >> " + tradewebMarket);
		
		marketPriceConnectionListener = new MarketPriceConnectionListener() {
			
			@Override
			public void onMarketBookPartial(MarketCode marketCode, Book book, String reason, List<String> marketMakersOnBothSides) {
				LOGGER.debug("marketCode = {}, book = {}, reason = {}, marketMakersOnBothSides = {}", marketCode, book, reason, marketMakersOnBothSides);
			}
			
			@Override
			public void onMarketBookNotAvailable(MarketPriceConnection source, String reason) {
				LOGGER.debug("source = {}, reason = {}", source, reason);
			}
			
			@Override
			public void onMarketBookComplete(MarketPriceConnection source, Book book) {
				LOGGER.debug("source = {}, book = {}", source, book);
			}
			
			@Override
			public void onMarketBookComplete(MarketCode marketCode, Book book) {
				LOGGER.debug("marketCode = {}, book = {}", marketCode, book);
				
				Collection<ClassifiedProposal> proposals = (Collection<ClassifiedProposal>) book.getAskProposals();
				for (ClassifiedProposal classifiedProposal : proposals) {
					
					Money price = classifiedProposal.getPrice();
					if (!price.getAmount().equals(BigDecimal.ZERO)) {
						
						try {
							Operation operation = operationRegistry.getNewOperationById(OperationIdType.TW_SESSION_ID, "TWID_" + System.currentTimeMillis(), false);
							System.out.println("onMarketBookComplete -> Accept proposal: " + classifiedProposal);
						
							String dealerCode = classifiedProposal.getMarketMarketMaker().getMarketSpecificCode();
							
							// DE000A1ALVC5@DLRZ [NEW] 1000000.0 - 96.491 || 96.516 - 500000.0
					    	int orderQty = classifiedProposal.getQty().intValue();
					    	
					        Instrument instrument = book.getInstrument();
					        System.out.println("****************** Instrument = " + instrument);
					        MarketOrder marketOrder = new MarketOrder();
					        marketOrder.setInstrument(instrument);
					        marketOrder.setSide(classifiedProposal.getSide() == ProposalSide.BID ? OrderSide.SELL : OrderSide.BUY);
							marketOrder.setQty(new BigDecimal(orderQty));
							marketOrder.setLimit(price);
					        marketOrder.setFutSettDate(new Date());
					        marketOrder.setCurrency("EUR");
					        String marketSessionId = "MS" + System.nanoTime();
					        marketOrder.setMarketSessionId(marketSessionId);
					        MarketMarketMaker marketMarketMaker = new MarketMarketMaker();
							marketMarketMaker.setMarketSpecificCode(dealerCode);
					        marketOrder.setMarketMarketMaker(marketMarketMaker);
					        
					        operation.setOrder(marketOrder);
							
					        System.out.println("onMarketBookComplete -> Send order: " + marketOrder);
	                        tradewebMarket.sendFokOrder(operation , marketOrder);
                        } catch (BestXException e) {
                        	LOGGER.error("{}", e.getMessage(), e);
	                        e.printStackTrace();
                        }
					} else {
						System.out.println("onMarketBookComplete -> Discard proposal: " + classifiedProposal);
					}
                }
			}
			
			@Override
			public boolean isActive() {
				LOGGER.debug("");
                
				return false;
			}
			
			@Override
			public Set<MarketPriceConnection> getRemainingMarketPriceConnections() {
				LOGGER.debug("");
                
				return null;
			}
			
			@Override
			public Order getOrder() {
				LOGGER.debug("");
                
				return null;
			}
			
			@Override
			public int getNumWaitingReplyMarketPriceConnection() {
				LOGGER.debug("");
                
				return 0;
			}
			
			@Override
			public List<MarketMarketMaker> getMarketMarketMakersForMarket(MarketCode marketCode) {
				LOGGER.debug("marketCode = {}", marketCode);
                
				return null;
			}
			
			@Override
			public List<MarketMarketMaker> getMarketMarketMakersForEnabledMarkets() {
				LOGGER.debug("");
                
				return null;
			}
			
			@Override
			public Date getCreationDate() {
				LOGGER.debug("");
                
				return null;
			}
			
			@Override
			public boolean deactivate() {
				LOGGER.debug("");
                
				return false;
			}
		};
		
		
//		MarketMakerFinder marketMakerFinder =  (MarketMakerFinder) context.getBean("marketMakerFinder");
//		MarketMaker marketMaker = marketMakerFinder.getMarketMakerByCode("DLR");
		venueFinder =  (VenueFinder) context.getBean("dbBasedVenueFinder");
		tradewebMarket.connect();
	}
	
	private void run() throws BestXException {
		
		
		new Thread() {
			@Override
			public void run() {
				try {

				    Thread.sleep(9000);
//				    String[] isinCodes = new String[] { "DE000A0GMHG2", "DE000A1ALVC5", "FR0010853226", "FR0010952770", "XS0169888558", "XS0197646218" };
//					String[] isinCodes = new String[] { "XS0619547838", "XS0275880267", "XS0388249962", "USG03762HG25", "XS0275880267", "XS0388249962", "DE000A1GNAH1", "XS0473964509", "XS0190174358", "FR0010737882", "XS0172546698", "DE000A1GNAH1", "XS0619547838", "XS0426513387", "XS0473964509" };
					
				    // Forniti da TW 27.01 ore 16.37
//					String[] isinCodes = new String[] { "XS0495913229", "XS0084657039", "XS0104440986", "XS0463509959", "XS0550978364", "USE0002VAC84", "XS0498817542", "ES0211845237", "XS0366202694" };
					
				    // Mail Patrick Mayo del 05.12  - Testing scenarios for integration BestX!-Tradeweb
					String[] isinCodes = new String[] { "FR0010163543", "FR0010517417", "FR0011619436", "FR0011461037", "FR0118462128", "FR0120746609" };
				    
//				    String[] isinCodes = new String[] { "FR0010379255" };
				    
					for (String isinCode : isinCodes) {
						Thread.sleep(5000);
						Set<Venue> venues = venueFinder.getAllVenues();
						
						Instrument instrument = new Instrument();
//						instrument.setIsin(ISIN);
						instrument.setIsin(isinCode);
						instrument.setCurrency("EUR");
						
						Order order = new Order();
						order.setFixOrderId("FixOrderID-" + System.currentTimeMillis());
						order.setInstrument(instrument);
						
						System.out.println("\n------------------------------------");
						System.out.println("Query price for isin = " + isinCode + ", venues = " + venues);
						tradewebMarket.queryPrice(marketPriceConnectionListener, venues, 3000, order);
                    }
                } catch (BestXException | InterruptedException e) {
                	LOGGER.error("{}", e.getMessage(), e);
                }
			}
		}.start();
	}

	public static void main(String[] args) {
		MyTradewebMarket myTradewebMarket = new MyTradewebMarket();
		try {
	        myTradewebMarket.init();
	        myTradewebMarket.run();
        } catch (Exception e) {
        	LOGGER.error("{}", e.getMessage(), e);
        }
	}

}
