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
package it.softsolutions.bestx.connections.tradeweb.tradexpress;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.tradestac.AbstractTradeStacConnection;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.MarketMarketMakerSpec;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.tradestac.api.TradeStacException;
import it.softsolutions.tradestac.client.TradeStacClientSession;
import it.softsolutions.tradestac.fix.field.Currency;
import it.softsolutions.tradestac.fix.field.HandlInst;
import it.softsolutions.tradestac.fix.field.OrdType;
import it.softsolutions.tradestac.fix.field.PartyIDSource;
import it.softsolutions.tradestac.fix.field.PartyRole;
import it.softsolutions.tradestac.fix.field.PartyRoleQualifier;
import it.softsolutions.tradestac.fix.field.PriceType;
import it.softsolutions.tradestac.fix.field.SecurityIDSource;
import it.softsolutions.tradestac.fix.field.SettlType;
import it.softsolutions.tradestac.fix.field.Side;
import it.softsolutions.tradestac.fix.field.TimeInForce;
import it.softsolutions.tradestac.fix50.TSBusinessMessageReject;
import it.softsolutions.tradestac.fix50.TSExecutionReport;
import it.softsolutions.tradestac.fix50.TSNewOrderSingle;
import it.softsolutions.tradestac.fix50.TSNoPartyID;
import it.softsolutions.tradestac.fix50.TSOrderCancelReject;
import it.softsolutions.tradestac.fix50.TSOrderCancelRequest;
import it.softsolutions.tradestac.fix50.component.TSInstrument;
import it.softsolutions.tradestac.fix50.component.TSOrderQtyData;
import it.softsolutions.tradestac.fix50.component.TSParties;
import it.softsolutions.tradestac.tw.fix.component.BlockedDealersGrpComponent;
import it.softsolutions.tradestac.tw.fix.component.BlockedDealersGrpComponent.NoBlockedDealers;
import quickfix.ConfigError;
import quickfix.Field;
import quickfix.MessageComponent;
import quickfix.SessionID;
import quickfix.field.ExecType;
import tw.quickfix.field.BlockedDealer;
import tw.quickfix.field.ClientTradingCapacity;
import tw.quickfix.field.ShortSellingIndicator;
import tw.quickfix.field.TradingMode;

/*
 *
 * Purpose: this class is mainly for ...
 *
 * Project Name : bestx-tradeweb-market First created by: davide.rossoni
 * Creation date: 19/dec/2014
 * 
 **/
@SuppressWarnings("deprecation")
public class TradeXpressConnectionImpl extends AbstractTradeStacConnection implements TradeXpressConnection {

   private static final Logger LOGGER = LoggerFactory.getLogger(TradeXpressConnectionImpl.class);
   private TradeXpressConnectionListener tradeXpressConnectionListener;
   //	private TradeStacClientSession tradeStacClientSession;
   @SuppressWarnings("unused")
   private static String eurogvCountries[] = { "CH", "DK", "NO", "SE", "IT", "DE", "FR", "IE", "LU", "BE", "NL", "SP", "PT", "GR", "AT", "FI", "UK" };
   private String traderCode = null;
   private String bestxAlgoID = null;
   private String defaultTradingMode = null;
   private int defaultShortSelling = 0;
   private Character defaultTradingCapacity = null;
   private String investmentDecisorID = null;
   private String investmentDecisorRoleQualifier = null;
   private boolean addBlockedDealers = false;
   private boolean addIncludeDealers = false;
   private int blockedDealersMaxNum = 0;
   private int includeDealersMaxNum = 0;
   private char handlInstr = '3';

   public char getHandlInstr() {
	return handlInstr;
}

   public void setHandlInstr(char handlInstr) {
	   this.handlInstr = handlInstr;
   }

   public boolean isAddIncludeDealers() {
	   return addIncludeDealers;
   }

   public void setAddIncludeDealers(boolean addIncludeDealers) {
	   this.addIncludeDealers = addIncludeDealers;
   }

   public int getBlockedDealersMaxNum() {
	   return blockedDealersMaxNum;
   }

   public void setBlockedDealersMaxNum(int blockedDealersMaxNum) {
	   this.blockedDealersMaxNum = blockedDealersMaxNum;
   }

   public int getIncludeDealersMaxNum() {
	   return includeDealersMaxNum;
   }

