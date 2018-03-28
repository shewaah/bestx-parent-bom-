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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.bloomberg.BogusTradeStacClient;
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnectionListener;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.jsscommon.SSLog;
import it.softsolutions.tradestac.api.ConnectionStatus;
import it.softsolutions.tradestac.api.TradeStacException;
import it.softsolutions.tradestac.fix.field.InstrumentPartyIDSource;
import it.softsolutions.tradestac.fix.field.InstrumentPartyRole;
import it.softsolutions.tradestac.fix.field.MDEntryType;
import it.softsolutions.tradestac.fix.field.MDUpdateType;
import it.softsolutions.tradestac.fix.field.NetworkRequestType;
import it.softsolutions.tradestac.fix.field.SecurityIDSource;
import it.softsolutions.tradestac.fix.field.SubscriptionRequestType;
import it.softsolutions.tradestac.fix50.TSMarketDataRequest;
import it.softsolutions.tradestac.fix50.TSNetworkCounterpartySystemStatusRequest;
import it.softsolutions.tradestac.fix50.TSNoMDEntryTypes;


/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestx-bloomberg-market 
* First created by: fabrizio.aponte 
* Creation date: 22/mag/2012 
* 
**/
public class BLPConnectorBogusTSTest{
   private static final Logger logger = LoggerFactory.getLogger(BLPConnectorBogusTSTest.class);

   private static BLPConnector blpConnector = null;
   private static BogusTradeStacClient tsClient = null;
   private static InstrumentFinder instrumentFinder = null;
   
   private static ConnectionStatus connStatus = ConnectionStatus.NotConnected;
   private static ConnectionStatus marketStatus = ConnectionStatus.NotConnected;
   private static TSNetworkCounterpartySystemStatusRequest tsNetworkStatusRequest = null;
   private static TSMarketDataRequest tsMarketDataRequest = null;
   private static ClassifiedProposal askProposal = null;
   private static ClassifiedProposal bidProposal = null;

   private static final String ISIN_CODE = "DE0001134922";
   private static final String MARKET_MAKER_CODE = "RABX";
   private static final String MARKET_MAKER_UNKNOWN = "UNKNOWN";
   private static final double BID_PRICE = 99.123;
   private static final double ASK_PRICE = 99.127;
   private static final double BID_QTY = 100.3;
   private static final double ASK_QTY = 100.3;
   private static final double ZERO_PRICE = 0.0;
   private static final double ZERO_QTY = 0.0;
   private static Date BID_UTC_TIME;
   private static Date ASK_UTC_TIME;

   
   @BeforeClass
   public static void setup() throws Exception{

      logger.info("Begin setup");
      SSLog.init("SSLog.properties");

      // Initialize bid and ask time
      GregorianCalendar calendar = new GregorianCalendar(2012,9,28,14,15,56);
      BID_UTC_TIME = ASK_UTC_TIME = calendar.getTime();
      
      // Initialize finders using Spring
      ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("cs-spring.xml");

      instrumentFinder = (InstrumentFinder) context.getBean("dbBasedInstrumentFinder");
      MarketMakerFinder marketMakerFinder = (MarketMakerFinder) context.getBean("dbBasedMarketMakerFinder");
      VenueFinder venueFinder = (VenueFinder) context.getBean("dbBasedVenueFinder");
      MarketFinder marketFinder = (MarketFinder) context.getBean("dbBasedMarketFinder");
      
      //BogusTradeStacClient.BogusTSClientListener l = new BLPConnectorBogusTSTestMyBogusTSClientListener();
      tsClient = new BogusTradeStacClient(new MyBogusTSClientListener());

      blpConnector = new BLPConnector();
      blpConnector.setFixConfigFileName("fix-tradestac-blp.properties");
      //blpConnector.setFixConfigFileName("fix-tradestac-simulated.properties");
      blpConnector.setTradeStacClientSession(tsClient);
      blpConnector.setInstrumentFinder(instrumentFinder);
      blpConnector.setMarketMakerFinder(marketMakerFinder);
      blpConnector.setVenueFinder(venueFinder);
      blpConnector.setMarketFinder(marketFinder);
      blpConnector.setTradeStacPreTradeConnectionListener( new MyBLPConnectionListener());
      blpConnector.init();

      logger.info("End setup");
   }

