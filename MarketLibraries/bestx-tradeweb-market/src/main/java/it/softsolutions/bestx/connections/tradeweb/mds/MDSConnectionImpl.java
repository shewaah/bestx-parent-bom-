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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

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
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.tradestac.api.TradeStacException;
import it.softsolutions.tradestac.client.TradeStacClientSession;
import it.softsolutions.tradestac.fix.field.MDEntryType;
import it.softsolutions.tradestac.fix.field.MsgType;
import it.softsolutions.tradestac.fix.field.SecurityIDSource;
import it.softsolutions.tradestac.fix.field.SecurityListRequestType;
import it.softsolutions.tradestac.fix50.TSBusinessMessageReject;
import it.softsolutions.tradestac.fix50.TSMarketDataRequest;
import it.softsolutions.tradestac.fix50.TSMarketDataRequestReject;
import it.softsolutions.tradestac.fix50.TSMarketDataSnapshotFullRefresh;
import it.softsolutions.tradestac.fix50.TSSecurityList;
import it.softsolutions.tradestac.fix50.TSSecurityListRequest;
import it.softsolutions.tradestac.fix50.component.TSInstrument;
import it.softsolutions.tradestac.fix50.component.TSSecListGrp;
import it.softsolutions.tradestac.fix50.component.TSSecListGrp.TSNoRelatedSym;
import quickfix.ConfigError;
import quickfix.SessionID;

/**
 * MDSConnection to TradeStac. While connecting for the first time to TradeStac,
 * a {@link TSSecurityListRequest} is sent. At the reception of the the entire
 * Security list the cache/memory is updated. Prior to sending request of
 * instrument price snapshot to TradeStac the security list is controlled; in
 * case the instrument is not present in this list a MarketDataRequestReject is
 * immediately sent back.
 * 
 * @author Davide Rossoni
 * 
 */
/*
 * Purpose: this class is mainly for ...
 * 
 * MDSConnection alla prima connessione con TS sottoscrive la
 * SecurityListRequest Riceve SecurityList e aggiorna in memoria / cache la
 * lista degli strumenti negoziabili Alla ricezione di una
 * requestInstrumetnPriceSnapshot, prima di mandare la richiesta a TradeStac,
 * controlla nella cache se lo strumento è negoziabile, in caso contrario
 * risponde subito con una MarketDataRequestReject
 * 
 * 
 * Project Name : bestx-tradeweb-market First created by: davide.rossoni
 * Creation date: 19/dec/2014
 */
