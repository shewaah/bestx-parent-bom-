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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields;
import it.softsolutions.bestx.connections.bloomberg.BloombergProposalInputLazyBean;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
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
import it.softsolutions.tradestac.fix50.component.TSInstrument;
import it.softsolutions.tradestac.fix50.component.TSInstrumentParties;
import it.softsolutions.tradestac.fix50.component.TSInstrumentParties.TSNoInstrumentParties;
import it.softsolutions.tradestac.fix50.component.TSMDFullGrp;
import it.softsolutions.tradestac.fix50.component.TSMDFullGrp.TSNoMDEntries;
import it.softsolutions.xt2.protocol.XT2Msg;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-bloomberg-market First created by: davide.rossoni Creation date: May 15, 2012
 * 
 **/
public class BLPHelperTest {

    private static final String ISIN_CODE = "DE0001134922";// "XS0176823424";
    private static final String MARKET_MAKER = "RABX";
    private static final ProposalSide PROPOSAL_SIDE = ProposalSide.ASK;
    private static final double PRICE = 101.25;
    private static final double QTY = 50000.0;
    private static final long TIMESTAMP = 103012;
    private static final long DATE = DateService.formatAsLong(DateService.dateISO, DateService.newLocalDate()) + 3;
    private static final long OLD_DATE = DATE - 6;
    private static final long NY_TIMEZONE = 21600000;
    private static final String REF_COMP_ID = MarketCode.BLOOMBERG.toString();
    private static final String NETWORK_REQUEST_ID = "NetworkRequestID";
    private static final String MD_REQ_ID = "1337356986370";
    private static final SecurityIDSource SECURITY_ID_SOURCE = SecurityIDSource.IsinNumber;
    private static final InstrumentPartyIDSource INSTRUMENT_PARTY_ID_SOURCE = InstrumentPartyIDSource.GenerallyAcceptedMarketPartecipantIdentifier;
    private static final InstrumentPartyRole INSTRUMENT_PARTY_ROLE = InstrumentPartyRole.MarketMaker;
    private static final BigDecimal ZERO_AMOUNT = new BigDecimal(0.0);
    private static final BigDecimal ZERO_QUANTITY = new BigDecimal(0.0);
    private static final Double MD_ENTRY_PX = new Double(125.269);
    private static final Double MD_ENTRY_SIZE = new Double(100.5);
    private static final Date MD_ENTRY_DATE = new Date();
    private static final Date MD_ENTRY_TIME = new Date();
    private static final String REASON = "Reject Reason";

    private static BLPHelper blpHelper;

