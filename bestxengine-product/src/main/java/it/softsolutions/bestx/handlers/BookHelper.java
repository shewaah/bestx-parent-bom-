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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Rfq;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.jsscommon.Money;

/**  

 *
 * Purpose: this class is a helper for validation of prices against max spread (sanity check on the book). 
 *
 * Project Name : bestxengine-product 
 * First created by: paolo.midali 
 * Creation date: 04/set/2013 
 * 
 **/
public class BookHelper {

   private static final Logger LOGGER = LoggerFactory.getLogger(BookHelper.class);

   /**
    * @param sortedProposals
    * @param order
    * @param resultValues: double[2] array, contains the values used in calculation - resultValues[0] is the calculated limit, resultValues[1] is the configured (max acceptable) limit    
    * @return
    * @throws BestXException
    */
   public static boolean isSpreadOverTheMax(List<ClassifiedProposal> sortedProposals, Order order, double[] resultValues) throws BestXException {
      resultValues[0] = 0.0;
      resultValues[1] = 0.0;

      if (sortedProposals == null) {
         LOGGER.trace("{}, book is null, no need to check the wide quote spread ", order.getFixOrderId());
         return false;
      }
      if (sortedProposals.size() < 2) {
         LOGGER.trace("{}, book depth < 2, no need to check the wide quote spread", order.getFixOrderId());
         return false;
      }

      // retrieve configuration value from customer attributes
      CustomerAttributes custAttr = (CustomerAttributes) order.getCustomer().getCustomerAttributes();
      if (custAttr.getWideQuoteSpread() == null) {
         LOGGER.debug("{}, wide quote spread is null, cannot check the book validity", order.getFixOrderId());
         return false;
      }
      else if (custAttr.getWideQuoteSpread().doubleValue() == 0.0) {
         LOGGER.debug("{}, wide quote spread is 0, cannot check the book validity", order.getFixOrderId());
         return false;
      }
      else {
         // verify if (best - 2nd best) is larger than quoteSpread 
         double deltaPerc = getQuoteSpread(sortedProposals, 2);

         if (deltaPerc < custAttr.getWideQuoteSpread().doubleValue()) {
            LOGGER.debug("{}, Spread between best and second best ({}%) smaller than the customer [{}] configured maximum allowed ({}%)", order.getFixOrderId(), deltaPerc,
                  order.getCustomer().getName(), custAttr.getWideQuoteSpread());
            return false;
         }
         else {
            LOGGER.info("{}, Spread between best and second best ({}%) larger than the customer [{}] configured maximum allowed ({}%)", order.getFixOrderId(), deltaPerc, order.getCustomer().getName(),
                  custAttr.getWideQuoteSpread());
            resultValues[0] = deltaPerc;
            resultValues[1] = custAttr.getWideQuoteSpread().doubleValue();
            return true;
         }
      }
   }

   /**
    * gets the spread in the sorted proposal list between the best and the i-th proposal
    * @param sortedProposals
    * @param i the maximum level to be used
    * We expect bestPrice to be always a valid proposal
    * @return
    */
   public static double getQuoteSpread(List<ClassifiedProposal> sortedProposals, int i) {
      BigDecimal bestPrice = BigDecimal.ZERO;
      BigDecimal otherPrice = BigDecimal.ZERO;
      double deltaPerc = 0.0;
      ClassifiedProposal validProposal = getValidIthProposal(sortedProposals, 1);
      ClassifiedProposal acceptableProposal = getAcceptableIthProposal(sortedProposals, i);
      
      if (validProposal != null && acceptableProposal != null) {
         bestPrice = validProposal.getPrice().getAmount();
         otherPrice = getAcceptableIthProposal(sortedProposals, i).getPrice().getAmount();
         if (bestPrice.doubleValue() > 0) {
            double delta = Math.abs(bestPrice.doubleValue() - otherPrice.doubleValue());
            deltaPerc = (delta / bestPrice.doubleValue()) * 100;
         }
      }
      return deltaPerc;
   }

   /**
    * get the i-th proposal in the sorted proposal list
    * @param sortedProposals
    * @param i the maximum level to be used
    * @return
    */
   public static ClassifiedProposal getIthProposal(List<ClassifiedProposal> sortedProposals, int i) {
      if (sortedProposals.size() == 0)
         return null;
      if (sortedProposals.size() >= i && i > 0)
         return sortedProposals.get(i - 1);
      else return sortedProposals.get(sortedProposals.size() - 1);
   }

   /**
    * get the valid i-th proposal in the sorted proposal list
    * @param sortedProposals
    * @param i the maximum level to be used
    * @return
    */
   public static ClassifiedProposal getValidIthProposal(List<ClassifiedProposal> sortedProposals, int i) {
      if (sortedProposals.size() == 0)
         return null;
      
      if (sortedProposals.size() >= i && i > 0 && ProposalState.VALID == sortedProposals.get(i - 1).getProposalState())
         return sortedProposals.get(i - 1);
      else if (ProposalState.VALID == sortedProposals.get(sortedProposals.size() - 1).getProposalState())
         return sortedProposals.get(sortedProposals.size() - 1);
      else {
         for (int index = 0; i < sortedProposals.size(); index++)
            if (ProposalState.VALID != sortedProposals.get(index).getProposalState())
               return sortedProposals.get(index - 1);
      }
      return null;
   }
   
