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
package it.softsolutions.bestx.connections.bloomberg.tsox.simulated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.connections.ConnectionListener;
import it.softsolutions.bestx.connections.bloomberg.tsox.TSOXConnection;
import it.softsolutions.bestx.connections.bloomberg.tsox.TSOXConnectionListener;
import it.softsolutions.bestx.connections.tradestac.AbstractTradeStacConnection;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.PriceType;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Rfq;
import it.softsolutions.tradestac.api.ConnectionStatus;
import it.softsolutions.tradestac.fix.field.ExecType;
import it.softsolutions.tradestac.fix.field.OrdStatus;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-tsox-market
 * First created by: davide.rossoni
 * Creation date: 30/gen/2015
 * 
 */
@SuppressWarnings("unused")
public class BogusTSOXConnection extends AbstractTradeStacConnection implements TSOXConnection {
	
    public BogusTSOXConnection() {
		super(Market.MarketCode.BLOOMBERG + "#tsox");
	}

    private MarketFinder marketFinder;
        
    private Market mkt;
    
    public void setMarketFinder (MarketFinder marketFinder)  {
    	this.marketFinder = marketFinder;
    	try {
			mkt = marketFinder.getMarketByCode(Market.MarketCode.BLOOMBERG,null);
		} catch (BestXException e) {
			LOGGER.info("Cannot find market for market code {} ", Market.MarketCode.BLOOMBERG, e);
		}
    }
    
	private static final Logger LOGGER = LoggerFactory.getLogger(BogusTSOXConnection.class);
	
    private TSOXConnectionListener tsoxConnectionListener;	
	
	private static List<String> cancelIsins = new ArrayList<String>();
	
	static {
		cancelIsins.add("AA0005240830");
	}

	private static List<String> rejectIsins = new ArrayList<String>();
	
	static {
		rejectIsins.add("XS0451037062");
	}
	
   private static List<String> fillIsins = new ArrayList<String>();
   
   static {
	      fillIsins.add("IT0005240830");
	      fillIsins.add("DE000A0DHUM0");
   }
	
	
	
