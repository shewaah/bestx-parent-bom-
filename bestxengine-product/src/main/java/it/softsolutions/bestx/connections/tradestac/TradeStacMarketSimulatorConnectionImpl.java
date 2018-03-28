
/*
 * Copyright 1997-2016 SoftSolutions! srl 
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Market.SubMarketCode;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.PriceDiscoveryPerformanceMonitor;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.tradestac.api.TradeStacException;
import it.softsolutions.tradestac.fix.field.InstrumentPartyIDSource;
import it.softsolutions.tradestac.fix.field.InstrumentPartyRole;
import it.softsolutions.tradestac.fix.field.MDEntryType;
import it.softsolutions.tradestac.fix.field.MDUpdateType;
import it.softsolutions.tradestac.fix.field.MsgType;
import it.softsolutions.tradestac.fix.field.SecurityIDSource;
import it.softsolutions.tradestac.fix.field.SubscriptionRequestType;
import it.softsolutions.tradestac.fix50.TSBusinessMessageReject;
import it.softsolutions.tradestac.fix50.TSMarketDataRequest;
import it.softsolutions.tradestac.fix50.TSMarketDataRequestReject;
import it.softsolutions.tradestac.fix50.TSMarketDataSnapshotFullRefresh;
import it.softsolutions.tradestac.fix50.TSNoMDEntryTypes;
import it.softsolutions.tradestac.fix50.TSNoPartyID;
import it.softsolutions.tradestac.fix50.TSTradeStacNotification;
import it.softsolutions.tradestac.fix50.TSTradeStacResponse;
import it.softsolutions.tradestac.fix50.component.TSInstrmtMDReqGrp;
import it.softsolutions.tradestac.fix50.component.TSInstrument;
import it.softsolutions.tradestac.fix50.component.TSInstrumentParties;
import it.softsolutions.tradestac.fix50.component.TSInstrumentParties.TSNoInstrumentParties;
import it.softsolutions.tradestac.fix50.component.TSMDFullGrp;
import it.softsolutions.tradestac.fix50.component.TSMDReqGrp;
import quickfix.ConfigError;
import quickfix.SessionID;

/**
 *
 * Purpose: this class the connector to the tradestac market simulator
 *
 * Project Name : bestxengine-product
 * First created by: anna.cochetti
 * Creation date: 16 ago 2016
 * 
 **/

public class TradeStacMarketSimulatorConnectionImpl extends AbstractTradeStacConnection implements TradeStacPreTradeConnection {

	private static final Logger LOGGER = LoggerFactory.getLogger(TradeStacMarketSimulatorConnectionImpl.class);
	public static final String REJECT_MARKET_MAKER_START_CONTAINER = "[";
	public static final String REJECT_MARKET_MAKER_END_CONTAINER = "]";
	public static final String PRICE_SOURCE_MARKET_MAKER_ASSOCIATOR = "@";
	private static final String PARAMS_CAN_NOT_BE_NULL = "params can't be null";

	public class LiteMarketDataRequest {

		private String mdReqID;
		private Instrument instrument;
		private List<String> marketMakerCodes;

		/**
		 * @param mdReqID
		 * @param instrument
		 * @param marketMakerCodes
		 */
		public LiteMarketDataRequest(String mdReqID, Instrument instrument, List<String> marketMakerCodes) {
			super();
			this.mdReqID = mdReqID;
			this.instrument = instrument;
			this.marketMakerCodes = marketMakerCodes;
		}

		/**
		 * @return the mdReqID
		 */
		public String getMdReqID() {
			return mdReqID;
		}

		/**
		 * @param mdReqID the mdReqID to set
		 */
		public void setMdReqID(String mdReqID) {
			this.mdReqID = mdReqID;
		}

		/**
		 * @return the instrument
		 */
		public Instrument getInstrument() {
			return instrument;
		}

		/**
		 * @param instrument the instrument to set
		 */
		public void setInstrument(Instrument instrument) {
			this.instrument = instrument;
		}

		/**
		 * @return the marketMakerCodes
		 */
		public List<String> getMarketMakerCodes() {
			return marketMakerCodes;
		}

		/**
		 * @param marketMakerCodes the marketMakerCodes to set
		 */
		public void setMarketMakerCodes(List<String> marketMakerCodes) {
			this.marketMakerCodes = marketMakerCodes;
		}
	}


	private TradeStacPreTradeConnectionListener tradeStacPreTradeConnectionListener;