   public static ClassifiedProposal getAcceptableIthProposal(List<ClassifiedProposal> sortedProposals, int i) {
	   if (sortedProposals.size() == 0)
		   return null;
	   
	   if (sortedProposals.size() >= i && i > 0 && (ProposalState.VALID == sortedProposals.get(i - 1).getProposalState() ||
	         ProposalState.ACCEPTABLE == sortedProposals.get(i - 1).getProposalState()))
		   return sortedProposals.get(i - 1);
	   else if (ProposalState.VALID == sortedProposals.get(sortedProposals.size() - 1).getProposalState() || 
			   ProposalState.ACCEPTABLE == sortedProposals.get(sortedProposals.size() - 1).getProposalState())
		   return sortedProposals.get(sortedProposals.size() - 1);
	   else {
		   for (int index = 0; i < sortedProposals.size(); index++) {
			   ClassifiedProposal classifiedProposal = sortedProposals.get(index);
			   if (ProposalState.VALID != classifiedProposal.getProposalState() || 
					   ProposalState.ACCEPTABLE == classifiedProposal.getProposalState()) 
				   return sortedProposals.get(index - 1);
		   }
	   }
	   return null;
   }

   /**
    * Gets the spread between the best ask and the best bid price in the book
    * @param sortedAskProposals a list of valid ask prices sorted for ask side
    * @param sortedBidProposals a list of valid bid prices sorted for bid side 
    * @return the delta in percentage on the best ask price as an absolute value between best price in sortedAskProposals and best price in sortedBidProposals. Return -1 if one of the sides is empty
    */
   public static double getBidAskSpread(List<ClassifiedProposal> sortedAskProposals, List<ClassifiedProposal> sortedBidProposals) {
      BigDecimal bestAskPrice = null;
      BigDecimal bestBidPrice = null;
      if (sortedAskProposals == null || sortedAskProposals.size() <= 0 || sortedBidProposals == null || sortedBidProposals.size() <= 0) {
         return -1.0;
      }
      bestAskPrice = sortedAskProposals.get(0).getPrice().getAmount();
      bestBidPrice = sortedBidProposals.get(0).getPrice().getAmount();
      double delta = Math.abs(bestAskPrice.doubleValue() - bestBidPrice.doubleValue());
      double deltaPerc = (delta / bestAskPrice.doubleValue()) * 100;
      return deltaPerc;
   }

   private static BigDecimal ONE_HUNDRED = new BigDecimal("100.0");

   /** 
    * Widens the quote by wide spread according to the provided side
    * @param quote the original quote
    * @param wideSpread the widening spread
    * @param side BUY or SELL
    * @param maxPrice the price threshold which shall never be passed
    * @return
    */
   public static Money widen(Money quote, BigDecimal wideSpread, Rfq.OrderSide side, BigDecimal maxPrice) {
	   if (quote == null)
		   return null;
	   if (wideSpread == null || wideSpread.compareTo(BigDecimal.ZERO) <= 0)
		   return quote;
	   BigDecimal price = quote.getAmount();
	   BigDecimal spread = wideSpread.multiply(price).divide(ONE_HUNDRED);
	   if (side == OrderSide.BUY) {
		   if (maxPrice == null)
			   maxPrice = price.add(spread);
		   price = price.add(spread).min(maxPrice);
	   }
	   else if (side == OrderSide.SELL) {
		   if (maxPrice == null)
			   maxPrice = price.subtract(spread);
		   price = price.subtract(spread).max(maxPrice);
	   }
	   return new Money(quote.getStringCurrency(), price);
   }

   /**
    * Get the best proposal for the given market in the sorted book
    * @param book the source sorted book
    * @param marketCode the code of the market to get the be best proposal for
    * @param side the order side, relevant to get the ask or bid side of the sorted book
    * @return
    */
   public static ClassifiedProposal getBestPrice(SortedBook book, MarketCode marketCode, Rfq.OrderSide side) {
      if (book == null || marketCode == null || side == null)
         throw new IllegalArgumentException();
      List<ClassifiedProposal> sideProposals = (side == Rfq.OrderSide.BUY) ? book.getAskProposals() : book.getBidProposals();
      for (ClassifiedProposal prop : sideProposals) {
         if (marketCode.compareTo(prop.getMarket().getMarketCode()) == 0 && ProposalState.VALID == prop.getProposalState())
            return prop;
      }
      // there are no valid proposals for that market
      return null;
   }

   /**
    * Get the best proposal for the given market in the sorted book
    * @param book the source sorted book
    * @param lastMarketCode the code of the market currently used. If null there is no such thing
    * @param side the order side, relevant to get the ask or bid side of the sorted book
    * @return null if no such proposal is available, next proposal to be used otherwise
    */
   public static ClassifiedProposal getNextProposalAfterMarket(SortedBook book, List<MarketCode> lastMarketCodes, Rfq.OrderSide side) {
      if (book == null || side == null)
         throw new IllegalArgumentException();
      List<ClassifiedProposal> sideProposals = book.getValidSideProposals(side);
      int index = -1;
      for (ClassifiedProposal prop : sideProposals) {
         if (lastMarketCodes == null && ProposalState.VALID == prop.getProposalState())
            return prop;
         index++; // contains the index of the current proposal in the list sideProposals
         if (lastMarketCodes != null && lastMarketCodes.contains(prop.getMarket().getMarketCode()) && ProposalState.VALID == prop.getProposalState()) {
            // now I am ready to look for next MarketCode from now on
            for (ClassifiedProposal newProp : sideProposals.subList(index, sideProposals.size() - 1)) {
               if (!lastMarketCodes.contains(newProp.getMarket().getMarketCode()) && ProposalState.VALID == newProp.getProposalState())
                  return newProp;
            }
            break;
         }
      }
      // there are no more valid proposals
      return null;
   }
}