   public void setIncludeDealersMaxNum(int includeDealersMaxNum) {
	   this.includeDealersMaxNum = includeDealersMaxNum;
   }
   
   public boolean isAddBlockedDealers() {
      return addBlockedDealers;
   }

   public void setAddBlockedDealers(boolean addBlockedDealers) {
      this.addBlockedDealers = addBlockedDealers;
   }

   public String getTraderCode() {
      return traderCode;
   }

   public void setTraderCode(String traderCode) {
      this.traderCode = traderCode;
   }

   public TradeXpressConnectionListener getTradeXpressConnectionListener() {
      return tradeXpressConnectionListener;
   }

   @Override
   public void setTradeXpressConnectionListener(TradeXpressConnectionListener tradeXpressConnectionListener) {
      this.tradeXpressConnectionListener = tradeXpressConnectionListener;
   }

   @Override
   public TradeStacClientSession getTradeStacClientSession() {
      return tradeStacClientSession;
   }

   @Override
   public void setTradeStacClientSession(TradeStacClientSession tradeStacClientSession) {
      this.tradeStacClientSession = tradeStacClientSession;
   }

   /**
     * Instantiates a new tSOX connection impl.
     */
   public TradeXpressConnectionImpl(){
      super("TradeXpress");
   }

   /**
    * Initializes a newly created {@link TradeXpressConnectionImpl}.
    *
    * @throws TradeStacException
    *             if an error occurred in the FIX connection initialization
    * @throws BestXException
    *             if an error occurred
    * @throws ConfigError when the configuration is notr correct
    */
   @Override
   public void init() throws TradeStacException, BestXException, ConfigError {
      super.init();
   }

   @Override
   public void onExecutionReport(SessionID sessionID, TSExecutionReport tsExecutionReport) throws TradeStacException {
      LOGGER.debug("{}, {}", sessionID, tsExecutionReport);
      // [DR20131126]
      //        if (tsExecutionReport.getExecType() == ExecType.New) {
      //            LOGGER.info("ExecutionReport with execType = {} has been ignored: {} {}", tsExecutionReport.getExecType(), tsExecutionReport, sessionID);
      //            return;
      //        }

      char execType = tsExecutionReport.getExecType().getFIXValue();
      // when RFQ with traders has been closed for any reason, free the trader pool counter  
      if (ExecType.REJECTED == execType) {
         this.getTradeXpressConnectionListener().onOrderReject(sessionID.toString(), tsExecutionReport.getClOrdID(), tsExecutionReport.getText());
      }
      else {
         // ClordID : is the ClOrdID sent by BestX in QuoteResponse ExecID : is the dealer's contract number
         this.getTradeXpressConnectionListener().onExecutionReport(sessionID.toString(), tsExecutionReport.getClOrdID(), tsExecutionReport);
      }
   }

   @Override
   public void onOrderCancelReject(SessionID sessionID, TSOrderCancelReject tsOrderCancelReject) throws TradeStacException {
      LOGGER.debug("sessionID = {}, tsOrderCancelReject = {}", sessionID, tsOrderCancelReject);

      this.getTradeXpressConnectionListener().onOrderCancelReject(sessionID.toString(), tsOrderCancelReject.getOrigClOrdID(), tsOrderCancelReject.getText());
   }

   /*
    * At the moment is not possible to notify the request is failed: it has not
    * the information to create the reject classifiedProposal. It is need to
    * store the request in the map.
    */
   @Override
   public void onBusinessMessageReject(SessionID sessionID, TSBusinessMessageReject tsBusinessMessageReject) throws TradeStacException {
      LOGGER.error("{}, {}", sessionID, tsBusinessMessageReject);
      String requestID = "" + tsBusinessMessageReject.getRefSeqNum();
      this.getTradeXpressConnectionListener().onOrderReject(sessionID.toString(), requestID, tsBusinessMessageReject.getText());
   }

