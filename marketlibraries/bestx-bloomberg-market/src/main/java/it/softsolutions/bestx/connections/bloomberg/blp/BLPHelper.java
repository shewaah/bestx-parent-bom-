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
package it.softsolutions.bestx.connections.bloomberg.blp;

import java.math.BigDecimal;
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
import it.softsolutions.bestx.connections.bloomberg.BloombergProposalInputLazyBean;
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
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.tradestac.fix.field.InstrumentPartyIDSource;
import it.softsolutions.tradestac.fix.field.InstrumentPartyRole;
import it.softsolutions.tradestac.fix.field.MDEntryType;
import it.softsolutions.tradestac.fix.field.MDUpdateType;
import it.softsolutions.tradestac.fix.field.NetworkRequestType;
import it.softsolutions.tradestac.fix.field.SecurityIDSource;
import it.softsolutions.tradestac.fix.field.SubscriptionRequestType;
import it.softsolutions.tradestac.fix50.TSMarketDataRequest;
import it.softsolutions.tradestac.fix50.TSMarketDataSnapshotFullRefresh;
import it.softsolutions.tradestac.fix50.TSNetworkCounterpartySystemStatusRequest;
import it.softsolutions.tradestac.fix50.TSNoMDEntryTypes;
import it.softsolutions.tradestac.fix50.component.TSCompIDReqGrp;
import it.softsolutions.tradestac.fix50.component.TSInstrmtMDReqGrp;
import it.softsolutions.tradestac.fix50.component.TSInstrument;
import it.softsolutions.tradestac.fix50.component.TSInstrumentParties;
import it.softsolutions.tradestac.fix50.component.TSInstrumentParties.TSNoInstrumentParties;
import it.softsolutions.tradestac.fix50.component.TSMDFullGrp;
import it.softsolutions.tradestac.fix50.component.TSMDReqGrp;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-bloomberg-market First created by: fabrizio.aponte Creation date: 22/mag/2012
 * 
 **/
