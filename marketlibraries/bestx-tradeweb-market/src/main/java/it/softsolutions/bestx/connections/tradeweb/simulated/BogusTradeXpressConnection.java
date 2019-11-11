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
package it.softsolutions.bestx.connections.tradeweb.simulated;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.ConnectionListener;
import it.softsolutions.bestx.connections.tradeweb.tradexpress.TradeXpressConnection;
import it.softsolutions.bestx.connections.tradeweb.tradexpress.TradeXpressConnectionListener;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.tradestac.api.ConnectionStatus;
import it.softsolutions.tradestac.fix.field.ExecType;
import it.softsolutions.tradestac.fix.field.OrdStatus;
import it.softsolutions.tradestac.fix.field.PartyIDSource;
import it.softsolutions.tradestac.fix.field.PartyRole;
import it.softsolutions.tradestac.fix50.TSExecutionReport;
import it.softsolutions.tradestac.fix50.TSNoPartyID;
import it.softsolutions.tradestac.fix50.component.TSCompDealersGrpComponent;
import it.softsolutions.tradestac.fix50.component.TSParties;
import quickfix.Field;
import quickfix.Group;
import tw.quickfix.field.CompDealerID;
import tw.quickfix.field.CompDealerQuote;
import tw.quickfix.field.CompDealerQuoteType;
import tw.quickfix.field.CompDealerStatus;
import tw.quickfix.field.MiFIDMIC;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-tradeweb-market
 * First created by: davide.rossoni
 * Creation date: 30/gen/2015
 * 
 */
public class BogusTradeXpressConnection implements TradeXpressConnection {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(BogusTradeXpressConnection.class);
	
	private TradeXpressConnectionListener tradeXpressConnectionListener;
	private static Map<String, String> switchExecutionBrokers = new HashMap<String, String>();
	
	static {
		switchExecutionBrokers.put("DLRX", "DLRY");
		switchExecutionBrokers.put("DLRY", "DLRX");
		switchExecutionBrokers.put("DLRW", "DLRZ");
		switchExecutionBrokers.put("DLRZ", "DLRW");
	}
		
	public void setRejectIsins(List<String> rejectIsins) {
		this.rejectIsins = rejectIsins;
	}

	public void setCancelIsins(List<String> cancelIsins) {
		this.cancelIsins = cancelIsins;
	}

	public void setWaitingIsins(List<String> waitingIsins) {
		this.waitingIsins = waitingIsins;
	}
	
	private List<String> cancelIsins = new ArrayList<String>();
	
	private List<String> rejectIsins = new ArrayList<String>();
	
	private List<String> waitingIsins = new ArrayList<String>();
	
