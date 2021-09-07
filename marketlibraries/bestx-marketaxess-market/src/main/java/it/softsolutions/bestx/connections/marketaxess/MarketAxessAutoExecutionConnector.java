
/*
 * Copyright 1997-2017 SoftSolutions! srl 
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

package it.softsolutions.bestx.connections.marketaxess;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.tradestac2.TradeStacTradeConnection;
import it.softsolutions.bestx.connections.tradestac2.TradeStacTradeConnectionListener;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.markets.marketaxess.MarketAxessExecutionReport;
import it.softsolutions.bestx.markets.marketaxess.MarketAxessMarket;
import it.softsolutions.bestx.markets.marketaxess.MarketAxessOrder;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.MarketMarketMakerSpec;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.ClOrdID;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.Currency;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.DealerID;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.DealerIDSource;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.HandlInst;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.IncludeDealers;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.MKTXESCBStblty;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.MKTXTargetLevel;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.NoPartyIDs;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.Notes;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.OrdStatus;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.OrdType;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.OrderQty;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.OrigClOrdID;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.PartyIDSource;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.PartyRole;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.PartyRoleQualifier;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.Price;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.PriceType;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.QtyType;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.RefMsgType;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.RefSeqNum;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.SecurityID;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.SecurityIDSource;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.SettlDate;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.SettlType;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.Side;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.Symbol;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.Text;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.TransactTime;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.ExecutionReport;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.NewOrderSingle;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.OrderCancelRequest;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.component.Parties;
import it.softsolutions.tradestac2.api.TradeStacApplicationCallback;
import it.softsolutions.tradestac2.api.TradeStacException;
import it.softsolutions.tradestac2.client.TradeStacSessionCallback;
import quickfix.BooleanField;
import quickfix.ConfigError;
import quickfix.FieldNotFound;
import quickfix.Group;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.MarketSegmentID;
import quickfix.field.OrderCapacity;
import quickfix.field.PartyID;

/**
 *
 * Purpose: this class is  for connection to execution channel of MarketAxess
 *
 * Project Name : bestx-marketaxess-market
 * First created by: anna.cochetti
 * Creation date: 11 gen 2017
 * 
 **/

public class MarketAxessAutoExecutionConnector extends Tradestac2MarketAxessConnection implements TradeStacTradeConnection, TradeStacApplicationCallback, TradeStacSessionCallback, MarketAxessConnectorMBean
{
   static final Logger LOGGER = LoggerFactory.getLogger(MarketAxessAutoExecutionConnector.class);
   private Character investmentDecisionIDSource = null;
   private String investmentDecisorID = null;
   private String executionDecisionID = null;
   private Character defaultTradingCapacity = null;
   private Character defaultShortSelling = '0';
   private Integer investmentDecisionQualifier = null;
   private boolean addBlockedDealers = false;
   private boolean addIncludeDealers = false;
   private int blockedDealersMaxNum = 1000;
   private int includeDealersMaxNum = 1000;
   private String marketSegmentID = null;
   private int minIncludeDealers = 1;
   private String traderPartyID;
	
   TradeStacTradeConnectionListener tradeStacTradeConnectionListener;  // the market bean

	public MarketAxessAutoExecutionConnector (String connectionName) {
		super(connectionName);
	}
	
	public MarketAxessAutoExecutionConnector () {
		super("MARKET_AXESS_BUY_SIDE#marketAxessFIX44");
	}
	
	@Override
	public void init() throws BestXException, ConfigError, TradeStacException {
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
		this.marketAxessHelper = new MarketAxessHelper(instrumentFinder, marketMakerFinder, venueFinder, marketFinder);
	}



