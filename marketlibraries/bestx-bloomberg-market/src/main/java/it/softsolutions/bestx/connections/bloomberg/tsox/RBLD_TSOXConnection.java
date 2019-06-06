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
import it.softsolutions.bestx.connections.tradestac.AbstractTradeStacConnection;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.tradestac.api.TradeStacException;
import it.softsolutions.tradestac.client.TradeStacClientSession;
import it.softsolutions.tradestac.fix.field.Currency;
import it.softsolutions.tradestac.fix.field.ExecType;
import it.softsolutions.tradestac.fix.field.MsgType;
import it.softsolutions.tradestac.fix.field.OrdType;
import it.softsolutions.tradestac.fix.field.PartyIDSource;
import it.softsolutions.tradestac.fix.field.PartyRole;
import it.softsolutions.tradestac.fix.field.PartyRoleQualifier;
import it.softsolutions.tradestac.fix.field.PriceType;
import it.softsolutions.tradestac.fix.field.SecurityIDSource;
import it.softsolutions.tradestac.fix.field.Side;
import it.softsolutions.tradestac.fix.field.TimeInForce;
import it.softsolutions.tradestac.fix50.TSBusinessMessageReject;
import it.softsolutions.tradestac.fix50.TSExecutionReport;
import it.softsolutions.tradestac.fix50.TSNewOrderSingle;
import it.softsolutions.tradestac.fix50.TSNoPartyID;
import it.softsolutions.tradestac.fix50.TSQuoteRequestReject;
import it.softsolutions.tradestac.fix50.component.TSInstrument;
import it.softsolutions.tradestac.fix50.component.TSOrderQtyData;
import it.softsolutions.tradestac.fix50.component.TSParties;
import quickfix.ConfigError;
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
public class RBLD_TSOXConnection extends AbstractTradeStacConnection implements TSOXConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(RBLD_TSOXConnection.class);

    private TSOXConnectionListener tsoxConnectionListener;
    private TradeStacClientSession tradeStacClientSession;
    
    private String traderCode;
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

    // trader code to be sent to Tsox in QuoteRequest with PartyRole=11(Originator)
    private String tsoxTradercode;
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
   
    private String defaultCapacity;
    public String getDefaultCapacity() {
		return defaultCapacity;
	}
	public void setDefaultCapacity(String defaultCapacity) {
		this.defaultCapacity = defaultCapacity;
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
    public RBLD_TSOXConnection() {
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
                        tsExecutionReport);
    }

    /*
     * At the moment is not possible to notify the request is failed: it has not the information to create the reject classifiedProposal. It
     * is need to store the request in the map.
     */
    @Override
    public void onBusinessMessageReject(SessionID sessionID, TSBusinessMessageReject tsBusinessMessageReject) throws TradeStacException {
        LOGGER.info("{}, {}", sessionID, tsBusinessMessageReject);

        // FIXME check if complete:
        // check if the message is requiring the resend of the original message
        switch(tsBusinessMessageReject.getBusinessRejectReason()) {
        // if so, do it
        // else ask to put order to warning state
        case ApplicationNotAvailable:
        default:
        	if(tsBusinessMessageReject.getRefMsgType() == MsgType.OrderSingle) {
        		tsoxConnectionListener.onOrderReject(sessionID.toString(), tsBusinessMessageReject.getBusinessRejectRefID(),
        				tsBusinessMessageReject.getText());
        	}
        	else if(tsBusinessMessageReject.getRefMsgType() == MsgType.OrderCancelRequest) {
        		tsoxConnectionListener.onCancelReject(sessionID.toString(), tsBusinessMessageReject.getBusinessRejectRefID(),
        				tsBusinessMessageReject.getText());
        	}
        		break;
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
            tradeStacClientSession.manageNewOrderSingle(tsNewOrderSingle);
        } catch (TradeStacException e) {
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
        Currency currency = Currency.getInstanceForFIXValue(marketOrder.getCurrency());
        
		Date validUntilTime = new Date(DateService.currentTimeMillis() + tsoxEnquiryTime * 1000); // got from configuration
        Date transactTime = DateService.newLocalDate();

        OrdType ordType = OrdType.Limit;
        PriceType priceType = (ordType == OrdType.Market ? null : PriceType.Percentage);
        Double price = (ordType == OrdType.Market ? null : marketOrder.getLimit().getAmount().doubleValue());
                
        String text = null; // Customer notes - can be used to carry info useful to BestX!

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
        trader.setPartyID(tsoxTradercode);
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

        List<TSNoPartyID> tsNoPartyIDsList = new ArrayList<TSNoPartyID>();
        tsNoPartyIDsList.add(trader);
        tsNoPartyIDsList.add(investmentDecisor);
        tsNoPartyIDsList.add(enteringFirm);

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
        tsNewOrderSingle.setSettlDate(settlDate);
        tsNewOrderSingle.setTransactTime(transactTime);
        tsNewOrderSingle.setCurrency(currency);
        tsNewOrderSingle.setPrice(price);
        tsNewOrderSingle.setPriceType(priceType);
        tsNewOrderSingle.setText(text);
	
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

}
