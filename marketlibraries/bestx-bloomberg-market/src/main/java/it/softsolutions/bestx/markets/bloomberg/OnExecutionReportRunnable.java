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
package it.softsolutions.bestx.markets.bloomberg;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ExecutablePrice;
import it.softsolutions.bestx.model.ExecutablePriceAskComparator;
import it.softsolutions.bestx.model.ExecutablePriceBidComparator;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Rfq;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.tradestac.fix.field.ExecType;
import it.softsolutions.tradestac.fix.field.OrdStatus;
import it.softsolutions.tradestac.fix.field.PartyIDSource;
import it.softsolutions.tradestac.fix.field.PartyRole;
import it.softsolutions.tradestac.fix.field.PriceType;
import it.softsolutions.tradestac.fix50.TSExecutionReport;
import it.softsolutions.tradestac.fix50.TSNoPartyID;
import it.softsolutions.tradestac.fix50.component.TSCompDealersGrpComponent;
import it.softsolutions.tradestac.fix50.component.TSParties;
import quickfix.DoubleField;
import quickfix.FieldNotFound;
import quickfix.Group;
import quickfix.MessageComponent;
import quickfix.StringField;
import quickfix.field.CompDealerID;
import quickfix.field.CompDealerQuote;

/**
 * 
 * Purpose: this class is created to decouple the order management from the channel management. The channel manager creates the TSExecutionReport and starts this class in a new thread
 * 
 * Project Name : bestx-bloomberg-market First created by: davide.rossoni Creation date: 12/lug/2013
 * 
 **/
