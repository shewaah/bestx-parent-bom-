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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.connections.tradestac.AbstractTradeStacConnection;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.tradestac.api.TradeStacException;
import it.softsolutions.tradestac.client.TradeStacClientSession;
import it.softsolutions.tradestac.fix.field.Currency;
import it.softsolutions.tradestac.fix.field.ExecType;
import it.softsolutions.tradestac.fix.field.OrdType;
import it.softsolutions.tradestac.fix.field.PartyIDSource;
import it.softsolutions.tradestac.fix.field.PartyRole;
import it.softsolutions.tradestac.fix.field.QuoteRespType;
import it.softsolutions.tradestac.fix.field.QuoteStatus;
import it.softsolutions.tradestac.fix.field.QuoteType;
import it.softsolutions.tradestac.fix.field.SecurityIDSource;
import it.softsolutions.tradestac.fix.field.Side;
import it.softsolutions.tradestac.fix.field.TimeInForce;
import it.softsolutions.tradestac.fix50.TSBusinessMessageReject;
import it.softsolutions.tradestac.fix50.TSExecutionReport;
import it.softsolutions.tradestac.fix50.TSNewOrderSingle;
import it.softsolutions.tradestac.fix50.TSNoPartyID;
import it.softsolutions.tradestac.fix50.TSQuote;
import it.softsolutions.tradestac.fix50.TSQuoteAck;
import it.softsolutions.tradestac.fix50.TSQuoteRequestReject;
import it.softsolutions.tradestac.fix50.TSQuoteResponse;
import it.softsolutions.tradestac.fix50.TSQuoteStatusReport;
import it.softsolutions.tradestac.fix50.component.TSInstrument;
import it.softsolutions.tradestac.fix50.component.TSOrderQtyData;
import it.softsolutions.tradestac.fix50.component.TSParties;
import quickfix.ConfigError;
import quickfix.SessionID;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-bloomberg-market 
 * First created by: davide.rossoni 
 * Creation date: 28/set/2012 
 * 
 **/