	@Override
	public void onExecutionReport(SessionID sessionID, Message message) throws TradeStacException {
		try {
			ExecutionReport tsExecutionReport = (ExecutionReport) message;
			LOGGER.debug("Received ExecutionReport {}", message);
			// get clordId
			String clordId = tsExecutionReport.getString(it.softsolutions.marketlibraries.quickfixjrootobjects.fields.ClOrdID.FIELD);
			//getInstrument
			Instrument instrument = this.marketAxessHelper.getInstrument(tsExecutionReport);
			// create execu report
			MarketAxessExecutionReport executionReport = MarketAxessHelper.createExecutionReport(instrument, tsExecutionReport);
			// send to Market
			executionReport.setMarket(this.market);
			executionReport.setMarketOrderID(clordId);
			copyTextInExecutionReport(tsExecutionReport, executionReport);
			tradeStacTradeConnectionListener.onExecutionReport(sessionID.toString(), clordId, executionReport);
		} catch(Exception e) {
			throw new TradeStacException(e);
		}
	}

	/**
	 * @param tsExecutionReport
	 * @param executionReport
	 */
	private void copyTextInExecutionReport(ExecutionReport tsExecutionReport,
			MarketAxessExecutionReport executionReport) {
		try {
			executionReport.setText(tsExecutionReport.getString(it.softsolutions.marketlibraries.quickfixjrootobjects.fields.Text.FIELD));
		} catch (@SuppressWarnings("unused") Exception e) {
			e.getMessage();
		}
	}
	
	@Override
	public void onOrderCancelReject(SessionID sessionID, Message tsOrderCancelReject) throws TradeStacException {
		String clordid = "";
		String text = "";
		String ordStatus = "";
		try {
			clordid = tsOrderCancelReject.getString(it.softsolutions.marketlibraries.quickfixjrootobjects.fields.OrigClOrdID.FIELD);
			text = tsOrderCancelReject.getString(it.softsolutions.marketlibraries.quickfixjrootobjects.fields.Text.FIELD);
			ordStatus = tsOrderCancelReject.getString(it.softsolutions.marketlibraries.quickfixjrootobjects.fields.OrdStatus.FIELD);
		} catch(FieldNotFound e) {
			throw new TradeStacException(e);
		}
        this.tradeStacTradeConnectionListener.onOrderCancelReject(sessionID.toString(), clordid, ordStatus, text);
	}
	
	@Override
	public void onBusinessMessageReject(SessionID sessionID, Message tsBusinessMessageReject, Message relatedMessage)
			throws TradeStacException {
		LOGGER.error("Session {} - Got Business Message reject {}", sessionID, tsBusinessMessageReject);
		String reason = "";
		String requestID = "";
		try {
			if(!"D".equalsIgnoreCase(tsBusinessMessageReject.getString(it.softsolutions.marketlibraries.quickfixjrootobjects.fields.RefMsgType.FIELD)))
				super.onBusinessMessageReject(sessionID, tsBusinessMessageReject, relatedMessage);
			reason = tsBusinessMessageReject.getString(it.softsolutions.marketlibraries.quickfixjrootobjects.fields.Text.FIELD);
			requestID = tsBusinessMessageReject.getString(RefSeqNum.FIELD);
		} catch (FieldNotFound e) {
			throw new TradeStacException(e);
		}
		
		tradeStacTradeConnectionListener.onOrderReject(sessionID.toString(), requestID, reason);
	}



	@Override
	public void sendOrder(MarketOrder marketOrder) throws BestXException {
		String clOrdID = marketOrder.getMarketSessionId();
		Instrument instrument = marketOrder.getInstrument();
		String securityID = instrument.getIsin();
		Side side = new Side(marketOrder.getSide().getFixCode().charAt(0));
		Double orderQty = marketOrder.getQty().doubleValue();
		Date settlDate = marketOrder.getFutSettDate();
		Double price = marketOrder.getLimit().getAmount().doubleValue();
        String settlementType = marketOrder.getSettlementType();
		

		NewOrderSingle newOrderSingle = new NewOrderSingle();
		newOrderSingle.set(new ClOrdID(clOrdID));
		if (settlDate != null) {
	      newOrderSingle.set(new SettlDate(DateService.formatAsUTC(DateService.dateISO, settlDate)));
		}
		newOrderSingle.set(new SettlType(settlementType));
		newOrderSingle.set(new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE)); 

