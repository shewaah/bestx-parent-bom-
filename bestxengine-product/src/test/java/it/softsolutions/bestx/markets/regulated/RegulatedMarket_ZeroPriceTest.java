/*
 * Project Name : BestXEngine_common
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author$
 * Date         : $Date$
 * Header       : $Id$
 * Revision     : $Revision$
 * Source       : $Source$
 * Tag name     : $Name$
 * State        : $State$
 *
 * For more details see cvs web documentation for project: BestXEngine_common
 */
package it.softsolutions.bestx.markets.regulated;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.regulated.RegulatedProposalInputLazyBean;
import it.softsolutions.bestx.helpers.__FakeMarketFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.xt2.protocol.XT2Msg;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

/**
 * This Unit testing class tests if, while processing the proposal received from MOT or
 * TLX, we set the best price to zero. If so we are in error.
 * All these methods call the RegulatedMarket method getConsolidatedProposal. Every one 
 * uses a different book.
 * 
 * There is also a test on the building of the proposals timestamp. It has been created 
 * only for the MOT market, because the TLX does not send us time and date.
 * 
 * @author ruggero.rizzo
 *
 */
public class RegulatedMarket_ZeroPriceTest
{
   
   private __TestableRegulatedMarket market;
   
   /**
    * Testing the processing of a book with some prices to zero and the remaining over
    * the limit.
    * The expected result is a MOT/TLX proposal with a valid price even if over the limit.
    * It will be discarded while the aggregated book of all the market will be classified.
    */
   @Test
   public void testTestGetConsolidatedProposalPricesZero()
   {
      market = new __TestableRegulatedMarket();
      
      __FakeMarketFinder marketFinder = new __FakeMarketFinder();
      market.setMarketFinder(marketFinder);
      
      ProposalSide side = ProposalSide.ASK; 
      OrderSide orderSide = OrderSide.BUY;
      BigDecimal limitPrice = new BigDecimal("96.1");
      
      __FakeXT2MessageBuilderForTLXAndMOT fakeBookBuilder = new __FakeXT2MessageBuilderForTLXAndMOT();
      XT2Msg fakeMsg = fakeBookBuilder.getPricesToZeroOrOverTheLimitMsg(limitPrice, side, "TLXFIX", "ETX");
      
      ArrayList<RegulatedProposalInputLazyBean> proposalsPricesToZeroOrOverTheLimit = new ArrayList<RegulatedProposalInputLazyBean>();
      for (int index = 1; index <= 5; index ++)
         proposalsPricesToZeroOrOverTheLimit.add(new RegulatedProposalInputLazyBean(fakeMsg, index));
      
      Money orderPriceLimit = new Money("EUR", BigDecimal.valueOf(96.1));
      BigDecimal quantity = new BigDecimal(10000);
      String instrumentCurrency = "EUR";
      ClassifiedProposal result = null;
      try
      {
         result = market.testGetConsolidatedProposal(side, orderSide, proposalsPricesToZeroOrOverTheLimit, orderPriceLimit, quantity, instrumentCurrency);
      }
      catch (BestXException e)
      {
         org.junit.Assert.fail("Method called failed, exception " + e.getCause());
         e.printStackTrace();
      }
      System.out.println("(Book with zeroes or prices over the limit) Result " + result);
      BigDecimal propPrice = result.getPrice().getAmount();
      boolean areEqualss = false;
      if (BigDecimal.ZERO.compareTo(propPrice) == 0)
         areEqualss = true;
      org.junit.Assert.assertFalse("(Book with zeroes or prices over the limit)Proposal price equal to zero : " + propPrice, areEqualss);
   }

   /**
    * Testing the processing of a book with all the prices over the limit.
    * The expected result is a MOT/TLX proposal with a valid price even if over the limit.
    * It will be discarded while the aggregated book of all the market will be classified.  
    */
   @Test
   public void testTestGetConsolidatedProposalPricesOverLimit()
   {
      market = new __TestableRegulatedMarket();
      
      __FakeMarketFinder marketFinder = new __FakeMarketFinder();
      market.setMarketFinder(marketFinder);
      
      ProposalSide side = ProposalSide.ASK; 
      OrderSide orderSide = OrderSide.BUY;
      BigDecimal limitPrice = new BigDecimal("96.1");
      
      __FakeXT2MessageBuilderForTLXAndMOT fakeBookBuilder = new __FakeXT2MessageBuilderForTLXAndMOT();
      XT2Msg fakeMsg = fakeBookBuilder.getPricesOverTheLimitMsg(limitPrice, side, "TLXFIX", "ETX");
      
      ArrayList<RegulatedProposalInputLazyBean> proposalsPricesOverTheLimit = new ArrayList<RegulatedProposalInputLazyBean>();
      for (int index = 1; index <= 5; index ++)
         proposalsPricesOverTheLimit.add(new RegulatedProposalInputLazyBean(fakeMsg, index));
      
      Money orderPriceLimit = new Money("EUR", BigDecimal.valueOf(96.1));
      BigDecimal quantity = new BigDecimal(10000);
      String instrumentCurrency = "EUR";
      ClassifiedProposal result = null;
      try
      {
         result = market.testGetConsolidatedProposal(side, orderSide, proposalsPricesOverTheLimit, orderPriceLimit, quantity, instrumentCurrency);
      }
      catch (BestXException e)
      {
         org.junit.Assert.fail("Method called failed, exception " + e.getCause());
         e.printStackTrace();
      }
      System.out.println("(Book with prices over the limit) Result " + result);
      BigDecimal propPrice = result.getPrice().getAmount();
      boolean areEqualss = false;
      if (BigDecimal.ZERO.compareTo(propPrice) == 0)
         areEqualss = true;
      org.junit.Assert.assertFalse("(Book with prices over the limit)Proposal price equal to zero : " + propPrice, areEqualss);
   }
   
