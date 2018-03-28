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
package it.softsolutions.bestx.connections.bloomberg;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.softsolutions.tradestac.api.ConnectionStatus;
import it.softsolutions.tradestac.api.TradeStacApplicationCallback;
import it.softsolutions.tradestac.api.TradeStacException;
import it.softsolutions.tradestac.client.TradeStacClientSession;
import it.softsolutions.tradestac.client.TradeStacSessionCallback;
import it.softsolutions.tradestac.fix.field.BusinessRejectReason;
import it.softsolutions.tradestac.fix.field.MDEntryType;
import it.softsolutions.tradestac.fix.field.MDReqRejReason;
import it.softsolutions.tradestac.fix.field.MsgType;
import it.softsolutions.tradestac.fix.field.NetworkStatusResponseType;
import it.softsolutions.tradestac.fix.field.StatusValue;
import it.softsolutions.tradestac.fix50.TSBusinessMessageReject;
import it.softsolutions.tradestac.fix50.TSCrossOrderCancelReplaceRequest;
import it.softsolutions.tradestac.fix50.TSCrossOrderCancelRequest;
import it.softsolutions.tradestac.fix50.TSMarketDataRequest;
import it.softsolutions.tradestac.fix50.TSMarketDataRequestReject;
import it.softsolutions.tradestac.fix50.TSMarketDataSnapshotFullRefresh;
import it.softsolutions.tradestac.fix50.TSNetworkCounterpartySystemStatusRequest;
import it.softsolutions.tradestac.fix50.TSNetworkCounterpartySystemStatusResponse;
import it.softsolutions.tradestac.fix50.TSNewOrderCross;
import it.softsolutions.tradestac.fix50.TSNewOrderSingle;
import it.softsolutions.tradestac.fix50.TSNoMDEntryTypes;
import it.softsolutions.tradestac.fix50.TSOrderCancelReplaceRequest;
import it.softsolutions.tradestac.fix50.TSOrderCancelRequest;
import it.softsolutions.tradestac.fix50.TSOrderMassCancelRequest;
import it.softsolutions.tradestac.fix50.TSQuote;
import it.softsolutions.tradestac.fix50.TSQuoteAck;
import it.softsolutions.tradestac.fix50.TSQuoteCancel;
import it.softsolutions.tradestac.fix50.TSQuoteRequest;
import it.softsolutions.tradestac.fix50.TSQuoteResponse;
import it.softsolutions.tradestac.fix50.TSSecurityDefinitionRequest;
import it.softsolutions.tradestac.fix50.TSSecurityListRequest;
import it.softsolutions.tradestac.fix50.TSTradeCaptureReportRequest;
import it.softsolutions.tradestac.fix50.TSTradeStacRequest;
import it.softsolutions.tradestac.fix50.TSTradingSessionListRequest;
import it.softsolutions.tradestac.fix50.component.TSCompIDStatGrp;
import it.softsolutions.tradestac.fix50.component.TSInstrument;
import it.softsolutions.tradestac.fix50.component.TSMDFullGrp;
import quickfix.SessionID;
import quickfix.SessionSettings;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-bloomberg-market First created by: fabrizio.aponte Creation date: 14/mag/2012
 * 
 **/
public class BogusTradeStacClient implements TradeStacClientSession {
    private static String senderCompID = "TRADESTAC_TRANSACTION_INITIATOR";
    private static String targetCompID = "TRADESTAC_ACCEPTOR";
    private static String sessionIDStr = "FIXT.1.1:" + senderCompID + "->" + targetCompID;

    private SessionID sessionID = null;
    private TradeStacApplicationCallback tradeStacApplicationCallback;
    private TradeStacSessionCallback tradeStacSessionCallback;
    private BogusTSClientListener listener;
    private boolean failInit = false;
    private boolean failStart = false;
    private boolean failStop = false;
    private boolean started = false;
    private ConnectionStatus connectionStatus = ConnectionStatus.NotConnected;
    private String networkRequestID;
    private String refCompID;
    private int networkResponseID;
    private List<TSNoMDEntryTypes> mdEntryTypeList;
    private TSInstrument tsInstrument;
    private String mdReqID = "";

    public BogusTradeStacClient(BogusTSClientListener listener) {
        this.listener = listener;
    }

    @Override
    public SessionID getSessionID() {
        return sessionID;
    }