		newOrderSingle.set(new Symbol("N/A"));
		newOrderSingle.set(new SecurityID(securityID));
		newOrderSingle.set(new SecurityIDSource(SecurityIDSource.ISIN_NUMBER));

		newOrderSingle.set(side);
		newOrderSingle.set(new TransactTime(DateService.newUTCDate()));

		newOrderSingle.set(new OrderQty(orderQty));
		newOrderSingle.set(new QtyType(QtyType.UNITS));

		newOrderSingle.set(new Price(price));
		newOrderSingle.set(new PriceType(PriceType.PERCENTAGE));
		newOrderSingle.setField(new MKTXTargetLevel(price.toString()));
		newOrderSingle.set(new OrdType(OrdType.LIMIT));
		
		// TDR: BESTX-394
		if(marketSegmentID == null || marketSegmentID.isEmpty()) {
			LOGGER.warn("MarketSegmentID is missed!");
		} else {
			newOrderSingle.setField(new MarketSegmentID(marketSegmentID));
		}
		
		newOrderSingle.set(new Currency(marketOrder.getCurrency()));
		newOrderSingle.setField(new Notes(clOrdID));
		
		//MIFID II
		//TradingCapacity
		Group originator = createGroupForTrader();

		Character tradingCapacity = MarketAxessHelper.convertOrderCapacity(marketOrder);
		if(tradingCapacity == null)
			tradingCapacity = getDefaultTradingCapacity();
		OrderCapacity oc = new OrderCapacity(tradingCapacity);

		newOrderSingle.setField(oc);

		//ExecutionDecisionMakerID PartyRole(452) = 12, PartyID(448)=<ID BestX>, PartyIDSource(447)=D (Short code) PartyRoleQualifier(2376)=22
		Group executionDecisionMakerGroup = createGroupForParty(getExecutionDecisionID(), PartyIDSource.PROPRIETARY_CUSTOM_CODE, PartyRole.EXECUTING_TRADER, PartyRoleQualifier.ALGORITHM);

		//InvestmentDecisionMakerID PartyRole(452) = 122, PartyID(448)=Token, PartyIDSource(447)=P (Short code)
		Group investmentDecisionMakerGroup = createGroupForParty(getInvestmentDecisorID(), getInvestmentDecisionIDSource(), PartyRole.INVESTMENT_DECISION_MAKER, getInvestmentDecisionQualifier());

		Parties parties = new Parties();
		parties.addGroup(originator);
		parties.addGroup(executionDecisionMakerGroup);
		parties.addGroup(investmentDecisionMakerGroup);
		newOrderSingle.set(parties);

		//ShortSellingIndicator (23066)
		char sideValue = MarketAxessHelper.convertShortSellIndicatorToSideValue(marketOrder, getDefaultShortSelling());
		// put it in the order only if it is > 0 (i.e. has to be attached to the order)
		if(sideValue > 0) {
			newOrderSingle.set(new Side(Character.toChars(sideValue)[0]));
		}
      
		//ESCB Stability Flag
		newOrderSingle.setField(new MKTXESCBStblty(MKTXESCBStblty.INVESTMENT_OPERATIONS));
				
		if(this.minIncludeDealers == 1 || this.minIncludeDealers == 2) {
	      newOrderSingle.setField(new IncludeDealers(this.minIncludeDealers));
	
	   }
		// BESTX-892 management of include/exclude dealers. Using configurable attributes
		// add dealers in the include list
		if(isAddIncludeDealers())
			addIncludeDealers(marketOrder, newOrderSingle);