   private TSNewOrderSingle createTSNewOrderSingle(MarketOrder marketOrder) {
      if (marketOrder == null || marketOrder.getInstrument() == null) {
         throw new IllegalArgumentException("Params can't be null");
      }
      TSNewOrderSingle tsNewOrderSingle = new TSNewOrderSingle();

      // MiFID II related data in custom tags
      // onMTF (22635) //marketOrder.setMifidRestricted(true);
      int tradingMode = Integer.parseInt(defaultTradingMode); // got from configuration
      if (marketOrder.isMiFIDRestricted() != null && !marketOrder.isMiFIDRestricted())
         tradingMode = TradingMode.OFF_MTF; //"offMTF";
      // removed for BESTX-395
      //        else if(marketOrder.isMiFIDRestricted() != null && marketOrder.isMiFIDRestricted())
      //        	tradingMode = TradingMode.ON_MTF; //"onMTF";
      List<Field<?>> customFields = new ArrayList<Field<?>>();
      customFields.add(new TradingMode(tradingMode));

      // trading capacity
      // ClientTradingCapacity (23082)
      if (tradingMode == TradingMode.ON_MTF || tradingMode == TradingMode.ON_EUMTF) { // BESTX-395
         Character clientTradingCapacity = TradewebDataHelper.convertTradingCapacity(marketOrder);
         if (clientTradingCapacity == null)
            clientTradingCapacity = defaultTradingCapacity; //defaultTradingCapacity='P';
         customFields.add(new ClientTradingCapacity(clientTradingCapacity));
         // ShortSellingIndicator (23066)
         int shortSellInd = TradewebDataHelper.convertShortSellIndicator(marketOrder, defaultShortSelling);
         // put it in the order only if it is > 0 (i.e. has to be attached to the order)
         if (shortSellInd > 0) {
            customFields.add(new ShortSellingIndicator(shortSellInd));
         }
         tsNewOrderSingle.setCustomFields(customFields);
      }

      // Previous data
      String clOrdID = marketOrder.getMarketSessionId();
      Instrument instrument = marketOrder.getInstrument();
      String securityID = instrument.getIsin();
      Side side = Side.getInstanceForFIXValue(marketOrder.getSide().getFixCode().charAt(0));
      Double orderQty = marketOrder.getQty().doubleValue();
      Date settlDate = marketOrder.getFutSettDate();
      String settlementType = marketOrder.getSettlementType();
      OrdType ordType = OrdType.Market;
      if (marketOrder.getLimit() != null) {
         ordType = OrdType.Limit;
         Double price = marketOrder.getLimit().getAmount().doubleValue();
         tsNewOrderSingle.setPrice(price);
         tsNewOrderSingle.setPriceType(PriceType.Percentage);
      }
      else {
         ordType = OrdType.Market;
      }
      String dealerCode = marketOrder.getMarketMarketMaker() != null ? marketOrder.getMarketMarketMaker().getMarketSpecificCode() : null;
      TSInstrument tsInstrument = new TSInstrument();
      tsInstrument.setSymbol("N/A");
      tsInstrument.setSecurityID(securityID);
      tsInstrument.setSecurityIDSource(SecurityIDSource.IsinNumber);

      tsNewOrderSingle.setTSInstrument(tsInstrument);
      tsNewOrderSingle.setHandlInst(HandlInst.getInstanceForFIXValue(this.handlInstr));  // TODO BESTX-891 AMC Needs to be amended to a new value 'X' or to a configurable value?
      tsNewOrderSingle.setClOrdID(clOrdID);
      tsNewOrderSingle.setSide(side);
      tsNewOrderSingle.setTransactTime(DateService.newLocalDate());
      tsNewOrderSingle.setOrdType(ordType);
      tsNewOrderSingle.setSettlDate(settlDate);
      tsNewOrderSingle.setSettlType(SettlType.getInstanceForFIXValue(settlementType));

      TSOrderQtyData tsOrderQtyData = new TSOrderQtyData();
      tsOrderQtyData.setOrderQty(orderQty);

      tsNewOrderSingle.setTSOrderQtyData(tsOrderQtyData);

      tsNewOrderSingle.setTimeInForce(TimeInForce.GoodTillDate);
      tsNewOrderSingle.setCurrency(Currency.EUR);

      // ## TraderCode ####
      TSNoPartyID tsNoPartyTrader = new TSNoPartyID();
      tsNoPartyTrader.setPartyID(this.traderCode); // increments
      tsNoPartyTrader.setPartyIDSource(PartyIDSource.GenerallyAcceptedMarketPartecipantIdentifier);
      tsNoPartyTrader.setPartyRole(PartyRole.OrderOriginationTrader);

      // ## Dealer with best price. Where there is none, no dealer and no limit price are specified and order type is Market
      TSNoPartyID tsNoPartyBestDealer = null;
      if (dealerCode != null) {
         String partID = dealerCode;
         tsNoPartyBestDealer = new TSNoPartyID();
         tsNoPartyBestDealer.setPartyID(partID);
         tsNoPartyBestDealer.setPartyIDSource(PartyIDSource.BIC);
         tsNoPartyBestDealer.setPartyRole(PartyRole.ExecutingFirm);
      }
      List<TSNoPartyID> tsNoPartyIDsList = new ArrayList<TSNoPartyID>();
      if (tradingMode == TradingMode.ON_MTF || tradingMode == TradingMode.ON_EUMTF) {
         // ## Execution within firm #### is BESTX
         TSNoPartyID tsNoPartyExecutionWithinFirm = new TSNoPartyID();
         tsNoPartyExecutionWithinFirm.setPartyID(this.bestxAlgoID); // increments
         tsNoPartyExecutionWithinFirm.setPartyIDSource(PartyIDSource.ProprietaryCustomCode);
         tsNoPartyExecutionWithinFirm.setPartyRole(PartyRole.ExecutingTrader);
         tsNoPartyExecutionWithinFirm.setPartyRoleQualifier(PartyRoleQualifier.Algorithm);

         // ## TraderCode ####  
         TSNoPartyID tsNoPartyInvestmentDecisor = new TSNoPartyID();
         tsNoPartyInvestmentDecisor.setPartyID(this.getInvestmentDecisorID()); // increments
         tsNoPartyInvestmentDecisor.setPartyIDSource(PartyIDSource.ShortCodeIdentifier);
         tsNoPartyInvestmentDecisor.setPartyRole(PartyRole.InvestmentDecisionMaker);
         tsNoPartyInvestmentDecisor.setPartyRoleQualifier(PartyRoleQualifier.getInstanceForFIXValue(Integer.parseInt(getInvestmentDecisorRoleQualifier())));

         tsNoPartyIDsList.add(tsNoPartyExecutionWithinFirm);
         tsNoPartyIDsList.add(tsNoPartyInvestmentDecisor);
      }
      if (tsNoPartyBestDealer != null)
         tsNoPartyIDsList.add(tsNoPartyBestDealer);
      tsNoPartyIDsList.add(tsNoPartyTrader);

      // BESTX-891 manage here the include/exclude dealers. Use configurable attributes
      // Add in the parties group all dealers in marketOrder.getDealers() with partyrole=1
      if(isAddIncludeDealers()) {
    	  for(int i = 0; i < marketOrder.getDealers().size() && i < includeDealersMaxNum; i++) {
		      TSNoPartyID inclDealer = new TSNoPartyID();
		      inclDealer.setPartyID(marketOrder.getDealers().get(i).getMarketMakerMarketSpecificCode());
		      inclDealer.setPartyIDSource(PartyIDSource.getInstanceForFIXValue(
		    		  marketOrder.getDealers().
		    		  	get(i).
		    		  		getMarketMakerMarketSpecificCodeSource().charAt(0)));
		      inclDealer.setPartyRole(PartyRole.ExecutingFirm);
		      tsNoPartyIDsList.add(inclDealer);
    	  }
    	  
      }
      TSParties tsParties = new TSParties();
      tsParties.setTSNoPartyIDsList(tsNoPartyIDsList);

      tsNewOrderSingle.setTSParties(tsParties);

      /** Get Custom Components */
      List<MessageComponent> customComponents = new ArrayList<MessageComponent>();
      //BESTX-375: SP-20190122 add blocked dealers custom group to new order single message
      if (addBlockedDealers) { // add management of max number of dealers
         BlockedDealersGrpComponent blockedDealersGrpCmp = new BlockedDealersGrpComponent();
         tw.quickfix.field.NoBlockedDealers noBlockedDealers = new tw.quickfix.field.NoBlockedDealers();

         if (marketOrder.getExcludeDealers().size() > 0) {
            noBlockedDealers.setValue(Integer.min(marketOrder.getExcludeDealers().size(), getBlockedDealersMaxNum()));
            blockedDealersGrpCmp.set(noBlockedDealers);

            for (int i = 0; i < marketOrder.getExcludeDealers().size() && i < getBlockedDealersMaxNum(); i++) {
            	MarketMarketMakerSpec blockedDealer = marketOrder.getExcludeDealers().get(i);
            	NoBlockedDealers blockedDealersGrp = new NoBlockedDealers();
            	BlockedDealer blckDealer = new BlockedDealer();
            	blckDealer.setValue(blockedDealer.getMarketMakerMarketSpecificCode());
            	blockedDealersGrp.set(blckDealer);
            	blockedDealersGrpCmp.addGroup(blockedDealersGrp);
            }
            customComponents.add(blockedDealersGrpCmp);
         }
      }

      if (!customComponents.isEmpty()) {
         tsNewOrderSingle.setCustomComponents(customComponents);
      }

      LOGGER.info("{}", tsNewOrderSingle);

      return tsNewOrderSingle;
   }