    @Override
    public void initFIX(SessionSettings sessionSettings, TradeStacApplicationCallback tradeStacApplicationCallback, TradeStacSessionCallback tradeStacSessionCallback) throws TradeStacException {
        if (failInit) {
            listener.onInit(false);
            throw new TradeStacException("Impossible to initialize Fix session");
        }
        this.tradeStacApplicationCallback = tradeStacApplicationCallback;
        this.tradeStacSessionCallback = tradeStacSessionCallback;
        listener.onInit(true);
    }

    @Override
    public ConnectionStatus getFIXConnectionStatus() {
        return connectionStatus;
    }

    @Override
    public void startFIX() throws TradeStacException {
        if (started) {
            listener.onStart(false);
            throw new TradeStacException("Already started");
        }
        if (failStart) {
            listener.onStart(false);
            throw new TradeStacException("Impossible to start the fix session");
        }

        started = true;
        listener.onStart(true);
    }

    public void sendLogon(boolean value) {
        if (value) {
            sessionID = new SessionID(sessionIDStr);
            connectionStatus = ConnectionStatus.Connected;
            tradeStacSessionCallback.onLogon(sessionID);
        } else {
            connectionStatus = ConnectionStatus.NotConnected;
        }
    }

    @Override
    public void stopFIX() throws TradeStacException {
        if (!started) {
            listener.onStop(false);
            throw new TradeStacException("Already Stopped");
        }
        if (failStop) {
            listener.onStop(false);
            throw new TradeStacException("Impossible to stop the fic session");
        }
        started = false;
        listener.onStop(true);
        connectionStatus = ConnectionStatus.Stopped;
    }

    public void sendLogout(boolean value) {
        if (value) {
            connectionStatus = ConnectionStatus.NotConnected;
            tradeStacSessionCallback.onLogout(sessionID);
            sessionID = null;
        }
    }

    @Override
    public void manageNetworkCounterpartySystemStatusRequest(TSNetworkCounterpartySystemStatusRequest tsNetworkCounterpartySystemStatusRequest) throws TradeStacException {

        listener.onNetworkStatusRequest(tsNetworkCounterpartySystemStatusRequest);
        if (tsNetworkCounterpartySystemStatusRequest.getTSCompIDReqGrp() != null && tsNetworkCounterpartySystemStatusRequest.getTSCompIDReqGrp().getTSNoCompIDsList() != null
                && tsNetworkCounterpartySystemStatusRequest.getTSCompIDReqGrp().getTSNoCompIDsList().size() > 0
                && tsNetworkCounterpartySystemStatusRequest.getTSCompIDReqGrp().getTSNoCompIDsList().get(0) != null) {
            networkRequestID = tsNetworkCounterpartySystemStatusRequest.getNetworkRequestID();
            refCompID = tsNetworkCounterpartySystemStatusRequest.getTSCompIDReqGrp().getTSNoCompIDsList().get(0).getRefCompID();
        }
    }

    public void sendNetworkStatusResponse(boolean value) throws TradeStacException {

        TSNetworkCounterpartySystemStatusResponse tsNetworkStatusResponse = new TSNetworkCounterpartySystemStatusResponse();
        tsNetworkStatusResponse.setNetworkRequestID(networkRequestID);
        tsNetworkStatusResponse.setLastNetworkResponseID("" + networkResponseID);
        networkResponseID++;
        tsNetworkStatusResponse.setNetworkResponseID("" + networkResponseID);
        tsNetworkStatusResponse.setNetworkStatusResponseType(NetworkStatusResponseType.IncrementalUpdate);
        TSCompIDStatGrp tsCompIDStatGrp = new TSCompIDStatGrp();
        List<TSCompIDStatGrp.TSNoCompIDs> tsNoCompIDsList = new ArrayList<TSCompIDStatGrp.TSNoCompIDs>();
        TSCompIDStatGrp.TSNoCompIDs tsNoCompIDs = new TSCompIDStatGrp.TSNoCompIDs();
        tsNoCompIDs.setRefCompID(refCompID);
        if (value) {
            tsNoCompIDs.setStatusValue(StatusValue.Connected);
        } else {
            tsNoCompIDs.setStatusValue(StatusValue.NotConnectedDownExpectedDown);
        }
        tsNoCompIDsList.add(tsNoCompIDs);
        tsCompIDStatGrp.setTSNoCompIDsList(tsNoCompIDsList);
        tsNetworkStatusResponse.setTSCompIDStatGrp(tsCompIDStatGrp);
        tradeStacApplicationCallback.onNetworkCounterpartySystemStatusResponse(sessionID, tsNetworkStatusResponse);
    }

