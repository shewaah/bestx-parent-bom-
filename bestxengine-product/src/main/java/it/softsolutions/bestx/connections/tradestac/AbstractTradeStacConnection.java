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
package it.softsolutions.bestx.connections.tradestac;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.connections.ConnectionListener;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.tradestac.api.ConnectionStatus;
import it.softsolutions.tradestac.api.TradeStacApplicationCallback;
import it.softsolutions.tradestac.api.TradeStacException;
import it.softsolutions.tradestac.client.TradeStacClientSession;
import it.softsolutions.tradestac.client.TradeStacSessionCallback;
import it.softsolutions.tradestac.fix.field.NetworkRequestType;
import it.softsolutions.tradestac.fix50.TSBusinessMessageReject;
import it.softsolutions.tradestac.fix50.TSExecutionReport;
import it.softsolutions.tradestac.fix50.TSMarketDataIncrementalRefresh;
import it.softsolutions.tradestac.fix50.TSMarketDataRequestReject;
import it.softsolutions.tradestac.fix50.TSMarketDataSnapshotFullRefresh;
import it.softsolutions.tradestac.fix50.TSMassQuoteAcknowledgement;
import it.softsolutions.tradestac.fix50.TSNetworkCounterpartySystemStatusRequest;
import it.softsolutions.tradestac.fix50.TSNetworkCounterpartySystemStatusResponse;
import it.softsolutions.tradestac.fix50.TSOrderCancelReject;
import it.softsolutions.tradestac.fix50.TSOrderMassCancelReport;
import it.softsolutions.tradestac.fix50.TSQuote;
import it.softsolutions.tradestac.fix50.TSQuoteRequestReject;
import it.softsolutions.tradestac.fix50.TSQuoteStatusReport;
import it.softsolutions.tradestac.fix50.TSReject;
import it.softsolutions.tradestac.fix50.TSSecurityList;
import it.softsolutions.tradestac.fix50.TSSecurityStatus;
import it.softsolutions.tradestac.fix50.TSTradeCaptureReport;
import it.softsolutions.tradestac.fix50.TSTradeCaptureReportRequestAck;
import it.softsolutions.tradestac.fix50.TSTradeStacNotification;
import it.softsolutions.tradestac.fix50.TSTradeStacResponse;
import it.softsolutions.tradestac.fix50.TSTradingSessionList;
import it.softsolutions.tradestac.fix50.TSTradingSessionListUpdateReport;
import it.softsolutions.tradestac.fix50.component.TSCompIDReqGrp;
import it.softsolutions.tradestac.fix50.component.TSCompIDStatGrp;
import quickfix.ConfigError;
import quickfix.SessionID;
import quickfix.SessionSettings;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by: davide.rossoni 
 * Creation date: 28/set/2012 
 * 
 **/