   /**
    * Testing the processing of a book with all the prices within the limit.
    * The expected result is a MOT/TLX proposal with a valid price.
    */
   @Test
   public void testTestGetConsolidatedProposalAllPricesValid()
   {
      market = new __TestableRegulatedMarket();
      
      __FakeMarketFinder marketFinder = new __FakeMarketFinder();
      market.setMarketFinder(marketFinder);
      
      ProposalSide side = ProposalSide.ASK; 
      OrderSide orderSide = OrderSide.BUY;
      BigDecimal limitPrice = new BigDecimal("96.1");
      
      __FakeXT2MessageBuilderForTLXAndMOT fakeBookBuilder = new __FakeXT2MessageBuilderForTLXAndMOT();
      XT2Msg fakeMsg = fakeBookBuilder.getPricesWithinTheLimitMsg(limitPrice, side, "TLXFIX", "ETX");
      
      ArrayList<RegulatedProposalInputLazyBean> proposalsPricesWithinLimit = new ArrayList<RegulatedProposalInputLazyBean>();
      for (int index = 1; index <= 5; index ++)
         proposalsPricesWithinLimit.add(new RegulatedProposalInputLazyBean(fakeMsg, index));
      
      Money orderPriceLimit = new Money("EUR", BigDecimal.valueOf(96.1));
      BigDecimal quantity = new BigDecimal(10000);
      String instrumentCurrency = "EUR";
      ClassifiedProposal result = null;
      try
      {
         result = market.testGetConsolidatedProposal(side, orderSide, proposalsPricesWithinLimit, orderPriceLimit, quantity, instrumentCurrency);
      }
      catch (BestXException e)
      {
         org.junit.Assert.fail("Method called failed, exception " + e.getCause());
         e.printStackTrace();
      }
      System.out.println("(Book with prices valid) Result " + result);
      BigDecimal propPrice = result.getPrice().getAmount();
      boolean areEqualss = false;
      if (BigDecimal.ZERO.compareTo(propPrice) == 0)
         areEqualss = true;
      org.junit.Assert.assertFalse("(Book with prices valid)Proposal price equal to zero : " + propPrice, areEqualss);
   }
   
   @Test
   public void testMOTGetConsolidatedProposalAllForTimestampBuilding()
   {
      market = new __TestableRegulatedMarket();
      
      __FakeMarketFinder marketFinder = new __FakeMarketFinder();
      market.setMarketFinder(marketFinder);

      __FakeXT2MessageBuilderForTLXAndMOT fakeBookBuilder = new __FakeXT2MessageBuilderForTLXAndMOT();
      XT2Msg fakeMsg = fakeBookBuilder.getMOTPricesWithDateAndTime();
      Money orderPriceLimit = new Money("EUR", BigDecimal.valueOf(96.1));
      BigDecimal quantity = new BigDecimal(10000);
      
      ArrayList<RegulatedProposalInputLazyBean> proposals = new ArrayList<RegulatedProposalInputLazyBean>();
      for (int index = 1; index <= 5; index ++)
         proposals.add(new RegulatedProposalInputLazyBean(fakeMsg, index));

      ProposalSide side = ProposalSide.ASK; 
      OrderSide orderSide = OrderSide.BUY;

      ClassifiedProposal result = null;
      try
      {
         result = market.testGetConsolidatedProposal(side, orderSide, proposals, orderPriceLimit, quantity, "EUR");
      }
      catch (BestXException e)
      {
         org.junit.Assert.fail("Method called failed, exception " + e.getCause());
         e.printStackTrace();
      }

      Calendar testCalendar = Calendar.getInstance();
      //months start from 0, so 10 = November
      testCalendar.set(2011, 10, 10, 12, 36, 8);
      Date orderDate = testCalendar.getTime();
      System.out.println("(MOT timestamp) Order time " + orderDate);
      
      Date proposalDate = result.getTimestamp();
      System.out.println("(MOT timestamp) Proposal time " + proposalDate);
      long deltaInMillis = Math.abs(proposalDate.getTime() - orderDate.getTime());
      //tolerate 5 seconds of difference between MOT price and order timestamps
      boolean propAfterOrder = deltaInMillis <= 5000;
      
      org.junit.Assert.assertTrue("(MOT timestamp) Proposal timestamp differs from the order timestamp for more than 5 seconds", propAfterOrder);
   }
}
