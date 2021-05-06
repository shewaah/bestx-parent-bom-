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
package it.softsolutions.bestx.connections.tradeweb.mds;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.MarketPriceDiscoveryHelper;
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
import it.softsolutions.bestx.model.Proposal.PriceType;
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
 * This class is mainly used for two purposes. It manages the informations about
 * {@link LiteMarketDataRequest} using a private concurrent Hash Map. This class
 * is also useful while converting the MarketDataSnapshotFullRefresh into
 * ClassifiedProposal.
 * 
 * @author Davide Rossoni
 */
/*
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-tradeweb-market First created by: davide.rossoni
 * Creation date: 19/dec/2014
 */
public class MDSHelper extends MarketPriceDiscoveryHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MDSHelper.class);

    /**
     * Creates a new MDSHelper, initializing all the indispensable parameters
     * for MDSConnection.
     * 
     * @param instrumentFinder
     *            the instrument finder
     * @param marketMakerFinder
     *            the marketMaker finder
     * @param venueFinder
     *            the venue finder
     * @param marketFinder
     *            the market finder
     * @throws BestXException when the object cannot be built
     */
    public MDSHelper(InstrumentFinder instrumentFinder, MarketMakerFinder marketMakerFinder, VenueFinder venueFinder, MarketFinder marketFinder) throws BestXException {
        super(instrumentFinder, marketMakerFinder, venueFinder, marketFinder);
    }

    /**
     * Method is protected in order to permit single unitTesting on it
     * 
     * @param isin
     *            the isinCode
     * @return the instrument with the specified isinCode
     * @throws MDSException
     *             if the passed param is null or no instrument found
     */
    protected Instrument getInstrument(String isin) throws MDSException {
        LOGGER.trace("{}", isin);

        if (isin == null) {
            throw new IllegalArgumentException("isin can't be null");
        }

        Instrument instrument = null;

        instrument = instrumentFinder.getInstrumentByIsin(isin);

        if (instrument == null) {
            throw new MDSException("No Instrument found with ISIN [" + isin + "]");
        }

        if (instrument.getCurrency() == null) {
            throw new MDSException("No default currency found for instrument with ISIN [" + isin + "]");
        }

        if (instrument.getBBSettlementDate() == null) {
            throw new MDSException("No default BBSettlementDate found for instrument with ISIN [" + isin + "]");
        }

        return instrument;
    }

    /**
     * Creates a classifiedProposal starting from a
     * MarketDataSnapshotFullRefresh
     * 
     * @param mdEntryType
     *            Bid or Offer accepted
     * @param tsMarketDataSnapshotFullRefresh
     *            the MarketDataSnapshotFullRefresh
     * @param instrument
     *            the instrument
     * @return a classifiedProposal
     * @throws MDSException
     *             if an error occured during the creation of the
     *             classifiedProposal
     */
    protected ClassifiedProposal getClassifiedProposal(MDEntryType mdEntryType, TSMarketDataSnapshotFullRefresh tsMarketDataSnapshotFullRefresh, Instrument instrument) throws MDSException {
        LOGGER.trace("mdEntryType = {}, tsMarketDataSnapshotFullRefresh = {}, instrument = {}", mdEntryType, tsMarketDataSnapshotFullRefresh, instrument);

        if (mdEntryType == null || tsMarketDataSnapshotFullRefresh == null || instrument == null) {
            throw new IllegalArgumentException(PARAMS_CAN_NOT_BE_NULL);
        }

        ClassifiedProposal classifiedProposal = null;

        try {
            String code = tsMarketDataSnapshotFullRefresh.getTSMDFullGrp().getTSNoMDEntriesList().get(0).getTsParties().getTSNoPartyIDsList().get(0).getPartyID();

            MarketMarketMaker marketMarketMaker = marketMakerFinder.getMarketMarketMakerByCode(MarketCode.TW, code);
            if (marketMarketMaker == null) {
                throw new MDSException("No marketMarketMaker found for code [" + code + "]");
            }

            Venue venue = venueFinder.getMarketMakerVenue(marketMarketMaker.getMarketMaker());
            if (venue == null) {
                throw new MDSException("No venue found for code [" + code + "]");
            }
            if (venue.getMarket() == null) {
                throw new MDSException("market is null for the venue retrieved with code [" + code + "]");
            }

            SubMarketCode subMarketCode = null;
            Market market = marketFinder.getMarketByCode(MarketCode.TW, subMarketCode);
            if (market == null) {
                throw new MDSException("No market found for code [" + MarketCode.TW + "]");
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
                throw new MDSException("Unsupported mdEntryType [" + mdEntryType + "]");
            }

            Date date = null;
            Date time = null;
            BigDecimal qty = null;
            BigDecimal amount = null;
            PriceType priceType = null;

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
                            // [DR20120515] do not use directly the Double but
                            // convert to String in order to prevent this issue:
                            // "price = 131.66300000000001091393642127513885498046875"
                            qty = new BigDecimal(Double.toString(tsNoMDEntries.getMDEntrySize()));
                        }

                        if (tsNoMDEntries.getMDEntryPx() != null) {
                            amount = new BigDecimal(Double.toString(tsNoMDEntries.getMDEntryPx()));
                        }
                        
                        if (tsNoMDEntries.getPriceType() != null) {
                        	switch (tsNoMDEntries.getPriceType()) {
         							case Percentage:
         								priceType = PriceType.PRICE;
         								break;
         							case Yield:
         								priceType = PriceType.YIELD;
         								break;
         							case Spread:
         								priceType = PriceType.SPREAD;
         								break;
         							case PerUnit:
         								priceType = PriceType.UNIT;
         								break;
         							default:
         								LOGGER.debug("PriceType = {} not managed yet", tsNoMDEntries.getPriceType());
         								break;
      							}
                        	
                        }
                    }
                }
            }
            // [DR20130924] Come concordato verbalmente con AMC, tutti i prezzi
            // provenienti da Bloomberg sono e saranno sempre INDICATIVE
            ProposalType proposalType = ProposalType.INDICATIVE;