	@Override
	public void init() {
		
		new Thread() {
    		@Override
    		public void run() {
    			try { Thread.sleep(2000); } catch (@SuppressWarnings("unused") InterruptedException e) { }
    			
    			tsoxConnectionListener.onMarketConnectionStatusChange(getConnectionName(), ConnectionStatus.Connected);
    		}
    	}.start();
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
    public void setTsoxConnectionListener(TSOXConnectionListener tsoxConnectionListener) {
        LOGGER.debug("{}", tsoxConnectionListener);

        this.tsoxConnectionListener = tsoxConnectionListener;
        super.setTradeStacConnectionListener(tsoxConnectionListener);
    }
	@Override
	public void sendRfq(MarketOrder marketOrder) throws BestXException {
		LOGGER.debug("marketOrder = {}", marketOrder);

		//sendNewExecutionReport(marketOrder);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {}

		if (cancelIsins.contains(marketOrder.getInstrument().getIsin())) {
			sendCancelledExecutionReport(marketOrder);
		} else if (rejectIsins.contains(marketOrder.getInstrument().getIsin())){
			sendOrderReject(marketOrder);
      } else if (fillIsins.contains(marketOrder.getInstrument().getIsin())){
         sendFilledExecutionReport(marketOrder);
		} else {
			sendQuote(marketOrder);
		}
	}

	private void sendQuote(MarketOrder marketOrder) {
		ClassifiedProposal proposal = new ClassifiedProposal();
		proposal.setQuoteReqId(marketOrder.getMarketSessionId());
		proposal.setMarket(mkt);
		proposal.setSenderQuoteId("quote:0");
		proposal.setFutSettDate(marketOrder.getFutSettDate());
		proposal.setMarketMarketMaker(marketOrder.getMarketMarketMaker());
		MarketMarketMaker mmm = marketOrder.getMarketMarketMaker();
		proposal.setInternal(false);
		proposal.setOrderPrice(marketOrder.getLimit());
		proposal.setPriceType(PriceType.PRICE);
		proposal.setProposalState(ProposalState.NEW);
		proposal.setQty(marketOrder.getQty());
		Proposal.ProposalSide side = marketOrder.getSide() == Rfq.OrderSide.BUY ? Proposal.ProposalSide.ASK : Proposal.ProposalSide.BID;
		proposal.setSide(side);
		proposal.setTimestamp(new Date());

		try {
			tsoxConnectionListener.onCounter(marketOrder.getMarketSessionId(), marketOrder.getMarketSessionId(), "quote:0", "quote:0", mmm.getMarketSpecificCode(),
						marketOrder.getLimit().getAmount(), marketOrder.getQty(), marketOrder.getCurrency(), side, ProposalType.COUNTER, marketOrder.getFutSettDate(), 0, "onBehalfOf");
		} catch (OperationNotExistingException e) {
			LOGGER.info("Exception", e);
		} catch (BestXException e) {
			LOGGER.info("Exception", e);
			}

	}
	
	private void sendNewExecutionReport(MarketOrder marketOrder) {
		String sessionId = "sessionID";
		String clOrdId = marketOrder.getMarketSessionId();
		ExecType execType = ExecType.New;
		OrdStatus ordStatus = OrdStatus.New;
		BigDecimal accruedInterestAmount = null;
		BigDecimal accruedInterestRate = null;
		BigDecimal lastPrice = marketOrder.getLimit().getAmount();
		String contractNo = "C#" + System.currentTimeMillis();
		Date futSettDate = DateUtils.addDays(new Date(), 3);
		Date transactTime = new Date();
		String text = "simulated execution";
		
		tsoxConnectionListener.onExecutionReport(sessionId, clOrdId, execType, ordStatus, accruedInterestAmount, accruedInterestRate, lastPrice, contractNo, futSettDate, transactTime, text);
	}

	public static void setCancelIsins(List<String> cancelIsins) {
		BogusTSOXConnection.cancelIsins = cancelIsins;
	}

	public static void setRejectIsins(List<String> rejectIsins) {
		BogusTSOXConnection.rejectIsins = rejectIsins;
	}

	private void sendFilledExecutionReport(MarketOrder marketOrder) {
		String sessionId = "sessionID";
		String clOrdId = marketOrder.getMarketSessionId();
		ExecType execType = ExecType.Trade;
		OrdStatus ordStatus = OrdStatus.Filled;
		BigDecimal accruedInterestAmount = null;
		BigDecimal accruedInterestRate = null;
		BigDecimal lastPrice = marketOrder.getLimit().getAmount();
		String contractNo = "C#" + System.currentTimeMillis();
		Date futSettDate = DateUtils.addDays(new Date(), 3);
		Date transactTime = new Date();
		String text = null;
		
		tsoxConnectionListener.onExecutionReport(sessionId, clOrdId, execType, ordStatus, accruedInterestAmount, accruedInterestRate, lastPrice, contractNo, futSettDate, transactTime, text);
		
	}

	private void sendCancelledExecutionReport(MarketOrder marketOrder) {
		String sessionId = "sessionID";
		String clOrdId = marketOrder.getMarketSessionId();
		ExecType execType = ExecType.Canceled;
		OrdStatus ordStatus = OrdStatus.Canceled;
		BigDecimal accruedInterestAmount = null;
		BigDecimal accruedInterestRate = null;
		BigDecimal lastPrice = marketOrder.getLimit().getAmount();
		String contractNo = "C#" + System.currentTimeMillis();
		Date futSettDate = DateUtils.addDays(new Date(), 3);
		Date transactTime = new Date();
		String text = "Don't like your ugly face";
		
		tsoxConnectionListener.onExecutionReport(sessionId, clOrdId, execType, ordStatus, accruedInterestAmount, accruedInterestRate, lastPrice, contractNo, futSettDate, transactTime, text);
		
	}

	private void sendOrderReject(MarketOrder marketOrder) {
		String sessionId = "sessionID";
		String clOrdId = marketOrder.getMarketSessionId();
		String reason = "Don't like your manners";
		
		tsoxConnectionListener.onOrderReject(sessionId, clOrdId, reason);
		
	}

	@Override
	public void sendSubjectOrder(MarketOrder marketOrder) throws BestXException {
		;
	}

	@Override
	public void ackProposal(Proposal proposal) throws BestXException {
		;
	}

	@Override
	public void acceptProposal(Operation operation, Instrument instrument, Proposal proposal) throws BestXException {
		sendFilledExecutionReport(operation.getLastAttempt().getMarketOrder());
	}

	@Override
	public void rejectProposal(Operation operation, Instrument instrument, Proposal proposal) throws BestXException {
		;
	}

}
