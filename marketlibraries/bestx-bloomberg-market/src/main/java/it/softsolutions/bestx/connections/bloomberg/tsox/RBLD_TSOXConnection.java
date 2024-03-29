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
package it.softsolutions.bestx.connections.bloomberg.tsox;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.connections.TradeStacConnectorMBean;
import it.softsolutions.bestx.connections.tradestac.AbstractTradeStacConnection;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketMarketMakerSpec;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.tradestac.api.TradeStacException;
import it.softsolutions.tradestac.client.TradeStacClientSession;
import it.softsolutions.tradestac.fix.field.Currency;
import it.softsolutions.tradestac.fix.field.ExecType;
import it.softsolutions.tradestac.fix.field.HandlInst;
import it.softsolutions.tradestac.fix.field.MsgType;
import it.softsolutions.tradestac.fix.field.OrdType;
import it.softsolutions.tradestac.fix.field.OrderCapacity;
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
import it.softsolutions.tradestac.fix50.TSQuoteRequestReject;
import it.softsolutions.tradestac.fix50.component.TSInstrument;
import it.softsolutions.tradestac.fix50.component.TSOrderQtyData;
import it.softsolutions.tradestac.fix50.component.TSParties;
import quickfix.ConfigError;
import quickfix.Field;
import quickfix.SessionID;

/**  
 *
 * Purpose: this class manages protocol of directed enquiries through TSOX on Bloomberg Multiasset protocol 
 *
 * Project Name : bestx-bloomberg-market 
 * First created by: anna.cochetti 
 * Creation date: 28/04/2019 
 * 
 **/
@SuppressWarnings("deprecation")
public class RBLD_TSOXConnection extends AbstractTradeStacConnection implements TSOXConnection, TradeStacConnectorMBean {

   private static final Logger LOGGER = LoggerFactory.getLogger(RBLD_TSOXConnection.class);

   private TSOXConnectionListener tsoxConnectionListener;

   private String traderCode;
   private String destinationMICCode = "BMTF";

   private boolean addBlockedDealers = false;
   private boolean addIncludeDealers = false;
   private int blockedDealersMaxNum = 1000;
   private int includeDealersMaxNum = 1000;

   public boolean isAddBlockedDealers() {
	   return addBlockedDealers;
   }

