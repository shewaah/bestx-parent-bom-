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
package it.softsolutions.bestx.connections.tradestac2;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.connections.ConnectionListener;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.marketlibraries.quickfixjrootobjects.RootNetworkCounterpartySystemStatusResponse;
import it.softsolutions.marketlibraries.quickfixjrootobjects.groups.RootNoCompIDs;
import it.softsolutions.tradestac2.api.ConnectionStatus;
import it.softsolutions.tradestac2.api.TradeStacApplicationCallback;
import it.softsolutions.tradestac2.api.TradeStacException;
import it.softsolutions.tradestac2.client.TradeStacClientSession;
import it.softsolutions.tradestac2.client.TradeStacSessionCallback;
import quickfix.ConfigError;
import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.NetworkRequestID;
import quickfix.field.NetworkRequestType;
import quickfix.field.RefCompID;
import quickfix.field.StatusValue;
import quickfix.fix50sp2.NetworkCounterpartySystemStatusRequest;



/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by: davide.rossoni 
 * Creation date: 28/set/2012 
 * 
 **/
public abstract class AbstractTradeStac2Connection implements Connection, TradeStacSessionCallback, TradeStacApplicationCallback {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTradeStac2Connection.class);

	protected String connectionName;
	protected TradeStacConnectionListener tradeStacConnectionListener;

	private String fixConfigFileName = null;
	protected TradeStacClientSession tradeStacClientSession = null;

	public AbstractTradeStac2Connection(String connectionName) {
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

		//		SessionSettings sessionSettings = new SessionSettings(fixConfigFileName);
		tradeStacClientSession.initFIX(fixConfigFileName, this, this);
	}

	@Override
	public void connect() throws BestXException {
		LOGGER.info("[{}]", connectionName);
		try {
			tradeStacClientSession.startFIX();
		} catch (TradeStacException e) {
			LOGGER.info("[{}] Error starting TradeStac connection: {}", connectionName, e.getMessage(), e);
		}
	}

	@Override
	public void disconnect() throws BestXException {
		LOGGER.info("[{}]", connectionName);
		if (tradeStacClientSession != null) {
			try {
				tradeStacClientSession.stopFIX();
			} catch (TradeStacException e) {
				LOGGER.info("[{}] Error stopping TradeStac connection: {}", connectionName, e.getMessage(), e);
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
	public void onExecutionReport(SessionID sessionID, Message tsExecutionReport) throws TradeStacException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onOrderCancelReject(SessionID sessionID, Message tsOrderCancelReject) throws TradeStacException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onBusinessMessageReject(SessionID sessionID, Message tsBusinessMessageReject, Message relatedMessage) throws TradeStacException {
		LOGGER.error("Received business reject message:\n{}", tsBusinessMessageReject);
	}

	@Override
	public void onMarketDataIncrementalRefresh(SessionID sessionID, Message tsMarketDataIncrementalRefresh) throws TradeStacException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onMarketDataSnapshotFullRefresh(SessionID sessionID, Message tsMarketDataSnapshotFullRefresh) throws TradeStacException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onMarketDataRequestReject(SessionID sessionID, Message tsMarketDataRequestReject) throws TradeStacException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onTradeStacResponse(SessionID sessionID, Message tsTradeStacResponse) throws TradeStacException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onTradeStacNotification(SessionID sessionID, Message tsTradeStacNotification) throws TradeStacException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onLogon(SessionID sessionID) {
		LOGGER.info("[{}] {}", connectionName, sessionID);

		if (tradeStacConnectionListener != null) {
			tradeStacConnectionListener.onClientConnectionStatusChange(connectionName, ConnectionStatus.Connected);
		}

		NetworkCounterpartySystemStatusRequest networkCounterpartySystemStatusRequest = createNetworkCounterpartySystemStatusRequest("" + System.currentTimeMillis(), connectionName);
		try {
			tradeStacClientSession.manageNetworkCounterpartySystemStatusRequest(networkCounterpartySystemStatusRequest);
		} catch (TradeStacException e) {
			LOGGER.error("[{}] Error managing {}: {}", connectionName, networkCounterpartySystemStatusRequest, e.getMessage(), e);
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
	private NetworkCounterpartySystemStatusRequest createNetworkCounterpartySystemStatusRequest(String networkRequestID, String refCompId) {
		LOGGER.trace("[{}] {}, {}", connectionName, networkRequestID, refCompId);

		if (networkRequestID == null || refCompId == null) {
			throw new IllegalArgumentException("params can't be null");
		}

		NetworkCounterpartySystemStatusRequest systemStatusRequest = new NetworkCounterpartySystemStatusRequest();
		systemStatusRequest.set(new NetworkRequestID(networkRequestID));
		systemStatusRequest.set(new NetworkRequestType(NetworkRequestType.SUBSCRIBE));

		NetworkCounterpartySystemStatusRequest.NoCompIDs compIDReqGrp = new NetworkCounterpartySystemStatusRequest.NoCompIDs();
		compIDReqGrp.set(new RefCompID(refCompId));        
		systemStatusRequest.addGroup(compIDReqGrp);

		return systemStatusRequest;
	}

	@Override
	public String getConnectionName() {
		return connectionName;
	}

	public void setTradeStacConnectionListener(TradeStacConnectionListener tradeStacConnectionListener) {
		this.tradeStacConnectionListener = tradeStacConnectionListener;
	}

	@Override
	public void onQuote(SessionID sessionID, Message tsQuote) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed
	}

	@Override
	public void onQuoteRequestReject(SessionID sessionID, Message tsQuoteRequestReject) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed
	}

	@Override
	public void onQuoteStatusReport(SessionID sessionID, Message tsQuoteStatusReport) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed
	}

	@Override
	public void onTradingSessionList(SessionID sessionID, Message tsTradingSessionList) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed
	}

	@Override
	public void onTradingSessionListUpdateReport(SessionID sessionID, Message tsTradingSessionListUpdateReport) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed
	}

	@Override
	public void onMassQuoteAcknowledgement(SessionID sessionID, Message tsMassQuoteAcknowledgement) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed
	}

	@Override
	public void onOrderMassCancelReport(SessionID sessionID, Message tsOrderMassCancelReport) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed
	}

	@Override
	public void onReject(SessionID sessionID, Message tsReject, Message relatedMessage) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed
	}

	@Override
	public void onSecurityList(SessionID sessionID, Message tsSecurityList) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed
	}

	@Override
	public void onSecurityStatus(SessionID sessionID, Message tsSecurityStatus) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed

	}

	@Override
	public void onTradeCaptureReport(SessionID sessionID, Message tsTradeCaptureReport)
			throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed

	}

	@Override
	public void onTradeCaptureReportRequestAck(SessionID sessionID,
			Message tsTradeCaptureReportRequestAck) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed

	}

	@Override
	public void onGenericMessage(SessionID arg0, Message arg1) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed
	}

	@Override
	public void onListStatus(SessionID arg0, Message arg1) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed
	}

	@Override
	public void onNetworkCounterpartySystemStatusResponse(SessionID sessionID, Message message) throws TradeStacException {
		LOGGER.debug("[{}] {}, {}", connectionName, sessionID, message);
		RootNetworkCounterpartySystemStatusResponse networkCounterpartySystemStatusResponse;
		try {
			networkCounterpartySystemStatusResponse = (RootNetworkCounterpartySystemStatusResponse) message;
		} catch(@SuppressWarnings("unused") ClassCastException e){
			LOGGER.error("received a message of wrong type:expected a NetworkCounterpartySystemStatusResponse while message was {}", message.toString());
			return;
		}

		try {
			networkCounterpartySystemStatusResponse.getNoCompIDsValue();
		} catch(@SuppressWarnings("unused") FieldNotFound e) {
			return;
		}
		ConnectionStatus connectionStatus = null;
		
//		CompIDStatGrp component;
//		try {
//			component = networkCounterpartySystemStatusResponse.getCompIDStatGrpComponent();
//		} catch (FieldNotFound e) {
//			throw new TradeStacException(e);
//		} 
		List<RootNoCompIDs> list;
		try {
			list = networkCounterpartySystemStatusResponse.getNoCompIDsList();
			for(RootNoCompIDs compIDs : list){  
				if(compIDs.getString(RefCompID.FIELD).equals(getConnectionName())) {
					switch(compIDs.getInt(StatusValue.FIELD)) {
					case StatusValue.CONNECTED:
						connectionStatus = ConnectionStatus.Connected;
						break;
					case StatusValue.IN_PROCESS:
						connectionStatus = ConnectionStatus.InProcess;
						break;
					case StatusValue.NOT_CONNECTED_DOWN_EXPECTED_DOWN:
					case StatusValue.NOT_CONNECTED_DOWN_EXPECTED_UP:
						connectionStatus = ConnectionStatus.NotConnected;
						break;
					default:
						LOGGER.warn("[{}] StatusValue [{}] not managed", connectionName, compIDs.getInt(StatusValue.FIELD));
						break;
					}
				}
			}
		} catch (FieldNotFound e) {
			throw new TradeStacException(e);
		}  
	
	
		if (connectionStatus != null) {
			tradeStacConnectionListener.onMarketConnectionStatusChange(connectionName, connectionStatus);
			LOGGER.info("[{}] MarketConnectionStatusChange: {}", connectionName, connectionStatus);
		} else {
			LOGGER.info("[{}] Discarded > {}", connectionName, networkCounterpartySystemStatusResponse);
		}
	}

	@Override
	public void onExecutionAck(SessionID sessionID, Message executionAck) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed
	}

	@Override
	public void onQuoteAcknowledgement(SessionID sessionID, Message quoteAck) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed
	}

	@Override
	public void onQuoteRequest(SessionID sessionID, Message quoteRequest) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed
	}

	@Override
	public void onQuoteResponse(SessionID sessionID, Message quoteResponse) throws TradeStacException {
		throw new UnsupportedOperationException();
		// Not managed
	}


}