		// add dealers that must be excluded
		if(isAddBlockedDealers())
			addBlockedDealers(marketOrder, newOrderSingle);
		try {
			tradeStacClientSession.manageNewOrderSingle(newOrderSingle);
		} catch (TradeStacException e) {
			LOGGER.warn("Exception got when sending order {} to market {}, client orderId: {}", newOrderSingle, "marketAxess", marketOrder.getCustomerOrderId(), e);
		}
	}

	/**
	 * @param marketOrder
	 * @param newOrderSingle
	 */
	private void addBlockedDealers(MarketOrder marketOrder, NewOrderSingle newOrderSingle) {
		if(marketOrder.getExcludeDealers() != null) {		
			for(int i = 0 ; i < blockedDealersMaxNum && i < marketOrder.getExcludeDealers().size(); i++) {
				MarketMarketMakerSpec maDealerCode= marketOrder.getExcludeDealers().get(i);
				NewOrderSingle.NoDealers dealer = new NewOrderSingle.NoDealers();
				if(maDealerCode != null) {
					dealer.set(new DealerID(maDealerCode.marketMakerCode));
					dealer.set(new DealerIDSource(maDealerCode.marketMakerCodeSource));
					dealer.setField(new BooleanField(7762 /*Exclude */, true));
					newOrderSingle.addGroup(dealer);
				}
			}
		}
	}

	/**
	 * @param marketOrder
	 * @param newOrderSingle
	 */
	private void addIncludeDealers(MarketOrder marketOrder, NewOrderSingle newOrderSingle) {
		if(marketOrder.getDealers() != null)
	    	  for(int i = 0 ; i < includeDealersMaxNum && i < marketOrder.getDealers().size(); i++) {
	    		  MarketMarketMakerSpec maDealerCode = marketOrder.getDealers().get(i);
	    		  NewOrderSingle.NoDealers dealer = new NewOrderSingle.NoDealers();
	    		  if(maDealerCode != null) {
	    			  dealer.set(new DealerID(maDealerCode.marketMakerCode));
	    			  dealer.set(new DealerIDSource(maDealerCode.marketMakerCodeSource));
	    			  newOrderSingle.addGroup(dealer);
	    		  }
	      }
	}

	private Group createGroupForTrader() {
	   return createGroupForParty(this.traderPartyID, 'C', PartyRole.ORDER_ORIGINATION_TRADER, null);
	}
	
	private Group createGroupForParty(String partyId, char partyIdSource, int partyRole, Integer partyRoleQualifier) {
      Group group = new Group(it.softsolutions.marketlibraries.quickfixjrootobjects.fields.NoPartyIDs.FIELD,
    		  		it.softsolutions.marketlibraries.quickfixjrootobjects.fields.PartyID.FIELD);
      group.setField(new PartyID(partyId));
      group.setField(new PartyIDSource(partyIdSource));  // partyIDSource required by MA 
      group.setField(new PartyRole(partyRole));
      if (partyRoleQualifier != null) {
         group.setField(new PartyRoleQualifier(partyRoleQualifier));
      }
      return group;
   }

	@Override
	public void cancelOrder(MarketOrder marketOrder) throws BestXException {
		String clOrdID = marketOrder.getMarketSessionId();
		Instrument instrument = marketOrder.getInstrument();
		String securityID = instrument.getIsin();
		Side side = new Side(marketOrder.getSide().getFixCode().charAt(0));
		Double orderQty = marketOrder.getQty().doubleValue();

		OrderCancelRequest orderCancelRequest = new OrderCancelRequest();
		orderCancelRequest.set(new OrigClOrdID(clOrdID));
		orderCancelRequest.set(new ClOrdID(clOrdID));

		orderCancelRequest.set(new Symbol("N/A"));
		orderCancelRequest.set(new SecurityID(securityID));
		orderCancelRequest.set(new SecurityIDSource(SecurityIDSource.ISIN_NUMBER));
		orderCancelRequest.set(side);
		orderCancelRequest.set(new TransactTime(DateService.newUTCDate()));
		orderCancelRequest.set(new OrderQty(orderQty));

		Group originator = createGroupForTrader();
		orderCancelRequest.addGroup(originator);

		try {
			tradeStacClientSession.manageOrderCancelRequest(orderCancelRequest);
		} catch (TradeStacException e) {
			LOGGER.warn("Exception got when sending cancel order request {} to market {}, client orderId: {}", orderCancelRequest, "marketAxess", marketOrder.getCustomerOrderId(), e);
		}

	}

	public TradeStacTradeConnectionListener getTradeStacTradeConnectionListener() {
		return tradeStacTradeConnectionListener;
	}

	@Override
	public void setTradeStacTradeConnectionListener(TradeStacTradeConnectionListener tradeStacTradeConnectionListener) {
		this.tradeStacTradeConnectionListener = tradeStacTradeConnectionListener;
	}

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.connections.marketaxess.MarketAxessMBean#getTraderPartyID()
	 */
	@Override
	public String getTraderPartyID() {
		return traderPartyID;
	}

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.connections.marketaxess.MarketAxessMBean#setTraderPartyID(java.lang.String)
	 */
	@Override
	public void setTraderPartyID(String traderPartyID) {
		this.traderPartyID = traderPartyID;
	}

   
   public Character getInvestmentDecisionIDSource() {
      return investmentDecisionIDSource;
   }

   
   public void setInvestmentDecisionIDSource(Character investmentDecisionIDSource) {
      this.investmentDecisionIDSource = investmentDecisionIDSource;
   }

   public Integer getInvestmentDecisionQualifier() {
	      return investmentDecisionQualifier;
   }

	   
   public void setInvestmentDecisionQualifier(Integer investmentDecisionQualifier) {
	      this.investmentDecisionQualifier = investmentDecisionQualifier;
   }

   
   public String getInvestmentDecisorID() {
      return investmentDecisorID;
   }

   
   public void setInvestmentDecisorID(String investmentDecisorID) {
      this.investmentDecisorID = investmentDecisorID;
   }

   
   public String getExecutionDecisionID() {
      return executionDecisionID;
   }

   
   public void setExecutionDecisionID(String executionDecisionID) {
      this.executionDecisionID = executionDecisionID;
   }

   
   public Character getDefaultTradingCapacity() {
      return defaultTradingCapacity;
   }

   
   public void setDefaultTradingCapacity(Character defaultTradingCapacity) {
      this.defaultTradingCapacity = defaultTradingCapacity;
   }

   
   public char getDefaultShortSelling() {
      return defaultShortSelling;
   }
   
   public void setDefaultShortSelling(char defaultShortSelling) {
	   this.defaultShortSelling = defaultShortSelling;
   }

   public String getMarketSegmentID() {
	   return marketSegmentID;
   }

   public void setMarketSegmentID(String marketSegmentID) {
	   this.marketSegmentID = marketSegmentID;
   }
   public boolean isAddIncludeDealers() {
	return addIncludeDealers;
   }

   public void setAddIncludeDealers(boolean addIncludeDealers) {
	   this.addIncludeDealers = addIncludeDealers;
   }

   public int getBlockedDealersMaxNum() {
	   return blockedDealersMaxNum;
   }

   public void setBlockedDealersMaxNum(int blockedDealersMaxNum) {
	   this.blockedDealersMaxNum = blockedDealersMaxNum;
   }

   public int getIncludeDealersMaxNum() {
	   return includeDealersMaxNum;
   }

   public void setIncludeDealersMaxNum(int includeDealersMaxNum) {
	   this.includeDealersMaxNum = includeDealersMaxNum;
   }
   public boolean isAddBlockedDealers() {
	   return addBlockedDealers;
   }

   public void setAddBlockedDealers(boolean addBlockedDealers) {
	   this.addBlockedDealers = addBlockedDealers;
   }

   public int getmMinIncludeDealers() {
	   return minIncludeDealers;
   }

   public void setMinIncludeDealers(int minIncludeDealers) {
	   this.minIncludeDealers = minIncludeDealers;
   }
}