@SuppressWarnings("deprecation")
public class AbstractTradeStacConnection implements Connection, TradeStacSessionCallback, TradeStacApplicationCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTradeStacConnection.class);
    
    protected String connectionName;
    private TradeStacConnectionListener tradeStacConnectionListener;

    private String fixConfigFileName = null;
    protected TradeStacClientSession tradeStacClientSession = null;
    
    public AbstractTradeStacConnection(String connectionName) {
        this.connectionName = connectionName;
    }
    
    public TradeStacClientSession getTradeStacClientSession() {
        return tradeStacClientSession;
    }
    
    /**
     * Initializes a newly created Connection
     * 
     * @throws TradeStacException if an error occurred in the FIX connection initialization
     * @throws BestXException if an error occurred
     * @throws ConfigError 
     */
    public void init() throws TradeStacException, BestXException, ConfigError {

        if (fixConfigFileName == null) {
            throw new ObjectNotInitializedException("FixConfigFileName not set");
        }
        if (tradeStacClientSession == null) {
            throw new ObjectNotInitializedException("TradeStacClientSession not set");
        }

		SessionSettings sessionSettings = new SessionSettings(fixConfigFileName);
        tradeStacClientSession.initFIX(sessionSettings, this, this);
    }
    
    @Override
    public void connect() throws BestXException {
        LOGGER.info("[{}]", connectionName);

        try {
            tradeStacClientSession.startFIX();
        } catch (TradeStacException e) {
            LOGGER.info("[{}] Trying to start TradeStac connection: {}", connectionName, e.getMessage());
        }
    }

    @Override
    public void disconnect() throws BestXException {
        LOGGER.info("[{}]", connectionName);
        if (tradeStacClientSession != null) {
            try {
                tradeStacClientSession.stopFIX();
            } catch (TradeStacException e) {
                LOGGER.info("[{}] Trying to stop TradeStac connection: {}", connectionName, e.getMessage());
            }
        }
    }

    @Override
    public boolean isConnected() {
        LOGGER.trace("[{}] {}", connectionName, tradeStacClientSession != null ? tradeStacClientSession.getFIXConnectionStatus() : null);
        return (tradeStacClientSession != null && tradeStacClientSession.getFIXConnectionStatus() == ConnectionStatus.Connected);
    }

    @Override
    public void setConnectionListener(ConnectionListener listener) {
        throw new UnsupportedOperationException();
    }

    public void setFixConfigFileName(String fixConfigFileName) {
        this.fixConfigFileName = fixConfigFileName;
    }

    public void setTradeStacClientSession(TradeStacClientSession tradeStacClientSession) {
        this.tradeStacClientSession = tradeStacClientSession;
    }

    @Override
    public void onExecutionReport(SessionID sessionID, TSExecutionReport tsExecutionReport) throws TradeStacException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onOrderCancelReject(SessionID sessionID, TSOrderCancelReject tsOrderCancelReject) throws TradeStacException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onBusinessMessageReject(SessionID sessionID, TSBusinessMessageReject tsBusinessMessageReject) throws TradeStacException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMarketDataIncrementalRefresh(SessionID sessionID, TSMarketDataIncrementalRefresh tsMarketDataIncrementalRefresh) throws TradeStacException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMarketDataSnapshotFullRefresh(SessionID sessionID, TSMarketDataSnapshotFullRefresh tsMarketDataSnapshotFullRefresh) throws TradeStacException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMarketDataRequestReject(SessionID sessionID, TSMarketDataRequestReject tsMarketDataRequestReject) throws TradeStacException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onTradeStacResponse(SessionID sessionID, TSTradeStacResponse tsTradeStacResponse) throws TradeStacException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onTradeStacNotification(SessionID sessionID, TSTradeStacNotification tsTradeStacNotification) throws TradeStacException {
        throw new UnsupportedOperationException();
    }
    

	@Override
    public void onNetworkCounterpartySystemStatusResponse(SessionID sessionID, TSNetworkCounterpartySystemStatusResponse tsNetworkCounterpartySystemStatusResponse) throws TradeStacException {
        LOGGER.debug("[{}] {}, {}", connectionName, sessionID, tsNetworkCounterpartySystemStatusResponse);

        ConnectionStatus connectionStatus = null;

        TSCompIDStatGrp tsCompIDStatGrp = tsNetworkCounterpartySystemStatusResponse.getTSCompIDStatGrp();
        if (tsCompIDStatGrp != null && tsCompIDStatGrp.getTSNoCompIDsList() != null) {

            for (TSCompIDStatGrp.TSNoCompIDs tsNoCompIDs : tsCompIDStatGrp.getTSNoCompIDsList()) {
                if (tsNoCompIDs.getRefCompID() != null && tsNoCompIDs.getRefCompID().equals(getConnectionName())) {

                    if (tsNoCompIDs.getStatusValue() != null) {
                        switch (tsNoCompIDs.getStatusValue()) {
                        case Connected:
                            connectionStatus = ConnectionStatus.Connected;
                            break;
                        case InProgress:
                            connectionStatus = ConnectionStatus.InProcess;
                            break;
                        case NotConnectedDownExpectedDown:
                        case NotConnectedDownExpectedUp:
                            connectionStatus = ConnectionStatus.NotConnected;
                            break;
                        default:
                            LOGGER.warn("[{}] StatusValue [{}] not managed", connectionName, tsNoCompIDs.getStatusValue());
                            break;
                        }
                    }
                }
            }
        }

        if (connectionStatus != null) {
            tradeStacConnectionListener.onMarketConnectionStatusChange(connectionName, connectionStatus);
            LOGGER.info("[{}] MarketConnectionStatusChange: {}", connectionName, connectionStatus);
        } else {
            LOGGER.info("[{}] Discarded > {}", connectionName, tsNetworkCounterpartySystemStatusResponse);
        }
    }

    @Override
    public void onLogon(SessionID sessionID) {
        LOGGER.info("[{}] {}", connectionName, sessionID);

        if (tradeStacConnectionListener != null) {
            tradeStacConnectionListener.onClientConnectionStatusChange(connectionName, ConnectionStatus.Connected);
        }

        TSNetworkCounterpartySystemStatusRequest tsNetworkCounterpartySystemStatusRequest = createNetworkCounterpartySystemStatusRequest("" + System.currentTimeMillis(), connectionName);
        try {
            tradeStacClientSession.manageNetworkCounterpartySystemStatusRequest(tsNetworkCounterpartySystemStatusRequest);
        } catch (TradeStacException e) {
            LOGGER.error("[{}] Error managing {}: {}", connectionName, tsNetworkCounterpartySystemStatusRequest, e.getMessage(), e);
        }
    }

    @Override
    public void onLogout(SessionID sessionID) {
        LOGGER.info("[{}] {}", connectionName, sessionID);

        if (tradeStacConnectionListener != null) {
            tradeStacConnectionListener.onClientConnectionStatusChange(connectionName, ConnectionStatus.NotConnected);
        }
    }

    /**
     * Creates a Network (Counterparty System) Status Request 
     * 
     * @param networkRequestID Unique identifier for a network resquest.
     * @param refCompId Used to restrict updates/request to specific CompID
     * @return a Network (Counterparty System) Status Request 
     */
    private TSNetworkCounterpartySystemStatusRequest createNetworkCounterpartySystemStatusRequest(String networkRequestID, String refCompId) {
        LOGGER.trace("[{}] {}, {}", connectionName, networkRequestID, refCompId);

        if (networkRequestID == null || refCompId == null) {
            throw new IllegalArgumentException("params can't be null");
        }

        TSNetworkCounterpartySystemStatusRequest tsSystemStatusRequest = new TSNetworkCounterpartySystemStatusRequest();
        tsSystemStatusRequest.setNetworkRequestID(networkRequestID);
        tsSystemStatusRequest.setNetworkRequestType(NetworkRequestType.Subscribe);

        TSCompIDReqGrp tsCompIDReqGrp = new TSCompIDReqGrp();
        List<TSCompIDReqGrp.TSNoCompIDs> tsNoCompIDsList = new ArrayList<TSCompIDReqGrp.TSNoCompIDs>();
        TSCompIDReqGrp.TSNoCompIDs e = new TSCompIDReqGrp.TSNoCompIDs();

        e.setRefCompID(refCompId);
        tsNoCompIDsList.add(e);
        tsCompIDReqGrp.setTSNoCompIDsList(tsNoCompIDsList);
        tsSystemStatusRequest.setTSCompIDReqGrp(tsCompIDReqGrp);

        return tsSystemStatusRequest;
    }
    
    @Override
    public String getConnectionName() {
        return connectionName;
    }

    public void setTradeStacConnectionListener(TradeStacConnectionListener tradeStacConnectionListener) {
        this.tradeStacConnectionListener = tradeStacConnectionListener;
    }

    @Override
    public void onQuote(SessionID sessionID, TSQuote tsQuote) throws TradeStacException {
        throw new UnsupportedOperationException();
		// Not managed
    }

    @Override
    public void onQuoteRequestReject(SessionID sessionID, TSQuoteRequestReject tsQuoteRequestReject) throws TradeStacException {
        throw new UnsupportedOperationException();
		// Not managed
    }

    @Override
    public void onQuoteStatusReport(SessionID sessionID, TSQuoteStatusReport tsQuoteStatusReport) throws TradeStacException {
        throw new UnsupportedOperationException();
		// Not managed
    }

	@Override
    public void onTradingSessionList(SessionID sessionID, TSTradingSessionList tsTradingSessionList) throws TradeStacException {
        throw new UnsupportedOperationException();
		// Not managed
    }

	@Override
    public void onTradingSessionListUpdateReport(SessionID sessionID, TSTradingSessionListUpdateReport tsTradingSessionListUpdateReport) throws TradeStacException {
        throw new UnsupportedOperationException();
		// Not managed
    }

	@Override
    public void onMassQuoteAcknowledgement(SessionID sessionID, TSMassQuoteAcknowledgement tsMassQuoteAcknowledgement) throws TradeStacException {
        throw new UnsupportedOperationException();
		// Not managed
    }

	@Override
    public void onOrderMassCancelReport(SessionID sessionID, TSOrderMassCancelReport tsOrderMassCancelReport) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed
    }

	@Override
    public void onReject(SessionID sessionID, TSReject tsReject) throws TradeStacException {
        throw new UnsupportedOperationException();
        // Not managed
    }

	@Override
    public void onSecurityList(SessionID sessionID, TSSecurityList tsSecurityList) throws TradeStacException {
        throw new UnsupportedOperationException();
        // Not managed
    }

	@Override
	public void onSecurityStatus(SessionID sessionID, TSSecurityStatus tsSecurityStatus) throws TradeStacException {
        throw new UnsupportedOperationException();
        // Not managed
		
	}

	@Override
	public void onTradeCaptureReport(SessionID sessionID, TSTradeCaptureReport tsTradeCaptureReport)
			throws TradeStacException {
        throw new UnsupportedOperationException();
        // Not managed
		
	}

	@Override
	public void onTradeCaptureReportRequestAck(SessionID sessionID,
			TSTradeCaptureReportRequestAck tsTradeCaptureReportRequestAck) throws TradeStacException {
        throw new UnsupportedOperationException();
        // Not managed
		
	}

}