public class TSOXConnectionImpl extends AbstractTradeStacConnection implements TSOXConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(TSOXConnectionImpl.class);

    private TSOXConnectionListener tsoxConnectionListener;
    private TradeStacClientSession tradeStacClientSession;

    // tsoxWiretime: validity time of quotes from dealer - when expired, quotes go subject (in seconds) 
    private long tsoxWiretime;
    // the dealer must respond to a hit/lift request within this time; if = 0 the value is not sent, and the reply expires at wiretime
    private long tsoxHitLiftTimeout;

    // trader code to be sent to Tsox in QuoteRequest with PartyRole=11(Originator)
    private String tsoxTradercode;
    /**
     * Instantiates a new tSOX connection impl.
     */
    public TSOXConnectionImpl() {
        super(Market.MarketCode.BLOOMBERG + "#tsox");
    }

    /**
     * Initializes a newly created {@link STPConnector}.
     *
     * @throws TradeStacException if an error occurred in the FIX connection initialization
     * @throws BestXException if an error occurred
     * @throws ConfigError 
     */
    public void init() throws TradeStacException, BestXException, ConfigError {
        super.init();

        tradeStacClientSession = super.getTradeStacClientSession();
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
        
        // [DR20131126]
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
        tsoxConnectionListener.onExecutionReport(sessionID.toString(),
                        tsExecutionReport.getClOrdID(),
                        tsExecutionReport.getExecType(), 
                        tsExecutionReport.getOrdStatus(), 
                        tsExecutionReport.getAccruedInterestAmt() != null ? BigDecimal.valueOf(tsExecutionReport.getAccruedInterestAmt()) : BigDecimal.ZERO, 
                        BigDecimal.ZERO /*accruedInterestRate*/, 
                        tsExecutionReport.getLastPx() != null ? BigDecimal.valueOf(tsExecutionReport.getLastPx()) : BigDecimal.ZERO, 
                        tsExecutionReport.getExecID(), 
                        tsExecutionReport.getSettlDate(), tsExecutionReport.getTransactTime(),
                        tsExecutionReport.getText());

    }

    /*
     * At the moment is not possible to notify the request is failed: it has not the information to create the reject classifiedProposal. It
     * is need to store the request in the map.
     */
    @Override
    public void onBusinessMessageReject(SessionID sessionID, TSBusinessMessageReject tsBusinessMessageReject) throws TradeStacException {
        LOGGER.error("{}, {}", sessionID, tsBusinessMessageReject);

        // TODO complete here!!!
    }

    @Override
    public void onQuote(SessionID sessionID, TSQuote tsQuote) throws TradeStacException {
    	LOGGER.debug("sessionID = {}, tsQuote = {}", sessionID, tsQuote);

        String marketMaker = "";
        List<TSNoPartyID> parties = tsQuote.getTSParties().getTSNoPartyIDsList();
        for (TSNoPartyID party : parties) {
            if (party.getPartyRole().equals(PartyRole.ExecutingFirm)) {
                marketMaker = party.getPartyID();
                break;
            }
        }
        
        ProposalType proposalType = ProposalType.INDICATIVE;
        if (tsQuote.getQuoteType() == QuoteType.Tradeable) {
            proposalType = ProposalType.TRADEABLE;
        }
        
        try{
        	String sessionId = sessionID.toString(); 
			String quoteReqId = tsQuote.getQuoteReqID();
			String quoteRespId = tsQuote.getQuoteRespID();
			String quoteId = tsQuote.getQuoteID();
			BigDecimal qty = (tsQuote.getTSOrderQtyData() != null && tsQuote.getTSOrderQtyData().getOrderQty() != null) ? BigDecimal.valueOf(tsQuote.getTSOrderQtyData().getOrderQty()) : null;
			String currency = tsQuote.getCurrency().getFIXValue();
			Date futSettDate = tsQuote.getSettlDate();
			int acknowledgeLevel = 0;
			String onBehalfOfCompID = tsQuote.getOnBehalfOfCompID();
			
			ProposalSide side = null;
			BigDecimal price = null;
			
            switch (tsQuote.getSide()) {
            case Buy:
            	side = ProposalSide.ASK;
            	price = tsQuote.getOfferPx() != null ? BigDecimal.valueOf(tsQuote.getOfferPx()) : null;
            	tsoxConnectionListener.onCounter(sessionId, quoteReqId, quoteRespId, quoteId, marketMaker, price, qty, currency, side, proposalType, futSettDate, acknowledgeLevel, onBehalfOfCompID);
            	break;
            case Sell:
            	side = ProposalSide.BID;
            	price = tsQuote.getBidPx() != null ? BigDecimal.valueOf(tsQuote.getBidPx()) : null;
                tsoxConnectionListener.onCounter(sessionId, quoteReqId, quoteRespId, quoteId, marketMaker, price, qty, currency, side, proposalType, futSettDate, acknowledgeLevel, onBehalfOfCompID);
                break;
            default:
                LOGGER.warn("[MktMsg] Side {} not managed, rejecting the quote with QuoteReqID = {}, QuoteID = {}, MM = {}", tsQuote.getSide(), tsQuote.getQuoteReqID(), tsQuote.getQuoteID(), marketMaker);
                rejectUnexpectedQuote(tsQuote, "Invalid side");
            }
        } catch (OperationNotExistingException e){
            LOGGER.warn("[MktMsg] The operation with QuoteReqID = {} does not exist, rejecting QuoteID = {}, MM = {}", tsQuote.getQuoteReqID(), tsQuote.getQuoteID(), marketMaker);
            rejectUnexpectedQuote(tsQuote, Messages.getString("OperationNotFoundForQuote"));
        } catch (BestXException e) {
            LOGGER.warn("[MktMsg] The operation Exception in notification of quote, rejecting it - QuoteReqID = {} QuoteID = [{}], MM = {}", tsQuote.getQuoteReqID(), tsQuote.getQuoteID(), marketMaker, e);
            rejectUnexpectedQuote(tsQuote, Messages.getString("OperationNotFoundForQuote"));
        }
    }
    
    private void rejectUnexpectedQuote(TSQuote tsQuote, String reason) {
    	
    	String quoteRespID = Long.toString(DateService.currentTimeMillis());

    	TSQuoteResponse tsQuoteResponse = new TSQuoteResponse();
		tsQuoteResponse.setQuoteID(tsQuote.getQuoteID());
		tsQuoteResponse.setClOrdID(tsQuote.getQuoteReqID());
		tsQuoteResponse.setQuoteRespID(quoteRespID);
		tsQuoteResponse.setQuoteType(tsQuote.getQuoteType());
		
		TSInstrument tsInstrument = tsQuote.getTSInstrument();
        tsQuoteResponse.setTSInstrument(tsInstrument);
        tsQuoteResponse.setTSOrderQtyData(tsQuote.getTSOrderQtyData());
        tsQuoteResponse.setSide(tsQuote.getSide());
        tsQuoteResponse.setSettlDate(tsQuote.getSettlDate());
        tsQuoteResponse.setTransacTime(DateService.newLocalDate());
        
        // @see mail AMC 22.11.2013 
        if (tsQuote.getOnBehalfOfCompID() != null) {
        	String onBehalfOfCompID = tsQuote.getOnBehalfOfCompID();
        	tsQuoteResponse.setDeliverToCompID(onBehalfOfCompID);
        }
        
        tsQuoteResponse.setQuoteRespType(QuoteRespType.Pass);
        tsQuoteResponse.setText(reason);

        try {
            LOGGER.info("[MktReq] Sending QuoteResponse/EndTrade for QuoteReqID = {}, QuoteReqID = {}", tsQuote.getQuoteReqID(), tsQuote.getQuoteID());
            tradeStacClientSession.manageQuoteResponse(tsQuoteResponse);
        } catch (TradeStacException e) {
            LOGGER.error("Error while sending quote response [{}] to reject quote [{}]", tsQuoteResponse, tsQuote, e);
        }
    }

    @Override
    public void onQuoteRequestReject(SessionID sessionID, TSQuoteRequestReject tsQuoteRequestReject) throws TradeStacException {
        LOGGER.debug("{}, {}", sessionID, tsQuoteRequestReject.getQuoteReqID());

        String reason = tsQuoteRequestReject.getQuoteRequestRejectReason().name();
        String text = tsQuoteRequestReject.getText();
        if ( (text != null) && (!(text.isEmpty())) ) {
            reason += " - " + text;
        }
        reason = "Rejected by dealer : " + reason;

        tsoxConnectionListener.onOrderReject(sessionID.toString(), tsQuoteRequestReject.getQuoteReqID(), reason);
    }

    @Override
    public void onQuoteStatusReport(SessionID sessionID, TSQuoteStatusReport tsQuoteStatusReport) throws TradeStacException {
        LOGGER.debug("{}, {}, {}", sessionID, tsQuoteStatusReport.getQuoteReqID(), tsQuoteStatusReport);

        String dealerCode = "";
        TSParties parties = tsQuoteStatusReport.getTSParties();
        if (parties != null && parties.getTSNoPartyIDsList() != null) {
            for (TSNoPartyID party : parties.getTSNoPartyIDsList()) {
                if (party != null && party.getPartyRole() != null && party.getPartyRole().equals(PartyRole.ExecutingFirm)) {
                    dealerCode = party.getPartyID();
                    break;
                }
            }
        }
        
        if (tsQuoteStatusReport.getQuoteStatus() == QuoteStatus.TradeEnded) {   // end of trade
            LOGGER.info("QuoteStatusReport/TradeEnded received for QuoteReqId {}, ignored", tsQuoteStatusReport.getQuoteReqID());
            
            // TradeEnded può arrivarmi se:
            // - è una risposta ad una QuoteResponse.Pass che è andata bene (riceverò anche un ExecutionReport.Canceled con text = "Trader Passed")
            // - è una risposta ad una QuoteResponse.HitLift che è andata male (Quote scaduta) > text = "Price version mismatch. Received vers 1, Current vers is 2"
            // - è scaduta l'ultima Quote (expireTime = 3 minuti) > Subito dopo arriva un ExecutionReport.Canceled con text = "Trade Expired"
            
            tsoxConnectionListener.onQuoteStatusTradeEnded(sessionID.toString(), tsQuoteStatusReport.getQuoteReqID(), tsQuoteStatusReport.getQuoteID(), dealerCode, tsQuoteStatusReport.getText());
        }
        else if (tsQuoteStatusReport.getQuoteStatus() == QuoteStatus.Expired) { // quote has gone subject
        	// Expired può arrivarmi se:
            // - è scaduta una Quote per i fatti suoi
            LOGGER.info("QuoteStatusReport/Expired received for QuoteReqId {}", tsQuoteStatusReport.getQuoteReqID());
            tsoxConnectionListener.onQuoteStatusExpired(sessionID.toString(), tsQuoteStatusReport.getQuoteReqID(), tsQuoteStatusReport.getQuoteID(), dealerCode);
        }
        else if (tsQuoteStatusReport.getQuoteStatus() == QuoteStatus.TimedOut) {    // dealer timeout (did non answer within dealer response time)
            LOGGER.info("QuoteStatusReport/TimedOut received for QuoteReqId {}, Dealer {}", tsQuoteStatusReport.getQuoteReqID(), tsQuoteStatusReport.getTSParties());
            tsoxConnectionListener.onQuoteStatusTimeout(sessionID.toString(), tsQuoteStatusReport.getQuoteReqID(), tsQuoteStatusReport.getQuoteID(), dealerCode, tsQuoteStatusReport.getText());
        }
        else {
            LOGGER.info("QuoteStatusReport/{} received for QuoteReqId {}, ignored", tsQuoteStatusReport.getQuoteStatus(), tsQuoteStatusReport.getQuoteReqID());
        }
    }

    @Override
    public void sendRfq(MarketOrder marketOrder) throws BestXException {
        LOGGER.debug("{}", marketOrder);

        TSNewOrderSingle tsNewOrderSingle = createTSNewOrderSingle(marketOrder);

        if (!isConnected()) {
            throw new BestXException("Not connected");
        }

        try {
            tradeStacClientSession.manageNewOrderSingle(tsNewOrderSingle);
        } catch (TradeStacException e) {
            throw new BestXException(String.format("Error managing newOrderSingle [%s]", tsNewOrderSingle), e);
        }
    }

    @Override
    public void ackProposal(Proposal proposal) throws BestXException {
        LOGGER.debug("{}", proposal);

        TSQuoteAck tsQuoteAck = new TSQuoteAck();
        tsQuoteAck.setQuoteReqID(proposal.getQuoteReqId());
        tsQuoteAck.setQuoteID(proposal.getSenderQuoteId());
        tsQuoteAck.setQuoteAckStatus(it.softsolutions.tradestac.fix.field.QuoteAckStatus.Received);

        try {
            tradeStacClientSession.manageQuoteAck(tsQuoteAck);
        } catch (TradeStacException e) {
            throw new BestXException(String.format("Error managing quoteAck [%s]", tsQuoteAck), e);
        }
    }


    @Override
    public void acceptProposal(Operation operation, Instrument instrument, Proposal proposal) throws BestXException {
    	LOGGER.debug("{}", proposal);

        String quoteRespID = Long.toString(DateService.currentTimeMillis());
        // clOrdId links QuoteResponse and ExecutionReport (which returns it)
        String clOrdID = proposal.getQuoteReqId(); // got from proposal so it is surely the last one, ok? operation.getIdentifiers().get(OperationIdType.TSOX_CLORD_ID);
        String quoteID = proposal.getSenderQuoteId();
        if (clOrdID == null) {
            throw new BestXException(String.format("ClOrdID missing for operation [%s]", operation.getOrder().getFixOrderId()));
        }
        
        if (!proposal.getQuoteReqId().equals(clOrdID)) {
        	throw new BestXException("Quote.quoteReqID [" + proposal.getQuoteReqId() + "] does not match NewOrderSingle.clOrdID [" + clOrdID + ']');
        }
        
        TSQuoteResponse tsQuoteResponse = new TSQuoteResponse();
        tsQuoteResponse.setQuoteRespID(quoteRespID);
        tsQuoteResponse.setQuoteID(quoteID);
        tsQuoteResponse.setClOrdID(clOrdID);

        tsQuoteResponse.setQuoteRespType(QuoteRespType.HitLift);
        TSInstrument tsInstrument = new TSInstrument();
        tsInstrument.setSymbol("[N/A]");
        tsInstrument.setSecurityID(instrument.getIsin());
        tsQuoteResponse.setTSInstrument(tsInstrument);
        
        TSOrderQtyData tsOrderQtyData = new TSOrderQtyData();
        tsOrderQtyData.setOrderQty(proposal.getQty().doubleValue());
        tsQuoteResponse.setTSOrderQtyData(tsOrderQtyData);
        
        tsQuoteResponse.setSide(proposal.getSide() == ProposalSide.ASK ? Side.Buy : Side.Sell);
        tsQuoteResponse.setSettlDate(proposal.getFutSettDate());
        tsQuoteResponse.setTransacTime(DateService.newLocalDate());
        tsQuoteResponse.setDeliverToCompID(proposal.getOnBehalfOfCompID());
        
        if (tsoxHitLiftTimeout > 0) {
            tsQuoteResponse.setValidUntilTime(new Date(DateService.currentTimeMillis()+tsoxHitLiftTimeout*1000)); // got from configuration);
        }

        try {
            LOGGER.info("Sending QuoteResponse/HitLift for ClOrdID/QuoteReqID = {}, QuoteID = {}", clOrdID, quoteID);
            tradeStacClientSession.manageQuoteResponse(tsQuoteResponse);
        } catch (TradeStacException e) {
            throw new BestXException(String.format("Error managing quoteResponse [%s]", tsQuoteResponse), e);
        }

    }

    @Override
    public void rejectProposal(Operation operation, Instrument instrument, Proposal proposal) throws BestXException {
    	LOGGER.debug("{}", proposal);

        String quoteRespID = Long.toString(DateService.currentTimeMillis());
        // clOrdId links QuoteResponse and ExecutionReport (which returns it)
        String clOrdID = proposal.getQuoteReqId();  // got from proposal so it is surely the last one, ok?  operation.getIdentifiers().get(OperationIdType.TSOX_CLORD_ID);
        String quoteID = proposal.getSenderQuoteId();
        if (clOrdID == null) {
            throw new BestXException(String.format("ClOrdID missing for operation [%s]", operation.getOrder().getFixOrderId()));
        }

        if (!proposal.getQuoteReqId().equals(clOrdID)) {
        	throw new BestXException("Quote.quoteReqID [" + proposal.getQuoteReqId() + "] does not match NewOrderSingle.clOrdID [" + clOrdID + ']');
        }
        
        TSQuoteResponse tsQuoteResponse = new TSQuoteResponse();
        tsQuoteResponse.setQuoteRespID(quoteRespID);
        tsQuoteResponse.setQuoteID(quoteID);
        tsQuoteResponse.setClOrdID(clOrdID);

        tsQuoteResponse.setQuoteRespType(QuoteRespType.Pass);
        TSInstrument tsInstrument = new TSInstrument();
        tsInstrument.setSymbol("[N/A]");
        tsInstrument.setSecurityID(instrument.getIsin());
        tsQuoteResponse.setTSInstrument(tsInstrument);

        TSOrderQtyData tsOrderQtyData = new TSOrderQtyData();
        tsOrderQtyData.setOrderQty(proposal.getQty().doubleValue());
        tsQuoteResponse.setTSOrderQtyData(tsOrderQtyData);
        
        tsQuoteResponse.setSide(proposal.getSide() == ProposalSide.ASK ? Side.Buy : Side.Sell);
        tsQuoteResponse.setSettlDate(proposal.getFutSettDate());
        tsQuoteResponse.setTransacTime(DateService.newLocalDate());
        tsQuoteResponse.setDeliverToCompID(proposal.getOnBehalfOfCompID());

        try {
            LOGGER.info("Sending QuoteResponse/Pass for ClOrdID/QuoteReqID = {}, QuoteID = {}", clOrdID, quoteID);
            tradeStacClientSession.manageQuoteResponse(tsQuoteResponse);
        } catch (TradeStacException e) {
            throw new BestXException(String.format("Error managing quoteResponse [%s]", tsQuoteResponse), e);
        }

    }
    
    @Override
    public void sendSubjectOrder(MarketOrder marketOrder) throws BestXException {
    	throw new UnsupportedOperationException();
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
        Currency currency = Currency.getInstanceForFIXValue(marketOrder.getCurrency());
        
		Date validUntilTime = new Date(DateService.currentTimeMillis() + tsoxWiretime * 1000); // got from configuration
//		Date dealerResponseTime = new Date(DateService.currentTimeMillis() + tsoxDueInTime * 1000); // got from configuration
        Date transactTime = DateService.newLocalDate();

        // [DR20131202] Come concordato con AMC, i marketOrder di BestX saranno:
        // - OrdType.Market se la Proposal è Indicative, a prescindere dal tipo di ordine in ingresso (Limit o Market)
        // - OrdType.Limit se Proposal è Tradeable, a prescindere dal tipo di ordine in ingresso (Limit o Market)
        // Attualmente le proposal Bloomberg sono sempre Indicative, per cui fissiamo qui a OrdType.Market la QuoteRequest da inviare a TSOX 
        OrdType ordType = OrdType.Market;
        // PriceType priceType = (ordType == OrdType.Market ? null : PriceType.Percentage);
        // Double price = (ordType == OrdType.Market ? null : marketOrder.getLimit().getAmount().doubleValue());
        
        String dealerCode = marketOrder.getMarketMarketMaker().getMarketSpecificCode();
        // TODO [DR20131126] Remove this!!! Workaround for TSOX Test environment
//        dealerCode = "D1";
//        LOGGER.warn("WARNING!!! DealerCode [{}] replaced with [{}] in order to permit TSOX testing", marketOrder.getMarketMarketMaker().getMarketSpecificCode(), dealerCode);
        
        String text = null; // Customer notes - can be used to carry info useful to BestX!

        TSInstrument tsInstrument = new TSInstrument();
        tsInstrument.setSymbol("[N/A]");
        tsInstrument.setSecurityID(securityID);
        tsInstrument.setSecurityIDSource(SecurityIDSource.IsinNumber);

        TSOrderQtyData tsOrderQtyData = new TSOrderQtyData();
        tsOrderQtyData.setOrderQty(orderQty);

        TSNoPartyID tsNoPartyID1 = new TSNoPartyID();
        tsNoPartyID1.setPartyID(dealerCode);
        tsNoPartyID1.setPartyIDSource(PartyIDSource.ProprietaryCustomCode);
        tsNoPartyID1.setPartyRole(PartyRole.ExecutingFirm);

        TSNoPartyID tsNoPartyID2 = new TSNoPartyID();
        tsNoPartyID2.setPartyID(tsoxTradercode);
        tsNoPartyID2.setPartyIDSource(PartyIDSource.ProprietaryCustomCode);
        tsNoPartyID2.setPartyRole(PartyRole.OrderOriginationTrader);

        List<TSNoPartyID> tsNoPartyIDsList = new ArrayList<TSNoPartyID>();
        tsNoPartyIDsList.add(tsNoPartyID1);
        tsNoPartyIDsList.add(tsNoPartyID2);

        TSParties tsParties = new TSParties();
        tsParties.setTSNoPartyIDsList(tsNoPartyIDsList);
        
        TSNewOrderSingle tsNewOrderSingle = new TSNewOrderSingle();
        tsNewOrderSingle.setClOrdID(clOrdID);
		tsNewOrderSingle.setOrdType(ordType);
		tsNewOrderSingle.setTimeInForce(TimeInForce.Day);
		tsNewOrderSingle.setExpireTime(validUntilTime);
		tsNewOrderSingle.setSide(side);
		tsNewOrderSingle.setCurrency(Currency.EUR);
		tsNewOrderSingle.setTransactTime(DateService.newLocalDate());
		tsNewOrderSingle.setTSInstrument(tsInstrument);
		tsNewOrderSingle.setTSOrderQtyData(tsOrderQtyData);
        tsNewOrderSingle.setTSParties(tsParties);
        tsNewOrderSingle.setSettlDate(settlDate);
        tsNewOrderSingle.setCurrency(currency);
//        tsNewOrderSingle.setPrice(price);
//        tsNewOrderSingle.setPriceType(priceType);
        tsNewOrderSingle.setTransactTime(transactTime);
        tsNewOrderSingle.setText(text);
	
//		tsNewOrderSingle.setAccount(account);
//		tsNewOrderSingle.setTradeDate(tradeDate);
//		tsNewOrderSingle.setEncodedText(encodedText);

        LOGGER.info("{}", tsNewOrderSingle);

        return tsNewOrderSingle;
    }

    /**
     * Gets the tsox wiretime.
     *
     * @return the tsox wiretime
     */
    public long getTsoxWiretime() {
        return tsoxWiretime;
    }


    /**
     * Gets the tsox hit lift timeout.
     *
     * @return the tsox hit lift timeout
     */
    public long getTsoxHitLiftTimeout() {
        return tsoxHitLiftTimeout;
    }

    /**
     * Sets the tsox hit lift timeout.
     *
     * @param tsoxHitLiftTimeout the new tsox hit lift timeout
     */
    public void setTsoxHitLiftTimeout(long tsoxHitLiftTimeout) {
        this.tsoxHitLiftTimeout = tsoxHitLiftTimeout;
    }
    
    /**
     * Sets the tsox wiretime.
     *
     * @param tsoxWiretime the new tsox wiretime
     */
    public void setTsoxWiretime(long tsoxWiretime) {
        this.tsoxWiretime = tsoxWiretime;
    }


    /**
     * Gets the tsox tradercode.
     *
     * @return the tsox tradercode
     */
    public String getTsoxTradercode() {
        return tsoxTradercode;
    }

    /**
     * Sets the tsox tradercode.
     *
     * @param tsoxTradercode the new tsox tradercode
     */
    public void setTsoxTradercode(String tsoxTradercode) {
        this.tsoxTradercode = tsoxTradercode;
    }

}