@SuppressWarnings("deprecation")
public class OnExecutionReportRunnable implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(OnExecutionReportRunnable.class);

	private final Operation operation;
	private final Market counterMarket;
	private final Market executionMarket;
	private final BigDecimal lastPrice;
	private final ExecType execType;
	private final OrdStatus ordStatus;
	private final String clOrdID;
	private final String contractNo;
	private final BloombergMarket market;
	private final Date futSettDate;
	private final Date transactTime;
	private final String text;
	private final BigDecimal accruedInterestAmount;
	//BESTX-348: SP-20180905 added numDaysInterest field
	private final Integer numDaysInterest;
	//BESTX-385: SP-20190116 manage factor (228) field
	private final BigDecimal factor;
	private final String lastMkt; //AKA MifidMIC
	private final TSExecutionReport tsExecutionReport;
	private final MarketMakerFinder marketMakerFinder;
	private final String settlType;

	public OnExecutionReportRunnable(Operation operation, BloombergMarket market, Market counterMarket, Market executionMarket, TSExecutionReport tsExecutionReport, MarketMakerFinder marketMakerFinder) {
		this.tsExecutionReport = tsExecutionReport;
		this.operation = operation;
		this.counterMarket = counterMarket;
		this.executionMarket = executionMarket;
		this.clOrdID = tsExecutionReport.getClOrdID();
		this.market = market;
		this.execType = tsExecutionReport.getExecType();
		this.ordStatus= tsExecutionReport.getOrdStatus();
		this.accruedInterestAmount = tsExecutionReport.getAccruedInterestAmt() == null ? null : new BigDecimal(tsExecutionReport.getAccruedInterestAmt().toString());
		if(tsExecutionReport.getPriceType() == PriceType.Percentage) {
			this.lastPrice = tsExecutionReport.getLastPx() == null ? null : new BigDecimal(tsExecutionReport.getLastPx().toString());
		} else {
			this.lastPrice = tsExecutionReport.getLastParPrice() != null ? BigDecimal.valueOf(tsExecutionReport.getLastParPrice()) : BigDecimal.ZERO;
			//this.lastPrice = tsExecutionReport.getLastParPrice() == null ? null : new BigDecimal(tsExecutionReport.getLastParPrice().toString());
		}
		this.contractNo = tsExecutionReport.getExecID();
		this.futSettDate = tsExecutionReport.getSettlDate();
		this.transactTime = tsExecutionReport.getTransactTime();
		this.text = tsExecutionReport.getText();
		//BESTX-348: SP-20180905 added numDaysInterest field
		this.numDaysInterest = tsExecutionReport.getNumDaysInterest();
		//BESTX-385: SP-20190116 manage factor (228) field
		this.factor = tsExecutionReport.getFactor() != null ? BigDecimal.valueOf(tsExecutionReport.getFactor()) : BigDecimal.ZERO;  
		this.lastMkt = tsExecutionReport.getCustomFieldString(23068); //MifidMIC
		this.marketMakerFinder = marketMakerFinder;
		this.settlType = (tsExecutionReport.getSettlType() != null ? tsExecutionReport.getSettlType().getFIXValue() : null);
	}

	//FIXME verify the values reported here are compliant
	private static String calculateStatus(ExecutablePrice price, OrdStatus ordStatus, String dealerCode, BigDecimal execPrice) {
		if(ordStatus == OrdStatus.Filled || ordStatus == OrdStatus.PartiallyFilled) { // dealer code is there and execPrice is there
			if(execPrice.compareTo(price.getPrice().getAmount()) == 0 && dealerCode.compareTo(price.getOriginatorID()) == 0) 
				return "Accepted";
			if(execPrice.compareTo(price.getPrice().getAmount()) == 0)
				return "Tied for Best";
			if(BigDecimal.ZERO.compareTo(price.getPrice().getAmount()) < 0)
				return "Covered";
		} else {
			return "Missed";
		}
		return "Passed";
	}


	public void run() {
		String dealerCode= null;

		TSParties parties = tsExecutionReport.getTSParties();
		if(parties != null && parties.getTSNoPartyIDsList() != null) {
			List<TSNoPartyID> list = parties.getTSNoPartyIDsList();
			for(TSNoPartyID party: list) {
				if(party.getPartyRole() == PartyRole.ExecutingFirm && party.getPartyIDSource() != PartyIDSource.LegalEntityIdentifier) {
					dealerCode = party.getPartyID();
					break;
				}
			}
		}

		Order order = this.operation.getLastAttempt().getMarketOrder();
		Rfq rfq = this.operation.getRfq();
		Attempt attempt = operation.getLastAttempt();
		if(order == null) {
			operation.onApplicationError(operation.getState(), new NullPointerException(), "Operation " + operation.getId() + " has no order!");
			return;
		}
		if (this.lastPrice == null || this.lastPrice.equals(new BigDecimal("0"))) {
			operation.onApplicationError(operation.getState(), null, Messages.getString("NoExecutionPriceError.0"));
			return;
		}

		MarketExecutionReport marketExecutionReport = new MarketExecutionReport();
		marketExecutionReport.setActualQty(order.getQty());
		marketExecutionReport.setInstrument(order.getInstrument());
		marketExecutionReport.setMarket(counterMarket);
		marketExecutionReport.setOrderQty(order.getQty());
		marketExecutionReport.setPrice(order.getLimit());
		marketExecutionReport.setLastPx(lastPrice);
		marketExecutionReport.setSide(order.getSide());

		marketExecutionReport.setExecType(execType.getFIXValue());
		marketExecutionReport.setOrdStatus(ordStatus.getFIXValue());
		// FIXME 20190506 AMC verify that this management is appropriate. Better a switch on ordStatus?
		ExecutionReportState executionReportState;
		if ((execType == ExecType.New)) {
			executionReportState = ExecutionReportState.NEW;
		} else if ((execType == ExecType.Canceled) && ordStatus == OrdStatus.Canceled) {
			executionReportState = ExecutionReportState.CANCELLED;
		} else if ((execType == ExecType.Trade) && (ordStatus == OrdStatus.Filled)) {
			executionReportState = ExecutionReportState.FILLED;
		} else {
			executionReportState = ExecutionReportState.REJECTED;
		}
		LOGGER.info("orderID={}, mapped execType {} - ordStatus {} to executionReportState {}", order.getFixOrderId(), execType, ordStatus, executionReportState);
		marketExecutionReport.setState(executionReportState);
		marketExecutionReport.setTransactTime(this.transactTime);
		marketExecutionReport.setSequenceId(null); // ID only
		// needed for sending to customer
		marketExecutionReport.setMarketOrderID(clOrdID); // <-- market order id is buy side's clOrdID
		marketExecutionReport.setTicket(contractNo);
		marketExecutionReport.setFutSettDate(futSettDate);
		marketExecutionReport.setSettlType(settlType);
		marketExecutionReport.setText(text);
		marketExecutionReport.setAccruedInterestAmount(new Money(order.getInstrument().getCurrency(), 
				accruedInterestAmount == null ? BigDecimal.ZERO : accruedInterestAmount));
		marketExecutionReport.setLastMkt(lastMkt);
		if(dealerCode != null) {
			MarketMarketMaker mmm = null;
			try {
				mmm = marketMakerFinder.getMarketMarketMakerByTSOXCode(dealerCode);
			} catch (BestXException e) {
				LOGGER.error("Exception occurred", e);
			}
			if(mmm == null) {
				LOGGER.debug("Market maker not defined in configuration");
				marketExecutionReport.setExecBroker(dealerCode);
			}
			else {
				marketExecutionReport.setExecBroker(mmm.getMarketMaker().getCode());
			}
		}
		
		if (numDaysInterest != null) {
			marketExecutionReport.setAccruedInterestDays(numDaysInterest);
		}
		if (factor != null) {
			marketExecutionReport.setFactor(factor); // For inflation linked products only
		}

		// get competing dealer quotes feedback. Does not contain the executed proposal, so we shall add it
		List<MessageComponent> customComp = tsExecutionReport.getCustomComponents();
		if (customComp != null) {
			for (MessageComponent comp : customComp) {
				List<ExecutablePrice> prices = new ArrayList<ExecutablePrice>();
				// add to prices the executed quote
				ExecutablePrice executedPrice = new ExecutablePrice();
				executedPrice.setMarket(this.executionMarket);
				executedPrice.setOriginatorID(dealerCode);
				try {
//					executedPrice.setMarketMarketMaker(marketMakerFinder.getMarketMarketMakerByCode(market.getMarketCode(), dealerCode));
					executedPrice.setMarketMarketMaker(marketMakerFinder.getMarketMarketMakerByTSOXCode(dealerCode));
				} catch (BestXException e1) {
					executedPrice.setMarketMarketMaker(null);
				}
				executedPrice.setPrice(new Money(operation.getOrder().getCurrency(), marketExecutionReport.getLastPx()));
				executedPrice.setPriceType(Proposal.PriceType.PRICE);
				executedPrice.setQty(operation.getOrder().getQty());
				// calculate status
				executedPrice.setTimestamp(tsExecutionReport.getTransactTime());
				executedPrice.setType(ProposalType.COUNTER);
				executedPrice.setSide(operation.getOrder().getSide() == OrderSide.BUY ? ProposalSide.ASK : ProposalSide.BID);
				executedPrice.setQuoteReqId(attempt.getMarketOrder().getFixOrderId());
				executedPrice.setAuditQuoteState("Accepted");
				
				prices.add(0, executedPrice);
	
				if (comp instanceof TSCompDealersGrpComponent) {
					try {
						quickfix.field.NoCompDealers compDealerGrp = ((TSCompDealersGrpComponent) comp).get(new quickfix.field.NoCompDealers());
						List<Group> groups = ((TSCompDealersGrpComponent) comp).getGroups(compDealerGrp.getField());

						for (int i = 0; i < groups.size(); i++) {
							ExecutablePrice price = new ExecutablePrice();
							price.setMarket(this.executionMarket);
							MarketMarketMaker tempMM = null;
							if (groups.get(i).isSetField(CompDealerID.FIELD)) {
								String quotingDealer = groups.get(i).getField(new StringField(CompDealerID.FIELD)).getValue();

//								tempMM = marketMakerFinder.getMarketMarketMakerByCode(market.getMarketCode(), quotingDealer);
								tempMM = marketMakerFinder.getMarketMarketMakerByTSOXCode(quotingDealer);
								if(tempMM == null) {
									LOGGER.info("IMPORTANT! Bloomberg returned dealer {} not configured in BestX!. Please configure it", quotingDealer);
									price.setOriginatorID(quotingDealer);
								} else {
									price.setOriginatorID(quotingDealer);
									price.setMarketMarketMaker(tempMM);
								}
							}
							if (groups.get(i).isSetField(CompDealerQuote.FIELD)) {
								Double compDealerQuote = groups.get(i).getField(new DoubleField(CompDealerQuote.FIELD)).getValue();
								price.setPrice(new Money(operation.getOrder().getCurrency(), Double.toString(compDealerQuote)));
							} else
								price.setPrice(new Money(operation.getOrder().getCurrency(), "0.0"));
							price.setPriceType(Proposal.PriceType.PRICE);
							price.setQty(operation.getOrder().getQty());
							// calculate status
							price.setTimestamp(tsExecutionReport.getTransactTime());
							price.setType(ProposalType.COUNTER);
							price.setSide(operation.getOrder().getSide() == OrderSide.BUY ? ProposalSide.ASK : ProposalSide.BID);
							price.setQuoteReqId(attempt.getMarketOrder().getFixOrderId());
							price.setAuditQuoteState(calculateStatus(price, ordStatus, dealerCode, lastPrice));
							if(tempMM == null) {
								LOGGER.info("Added Executable price for order {}, attempt {}, marketmaker {}, price {}, status {}", 
										operation.getOrder().getFixOrderId(), operation.getAttemptNo(), price.getOriginatorID(), price.getPrice().getAmount().toString(), price.getAuditQuoteState());
							} else {
								LOGGER.info("Added Executable price for order {}, attempt {}, marketmaker {}, price {}, status {}",
										operation.getOrder().getFixOrderId(), operation.getAttemptNo(), price.getMarketMarketMaker().getMarketMaker().getName(), price.getPrice().getAmount().toString(), price.getAuditQuoteState());
							}
							prices.add(price);
						}
					}
					catch (FieldNotFound | BestXException e) {
						LOGGER.warn("[MktMsg] Field not found in component dealers", e);
					}
				}
				// sort the executable prices
				Comparator<ExecutablePrice> comparator;
				comparator = operation.getOrder().getSide() == OrderSide.BUY ? new ExecutablePriceAskComparator() : new ExecutablePriceBidComparator();
		        Collections.sort(prices, comparator);
		        // give them their rank
				for(int i = 0; i < prices.size(); i++)
					prices.get(i).setRank(i + 1);
				// add the sorted, ranked executable prices list to the attempt
				attempt.setExecutablePrices(prices);
			}
		} else {
			LOGGER.info("[MktMsg] No custom component found in execution report {}", tsExecutionReport.getClOrdID());
		}

		operation.onMarketExecutionReport(market, order, marketExecutionReport);
	}
}