   public void setAddBlockedDealers(boolean addBlockedDealers) {
	   this.addBlockedDealers = addBlockedDealers;
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

   /**
   * @return the destinationMICCode
   */
   public String getDestinationMICCode() {
      return destinationMICCode;
   }

   /**
    * @param destinationMICCode the destinationMICCode to set
    */
   public void setDestinationMICCode(String destinationMICCode) {
      this.destinationMICCode = destinationMICCode;
   }

   private String investmentDecisionMakerID;
   private String investmentDecisionQualifier;

   /**
   * @return the traderCode
   */
   public String getTraderCode() {
      return traderCode;
   }

   /**
    * @param traderCode the traderCode to set
    */
   public void setTraderCode(String traderCode) {
      this.traderCode = traderCode;
   }

   /**
    * @return the investmentDecisionMakerID
    */
   public String getInvestmentDecisionMakerID() {
      return investmentDecisionMakerID;
   }

   /**
    * @param investmentDecisionMakerID the investmentDecisionMakerID to set
    */
   public void setInvestmentDecisionMakerID(String investmentDecisionMakerID) {
      this.investmentDecisionMakerID = investmentDecisionMakerID;
   }

   /**
    * @return the investmentDecisionQualifier
    */
   public String getInvestmentDecisionQualifier() {
      return investmentDecisionQualifier;
   }

   /**
    * @param investmentDecisionQualifier the investmentDecisionQualifier to set
    */
   public void setInvestmentDecisionQualifier(String investmentDecisionQualifier) {
      this.investmentDecisionQualifier = investmentDecisionQualifier;
   }

   /**
    * @return the executionDecisionMakerID
    */
   public String getExecutionDecisionMakerID() {
      return executionDecisionMakerID;
   }

   /**
    * @param executionDecisionMakerID the executionDecisionMakerID to set
    */
   public void setExecutionDecisionMakerID(String executionDecisionMakerID) {
      this.executionDecisionMakerID = executionDecisionMakerID;
   }

   private String executionDecisionMakerID;

   // tsoxEnquiryTime: maximum validity time of orders (in seconds) 
   private long tsoxEnquiryTime;

   /**
    * Gets the tsox EnquiryTime.
    *
    * @return the tsox EnquiryTime
    */
   public long getTsoxEnquiryTime() {
      return tsoxEnquiryTime;
   }

   /**
    * Sets the tsox EnquiryTime.
    *
    * @param tsoxEnquiryTime the new tsox EnquiryTime
    */
   public void setTsoxEnquiryTime(long tsoxEnquirytime) {
      this.tsoxEnquiryTime = tsoxEnquirytime;
   }

   /**
    * Gets the tsox tradercode.
    *
    * @return the tsox tradercode
    */
   public String getTsoxTradercode() {
      return getTraderCode();
   }

   /**
    * Sets the tsox tradercode.
    *
    * @param tsoxTradercode the new tsox tradercode
    */
   public void setTsoxTradercode(String tsoxTradercode) {
      this.traderCode = tsoxTradercode;
   }

   private OrderCapacity defaultCapacity = OrderCapacity.Principal;

   public String getDefaultCapacity() {
      return "" + defaultCapacity.getFIXValue();
   }

   public void setDefaultCapacity(String defaultCapacity) {
      this.defaultCapacity = OrderCapacity.getInstanceForFIXValue(defaultCapacity.trim().charAt(0));
   }

   private String enteringFirmCode;

   public String getEnteringFirmCode() {
      return enteringFirmCode;
   }

   public void setEnteringFirmCode(String enteringFirmCode) {
      this.enteringFirmCode = enteringFirmCode;
   }

   private PartyRoleQualifier investmentDecisorQualifier;

   public PartyRoleQualifier getInvestmentDecisorQualifier() {
      return investmentDecisorQualifier;
   }

   public void setInvestmentDecisorQualifier(PartyRoleQualifier investmentDecisorQualifier) {
      this.investmentDecisorQualifier = investmentDecisorQualifier;
   }

   /**
     * Instantiates a new tSOX connection impl.
     */
   public RBLD_TSOXConnection(){
      super(Market.MarketCode.BLOOMBERG + "#tsox");
   }

   @Override
   public void setTsoxConnectionListener(TSOXConnectionListener tsoxConnectionListener) {
      LOGGER.debug("{}", tsoxConnectionListener);

      this.tsoxConnectionListener = tsoxConnectionListener;
      super.setTradeStacConnectionListener(tsoxConnectionListener);
   }

   @Override
   public void onExecutionReport(SessionID sessionID, TSExecutionReport tsExecutionReport) throws TradeStacException {
      LOGGER.debug("{}, {}", sessionID, tsExecutionReport);

      //Check if the exec type (150) is NEW
      if (tsExecutionReport.getExecType() == ExecType.New) {
            LOGGER.info("ExecutionReport with execType = {} has been ignored: {} {}", tsExecutionReport.getExecType(), tsExecutionReport, sessionID);
            return;
      }

      /**
       * 
       * ClordID : is the ClOrdID sent by BestX in QuoteResponse
       * ExecID  : is the dealer's contract number
       * 
       */
      char execType = tsExecutionReport.getExecType().getFIXValue();
      // when RFQ with traders has been closed for any reason, free the trader pool counter  
      if (quickfix.field.ExecType.ORDER_STATUS == execType) {
         tsoxConnectionListener.onExecutionStatus(sessionID.toString(), tsExecutionReport.getClOrdID(), tsExecutionReport);
      }
      else if (quickfix.field.ExecType.REJECTED == execType) {
         tsoxConnectionListener.onOrderReject(sessionID.toString(), tsExecutionReport.getClOrdID(), tsExecutionReport.getText());
      }
      else if(quickfix.field.ExecType.CANCELED == execType && tsExecutionReport.getOrigClOrdID() != null) {
    	  tsoxConnectionListener.onExecutionReport(sessionID.toString(), tsExecutionReport.getOrigClOrdID(), tsExecutionReport);
      }
      else {
         tsoxConnectionListener.onExecutionReport(sessionID.toString(), tsExecutionReport.getClOrdID(), tsExecutionReport);
      }
   }

   /*
    * At the moment is not possible to notify the request is failed: it has not the information to create the reject classifiedProposal. It
    * is need to store the request in the map.
    */
   @Override
   public void onBusinessMessageReject(SessionID sessionID, TSBusinessMessageReject tsBusinessMessageReject) throws TradeStacException {
      LOGGER.info("{}, {}", sessionID, tsBusinessMessageReject);

      // check if the message is requiring the resend of the original message
//      switch (tsBusinessMessageReject.getBusinessRejectReason()) {
//         // if so, do it
//         // else ask to put order to warning state
//         case ApplicationNotAvailable:
//         default:
            if (tsBusinessMessageReject.getBusinessRejectRefID() == null) {
               LOGGER.error("onBusinessMessageReject with null BusinessRejectRefID received, reason was {}", tsBusinessMessageReject.getText());
            }
            else if (tsBusinessMessageReject.getRefMsgType() == MsgType.OrderSingle) {
               tsoxConnectionListener.onOrderReject(sessionID.toString(), tsBusinessMessageReject.getBusinessRejectRefID(), tsBusinessMessageReject.getText());
            }
            else if (tsBusinessMessageReject.getRefMsgType() == MsgType.OrderCancelRequest) {
               tsoxConnectionListener.onCancelReject(sessionID.toString(), tsBusinessMessageReject.getBusinessRejectRefID(), tsBusinessMessageReject.getText());
            }
//         break;
//      }
   }

   @Override
   public void onQuoteRequestReject(SessionID sessionID, TSQuoteRequestReject tsQuoteRequestReject) throws TradeStacException {
      LOGGER.debug("{}, {}", sessionID, tsQuoteRequestReject.getQuoteReqID());

      String reason = tsQuoteRequestReject.getQuoteRequestRejectReason().name();
      String text = tsQuoteRequestReject.getText();
      if ((text != null) && (!(text.isEmpty()))) {
         reason += " - " + text;
      }
      reason = "Rejected by market : " + reason;

      tsoxConnectionListener.onOrderReject(sessionID.toString(), tsQuoteRequestReject.getQuoteReqID(), reason);
   }

   @Override
   public void sendRfq(MarketOrder marketOrder) throws BestXException {
      LOGGER.debug("{}", marketOrder);

      TSNewOrderSingle tsNewOrderSingle = createTSNewOrderSingle(marketOrder);

      if (!isConnected()) {
         throw new BestXException("Not connected");
      }

      try {
         getTradeStacClientSession().manageNewOrderSingle(tsNewOrderSingle); //tsNewOrderSingle.toFIXMessage()
      }
      catch (TradeStacException e) {
         throw new BestXException(String.format("Error managing newOrderSingle [%s]", tsNewOrderSingle), e);
      }
   }

   private TSNewOrderSingle createTSNewOrderSingle(MarketOrder marketOrder) {
      if (marketOrder == null) {
         throw new IllegalArgumentException("Params can't be null");
      }

      String clOrdID = marketOrder.getMarketSessionId();
      String securityID = marketOrder.getInstrument().getIsin();
      Side side = Side.getInstanceForFIXValue(marketOrder.getSide().getFixCode().charAt(0));
      Double orderQty = marketOrder.getQty().doubleValue();
      Date settlDate = marketOrder.getFutSettDate();
      String settlementType = marketOrder.getSettlementType();
      Currency currency = Currency.getInstanceForFIXValue(marketOrder.getCurrency());

      Date validUntilTime = new Date(DateService.currentTimeMillis() + tsoxEnquiryTime * 1000); // got from configuration
      Date transactTime = DateService.newLocalDate();

      OrdType ordType = OrdType.Limit;
      PriceType priceType = (ordType == OrdType.Market ? null : PriceType.Percentage);
      Double price = (ordType == OrdType.Market ? null : marketOrder.getLimit().getAmount().doubleValue());

      String text = null; // Customer notes - can be used to carry info useful to BestX:FI-A

      TSNewOrderSingle tsNewOrderSingle = new TSNewOrderSingle();
      //instrument
      TSInstrument tsInstrument = new TSInstrument();
      tsInstrument.setSymbol("[N/A]");
      tsInstrument.setSecurityID(securityID);
      tsInstrument.setSecurityIDSource(SecurityIDSource.IsinNumber);

      //quantity
      TSOrderQtyData tsOrderQtyData = new TSOrderQtyData();
      tsOrderQtyData.setOrderQty(orderQty);

      //parties
      TSNoPartyID trader = new TSNoPartyID();
      trader.setPartyID(traderCode);
      trader.setPartyIDSource(PartyIDSource.ProprietaryCustomCode);
      trader.setPartyRole(PartyRole.OrderOriginationTrader);

      TSNoPartyID investmentDecisor = new TSNoPartyID();
      investmentDecisor.setPartyID(investmentDecisionMakerID);
      investmentDecisor.setPartyIDSource(PartyIDSource.ProprietaryCustomCode);
      investmentDecisor.setPartyRole(PartyRole.InvestmentDecisionMaker);
      investmentDecisor.setPartyRoleQualifier(investmentDecisorQualifier);

      TSNoPartyID enteringFirm = new TSNoPartyID();
      enteringFirm.setPartyID(enteringFirmCode);
      enteringFirm.setPartyIDSource(PartyIDSource.ProprietaryCustomCode);
      enteringFirm.setPartyRole(PartyRole.EnteringFirm);

      List<TSNoPartyID> tsNoPartyIDsList = new ArrayList<>();
      tsNoPartyIDsList.add(trader);
      tsNoPartyIDsList.add(investmentDecisor);
      tsNoPartyIDsList.add(enteringFirm);
      
      // BESTX-867 management of include dealers. Use configurable attributes
      if(isAddIncludeDealers()) {
    	  for (int i = 0; i < includeDealersMaxNum && i < marketOrder.getDealers().size(); i++) {
    		  MarketMarketMakerSpec dealer = marketOrder.getDealers().get(i);
    		  TSNoPartyID incldealer = new TSNoPartyID();
    		  incldealer.setPartyID(dealer.getMarketMakerMarketSpecificCode());
    		  incldealer.setPartyIDSource(PartyIDSource.ProprietaryCustomCode);
    		  incldealer.setPartyRole(PartyRole.AcceptableCounterparty);
    		  tsNoPartyIDsList.add(incldealer);
    	  }
      }      
      // BESTX-867 a management of exclude dealers. Use configurable attributes
      if(isAddBlockedDealers()) {
    	  for (int i = 0; i < blockedDealersMaxNum && i < marketOrder.getExcludeDealers().size(); i++) {
    		  MarketMarketMakerSpec dealer = marketOrder.getExcludeDealers().get(i);
    		  TSNoPartyID excldealer = new TSNoPartyID();
    		  excldealer.setPartyID(dealer.getMarketMakerMarketSpecificCode());
    		  excldealer.setPartyIDSource(PartyIDSource.ProprietaryCustomCode);
    		  excldealer.setPartyRole(PartyRole.UnacceptableCounterparty);
    		  tsNoPartyIDsList.add(excldealer);
    	  }
      }
      
      TSParties tsParties = new TSParties();
      tsParties.setTSNoPartyIDsList(tsNoPartyIDsList);

      tsNewOrderSingle.setClOrdID(clOrdID);
      tsNewOrderSingle.setOrdType(ordType);
      tsNewOrderSingle.setTimeInForce(TimeInForce.GoodTillDate);
      tsNewOrderSingle.setExpireTime(validUntilTime);
      tsNewOrderSingle.setSide(side);
      tsNewOrderSingle.setTSInstrument(tsInstrument);
      tsNewOrderSingle.setTSOrderQtyData(tsOrderQtyData);
      tsNewOrderSingle.setTSParties(tsParties);
      tsNewOrderSingle.setSettlType(SettlType.getInstanceForFIXValue(settlementType));
      tsNewOrderSingle.setSettlDate(settlDate);
      tsNewOrderSingle.setTransactTime(transactTime);
      tsNewOrderSingle.setCurrency(currency);
      tsNewOrderSingle.setPrice(price);
      tsNewOrderSingle.setPriceType(priceType);
      tsNewOrderSingle.setText(text);
      tsNewOrderSingle.setOrderCapacity(defaultCapacity);
      tsNewOrderSingle.setHandlInst(HandlInst.ManualOrderBestExecution); // required for scenario FI 1 -- correction asked by TJ Senkhas

      // set custom fields
      List<Field<?>> customFields = new ArrayList<>();
      Field<Character> autoExRuleIns = new Field<>(22515, 'y'); // required for scenario FI 1 -- correction asked by TJ Senkhas
      customFields.add(autoExRuleIns);
      Field<String> stagedOrderIsInquiry = new Field<>(9575, "Y");
      customFields.add(stagedOrderIsInquiry);
      Field<Integer> qtyType = new Field<>(854, 0);
      customFields.add(qtyType);
      Field<String> marketSegmentID = new Field<>(1300, this.getDestinationMICCode());
      customFields.add(marketSegmentID);
      tsNewOrderSingle.setCustomFields(customFields);

      //		tsNewOrderSingle.setTradeDate(tradeDate);
      //		tsNewOrderSingle.setEncodedText(encodedText);



      LOGGER.info("{}", tsNewOrderSingle);

      return tsNewOrderSingle;
   }

   @Override
   public void sendSubjectOrder(MarketOrder marketOrder) throws BestXException {
      throw new UnsupportedOperationException();
   }

   @Override
   public void ackProposal(Proposal proposal) throws BestXException {
      throw new UnsupportedOperationException();
   }

   @Override
   public void acceptProposal(Operation operation, Instrument instrument, Proposal proposal) throws BestXException {
      throw new UnsupportedOperationException();
   }

   @Override
   public void rejectProposal(Operation operation, Instrument instrument, Proposal proposal) throws BestXException {
      throw new UnsupportedOperationException();
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
        tsOrderCancelRequest.setClOrdID("OCR#" + origClOrdID);  // ATTENZIONE; non posso cancellare più di 1 volta
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
        } catch (TradeStacException e) {
            throw new BestXException(String.format("Error managing orderCancelRequest [%s]", tsOrderCancelRequest), e);
        }

	}

	@Override
	public void onOrderCancelReject(SessionID sessionID, TSOrderCancelReject tsOrderCancelReject) throws TradeStacException {
		String clordid = tsOrderCancelReject.getOrigClOrdID();
		String text = tsOrderCancelReject.getText();
        this.tsoxConnectionListener.onCancelReject(sessionID.toString(), clordid, text);
	}		
	
}
