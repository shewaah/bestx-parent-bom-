package it.softsolutions.bestx.services.bondvision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.tradestac.fix.field.MDEntryType;
import it.softsolutions.tradestac.fix.field.PartyIDSource;
import it.softsolutions.tradestac.fix.field.PriceType;
import it.softsolutions.tradestac.fix.field.QuoteCondition;
import it.softsolutions.tradestac.fix.field.SecurityIDSource;
import it.softsolutions.tradestac.fix50.TSMarketDataSnapshotFullRefresh;
import it.softsolutions.tradestac.fix50.TSNoMDEntryTypes;
import it.softsolutions.tradestac.fix50.TSNoPartyID;
import it.softsolutions.tradestac.fix50.component.TSInstrument;
import it.softsolutions.tradestac.fix50.component.TSMDFullGrp;
import it.softsolutions.tradestac.fix50.component.TSParties;

@SuppressWarnings("deprecation")
public class BondvisionBookManagerTest {
	String rFCQID;
	BondvisionBookManager mgr;
	MarketMakerFinder marketMakerFinder;
	PartyIDSource bic = PartyIDSource.BIC;
	int attemptNo;
	boolean refreshCounters;

	@Before
	public void init() {
		mgr = BondvisionBookManager.getInstance();
		marketMakerFinder = new TestMarketMakerFinder();
		this.mgr.setMarketMakerFinder(marketMakerFinder);
		attemptNo = 1;
		refreshCounters = false;
		rFCQID = "RFCQID";
	}
	
	@After
	public void closeAll() {
		mgr.dismiss(rFCQID);		
	}
	
	@Test
	public final void testOpen() throws BestXException {
		boolean isOpen = false;
		mgr.open(rFCQID);
		isOpen = mgr.isOpen(rFCQID);
		assertTrue("RFCQ is not open", isOpen);

	}

	@Test
	public final void testClose() throws BestXException {
		boolean isOpen = false;
		mgr.open(rFCQID);
		mgr.close(rFCQID);
		isOpen = mgr.isOpen(rFCQID);
		assertFalse("RFCQ is still open", isOpen);
	}

	@Test
	public final void testDismiss() throws BestXException {
		mgr.open(rFCQID);
		mgr.close(rFCQID);
		mgr.dismiss(rFCQID);
		try {
		ClassifiedProposal cp = mgr.getBest(rFCQID);
		assertNull("RFCQ is still there", cp);
		} catch(NullPointerException e) {
			assert(true);
		}
	}

	@Test
	public final void testUpdateBook() throws BestXException {
		mgr.open(rFCQID);
		TSMarketDataSnapshotFullRefresh book = addBookToMgr();
		assertEquals(101.1,mgr.getBest(rFCQID).getPrice().getAmount().doubleValue(), 0.01);
		assertEquals(100.1,mgr.getLevel(rFCQID, 1).getPrice().getAmount().doubleValue(), 0.01);
		assertEquals(100.0,mgr.getLevel(rFCQID, 2).getPrice().getAmount().doubleValue(), 0.01);
		book.getTSMDFullGrp().getTSNoMDEntriesList().remove(1);
		mgr.updateBook(rFCQID, book, refreshCounters, attemptNo, Proposal.ProposalSide.BID);
		assertEquals(101.1,mgr.getBest(rFCQID).getPrice().getAmount().doubleValue(), 0.01);
		assertEquals(100.0,mgr.getLevel(rFCQID, 1).getPrice().getAmount().doubleValue(), 0.01);
	}

	@Test
	public final void testRejectQuote() throws BestXException{
		mgr.open(rFCQID);
		TSMarketDataSnapshotFullRefresh book = addBookToMgr();
		ClassifiedProposal quote = mgr.getLevel(rFCQID, 1).getClassifiedProposal().clone();
		mgr.rejectQuote(rFCQID, quote, "test on reject quote");
		assertEquals(ProposalState.REJECTED, mgr.getLevel(rFCQID, 1).getProposalState());
	}
	@Test
	public final void testUpdateQuote() throws BestXException {
		mgr.open(rFCQID);
		TSMarketDataSnapshotFullRefresh book = addBookToMgr();
		ClassifiedProposal quote = mgr.getLevel(rFCQID, 1).getClassifiedProposal().clone();
		quote.setPrice(new Money("EUR", new BigDecimal("99.99")));
		mgr.updateQuote(rFCQID, quote, "test on reject quote");
		assertEquals(99.9, mgr.getLevel(rFCQID, 1).getPrice().getAmount().doubleValue(), 0.01);
	}
	
