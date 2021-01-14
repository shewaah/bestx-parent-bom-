package it.softsolutions.bestx.services.booksorter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Market.SubMarketCode;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Venue;

public class ClassifiedAskComparatorTest {
    ClassifiedAskComparator comparator = new ClassifiedAskComparator();

    @BeforeClass
    public static void setUp() throws Exception {
//        Messages messages = new Messages();
//        messages.setBundleName("messages");
//        messages.setLanguage("it");
//        messages.setCountry("IT");
    }

    @Test
    public void testDifferentPrices() {
        Market marketTW = getMarkets().get(MarketCode.TW);

        // price(p1) < price(p2) --> expect (-1)
        ClassifiedProposal p1 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, marketTW);
        ClassifiedProposal p2 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(102.35), ProposalSide.ASK, marketTW);
        org.junit.Assert.assertEquals(-1, comparator.compare(p1, p2));

        // price(p1) > price(p2) --> expect (+1)
        p1 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(102.35), ProposalSide.ASK, marketTW);
        p2 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, marketTW);
        org.junit.Assert.assertEquals(+1, comparator.compare(p1, p2));
    }
    
    @Test
    public void testOneCounterPrice() {
        Market marketTW = getMarkets().get(MarketCode.TW);

        // price(p1) = price(p2), p1 is counter --> expect (-1)
        ClassifiedProposal p1 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.COUNTER, marketTW);
        ClassifiedProposal p2 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.TRADEABLE, marketTW);
        org.junit.Assert.assertEquals(-1, comparator.compare(p1, p2));

        // price(p1) = price(p2), p2 is counter --> expect (+1)
        p1 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.TRADEABLE, marketTW);
        p2 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.COUNTER, marketTW);
        org.junit.Assert.assertEquals(+1, comparator.compare(p1, p2));
    }

    @Test
    public void testTwoCounterPricesDifferentTimestamp() {
        Market marketTW = getMarkets().get(MarketCode.TW);

        Date timestamp = new Date();

        // price(p1) = price(p2), both counter, p1 newer --> expect (-1)
        ClassifiedProposal p1 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.COUNTER, DateUtils.addSeconds(timestamp, 1), marketTW);
        ClassifiedProposal p2 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.COUNTER, timestamp, marketTW);
        org.junit.Assert.assertEquals(-1, comparator.compare(p1, p2));

        // price(p1) = price(p2), both counter, p2 newer --> expect (+1)
        p1 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.COUNTER, timestamp, marketTW);
        p2 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.COUNTER, DateUtils.addSeconds(timestamp, 1), marketTW);
        org.junit.Assert.assertEquals(+1, comparator.compare(p1, p2));
    }
    
    @Test
    public void testSamePriceSameTimestampDifferentMMRank() {
        Market marketTW = getMarkets().get(MarketCode.TW);

        Date timestamp = new Date();
        Venue venue1 = ClassifieProposalGeneratorHelper.getNewVenue("MM1", 1);  // rank 1 is better than 2 (has higher priority)
        Venue venue2 = ClassifieProposalGeneratorHelper.getNewVenue("MM2", 2);

        // p1=p2, rank1 < rank2
        ClassifiedProposal p1 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.TRADEABLE, timestamp, venue1, marketTW);
        ClassifiedProposal p2 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.TRADEABLE, timestamp, venue2, marketTW);
        org.junit.Assert.assertEquals(-1, comparator.compare(p1, p2));

        // p1=p2, rank1 > rank2
        venue1 = ClassifieProposalGeneratorHelper.getNewVenue("MM1", 2);
        venue2 = ClassifieProposalGeneratorHelper.getNewVenue("MM2", 1);
        p1 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.TRADEABLE, timestamp, venue1, marketTW);
        p2 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.TRADEABLE, timestamp, venue2, marketTW);
        org.junit.Assert.assertEquals(+1, comparator.compare(p1, p2));
    }
    
    @Test
    public void testSamePriceOneOnTW() {
        Market marketTW = getMarkets().get(MarketCode.TW);              // TW market has higher priority than others (except internalization)
        Market marketRTFI = getMarkets().get(MarketCode.MARKETAXESS);
        
        //MarketMarketMaker mmm = new MarketMarketMaker();
        Date timestamp = new Date();
        Venue venue1 = ClassifieProposalGeneratorHelper.getNewVenue("MM1", 1);  // rank 1 is better than 2 (has higher priority)
        Venue venue2 = ClassifieProposalGeneratorHelper.getNewVenue("MM2", 2);

        // p1=p2, rank MM1 better than MM2, mkt1 is TW --> choose p1
        ClassifiedProposal p1 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.TRADEABLE, timestamp, venue1, marketTW);
        ClassifiedProposal p2 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.TRADEABLE, timestamp, venue2, marketRTFI);
        org.junit.Assert.assertEquals(-1, comparator.compare(p1, p2));

        // p1=p2, rank MM1 better than MM2, mkt2 is TW --> choose p2
        venue1 = ClassifieProposalGeneratorHelper.getNewVenue("MM1", 1);
        venue2 = ClassifieProposalGeneratorHelper.getNewVenue("MM2", 2);
        p1 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.TRADEABLE, timestamp, venue1, marketRTFI);
        p2 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.TRADEABLE, timestamp, venue2, marketTW);
        org.junit.Assert.assertEquals(+1, comparator.compare(p1, p2));
    }
    
    @Test
    public void testSortingWithThousandProposals() {
        Market marketTW = getMarkets().get(MarketCode.TW);              // TW market has higher priority than others (except internalization)
        Market marketMA = getMarkets().get(MarketCode.MARKETAXESS);
        
        //MarketMarketMaker mmm = new MarketMarketMaker();
        Date timestamp = new Date();
        Venue venue1 = ClassifieProposalGeneratorHelper.getNewVenue("MM1", 0);  // rank 1 is better than 2 (has higher priority)
        Venue venue2 = ClassifieProposalGeneratorHelper.getNewVenue("MM2", 0);
        Venue venue3 = ClassifieProposalGeneratorHelper.getNewVenue("MM3", 0);
        Venue venue4 = ClassifieProposalGeneratorHelper.getNewVenue("MM4", 0);
        Venue venue5 = ClassifieProposalGeneratorHelper.getNewVenue("MM5", 0);
        Venue venue6 = ClassifieProposalGeneratorHelper.getNewVenue("MM6", 0);
        Venue venue7 = ClassifieProposalGeneratorHelper.getNewVenue("MM7", 0);
        Venue venue8 = ClassifieProposalGeneratorHelper.getNewVenue("MM8", 0);
        
        // p1=p2, rank MM1 better than MM2, mkt1 is TW --> choose p1
        ClassifiedProposal p1 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.INDICATIVE, timestamp, venue1, marketTW);
        ClassifiedProposal p2 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.INDICATIVE, timestamp, venue2, marketTW);
        ClassifiedProposal p3 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.INDICATIVE, timestamp, venue3, marketMA);
        ClassifiedProposal p4 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.INDICATIVE, timestamp, venue4, marketTW);
        ClassifiedProposal p5 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.INDICATIVE, timestamp, venue5, marketMA);
        ClassifiedProposal p6 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.INDICATIVE, timestamp, venue6, marketTW);
        ClassifiedProposal p7 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.INDICATIVE, timestamp, venue7, marketMA);
        ClassifiedProposal p8 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.INDICATIVE, timestamp, venue8, marketTW);
        ClassifiedProposal p9 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.INDICATIVE, timestamp, null, marketTW);
        ClassifiedProposal p10 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.INDICATIVE, timestamp, null, marketTW);
        
        ArrayList<ClassifiedProposal> tempProposals = new ArrayList<ClassifiedProposal>();
        tempProposals.add(p1);
        tempProposals.add(p10);
        tempProposals.add(p2);
        tempProposals.add(p3);
        tempProposals.add(p4);
        tempProposals.add(p5);
        tempProposals.add(p6);
        tempProposals.add(p7);
        tempProposals.add(p8);
        tempProposals.add(p9);

        ArrayList<ClassifiedProposal> classifiedProposals = new ArrayList<ClassifiedProposal>();
        for (int i = 0; i < 20; i++) {
           classifiedProposals.add(tempProposals.get(i % 10));
        }
        Collections.sort(classifiedProposals, comparator);
        
        for (int i = 0; i < classifiedProposals.size(); i++)
           System.out.println("" + i + "- " + classifiedProposals.get(i).toStringShort() + " - " + 
                 (classifiedProposals.get(i).getVenue() != null ? classifiedProposals.get(i).getVenue().getMarketMaker() : ""));

        
        org.junit.Assert.assertEquals(0, comparator.compare(p1, p2));

        // p1=p2, rank MM1 better than MM2, mkt2 is TW --> choose p2
        venue1 = ClassifieProposalGeneratorHelper.getNewVenue("MM1", 2);
        venue2 = ClassifieProposalGeneratorHelper.getNewVenue("MM2", 2);
        p1 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.INDICATIVE, timestamp, venue1, marketMA);
        p2 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.INDICATIVE, timestamp, venue2, marketMA);
        org.junit.Assert.assertEquals(0, comparator.compare(p1, p2));
    }
    
    @Test
    public void testSameBrokerOneOnTW() {
        Market marketTW = getMarkets().get(MarketCode.TW);              // TW market has higher priority than others (except internalization)
        Market marketRTFI = getMarkets().get(MarketCode.MARKETAXESS);
        
        Date timestamp = new Date();
        Venue venue1 = ClassifieProposalGeneratorHelper.getNewVenue("MM1", 1);  // same venue on different markets

        // p1=p2, rank MM1 better than MM2, mkt1 is TW --> choose p1
        ClassifiedProposal p1 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.TRADEABLE, timestamp, venue1, marketTW);
        ClassifiedProposal p2 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.TRADEABLE, timestamp, venue1, marketRTFI);
        org.junit.Assert.assertEquals(-1, comparator.compare(p1, p2));

        // p1=p2, rank MM1 better than MM2, mkt2 is TW --> choose p2
        p1 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.TRADEABLE, timestamp, venue1, marketRTFI);
        p2 = ClassifieProposalGeneratorHelper.getProposal(BigDecimal.valueOf(101.35), ProposalSide.ASK, ProposalType.TRADEABLE, timestamp, venue1, marketTW);
        org.junit.Assert.assertEquals(+1, comparator.compare(p1, p2));
    }
    
    
    
    
    
    
    private static Map<MarketCode, Market> getMarkets()
    {
        Map<MarketCode, Market> mkts = new HashMap<MarketCode, Market>();

        String[] mktNames = new String[] {"INTERNALIZZAZIONE","BLOOMBERG","MARKETAXESS", "MATCHING", "TW"};
        MarketCode[] mktCodes = new MarketCode[] {MarketCode.INTERNALIZZAZIONE, MarketCode.BLOOMBERG, MarketCode.MARKETAXESS, MarketCode.TW};
        String[] subMarketCodes = new String[] {"","","","",""};
        long[] marketIds = new long[] {0,1,2,5,6};
        int[] rankings = new int[] {0,3,2,4,1};

        for (int i = 0; i < marketIds.length; i++) {
            Market mkt = new Market();
            mkt.setMarketCode(mktCodes[i]);
            mkt.setMarketId(marketIds[i]);
            mkt.setMarketCode(MarketCode.valueOf(mktNames[i]));
            mkt.setSubMarketCode(subMarketCodes[i].isEmpty() ? null : SubMarketCode.valueOf(subMarketCodes[i]));
            mkt.setRanking(rankings[i]);

            mkts.put(mktCodes[i], mkt);
        }

        return mkts; 
    }
    
    //precedenza mercato TW anche se MM ha ranking peggiore: o scalpello la precedenza di TW, o controllo prima il market e dopo la venue (MM), ora è il contrario 
    
}