    @Override
    public void manageMarketDataRequest(TSMarketDataRequest tsMarketDataRequest) throws TradeStacException {
        listener.onMarketDataRequest(tsMarketDataRequest);
        if (tsMarketDataRequest != null && tsMarketDataRequest.getTSInstrmtMDReqGrp() != null && tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList() != null
                && tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().size() > 0 && tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0) != null
                && tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument() != null && tsMarketDataRequest.getTSMDReqGrp() != null
                && tsMarketDataRequest.getTSMDReqGrp().getTSMDEntryTypesList() != null && tsMarketDataRequest.getTSMDReqGrp().getTSMDEntryTypesList().size() > 0
                && tsMarketDataRequest.getTSMDReqGrp().getTSMDEntryTypesList().get(0) != null) {
            tsInstrument = tsMarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument();
            mdEntryTypeList = tsMarketDataRequest.getTSMDReqGrp().getTSMDEntryTypesList();
            mdReqID = tsMarketDataRequest.getMDReqID();
        }
    }

    public void sentSnapshotPrice(double bidPrice, double bidQty, double askPrice, double askQty, Date bidUtcTime, Date askUtcTime) throws TradeStacException {

        TSMarketDataSnapshotFullRefresh tsMarketDataRefresh = new TSMarketDataSnapshotFullRefresh();

        tsMarketDataRefresh.setMDReqID(mdReqID);
        tsMarketDataRefresh.setMDReportID(Integer.getInteger("" + System.currentTimeMillis()));
        tsMarketDataRefresh.setTSInstrument(tsInstrument);

        TSMDFullGrp tsMDFullGrp = new TSMDFullGrp();
        List<TSMDFullGrp.TSNoMDEntries> tsNoMDEntriesList = new ArrayList<TSMDFullGrp.TSNoMDEntries>();
        for (TSNoMDEntryTypes entryType : mdEntryTypeList) {
            if (entryType.getMDEntryType() == MDEntryType.Bid && bidPrice > 0.0) {
                TSMDFullGrp.TSNoMDEntries entry = new TSMDFullGrp.TSNoMDEntries();

                entry.setMDEntryPx(bidPrice);
                entry.setMDEntrySize(bidQty);
                entry.setMDPriceLevel(1);
                entry.setMDEntryTime(bidUtcTime);
                entry.setMDEntryType(MDEntryType.Bid);
                tsNoMDEntriesList.add(entry);
            } else if (entryType.getMDEntryType() == MDEntryType.Offer && askPrice > 0.0) {
                TSMDFullGrp.TSNoMDEntries entry = new TSMDFullGrp.TSNoMDEntries();

                entry.setMDEntryPx(askPrice);
                entry.setMDEntrySize(askQty);
                entry.setMDPriceLevel(1);
                entry.setMDEntryTime(askUtcTime);
                entry.setMDEntryType(MDEntryType.Offer);
                tsNoMDEntriesList.add(entry);
            }
        }

        if (tsNoMDEntriesList.size() > 0) {
            tsMDFullGrp.setTSNoMDEntriesList(tsNoMDEntriesList);
            tsMarketDataRefresh.setTSMDFullGrp(tsMDFullGrp);
        }
        tradeStacApplicationCallback.onMarketDataSnapshotFullRefresh(sessionID, tsMarketDataRefresh);
    }

    public void sentMarketDataReject() throws TradeStacException {

        TSMarketDataRequestReject tsMarketDataRequestReject = new TSMarketDataRequestReject();

        tsMarketDataRequestReject.setMDReqID(mdReqID);
        tsMarketDataRequestReject.setMDReqRejReason(MDReqRejReason.InsufficientPermission);
        String instrumentCode = tsInstrument.getSecurityID();
        String marketMaker = null;
        if (tsInstrument.getTSInstrumentParties() != null && tsInstrument.getTSInstrumentParties().getTSNoInstrumentPartiesList() != null
                && tsInstrument.getTSInstrumentParties().getTSNoInstrumentPartiesList().size() > 0 && tsInstrument.getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0) != null) {
            marketMaker = tsInstrument.getTSInstrumentParties().getTSNoInstrumentPartiesList().get(0).getInstrumentPartyID();
        }
        String text = "[";
        if (instrumentCode != null) {
            text = text + instrumentCode;
        }
        if (marketMaker != null) {
            text = text + "@" + marketMaker;
        }
        text = text + "] Entitlements required for this security are not enabled! [nid:89]";
        tsMarketDataRequestReject.setText(text);

        tradeStacApplicationCallback.onMarketDataRequestReject(sessionID, tsMarketDataRequestReject);
    }

    public void sentBusinessMessageReject() throws TradeStacException {

        TSBusinessMessageReject tsBusinessMessageReject = new TSBusinessMessageReject();
        tsBusinessMessageReject.setRefMsgType(MsgType.MarketDataRequest);
        tsBusinessMessageReject.setBusinessRejectRefID(mdReqID);
        tsBusinessMessageReject.setBusinessRejectReason(BusinessRejectReason.DeliverToFirmNotAvailableAtThisTime);
        tsBusinessMessageReject.setEncodedText("Wrong marketConnector state [Started]");

        tradeStacApplicationCallback.onBusinessMessageReject(sessionID, tsBusinessMessageReject);
    }

    public void sentUpdatePrice(double bidPrice, double askPrice) throws TradeStacException {

        // TSMarketDataIncrementalRefresh tsMarketDataRefresh = new TSMarketDataIncrementalRefresh();
        // TSMDIncGrp tsMDIncGrp = new TSMDIncGrp();
        // List<TSNoMDEntries> tsNoMDEntriesList = new ArrayList<TSNoMDEntries>();
        //
        //
        // TSNoMDEntries tsNoMDEntries = new TSNoMDEntries();
        // tsNoMDEntries.setMdEntryPx(bidPrice);
        // tsNoMDEntries.setTSInstrument(tsInstrument);
        // tsNoMDEntries.setMdEntryType(mdEntryType);
        // tsNoMDEntriesList.add(tsNoMDEntries );
        // tsMDIncGrp.setTSNoMDEntriesList;(tsNoMDEntriesList );
        // tsMarketDataRefresh.setTSMDIncGrp(tsMDIncGrp );
        //
        // tradeStacApplicationCallback.onMarketDataIncrementalRefresh(sessionID, tsMarketDataRefresh);

    }

    @Override
    public void manageNewOrderSingle(TSNewOrderSingle tsNewOrderSingle) throws TradeStacException {
        // TODO Auto-generated method stub

    }

    @Override
    public void manageOrderCancelRequest(TSOrderCancelRequest tsOrderCancelRequest) throws TradeStacException {
        // TODO Auto-generated method stub

    }

    @Override
    public void manageTradeStacRequest(TSTradeStacRequest tsTradeStacRequest) throws TradeStacException {
        // TODO Auto-generated method stub

    }

    public interface BogusTSClientListener {
        void onInit(boolean result);

        void onStart(boolean result);

        void onNetworkStatusRequest(TSNetworkCounterpartySystemStatusRequest tsNetworkCounterpartySystemStatusRequest);

        void onMarketDataRequest(TSMarketDataRequest tsMarketDataRequest);

        void onStop(boolean result);
    }

    @Override
    public void manageSecurityDefinitionRequest(TSSecurityDefinitionRequest tsSecurityDefinitionRequest) throws TradeStacException {
        // TODO Auto-generated method stub

    }

    @Override
    public void manageQuoteRequest(TSQuoteRequest tsQuoteRequest) throws TradeStacException {
        // TODO Auto-generated method stub

    }

    @Override
    public void manageQuoteAck(TSQuoteAck tsQuoteAck) throws TradeStacException {
        // TODO Auto-generated method stub

    }

    @Override
    public void manageQuoteResponse(TSQuoteResponse tsQuoteResponse) throws TradeStacException {
        // TODO Auto-generated method stub

    }

	@Override
    public void manageQuote(TSQuote tsQuote) throws TradeStacException {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void manageNewOrderCross(TSNewOrderCross tsNewOrderCross) throws TradeStacException {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void manageCrossOrderCancelRequest(TSCrossOrderCancelRequest tsCrossOrderCancelRequest) throws TradeStacException {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void manageCrossOrderCancelReplaceRequest(TSCrossOrderCancelReplaceRequest tsCrossOrderCancelReplaceRequest) throws TradeStacException {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void manageTradingSessionListRequest(TSTradingSessionListRequest tsTradingSessionListRequest) throws TradeStacException {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void manageOrderMassCancelRequest(TSOrderMassCancelRequest tsOrderCancelRequest) throws TradeStacException {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void manageOrderCancelReplaceRequest(TSOrderCancelReplaceRequest tsOrderCancelReplaceRequest) throws TradeStacException {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void manageQuoteCancel(TSQuoteCancel tsQuoteCancel) throws TradeStacException {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void manageSecurityListRequest(TSSecurityListRequest tsSecurityListRequest) throws TradeStacException {
	    // TODO Auto-generated method stub
	    
    }

	@Override
	public void manageTradeCaptureReportRequest(TSTradeCaptureReportRequest tradeCaptureReportRequest)
			throws TradeStacException {
		// TODO Auto-generated method stub
		
	}

}