	private TSMarketDataSnapshotFullRefresh createTSMarketDataSnapshotFullRefresh(String request, TSInstrument tsInstrument) {
		TSMarketDataSnapshotFullRefresh tsMarketDataRefresh = new TSMarketDataSnapshotFullRefresh();

		tsMarketDataRefresh.setMDReqID("MDRequest"+ request);
		tsMarketDataRefresh.setMDReportID(Integer.getInteger("" + System.currentTimeMillis()));
		tsMarketDataRefresh.setTSInstrument(tsInstrument);

		TSMDFullGrp tsMDFullGrp = new TSMDFullGrp();
		List<TSMDFullGrp.TSNoMDEntries> tsNoMDEntriesList = new ArrayList<TSMDFullGrp.TSNoMDEntries>();
		TSNoMDEntryTypes entryType = new TSNoMDEntryTypes(MDEntryType.Bid);

		TSMDFullGrp.TSNoMDEntries entry1 = new TSMDFullGrp.TSNoMDEntries();
		TSMDFullGrp.TSNoMDEntries entry2 = new TSMDFullGrp.TSNoMDEntries();
		TSMDFullGrp.TSNoMDEntries entry3 = new TSMDFullGrp.TSNoMDEntries();
		
		TSParties parties1 = new TSParties();
		TSParties parties2 = new TSParties();
		TSParties parties3 = new TSParties();
		
		ArrayList<TSNoPartyID> list1 = new ArrayList<TSNoPartyID>();
		TSNoPartyID party1 = new TSNoPartyID();
		party1.setPartyID("party1");
		party1.setPartyIDSource(bic);
		party1.setPartyRole(BondvisionBookManager.executingFirm);
		list1.add(party1);
		parties1.setTSNoPartyIDsList(list1);

		entry1.setMDEntryPx(100.0);
		entry1.setMDEntrySize(50000.0);
		entry1.setPriceType(PriceType.Percentage);
		entry1.setMDPriceLevel(1);
		entry1.setQuoteCondition(QuoteCondition.NonFirm);
		entry1.setMDEntryTime(DateService.newUTCDate());
		entry1.setMDEntryType(MDEntryType.Bid);
		entry1.setTsParties(parties1);
		tsNoMDEntriesList.add(entry1);

		ArrayList<TSNoPartyID> list2 = new ArrayList<TSNoPartyID>();
		TSNoPartyID party2 = new TSNoPartyID();
		party2.setPartyID("party2");
		party2.setPartyIDSource(bic);
		party2.setPartyRole(BondvisionBookManager.executingFirm);
		list2.add(party2);
		parties2.setTSNoPartyIDsList(list2);
		
		entry2.setMDEntryPx(100.1);
		entry2.setMDEntrySize(50000.0);
		entry2.setMDPriceLevel(2);
		entry2.setPriceType(PriceType.Percentage);
		entry2.setQuoteCondition(QuoteCondition.NonFirm);
		entry2.setMDEntryTime(DateService.newUTCDate());
		entry2.setMDEntryType(MDEntryType.Bid);
		entry2.setTsParties(parties2);
		tsNoMDEntriesList.add(entry2);

		ArrayList<TSNoPartyID> list3 = new ArrayList<TSNoPartyID>();
		TSNoPartyID party3 = new TSNoPartyID();
		party3.setPartyID("party3");
		party3.setPartyIDSource(bic);
		party3.setPartyRole(BondvisionBookManager.executingFirm);
		list3.add(party3);
		parties3.setTSNoPartyIDsList(list3);

		entry3.setMDEntryPx(101.1);
		entry3.setMDEntrySize(50000.0);
		entry3.setMDPriceLevel(3);
		entry3.setPriceType(PriceType.Percentage);
		entry3.setQuoteCondition(QuoteCondition.CloseInactive);
		entry3.setMDEntryTime(DateService.newUTCDate());
		entry3.setMDEntryType(MDEntryType.Bid);
		entry3.setTsParties(parties3);
		tsNoMDEntriesList.add(entry3);
		
        if (tsNoMDEntriesList.size() > 0) {
            tsMDFullGrp.setTSNoMDEntriesList(tsNoMDEntriesList);
            tsMarketDataRefresh.setTSMDFullGrp(tsMDFullGrp);
        }
		
		return tsMarketDataRefresh;
	}
	
	/**
	 * @return
	 * @throws BestXException
	 */
	protected TSMarketDataSnapshotFullRefresh addBookToMgr() throws BestXException {
		TSInstrument tsInstrument = new TSInstrument();
		tsInstrument.setSecurityID("AC1123456789");
		tsInstrument.setSecurityIDSource(SecurityIDSource.IsinNumber);
		TSMarketDataSnapshotFullRefresh book = createTSMarketDataSnapshotFullRefresh(rFCQID, tsInstrument);
		mgr.updateBook(rFCQID, book, refreshCounters, attemptNo, Proposal.ProposalSide.BID);
		return book;
	}
}