	public void init() {
		
		new Thread() {
    		@Override
    		public void run() {
    			try { Thread.sleep(3000); } catch (InterruptedException e) { }
    			
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
	    ;
    }

	@Override
    public void disconnect() throws BestXException {
	    ;
    }

	@Override
    public boolean isConnected() {
	    return true;
    }

	@Override
    public void setConnectionListener(ConnectionListener listener) {
	    ;
    }

	@Override
    public void setTradeXpressConnectionListener(TradeXpressConnectionListener tradeXpressConnectionListener) {
		this.tradeXpressConnectionListener = tradeXpressConnectionListener;	    
    }

	@Override
	public void sendOrder(MarketOrder marketOrder) throws BestXException {
		LOGGER.debug("marketOrder = {}", marketOrder);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {}

		sendNewExecutionReport(marketOrder);

		if(cancelIsins.isEmpty() || !cancelIsins.contains("US912810QX90")) cancelIsins.add("US912810QX90");
		if(!cancelIsins.contains("XS1897488091"))cancelIsins.add("XS1897488091");
		if(!cancelIsins.contains("US912810EC81"))cancelIsins.add("US912810EC81");
//		if(!cancelIsins.contains("TRFAKBK11926"))cancelIsins.add("TRFAKBK11926");
//		cancelIsins.remove("US912810EC81");

		if (!waitingIsins.contains(marketOrder.getInstrument().getIsin())) {
			if (cancelIsins.contains(marketOrder.getInstrument().getIsin())) {
				sendCancelledExecutionReport(marketOrder);
			} else if (rejectIsins.contains(marketOrder.getInstrument().getIsin())){  //rejectIsins.add("XS0365323608");
				sendOrderReject(marketOrder);										//rejectIsins.remove("XS0365323608");
			} else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
				sendFilledExecutionReport(marketOrder);
			}
		}
	}


	@Override
    public void cancelOrder(MarketOrder marketOrder) throws BestXException {
		LOGGER.debug("marketOrder = {}", marketOrder);
		
		sendOrderCancelReject(marketOrder);
		
	    try {
          Thread.sleep(30000);
       } catch (InterruptedException e) {}
		
		sendFilledExecutionReport(marketOrder);
		
		//Response to Order Cancel with CANCEL status
//      String sessionId = "sessionID";
//      String clOrdId = marketOrder.getMarketSessionId();
//      TSExecutionReport execReport = new TSExecutionReport();
//      execReport.setExecType(ExecType.Canceled);
//      execReport.setOrdStatus(OrdStatus.Canceled);
//      execReport.setAccruedInterestAmt(null);
//      execReport.setLastPx(0.0);
//      execReport.setExecID("C#" + System.currentTimeMillis());
//      execReport.setSettlDate(DateUtils.addDays(new Date(), 3));
//      execReport.setTransactTime(new Date());
//      execReport.setText(null);
//		
//      tradeXpressConnectionListener.onExecutionReport(sessionId, clOrdId, execReport);
	}
	
	private void sendNewExecutionReport(MarketOrder marketOrder) {
		String sessionId = "sessionID";
		String clOrdId = marketOrder.getMarketSessionId();
		TSExecutionReport execReport = new TSExecutionReport();
		execReport.setExecType(ExecType.New);
		execReport.setOrdStatus(OrdStatus.New);
		execReport.setAccruedInterestAmt(null);
		execReport.setLastPx(0.0);
		execReport.setExecID("C#" + System.currentTimeMillis());
		execReport.setSettlDate(DateUtils.addDays(new Date(), 3));
		execReport.setTransactTime(new Date());
		execReport.setText(null);

		tradeXpressConnectionListener.onExecutionReport(sessionId, clOrdId, execReport);
	}

	private void sendFilledExecutionReport(MarketOrder marketOrder) {
		String sessionId = "sessionID";
		String clOrdId = marketOrder.getMarketSessionId();
		TSExecutionReport execReport = new TSExecutionReport();
		execReport.setExecType(ExecType.Trade);
		execReport.setOrdStatus(OrdStatus.Filled);
		if(marketOrder.getLimit() != null)
			execReport.setLastPx(marketOrder.getLimit().getAmount().doubleValue());
		else 
			execReport.setLastPx(100.0);
		execReport.setExecID("C#" + System.currentTimeMillis());
		execReport.setSettlDate(DateUtils.addDays(new Date(), 3));
		execReport.setTransactTime(new Date());
		execReport.setText(null);
		execReport.setAccruedInterestAmt(12056.0);
		execReport.setNumDaysInterest(124);
		
		List<Field<?>> customFields = new ArrayList<Field<?>>();
		MiFIDMIC mic = new MiFIDMIC("TREU");
		customFields.add(mic);
		execReport.setCustomFields(customFields);
		
		TSParties tsp = new TSParties();
		ArrayList<TSNoPartyID> tsNoPartyIDsList = new ArrayList<TSNoPartyID>();
		if(marketOrder.getMarketMarketMaker() != null)
			tsNoPartyIDsList.add(new TSNoPartyID(marketOrder.getMarketMarketMaker().getMarketSpecificCode(), PartyIDSource.BIC, PartyRole.ExecutingFirm));
		else
			tsNoPartyIDsList.add(new TSNoPartyID("NoKnownDealer", PartyIDSource.BIC, PartyRole.ExecutingFirm));
			//tsNoPartyIDsList.add(new TSNoPartyID("DLRW", PartyIDSource.BIC, PartyRole.ExecutingFirm));
		tsp.setTSNoPartyIDsList(tsNoPartyIDsList);
		execReport.setTSParties(tsp);
		//TODO add MiFID II fields

		TSCompDealersGrpComponent compdealersComp = new TSCompDealersGrpComponent();

		Group compdealer2 = new Group(10009, 10010, new int[] { 10010, 10011, 10012, 10015, 10016, 0 });
		compdealer2.setField(new CompDealerQuote(100.674));
		compdealer2.setField(new CompDealerID("DLRW"));
		compdealer2.setField(new CompDealerQuoteType(1));
		compdealersComp.addGroup(compdealer2);
		
		Group compdealer3 = new Group(10009, 10010, new int[] { 10010, 10011, 10012, 10015, 10016, 0 });
//		compdealer3.setField(new CompDealerStatus(6));
		compdealer3.setField(new CompDealerQuote(101.435));
		compdealer3.setField(new CompDealerID("DLRB"));	
//		compdealersComp.addGroup(compdealer3);
		
		Group compdealer4 = new Group(10009, 10010, new int[] { 10010, 10011, 10012, 10015, 10016, 0 });
		compdealer4.setField(new CompDealerStatus(2));
		compdealer4.setField(new CompDealerQuote(0.0));
		compdealer4.setField(new CompDealerID("DLRZ"));	
//		compdealersComp.addGroup(compdealer4);

		Group compdealer5 = new Group(10009, 10010, new int[] { 10010, 10011, 10012, 10015, 10016, 0 });
		compdealer5.setField(new CompDealerStatus(2));
		compdealer5.setField(new CompDealerID("DLRK"));	
//		compdealersComp.addGroup(compdealer5);

		execReport.addCustomComponent(compdealersComp);
		
		tradeXpressConnectionListener.onExecutionReport(sessionId, clOrdId, execReport);	
	}

	private void sendCancelledExecutionReport(MarketOrder marketOrder) {
		String sessionId = "sessionID";
		String clOrdId = marketOrder.getMarketSessionId();

		TSExecutionReport execReport = new TSExecutionReport();
		execReport.setExecType(ExecType.Canceled);
		execReport.setOrdStatus(OrdStatus.Canceled);
		execReport.setAccruedInterestAmt(null);
		execReport.setLastPx(marketOrder.getLimit() == null ? 0.0: marketOrder.getLimit().getAmount().doubleValue());
		execReport.setExecID("C#" + System.currentTimeMillis());
		execReport.setSettlDate(DateUtils.addDays(new Date(), 3));
		execReport.setTransactTime(new Date());
		execReport.setText("Target price not met/Quoted:DLRX:103.27;DLRY:103.27");
		
		if(marketOrder.getMarketMarketMaker() != null) {
			TSParties tsp = new TSParties();
			ArrayList<TSNoPartyID> tsNoPartyIDsList = new ArrayList<TSNoPartyID>();
			tsNoPartyIDsList.add(new TSNoPartyID(marketOrder.getMarketMarketMaker().getMarketSpecificCode(), PartyIDSource.BIC, PartyRole.ExecutingFirm));
			tsp.setTSNoPartyIDsList(tsNoPartyIDsList);
			execReport.setTSParties(tsp);
		}
		//TODO add MiFID II fields
				
		tradeXpressConnectionListener.onExecutionReport(sessionId, clOrdId, execReport);
	}

	private void sendOrderReject(MarketOrder marketOrder) {
		String sessionId = "sessionID";
		String clOrdId = marketOrder.getMarketSessionId();
		String reason = "Don't like your manners";
		
		tradeXpressConnectionListener.onOrderReject(sessionId, clOrdId, reason);
	}

	private void sendOrderCancelReject(MarketOrder marketOrder) {
		String sessionId = "sessionID";
		String clOrdId = marketOrder.getMarketSessionId();
		String reason = "Up yours!";
		
		tradeXpressConnectionListener.onOrderCancelReject(sessionId, clOrdId, reason);
	}



}
