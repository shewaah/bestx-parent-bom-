
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.connections.MarketPriceDiscoveryHelper;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.markets.marketaxess.MarketAxessExecutionReport;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Market.SubMarketCode;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketMarketMakerSpec;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.AccruedInterestAmt;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.AccruedInterestRate;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.AggregatedBook;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.AvgPx;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.CompetitiveStatus;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.CumQty;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.Currency;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.DealerID;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.DealerIDSource;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.DealerQuoteOrdQty;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.DealerQuotePrice;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.DealerQuotePriceType;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.DealerQuoteText;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.ExecID;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.ExecRefID;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.ExecType;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.LastCapacity;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.LastMkt;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.LastParPx;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.LastPx;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.MDEntryDate;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.MDEntryOriginator;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.MDEntryPx;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.MDEntrySize;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.MDEntryTime;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.MDEntryType;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.MDReqID;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.MKTXTradeReportingInd;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.MKTXTrdRegPublicationReason;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.MarketDepth;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.NoDealers;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.NoPartyIDs;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.Notes;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.OrdStatus;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.OrderCapacity;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.OrderQty;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.PartyIDSource;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.PartyRole;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.PriceType;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.QuoteCondition;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.QuoteRank;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.ReferencePrice;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.SecurityID;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.SecurityIDSource;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.SubscriptionRequestType;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.Symbol;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.Text;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.TransactTime;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.ExecutionReport;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.MarketDataRequest;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.MarketDataSnapshotFullRefresh.NoMDEntries;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.component.Dealers;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.component.InstrmtMDReqGrp;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.component.MDReqGrp;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.component.Parties;
import it.softsolutions.marketlibraries.quickfixjrootobjects.fields.PartyID;
import it.softsolutions.marketlibraries.quickfixjrootobjects.groups.RootNoPartyIDs;
import it.softsolutions.marketlibraries.quickfixjrootobjects.groups.RootNoSecurityAltID;
import it.softsolutions.tradestac2.api.TradeStacException;
import quickfix.FieldNotFound;
import quickfix.Group;
import quickfix.field.FutSettDate;

/**
 *
 * Purpose: this class is mainly for managing MarketAxess specific aspects data
 *
 * Project Name : bestx-marketaxess-market First created by: anna.cochetti
 * Creation date: 30 gen 2017
 * 
 **/