public class MDSConnectionImpl extends AbstractTradeStacConnection implements TradeStacPreTradeConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(MDSConnectionImpl.class);

    private TradeStacPreTradeConnectionListener mdsConnectionListener;
    
    private InstrumentFinder instrumentFinder;
    private MarketMakerFinder marketMakerFinder;
    private VenueFinder venueFinder;
    private MarketFinder marketFinder;

    private MDSHelper mdsHelper;
    private TradeStacClientSession tradeStacClientSession;

    private Set<String> securityList = new CopyOnWriteArraySet<>();

    private Executor executor;

    /**
     * Creates a new MDSConnection.
     */
    public MDSConnectionImpl() {
        super(Market.MarketCode.TW + "#mds");
    }

    /**
     * Initialize a new MDSConnection and control that fundamental parameters
     * are not null.
     * 
     * @throws TradeStacException
     *             if an error occurred during the initializzation of the
     *             TradeStac Session.
     * @throws BestXException
     *             if an error occurred at BestX! level.
     * @throws ConfigError
     *             if there is an error in the configuration at fix level.
     */
    @Override
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

        mdsHelper = new MDSHelper(instrumentFinder, marketMakerFinder, venueFinder, marketFinder);
    }

    @Override
    public void setTradeStacPreTradeConnectionListener(TradeStacPreTradeConnectionListener mdsConnectionListener) {
        LOGGER.debug("{}", mdsConnectionListener);

        this.mdsConnectionListener = mdsConnectionListener;
        super.setTradeStacConnectionListener(mdsConnectionListener);
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


        TSMarketDataRequest tsMarketDataRequest = mdsHelper.createMarketDataRequest(instrument, marketMakerCodes);
        try {
            mdsHelper.addLiteMarketDataRequest(tsMarketDataRequest.getMDReqID(), instrument, marketMakerCodes);

            tradeStacClientSession.manageMarketDataRequest(tsMarketDataRequest);

        } catch (TradeStacException e) {
            throw new BestXException(String.format("Error managing marketDataRequest [%s]", tsMarketDataRequest), e);
        }
    }

    @Override
    public void requestInstrumentStatus() throws BestXException {
        LOGGER.info("");

        if (!isConnected()) {
            throw new BestXException("Not connected");
        }

        TSSecurityListRequest tsSecurityListRequest = new TSSecurityListRequest();
        String securityReqID = "" + DateService.currentTimeMillis();
        tsSecurityListRequest.setSecurityReqID(securityReqID);
        tsSecurityListRequest.setSecurityListRequestType(SecurityListRequestType.Product);

        try {
            tradeStacClientSession.manageSecurityListRequest(tsSecurityListRequest);

            // mdsHelper.addLiteMarketDataRequest(tsMarketDataRequest.getMDReqID(),
            // instrument, marketMakerCodes);

        } catch (TradeStacException e) {
            throw new BestXException(String.format("Error managing securityListRequest [%s]", tsSecurityListRequest), e);
        }
    }

    /*
     * At the moment is not possible to notify the request is failed: it has not
     * the information to create the reject classifiedProposal. It is need to
     * store the request in the map.
     */
    @Override
    public void onBusinessMessageReject(SessionID sessionID, TSBusinessMessageReject tsBusinessMessageReject) throws TradeStacException {
        LOGGER.error("{}, {}", sessionID, tsBusinessMessageReject);

        MsgType refMsgType = tsBusinessMessageReject.getRefMsgType();
        if (refMsgType == MsgType.MarketDataRequest) {
            String mdReqID = tsBusinessMessageReject.getBusinessRejectRefID();

            MDSHelper.LiteMarketDataRequest liteMarketDataRequest = mdsHelper.getLiteMarketDataRequest(mdReqID);

            if (liteMarketDataRequest != null) {
                Instrument instrument = liteMarketDataRequest.getInstrument();
                List<String> marketMakerCodes = liteMarketDataRequest.getMarketMakerCodes();
                String reason = tsBusinessMessageReject.getText() != null ? tsBusinessMessageReject.getText() : "Generic error, see log for details";

                for (String marketMakerCode : marketMakerCodes) {
                    sendClassifiedProposalReject(instrument, marketMakerCode, reason);
                }

                mdsHelper.removeLiteMarketDataRequest(mdReqID);
            } else {
                LOGGER.warn("Unable to retrieve information for the mdReqID specified [{}], skip it: {}", mdReqID, tsBusinessMessageReject);
            }
        } else {
            LOGGER.error("{}, {}", sessionID, tsBusinessMessageReject);
        }
    }

    @Override
    public void onMarketDataSnapshotFullRefresh(SessionID sessionID, TSMarketDataSnapshotFullRefresh tsMarketDataSnapshotFullRefresh) throws TradeStacException {
        LOGGER.debug("{}, {}", sessionID, tsMarketDataSnapshotFullRefresh);

        String isinCode = null;
        TSInstrument tsInstrument = tsMarketDataSnapshotFullRefresh.getTSInstrument();
        if (tsInstrument != null && tsInstrument.getSecurityIDSource() == SecurityIDSource.IsinNumber) {
            isinCode = tsInstrument.getSecurityID();
        } else {
            throw new TradeStacException("IsinCode has not been specified: " + tsMarketDataSnapshotFullRefresh);
        }

        try {

            Instrument instrument = mdsHelper.getInstrument(isinCode);

            ClassifiedProposal bidClassifiedProposal = mdsHelper.getClassifiedProposal(MDEntryType.Bid, tsMarketDataSnapshotFullRefresh, instrument);
            ClassifiedProposal askClassifiedProposal = mdsHelper.getClassifiedProposal(MDEntryType.Offer, tsMarketDataSnapshotFullRefresh, instrument);

            // finally invoke the MDS listener
            mdsConnectionListener.onClassifiedProposal(instrument, askClassifiedProposal, bidClassifiedProposal);

            String mdReqID = tsMarketDataSnapshotFullRefresh.getMDReqID();
            mdsHelper.removeLiteMarketDataRequest(mdReqID, bidClassifiedProposal.getMarketMarketMaker().getMarketSpecificCode());

        } catch (MDSException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /*
     * At the moment it is managed only if the reject message contains isin and
     * market maker code. To manage the general reject (reject all request)
     * needs to store in the map all request and to verify if all response are
     * been sent for the request.
     */
    @Override
    public void onMarketDataRequestReject(SessionID sessionID, TSMarketDataRequestReject tsMarketDataRequestReject) throws TradeStacException {
        LOGGER.debug("{}, {}", sessionID, tsMarketDataRequestReject);

        String text = tsMarketDataRequestReject.getText();
        LOGGER.info("{}", text);
        if (text.startsWith(MDSHelper.REJECT_MARKET_MAKER_START_CONTAINER)) {
            String marketMakerCode = "";
            String reason = "";
            int endIndex = text.indexOf(MDSHelper.REJECT_MARKET_MAKER_END_CONTAINER);
            int separatorIndex = text.indexOf(MDSHelper.PRICE_SOURCE_MARKET_MAKER_ASSOCIATOR);

            if (separatorIndex >= 0) {
                marketMakerCode = text.substring(separatorIndex + 1, endIndex);
                reason = text.substring(endIndex + 1);

                String mdReqID = tsMarketDataRequestReject.getMDReqID();
                MDSHelper.LiteMarketDataRequest liteMarketDataRequest = mdsHelper.getLiteMarketDataRequest(mdReqID);
                Instrument instrument = liteMarketDataRequest.getInstrument();

                sendClassifiedProposalReject(instrument, marketMakerCode, reason);

                mdsHelper.removeLiteMarketDataRequest(mdReqID, marketMakerCode);
            } else {
                LOGGER.error("IsinCode or/and MarketMakerCode have not been specified: {}", tsMarketDataRequestReject);
            }
        } else {
            LOGGER.error("IsinCode or/and MarketMakerCode have not been specified: {}", tsMarketDataRequestReject);
        }
    }

    @Override
    public void onSecurityList(SessionID sessionID, TSSecurityList tsSecurityList) throws TradeStacException {
        LOGGER.debug("sessionID = {}, tsSecurityList = {}", sessionID, tsSecurityList);
        executor.execute(new SecurityListWorker(tsSecurityList));
    }

    private class SecurityListWorker implements Runnable {

        private TSSecurityList tsSecurityList;

        public SecurityListWorker(TSSecurityList tsSecurityList) {
            super();
            this.tsSecurityList = tsSecurityList;
        }

        
        @Override
        public void run() {
            TSSecListGrp tsSecListGrp = tsSecurityList.getTsSecListGrp();
            List<TSNoRelatedSym> tsNoRelatedSymList = tsSecListGrp.getTsNoRelatedSymList();
            for (TSNoRelatedSym tsNoRelatedSym : tsNoRelatedSymList) {
                String securityID = tsNoRelatedSym.getTsInstrument().getSecurityID();

                if (!securityList.contains(securityID)) {
                    securityList.add(securityID);
                    if (securityList.size() > 4920) {
                        LOGGER.debug("securityList.size = {}", securityList.size());
                    }
                }
            }
            
            if(tsSecurityList.getLastFragment() != null && tsSecurityList.getLastFragment()){
                /*
                 * This must be printed only once.
                 **/
                LOGGER.debug("RECEIVED THE LAST FRAGMENT OF THE SECURITY LIST");
                
                mdsConnectionListener.onSecurityListCompleted(securityList);
            } 
        }         
    }

    private void sendClassifiedProposalReject(Instrument instrument, String marketMakerCode, String reason) {
        try {
            ClassifiedProposal bidClassifiedProposal = mdsHelper.getClassifiedProposalReject(MDEntryType.Bid, instrument, marketMakerCode, reason);
            ClassifiedProposal askClassifiedProposal = mdsHelper.getClassifiedProposalReject(MDEntryType.Offer, instrument, marketMakerCode, reason);

            // finally invoke the MDS listener
            mdsConnectionListener.onClassifiedProposal(instrument, askClassifiedProposal, bidClassifiedProposal);

        } catch (MDSException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Changes the value of the instrumentFinder.
     * 
     * @param instrumentFinder
     *            the new value of the instrumentFinder.
     */
    public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
        this.instrumentFinder = instrumentFinder;
    }

    /**
     * Changes the value of the marketMakerFinder.
     * 
     * @param marketMakerFinder
     *            the new value of the marketMakerFinder.
     */
    public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
        this.marketMakerFinder = marketMakerFinder;
    }

    /**
     * Changes the value of the venueFinder.
     * 
     * @param venueFinder
     *            the new value of the venueFinder.
     */
    public void setVenueFinder(VenueFinder venueFinder) {
        this.venueFinder = venueFinder;
    }

    /**
     * Changes the value of the marketFinder.
     * 
     * @param marketFinder
     *            the new value of the marketFinder.
     */
    public void setMarketFinder(MarketFinder marketFinder) {
        this.marketFinder = marketFinder;
    }

    /**
     * Set the value of the executor. This is done using Spring file
     * configuration.
     * 
     * @param executor
     *            the new value of the Executor.
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public TradeStacPreTradeConnectionListener getTradeStacPreTradeConnectionListener() {
        return mdsConnectionListener;
    }

}
