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

package it.softsolutions.bestx.services.proposalclassifiers.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import it.softsolutions.bestx.model.Book;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Market.SubMarketCode;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Order.OrderType;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.model.Venue.VenueType;
import it.softsolutions.jsscommon.Money;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class BookClassifiersTestHelper {

    /**
     * 
     * Please refer to  \\Samba\docs\Commesse_Interne\SS_904_BESTX fusione e trasformazione in prodotto\09 WIP\Test\TestCase Classificatori PD_PM-20120326_2_0.xls
     * 
     * 
     */
    private static Map<String, MarketMarketMaker> mmms_ = null;
    private static Map<String, Venue> venues_ = null;


    /**
     * Gets the order.
     *
     * @param qty the qty
     * @param side the side
     * @param limitPrice the limit price
     * @param settlementDays the settlement days
     * @param assetType the asset type
     * @param rtfiTicker the rtfi ticker
     * @return the order
     */
    public static Order getOrder(BigDecimal qty, OrderSide side, BigDecimal limitPrice, int settlementDays, String assetType, String rtfiTicker)
    {
        Order order = new Order();
        order.setQty(qty);
        order.setSide(side);
        order.setType(OrderType.MARKET);
        if (limitPrice != null) {
            order.setLimit(new Money("EUR", limitPrice));
            order.setType(OrderType.LIMIT);
        }

        Instrument instrument = new Instrument();
        instrument.setIsin("IT0000000001");
        instrument.setAssetType(assetType);
        instrument.setBondType(rtfiTicker);
        instrument.setCurrency("EUR");
        order.setInstrument(instrument);

        order.setFutSettDate(getDateWithDaysOffset(settlementDays));

        order.setExecutionDestination(null);

        return order;
    }

    private static Market getMarketByCode(MarketCode code)
    {
        Map<Long, Market> markets = getMarketsList();
        Market mkt = null;
        for (long mktId2 : markets.keySet()) {
            Market mkt2 = markets.get(mktId2);
            if (mkt2.getMarketCode().equals(code)) {
                mkt = mkt2;
                break;
            }
        }

        return mkt;
    }

    /**
     * Gets the new complete proposal.
     *
     * @param qty the qty
     * @param price the price
     * @param currency the currency
     * @param side the side
     * @param proposalType the proposal type
     * @param marketCode the market code
     * @param mmmBankMarketCode the mmm bank market code
     * @param venueCode the venue code
     * @param minutesAgo the minutes ago
     * @param settlementDays the settlement days
     * @return the new complete proposal
     */
    public static ClassifiedProposal getNewCompleteProposal(BigDecimal qty, BigDecimal price, String currency, ProposalSide side, 
                    ProposalType proposalType,
                    MarketCode marketCode, String mmmBankMarketCode, String venueCode, int minutesAgo, int settlementDays)
    {
        if (mmms_ == null)
            mmms_ = getMarketMarketMakersList();
        if (venues_ == null)
            venues_ = getVenuesList();


        ClassifiedProposal proposal = new ClassifiedProposal();
        proposal.setQty(qty);
        proposal.setSide(side);
        proposal.setProposalState(ProposalState.NEW);
        proposal.setType(proposalType);
        proposal.setPrice(new Money(currency, price));
        setProposalTimestampWithMinutesOffset(proposal, minutesAgo);
        proposal.setFutSettDate(getDateWithDaysOffset(settlementDays));


        // get venue by venueCode, and set it
        Venue venue = venues_.get(venueCode);
        Market mkt = null;
        if (venue.isIsMarket()) {
            mkt = venue.getMarket();
        }
        else {
            mkt = getMarketByCode(marketCode);
        }
        proposal.setMarket(mkt);
        proposal.setVenue(venue);

        // get mmm by Index is MarketId|BankMarketCode, and set it
        if (!mmmBankMarketCode.isEmpty()) {

            String mmmKey = Long.toString(mkt.getMarketId()) + "|" + mmmBankMarketCode;
            MarketMarketMaker mmm = mmms_.get(mmmKey);
            proposal.setMarketMarketMaker(mmm);
        }

        return proposal;
    }	

    /**
     * Gets the complete proposal.
     *
     * @param qty the qty
     * @param price the price
     * @param currency the currency
     * @param side the side
     * @param proposalType the proposal type
     * @param marketCode the market code
     * @param mmmCode the mmm code
     * @param venueCode the venue code
     * @param minutesAgo the minutes ago
     * @param settlementDays the settlement days
     * @param mmIsInPolicy the mm is in policy
     * @return the complete proposal
     */
    public static ClassifiedProposal getCompleteProposal(BigDecimal qty, BigDecimal price, String currency, ProposalSide side, 
                    ProposalType proposalType,
                    MarketCode marketCode, String mmmCode,
                    String venueCode, int minutesAgo, int settlementDays, boolean mmIsInPolicy)
    {
        // subMarketCode, mmCode and mmIsInPolicy not used!
        return getNewCompleteProposal(qty, price, currency, side, proposalType, marketCode, mmmCode, venueCode, minutesAgo, settlementDays);
    }

    private static int getMarketRanking(MarketCode mktCode, SubMarketCode subMktCode)
    {
        int ret = -1;
        if (mktCode.equals(MarketCode.MARKETAXESS))
            ret = 15;
        else if (mktCode.equals(MarketCode.BLOOMBERG))
            ret = 8;
        else if (mktCode.equals(MarketCode.TW))
            ret = 9;
        return ret;
    }


    /**
     * Gets the venue.
     *
     * @param MMCode the mM code
     * @return the venue
     */
    public static Venue getVenue(String MMCode)
    {
        Venue venue = new Venue();
        venue.setCode(MMCode);
        venue.setVenueType(VenueType.MARKET_MAKER);
        MarketMaker mm1 = new MarketMaker();
        mm1.setCode(MMCode);
        mm1.setMarketMarketMakers(null);
        venue.setMarketMaker(mm1);
        venue.getMarketMaker().setEnabled(true);

        return venue;
    }

    /**
     * Verify proposal state.
     *
     * @param proposalName the proposal name
     * @param book the book
     * @param mktCode the mkt code
     * @param subMktCode the sub mkt code
     * @param venueCode the venue code
     * @param mmmCode the mmm code
     * @param side the side
     * @param expectedState the expected state
     * @param expectedRejectMessage the expected reject message
     */
    public static void verifyProposalState(String proposalName, Book book, MarketCode mktCode, SubMarketCode subMktCode, String venueCode, String mmmCode, ProposalSide side, ProposalState expectedState, String expectedRejectMessage)
    {
        Collection<? extends Proposal> proposals;

        if (side == ProposalSide.BID)
            proposals = book.getBidProposals();
        else
            proposals = book.getAskProposals();

        //for (Proposal prop : proposals) {
        //	System.out.println("** Proposal dump: " + prop.toString());
        //}

        MarketCode propMktCode = null;
        SubMarketCode propSubMktCode = null;
        String propMmmCode = "";
        String propVenueCode = "";

        ClassifiedProposal proposal = null;
        // look for proposal, otherwise fail
        for (Proposal prop : proposals) {
            ClassifiedProposal clProp = (ClassifiedProposal)prop; 

            propMktCode = null;
            propSubMktCode = null;
            propMmmCode = "";

            boolean isMarket = clProp.getVenue() != null && clProp.getVenue().isIsMarket();
            if (isMarket) {
                /*
                 *  proposal ---> venue (MOTXMOT) ---> mkt (MOT/XMOT)
                 *            |                    |
                 *            |                    |-> MM (null)
                 *            |
                 *            |-> mkt (MOT/XMOT)
|*            |
                 *            |-> MMM (null)
                 *            
                 * 
                 */
                propMktCode =  clProp.getMarket().getMarketCode();
                propSubMktCode = clProp.getMarket().getSubMarketCode();
                propVenueCode = clProp.getVenue().getCode();
            }
            else {
                /*
                 *  proposal ---> venue (RABAX) ---> mkt (null)
                 *            |                  |
                 *            |                  |-> MM (RABAX) --> MMMs (null)
                 *            |
                 *            |-> mkt (BLOOMBERG/null)
				|*            |
                 *            |-> MMM (ABAX)---> MM (RABAX)
                 *                           |
                 *                           |-> mkt (BLOOMBERG/null)
                 *            
                 * 
                 */
                propMktCode =  clProp.getMarket().getMarketCode();
                propSubMktCode = clProp.getMarket().getSubMarketCode();
                propMmmCode = clProp.getMarketMarketMaker().getMarketSpecificCode();
                propVenueCode = clProp.getVenue().getCode();
            }

            if (!propMktCode.equals(mktCode))
                continue;	// not same proposal
            if ( ((propSubMktCode == null) && (subMktCode != null)) || ((propSubMktCode != null) && (subMktCode == null)) )
                continue;
            if ( (propSubMktCode != null) && (!propSubMktCode.equals(subMktCode)) )
                continue;	// not same proposal
            if (!propMmmCode.equals(mmmCode))
                continue;	// not same proposal
            if (!propVenueCode.equals(venueCode))
                continue;	// not same proposal

            /*			if (!clProp.getMarket().getMarketCode().equals(marketCode))
				continue;
			if ( (subMktCode != null) && (clProp.getMarket().getSubMarketCode() == null) )
				continue;
			if ( (subMktCode != null) && (!clProp.getMarket().getSubMarketCode().equals(subMktCode)) )
				continue;
			if (!mmmCode.isEmpty() && !clProp.getMarketMarketMaker().getMarketSpecificCode().equals(mmmCode))
				continue;
			if ( (clProp.getVenue().getMarketMaker() == null) && (!mmCode.isEmpty()) ) {
				continue;
			}
			if ( (clProp.getVenue().getMarketMaker() != null) && (clProp.getVenue().getMarketMaker().getCode() != null) && 
				 (!clProp.getVenue().getMarketMaker().getCode().equals(mmCode)) ) {
				continue;
			}
             */
            proposal = clProp;
            break;
        }

        if (proposal == null)
            fail("Could not find proposal: " + side.name() + " _- " + mktCode.name() + " - " + venueCode + " - " + mmmCode);


        if ( (expectedState == ProposalState.REJECTED) || (expectedState == ProposalState.DROPPED) ) {
            assertTrue("Proposal [" + proposalName + "] Invalid output state [" + proposal.getProposalState().name() + " - " + proposal.getReason() + "], expected [" + expectedState.name() + " - " + expectedRejectMessage + "]", proposal.getProposalState() == expectedState);
            boolean rejectMessageOk = false;
            if ( ((proposal.getReason() == null) || (proposal.getReason().isEmpty())) && expectedRejectMessage.isEmpty() ) {
                rejectMessageOk = true;
            }
            else if (proposal.getReason() != null && !expectedRejectMessage.isEmpty() && proposal.getReason().startsWith(expectedRejectMessage)) {
                rejectMessageOk = true;
            }
            assertTrue("Proposal [" + proposalName + "] Invalid discard message [" + proposal.getReason() + "], expected [" + expectedRejectMessage + "]", rejectMessageOk);
        }
        else {
            assertTrue("Proposal [" + proposalName + "] Invalid output state: " + proposal.getProposalState().name(), proposal.getProposalState() == expectedState);
        }
    }

    /**
     * 
     * Sets a timestamp with a defined offset (hours ago)
     *  
     */
    private static void setProposalTimestampWithMinutesOffset(ClassifiedProposal proposal, int minutesAgo)
    {
        Date minutesAgoDate = new Date();
        long time = minutesAgoDate.getTime();
        minutesAgoDate.setTime(minutesAgoDate.getTime()- minutesAgo * 60 * 1000);	
        time = minutesAgoDate.getTime();

        proposal.setTimestamp(minutesAgoDate);
    }

    /**
     * Gets the date with days offset.
     *
     * @param daysOffset the days offset
     * @return the date with days offset
     */
    public static Date getDateWithDaysOffset(int daysOffset)
    {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, daysOffset);

        return cal.getTime();
    }


    /**
     * Maps MarketTable to Markets
     * 
     * Index is MarketId.
     *
     * @return the markets list
     */
    public static Map<Long, Market> getMarketsList()
    {
        Map<Long, Market> mkts = new HashMap<Long, Market>();

        String[] mktNames = new String[] {"INTERNALIZZAZIONE","BLOOMBERG","RTFI","MOT","TLX","MATCHING","MOT","TLX","HIMTF","MOT","BV","TW","XBRIDGE"};
        String[] subMarketCodes = new String[] {"","","","MOT","TLX","","MEM","ETX","HIMTF","XMOT","","",""};
        long[] marketIds = new long[] {0,1,2,3,4,5,6,7,8,9,10,11,12};
        int[] rankings = new int[] {0,9,7,4,1,11,5,2,3,6,8,10,12};

        for (int i = 0; i < marketIds.length; i++) {
            Long mktId = marketIds[i];

            Market mkt = new Market();
            mkt.setMarketId(mktId);
            mkt.setMarketCode(MarketCode.valueOf(mktNames[i]));
            mkt.setSubMarketCode(subMarketCodes[i].isEmpty() ? null : SubMarketCode.valueOf(subMarketCodes[i]));
            mkt.setRanking(rankings[i]);

            mkts.put(mktId, mkt);
        }

        return mkts; 
    }

    /**
     * Maps BankTable to MarketMakers
     * 
     * Index is BankCode.
     *
     * @return the market makers list
     */
    public static Map<String, MarketMaker> getMarketMakersList()
    {
        Map<String, MarketMaker> mms = new HashMap<String, MarketMaker>();

        String[] bankCodes = new String[] {"ING","RABAX","RABN","RAKRO","RAKROSALES","RAKROSI","RAKROSPRIM","RAKROSSPEZ","RANZ","RAPERTA","RBARCC","RBARCL","RBAYON","RBPEX","RCALY","RCASSL","RCBANCA","RCG","RCOMM","RCONDUIT","RCREDEM","RCSFB","RCSFBL","RDAIWA","RDANSKE","RDEKA","RDEU","RDEXIA","RDRKW","RDZBANK","REAKROS","RFRTB","RGOLDMAN","RHELABA","RHSBC","RHYPO","RIING","RIMI","RIXIS","RJEFFERIES","RJP","RKBC","RKREDIET","RLBBW","RLEHM","RLEONARDO","RLOYDS","RMEDIO","RMELIOR","RMITSU","RMLI","RMOT","RMOTUS","RMPS","RMSTA","RNAB","RNOMURA","RPROFILO","RPROMOS","RRABO","RRBC","RRBOS","RRZB","RSCOTIA","RSETA","RSNS","RSOCGEN","RSTANDARD","RSYNESIS","RTAKROS","RTDB","RUBI","RUBS","RWESTLB","RZUERCHER","TEST21","TEST22","TEST23","TEST25","TPROMETEO","TRAD","ZION"};
        String[] bankNames = new String[] {"Ing obsoleto","Abax Bank","Abn","Banca Akros","akros sales","akros IS","Akros primario","Akros conto spezzature","Anz bank","BANCA APERTA","Barclays crediti","barclays","bayerishe","BNP Paribas","CALYON","cassa lombarda","Centro Banca","Citibank","Commerzbank","conduit","Credem","Credit Suisse","Credit Suisse Londra","DAIWA","DANSKE","deka","Deutsche Bank","dexia","Dresdner","DZ Bank","akros conto errori","Fortis Bank","goldman sachs","HELABA","Hsbc","HYPO","Ing","Banca IMI","Ixis","Jefferies","JP Morgan","Kbc","Kredietbank","LBBW","Lehman","Banca Leonardo","Lloyds","Mediobanca","Meliorbanca","mitsubischi","Merril Lynch","conto mot","motus","MPS","Morgan Stanley","Nat. australian bank","Nomura","Banca Profilo","promos","Rabo Bank","Royal Bank of Canada","Royal Bank of Scotland","rzb","Scotia Capital","Santander","SNS Securities","Societe' Generale","standard chartered bank","SYNESIS","Akros conto best","Toronto Dominion","ubi banca","Ubs","WESTLB","ZURCHER","TEST21","TEST22","TEST23","TEST25","prometeo","Trader","Zions Bank"};

        for (int i = 0; i < bankCodes.length; i++) {
            String bankCode = bankCodes[i];

            MarketMaker mm = new MarketMaker();
            mm.setCode(bankCode);
            mm.setName(bankNames[i]);
            mm.setEnabled(true);

            // to set later, upon mmms creation
            // mm.setMarketMarketMakers(marketMarketMakers);

            mms.put(bankCode, mm);
        }

        return mms; 
    }

    /**
     * Maps MarketBanks to MarketMarketMakers
     * 
     * Index is MarketId|BankMarketCode
     * 
     * Also adds MarketMarketMakers to corresponding MarketMaker.
     *
     * @return the market market makers list
     */
    public static Map<String, MarketMarketMaker> getMarketMarketMakersList()
    {
        Map<Long, Market> markets = getMarketsList();
        Map<String, MarketMaker> marketMakers = getMarketMakersList();

        Map<String, MarketMarketMaker> mmms = new HashMap<String, MarketMarketMaker>();

        long[] marketIds = new long[] {1,1,1,2,1,1,1,1,2,2,10,11,1,1,10,1,1,1,11,1,1,10,1,1,1,1,1,10,1,1,1,2,11,1,1,1,1,1,1,1,1,1,1,2,10,11,1,1,1,1,1,1,2,1,1,1,11,2,1,1,1,11,1,2,1,2,10,1,1,1,1,1,1,1,1,2,1,1,1,1,1,1,1,2,10,1,1,1,1,1,1,1,2,1,1,1,1,1,1,1,1,2,1,1,1,1,1,1,1,1,1,1,2,1,1,2,1,1,1,2,1,2,2,2,2,1,1,1,1,2,2,12};
        String[] marketCodes = new String[] {"BLOOMBERG","BLOOMBERG","BLOOMBERG","RTFI","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","RTFI","RTFI","BV","TW","BLOOMBERG","BLOOMBERG","BV","BLOOMBERG","BLOOMBERG","BLOOMBERG","TW","BLOOMBERG","BLOOMBERG","BV","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BV","BLOOMBERG","BLOOMBERG","BLOOMBERG","RTFI","TW","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","RTFI","BV","TW","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","RTFI","BLOOMBERG","BLOOMBERG","BLOOMBERG","TW","RTFI","BLOOMBERG","BLOOMBERG","BLOOMBERG","TW","BLOOMBERG","RTFI","BLOOMBERG","RTFI","BV","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","RTFI","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","RTFI","BV","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","RTFI","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","RTFI","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","RTFI","BLOOMBERG","BLOOMBERG","RTFI","BLOOMBERG","BLOOMBERG","BLOOMBERG","RTFI","BLOOMBERG","RTFI","RTFI","RTFI","RTFI","BLOOMBERG","BLOOMBERG","BLOOMBERG","BLOOMBERG","RTFI","RTFI","XBRIDGE"};
        String[] bankCodes = new String[] {"RABAX","RABN","RABN","RABN","RANZ","RIMI","RLEONARDO","RBARCL","RBARCL","RBARCL","RBARCL","RBARCL","RBARCC","RBARCC","RBARCC","RBAYON","RBPEX","RBPEX","RBPEX","RCALY","RCALY","RCALY","RCASSL","RCBANCA","RCG","RCG","RCG","RCG","RCOMM","RCOMM","RCOMM","RCOMM","RCOMM","RCREDEM","RCSFB","RCSFBL","RCSFBL","RCSFBL","RCSFBL","RDAIWA","RDANSKE","RDEKA","RDEU","RDEU","RDEU","RDEU","RDEXIA","RDEXIA","RDZBANK","RFRTB","RGOLDMAN","RGOLDMAN","RHELABA","RHSBC","RHSBC","RHSBC","RHSBC","RHYPO","RIING","RIING","RIING","RIING","RIXIS","RIXIS","RJEFFERIES","RJEFFERIES","RJEFFERIES","RJP","RJP","RKBC","RKREDIET","RLBBW","RLEHM","RLEHM","RLEHM","RLEHM","RLOYDS","RLOYDS","RMEDIO","RMELIOR","RMLI","RMLI","RMLI","RMLI","RMLI","RMITSU","RMSTA","RMSTA","RMSTA","RMSTA","RMSTA","RMSTA","RMSTA","RMPS","RNAB","RNOMURA","RNOMURA","RNOMURA","RRABO","RRABO","RRABO","RRABO","RRBC","RRBOS","RRBOS","RRZB","RRZB","RSETA","RSCOTIA","RSNS","RSOCGEN","RSTANDARD","TEST25","RTDB","RWESTLB","RWESTLB","ZION","ZION","RZUERCHER","RZUERCHER","RAKRO","RAKRO","TEST21","TEST22","TEST23","RUBS","RUBS","RUBS","RUBS","RUBS","RUBS","RAKRO"};
        String[] bankMarketCodes = new String[] {"ABAX","ABN","ABNV","ABNX","ANZI","IMIT","GBL","BARX","BART","BARX","23341SS1","DLRX","BARL","BCAP","95069BARB","BAYL","EBNP","BPRX","DLRY","CALY","CACB","35087INDOP","LOMB","CBAN","CGRT","CGC","CG","22133SBROL","CBKG","CBKF","CBK","CBKG","DLRW","CEMM","CSEZ","CSPF","CSEM","CSEG","CSEB","DSEL","DBEX","DEKA","DAB","DBAB","23340SS0","DLRZ","DEXB","DEXG","DZAG","FRTB","GS","GSEB","HELL","HSLA","HSET","HSBW","HSBC","HVBT","INGG","INGD","INGN","DLRP","NATX","NXIS","JEFX","JEFX","35232JILT","JPE","JPEX","KBCP","KBLU","LBBW","LBSF","LBEX","LBEU","LEHB","LLYD","LLOX","MEDX","MELI","ML","BMLL","BMLX","MLIL","35071MLI","MUSI","MSOL","MSTY","MSEB","MSEG","MSFI","MSPF","MSXL","MPNG","NABK","NOMX","NOMT","NOMS","RABO","RAB0","RABX","RABX","RBCL","RBSA","RBOS","RZBA","RZB","BSCX","SCI","SNSX","SG","SCBX","TS25","TDB","WLBG","WLBT","ZNBK","ZION","ZKB","ZKBE","AKRO","AKRX","TS21","TS22","TS23","UBSX","UBAP","UBSB","UBSZ","UBS","FUBS","AKRX"};
        String[] enabledFilters = new String[] {"Tutto","Tutto","Tutto","Tutto","Tutto","Tutto","Tutto","Non usare","Non usare","Non usare","Tutto","Tutto","Non Govies euro","Non Govies euro","Non Govies euro","Tutto","Non usare","Non usare","Tutto","Non usare","Non usare","Tutto","Tutto","Tutto","Non usare","Non usare","Non usare","Tutto","Non usare","Non usare","Non usare","Non usare","Tutto","Tutto","Non usare","Non usare","Non usare","Non usare","Non usare","Tutto","Tutto","Tutto","Tutto","Non usare","Tutto","Tutto","Tutto","Tutto","Tutto","Tutto","Non usare","Non usare","Tutto","Tutto","Tutto","Tutto","Non Govies euro","Non usare","Non Govies euro","Non Govies euro","Non Govies euro","Tutto","Tutto","Tutto","Non usare","Tutto","Tutto","Non usare","Non usare","Tutto","Tutto","Tutto","Tutto","Tutto","Tutto","Tutto","Tutto","Tutto","Tutto","Tutto","Non usare","Non usare","Non usare","Non Govies euro","Non Govies euro","Tutto","Non usare","Non usare","Non usare","Non usare","Non usare","Non usare","Non usare","Tutto","Tutto","Tutto","Tutto","Tutto","Non usare","Non usare","Non usare","Non usare","Tutto","Tutto","Tutto","Tutto","Tutto","Tutto","Tutto","Non Govies euro","Non usare","Tutto","Tutto","Tutto","Tutto","Tutto","Tutto","Tutto","Non Govies euro","Non usare","Tutto","Tutto","Tutto","Tutto","Tutto","Non usare","Non usare","Non usare","Non usare","Tutto","Tutto","Tutto"};

        for (int i = 0; i < marketIds.length; i++) {
            long marketId = marketIds[i];
            String bankMarketCode = bankMarketCodes[i];
            String key = Long.toString(marketId) + "|" + bankMarketCode;

            MarketMarketMaker mmm = new MarketMarketMaker();
            mmm.setMarketSpecificCode(bankMarketCodes[i]);
            mmm.setEnabledFilter(enabledFilters[i]);

            Market mkt = markets.get(marketId);
            mmm.setMarket(mkt);

            MarketMaker mm = marketMakers.get(bankCodes[i]);
            // add mmm to corresponding mm 

            if (mm.getMarketMarketMakers() == null)
                mm.setMarketMarketMakers(new HashSet<MarketMarketMaker>());
            mm.getMarketMarketMakers().add(mmm);
            mmm.setMarketMaker(mm);

            mmms.put(key, mmm);
        }

        return mmms; 
    }			

    /**
     * Maps ExecutionVenueTable to Venues
     * 
     * Index is ExecutionVenueCode.
     *
     * @return the venues list
     */
    public static Map<String, Venue> getVenuesList()
    {
        Map<Long, Market> markets = getMarketsList();
        Map<String, MarketMaker> marketMakers = getMarketMakersList();

        Map<String, Venue> venues = new HashMap<String, Venue>();

        String[] executionVenueCodes = new String[] {"BLOOMBERG","BV","HIMTF","ING","Internalizzazione","Matching","MOTMEM","MOTMOT","MOTXMOT","MTSCREDIT","RABAX","RABN","RAKRO","RAKROSALES","RBARCL","RBPEX","RCALY","RCASSL","RCG","RCOMM","RCSFB","RCSFBL","RDAIWA","RDANSKE","RDEKA","RDEU","RDEXIA","RDRKW","RDZBANK","REAKROS","RFRTB","RGOLDMAN","RHELABA","RHSBC","RHYPO","RIING","RIMI","RIXIS","RJEFFERIES","RJP","RKBC","RKREDIET","RLBBW","RLEHM","RMEDIO","RMELIOR","RMLI","RMPS","RMSTA","RNOMURA","RRABO","RRBC","RRBOS","RRZB","RSCOTIA","RSETA","RSNS","RSOCGEN","RSTANDARD","RTAKROS","RTDB","RTFI","RUBI","RUBS","RWESTLB","RZUERCHER","TEST21","TEST22","TEST25","TLXETX","TLXTLX","TRAD","TW","ZION"};
        String[] subMarketCodes = new String[] {"","","HIMTF","","","","MEM","MOT","XMOT","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","ETX","TLX","","",""};
        String[] bankCodes = new String[] {"","","","ING","","","","","","","RABAX","RABN","RAKRO","RAKROSALES","RBARCL","RBPEX","RCALY","RCASSL","RCG","RCOMM","RCSFB","RCSFBL","RDAIWA","RDANSKE","RDEKA","RDEU","RDEXIA","RDRKW","RDZBANK","REAKROS","RFRTB","RGOLDMAN","RHELABA","RHSBC","RHYPO","RIING","RIMI","RIXIS","RJEFFERIES","RJP","RKBC","RKREDIET","RLBBW","RLEHM","RMEDIO","RMELIOR","RMLI","RMPS","RMSTA","RNOMURA","RRABO","RRBC","RRBOS","RRZB","RSCOTIA","RSETA","RSNS","RSOCGEN","RSTANDARD","RTAKROS","RTDB","","RUBI","RUBS","RWESTLB","RZUERCHER","TEST21","TEST22","TEST25","","","TRAD","","ZION"};
        boolean[] isMarkets = new boolean[] {true,true,true,false,true,true,true,true,true,true,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,true,false,false,false,false,false,false,false,true,true,false,true,false};
        long[] marketIds = new long[] {1,10,8,0,0,5,6,3,9,13,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,7,4,0,11,0};

        for (int i = 0; i < executionVenueCodes.length; i++) {
            String executionVenueCode = executionVenueCodes[i];

            Venue venue = new Venue();
            venue.setCode(executionVenueCode);
            venue.setVenueType(isMarkets[i] ? VenueType.MARKET : VenueType.MARKET_MAKER);

            if (isMarkets[i]) {
                Market mkt = markets.get(marketIds[i]);
                venue.setMarket(mkt);
            }

            MarketMaker mm = marketMakers.get(bankCodes[i]);
            venue.setMarketMaker(mm);

            venues.put(executionVenueCode, venue);
        }

        return venues; 
    }	

    /**
     * Gets the venues list as set.
     *
     * @return the venues list as set
     */
    public static Set<Venue> getVenuesListAsSet()
    {
        Set<Venue> venuesSet = new HashSet<Venue>();
        Map<String, Venue> venuesMap = getVenuesList();
        Iterator iter = venuesMap.entrySet().iterator();

        for (String venueKey : venuesMap.keySet()) {
            venuesSet.add(venuesMap.get(venueKey));
        }

        return venuesSet;
    }

}