   @Override
   public void sendOrder(MarketOrder marketOrder) throws BestXException {
      LOGGER.debug("marketOrder = {}", marketOrder);

      if (!isConnected()) {
         throw new BestXException("Not connected");
      }

      TSNewOrderSingle tsNewOrderSingle = createTSNewOrderSingle(marketOrder);

      try {
         this.getTradeStacClientSession().manageNewOrderSingle(tsNewOrderSingle);
      }
      catch (TradeStacException e) {
         throw new BestXException(String.format("Error managing newOrderSingle [%s]", tsNewOrderSingle), e);
      }
   }

   @Override
   public void cancelOrder(MarketOrder marketOrder) throws BestXException {
      LOGGER.debug("marketOrder = {}", marketOrder);

      if (!isConnected()) {
         throw new BestXException("Not connected");
      }

      Side side = Side.getInstanceForFIXValue(marketOrder.getSide().getFixCode().charAt(0));
      String origClOrdID = marketOrder.getMarketSessionId();
      String securityID = marketOrder.getInstrument().getIsin();
      Double orderQty = marketOrder.getQty().doubleValue();

      TSOrderCancelRequest tsOrderCancelRequest = new TSOrderCancelRequest();
      tsOrderCancelRequest.setClOrdID("OCR#" + origClOrdID); // ATTENZIONE; non posso cancellare più di 1 volta
      tsOrderCancelRequest.setOrigClOrdID(origClOrdID);
      tsOrderCancelRequest.setSide(side);
      tsOrderCancelRequest.setTransactTime(DateService.newLocalDate());

      TSOrderQtyData tsOrderQtyData = new TSOrderQtyData();
      tsOrderQtyData.setOrderQty(orderQty);
      tsOrderCancelRequest.setTSOrderQtyData(tsOrderQtyData);

      TSInstrument tsInstrument = new TSInstrument();
      tsInstrument.setSymbol("N/A");
      tsInstrument.setSecurityID(securityID);
      tsInstrument.setSecurityIDSource(SecurityIDSource.IsinNumber);
      tsOrderCancelRequest.setTSInstrument(tsInstrument);

      try {
         this.getTradeStacClientSession().manageOrderCancelRequest(tsOrderCancelRequest);
      }
      catch (TradeStacException e) {
         throw new BestXException(String.format("Error managing orderCancelRequest [%s]", tsOrderCancelRequest), e);
      }
   }

