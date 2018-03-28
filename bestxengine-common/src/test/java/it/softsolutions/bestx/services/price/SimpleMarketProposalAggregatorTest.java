package it.softsolutions.bestx.services.price;

import static org.junit.Assert.assertEquals;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.connections.MarketPriceConnection;
import it.softsolutions.bestx.helpers.__FakeMarketFinder;
import it.softsolutions.bestx.model.Book;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.model.Venue.VenueType;
import it.softsolutions.jsscommon.Money;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SimpleMarketProposalAggregatorTest {

	private SimpleMarketProposalAggregator aggregator = null;
	private __FakeMarketPriceConnectionListener priceListener = null;
	private Map<String, Market> markets = null;
	private Map<String, MarketPriceConnection> marketPriceConnections = null;
	private Messages messages;
	
	Map<String, List<MarketMarketMaker>> mmmsByMarket = null;
	
	@Before
	public void setUp() throws Exception {
		aggregator = SimpleMarketProposalAggregator.getInstance();
		aggregator.setMarketFinder(new __FakeMarketFinder());
		priceListener = new __FakeMarketPriceConnectionListener();
		//[RBCL, TEST21, MSFI, MSOL, MSTY, MSEG, MSEB, MSPF, BARX, CBKF, CBKG, CBK, INGG, INGD, INGN, EBNP, BPRX, RAB0, RABO, RABX, ZKB, UBSB, UBAP, UBSX, UBSZ, DAB, AKRO]

		// uses MarketMarketMaker, which uses Messages, so it is mandatory to setup Messages...
		messages = new Messages();
		messages.setBundleName("messages");
		messages.setLanguage("it");
		messages.setCountry("IT");
	}

	@After
	public void tearDown() throws Exception {
		aggregator = null;
		priceListener = null;
	}

	ClassifiedProposal getProposal(Market market, MarketMarketMaker mmm, ProposalSide side, Money price) {
		ClassifiedProposal proposal = new ClassifiedProposal();
		
		proposal.setMarket(market);
		proposal.setMarketMarketMaker(mmm);
		proposal.setSide(side);
		proposal.setPrice(price);
		proposal.setProposalState(ProposalState.NEW);

		Venue venue = new Venue();
		venue.setMarket(market);
		venue.setVenueType(VenueType.MARKET_MAKER);
		venue.setCode(mmm.getMarketMaker().getCode());
		proposal.setVenue(venue);
		
		return proposal;
	}
	
	@Test
	public void testProposalResult() throws IOException, BestXException {
		String baseDir = System.getProperty("user.dir");
		//File file = new File(baseDir + "/src/it/softsolutions/bestx/test/services/price/SimpleMarketProposalAggregatorJsonTests.txt");
		File file = new File(baseDir + "/src/test/resources/SimpleMarketProposalAggregatorJsonTests.txt");
		
		String testName = "";
		
			markets = new HashMap<String, Market>();
			marketPriceConnections = new HashMap<String, MarketPriceConnection>(); 
			mmmsByMarket = new HashMap<String, List<MarketMarketMaker>>();

			// Read the entire contents of sample.txt
			
			String jsonTxt = FileUtils.readFileToString(file);

			////////////////// JSON file structure /////////////////
			// tests[]
			//   testName
			//   marketMakers[]
			//     sabeCode
			//     sabeName
			//     enabled
			//     marketMarketMakers[]
			//       market
			//       marketCodes[]
			//         marketCode
			//       filter
			//   orderIsin
			//   proposals[]
			//     isin, market, mmmMarketCode, side, currency, price, errorCode, errorMsg, isLastProposal
			///////////////////////////////////////////////////////       
			
			JSONObject jsonBaseObj = (JSONObject) JSONSerializer.toJSON( jsonTxt );
			JSONArray jsonTestsArray = jsonBaseObj.getJSONArray("tests");
			for (int i = 0; i < jsonTestsArray.size(); i++) {
				markets.clear();
				marketPriceConnections.clear();
				mmmsByMarket.clear();
				priceListener.reset();
				
				JSONObject testObj = jsonTestsArray.getJSONObject(i);
				testName = testObj.getString("testName");
				
				//
				// create and set market makers configuration
				//
				JSONArray jsonMarketMakersArray = testObj.getJSONArray("marketMakers");
				for (int i2 = 0; i2 < jsonMarketMakersArray.size(); i2++) {
					JSONObject mmObj = jsonMarketMakersArray.getJSONObject(i2);
					
					MarketMaker mm = new MarketMaker();
					mm.setCode(mmObj.getString("sabeCode"));
					mm.setName(mmObj.getString("sabeName"));
					mm.setEnabled(mmObj.getBoolean("enabled"));
					
					Set<MarketMarketMaker> mmmSet = new HashSet<MarketMarketMaker>();
					mmmSet.clear();
					JSONArray jsonMMMArray = mmObj.getJSONArray("marketMarketMakers");
					for (int i3 = 0; i3 < jsonMMMArray.size(); i3++) {
						JSONObject mmmObj = jsonMMMArray.getJSONObject(i3);
						String marketName = mmmObj.getString("market");
						
						if (!markets.containsKey(marketName)) {
							Market market = new Market();
							market.setMarketCode(MarketCode.valueOf(marketName));
							markets.put(marketName, market);
						}
						if (!marketPriceConnections.containsKey(marketName)) {
							MarketPriceConnection priceConnection = new __FakeMarketPriceConnection();
							((__FakeMarketPriceConnection)priceConnection).setMarketCode(MarketCode.valueOf(marketName));
							marketPriceConnections.put(marketName, priceConnection);
							priceListener.addMarketPriceConnection(priceConnection);
						}
						String filter = mmmObj.getString("filter");

						JSONArray jsonMarketCodesArray = mmmObj.getJSONArray("marketCodes");
						for (int i4 = 0; i4 < jsonMarketCodesArray.size(); i4++) {
							JSONObject marketCodeObj = jsonMarketCodesArray.getJSONObject(i4);

							//
							// set market maker for mmm (1 mm for each mmm) 
							//
							MarketMarketMaker mmm = new MarketMarketMaker();
							mmm.setMarketSpecificCode(marketCodeObj.getString("marketCode"));
							mmm.setEnabledFilter(filter);
							mmm.setMarketMaker(mm);
							mmm.setMarket(markets.get(marketName));
							mmmSet.add(mmm);
							
							if (!mmmsByMarket.containsKey(marketName)) {
								mmmsByMarket.put(marketName, new ArrayList<MarketMarketMaker>());
							}
							mmmsByMarket.get(marketName).add(mmm);
						}
					}
					mm.setMarketMarketMakers(mmmSet);
				}
				for (String marketName : mmmsByMarket.keySet()) {
					priceListener.setMarketMarketMakersForMarket(mmmsByMarket.get(marketName), marketName);	// <<--- add mmm to pricelistener
				}
				
				//
				// create instrument
				//
				JSONObject orderObj = testObj.getJSONObject("order");
				
				String orderIsin = orderObj.getString("isin");
				Instrument instrument = new Instrument();
				instrument.setIsin(orderIsin);
				
				//
				// create order with all mmms, still waiting for prices from the markets
				//
				Order order = new Order();
				for (String marketName : mmmsByMarket.keySet()) {
					for (MarketMarketMaker mmm : mmmsByMarket.get(marketName)) {
						order.addMarketMakerNotQuotingInstr(mmm, "Price not yet received");
					}
				}
				order.setInstrument(instrument);
				order.setSide(OrderSide.SELL);
				priceListener.setOrder(order);
				
				aggregator.addBookListener(instrument, priceListener);

				//
				// create and send proposals
				//
				JSONArray jsonProposalsArray = testObj.getJSONArray("proposals");
				for (int i5 = 0; i5 < jsonProposalsArray.size(); i5++) {
					JSONObject proposalObj = jsonProposalsArray.getJSONObject(i5);

					String proposalMarket = proposalObj.getString("market");
					String proposalMMMStr = proposalObj.getString("mmmMarketCode");
					String sideStr = proposalObj.getString("side");
					ProposalSide proposalSide = ProposalSide.valueOf(sideStr);
					String proposalCurrency = proposalObj.getString("currency");
					double proposalPrice = proposalObj.getDouble("price");
					int proposalErrorCode = proposalObj.getInt("errorCode");
					String proposalErrorMsg = proposalObj.getString("errorMsg");
					boolean isLastProposal = proposalObj.getBoolean("isLastProposal");

					MarketMarketMaker proposalMMM = null; 
					for (MarketMarketMaker mmm : mmmsByMarket.get(proposalMarket)) {
						if (mmm.getMarketSpecificCode().equals(proposalMMMStr)) {
							proposalMMM = mmm;
							break;
						}
					}
					MarketPriceConnection marketPriceConnection = marketPriceConnections.get(proposalMarket);
					ClassifiedProposal proposal = getProposal(markets.get(proposalMarket), proposalMMM, proposalSide, new Money(proposalCurrency, new BigDecimal(proposalPrice)));
					aggregator.onProposal(instrument, proposal, marketPriceConnection, proposalErrorCode, proposalErrorMsg, isLastProposal);
				}
				
				//
				// get books (one for each market), and compare to expected results
				//
				JSONArray jsonExpectedBooksArray = testObj.getJSONArray("expectedBooks");
				for (int i6 = 0; i6 < jsonExpectedBooksArray.size(); i6++) {
					JSONObject expectedBookObj = jsonExpectedBooksArray.getJSONObject(i6);
					
					String expectedBookMarketName = expectedBookObj.getString("market");
					JSONObject expectedBookBidObj = expectedBookObj.getJSONObject("bid");
					JSONObject expectedBookAskObj = expectedBookObj.getJSONObject("ask");
					int expectedBookBidDepth = expectedBookBidObj.getInt("depth");
					int expectedBookAskDepth = expectedBookAskObj.getInt("depth");
					
					Book resultBook = priceListener.getResultBook(expectedBookMarketName);					
					if ( (expectedBookBidDepth == 0) && (expectedBookAskDepth == 0) ) {
						org.junit.Assert.assertNull("Test [" + testName + "] Result Book should be empty", resultBook);
					}
					else {
						org.junit.Assert.assertNotNull("Test [" + testName + "] Result Book should not be empty", resultBook);
						
						assertEquals("Test [" + testName + "] Unexpected book bid size", expectedBookBidDepth, resultBook.getBidProposals().size());
						assertEquals("Test [" + testName + "] Unexpected book ask size", expectedBookAskDepth, resultBook.getAskProposals().size());
						//verifyBookResult(resultBook, 1, ProposalSide.BID, 99.0, MarketCode.BLOOMBERG, "RBCL", "");
					}
				}
			}
		
		//
		// Dubbi: 
		// 1) l'elenco di mmm (con relativi mm) viene passato sia al MarketPriceConnectionListener che all'order : e' necessario?
		// 2) all'ordine devo passare tutti i mmm, anche quelli disabled? 
		//
		//
		//
	}

	/*private void verifyBookResult(Book resultBook, int bookLevel, ProposalSide side, double price, MarketCode mktCode, String mktSpecificCode, String discardReason)
	{
		Object[] results = null;
		if (side == ProposalSide.BID) 
			results = resultBook.getBidProposals().toArray();
		else 
			results = resultBook.getAskProposals().toArray();
		
		if ( (results == null) || (results.length < bookLevel) ) {
			fail("Book level [" + bookLevel + "] not available for [" + side + "]");
		}
		
		ClassifiedProposal bookProposal = (ClassifiedProposal)results[bookLevel-1];

		assertTrue("Wrong side", bookProposal.getSide().equals(side));
		assertTrue("Wrong price, expected [" + price + "] but found [" + bookProposal.getPrice().getAmount() + "]", bookProposal.getPrice().equals(new Money("EUR", new BigDecimal(price))));
		assertTrue("Wrong market code, expected [" + mktCode + "] but found [" + bookProposal.getMarket().getMarketCode() + "]", bookProposal.getMarket().getMarketCode().equals(mktCode));
		assertTrue("Wrong mmm mkt code, expected [" + mktSpecificCode + "] but found [" + bookProposal.getMarketMarketMaker().getMarketSpecificCode() + "]", bookProposal.getMarketMarketMaker().getMarketSpecificCode().equals(mktSpecificCode));
		assertTrue("Wrong discard reason, expected [" + mktSpecificCode + "] but found [" + bookProposal.getMarketMarketMaker().getMarketSpecificCode() + "]", bookProposal.getMarketMarketMaker().getMarketSpecificCode().equals(mktSpecificCode));
		boolean discardReasonOk = false;
		if ( (discardReason == null) && ((bookProposal.getReason() == null) || bookProposal.getReason().equals("")))
			discardReasonOk = true;
		else if ( (discardReason.equals("")) && ((bookProposal.getReason() == null) || bookProposal.getReason().equals("")))
			discardReasonOk = true;
		else discardReasonOk = discardReason.equals(bookProposal.getReason());
		assertTrue("Wrong discard reason, expected [" + discardReason + "] but found [" + bookProposal.getReason() + "]", discardReasonOk);
	}*/
}