   @Test
   public void networkStatusTest() throws BestXException, TradeStacException{
      logger.info("Begin networkStatusTest");
      blpConnector.connect();
      assertFalse(blpConnector.isConnected());
      
//    logon phase
      tsClient.sendLogon(false);
      assertTrue(connStatus == ConnectionStatus.NotConnected);
      assertTrue(marketStatus == ConnectionStatus.NotConnected);
      assertFalse(blpConnector.isConnected());
      assertNull(tsNetworkStatusRequest);
      assertNull(tsMarketDataRequest);

      tsClient.sendLogon(true);
      assertTrue(connStatus == ConnectionStatus.Connected);
      assertTrue(marketStatus == ConnectionStatus.NotConnected);
      assertTrue(blpConnector.isConnected());
      assertNotNull(tsNetworkStatusRequest);
      assertNull(tsMarketDataRequest);
      assertEquals(NetworkRequestType.Subscribe, tsNetworkStatusRequest.getNetworkRequestType());
      assertEquals(blpConnector.getConnectionName(), tsNetworkStatusRequest.getTSCompIDReqGrp().getTSNoCompIDsList().get(0).getRefCompID());
      tsNetworkStatusRequest = null;
      
//    networkstatus up phase
      tsClient.sendNetworkStatusResponse(true);
      assertTrue(connStatus == ConnectionStatus.Connected);
      assertTrue(marketStatus == ConnectionStatus.Connected);
      assertTrue(blpConnector.isConnected());
      assertNull(tsNetworkStatusRequest);
      assertNull(tsMarketDataRequest);
      
//    networkstatus down phase
      tsClient.sendNetworkStatusResponse(false);
      assertTrue(connStatus == ConnectionStatus.Connected);
      assertTrue(marketStatus == ConnectionStatus.NotConnected);
      assertTrue(blpConnector.isConnected());
      assertNull(tsNetworkStatusRequest);
      assertNull(tsMarketDataRequest);

//    logout phase
      tsClient.sendLogout(true);
      assertTrue(connStatus == ConnectionStatus.NotConnected);
      assertTrue(marketStatus == ConnectionStatus.NotConnected);
      assertFalse(blpConnector.isConnected());
      assertNull(tsNetworkStatusRequest);
      assertNull(tsMarketDataRequest);

//    logon phase
      tsClient.sendLogon(true);
      assertTrue(connStatus == ConnectionStatus.Connected);
      assertTrue(marketStatus == ConnectionStatus.NotConnected);
      assertTrue(blpConnector.isConnected());
      assertNotNull(tsNetworkStatusRequest);
      assertNull(tsMarketDataRequest);
      assertEquals(NetworkRequestType.Subscribe, tsNetworkStatusRequest.getNetworkRequestType());
      assertEquals(blpConnector.getConnectionName(), tsNetworkStatusRequest.getTSCompIDReqGrp().getTSNoCompIDsList().get(0).getRefCompID());
      tsNetworkStatusRequest = null;

//    networkstatus up phase
      tsClient.sendNetworkStatusResponse(true);
      assertTrue(connStatus == ConnectionStatus.Connected);
      assertTrue(marketStatus == ConnectionStatus.Connected);
      assertTrue(blpConnector.isConnected());
      assertNull(tsNetworkStatusRequest);
      assertNull(tsMarketDataRequest);

//    logout phase
      tsClient.sendLogout(true);
      assertTrue(connStatus == ConnectionStatus.NotConnected);
      assertTrue(marketStatus == ConnectionStatus.Connected);
      assertFalse(blpConnector.isConnected());
      assertNull(tsNetworkStatusRequest);
      assertNull(tsMarketDataRequest);
      marketStatus = ConnectionStatus.NotConnected;

//    logon phase
      tsClient.sendLogon(true);
      assertTrue(connStatus == ConnectionStatus.Connected);
      assertTrue(marketStatus == ConnectionStatus.NotConnected);
      assertTrue(blpConnector.isConnected());
      assertNotNull(tsNetworkStatusRequest);
      assertNull(tsMarketDataRequest);
      assertEquals(NetworkRequestType.Subscribe, tsNetworkStatusRequest.getNetworkRequestType());
      assertEquals(blpConnector.getConnectionName(), tsNetworkStatusRequest.getTSCompIDReqGrp().getTSNoCompIDsList().get(0).getRefCompID());
      tsNetworkStatusRequest = null;

//    disconnect phase
      blpConnector.disconnect();
      assertTrue(connStatus == ConnectionStatus.Connected);
      assertTrue(marketStatus == ConnectionStatus.NotConnected);
      assertFalse(blpConnector.isConnected());
      assertNull(tsNetworkStatusRequest);
      assertNull(tsMarketDataRequest);
      
      logger.info("End networkStatusTest");
   }
   
