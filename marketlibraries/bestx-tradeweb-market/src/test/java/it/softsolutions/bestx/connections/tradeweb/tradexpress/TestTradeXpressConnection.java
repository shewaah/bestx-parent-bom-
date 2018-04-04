/**
 * Copyright 1997-2015 SoftSolutions! srl 
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.ConnectionListener;
import it.softsolutions.bestx.connections.tradeweb.simulated.BogusTradeXpressConnection;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.tradestac.api.ConnectionStatus;
import it.softsolutions.tradestac.fix.field.ExecType;
import it.softsolutions.tradestac.fix.field.OrdStatus;
import it.softsolutions.tradestac.fix.field.PartyIDSource;
import it.softsolutions.tradestac.fix.field.PartyRole;
import it.softsolutions.tradestac.fix50.TSExecutionReport;
import it.softsolutions.tradestac.fix50.TSNoPartyID;
import it.softsolutions.tradestac.fix50.component.TSParties;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-tradeweb-market
 * First created by: davide.rossoni
 * Creation date: 30/gen/2015
 * 
 */
public class TestTradeXpressConnection implements TradeXpressConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(BogusTradeXpressConnection.class);
	
	private TradeXpressConnectionListener tradeXpressConnectionListener;

	public void setExecuteIsins(List<String> executeIsins) {
		this.executeIsins = executeIsins;
	}

	public void setCancelIsins(List<String> cancelIsins) {
		this.cancelIsins = cancelIsins;
	}

	public void setRejectIsins(List<String> rejectIsins) {
		this.rejectIsins = rejectIsins;
	}

	private List<String> executeIsins = new ArrayList<String>();
	private List<String> cancelIsins = new ArrayList<String>();
	private List<String> rejectIsins = new ArrayList<String>();
	
	public void init() {
		
		new Thread() {
    		@Override
    		public void run() {
    			try { Thread.sleep(2000); } catch (InterruptedException e) { }
    			
    			tradeXpressConnectionListener.onMarketConnectionStatusChange(getConnectionName(), ConnectionStatus.Connected);
    		}
    	}.start();
	}

	@Override
    public String getConnectionName() {
	    return "TradeXpress";
    }

	@Override
    public void connect() throws BestXException {
//		nothing to do
		}

	@Override
    public void disconnect() throws BestXException {
//		nothing to do
	}

	@Override
    public boolean isConnected() {
	    return true;
    }

	@Override
    public void setConnectionListener(ConnectionListener listener) {
//		nothing to do
		}

	@Override
    public void setTradeXpressConnectionListener(TradeXpressConnectionListener tradeXpressConnectionListener) {
		this.tradeXpressConnectionListener = tradeXpressConnectionListener;	    
    }

	@Override
    public void sendOrder(MarketOrder marketOrder) throws BestXException {
		LOGGER.debug("marketOrder = {}", marketOrder);

		sendNewExecutionReport(marketOrder);
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {}
		
		if (executeIsins.contains(marketOrder.getInstrument().getIsin())) {
			sendFilledExecutionReport(marketOrder);
		} else if (cancelIsins.contains(marketOrder.getInstrument().getIsin())) {
			sendCancelledExecutionReport(marketOrder);
		} else if (rejectIsins.contains(marketOrder.getInstrument().getIsin())){
			if (Math.random() > 0.4) {
				sendOrderReject(marketOrder);
			} else {
				sendFilledExecutionReport(marketOrder);
			}
		}
    }

	@Override
    public void cancelOrder(MarketOrder marketOrder) throws BestXException {
		LOGGER.debug("marketOrder = {}", marketOrder);
        

    }
	
	private void sendNewExecutionReport(MarketOrder marketOrder) {
		String sessionId = "sessionID";
		String clOrdId = marketOrder.getMarketSessionId();
		TSExecutionReport execReport = new TSExecutionReport();
		execReport.setExecType(ExecType.New);
		execReport.setOrdStatus(OrdStatus.New);
		execReport.setAccruedInterestAmt(null);
		execReport.setLastPx(marketOrder.getLimit().getAmount().doubleValue());
		execReport.setExecID("C#" + System.currentTimeMillis());
		execReport.setSettlDate(DateUtils.addDays(new Date(), 3));
		execReport.setTransactTime(new Date());
		execReport.setText(null);

		TSParties tsp = new TSParties();
		ArrayList<TSNoPartyID> tsNoPartyIDsList = new ArrayList<TSNoPartyID>();
		tsNoPartyIDsList.add(new TSNoPartyID(marketOrder.getMarketMarketMaker().getMarketSpecificCode(), PartyIDSource.BIC, PartyRole.ExecutingFirm));
		tsp.setTSNoPartyIDsList(tsNoPartyIDsList);
		execReport.setTSParties(tsp);
		
		tradeXpressConnectionListener.onExecutionReport(sessionId, clOrdId, execReport);
	}

	private void sendFilledExecutionReport(MarketOrder marketOrder) {
		String sessionId = "sessionID";
		String clOrdId = marketOrder.getMarketSessionId();
		TSExecutionReport execReport = new TSExecutionReport();
		execReport.setExecType(ExecType.Trade);
		execReport.setOrdStatus(OrdStatus.Filled);
		execReport.setAccruedInterestAmt(null);
		execReport.setLastPx(marketOrder.getLimit().getAmount().doubleValue());
		execReport.setExecID("C#" + System.currentTimeMillis());
		execReport.setSettlDate(DateUtils.addDays(new Date(), 3));
		execReport.setTransactTime(new Date());
		execReport.setText(null);

		TSParties tsp = new TSParties();
		ArrayList<TSNoPartyID> tsNoPartyIDsList = new ArrayList<TSNoPartyID>();
		tsNoPartyIDsList.add(new TSNoPartyID(marketOrder.getMarketMarketMaker().getMarketSpecificCode(), PartyIDSource.BIC, PartyRole.ExecutingFirm));
		tsp.setTSNoPartyIDsList(tsNoPartyIDsList);
		execReport.setTSParties(tsp);
		
		tradeXpressConnectionListener.onExecutionReport(sessionId, clOrdId, execReport);
		
	}

	private void sendCancelledExecutionReport(MarketOrder marketOrder) {
		String sessionId = "sessionID";
		String clOrdId = marketOrder.getMarketSessionId();

		TSExecutionReport execReport = new TSExecutionReport();
		execReport.setExecType(ExecType.Canceled);
		execReport.setOrdStatus(OrdStatus.Canceled);
		execReport.setAccruedInterestAmt(null);
		execReport.setLastPx(marketOrder.getLimit().getAmount().doubleValue());
		execReport.setExecID("C#" + System.currentTimeMillis());
		execReport.setSettlDate(DateUtils.addDays(new Date(), 3));
		execReport.setTransactTime(new Date());
		execReport.setText("Trader not available in simulated env");

		TSParties tsp = new TSParties();
		ArrayList<TSNoPartyID> tsNoPartyIDsList = new ArrayList<TSNoPartyID>();
		tsNoPartyIDsList.add(new TSNoPartyID(marketOrder.getMarketMarketMaker().getMarketSpecificCode(), PartyIDSource.BIC, PartyRole.ExecutingFirm));
		tsp.setTSNoPartyIDsList(tsNoPartyIDsList);
		execReport.setTSParties(tsp);
		
		tradeXpressConnectionListener.onExecutionReport(sessionId, clOrdId, execReport);
		
	}

	private void sendOrderReject(MarketOrder marketOrder) {
		String sessionId = "sessionID";
		String clOrdId = marketOrder.getMarketSessionId();
		String reason = "Don't like your manners";
		
		tradeXpressConnectionListener.onOrderReject(sessionId, clOrdId, reason);
		
	}

	@SuppressWarnings("unused")
	private void sendOrderCancelReject(MarketOrder marketOrder) {
		String sessionId = "sessionID";
		String clOrdId = marketOrder.getMarketSessionId();
		String reason = "Up yours!";
		
		tradeXpressConnectionListener.onOrderCancelReject(sessionId, clOrdId, reason);
	}



}