   public String getBestxAlgoID() {
      return bestxAlgoID;
   }

   public void setBestxAlgoID(String bestxAlgoID) {
      this.bestxAlgoID = bestxAlgoID;
   }

   public String getDefaultTradingMode() {
      return defaultTradingMode;
   }

   public void setDefaultTradingMode(String defaultTradingMode) {
      this.defaultTradingMode = defaultTradingMode;
   }

   public int getDefaultShortSelling() {
      return defaultShortSelling;
   }

   public void setDefaultShortSelling(int defaultShortSelling) {
      this.defaultShortSelling = defaultShortSelling;
   }

   public Character getDefaultTradingCapacity() {
      return defaultTradingCapacity;
   }

   public void setDefaultTradingCapacity(Character defaultTradingCapacity) {
      this.defaultTradingCapacity = defaultTradingCapacity;
   }

   public String getInvestmentDecisorID() {
      return investmentDecisorID;
   }

   public void setInvestmentDecisorID(String investmentDecisorID) {
      this.investmentDecisorID = investmentDecisorID;
   }

   public String getInvestmentDecisorRoleQualifier() {
      return investmentDecisorRoleQualifier;
   }

   public void setInvestmentDecisorRoleQualifier(String investmentDecisorRoleQualifier) {
      this.investmentDecisorRoleQualifier = investmentDecisorRoleQualifier;
   }
}