   @Test
   public void doubleConnectionTest() throws BestXException{
      logger.info("Begin doubleConnectionTest");

      blpConnector.connect();
      try{
         blpConnector.connect();
         assertTrue(false);
      }catch (BestXException e) {
         assertTrue(true);
      }
      blpConnector.disconnect();

      logger.info("End doubleConnectionTest");
   }

   @Test
   public void doubleDisconnectionTest() throws BestXException{
      logger.info("Begin doubleDisconnectionTest");
   
      blpConnector.connect();
      blpConnector.disconnect();
      try{
         blpConnector.disconnect();
         assertTrue(false);
      }catch (BestXException e) {
         assertTrue(true);
      }
      logger.info("End doubleDisconnectionTest");
   }

   @Test
   public void priceTest() throws BestXException, TradeStacException{
      logger.info("Begin priceTest");
      marketStatus = ConnectionStatus.NotConnected;
      connStatus = ConnectionStatus.Connected;

      blpConnector.connect();
      tsClient.sendLogon(true);
      tsNetworkStatusRequest = null;
      
      tsClient.sendNetworkStatusResponse(true);
      assertTrue(marketStatus == ConnectionStatus.Connected);
      assertTrue(blpConnector.isConnected());
      
      // send snapshot request
      Instrument instrument = instrumentFinder.getInstrumentByIsin(ISIN_CODE);
      tsMarketDataRequest = null;
      blpConnector.requestInstrumentPriceSnapshot(instrument , MARKET_MAKER_CODE);
      
      // Snapshot Request Verification
      assertNotNull(tsMarketDataRequest);
      assertEquals(1, tsMarketDataRequest.getMarketDepth().intValue());
      assertEquals(MDUpdateType.FullRefresh, tsMarketDataRequest.getMDUpdateType());
      assertEquals(SubscriptionRequestType.Snapshot, tsMarketDataRequest.getSubscriptionRequestType());
      assertEquals(1, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().size());
      assertEquals(ISIN_CODE, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getSecurityID());
      assertEquals(SecurityIDSource.IsinNumber, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getSecurityIDSource());
      assertEquals(1, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().size());
      assertEquals(MARKET_MAKER_CODE, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0).getInstrumentPartyID());
      assertEquals(InstrumentPartyIDSource.GenerallyAcceptedMarketPartecipantIdentifier, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0).getInstrumentPartyIDSource());
      assertEquals(InstrumentPartyRole.MarketMaker, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0).getInstrumentPartyRole());
      boolean bidSide = false;
      boolean askSide = false;
      assertEquals(2, tsMarketDataRequest.getTSMDReqGrp().getTSMDEntryTypesList().size());
      for(TSNoMDEntryTypes entry : tsMarketDataRequest.getTSMDReqGrp().getTSMDEntryTypesList()){
         if(entry.getMDEntryType() == MDEntryType.Bid){
            bidSide = true;
         }
         else if(entry.getMDEntryType() == MDEntryType.Offer){
            askSide = true;
         }
      }
      assertTrue(bidSide);
      assertTrue(askSide);
      
      // Send Snapshot Response
      bidProposal = askProposal = null;
      tsClient.sentSnapshotPrice(BID_PRICE, BID_QTY, ASK_PRICE, ASK_QTY, BID_UTC_TIME, ASK_UTC_TIME);
      //  Bid Proposal Response
      assertNotNull(bidProposal);
      assertEquals(MarketCode.BLOOMBERG, bidProposal.getMarket().getMarketCode());
      assertEquals(MARKET_MAKER_CODE, bidProposal.getMarketMarketMaker().getMarketSpecificCode());
      assertEquals(instrument.getCurrency(), bidProposal.getPrice().getStringCurrency());
      assertEquals(BID_PRICE, bidProposal.getPrice().getAmount().doubleValue(), 4);
      assertEquals( BID_QTY, bidProposal.getQty().doubleValue(), 4);
      assertEquals(ProposalState.NEW ,bidProposal.getProposalState());
      assertEquals(Proposal.ProposalSide.BID, bidProposal.getSide());
      assertEquals(ProposalType.TRADEABLE, bidProposal.getType());
      //  Ask Proposal Response
      assertNotNull(askProposal);
      assertEquals(MarketCode.BLOOMBERG, askProposal.getMarket().getMarketCode());
      assertEquals(MARKET_MAKER_CODE, askProposal.getMarketMarketMaker().getMarketSpecificCode());
      assertEquals(instrument.getCurrency(), askProposal.getPrice().getStringCurrency());
      assertEquals(ASK_PRICE, askProposal.getPrice().getAmount().doubleValue(), 4);
      assertEquals( ASK_QTY, askProposal.getQty().doubleValue(), 4);
      assertEquals(ProposalState.NEW ,askProposal.getProposalState());
      assertEquals(Proposal.ProposalSide.ASK, askProposal.getSide());
      assertEquals(ProposalType.TRADEABLE, askProposal.getType());
      
      blpConnector.disconnect();

      logger.info("End priceTest");
   }

   @Test
   public void nullPriceTest() throws BestXException, TradeStacException{
      logger.info("Begin nullPriceTest");
      marketStatus = ConnectionStatus.NotConnected;
      connStatus = ConnectionStatus.Connected;

      blpConnector.connect();
      tsClient.sendLogon(true);
      tsNetworkStatusRequest = null;
      
      tsClient.sendNetworkStatusResponse(true);
      assertTrue(marketStatus == ConnectionStatus.Connected);
      assertTrue(blpConnector.isConnected());
      
      // send snapshot request
      Instrument instrument = instrumentFinder.getInstrumentByIsin(ISIN_CODE);
      tsMarketDataRequest = null;
      blpConnector.requestInstrumentPriceSnapshot(instrument , MARKET_MAKER_CODE);
      
      // Snapshot Request Verification
      assertNotNull(tsMarketDataRequest);
      assertEquals(1, tsMarketDataRequest.getMarketDepth().intValue());
      assertEquals(MDUpdateType.FullRefresh, tsMarketDataRequest.getMDUpdateType());
      assertEquals(SubscriptionRequestType.Snapshot, tsMarketDataRequest.getSubscriptionRequestType());
      assertEquals(1, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().size());
      assertEquals(ISIN_CODE, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getSecurityID());
      assertEquals(SecurityIDSource.IsinNumber, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getSecurityIDSource());
      assertEquals(1, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().size());
      assertEquals(MARKET_MAKER_CODE, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0).getInstrumentPartyID());
      assertEquals(InstrumentPartyIDSource.GenerallyAcceptedMarketPartecipantIdentifier, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0).getInstrumentPartyIDSource());
      assertEquals(InstrumentPartyRole.MarketMaker, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0).getInstrumentPartyRole());
      boolean bidSide = false;
      boolean askSide = false;
      assertEquals(2, tsMarketDataRequest.getTSMDReqGrp().getTSMDEntryTypesList().size());
      for(TSNoMDEntryTypes entry : tsMarketDataRequest.getTSMDReqGrp().getTSMDEntryTypesList()){
         if(entry.getMDEntryType() == MDEntryType.Bid){
            bidSide = true;
         }
         else if(entry.getMDEntryType() == MDEntryType.Offer){
            askSide = true;
         }
      }
      assertTrue(bidSide);
      assertTrue(askSide);
      
      // Send Snapshot Response
      bidProposal = askProposal = null;
      tsClient.sentSnapshotPrice(ZERO_PRICE, BID_QTY, ZERO_PRICE, ASK_QTY, BID_UTC_TIME, ASK_UTC_TIME);
      //  Bid Proposal Response
      assertNotNull(bidProposal);
      assertEquals(MarketCode.BLOOMBERG, bidProposal.getMarket().getMarketCode());
      assertEquals(MARKET_MAKER_CODE, bidProposal.getMarketMarketMaker().getMarketSpecificCode());
      assertEquals(instrument.getCurrency(), bidProposal.getPrice().getStringCurrency());
      assertEquals(ZERO_PRICE, bidProposal.getPrice().getAmount().doubleValue(), 4);
      assertEquals(ZERO_QTY, bidProposal.getQty().doubleValue(), 4);
      assertEquals(ProposalState.NEW ,bidProposal.getProposalState());
      assertEquals(Proposal.ProposalSide.BID, bidProposal.getSide());
      assertEquals(ProposalType.INDICATIVE, bidProposal.getType());
      //  Ask Proposal Response
      assertNotNull(askProposal);
      assertEquals(MarketCode.BLOOMBERG, askProposal.getMarket().getMarketCode());
      assertEquals(MARKET_MAKER_CODE, askProposal.getMarketMarketMaker().getMarketSpecificCode());
      assertEquals(instrument.getCurrency(), askProposal.getPrice().getStringCurrency());
      assertEquals(ZERO_PRICE, askProposal.getPrice().getAmount().doubleValue(), 4);
      assertEquals(ZERO_QTY, askProposal.getQty().doubleValue(), 4);
      assertEquals(ProposalState.NEW ,askProposal.getProposalState());
      assertEquals(Proposal.ProposalSide.ASK, askProposal.getSide());
      assertEquals(ProposalType.INDICATIVE, askProposal.getType());
      
      blpConnector.disconnect();

      logger.info("End nullPriceTest");
   }
   
   @Test
   public void marketMakerNoMarketGrant() throws BestXException, TradeStacException{
      logger.info("Begin marketMakerNoMarketGrant");

      marketStatus = ConnectionStatus.NotConnected;
      connStatus = ConnectionStatus.Connected;

      blpConnector.connect();
      tsClient.sendLogon(true);
      tsNetworkStatusRequest = null;
      
      tsClient.sendNetworkStatusResponse(true);
      assertTrue(marketStatus == ConnectionStatus.Connected);
      assertTrue(blpConnector.isConnected());
      
      // send snapshot request
      Instrument instrument = instrumentFinder.getInstrumentByIsin(ISIN_CODE);
      tsMarketDataRequest = null;
      blpConnector.requestInstrumentPriceSnapshot(instrument , MARKET_MAKER_CODE);
      
      // Snapshot Request Verification
      assertNotNull(tsMarketDataRequest);
      assertEquals(1, tsMarketDataRequest.getMarketDepth().intValue());
      assertEquals(MDUpdateType.FullRefresh, tsMarketDataRequest.getMDUpdateType());
      assertEquals(SubscriptionRequestType.Snapshot, tsMarketDataRequest.getSubscriptionRequestType());
      assertEquals(1, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().size());
      assertEquals(ISIN_CODE, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getSecurityID());
      assertEquals(SecurityIDSource.IsinNumber, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getSecurityIDSource());
      assertEquals(1, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().size());
      assertEquals(MARKET_MAKER_CODE, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0).getInstrumentPartyID());
      assertEquals(InstrumentPartyIDSource.GenerallyAcceptedMarketPartecipantIdentifier, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0).getInstrumentPartyIDSource());
      assertEquals(InstrumentPartyRole.MarketMaker, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0).getInstrumentPartyRole());
      boolean bidSide = false;
      boolean askSide = false;
      assertEquals(2, tsMarketDataRequest.getTSMDReqGrp().getTSMDEntryTypesList().size());
      for(TSNoMDEntryTypes entry : tsMarketDataRequest.getTSMDReqGrp().getTSMDEntryTypesList()){
         if(entry.getMDEntryType() == MDEntryType.Bid){
            bidSide = true;
         }
         else if(entry.getMDEntryType() == MDEntryType.Offer){
            askSide = true;
         }
      }
      assertTrue(bidSide);
      assertTrue(askSide);
      
      // Send Snapshot Response
      bidProposal = askProposal = null;
      tsClient.sentMarketDataReject();
      //  Bid Proposal Response
      assertNotNull(bidProposal);
      assertEquals(MarketCode.BLOOMBERG, bidProposal.getMarket().getMarketCode());
      assertEquals(MARKET_MAKER_CODE, bidProposal.getMarketMarketMaker().getMarketSpecificCode());
      assertEquals(instrument.getCurrency(), bidProposal.getPrice().getStringCurrency());
      assertEquals(ZERO_PRICE, bidProposal.getPrice().getAmount().doubleValue(), 4);
      assertEquals(ZERO_QTY, bidProposal.getQty().doubleValue(), 4);
      assertEquals(ProposalState.REJECTED ,bidProposal.getProposalState());
      assertNotNull(bidProposal.getReason());
      assertEquals(Proposal.ProposalSide.BID, bidProposal.getSide());
      assertEquals(ProposalType.CLOSED, bidProposal.getType());
      //  Ask Proposal Response
      assertNotNull(askProposal);
      assertEquals(MarketCode.BLOOMBERG, askProposal.getMarket().getMarketCode());
      assertEquals(MARKET_MAKER_CODE, askProposal.getMarketMarketMaker().getMarketSpecificCode());
      assertEquals(instrument.getCurrency(), askProposal.getPrice().getStringCurrency());
      assertEquals(ZERO_PRICE, askProposal.getPrice().getAmount().doubleValue(), 4);
      assertEquals(ZERO_QTY, askProposal.getQty().doubleValue(), 4);
      assertEquals(ProposalState.REJECTED ,askProposal.getProposalState());
      assertNotNull(askProposal.getReason());
      assertEquals(Proposal.ProposalSide.ASK, askProposal.getSide());
      assertEquals(ProposalType.CLOSED, askProposal.getType());
      
      blpConnector.disconnect();

      logger.info("End marketMakerNoMarketGrant");
   }
   
   @Test 
   public void marketMakerNotAvailable() throws BestXException, TradeStacException{
      logger.info("Begin marketMakerNotAvailable");

      marketStatus = ConnectionStatus.NotConnected;
      connStatus = ConnectionStatus.Connected;

      blpConnector.connect();
      tsClient.sendLogon(true);
      tsNetworkStatusRequest = null;
      
      tsClient.sendNetworkStatusResponse(true);
      assertTrue(marketStatus == ConnectionStatus.Connected);
      assertTrue(blpConnector.isConnected());
      
      // send snapshot request
      Instrument instrument = instrumentFinder.getInstrumentByIsin(ISIN_CODE);
      tsMarketDataRequest = null;
      blpConnector.requestInstrumentPriceSnapshot(instrument , MARKET_MAKER_UNKNOWN);
      
      // Snapshot Request Verification
      assertNotNull(tsMarketDataRequest);
      assertEquals(1, tsMarketDataRequest.getMarketDepth().intValue());
      assertEquals(MDUpdateType.FullRefresh, tsMarketDataRequest.getMDUpdateType());
      assertEquals(SubscriptionRequestType.Snapshot, tsMarketDataRequest.getSubscriptionRequestType());
      assertEquals(1, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().size());
      assertEquals(ISIN_CODE, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getSecurityID());
      assertEquals(SecurityIDSource.IsinNumber, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getSecurityIDSource());
      assertEquals(1, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().size());
      assertEquals(MARKET_MAKER_UNKNOWN, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0).getInstrumentPartyID());
      assertEquals(InstrumentPartyIDSource.GenerallyAcceptedMarketPartecipantIdentifier, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0).getInstrumentPartyIDSource());
      assertEquals(InstrumentPartyRole.MarketMaker, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0).getInstrumentPartyRole());
      boolean bidSide = false;
      boolean askSide = false;
      assertEquals(2, tsMarketDataRequest.getTSMDReqGrp().getTSMDEntryTypesList().size());
      for(TSNoMDEntryTypes entry : tsMarketDataRequest.getTSMDReqGrp().getTSMDEntryTypesList()){
         if(entry.getMDEntryType() == MDEntryType.Bid){
            bidSide = true;
         }
         else if(entry.getMDEntryType() == MDEntryType.Offer){
            askSide = true;
         }
      }
      assertTrue(bidSide);
      assertTrue(askSide);

      // Send Snapshot Response
      bidProposal = askProposal = null;
      tsClient.sentSnapshotPrice(BID_PRICE, BID_QTY, ASK_PRICE, ASK_QTY, BID_UTC_TIME, ASK_UTC_TIME);
      //  Bid Proposal Response
      assertNull(bidProposal);
      assertNull(askProposal);

      blpConnector.disconnect();

      logger.info("End marketMakerNotAvailable");
   }
 
   @Test
   public void receiveOnBusinessMessageReject() throws BestXException, TradeStacException{
      logger.info("Begin receiveOnBusinessMessageReject");

      marketStatus = ConnectionStatus.NotConnected;
      connStatus = ConnectionStatus.Connected;

      blpConnector.connect();
      tsClient.sendLogon(true);
      tsNetworkStatusRequest = null;
      
      tsClient.sendNetworkStatusResponse(false);
      assertTrue(marketStatus == ConnectionStatus.NotConnected);
      assertTrue(blpConnector.isConnected());
      
      // send snapshot request
      Instrument instrument = instrumentFinder.getInstrumentByIsin(ISIN_CODE);
      tsMarketDataRequest = null;
      blpConnector.requestInstrumentPriceSnapshot(instrument , MARKET_MAKER_CODE);
      
      // Snapshot Request Verification
      assertNotNull(tsMarketDataRequest);
      assertEquals(1, tsMarketDataRequest.getMarketDepth().intValue());
      assertEquals(MDUpdateType.FullRefresh, tsMarketDataRequest.getMDUpdateType());
      assertEquals(SubscriptionRequestType.Snapshot, tsMarketDataRequest.getSubscriptionRequestType());
      assertEquals(1, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().size());
      assertEquals(ISIN_CODE, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getSecurityID());
      assertEquals(SecurityIDSource.IsinNumber, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getSecurityIDSource());
      assertEquals(1, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().size());
      assertEquals(MARKET_MAKER_CODE, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0).getInstrumentPartyID());
      assertEquals(InstrumentPartyIDSource.GenerallyAcceptedMarketPartecipantIdentifier, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0).getInstrumentPartyIDSource());
      assertEquals(InstrumentPartyRole.MarketMaker, tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0).getInstrumentPartyRole());
      boolean bidSide = false;
      boolean askSide = false;
      assertEquals(2, tsMarketDataRequest.getTSMDReqGrp().getTSMDEntryTypesList().size());
      for(TSNoMDEntryTypes entry : tsMarketDataRequest.getTSMDReqGrp().getTSMDEntryTypesList()){
         if(entry.getMDEntryType() == MDEntryType.Bid){
            bidSide = true;
         }
         else if(entry.getMDEntryType() == MDEntryType.Offer){
            askSide = true;
         }
      }
      assertTrue(bidSide);
      assertTrue(askSide);
      
      // Send Snapshot Response
      bidProposal = askProposal = null;
      tsClient.sentBusinessMessageReject();
      //  Bid Proposal Response
      assertNull(bidProposal);
      assertNull(askProposal);

      blpConnector.disconnect();

      logger.info("End receiveOnBusinessMessageReject");
   }
   
   static class MyBLPConnectionListener implements TradeStacPreTradeConnectionListener{

      @Override
      public void onClassifiedProposal(Instrument instrument, ClassifiedProposal askClassifiedProposal,
            ClassifiedProposal bidClassifiedProposal) {
         askProposal = askClassifiedProposal;
         bidProposal = bidClassifiedProposal;
      }

      @Override
      public void onClientConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
         connStatus = connectionStatus;
      }

      @Override
      public void onMarketConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
         marketStatus = connectionStatus;
      }

	@Override
	public void onSecurityListCompleted(Set<String> securityList) {
	}
      
   }
   
   public static class MyBogusTSClientListener implements BogusTradeStacClient.BogusTSClientListener{

      @Override
      public void onInit(boolean result)
      {
         // TODO Auto-generated method stub
         
      }

      @Override
      public void onStart(boolean result)
      {
         // TODO Auto-generated method stub
         
      }

      @Override
      public void onNetworkStatusRequest(
            TSNetworkCounterpartySystemStatusRequest tsNetworkCounterpartySystemStatusRequest)
      {
         tsNetworkStatusRequest = tsNetworkCounterpartySystemStatusRequest;
      }

      @Override
      public void onMarketDataRequest(TSMarketDataRequest tsMktDataRequest){
         tsMarketDataRequest = tsMktDataRequest;
      }

      @Override
      public void onStop(boolean result)
      {
         // TODO Auto-generated method stub
         
      }
      
   }
}

