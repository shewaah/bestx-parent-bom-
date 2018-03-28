package it.softsolutions.bestx.markets.bondvision;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.jsscommon.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BondVisionMarketTest {
	private __TestableBondVisionMarket bvMkt;
	private ArrayList<ClassifiedProposal> inProposals;
	private Map<MarketMarketMaker, ClassifiedProposal> outBidProposals; 
	private Map<MarketMarketMaker, ClassifiedProposal> outAskProposals; 
	private Messages messages;
	
	@Before
	public void setUp() throws Exception {
		bvMkt = new __TestableBondVisionMarket();
		outBidProposals = new HashMap<MarketMarketMaker, ClassifiedProposal>();
		outAskProposals = new HashMap<MarketMarketMaker, ClassifiedProposal>();
		
		inProposals = new ArrayList<ClassifiedProposal>();
		
		// initializa Messages, too
		messages = new Messages();
		messages.setBundleName("messages");
		messages.setLanguage("it");
		messages.setCountry("IT");
	}

	@After
	public void tearDown() throws Exception {
	}

/*	private XT2Msg getMsg() {
		XT2Msg msg = new XT2Msg();
		
		msg.setValue("Bid.NoMDEntries.ItemCount", 1);
		msg.setValue("Ask.NoMDEntries.ItemCount", 0);
		
		return msg;
	}*/

/*				
		Ask.Columns.ItemCount.2=1
			Ask.Columns.ItemCount.1=1
			SubMarketName=BVS
			Ask.Time.2.1=162947874
			Ask.Qty.1.1=0.0010
			Ask.Price.2.1=100.0
			Ask.Yield.2.1=2.973
			Bid.Date.2.1=20111215
			Ask.Time.1.1=180302083
			Ask.Price.1.1=99.9
			Bid.CodeMM.2.1=23340SS0
			Isin=IT0004467483
			Ask.Yield.1.1=3.4775
			Bid.PriceStatus.2.1=I
			Bid.Date.1.1=20111215
			Ask.CodeMM.2.1=23340SS0
			Bid.MinExecQty.2.1=0.0
			Bid.DescMM.2.1=SOFTSOLUTIONS
			Ask.QuoteID.2.1=600000242
			Bid.Price.2.1=99.0
			Bid.CodeMM.1.1=23341SS1
			Bid.Qty.2.1=0.0010
			Bid.PriceStatus.1.1=I
			SourceMarketName=BV
			Bid.Yield.2.1=8.0631
			Ask.DescMM.2.1=SOFTSOLUTIONS
			Bid.MinExecQty.1.1=0.0
			Ask.CodeMM.1.1=23341SS1
			IsYieldQuoted=0
			CD=P00000158
			x Ask.NoMDEntries.ItemCount=2
			Ask.Date.2.1=20111215
			Bid.DescMM.1.1=SOFTSOLUTIONS
			Ask.MinExecQty.2.1=0.0
			Ask.QuoteID.1.1=600000244
			Bid.Price.1.1=99.1
			x Bid.NoMDEntries.ItemCount=2
			Bid.Qty.1.1=0.0010
			Bid.Yield.1.1=7.5495
			Ask.DescMM.1.1=SOFTSOLUTIONS
			Bid.Columns.ItemCount.2=1
			Bid.Columns.ItemCount.1=1
			Ask.Date.1.1=20111215
			Ask.MinExecQty.1.1=0.0
			Ask.PriceStatus.2.1=I
			Ask.PriceStatus.1.1=I
			Bid.QuoteID.2.1=600000242
			Bid.Time.2.1=162947874
			Bid.QuoteID.1.1=600000244
			UserSessionName=PAOLO
			Bid.Time.1.1=180302083
			Ask.Qty.2.1=0.0010  */
	
	@Test
	public void testLevelBook_NullValues1() {
		
		// null params
		try {
			bvMkt.levelBook(null, null, null);
			fail("Should throw exception");
		} catch (BestXException e) {
			; // ok
		}
	}
	@Test
	public void testLevelBook_NullValues2() {
		
		// null params
		try {
			bvMkt.levelBook(null, null, null);
			fail("Should throw exception");
		} catch (BestXException e) {
			; // ok
		}
	}

	@Test
	public void testLevelBook_EmptyValues() {
		
		try {
			bvMkt.levelBook(inProposals, outBidProposals, outAskProposals);
			assertTrue("invalid bid proposals size", outBidProposals.size() == 0);
			assertTrue("invalid ask proposals size", outAskProposals.size() == 0);
		} catch (BestXException e) {
			fail("Exception: " + e.getMessage());
		}
	}

	@Test
	/*
	 * Input: 1 bid level
	 * 
	 * Expected output: 1 bid and 1 (empty) ask
	 * 
	 */
	
	public void testLevelBook_OneBidValue() {

		String mmmCode = "23340";
		MarketCode mktCode = MarketCode.BV;
		addProposal(ProposalSide.BID, mmmCode, mktCode, 101.0, 10000.0);
		
		try {
			bvMkt.levelBook(inProposals, outBidProposals, outAskProposals);
			
			assertTrue("invalid bid proposals size", outBidProposals.size() == 1);
			verifyProposal(ProposalSide.BID, mmmCode, MarketCode.BV, 101.0);
			
			assertTrue("invalid ask proposals size", outAskProposals.size() == 1);
			verifyProposal(ProposalSide.ASK, mmmCode, MarketCode.BV, 0.0);
		} catch (BestXException e) {
			fail("Exception: " + e.getMessage());
		}
	}

	@Test
	/*
	 * Input: 1 ask level
	 * 
	 * Expected output: 1 bid (empty) and 1 ask
	 * 
	 */
	
	public void testLevelBook_OneAskValue() {

		String mmmCode = "23340";
		MarketCode mktCode = MarketCode.BV;
		addProposal(ProposalSide.ASK, mmmCode, mktCode, 101.0, 10000.0);
		
		try {
			bvMkt.levelBook(inProposals, outBidProposals, outAskProposals);
			
			assertTrue("invalid bid proposals size", outBidProposals.size() == 1);
			verifyProposal(ProposalSide.BID, mmmCode, MarketCode.BV, 0.0);
			
			assertTrue("invalid ask proposals size", outAskProposals.size() == 1);
			verifyProposal(ProposalSide.ASK, mmmCode, MarketCode.BV, 101.0);
		} catch (BestXException e) {
			fail("Exception: " + e.getMessage());
		}
	}
	@Test
	/*
	 * Input: 1 bid level, 1 ask level
	 * 
	 * Expected output: 1 bid and 1 ask
	 * 
	 */
	
	public void testLevelBook_OneBidValue_OneAskValue() 
	{
		String mmmCode = "23340";
		MarketCode mktCode = MarketCode.BV;
		addProposal(ProposalSide.BID, mmmCode, mktCode, 101.0, 10000.0);
		addProposal(ProposalSide.ASK, mmmCode, mktCode, 101.5, 10000.0);
		
		try {
			bvMkt.levelBook(inProposals, outBidProposals, outAskProposals);
			
			assertTrue("invalid bid proposals size", outBidProposals.size() == 1);
			verifyProposal(ProposalSide.BID, mmmCode, MarketCode.BV, 101.0);
			
			assertTrue("invalid ask proposals size", outAskProposals.size() == 1);
			verifyProposal(ProposalSide.ASK, mmmCode, MarketCode.BV, 101.5);
		} catch (BestXException e) {
			fail("Exception: " + e.getMessage());
		}
	}

	@Test
	/*
	 * Input: 2 bid levels
	 * 
	 * Expected output: 2 bid and 2 ask (empty) levels
	 * 
	 */
	
	public void testLevelBook_TwoBidValues() {

		String mmmCode = "23340";
		String mmmCode2 = "23341";
		MarketCode mktCode = MarketCode.BV;
		addProposal(ProposalSide.BID, mmmCode, mktCode, 101.0, 10000.0);
		addProposal(ProposalSide.BID, mmmCode2, mktCode, 101.5, 10000.0);
		
		try {
			bvMkt.levelBook(inProposals, outBidProposals, outAskProposals);
			
			assertTrue("invalid bid proposals size", outBidProposals.size() == 2);
			verifyProposal(ProposalSide.BID, mmmCode, MarketCode.BV, 101.0);
			verifyProposal(ProposalSide.BID, mmmCode2, MarketCode.BV, 101.5);
			
			assertTrue("invalid ask proposals size", outAskProposals.size() == 2);
			verifyProposal(ProposalSide.ASK, mmmCode, MarketCode.BV, 0.0);
			verifyProposal(ProposalSide.ASK, mmmCode2, MarketCode.BV, 0.0);
		} catch (BestXException e) {
			fail("Exception: " + e.getMessage());
		}
	}
	

	@Test
	/*
	 * Input: 2 ask levels
	 * 
	 * Expected output: 2 bid (empty) and 2 ask levels
	 * 
	 */
	
	public void testLevelBook_TwoAskValues() {

		String mmmCode = "23340";
		String mmmCode2 = "23341";
		MarketCode mktCode = MarketCode.BV;
		addProposal(ProposalSide.ASK, mmmCode, mktCode, 101.0, 10000.0);
		addProposal(ProposalSide.ASK, mmmCode2, mktCode, 101.5, 10000.0);
		
		try {
			bvMkt.levelBook(inProposals, outBidProposals, outAskProposals);
			
			assertTrue("invalid bid proposals size", outBidProposals.size() == 2);
			verifyProposal(ProposalSide.BID, mmmCode, MarketCode.BV, 0.0);
			verifyProposal(ProposalSide.BID, mmmCode2, MarketCode.BV, 0.0);
			
			assertTrue("invalid ask proposals size", outAskProposals.size() == 2);
			verifyProposal(ProposalSide.ASK, mmmCode, MarketCode.BV, 101.0);
			verifyProposal(ProposalSide.ASK, mmmCode2, MarketCode.BV, 101.5);
		} catch (BestXException e) {
			fail("Exception: " + e.getMessage());
		}
	}
	

	@Test
	/*
	 * Input: 2 ask levels
	 * 
	 * Expected output: 2 bid (empty) and 2 ask levels
	 * 
	 */
	
	public void testLevelBook_OneBidValue_OneDifferentAskValue() 
	{
		String mmmCode = "23340";
		String mmmCode2 = "23341";
		MarketCode mktCode = MarketCode.BV;
		addProposal(ProposalSide.BID, mmmCode, mktCode, 101.0, 10000.0);
		addProposal(ProposalSide.ASK, mmmCode2, mktCode, 101.5, 10000.0);
		
		try {
			bvMkt.levelBook(inProposals, outBidProposals, outAskProposals);
			
			assertTrue("invalid bid proposals size", outBidProposals.size() == 2);
			verifyProposal(ProposalSide.BID, mmmCode, MarketCode.BV, 101.0);
			verifyProposal(ProposalSide.BID, mmmCode2, MarketCode.BV, 0.0);
			
			assertTrue("invalid ask proposals size", outAskProposals.size() == 2);
			verifyProposal(ProposalSide.ASK, mmmCode, MarketCode.BV, 0.0);
			verifyProposal(ProposalSide.ASK, mmmCode2, MarketCode.BV, 101.5);
		} catch (BestXException e) {
			fail("Exception: " + e.getMessage());
		}
	}
	
	private void addProposal(ProposalSide side, String mmmCode, MarketCode mmmMktCode, double price, double qty)
	{
		MarketMarketMaker mmm = new MarketMarketMaker();
		mmm.setMarketSpecificCode(mmmCode);
		Market mkt = new Market();
		mkt.setMarketCode(mmmMktCode);
		mmm.setMarket(mkt);
		
		Venue venue = new Venue();
		ClassifiedProposal proposal = new ClassifiedProposal();
		
		proposal.setMarketMarketMaker(mmm);
		proposal.setSide(side);
		proposal.setVenue(venue);
		proposal.setPrice(new Money("EUR", new BigDecimal(price)));
		proposal.setQty(new BigDecimal(qty));

		inProposals.add(proposal);
	}
	
	private void verifyProposal(ProposalSide side, String mmmCode, MarketCode mmmMktCode, double expectedPrice)
	{
		MarketMarketMaker mmm = new MarketMarketMaker();
		mmm.setMarketSpecificCode(mmmCode);
		Market mkt = new Market();
		mkt.setMarketCode(mmmMktCode);
		mmm.setMarket(mkt);
		
		ClassifiedProposal proposalToVerify = null;
		if (side.equals(ProposalSide.BID))
			proposalToVerify = outBidProposals.get(mmm);
		else
			proposalToVerify = outAskProposals.get(mmm);
		
		assertNotNull("Missing proposal for [" + mmmCode + "] [" + mmmMktCode + "], side [" + side + "]", proposalToVerify);
		assertTrue("Invalid price [" + proposalToVerify.getPrice().getAmount() + "], expected [" + expectedPrice + "] for [" + mmmCode  + "], [" + mmmMktCode + "], side [" + side + "]", proposalToVerify.getPrice().getAmount().equals(new BigDecimal(expectedPrice)));
	}

	@Test
	public void testLevelBook_NullProposals() {
		
		try {
			inProposals.add(null);
			bvMkt.levelBook(inProposals, outBidProposals, outAskProposals);
			fail("Should throw nullPointerException");
			assertTrue("invalid bid proposals size", outBidProposals.size() == 0);
			assertTrue("invalid ask proposals size", outAskProposals.size() == 0);
		} catch (BestXException e) {
			fail("Exception: " + e.getMessage());
		} catch (NullPointerException e2) {
			;
		} catch (Throwable t) {
			fail("Exception: " + t.getMessage());
		}
	}
	
}