public class BLPHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(BLPHelper.class);

    public static final String REJECT_MARKET_MAKER_START_CONTAINER = "[";
    public static final String REJECT_MARKET_MAKER_END_CONTAINER = "]";
    public static final String PRICE_SOURCE_MARKET_MAKER_ASSOCIATOR = "@";
    private static final String PARAMS_CAN_NOT_BE_NULL = "params can't be null";

    private InstrumentFinder instrumentFinder;
    private MarketMakerFinder marketMakerFinder;
    private VenueFinder venueFinder;
    private MarketFinder marketFinder;
    
    private ConcurrentMap<String, LiteMarketDataRequest> mdReqIDMap = new ConcurrentHashMap<String, LiteMarketDataRequest>();

    /**
     * Constructor
     * 
     * @param instrumentFinder
     *            the instrument finder
     * @param marketMakerFinder
     *            the marketMaker finder
     * @param venueFinder
     *            the venue finder
     * @param marketFinder
     *            the market finder
     */
    public BLPHelper(InstrumentFinder instrumentFinder, MarketMakerFinder marketMakerFinder, VenueFinder venueFinder, MarketFinder marketFinder) {
        this.instrumentFinder = instrumentFinder;
        this.marketMakerFinder = marketMakerFinder;
        this.venueFinder = venueFinder;
        this.marketFinder = marketFinder;
    }

    /**
     * Method is protected in order to permit single unitTesting on it
     * 
     * @param isin
     *            the isinCode
     * @return the instrument with the specified isinCode
     * @throws BLPException
     *             if the passed param is null or no instrument found
     */
    protected Instrument getInstrument(String isin) throws BLPException {
        LOGGER.trace("{}", isin);

        if (isin == null) {
            throw new IllegalArgumentException("isin can't be null");
        }

        Instrument instrument = null;

        instrument = instrumentFinder.getInstrumentByIsin(isin);

        if (instrument == null) {
            throw new BLPException("No Instrument found with ISIN [" + isin + "]");
        }

        if (instrument.getCurrency() == null) {
            throw new BLPException("No default currency found for instrument with ISIN [" + isin + "]");
        }
        
        if (instrument.getBBSettlementDate() == null) {
            throw new BLPException("No default BBSettlementDate found for instrument with ISIN [" + isin + "]");
        }

        return instrument;
    }

    /**
     * Creates a classifiedProposal starting from a MarketDataSnapshotFullRefresh
     * 
     * @param mdEntryType
     *            Bid or Offer accepted
     * @param tsMarketDataSnapshotFullRefresh
     *            the MarketDataSnapshotFullRefresh
     * @param instrument
     *            the instrument
     * @return a classifiedProposal
     * @throws BLPException
     *             if an error occured during the creation of the classifiedProposal
     */
    protected ClassifiedProposal getClassifiedProposal(MDEntryType mdEntryType, TSMarketDataSnapshotFullRefresh tsMarketDataSnapshotFullRefresh, Instrument instrument) throws BLPException {
        LOGGER.trace("mdEntryType = {}, tsMarketDataSnapshotFullRefresh = {}, instrument = {}", mdEntryType, tsMarketDataSnapshotFullRefresh, instrument);

        if (mdEntryType == null || tsMarketDataSnapshotFullRefresh == null || instrument == null) {
            throw new IllegalArgumentException(PARAMS_CAN_NOT_BE_NULL);
        }

        ClassifiedProposal classifiedProposal = null;

        try {
            String code = getMarketMaker(tsMarketDataSnapshotFullRefresh.getTSInstrument());

            MarketMarketMaker marketMarketMaker = marketMakerFinder.getMarketMarketMakerByCode(MarketCode.BLOOMBERG, code);
            if (marketMarketMaker == null) {
                throw new BLPException("No marketMarketMaker found for code [" + code + "]");
            }

            Venue venue = venueFinder.getMarketMakerVenue(marketMarketMaker.getMarketMaker());
            if (venue == null) {
                throw new BLPException("No venue found for code [" + code + "]");
            }
            if (venue.getMarket() == null) {
                throw new BLPException("market is null for the venue retrieved with code [" + code + "]");
            }

            SubMarketCode subMarketCode = null;
            Market market = marketFinder.getMarketByCode(MarketCode.BLOOMBERG, subMarketCode);
            if (market == null) {
                throw new BLPException("No market found for code [" + MarketCode.BLOOMBERG + "]");
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
                throw new BLPException("Unsupported mdEntryType [" + mdEntryType + "]");
            }

            Date date = null;
            Date time = null;
            BigDecimal qty = null;
            BigDecimal amount = null;
            Proposal.PriceType priceType = Proposal.PriceType.PRICE;

            if (tsMarketDataSnapshotFullRefresh.getTSMDFullGrp() != null && tsMarketDataSnapshotFullRefresh.getTSMDFullGrp().getTSNoMDEntriesList() != null) {

                List<TSMDFullGrp.TSNoMDEntries> tsNoMDEntriesList = tsMarketDataSnapshotFullRefresh.getTSMDFullGrp().getTSNoMDEntriesList();

                // Set defaultDate as today
                for (TSMDFullGrp.TSNoMDEntries tsNoMDEntries : tsNoMDEntriesList) {

                    if (tsNoMDEntries.getMDEntryType() == mdEntryType) {

                        if (tsNoMDEntries.getMDEntryTime() != null) {
                            time = tsNoMDEntries.getMDEntryTime();
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
                            amount = new BigDecimal("" + tsNoMDEntries.getMDEntryPx());
                        }
                        
                        if (tsNoMDEntries.getPriceType() != null) {
                           switch (tsNoMDEntries.getPriceType()) {
                              case Percentage:
                                 priceType = Proposal.PriceType.PRICE;
                                 break;
                              case Yield:
                                 priceType = Proposal.PriceType.YIELD;
                                 break;
                              case Spread:
                                 priceType = Proposal.PriceType.SPREAD;
                                 break;
                              default:
                                 LOGGER.debug("PriceType = {} not managed yet", tsNoMDEntries.getPriceType());
                                 break;
                           }
                        }
                    }
                }
            }
            // [DR20130924] Come concordato verbalmente con AMC, tutti i prezzi provenienti da Bloomberg sono e saranno sempre INDICATIVE
            ProposalType proposalType = ProposalType.INDICATIVE;
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
            classifiedProposal.setPriceType(priceType);

            Date today = DateService.newLocalDate();
            if (!DateUtils.isSameDay(timestamp, today)) {
                String todayStr = DateService.format(DateService.dateISO, today);
                String dateStr = DateService.format(DateService.dateISO, timestamp);

                LOGGER.info("The proposal " + classifiedProposal + ", has a date (" + dateStr + ") different from today (" + todayStr + "): set it as rejected. Original values: " + date + " - " + time);
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
            throw new BLPException("Error retrieving classifiedProposal for [" + tsMarketDataSnapshotFullRefresh + "]: " + e.getMessage(), e);
        }

        return classifiedProposal;
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
    protected ClassifiedProposal getClassifiedProposalReject(MDEntryType mdEntryType, Instrument instrument, String marketMakerCode, String reason) throws BLPException {
        LOGGER.debug("mdEntryType = {}, instrument = {}, marketMakerCode = {}, reason = {}", mdEntryType, instrument, marketMakerCode, reason);
        
        if (mdEntryType == null || marketMakerCode == null || instrument == null || reason == null) {
            throw new IllegalArgumentException(PARAMS_CAN_NOT_BE_NULL);
        }

        if (mdEntryType != MDEntryType.Bid && mdEntryType != MDEntryType.Offer) {
            throw new IllegalArgumentException("Unsupported mdEntryType [" + mdEntryType + "]! Only Bid or Offer value is accepted");
        }
        ClassifiedProposal classifiedProposal = null;

        try {

            MarketMarketMaker marketMarketMaker = marketMakerFinder.getMarketMarketMakerByCode(MarketCode.BLOOMBERG, marketMakerCode);
            if (marketMarketMaker == null) {
                throw new BLPException("No marketMarketMaker found for code [" + marketMakerCode + "]");
            }

            Venue venue = venueFinder.getMarketMakerVenue(marketMarketMaker.getMarketMaker());
            if (venue == null) {
                throw new BLPException("No venue found for code [" + marketMakerCode + "]");
            }
            if (venue.getMarket() == null) {
                throw new BLPException("market is null for the venue retrieved with code [" + marketMakerCode + "]");
            }

            SubMarketCode subMarketCode = null;
            Market market = marketFinder.getMarketByCode(MarketCode.BLOOMBERG, subMarketCode);
            if (market == null) {
                throw new BLPException("No market found for code [" + MarketCode.BLOOMBERG + "]");
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

            classifiedProposal.setType(ProposalType.CLOSED);
            classifiedProposal.setProposalState(ProposalState.REJECTED);
            classifiedProposal.setProposalSubState(ProposalSubState.REJECTED_BY_MARKET);
            classifiedProposal.setReason(reason);

            Venue bloombergVenue = new Venue(venue);
            bloombergVenue.setMarket(market);
            classifiedProposal.setMarket(market);

            classifiedProposal.setMarketMarketMaker(marketMarketMaker);
            classifiedProposal.setVenue(bloombergVenue);
            classifiedProposal.setQty(BigDecimal.ZERO);

            Money price = new Money(instrument.getCurrency(), BigDecimal.ZERO);
            classifiedProposal.setPrice(price);
            classifiedProposal.setSide(proposalSide);

        } catch (Exception e) {
            String proposal = instrument.getIsin() + "-" + marketMakerCode + "- " + mdEntryType.name();

            throw new BLPException("Error retrieving classifiedProposal for [" + proposal + "]: " + e.getMessage(), e);
        }

        return classifiedProposal;
    }

    protected String getMarketMaker(TSInstrument tsInstrument) {
        if (tsInstrument == null) {
            throw new IllegalArgumentException("tsInstrument can't be null");
        }

        String res = null;

        TSInstrumentParties tsInstrumentParties = tsInstrument.getTSInstrumentParties();

        if (tsInstrumentParties != null && tsInstrumentParties.getTSNoInstrumentPartiesList() != null) {

            List<TSNoInstrumentParties> tsNoInstrumentPartiesList = tsInstrumentParties.getTSNoInstrumentPartiesList();

            for (TSNoInstrumentParties tsNoInstrumentParties : tsNoInstrumentPartiesList) {

                if (tsNoInstrumentParties.getInstrumentPartyIDSource() == InstrumentPartyIDSource.GenerallyAcceptedMarketPartecipantIdentifier
                                && tsNoInstrumentParties.getInstrumentPartyRole() == InstrumentPartyRole.MarketMaker) {
                    res = tsNoInstrumentParties.getInstrumentPartyID();
                    break;
                }
            }

        }

        return res;
    }

    /**
     * Given a {@code BloombergProposalInputLazyBean} and an {@code Instrument}, creates a {@code ClassifiedProposal}
     * 
     * @param bloombergProposalInputLazyBean
     *            the lazyBean used to create the classifiedProposal
     * @param instrument
     *            the instrument used to create the classifiedProposal
     * @return a ClassifiedProposal
     * @throws BLPException
     *             if the lazyBean does not contains valid informations related to MarketMarketMaker, Venue or Market, and when a generic error occurs
     */
    protected ClassifiedProposal getClassifiedProposal(BloombergProposalInputLazyBean bloombergProposalInputLazyBean, Instrument instrument) throws BLPException {
        LOGGER.trace("proposal = {}, instrument = {}", bloombergProposalInputLazyBean, instrument);

        if (bloombergProposalInputLazyBean == null || instrument == null) {
            throw new IllegalArgumentException(PARAMS_CAN_NOT_BE_NULL);
        }

        ClassifiedProposal classifiedProposal = null;

        try {
            String code = bloombergProposalInputLazyBean.getBloombergMarketMaker();
            MarketMarketMaker marketMarketMaker = marketMakerFinder.getMarketMarketMakerByCode(MarketCode.BLOOMBERG, code);
            if (marketMarketMaker == null) {
                throw new BLPException("No marketMarketMaker found for code [" + code + "]");
            }

            Venue venue = venueFinder.getMarketMakerVenue(marketMarketMaker.getMarketMaker());
            if (venue == null) {
                throw new BLPException("No venue found for code [" + code + "]");
            }
            if (venue.getMarket() == null) {
                throw new BLPException("market is null for the venue retrieved with code [" + code + "]");
            }

            SubMarketCode subMarketCode = null;
            Market market = marketFinder.getMarketByCode(MarketCode.BLOOMBERG, subMarketCode);
            if (market == null) {
                throw new BLPException("No market found for code [" + MarketCode.BLOOMBERG + "]");
            }

            // --- Fill the proposal ---

            Venue bloombergVenue = new Venue(venue);
            bloombergVenue.setMarket(market);

            classifiedProposal = new ClassifiedProposal();
            classifiedProposal.setMarket(market);
            classifiedProposal.setMarketMarketMaker(marketMarketMaker);
            classifiedProposal.setVenue(bloombergVenue);

            classifiedProposal.setProposalState(ProposalState.NEW);
            classifiedProposal.setType(ProposalType.INDICATIVE);
            classifiedProposal.setSide(bloombergProposalInputLazyBean.getProposalSide());
            classifiedProposal.setTimestamp(bloombergProposalInputLazyBean.getTimeStamp());

            Money price = new Money(instrument.getCurrency(), bloombergProposalInputLazyBean.getPrice());
            classifiedProposal.setPrice(price);
            classifiedProposal.setQty(bloombergProposalInputLazyBean.getQty());
            classifiedProposal.setFutSettDate(instrument.getBBSettlementDate());
            classifiedProposal.setNonStandardSettlementDateAllowed(false);
            classifiedProposal.setNativeMarketMarketMaker(marketMarketMaker);

            String todayStr = DateService.format(DateService.dateISO, DateService.newLocalDate());
            String proposalDateStr = "" + bloombergProposalInputLazyBean.getBloombergDate();

            if (todayStr.compareTo(proposalDateStr) > 0) {
                LOGGER.info("The proposal " + classifiedProposal + ", has a date (" + proposalDateStr + ") previous than today (" + todayStr + "): set it as rejected.");
                classifiedProposal.setProposalState(ProposalState.REJECTED);
                classifiedProposal.setReason(Messages.getString("RejectProposalPriceTooOld", "" + proposalDateStr));
            }

            // [DR20120507] Nel BloombergMarket, l'arrivo di un errorCode non interrompe la gestione della proposal e non imposta alcun
            // ProposalState.
            if (bloombergProposalInputLazyBean.getErrorCode() != 0) {
                LOGGER.info("Proposal received with an error: [{}] {}.", bloombergProposalInputLazyBean.getErrorCode(), bloombergProposalInputLazyBean.getErrorMsg());

                // [DR20120507] Sarebbe più opportuno in questo punto settare ClassifiedProposal ProposalState.REJECTED e Reason =
                // [errorCode] errorMessage
                // classifiedProposal.setProposalState(ProposalState.REJECTED);
                // classifiedProposal.setReason(Messages.getString("RejectProposalPriceTooOld",
                // bloombergProposalInputLazyBean.getErrorMsg()));
            }

        } catch (BestXException e) {
            throw new BLPException("Error retrieving classifiedProposal for [" + bloombergProposalInputLazyBean + "]: " + e.getMessage(), e);

        }

        return classifiedProposal;
    }

    /**
     * Creates a Network (Counterparty System) Status Request
     * 
     * @param networkRequestID
     *            Unique identifier for a network resquest.
     * @param refCompId
     *            Used to restrict updates/request to specific CompID
     * @return a Network (Counterparty System) Status Request
     */
    public TSNetworkCounterpartySystemStatusRequest createNetworkCounterpartySystemStatusRequest(String networkRequestID, String refCompId) {
        LOGGER.trace("{}, {}", networkRequestID, refCompId);

        if (networkRequestID == null || refCompId == null) {
            throw new IllegalArgumentException(PARAMS_CAN_NOT_BE_NULL);
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

        String mdReqID = "" + DateService.currentTimeMillis();

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
}