//            String reason = null;

            // [FA20120518] Workaround the market maker might not have prices
            // (one or both side)
            if (amount == null) {
                amount = BigDecimal.ZERO;
                proposalType = ProposalType.INDICATIVE;
                // reason = "No price available";

                LOGGER.warn("The market maker {} might not have prices for {}", code, instrument.getIsin());
            }

            // [DR20120515] Workaround due to missing quantities passed by
            // Bloomberg
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
            classifiedProposal.setPriceType(priceType);
            classifiedProposal.setFutSettDate(instrument.getBBSettlementDate());
            classifiedProposal.setNonStandardSettlementDateAllowed(false);
            classifiedProposal.setNativeMarketMarketMaker(marketMarketMaker);

            Date timestamp = DateService.convertUTCToLocal(date, time);
            classifiedProposal.setTimestamp(timestamp);

            Money price = new Money(instrument.getCurrency(), amount);
            classifiedProposal.setPrice(price);

            /* SP20200805 - BESTX-723 - Removed in order to use only DiscardOldProposalClassifier 
            Date today = DateService.newLocalDate();
            if (!DateUtils.isSameDay(timestamp, today)) {
                String todayStr = DateService.format(DateService.dateISO,today);
                String dateStr = DateService.format(DateService.dateISO, timestamp);

                LOGGER.info("The proposal " + classifiedProposal + ", has a date (" + dateStr + ") different from today (" + todayStr + "): set it as rejected. Original values: " + date + " - " + time);
                classifiedProposal.setProposalState(ProposalState.REJECTED);
                reason = Messages.getString("RejectProposalPriceTooOld", "" + dateStr);
            }
            if (reason != null) {
                classifiedProposal.setReason(reason);
            }*/

            LOGGER.info("{} {} {} {} {} {} {} {} {}", instrument.getIsin(), market.getMarketCode(), marketMarketMaker.getMarketSpecificCode(), classifiedProposal.getProposalState().name(), proposalType.name(), mdEntryType, amount, qty, null);
        } catch (Exception e) {
            throw new MDSException("Error retrieving classifiedProposal for [" + tsMarketDataSnapshotFullRefresh + "]: " + e.getMessage(), e);
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
     * @throws MDSException
     *             if an error occured during the creation of the
     *             classifiedProposal
     */
    protected ClassifiedProposal getClassifiedProposalReject(MDEntryType mdEntryType, Instrument instrument, String marketMakerCode, String reason) throws MDSException {
        LOGGER.debug("mdEntryType = {}, instrument = {}, marketMakerCode = {}, reason = {}", mdEntryType, instrument, marketMakerCode, reason);

        if (mdEntryType == null || marketMakerCode == null || instrument == null || reason == null) {
            throw new IllegalArgumentException(PARAMS_CAN_NOT_BE_NULL);
        }

        if (mdEntryType != MDEntryType.Bid && mdEntryType != MDEntryType.Offer) {
            throw new IllegalArgumentException("Unsupported mdEntryType [" + mdEntryType + "]! Only Bid or Offer value is accepted");
        }
        ClassifiedProposal classifiedProposal = null;

        try {

            MarketMarketMaker marketMarketMaker = marketMakerFinder.getMarketMarketMakerByCode(MarketCode.TW, marketMakerCode);
            if (marketMarketMaker == null) {
                throw new MDSException("No marketMarketMaker found for code [" + marketMakerCode + "]");
            }

            Venue venue = venueFinder.getMarketMakerVenue(marketMarketMaker.getMarketMaker());
            if (venue == null) {
                throw new MDSException("No venue found for code [" + marketMakerCode + "]");
            }
            if (venue.getMarket() == null) {
                throw new MDSException("market is null for the venue retrieved with code [" + marketMakerCode + "]");
            }

            SubMarketCode subMarketCode = null;
            Market market = marketFinder.getMarketByCode(MarketCode.TW, subMarketCode);
            if (market == null) {
                throw new MDSException("No market found for code [" + MarketCode.TW + "]");
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

            throw new MDSException("Error retrieving classifiedProposal for [" + proposal + "]: " + e.getMessage(), e);
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
     *            the set of "marketMakerCodes" requested for the specified
     *            instrument
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
}
