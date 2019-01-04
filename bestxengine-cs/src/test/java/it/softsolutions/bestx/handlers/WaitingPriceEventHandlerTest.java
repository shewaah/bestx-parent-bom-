/*
* Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx.handlers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.price.PriceService;
import it.softsolutions.jsscommon.Money;

/**  

 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by: paolo.midali 
 * Creation date: 18/set/2013 
 * 
 **/
public class WaitingPriceEventHandlerTest {
    private WaitingPriceEventHandler handler;
    private static List<String> internalMMs = new ArrayList<String>();
    private static String imm1Code = "IMM1";
    private static String imm2Code = "IMM2";
    private static PriceService priceService;
    private static ClassPathXmlApplicationContext context;
    
    @BeforeClass
    public static void setUp() throws Exception {
        //@SuppressWarnings("resource")
        context = new ClassPathXmlApplicationContext("cs-spring.xml");
        priceService = (PriceService) context.getBean("normalPriceService");

        internalMMs.add(imm1Code);
        internalMMs.add(imm2Code);
    }


 
    private ClassifiedProposal createProposal(String mmmCode, MarketCode mmmMktCode, double price, double qty)
    {
        MarketMarketMaker mmm = new MarketMarketMaker();
        mmm.setMarketSpecificCode(mmmCode);
        Market mkt = new Market();
        mkt.setMarketCode(mmmMktCode);
        mmm.setMarket(mkt);
        MarketMaker mm = new MarketMaker();
        mm.setCode(mmmCode);
        mmm.setMarketMaker(mm);
        
        Venue venue = new Venue();
        ClassifiedProposal proposal = new ClassifiedProposal();
        
        proposal.setMarketMarketMaker(mmm);
        proposal.setMarket(mkt);
        proposal.setSide(ProposalSide.BID);
        proposal.setVenue(venue);
        proposal.setPrice(new Money("EUR", new BigDecimal(price)));
        proposal.setQty(new BigDecimal(qty));
        proposal.setProposalState(ProposalState.VALID);

        return proposal;
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        context.close();    Thread.sleep(1000);
    }
    
    
}
