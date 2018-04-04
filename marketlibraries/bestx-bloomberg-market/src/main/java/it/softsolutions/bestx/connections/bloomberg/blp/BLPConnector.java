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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.tradestac.AbstractTradeStacConnection;
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnection;
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnectionListener;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.tradestac.api.TradeStacException;
import it.softsolutions.tradestac.client.TradeStacClientSession;
import it.softsolutions.tradestac.fix.field.MDEntryType;
import it.softsolutions.tradestac.fix.field.MsgType;
import it.softsolutions.tradestac.fix.field.SecurityIDSource;
import it.softsolutions.tradestac.fix50.TSBusinessMessageReject;
import it.softsolutions.tradestac.fix50.TSMarketDataRequest;
import it.softsolutions.tradestac.fix50.TSMarketDataRequestReject;
import it.softsolutions.tradestac.fix50.TSMarketDataSnapshotFullRefresh;
import it.softsolutions.tradestac.fix50.component.TSInstrument;
import quickfix.ConfigError;
import quickfix.SessionID;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-bloomberg-market First created by: fabrizio.aponte Creation date: 22/mag/2012
 * 
 **/
public class BLPConnector extends AbstractTradeStacConnection implements TradeStacPreTradeConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(BLPConnector.class);

    private TradeStacPreTradeConnectionListener blpConnectionListener;

    private InstrumentFinder instrumentFinder;
    private MarketMakerFinder marketMakerFinder;
    private VenueFinder venueFinder;
    private MarketFinder marketFinder;

    private BLPHelper blpHelper;

    private TradeStacClientSession tradeStacClientSession;

    /**
     * Constructor
     */
    public BLPConnector() {
        super(Market.MarketCode.BLOOMBERG + "#blp");
    }

    /**
     * Initializes a newly created {@link BLPConnector}
     * 
     * @throws TradeStacException
     *             if an error occurred in the FIX connection initialization
     * @throws BestXException
     *             if an error occurred
     * @throws ConfigError 
     */
    public void init() throws TradeStacException, BestXException, ConfigError {
        super.init();

        tradeStacClientSession = super.getTradeStacClientSession();

        if (instrumentFinder == null) {
            throw new ObjectNotInitializedException("InstrumentFinder not set");
        }
        if (marketMakerFinder == null) {
            throw new ObjectNotInitializedException("MarketMakerFinder not set");
        }
        if (venueFinder == null) {
            throw new ObjectNotInitializedException("VenueFinder not set");
        }
        if (marketFinder == null) {
            throw new ObjectNotInitializedException("MarketFinder not set");
        }

        blpHelper = new BLPHelper(instrumentFinder, marketMakerFinder, venueFinder, marketFinder);
    }

    @Override
    public void setTradeStacPreTradeConnectionListener(TradeStacPreTradeConnectionListener blpConnectionListener) {
        LOGGER.debug("{}", blpConnectionListener);

        this.blpConnectionListener = blpConnectionListener;
        super.setTradeStacConnectionListener(blpConnectionListener);
    }

    @Override
    public void requestInstrumentPriceSnapshot(Instrument instrument, String marketMakerCode) throws BestXException {
        LOGGER.debug("{}, {}", instrument, marketMakerCode);

        if (marketMakerCode == null) {
            throw new BestXException("MarketMakerCode is null");
        }

        List<String> marketMakerCodes = new ArrayList<String>();
        marketMakerCodes.add(marketMakerCode);
        requestInstrumentPriceSnapshot(instrument, marketMakerCodes);
    }

    @Override
    public void requestInstrumentPriceSnapshot(Instrument instrument, List<String> marketMakerCodes) throws BestXException {
        LOGGER.info("{}, {}", instrument, marketMakerCodes);

        if (instrument == null || marketMakerCodes == null) {
            throw new IllegalArgumentException("Params can't be null");
        }

        if (!isConnected()) {
            throw new BestXException("Not connected");
        }

        TSMarketDataRequest tsMarketDataRequest = blpHelper.createMarketDataRequest(instrument, marketMakerCodes);
        try {
            tradeStacClientSession.manageMarketDataRequest(tsMarketDataRequest);
            
            blpHelper.addLiteMarketDataRequest(tsMarketDataRequest.getMDReqID(), instrument, marketMakerCodes);
            
        } catch (TradeStacException e) {
            throw new BestXException(String.format("Error managing marketDataRequest [%s]", tsMarketDataRequest), e);
        }
    }

    /*
     * At the moment is not possible to notify the request is failed: it has not the information to create the reject classifiedProposal. 
     * It is need to store the request in the map.
     */
    @Override
    public void onBusinessMessageReject(SessionID sessionID, TSBusinessMessageReject tsBusinessMessageReject) throws TradeStacException {
    	LOGGER.error("{}, {}", sessionID, tsBusinessMessageReject);

        MsgType refMsgType = tsBusinessMessageReject.getRefMsgType();
        if (refMsgType == MsgType.MarketDataRequest) {
        	String mdReqID = tsBusinessMessageReject.getBusinessRejectRefID();
        	
        	BLPHelper.LiteMarketDataRequest liteMarketDataRequest = blpHelper.getLiteMarketDataRequest(mdReqID);
        	
        	if (liteMarketDataRequest != null) {
        		Instrument instrument = liteMarketDataRequest.getInstrument();
        		List<String> marketMakerCodes = liteMarketDataRequest.getMarketMakerCodes();
        		String reason = tsBusinessMessageReject.getText() != null ? tsBusinessMessageReject.getText() : "Generic error, see log for details";
        		
        		for (String marketMakerCode : marketMakerCodes) {
        			sendClassifiedProposalReject(instrument, marketMakerCode, reason);
                }
        		
        		blpHelper.removeLiteMarketDataRequest(mdReqID);
        	} else {
        		LOGGER.warn("Unable to retrieve information for the mdReqID specified [{}], skip it: {}", mdReqID, tsBusinessMessageReject);
        	}
        } else {
        	LOGGER.error("{}, {}", sessionID, tsBusinessMessageReject);
        }
    }

    @Override
    public void onMarketDataSnapshotFullRefresh(SessionID sessionID, TSMarketDataSnapshotFullRefresh tsMarketDataSnapshotFullRefresh) throws TradeStacException {
        LOGGER.trace("{}, {}", sessionID, tsMarketDataSnapshotFullRefresh);

        String isinCode = null;
        TSInstrument tsInstrument = tsMarketDataSnapshotFullRefresh.getTSInstrument();
        if (tsInstrument != null && tsInstrument.getSecurityIDSource() == SecurityIDSource.IsinNumber) {
            isinCode = tsInstrument.getSecurityID();
        } else {
            throw new TradeStacException("IsinCode has not been specified: " + tsMarketDataSnapshotFullRefresh);
        }

        try {
            Instrument instrument = blpHelper.getInstrument(isinCode);

            ClassifiedProposal bidClassifiedProposal = blpHelper.getClassifiedProposal(MDEntryType.Bid, tsMarketDataSnapshotFullRefresh, instrument);
            ClassifiedProposal askClassifiedProposal = blpHelper.getClassifiedProposal(MDEntryType.Offer, tsMarketDataSnapshotFullRefresh, instrument);

            // finally invoke the BLP listener
            blpConnectionListener.onClassifiedProposal(instrument, askClassifiedProposal, bidClassifiedProposal);

            String mdReqID = tsMarketDataSnapshotFullRefresh.getMDReqID();
            blpHelper.removeLiteMarketDataRequest(mdReqID, bidClassifiedProposal.getMarketMarketMaker().getMarketSpecificCode());
            
        } catch (BLPException e) {
            String mdReqID = tsMarketDataSnapshotFullRefresh.getMDReqID();
        	BLPHelper.LiteMarketDataRequest liteMarketDataRequest = blpHelper.getLiteMarketDataRequest(mdReqID);
    		Instrument instrument = liteMarketDataRequest.getInstrument();
    		String marketMakerCode = blpHelper.getMarketMaker(tsMarketDataSnapshotFullRefresh.getTSInstrument());
            sendClassifiedProposalReject(instrument, marketMakerCode, "Settlement date missing");
            LOGGER.error(e.getMessage(), e);
        }
    }

    /*
     * At the moment it is managed only if the reject message contains isin and market maker code. To manage the general reject (reject all
     * request) needs to store in the map all request and to verify if all response are been sent for the request.
     */
    @Override
    public void onMarketDataRequestReject(SessionID sessionID, TSMarketDataRequestReject tsMarketDataRequestReject) throws TradeStacException {
        LOGGER.trace("{}, {}", sessionID, tsMarketDataRequestReject);

        String text = tsMarketDataRequestReject.getText();
        LOGGER.info("{}", text);
        if (text.startsWith(BLPHelper.REJECT_MARKET_MAKER_START_CONTAINER)) {
            String marketMakerCode = "";
            String reason = "";
            int endIndex = text.indexOf(BLPHelper.REJECT_MARKET_MAKER_END_CONTAINER);
            int separetorIndex = text.indexOf(BLPHelper.PRICE_SOURCE_MARKET_MAKER_ASSOCIATOR);

            if (separetorIndex >= 0) {
                marketMakerCode = text.substring(separetorIndex + 1, endIndex);
                reason = text.substring(endIndex + 1);

                String mdReqID = tsMarketDataRequestReject.getMDReqID();
            	BLPHelper.LiteMarketDataRequest liteMarketDataRequest = blpHelper.getLiteMarketDataRequest(mdReqID);
        		Instrument instrument = liteMarketDataRequest.getInstrument();

                sendClassifiedProposalReject(instrument, marketMakerCode, reason);
                
                blpHelper.removeLiteMarketDataRequest(mdReqID, marketMakerCode);
            } else {
                LOGGER.error("IsinCode or/and MarketMakerCode have not been specified: {}", tsMarketDataRequestReject);
            }
        } else {
            LOGGER.error("IsinCode or/and MarketMakerCode have not been specified: {}", tsMarketDataRequestReject);
        }
    }
    
    private void sendClassifiedProposalReject(Instrument instrument, String marketMakerCode, String reason) {
    	try {
            ClassifiedProposal bidClassifiedProposal = blpHelper.getClassifiedProposalReject(MDEntryType.Bid, instrument, marketMakerCode, reason);
            ClassifiedProposal askClassifiedProposal = blpHelper.getClassifiedProposalReject(MDEntryType.Offer, instrument, marketMakerCode, reason);

            // finally invoke the BLP listener
            blpConnectionListener.onClassifiedProposal(instrument, askClassifiedProposal, bidClassifiedProposal);

        } catch (BLPException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
        this.instrumentFinder = instrumentFinder;
    }

    public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
        this.marketMakerFinder = marketMakerFinder;
    }

    public void setVenueFinder(VenueFinder venueFinder) {
        this.venueFinder = venueFinder;
    }

    public void setMarketFinder(MarketFinder marketFinder) {
        this.marketFinder = marketFinder;
    }

	@Override
    public TradeStacPreTradeConnectionListener getTradeStacPreTradeConnectionListener() {
	    return blpConnectionListener;
    }

	@Override
	public void requestInstrumentStatus() throws BestXException {
		LOGGER.error("call to unimplemented method requestInstrumentStatus");
	}

}