public class MarketAxessHelper extends MarketPriceDiscoveryHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(MarketAxessHelper.class);

	static public final String MARKETAXESS_SESSION_ID = OperationIdType.MARKETAXESS_SESSION_ID.toString();
	static public final String MARKETAXESS_CLORD_ID = OperationIdType.MARKETAXESS_CLORD_ID.toString();

	static public final String SUBSCR_WORKFLOW_TYPE = "Inventory-Subscription-Open";

	public MarketAxessHelper(InstrumentFinder instrumentFinder, MarketMakerFinder marketMakerFinder,
			VenueFinder venueFinder, MarketFinder marketFinder) throws BestXException {
		super(instrumentFinder, marketMakerFinder, venueFinder, marketFinder);
		this.market = marketFinder.getMarketByCode(Market.MarketCode.MARKETAXESS, null);
	}

	/**
	 * Get BestX Model Instrument from a Market Axess
	 * marketDataSnapshotFullRefresh
	 * 
	 * @param marketDataSnapshotFullRefresh
	 *            the tradestac message
	 * @return an Instrument if in the message there is the ISIN code.
	 * @throws BestXException
	 *             when unable to get the ISIN code or there is no such
	 *             instrument in BestX Bond universe
	 */
	public Instrument getInstrument(
			it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.MarketDataSnapshotFullRefresh marketDataSnapshotFullRefresh)
			throws BestXException {
		String isin = null;
		try {
			String securitySourceId = marketDataSnapshotFullRefresh.getSecurityIDSource().getValue();
			if (it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.SecurityIDSource.ISIN_NUMBER
					.equalsIgnoreCase(securitySourceId)) {
				isin = marketDataSnapshotFullRefresh.getSecurityID().getValue();
			} else {
				List<RootNoSecurityAltID> altSecIds = marketDataSnapshotFullRefresh.getNoSecurityAltIDList();
				for (RootNoSecurityAltID altSecId : altSecIds) {
					if (it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.SecurityIDSource.ISIN_NUMBER
							.equalsIgnoreCase(altSecId.getSecurityAltIDSource().getValue())) {
						isin = altSecId.getSecurityAltID().getValue();
						break;
					}
				}
			}
		} catch (Exception e) {
			throw new BestXException(e);
		}
		return this.instrumentFinder.getInstrumentByIsin(isin);
	}

	/**
	 * Get BestX Model Instrument from a Market Axess ExecutionReport message
	 * 
	 * @param executionReport
	 *            the tradestac message
	 * @return an Instrument if in the message there is the ISIN code.
	 * @throws BestXException
	 *             when unable to get the ISIN code or there is no such
	 *             instrument in BestX Bond universe
	 */
	public Instrument getInstrument(ExecutionReport executionReport) throws BestXException {
		String isin = null;
		try {
			String securitySourceId = executionReport.getSecurityIDSource().getValue();
			if (SecurityIDSource.ISIN_NUMBER.equalsIgnoreCase(securitySourceId)) {
				isin = executionReport.getSecurityID().getValue();
			} else {
				List<RootNoSecurityAltID> altSecIds = executionReport.getNoSecurityAltIDList();
				for (RootNoSecurityAltID altSecId : altSecIds) {
					if (SecurityIDSource.ISIN_NUMBER.equalsIgnoreCase(altSecId.getSecurityAltIDSource().getValue())) {
						isin = altSecId.getSecurityAltID().getValue();
						break;
					}
				}
			}
		} catch (Exception e) {
			throw new BestXException(e);
		}
		return this.instrumentFinder.getInstrumentByIsin(isin);
	}

	/**
	 * Creates a marketDataRequest for Tradestac for a given isntrumenta and a
	 * listo of dealer market specific codes
	 * 
	 * @param instr
	 *            BestX instrument
	 * @param marketMakerCodes
	 *            List of market specific codes
	 * @return a correct MarketDataRequest
	 * @throws BestXException
	 *             if any error is preventing the creation
	 */
	public MarketDataRequest createMarketDataRequest(Instrument instr, List<MarketMarketMakerSpec> marketMakerCodes)
			throws BestXException {
		LOGGER.debug("enter createMarketDataRequest");
		MarketDataRequest marketDataRequest = new MarketDataRequest();
		String securityID = instr.getIsin();

		String reqId = MARKETAXESS_SESSION_ID + System.nanoTime();

		marketDataRequest.set(new MDReqID(reqId));
		marketDataRequest.set(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));
		marketDataRequest.set(new MarketDepth(0));
		marketDataRequest.set(new AggregatedBook(true));

		InstrmtMDReqGrp instrumentMDRequestGrp = new InstrmtMDReqGrp();
		InstrmtMDReqGrp.NoRelatedSym noRelatedSym = new InstrmtMDReqGrp.NoRelatedSym();

		it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.component.Instrument instrument = 
				new it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.component.Instrument();
		instrument.set(new Symbol("[N/A]"));
		instrument.set(new SecurityID(securityID));
		instrument.set(new SecurityIDSource(SecurityIDSource.ISIN_NUMBER));

		noRelatedSym.set(instrument);

		Dealers dealers = new Dealers();
		for (MarketMarketMakerSpec dealerCode : marketMakerCodes) {
			Dealers.NoDealers dealer = new Dealers.NoDealers();
			dealer.setField(new DealerID(dealerCode.marketMakerCode));
			String dealerCodeSource = dealerCode.marketMakerCodeSource;
			if (dealerCodeSource == null)
				dealer.setField(new DealerIDSource(DealerIDSource.BIC));
			else
				dealer.setField(new DealerIDSource(dealerCodeSource));
			dealers.addGroup(dealer);
		}
		noRelatedSym.set(dealers); // number of dealers to get prices from
		instrumentMDRequestGrp.addGroup(noRelatedSym);

		MDReqGrp mdReqGrp = new MDReqGrp();
		MDReqGrp.NoMDEntryTypes noMDEntryTypesBid = new MDReqGrp.NoMDEntryTypes();
		noMDEntryTypesBid.set(new MDEntryType(MDEntryType.BID));
		mdReqGrp.addGroup(noMDEntryTypesBid);
		MDReqGrp.NoMDEntryTypes noMDEntryTypesOffer = new MDReqGrp.NoMDEntryTypes();
		noMDEntryTypesOffer.set(new MDEntryType(MDEntryType.OFFER));
		mdReqGrp.addGroup(noMDEntryTypesOffer);
		marketDataRequest.set(mdReqGrp);

		marketDataRequest.set(instrumentMDRequestGrp);
		// Deleted after issue in prod when this field become not acceptable
		// marketDataRequest.set(new MKTXWorkflowType(SUBSCR_WORKFLOW_TYPE));

		return marketDataRequest;
	}

	/**
	 * Creates a classified proposal from a single MDEntry in a
	 * marketDataSnapshotFullRefresh
	 * 
	 * @param instrument
	 *            the reference instrument
	 * @param entry
	 *            the MDEntry
	 * @return a new ClassifiedProposal
	 * @throws BestXException
	 *             if any error is preventing the creation
	 */
	public ClassifiedProposal createProposal(Instrument instrument, NoMDEntries entry) throws BestXException {
		ClassifiedProposal proposal = new ClassifiedProposal();
		proposal.setMarket(this.market);
		String dealerCode = null;
		try {
			dealerCode = entry.getString(MDEntryOriginator.FIELD);
			if (dealerCode == null) {
				// get dealer
				for (Group party : entry.getGroups(NoPartyIDs.FIELD)) {
					if (PartyRole.CONTRA_FIRM == party.getInt(PartyRole.FIELD)) {
						dealerCode = party.getString(PartyID.FIELD);
						break;
					}
				}
			}
			MarketMarketMaker mmm = this.marketMakerFinder.getMarketMarketMakerByCode(Market.MarketCode.MARKETAXESS,
					dealerCode);
			proposal.setMarketMarketMaker(mmm);
			// get Px and currency
			proposal.setPriceType(Proposal.PriceType.PRICE);

			BigDecimal px = new BigDecimal(Double.toString(entry.getDouble(MDEntryPx.FIELD)));
			if (px.scale() > 10)
				px = px.setScale(10, RoundingMode.HALF_UP);
			proposal.setPrice(new Money(instrument.getCurrency(), px));
			// get Qty
			proposal.setQty(new BigDecimal(Double.toString(entry.getDouble(MDEntrySize.FIELD))));
			// get date and time
			// getUtcDateOnly and getUtcTimeOnly convert to local timezone
			proposal.setTimestamp(DateService.composeDate(entry.getUtcDateOnly(MDEntryDate.FIELD),
					entry.getUtcTimeOnly(MDEntryTime.FIELD)));
			// proposal.setTimestampStr(DateService.format(DateService.dateTimeISO,
			// proposal.getTimestamp()));
			// get proposal type
			String type = "";
//			if (entry.getDouble(MDEntrySize.FIELD) > 0.0) { // get condition also when qty = 0
				type = entry.getString(QuoteCondition.FIELD);
//			}
			if (type.contains("I")) {
				proposal.setType(ProposalType.INDICATIVE);
				proposal.setProposalState(ProposalState.NEW);
			} else if (type.contains("A") || type.contains("L") || type.contains("d")) { // also
																							// "L",
																							// "d"
				proposal.setType(ProposalType.TRADEABLE);
				proposal.setProposalState(ProposalState.NEW);
			} else if (type.contains("B")) { // CLOSED
				proposal.setType(ProposalType.CLOSED);
				proposal.setProposalState(ProposalState.REJECTED);
				proposal.setProposalSubState(ProposalSubState.REJECTED_BY_MARKET);
			} else {
				proposal.setType(ProposalType.CLOSED);
				proposal.setProposalState(ProposalState.DROPPED);
				proposal.setProposalSubState(ProposalSubState.NONE);
			}
			// get settleDate
			try {
				proposal.setFutSettDate(DateService.convertUTCToLocal(entry.getUtcDateOnly(FutSettDate.FIELD)));
			} catch (@SuppressWarnings("unused") FieldNotFound e) {
				proposal.setFutSettDate(instrument.getDefaultSettlementDate());
			}
			// get side
			proposal.setSide(
					(entry.getChar(MDEntryType.FIELD) == MDEntryType.OFFER) ? ProposalSide.ASK : ProposalSide.BID);
			proposal.setVenue(venueFinder.getMarketMakerVenue(mmm.getMarketMaker()));
		} catch (Exception e) {
			throw new BestXException(e);
		}
		return proposal;
	}

	public ClassifiedProposal createZeroProposal(Instrument instrument, MarketMarketMaker mmm,
			Proposal.ProposalSide side) throws BestXException {
		ClassifiedProposal proposal = new ClassifiedProposal();
		proposal.setMarket(this.market);

		proposal.setMarketMarketMaker(mmm);
		// get Px and currency
		proposal.setPriceType(Proposal.PriceType.PRICE);
		proposal.setPrice(new Money(instrument.getCurrency(), BigDecimal.ZERO));
		// get Qty
		proposal.setQty(BigDecimal.ZERO);
		// get date and time
		// getUtcDateOnly and getUtcTimeOnly convert to local timezone
		proposal.setTimestamp(DateService.newUTCDate());
		// proposal.setTimestampStr(DateService.format(DateService.dateTimeISO,
		// proposal.getTimestamp()));
		// get proposal type
		proposal.setType(ProposalType.INDICATIVE);
		proposal.setProposalState(ProposalState.NEW);
		// get settleDate
		proposal.setFutSettDate(instrument.getDefaultSettlementDate());
		// get side
		proposal.setSide(side);
		proposal.setVenue(venueFinder.getMarketMakerVenue(mmm.getMarketMaker()));
		return proposal;
	}

	public static MarketAxessExecutionReport createExecutionReport(Instrument instrument,
			ExecutionReport tsExecutionReport) throws BestXException {
		MarketAxessExecutionReport executionReport = new MarketAxessExecutionReport();
		executionReport.setInstrument(instrument);

		boolean noPrices = true;
		try {
			// get exec type
			char execType = tsExecutionReport.getChar(ExecType.FIELD);
			// get order status
			char ordStatus = tsExecutionReport.getChar(OrdStatus.FIELD);
			executionReport.setOrdStatus(ordStatus);
			executionReport.setExecType(execType);
			// get fill data only if filled or cancelled
			if ((ExecType.CANCELED == execType || ExecType.TRADE == execType) && (OrdStatus.CANCELED == ordStatus
					|| OrdStatus.FILLED == ordStatus || OrdStatus.PARTIALLY_FILLED == ordStatus)) {
				try {
					if (tsExecutionReport.getInt(NoDealers.FIELD) > 0) {
						List<Group> dealerList = tsExecutionReport.getGroups(NoDealers.FIELD);
						for (Group dealer : dealerList) {
							// get dealers
							Dealers.NoDealers newDealer = new Dealers.NoDealers();
							String origDealerCode = dealer.getString(DealerID.FIELD);
							LOGGER.trace("Adding dealer to Execution report: original dealer {}", origDealerCode);
							if (origDealerCode.length() > 25) { // 20170930 AMC
																// max size
																// storable in
																// Database
								LOGGER.info("Will truncate original dealer code {} in Execution report to {}",
										origDealerCode, origDealerCode.substring(0, 24));
								origDealerCode = origDealerCode.substring(0, 24);
							}
							newDealer.set(new DealerID(origDealerCode));
							newDealer.set(new DealerIDSource(dealer.getString(DealerIDSource.FIELD)));
							try {
								newDealer.set(new DealerQuoteText(dealer.getString(DealerQuoteText.FIELD)));
							} catch (@SuppressWarnings("unused") Exception e) {
								; // no text
							}
							String temp = dealer.getString(CompetitiveStatus.FIELD);
							newDealer.set(new CompetitiveStatus(temp));
							if (temp.equalsIgnoreCase("Passed") || temp.equalsIgnoreCase("Timed Out")) { // if
																											// dealer
																											// has
																											// not
																											// priced
																											// something
								newDealer.set(new DealerQuotePriceType(Integer.toString(PriceType.PERCENTAGE)));
								newDealer.set(new DealerQuotePrice(0.0));
								newDealer.set(new DealerQuoteOrdQty(0.0));
								newDealer.set(new QuoteRank(dealer.getString(QuoteRank.FIELD)));
								executionReport.addDealer(newDealer);
								noPrices = false; // tells that there are prices
													// - no EXP-DNQ... prices
													// required
							} else if ((temp.equalsIgnoreCase("EXP-DNQ") || temp.equalsIgnoreCase("Cancelled"))
									&& noPrices) { // include only if there are
													// no other prices
								newDealer.set(new DealerQuotePriceType(Integer.toString(PriceType.PERCENTAGE)));
								newDealer.set(new DealerQuotePrice(0.0));
								newDealer.set(new DealerQuoteOrdQty(0.0));
								newDealer.set(new QuoteRank(dealer.getString(QuoteRank.FIELD)));
								executionReport.addDealer(newDealer);
							} else {
								if (temp.endsWith("Amended"))
									LOGGER.warn(
											"IMPORTANT! Received dealer quote with state {}. Please be sure this has been agreed with the dealer {}",
											temp, dealer.getString(DealerID.FIELD));
								newDealer.set(new DealerQuotePriceType(dealer.getString(DealerQuotePriceType.FIELD)));
								newDealer.set(new DealerQuotePrice(dealer.getDouble(DealerQuotePrice.FIELD)));
								newDealer.set(new DealerQuoteOrdQty(dealer.getDouble(DealerQuoteOrdQty.FIELD)));
								if (!"1".equalsIgnoreCase(dealer.getString(DealerQuotePriceType.FIELD)))
									newDealer.set(new ReferencePrice(dealer.getDouble(ReferencePrice.FIELD)));
								newDealer.set(new QuoteRank(dealer.getString(QuoteRank.FIELD)));
								executionReport.addDealer(newDealer);
								noPrices = false; // tells that there are prices
													// - no EXP-DNQ... prices
													// required
							}
						}
					}
				} catch (@SuppressWarnings("unused") FieldNotFound e) {
					;// do nothing.This happens when processing dealers with no
						// prices and there are valid prices with better ranking
				}
				if (ExecType.CANCELED != execType) {
					List<RootNoPartyIDs> parties = tsExecutionReport.getNoPartyIDsList();
					for (RootNoPartyIDs party : parties) {
						if (party.getPartyRole().getValue() == PartyRole.EXECUTING_FIRM) {
							executionReport.setExecBroker(party.getPartyID().getValue());
							break;
						}
					}
				}
			}
			String currency;
			try {
				currency = tsExecutionReport.getString(Currency.FIELD);
			} catch (FieldNotFound e) {
				throw new BestXException("Currency not defined", e);
			}
			// get Qty
			try {
				executionReport
						.setActualQty(new BigDecimal(Double.toString(tsExecutionReport.getDouble(CumQty.FIELD))));
			} catch (FieldNotFound e) {
				throw new BestXException("Actual Quantity not defined", e);
			}
			try {
				executionReport
						.setOrderQty(new BigDecimal(Double.toString(tsExecutionReport.getDouble(OrderQty.FIELD))));
			} catch (@SuppressWarnings("unused") FieldNotFound e) {
			}
			// get Price
			try {
				BigDecimal px = new BigDecimal(Double.toString(tsExecutionReport.getDouble(LastPx.FIELD)));
				if (px.scale() > 10)
					px = px.setScale(10, RoundingMode.HALF_UP);
				executionReport.setLastPx(px);
			} catch (@SuppressWarnings("unused") FieldNotFound e) {
			}
			try {
				BigDecimal px = new BigDecimal(Double.toString(tsExecutionReport.getDouble(AvgPx.FIELD)));
				if (px.scale() > 10)
					px = px.setScale(10, RoundingMode.HALF_UP);
				executionReport.setPrice(new Money(currency, px));
			} catch (@SuppressWarnings("unused") FieldNotFound e) {
			}
			try {
				executionReport.setPriceType(tsExecutionReport.getInt(PriceType.FIELD));
			} catch (@SuppressWarnings("unused") FieldNotFound e) {
			}
			// BESTX-314 CS tracking defect ID 16170
			// spread price in fill while all other prices were in percentage.
			// LastPx in percentage was in LastParPx
			if (executionReport.getPriceType() != null && executionReport.getPriceType() != PriceType.PERCENTAGE) {
				try {
					executionReport.setLastParPx(null);
					BigDecimal px = new BigDecimal(Double.toString(tsExecutionReport.getDouble(LastParPx.FIELD)));
					if (px.scale() > 10)
						px = px.setScale(10, RoundingMode.HALF_UP);
					executionReport.setLastParPx(px);
				} catch (@SuppressWarnings("unused") FieldNotFound e) {
				}
				if (executionReport.getLastParPx() != null) {
					executionReport.setLastPx(executionReport.getLastParPx());
					executionReport.setPriceType(PriceType.PERCENTAGE);
				}
			}
			// get transactionTime
			try {
				executionReport.setTransactTime(tsExecutionReport.getUtcTimeStamp(TransactTime.FIELD)); 
			} catch (@SuppressWarnings("unused") FieldNotFound e) {
			}
			try {
				executionReport.setExecutionReportId(tsExecutionReport.getString(ExecID.FIELD)); 
			} catch (@SuppressWarnings("unused") FieldNotFound e) {
			}
			try {
				executionReport.setTicket(tsExecutionReport.getString(ExecRefID.FIELD));
			} catch (@SuppressWarnings("unused") FieldNotFound e) {
			}
			try {
				// get accrued interest
				executionReport.setAccruedInterestAmount(new Money(currency,
						new BigDecimal(Double.toString(tsExecutionReport.getDouble(AccruedInterestAmt.FIELD)))));
			} catch (@SuppressWarnings("unused") FieldNotFound e) {
			}
			try {
				executionReport.setAccruedInterestRate(
						new BigDecimal(Double.toString(tsExecutionReport.getDouble(AccruedInterestRate.FIELD))));
			} catch (@SuppressWarnings("unused") FieldNotFound e) {
			}
			// try {
			// executionReport.setLastMkt(tsExecutionReport.getString(LastMkt.FIELD));
			// } catch (@SuppressWarnings("unused") FieldNotFound e) {
			// }
			try {
				// executionReport.setAccruedInterestDays();
				// get text and notes
				executionReport.setText(tsExecutionReport.getString(Text.FIELD));
			} catch (@SuppressWarnings("unused") FieldNotFound e) {
				executionReport.setText("");
			}
			try {
				executionReport.setNotes(tsExecutionReport.getString(Notes.FIELD));
			} catch (@SuppressWarnings("unused") FieldNotFound e) {
			}

			try {
				executionReport.setLastCapacity(tsExecutionReport.getChar(LastCapacity.FIELD));
			} catch (@SuppressWarnings("unused") FieldNotFound fnf) {
			}
			try {
				executionReport.setLastMkt(tsExecutionReport.getString(LastMkt.FIELD));
			} catch (@SuppressWarnings("unused") FieldNotFound fnf) {
			}
			try {
				executionReport.setOrderCapacity(tsExecutionReport.getChar(OrderCapacity.FIELD));
			} catch (@SuppressWarnings("unused") FieldNotFound fnf) {
			}
			try {
				executionReport
						.setMKTXTrdRegPublicationReason(tsExecutionReport.getChar(MKTXTrdRegPublicationReason.FIELD));
			} catch (@SuppressWarnings("unused") FieldNotFound fnf) {
			}
			try {
				executionReport.setMKTXTradeReportingInd(tsExecutionReport.getChar(MKTXTradeReportingInd.FIELD));
			} catch (@SuppressWarnings("unused") FieldNotFound fnf) {
			}
			try {
			   executionReport.setFutSettDate(tsExecutionReport.getUtcDateOnly(FutSettDate.FIELD));
         } catch (@SuppressWarnings("unused") FieldNotFound fnf) {
         }
			
			
			Parties parties = tsExecutionReport.getPartiesComponent();
			for (Group party : parties.getGroups(NoPartyIDs.FIELD)) {

				char partyIDSource = party.getChar(PartyIDSource.FIELD);
				int partyRole = party.getInt(PartyRole.FIELD);
				String partyId = party
						.getString(it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.PartyID.FIELD);

				if (partyIDSource == 'N' && partyRole == 1) {

					executionReport.setCounterpartLEI(partyId);
					break;

				}
			}

		} catch (FieldNotFound e1) {
			throw new BestXException("Inconsistent executionReport received", e1);
		}
		LOGGER.debug("MarketAxessExecutionReport created {}", executionReport);
		return executionReport;
	}

	/**
	 * Creates a classifiedProposal (Reject) with the specified reason
	 * 
	 * @param mdEntryType
	 *            Bid or Offer accepted
	 * @param instrument
	 *            the instrument
	 * @param marketMakerCode
	 *            the marketMakerCode associated with the ClassifiedProposal
	 * @param reason
	 *            the reason of the rejection
	 * @return a classifiedProposal
	 * @throws TradeStacException
	 *             if an error occured during the creation of the
	 *             classifiedProposal
	 */
	protected ClassifiedProposal getClassifiedProposalReject(Character mdEntryType, Instrument instrument,
			String marketMakerCode, String reason) throws TradeStacException {
		LOGGER.debug("mdEntryType = {}, instrument = {}, marketMakerCode = {}, reason = {}", mdEntryType, instrument,
				marketMakerCode, reason);

		if (mdEntryType == null || marketMakerCode == null || instrument == null || reason == null) {
			throw new IllegalArgumentException(MarketPriceDiscoveryHelper.PARAMS_CAN_NOT_BE_NULL);
		}

		if (mdEntryType != MDEntryType.BID && mdEntryType != MDEntryType.OFFER) {
			throw new IllegalArgumentException(
					"Unsupported mdEntryType [" + mdEntryType + "]! Only Bid or Offer value is accepted");
		}
		ClassifiedProposal classifiedProposal = null;

		try {

			MarketMarketMaker marketMarketMaker = marketMakerFinder.getMarketMarketMakerByCode(MarketCode.MARKETAXESS,
					marketMakerCode);
			if (marketMarketMaker == null) {
				throw new TradeStacException("No marketMarketMaker found for code [" + marketMakerCode + "]");
			}

			Venue venue = venueFinder.getMarketMakerVenue(marketMarketMaker.getMarketMaker());
			if (venue == null) {
				throw new TradeStacException("No venue found for code [" + marketMakerCode + "]");
			}
			if (venue.getMarket() == null) {
				throw new TradeStacException(
						"market is null for the venue retrieved with code [" + marketMakerCode + "]");
			}

			SubMarketCode subMarketCode = null;
			Market market = marketFinder.getMarketByCode(MarketCode.MARKETAXESS, subMarketCode);
			if (market == null) {
				throw new TradeStacException("No market found for code [" + MarketCode.MARKETAXESS + "]");
			}

			// --- Fill the proposal ---
			ProposalSide proposalSide = null;
			switch (mdEntryType) {
			case MDEntryType.BID:
				proposalSide = ProposalSide.BID;
				break;
			case MDEntryType.OFFER:
				proposalSide = ProposalSide.ASK;
				break;
			default:
				throw new IllegalArgumentException("Unsupported mdEntryType [" + mdEntryType + "]");
			}

			classifiedProposal = new ClassifiedProposal();

			classifiedProposal.setProposalState(ProposalState.REJECTED);
			classifiedProposal.setProposalSubState(ProposalSubState.REJECTED_BY_MARKET);
			classifiedProposal.setReason(reason);
			classifiedProposal.setType(ProposalType.CLOSED);

			classifiedProposal.setMarket(market);

			classifiedProposal.setMarketMarketMaker(marketMarketMaker);
			classifiedProposal.setVenue(venue);
			classifiedProposal.setQty(BigDecimal.ZERO);

			classifiedProposal.setPrice(new Money(instrument.getCurrency(), BigDecimal.ZERO));

			classifiedProposal.setSide(proposalSide);

		} catch (Exception e) {
			String proposal = instrument.getIsin() + "-" + marketMakerCode + "- " + mdEntryType;

			throw new TradeStacException(
					"Error retrieving classifiedProposal for [" + proposal + "]: " + e.getMessage(), e);
		}

		return classifiedProposal;
	}


	public static Character convertOrderCapacity( MarketOrder marketOrder) {
		if(marketOrder == null || marketOrder.getOrderCapacity() == null)
			return null;
		return marketOrder.getOrderCapacity().getFixCode();
	}


	public static char convertShortSellIndicatorToSideValue(MarketOrder marketOrder, char defaultShortSelling) {
		if(OrderSide.isBuy(marketOrder.getSide()))
				return OrderSide.BUY.getFixCode().charAt(0);
		OrderSide ssi = marketOrder.getShortSellIndicator();
		if (ssi == null)
			return defaultShortSelling;
		return ssi.getFixCode().charAt(0); // Marketaxess Short Sell management is the FIX standard one.
		// Default short sell indicator in BestX! is the value to be put in the side when there is no such indication in the market order
	}

}