    @BeforeClass
    public static void setUp() throws Exception {
        // Initialize finders using Spring
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("cs-spring.xml");

        InstrumentFinder instrumentFinder = (InstrumentFinder) context.getBean("dbBasedInstrumentFinder");
        MarketMakerFinder marketMakerFinder = (MarketMakerFinder) context.getBean("dbBasedMarketMakerFinder");
        VenueFinder venueFinder = (VenueFinder) context.getBean("dbBasedVenueFinder");
        MarketFinder marketFinder = (MarketFinder) context.getBean("dbBasedMarketFinder");

        blpHelper = new BLPHelper(instrumentFinder, marketMakerFinder, venueFinder, marketFinder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getInstrumentNull() throws BLPException {
        blpHelper.getInstrument(null);
    }

    @Test(expected = BLPException.class)
    public void getInstrumentInvalid() throws BLPException {
        blpHelper.getInstrument("#INVALID#");
    }

    @Test
    public void getInstrumentValid() throws BLPException {
        Instrument instrument = blpHelper.getInstrument(ISIN_CODE);
        assertNotNull(instrument);
        assertEquals(ISIN_CODE, instrument.getIsin());
        assertEquals("EUR", instrument.getCurrency());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getClassifiedProposalNull() throws BLPException {
        blpHelper.getClassifiedProposal(null, null);
    }

    @Test(expected = BLPException.class)
    public void getClassifiedProposalInvalid() throws BLPException {
        String marketMaker = "#INVALID#";
        Instrument instrument = blpHelper.getInstrument(ISIN_CODE);

        XT2Msg xt2Msg = new XT2Msg();
        xt2Msg.setSubject(ISIN_CODE + '@' + marketMaker);

        BloombergProposalInputLazyBean bloombergProposalInputLazyBean = new BloombergProposalInputLazyBean(xt2Msg, null, NY_TIMEZONE);

        blpHelper.getClassifiedProposal(bloombergProposalInputLazyBean, instrument);
    }

    @Test
    public void getClassifiedProposalValid() throws Exception {
        Instrument instrument = blpHelper.getInstrument(ISIN_CODE);

        XT2Msg xt2Msg = new XT2Msg();
        xt2Msg.setSubject(ISIN_CODE + '@' + MARKET_MAKER);
        xt2Msg.setValue(BloombergMessageFields.BLOOM_ASK_PRICE, PRICE);
        xt2Msg.setValue(BloombergMessageFields.BLOOM_ASK_QTY, QTY);
        xt2Msg.setValue(BloombergMessageFields.BLOOM_TIME, TIMESTAMP);
        xt2Msg.setValue(BloombergMessageFields.BLOOM_DATE, DATE);

        BloombergProposalInputLazyBean bloombergProposalInputLazyBean = new BloombergProposalInputLazyBean(xt2Msg, PROPOSAL_SIDE, NY_TIMEZONE);

        ClassifiedProposal classifiedProposal = blpHelper.getClassifiedProposal(bloombergProposalInputLazyBean, instrument);
        assertNotNull(classifiedProposal);
        assertNotNull(classifiedProposal.getMarket());
        assertNotNull(classifiedProposal.getMarketMarketMaker());
        assertNotNull(classifiedProposal.getVenue());

        assertEquals(ProposalState.NEW, classifiedProposal.getProposalState());
        assertEquals(ProposalType.TRADEABLE, classifiedProposal.getType());
        assertEquals(PROPOSAL_SIDE, classifiedProposal.getSide());
        // assertEquals(TIMESTAMP, classifiedProposal.getTimestamp());
        assertEquals(new Money(instrument.getCurrency(), new BigDecimal(PRICE)), classifiedProposal.getPrice());
        assertEquals(QTY, classifiedProposal.getQty().doubleValue(), 0);
        assertTrue(classifiedProposal.isNonStandardSettlementDateAllowed());
        assertEquals(classifiedProposal.getMarketMarketMaker(), classifiedProposal.getNativeMarketMarketMaker());
    }

    @Test
    public void getClassifiedProposalValidRejected() throws Exception {
        Instrument instrument = blpHelper.getInstrument(ISIN_CODE);

        XT2Msg xt2Msg = new XT2Msg();
        xt2Msg.setSubject(ISIN_CODE + '@' + MARKET_MAKER);
        xt2Msg.setValue(BloombergMessageFields.BLOOM_ASK_PRICE, PRICE);
        xt2Msg.setValue(BloombergMessageFields.BLOOM_ASK_QTY, QTY);
        xt2Msg.setValue(BloombergMessageFields.BLOOM_TIME, TIMESTAMP);

        // [DR20120507] set an old Date in order to reject the proposal
        xt2Msg.setValue(BloombergMessageFields.BLOOM_DATE, OLD_DATE);

        BloombergProposalInputLazyBean bloombergProposalInputLazyBean = new BloombergProposalInputLazyBean(xt2Msg, PROPOSAL_SIDE, NY_TIMEZONE);

        ClassifiedProposal classifiedProposal = blpHelper.getClassifiedProposal(bloombergProposalInputLazyBean, instrument);
        assertNotNull(classifiedProposal);
        assertEquals(ProposalState.REJECTED, classifiedProposal.getProposalState());
    }

    @Test
    public void createNetworkSystemStatusRequest() {

        TSNetworkCounterpartySystemStatusRequest tsSystemStatusRequest = blpHelper.createNetworkCounterpartySystemStatusRequest(NETWORK_REQUEST_ID, REF_COMP_ID);

        assertEquals(NETWORK_REQUEST_ID, tsSystemStatusRequest.getNetworkRequestID());
        assertEquals(NetworkRequestType.Subscribe, tsSystemStatusRequest.getNetworkRequestType());
        assertNotNull(tsSystemStatusRequest.getTSCompIDReqGrp());
        assertNotNull(tsSystemStatusRequest.getTSCompIDReqGrp().getTSNoCompIDsList());
        assertTrue(tsSystemStatusRequest.getTSCompIDReqGrp().getTSNoCompIDsList().size() == 1);
        assertNotNull(tsSystemStatusRequest.getTSCompIDReqGrp().getTSNoCompIDsList().get(0));
        assertEquals(REF_COMP_ID, tsSystemStatusRequest.getTSCompIDReqGrp().getTSNoCompIDsList().get(0).getRefCompID());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createNetworkSystemStatusRequestWithNetworkRequestIdNull() {

        blpHelper.createNetworkCounterpartySystemStatusRequest(null, REF_COMP_ID);

    }

    @Test(expected = IllegalArgumentException.class)
    public void createNetworkSystemStatusRequestWithRefCompIDNull() {

        blpHelper.createNetworkCounterpartySystemStatusRequest(NETWORK_REQUEST_ID, null);

    }

    @Test
    public void createMarketDataRequest() throws BLPException, BestXException {
        Instrument instrument = blpHelper.getInstrument(ISIN_CODE);
        List<String> marketMakerCodes = new ArrayList<String>();
        marketMakerCodes.add(MARKET_MAKER);

        TSMarketDataRequest tsmarketDataRequest = blpHelper.createMarketDataRequest(instrument, marketMakerCodes);

        assertFalse(tsmarketDataRequest.getAggregatedBook());
        assertTrue(tsmarketDataRequest.getMarketDepth() == 1);
        assertNotNull(tsmarketDataRequest.getMDReqID());
        assertEquals(MDUpdateType.FullRefresh, tsmarketDataRequest.getMDUpdateType());
        assertEquals(SubscriptionRequestType.Snapshot, tsmarketDataRequest.getSubscriptionRequestType());
        assertNotNull(tsmarketDataRequest.getTSInstrmtMDReqGrp());
        assertNotNull(tsmarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList());
        assertTrue(tsmarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().size() == 1);
        assertNotNull(tsmarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0));
        assertNotNull(tsmarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument());
        assertEquals(ISIN_CODE, tsmarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getSecurityID());
        assertEquals(SecurityIDSource.IsinNumber, tsmarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getSecurityIDSource());

        TSInstrumentParties tsInstrumentParties = tsmarketDataRequest.getTSInstrmtMDReqGrp().getTSNoRelatedSymList().get(0).getTSInstrument().getTSInstrumentParties();
        assertNotNull(tsInstrumentParties);
        assertNotNull(tsInstrumentParties.getTSNoInstrumentPartiesList());
        assertTrue(tsInstrumentParties.getTSNoInstrumentPartiesList().size() == 1);
        assertNotNull(tsInstrumentParties.getTSNoInstrumentPartiesList().get(0));
        assertEquals(MARKET_MAKER, tsInstrumentParties.getTSNoInstrumentPartiesList().get(0).getInstrumentPartyID());
        assertEquals(INSTRUMENT_PARTY_ID_SOURCE, tsInstrumentParties.getTSNoInstrumentPartiesList().get(0).getInstrumentPartyIDSource());
        assertEquals(INSTRUMENT_PARTY_ROLE, tsInstrumentParties.getTSNoInstrumentPartiesList().get(0).getInstrumentPartyRole());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createMarketDataRequestWithInstrumentNull() throws BestXException {
        List<String> marketMakerCodes = new ArrayList<String>();
        marketMakerCodes.add(MARKET_MAKER);

        blpHelper.createMarketDataRequest(null, marketMakerCodes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createMarketDataRequestWithMarketMakeCodesNull() throws BestXException, BLPException {
        Instrument instrument = blpHelper.getInstrument(ISIN_CODE);

        blpHelper.createMarketDataRequest(instrument, null);
    }

    @Test
    public void getTSClassifiedProposalValid() throws BLPException {
        TSMarketDataSnapshotFullRefresh tsMarketDataSnapshotFullRefresh = new TSMarketDataSnapshotFullRefresh();
        TSInstrument tsInstrument = new TSInstrument();
        // Instrument
        tsInstrument.setSecurityID(ISIN_CODE);
        tsInstrument.setSecurityIDSource(SECURITY_ID_SOURCE);
        Instrument instrument = blpHelper.getInstrument(ISIN_CODE);

        // InstrumentParties
        TSInstrumentParties tsInstrumentParties = new TSInstrumentParties();
        List<TSNoInstrumentParties> tsNoInstrumentPartiesList = new ArrayList<TSInstrumentParties.TSNoInstrumentParties>();
        TSInstrumentParties.TSNoInstrumentParties party = new TSInstrumentParties.TSNoInstrumentParties();
        party.setInstrumentPartyID(MARKET_MAKER);
        party.setInstrumentPartyIDSource(INSTRUMENT_PARTY_ID_SOURCE);
        party.setInstrumentPartyRole(INSTRUMENT_PARTY_ROLE);
        tsNoInstrumentPartiesList.add(party);
        tsInstrumentParties.setTSNoPartyIDsList(tsNoInstrumentPartiesList);
        tsInstrument.setTSInstrumentParties(tsInstrumentParties);

        tsMarketDataSnapshotFullRefresh.setMDReqID(MD_REQ_ID);
        tsMarketDataSnapshotFullRefresh.setMDReportID(null);
        tsMarketDataSnapshotFullRefresh.setTSInstrument(tsInstrument);

        // TSMDFullGrp
        TSMDFullGrp tsMDFullGrp = new TSMDFullGrp();
        List<TSNoMDEntries> tsNoMDEntriesList = new ArrayList<TSMDFullGrp.TSNoMDEntries>();
        TSNoMDEntries tsMDEntryBid = new TSNoMDEntries();
        tsMDEntryBid.setMDEntryType(MDEntryType.Bid);
        tsMDEntryBid.setMDPriceLevel(1);
        tsMDEntryBid.setMDEntryDate(MD_ENTRY_DATE);
        tsMDEntryBid.setMDEntryPx(MD_ENTRY_PX);
        tsMDEntryBid.setMDEntrySize(MD_ENTRY_SIZE);
        tsMDEntryBid.setMDEntryTime(MD_ENTRY_TIME);
        tsNoMDEntriesList.add(tsMDEntryBid);
        TSNoMDEntries tsMDEntryOffer = new TSNoMDEntries();
        tsMDEntryOffer.setMDEntryType(MDEntryType.Offer);
        tsMDEntryOffer.setMDPriceLevel(1);
        tsMDEntryOffer.setMDEntryDate(MD_ENTRY_DATE);
        tsMDEntryOffer.setMDEntryPx(MD_ENTRY_PX);
        tsMDEntryOffer.setMDEntrySize(MD_ENTRY_SIZE);
        tsMDEntryOffer.setMDEntryTime(MD_ENTRY_TIME);
        tsNoMDEntriesList.add(tsMDEntryOffer);
        tsMDFullGrp.setTSNoMDEntriesList(tsNoMDEntriesList);
        tsMarketDataSnapshotFullRefresh.setTSMDFullGrp(tsMDFullGrp);

        // BigDecimal bdTest = new BigDecimal().setScale(3);
        ClassifiedProposal bid = blpHelper.getClassifiedProposal(MDEntryType.Bid, tsMarketDataSnapshotFullRefresh, instrument);
        assertNotNull(bid);
        assertNotNull(bid.getMarket());
        assertEquals(MarketCode.BLOOMBERG, bid.getMarket().getMarketCode());
        assertNotNull(bid.getMarketMarketMaker());
        assertEquals(MARKET_MAKER, bid.getMarketMarketMaker().getMarketSpecificCode());
        assertEquals(instrument.getBBSettlementDate(), bid.getFutSettDate());
        assertEquals(MARKET_MAKER, bid.getMarketMarketMaker().getMarketSpecificCode());
        assertEquals(new BigDecimal(MD_ENTRY_PX).setScale(3, RoundingMode.HALF_EVEN), bid.getPrice().getAmount());
        assertEquals(ProposalState.NEW, bid.getProposalState());
        assertEquals(new BigDecimal(MD_ENTRY_SIZE).setScale(3, RoundingMode.HALF_EVEN), bid.getQty().setScale(3, RoundingMode.HALF_EVEN));
        assertEquals(ProposalSide.BID, bid.getSide());
        assertEquals(ProposalType.TRADEABLE, bid.getType());
        assertNotNull(bid.getTimestamp());
        assertNotNull(bid.getVenue());

        ClassifiedProposal ask = blpHelper.getClassifiedProposal(MDEntryType.Offer, tsMarketDataSnapshotFullRefresh, instrument);
        assertNotNull(ask);
        assertNotNull(ask.getMarket());
        assertEquals(MarketCode.BLOOMBERG, ask.getMarket().getMarketCode());
        assertNotNull(ask.getMarketMarketMaker());
        assertEquals(MARKET_MAKER, ask.getMarketMarketMaker().getMarketSpecificCode());
        assertEquals(instrument.getBBSettlementDate(), ask.getFutSettDate());
        assertEquals(MARKET_MAKER, ask.getMarketMarketMaker().getMarketSpecificCode());
        assertEquals(new BigDecimal(MD_ENTRY_PX).setScale(3, RoundingMode.HALF_EVEN), ask.getPrice().getAmount());
        assertEquals(ProposalState.NEW, ask.getProposalState());
        assertEquals(new BigDecimal(MD_ENTRY_SIZE).setScale(3, RoundingMode.HALF_EVEN), ask.getQty().setScale(3, RoundingMode.HALF_EVEN));
        assertEquals(ProposalSide.ASK, ask.getSide());
        assertEquals(ProposalType.TRADEABLE, ask.getType());
        assertNotNull(ask.getTimestamp());
        assertNotNull(ask.getVenue());
    }

    @Test
    public void getTSClassifiedProposalNullPrice() throws BLPException {
        TSMarketDataSnapshotFullRefresh tsMarketDataSnapshotFullRefresh = new TSMarketDataSnapshotFullRefresh();
        TSInstrument tsInstrument = new TSInstrument();
        // Instrument
        tsInstrument.setSecurityID(ISIN_CODE);
        tsInstrument.setSecurityIDSource(SECURITY_ID_SOURCE);
        Instrument instrument = blpHelper.getInstrument(ISIN_CODE);

        // InstrumentParties
        TSInstrumentParties tsInstrumentParties = new TSInstrumentParties();
        List<TSNoInstrumentParties> tsNoInstrumentPartiesList = new ArrayList<TSInstrumentParties.TSNoInstrumentParties>();
        TSInstrumentParties.TSNoInstrumentParties party = new TSInstrumentParties.TSNoInstrumentParties();
        party.setInstrumentPartyID(MARKET_MAKER);
        party.setInstrumentPartyIDSource(INSTRUMENT_PARTY_ID_SOURCE);
        party.setInstrumentPartyRole(INSTRUMENT_PARTY_ROLE);
        tsNoInstrumentPartiesList.add(party);
        tsInstrumentParties.setTSNoPartyIDsList(tsNoInstrumentPartiesList);
        tsInstrument.setTSInstrumentParties(tsInstrumentParties);

        tsMarketDataSnapshotFullRefresh.setMDReqID(MD_REQ_ID);
        tsMarketDataSnapshotFullRefresh.setMDReportID(null);
        tsMarketDataSnapshotFullRefresh.setTSInstrument(tsInstrument);

        ClassifiedProposal bidClassifiedProposal = blpHelper.getClassifiedProposal(MDEntryType.Bid, tsMarketDataSnapshotFullRefresh, instrument);
        assertNotNull(bidClassifiedProposal);
        assertNotNull(bidClassifiedProposal.getMarket());
        assertEquals(MarketCode.BLOOMBERG, bidClassifiedProposal.getMarket().getMarketCode());
        assertNotNull(bidClassifiedProposal.getMarketMarketMaker());
        assertEquals(MARKET_MAKER, bidClassifiedProposal.getMarketMarketMaker().getMarketSpecificCode());
        assertEquals(bidClassifiedProposal.getFutSettDate(), instrument.getBBSettlementDate());
        assertEquals(MARKET_MAKER, bidClassifiedProposal.getMarketMarketMaker().getMarketSpecificCode());
        assertEquals(ZERO_AMOUNT, bidClassifiedProposal.getPrice().getAmount());
        assertEquals(ProposalState.NEW, bidClassifiedProposal.getProposalState());
        assertEquals(ZERO_QUANTITY, bidClassifiedProposal.getQty());
        assertEquals(ProposalSide.BID, bidClassifiedProposal.getSide());
        assertEquals(ProposalType.INDICATIVE, bidClassifiedProposal.getType());
        assertNotNull(bidClassifiedProposal.getTimestamp());
        assertNotNull(bidClassifiedProposal.getVenue());
    }

    @Test
    public void getTSClassifiedProposalReject() throws BLPException {
        Instrument instrument = blpHelper.getInstrument(ISIN_CODE);

        ClassifiedProposal bid = blpHelper.getClassifiedProposalReject(MDEntryType.Bid, instrument, MARKET_MAKER, REASON);
        assertNotNull(bid);
        assertEquals(REASON, bid.getReason());
        assertEquals(ProposalSide.BID, bid.getSide());
        assertEquals(ProposalType.CLOSED, bid.getType());
        assertEquals(ProposalState.REJECTED, bid.getProposalState());
        assertEquals(BigDecimal.ZERO, bid.getQty());
        assertEquals(BigDecimal.ZERO, bid.getPrice().getAmount());
        assertEquals(instrument.getCurrency(), bid.getPrice().getStringCurrency());
        assertEquals(MarketCode.BLOOMBERG, bid.getMarket().getMarketCode());
        assertEquals(MARKET_MAKER, bid.getMarketMarketMaker().getMarketSpecificCode());
        assertNotNull(bid.getVenue());

        ClassifiedProposal ask = blpHelper.getClassifiedProposalReject(MDEntryType.Offer, instrument, MARKET_MAKER, REASON);
        assertNotNull(ask);
        assertEquals(REASON, ask.getReason());
        assertEquals(ProposalSide.ASK, ask.getSide());
        assertEquals(ProposalType.CLOSED, ask.getType());
        assertEquals(ProposalState.REJECTED, ask.getProposalState());
        assertEquals(BigDecimal.ZERO, ask.getQty());
        assertEquals(BigDecimal.ZERO, ask.getPrice().getAmount());
        assertEquals(instrument.getCurrency(), ask.getPrice().getStringCurrency());
        assertEquals(MarketCode.BLOOMBERG, ask.getMarket().getMarketCode());
        assertEquals(MARKET_MAKER, ask.getMarketMarketMaker().getMarketSpecificCode());
        assertNotNull(ask.getVenue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getTSClassifiedProposalRejectMDEntryTypeNull() throws BLPException {
        Instrument instrument = blpHelper.getInstrument(ISIN_CODE);

        blpHelper.getClassifiedProposalReject(null, instrument, MARKET_MAKER, REASON);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getTSClassifiedProposalRejectMDEntryTypeInvalid() throws BLPException {
        Instrument instrument = blpHelper.getInstrument(ISIN_CODE);

        blpHelper.getClassifiedProposalReject(MDEntryType.Unknown, instrument, MARKET_MAKER, REASON);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getTSClassifiedProposalRejectInstrumentNull() throws BLPException {

        blpHelper.getClassifiedProposalReject(MDEntryType.Bid, null, MARKET_MAKER, REASON);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getTSClassifiedProposalRejectReasonNull() throws BLPException {
        Instrument instrument = blpHelper.getInstrument(ISIN_CODE);

        blpHelper.getClassifiedProposalReject(MDEntryType.Offer, instrument, MARKET_MAKER, null);
    }
}