	private InstrumentFinder instrumentFinder;
	private MarketMakerFinder marketMakerFinder;
	private VenueFinder venueFinder;
	private MarketFinder marketFinder;
	protected MarketCode marketCode;

	public void setMarketCode(String marketCode){
		this.marketCode = MarketCode.valueOf(marketCode);
	}

	private ConcurrentMap<String, LiteMarketDataRequest> mdReqIDMap = new ConcurrentHashMap<String, LiteMarketDataRequest>();



	public TradeStacMarketSimulatorConnectionImpl() {
		super("marketSimulator - needs initialization");

	}

	public void setConnectionName(String connectionName) {
		this.connectionName = connectionName;
	}

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

	}

	@SuppressWarnings("unused")
	private void delay(long delayMillisecs) {
		try {
			Thread.sleep(delayMillisecs);
		} catch (InterruptedException e) {
			;
		}
	}

	@Override
	public void onMarketDataSnapshotFullRefresh(SessionID sessionID, TSMarketDataSnapshotFullRefresh tsMarketDataSnapshotFullRefresh) throws TradeStacException {
		LOGGER.trace("{}, {}", sessionID, tsMarketDataSnapshotFullRefresh);
		
		PriceDiscoveryPerformanceMonitor.logEvent(sessionID.toString(), "---->  onMarketDataSnapshotFullRefresh");

		String isinCode = null;
		TSInstrument tsInstrument = tsMarketDataSnapshotFullRefresh.getTSInstrument();
		if(tsInstrument.getSecurityIDSource() == null)
			tsInstrument.setSecurityIDSource(SecurityIDSource.IsinNumber);
		if (tsInstrument != null && tsInstrument.getSecurityIDSource() == SecurityIDSource.IsinNumber) {
			isinCode = tsInstrument.getSecurityID();
		} else {
			throw new TradeStacException("IsinCode has not been specified: " + tsMarketDataSnapshotFullRefresh);
		}

		try {
			Instrument instrument = instrumentFinder.getInstrumentByIsin(isinCode);


			ClassifiedProposal bidClassifiedProposal = getClassifiedProposal(MDEntryType.Bid, tsMarketDataSnapshotFullRefresh, instrument);
			ClassifiedProposal askClassifiedProposal = getClassifiedProposal(MDEntryType.Offer, tsMarketDataSnapshotFullRefresh, instrument);

			// finally invoke the BLP listener
			tradeStacPreTradeConnectionListener.onClassifiedProposal(instrument, askClassifiedProposal, bidClassifiedProposal);
			
			String mdReqID = tsMarketDataSnapshotFullRefresh.getMDReqID();
			removeLiteMarketDataRequest(mdReqID, bidClassifiedProposal.getMarketMarketMaker().getMarketSpecificCode());

		} catch (BestXException e) {
			String mdReqID = tsMarketDataSnapshotFullRefresh.getMDReqID();
			LiteMarketDataRequest liteMarketDataRequest = getLiteMarketDataRequest(mdReqID);
			Instrument instrument = liteMarketDataRequest.getInstrument();
			String marketMakerCode = getMarketMarketMakerCode(tsMarketDataSnapshotFullRefresh, 0);
			sendClassifiedProposalReject(instrument, marketMakerCode, "Settlement date missing");
			LOGGER.error(e.getMessage(), e);
		}
		
		PriceDiscoveryPerformanceMonitor.finalize(sessionID.toString(), "---->  onMarketDataSnapshotFullRefresh done");
	}

	/**
	 * @param tsMarketDataSnapshotFullRefresh
	 * @return
	 */
	private String getMarketMarketMakerCode(TSMarketDataSnapshotFullRefresh tsMarketDataSnapshotFullRefresh, int i) {
		return tsMarketDataSnapshotFullRefresh.getTSMDFullGrp().getTSNoMDEntriesList().get(i).getTsParties().getTSNoPartyIDsList().get(0).getPartyID();
	}


	@Override
	public void onTradeStacResponse(SessionID sessionID, TSTradeStacResponse tsTradeStacResponse) throws TradeStacException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onTradeStacNotification(SessionID sessionID, TSTradeStacNotification tsTradeStacNotification) throws TradeStacException {
		throw new UnsupportedOperationException();
	}


	protected ClassifiedProposal getClassifiedProposal(MDEntryType mdEntryType, TSMarketDataSnapshotFullRefresh tsMarketDataSnapshotFullRefresh, Instrument instrument) throws BestXException {
		LOGGER.trace("mdEntryType = {}, tsMarketDataSnapshotFullRefresh = {}, instrument = {}", mdEntryType, tsMarketDataSnapshotFullRefresh, instrument);

		if (mdEntryType == null || tsMarketDataSnapshotFullRefresh == null || instrument == null) {
			throw new IllegalArgumentException(PARAMS_CAN_NOT_BE_NULL);
		}

		ClassifiedProposal classifiedProposal = null;

		try {
			String code = getMarketMarketMakerCode(tsMarketDataSnapshotFullRefresh, 0);  // Assume all data are on same MMM which is placed at position 0 in group
			//			String code = getMarketMaker(tsMarketDataSnapshotFullRefresh.getTSInstrument());

			MarketMarketMaker marketMarketMaker = marketMakerFinder.getMarketMarketMakerByCode(marketCode, code);
			if (marketMarketMaker == null) {
				throw new BestXException("No marketMarketMaker found for code [" + code + "]");
			}

			Venue venue = venueFinder.getMarketMakerVenue(marketMarketMaker.getMarketMaker());
			if (venue == null) {
				throw new BestXException("No venue found for code [" + code + "]");
			}
			if (venue.getMarket() == null) {
				throw new BestXException("market is null for the venue retrieved with code [" + code + "]");
			}

			SubMarketCode subMarketCode = null;
			Market market = marketFinder.getMarketByCode(marketCode, subMarketCode);
			if (market == null) {
				throw new BestXException("No market found for code [" + marketCode + "]");
			}

			// --- Fill the proposal ---
			ProposalSide proposalSide = null;
			switch (mdEntryType) {
			case Bid:
				proposalSide = ProposalSide.BID;
				break;
			case Offer:
				proposalSide = ProposalSide.ASK;
				break;
			default:
				throw new BestXException("Unsupported mdEntryType [" + mdEntryType + "]");
			}

			Date date = null;
			Date time = null;
			BigDecimal qty = null;
			BigDecimal amount = null;
			ProposalType proposalType = null;

			if (tsMarketDataSnapshotFullRefresh.getTSMDFullGrp() != null && tsMarketDataSnapshotFullRefresh.getTSMDFullGrp().getTSNoMDEntriesList() != null) {

				List<TSMDFullGrp.TSNoMDEntries> tsNoMDEntriesList = tsMarketDataSnapshotFullRefresh.getTSMDFullGrp().getTSNoMDEntriesList();

				// Set defaultDate as today

				for (TSMDFullGrp.TSNoMDEntries tsNoMDEntries : tsNoMDEntriesList) {

					if (tsNoMDEntries.getMDEntryType() == mdEntryType) {

						if (tsNoMDEntries.getMDEntryTime() != null) {
							time = tsNoMDEntries.getMDEntryTime();
						}

						if(tsNoMDEntries.getQuoteCondition() != null) {
							switch (tsNoMDEntries.getQuoteCondition()) {
							case NonFirm:
								proposalType = ProposalType.INDICATIVE;
								break;
							case OpenActive:
							case ConsolidatedBest:
								proposalType = ProposalType.TRADEABLE;
								break;
							default:
								proposalType = ProposalType.CLOSED;
							}
						}
						if (tsNoMDEntries.getMDEntryDate() != null) {
							date = tsNoMDEntries.getMDEntryDate();
						}

						if (tsNoMDEntries.getMDEntrySize() != null) {
							// [DR20120515] do not use directly the Double but convert to String in order to prevent this issue:
							// "price = 131.66300000000001091393642127513885498046875"
							qty = new BigDecimal("" + tsNoMDEntries.getMDEntrySize());
						}

						if (tsNoMDEntries.getMDEntryPx() != null) {
							amount = new BigDecimal("" + tsNoMDEntries.getMDEntryPx()).setScale(10, RoundingMode.HALF_UP);
						}
					}
				}
			}
			// [AMC20160823] when there is no quote condition in message assume indicative
			if(proposalType == null) {
				proposalType = ProposalType.INDICATIVE;
			}
			String reason = null;

			// [FA20120518] Workaround the market maker might not have prices (one or both side)
			if (amount == null) {
				amount = BigDecimal.ZERO;
				proposalType = ProposalType.INDICATIVE;
				// reason = "No price available";

				LOGGER.warn("The market maker {} might not have prices for {}", code, instrument.getIsin());
			}

			// [DR20120515] Workaround due to missing quantities passed by Bloomberg
			if (qty == null) {
				qty = BigDecimal.ZERO;
				LOGGER.warn("The market maker {} might not have quantities for {}", code, instrument.getIsin());
			}

			// [DR20120504] TODO is the venue.copyOf necessary ?
			Venue bloombergVenue = new Venue(venue);
			bloombergVenue.setMarket(market);

			classifiedProposal = new ClassifiedProposal();
			classifiedProposal.setMarket(market);
			classifiedProposal.setMarketMarketMaker(marketMarketMaker);
			classifiedProposal.setVenue(bloombergVenue);
			classifiedProposal.setProposalState(ProposalState.NEW);
			classifiedProposal.setType(proposalType);
			classifiedProposal.setSide(proposalSide);
			classifiedProposal.setQty(qty);
			classifiedProposal.setFutSettDate(instrument.getBBSettlementDate());
			classifiedProposal.setNonStandardSettlementDateAllowed(false);
			classifiedProposal.setNativeMarketMarketMaker(marketMarketMaker);

			Date timestamp = DateService.convertUTCToLocal(date, time);
			classifiedProposal.setTimestamp(timestamp);

			Money price = new Money(instrument.getCurrency(), amount);
			classifiedProposal.setPrice(price);

			Date today = DateService.newLocalDate();
			if (!DateUtils.isSameDay(timestamp, today)) {
				String todayStr = DateService.format(DateService.dateISO, today);
				String dateStr = DateService.format(DateService.dateISO, timestamp);

				LOGGER.info("The proposal " + classifiedProposal + ", has a date (" + dateStr + ") different from today (" + todayStr + "): set it as rejected.");
				classifiedProposal.setProposalState(ProposalState.REJECTED);
				reason = Messages.getString("RejectProposalPriceTooOld", "" + dateStr);
				// classifiedProposal.setReason(Messages.getString("RejectProposalPriceTooOld", "" + dateStr));
			}

			if (reason != null) {
				classifiedProposal.setReason(reason);
			}

			LOGGER.info("{} {} {} {} {} {} {} {} {}", instrument.getIsin(), market.getMarketCode(), marketMarketMaker.getMarketSpecificCode(),
					classifiedProposal.getProposalState().name(), proposalType.name(), mdEntryType, amount, qty, reason);
		} catch (Exception e) {
			throw new BestXException("Error retrieving classifiedProposal for [" + tsMarketDataSnapshotFullRefresh + "]: " + e.getMessage(), e);
		}

		return classifiedProposal;
	}

	public void addLiteMarketDataRequest(String mdReqID, Instrument instrument, List<String> marketMakerCodes) {
		LOGGER.trace("mdReqID = {}, instrument = {}, marketMakerCodes = {}", mdReqID, instrument, marketMakerCodes);

		LiteMarketDataRequest liteMarketDataRequest = new LiteMarketDataRequest(mdReqID, instrument, marketMakerCodes);
		mdReqIDMap.put(mdReqID, liteMarketDataRequest);
	}

	public LiteMarketDataRequest getLiteMarketDataRequest(String mdReqID) {
		return mdReqIDMap.get(mdReqID);
	}

	/**
	 * @param mdReqID
	 */
	public void removeLiteMarketDataRequest(String mdReqID) {
		LOGGER.trace("mdReqID = {}", mdReqID);

		mdReqIDMap.remove(mdReqID);
	}

	/**
	 * @param mdReqID
	 */
	public void removeLiteMarketDataRequest(String mdReqID, String marketMakerCode) {
		LOGGER.trace("mdReqID = {}, marketMakerCode = {}", mdReqID, marketMakerCode);

		LiteMarketDataRequest liteMarketDataRequest = mdReqIDMap.get(mdReqID);

		if (liteMarketDataRequest != null) {
			liteMarketDataRequest.getMarketMakerCodes().remove(marketMakerCode);

			if (liteMarketDataRequest.getMarketMakerCodes().size() == 0) {
				mdReqIDMap.remove(mdReqID);
			}
		}
	}

	private void sendClassifiedProposalReject(Instrument instrument, String marketMakerCode, String reason) {
		try {
			ClassifiedProposal bidClassifiedProposal = getClassifiedProposalReject(MDEntryType.Bid, instrument, marketMakerCode, reason);
			ClassifiedProposal askClassifiedProposal = getClassifiedProposalReject(MDEntryType.Offer, instrument, marketMakerCode, reason);

			// finally invoke the BLP listener
			tradeStacPreTradeConnectionListener.onClassifiedProposal(instrument, askClassifiedProposal, bidClassifiedProposal);

		} catch (BestXException e) {
			LOGGER.error(e.getMessage(), e);
		}
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
	 * @throws BLPException
	 *             if an error occured during the creation of the classifiedProposal
	 */
	protected ClassifiedProposal getClassifiedProposalReject(MDEntryType mdEntryType, Instrument instrument, String marketMakerCode, String reason) throws BestXException {
		LOGGER.debug("mdEntryType = {}, instrument = {}, marketMakerCode = {}, reason = {}", mdEntryType, instrument, marketMakerCode, reason);

		if (mdEntryType == null || marketMakerCode == null || instrument == null || reason == null) {
			throw new IllegalArgumentException(PARAMS_CAN_NOT_BE_NULL);
		}

		if (mdEntryType != MDEntryType.Bid && mdEntryType != MDEntryType.Offer) {
			throw new IllegalArgumentException("Unsupported mdEntryType [" + mdEntryType + "]! Only Bid or Offer value is accepted");
		}
		ClassifiedProposal classifiedProposal = null;

		try {

			MarketMarketMaker marketMarketMaker = marketMakerFinder.getMarketMarketMakerByCode(marketCode, marketMakerCode);
			if (marketMarketMaker == null) {
				throw new BestXException("No marketMarketMaker found for code [" + marketMakerCode + "]");
			}

			Venue venue = venueFinder.getMarketMakerVenue(marketMarketMaker.getMarketMaker());
			if (venue == null) {
				throw new BestXException("No venue found for code [" + marketMakerCode + "]");
			}
			if (venue.getMarket() == null) {
				throw new BestXException("market is null for the venue retrieved with code [" + marketMakerCode + "]");
			}

			SubMarketCode subMarketCode = null;
			Market market = marketFinder.getMarketByCode(marketCode, subMarketCode);
			if (market == null) {
				throw new BestXException("No market found for code [" + marketCode + "]");
			}

			// --- Fill the proposal ---
			ProposalSide proposalSide = null;
			switch (mdEntryType) {
			case Bid:
				proposalSide = ProposalSide.BID;
				break;
			case Offer:
				proposalSide = ProposalSide.ASK;
				break;
			default:
				throw new IllegalArgumentException("Unsupported mdEntryType [" + mdEntryType + "]");
			}

			classifiedProposal = new ClassifiedProposal();

			classifiedProposal.setProposalState(ProposalState.REJECTED);
			classifiedProposal.setProposalSubState(ProposalSubState.REJECTED_BY_MARKET);
			classifiedProposal.setReason(reason);
			// [FA20120521] Workaround: ClassifiedSpreadComparator.compare (Sort Book) needs type not null
			classifiedProposal.setType(ProposalType.CLOSED);

			// [DR20120504] TODO is the venue.copyOf necessary ?
			Venue bloombergVenue = new Venue(venue);
			bloombergVenue.setMarket(market);
			classifiedProposal.setMarket(market);

			classifiedProposal.setMarketMarketMaker(marketMarketMaker);
			classifiedProposal.setVenue(bloombergVenue);
			classifiedProposal.setQty(BigDecimal.ZERO);

			Money price = new Money(instrument.getCurrency(), BigDecimal.ZERO);
			classifiedProposal.setPrice(price);

			classifiedProposal.setProposalState(ProposalState.REJECTED);
			classifiedProposal.setProposalSubState(ProposalSubState.REJECTED_BY_MARKET);
			classifiedProposal.setSide(proposalSide);

		} catch (Exception e) {
			String proposal = instrument.getIsin() + "-" + marketMakerCode + "- " + mdEntryType.name();

			throw new BestXException("Error retrieving classifiedProposal for [" + proposal + "]: " + e.getMessage(), e);
		}

		return classifiedProposal;
	}

	public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
		this.instrumentFinder = instrumentFinder;
	}

	public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
		this.marketMakerFinder = marketMakerFinder;
	}

	public void setVenueFinder(VenueFinder venueFinder) {
		this.venueFinder = venueFinder;
	}

	public void setMarketFinder(MarketFinder marketFinder) {
		this.marketFinder = marketFinder;
	}

	public TradeStacPreTradeConnectionListener getTradeStacPreTradeConnectionListener() {
		return tradeStacPreTradeConnectionListener;
	}

	public void setTradeStacPreTradeConnectionListener(
			TradeStacPreTradeConnectionListener tradeStacPreTradeConnectionListener) {
		this.tradeStacPreTradeConnectionListener = tradeStacPreTradeConnectionListener;
	}

	@Override
	public void requestInstrumentStatus() throws BestXException {
		LOGGER.error("call of unimplemented method requestInstrumentStatus");
		LOGGER.error(new BestXException().getStackTrace().toString());
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
	/**
	 * Creates a Market Data Request
	 * 
	 * @param instrument
	 *            the instrument requested
	 * @param marketMakerCodes
	 *            the set of "marketMakerCodes" requested for the specified instrument
	 * @return a Market Data Request
	 * @throws BestXException
	 *             if the specified instrument's isin is null
	 */
	public TSMarketDataRequest createMarketDataRequest(Instrument instrument, List<String> marketMakerCodes) throws BestXException {
		LOGGER.trace("{}, {}", instrument, marketMakerCodes);

		if (instrument == null || marketMakerCodes == null) {
			throw new IllegalArgumentException(PARAMS_CAN_NOT_BE_NULL);
		}

		if (instrument.getIsin() == null) {
			throw new BestXException("Instrument.isin is null");
		}

		TSMarketDataRequest tsMarketDataRequest = new TSMarketDataRequest();

		String mdReqID = "" + System.currentTimeMillis();

		tsMarketDataRequest.setMDReqID(mdReqID);
		tsMarketDataRequest.setAggregatedBook(false);
		tsMarketDataRequest.setMarketDepth(1);
		tsMarketDataRequest.setMDUpdateType(MDUpdateType.FullRefresh);
		tsMarketDataRequest.setSubscriptionRequestType(SubscriptionRequestType.Snapshot);

		// isin setting
		TSInstrument tsInstrument = new TSInstrument();
		TSInstrumentParties tsInstrumentParties = new TSInstrumentParties();
		List<TSNoInstrumentParties> tsNoInstrumentPartiesList = new ArrayList<TSInstrumentParties.TSNoInstrumentParties>();

		tsInstrument.setSecurityID(instrument.getIsin());
		tsInstrument.setSecurityIDSource(SecurityIDSource.IsinNumber);
		tsInstrument.setSymbol("[N/A]");

		for (String marketMaker : marketMakerCodes) {
			TSNoInstrumentParties party = new TSNoInstrumentParties();

			party.setInstrumentPartyID(marketMaker);
			party.setInstrumentPartyIDSource(InstrumentPartyIDSource.GenerallyAcceptedMarketPartecipantIdentifier);
			party.setInstrumentPartyRole(InstrumentPartyRole.MarketMaker);
			tsNoInstrumentPartiesList.add(party);
		}
		tsInstrumentParties.setTSNoPartyIDsList(tsNoInstrumentPartiesList);
		tsInstrument.setTSInstrumentParties(tsInstrumentParties);

		TSInstrmtMDReqGrp.TSNoRelatedSym noRelatedSym = new TSInstrmtMDReqGrp.TSNoRelatedSym();
		noRelatedSym.setTSInstrument(tsInstrument);

		List<TSInstrmtMDReqGrp.TSNoRelatedSym> tsNoRelatedSymList = new ArrayList<TSInstrmtMDReqGrp.TSNoRelatedSym>();
		TSInstrmtMDReqGrp instrmtMDReqGrp = new TSInstrmtMDReqGrp();
		tsNoRelatedSymList.add(noRelatedSym);
		instrmtMDReqGrp.setTSNoRelatedSymList(tsNoRelatedSymList);
		tsMarketDataRequest.setTSInstrmtMDReqGrp(instrmtMDReqGrp);

		// TSMDReqGrp
		TSMDReqGrp tsMDReqGrp = new TSMDReqGrp();
		List<TSNoMDEntryTypes> tsMDEntryTypesList = new ArrayList<TSNoMDEntryTypes>();

		// Bid
		{
			TSNoMDEntryTypes tsNoMDEntryTypesBid = new TSNoMDEntryTypes();
			tsNoMDEntryTypesBid.setMDEntryType(MDEntryType.Bid);
			tsMDEntryTypesList.add(tsNoMDEntryTypesBid);
		}

		// Offer
		{
			TSNoMDEntryTypes tsNoMDEntryTypesOffer = new TSNoMDEntryTypes();
			tsNoMDEntryTypesOffer.setMDEntryType(MDEntryType.Offer);
			tsMDEntryTypesList.add(tsNoMDEntryTypesOffer);
		}
		tsMDReqGrp.setTSMDEntryTypesList(tsMDEntryTypesList);
		tsMarketDataRequest.setTSMDReqGrp(tsMDReqGrp);

		return tsMarketDataRequest;
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

		TSMarketDataRequest tsMarketDataRequest = createMarketDataRequest(instrument, marketMakerCodes);
		PriceDiscoveryPerformanceMonitor.logEvent(tsMarketDataRequest.getMDReqID(), "---->  requestInstrumentPriceSnapshot");
		try {
			tradeStacClientSession.manageMarketDataRequest(tsMarketDataRequest);

			addLiteMarketDataRequest(tsMarketDataRequest.getMDReqID(), instrument, marketMakerCodes);

		} catch (TradeStacException e) {
			throw new BestXException(String.format("Error managing marketDataRequest [%s]", tsMarketDataRequest), e);
		}
	}

	/*
	 * At the moment is not possible to notify the request is failed: it has not the information to create the reject classifiedProposal. 
	 * It is need to store the request in the map.
	 */
	@Override
	public void onBusinessMessageReject(SessionID sessionID, TSBusinessMessageReject tsBusinessMessageReject) throws TradeStacException {
		LOGGER.error("{}, {}", sessionID, tsBusinessMessageReject);
		PriceDiscoveryPerformanceMonitor.logEvent(sessionID.toString(), "---->  onBusinessMessageReject");

		MsgType refMsgType = tsBusinessMessageReject.getRefMsgType();
		if (refMsgType == MsgType.MarketDataRequest) {
			String mdReqID = tsBusinessMessageReject.getBusinessRejectRefID();

			LiteMarketDataRequest liteMarketDataRequest = getLiteMarketDataRequest(mdReqID);

			if (liteMarketDataRequest != null) {
				Instrument instrument = liteMarketDataRequest.getInstrument();
				List<String> marketMakerCodes = liteMarketDataRequest.getMarketMakerCodes();
				String reason = tsBusinessMessageReject.getText() != null ? tsBusinessMessageReject.getText() : "Generic error, see log for details";

				for (String marketMakerCode : marketMakerCodes) {
					sendClassifiedProposalReject(instrument, marketMakerCode, reason);
				}

				removeLiteMarketDataRequest(mdReqID);
			} else {
				LOGGER.warn("Unable to retrieve information for the mdReqID specified [{}], skip it: {}", mdReqID, tsBusinessMessageReject);
			}
		} else {
			LOGGER.error("{}, {}", sessionID, tsBusinessMessageReject);
		}
		PriceDiscoveryPerformanceMonitor.finalize(sessionID.toString(), "---->  onBusinessMessageReject done");
	}

   @Override
   public void onMarketDataRequestReject(SessionID sessionID, TSMarketDataRequestReject tsMarketDataRequestReject) throws TradeStacException {
      LOGGER.trace("{}, {}", sessionID, tsMarketDataRequestReject);
      PriceDiscoveryPerformanceMonitor.logEvent(sessionID.toString(), "---->  onMarketDataRequestReject");
      final String mdReqID = tsMarketDataRequestReject.getMDReqID();
      final LiteMarketDataRequest liteMarketDataRequest = getLiteMarketDataRequest(mdReqID);
      final Instrument instrument = liteMarketDataRequest.getInstrument();

      final StringBuilder marketMakerCodeBuilder = new StringBuilder();
      if (tsMarketDataRequestReject.getTSParties() != null) {
         for (TSNoPartyID partyID : tsMarketDataRequestReject.getTSParties().getTSNoPartyIDsList()) {
            marketMakerCodeBuilder.append(partyID.getPartyID());
         }
      }

      final String marketMakerCode = marketMakerCodeBuilder.toString();
      final String reason = String.valueOf(tsMarketDataRequestReject.getMDReqRejReason());

      sendClassifiedProposalReject(instrument, marketMakerCode, reason);
      removeLiteMarketDataRequest(mdReqID, marketMakerCode);
      PriceDiscoveryPerformanceMonitor.finalize(sessionID.toString(), "---->  onMarketDataRequestReject done");
   }

}
