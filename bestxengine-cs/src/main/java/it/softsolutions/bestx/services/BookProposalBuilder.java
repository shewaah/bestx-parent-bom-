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
package it.softsolutions.bestx.services;

import java.math.BigDecimal;
import java.util.List;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.jsscommon.Money;

/**
 * When we do not receive proposals from a market or a market maker we must notify
 * it.
 * What we can do is build "fake" proposals and put them in the book, this way, when
 * the trader will load the order detail, he will see :
 * - for a market that did not list the instrument "Instrument not listed"
 * - for a market maker that did not quote the instrument "Instrument not quoted"
 * 
 * This is importat, because the trader can show to the customer that every possible
 * trading venue has been enquired in order to place the order.
 * 
 * @author ruggero.rizzo
 *
 */
public class BookProposalBuilder
{
   public MarketFinder marketFinder;
   public VenueFinder venueFinder;
   
   public void init()
   {
      checkPrerequisites();
   }
   
   private void checkPrerequisites()
   {
      if (marketFinder == null)
      {
         throw new ObjectNotInitializedException("MarketFinder not set");
      }
      if (venueFinder == null)
      {
         throw new ObjectNotInitializedException("VenueFinder not set");
      }
   }
   
   /**
    * We build an empty proposal for instruments not listed in a market or not quoted by a market maker.
    * Any kind of reason could be given.
    * @param marketCode : the code of the market
    * @param mmm : if not null it is the market market maker not quoting the instrument
    * @param order : the order
    * @param reason : the rejection reason
    * @return the REJECTED proposal with the right reason
    * @throws Exception
    */
   public ClassifiedProposal buildEmptyProposalWithReason(MarketCode marketCode, MarketMarketMaker mmm, Order order, String reason) throws Exception
   {
      ClassifiedProposal clProposal = new ClassifiedProposal();
      
      /* Find the right market for this proposal :
       * - could be in the MarketSecurityStatus table (we have only the market code, but we
       *   must find the specific market/submarket couple)
       * - if not, could be found through the market code (it is not a market with submarkets)
       * 
       * If nothing has been found, throws an exception.
       */
      Market market = null;
      
      List<Market> markets = marketFinder.getMarketsByCode(marketCode);
      if (markets != null)
      {
         market = markets.get(0);
      }
      
      if (market != null)
      {
         clProposal.setMarket(market);
         clProposal.setProposalState(ProposalState.REJECTED);
         clProposal.setReason(reason);
         
         if (mmm != null)
         {
            clProposal.setMarketMarketMaker(mmm);
            clProposal.setVenue(venueFinder.getMarketMakerVenue(mmm.getMarketMaker()));
         }
         else
         {
            clProposal.setVenue(venueFinder.getMarketVenue(market));
         }
         clProposal.setQty(BigDecimal.ZERO);
         clProposal.setPrice(new Money(order.getCurrency(), BigDecimal.ZERO));
      }
      else
      {
         throw new BestXException("Cannot find a market with code " + marketCode.toString());
      }
      return clProposal;
   }
   public MarketFinder getMarketFinder()
   {
      return marketFinder;
   }

   public void setMarketFinder(MarketFinder marketFinder)
   {
      this.marketFinder = marketFinder;
   }

   public VenueFinder getVenueFinder()
   {
      return venueFinder;
   }

   public void setVenueFinder(VenueFinder venueFinder)
   {
      this.venueFinder = venueFinder;
   }
}
