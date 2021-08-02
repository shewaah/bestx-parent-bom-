/*
* Copyright 1997-2021 SoftSolutions! srl 
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
 
package it.softsolutions.bestx.services.rest;

import java.util.Optional;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest.BidAsk;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest.ConsolidatedBookElement;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest.LegalEntity;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest.MarketDataSource;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest.PriceQuality;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest.PriceTypeFIX;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest.Side;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalResponse;
import it.softsolutions.jsscommon.Money;


/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-cs 
* First created by: stefano.pontillo 
* Creation date: 27 lug 2021 
* 
**/
public class CSMarketOrderBuilder implements MarketOrderBuilder {

   private static final Logger LOGGER = LoggerFactory.getLogger(CSMarketOrderBuilder.class);
   
   private CSAlgoRestService csAlgoService;
   private MarketFinder marketFinder;
   private MarketMakerFinder marketMakerFinder;
   
   private Executor executor;

   private ConsolidatedBookElement buildConsolidatedBookElement(BidAsk bidAsk, ClassifiedProposal proposal) {
	  ConsolidatedBookElement elem = new ConsolidatedBookElement();
	  elem.setBidAsk(bidAsk);
	  elem.setPrice(proposal.getPrice().getAmount());
	  elem.setSize(proposal.getQty());
	  elem.setDateTime(proposal.getTimestamp());
	  switch (proposal.getType()) {
	  case INDICATIVE:
		  elem.setPriceQuality(PriceQuality.IND);
		  break;
	  case COMPOSITE:
		  elem.setPriceQuality(PriceQuality.CMP);
		  break;
	  case TRADEABLE:
		  elem.setPriceQuality(PriceQuality.FRM);
		  break;
	  case AXE:
		  elem.setPriceQuality(PriceQuality.AXE);
		  break;
	  default: // What to do in this case?
	  }
	  elem.setDealerAtVenue(proposal.getMarketMarketMaker().getMarketSpecificCode());
	  elem.setDataSource(MarketDataSource.valueOf(proposal.getMarket().getEffectiveMarket().getMarketCode().toString()));
	  elem.setMarketMakerCode(proposal.getMarketMarketMaker().getMarketMaker().getCode());
	  if (proposal.getType() == ProposalType.TRADEABLE && proposal.getAuditQuoteState() != null) {
		  elem.setQuoteStatus(Optional.of(proposal.getAuditQuoteState()));
	  }	   
	  return elem;
   }
   
   @Override
   public void buildMarketOrder(Operation operation) {
	   this.executor.execute(() -> {
		   try {
		      MarketOrder marketOrder = new MarketOrder();
		      Attempt currentAttempt = operation.getLastAttempt();
		
		      GetRoutingProposalRequest request = new GetRoutingProposalRequest();
		      request.setIsin(operation.getOrder().getInstrumentCode());
		      request.setSide(OrderSide.isBuy(operation.getOrder().getSide()) ? Side.BUY : Side.SELL);
		      request.setPriceTypeFIX(PriceTypeFIX.PERCENTAGE_OF_PAR); // Only price type supported for the moment
		      request.setSize(operation.getOrder().getQty());
		      request.setLegalEntity(LegalEntity.ZRH); // FIXME
		      
		      for (ClassifiedProposal proposal : currentAttempt.getSortedBook().getAskProposals()) {
		    	  if (proposal.getProposalState() == ProposalState.ACCEPTABLE ||
		    			  proposal.getProposalState() == ProposalState.VALID) {
		    		  request.getConsolidatedBook().add(this.buildConsolidatedBookElement(BidAsk.ASK, proposal));
		    	  }
		      }
		      for (ClassifiedProposal proposal : currentAttempt.getSortedBook().getBidProposals()) {
		    	  if (proposal.getProposalState() == ProposalState.ACCEPTABLE ||
		    			  proposal.getProposalState() == ProposalState.VALID) {
		    		  request.getConsolidatedBook().add(this.buildConsolidatedBookElement(BidAsk.BID, proposal));
		    	  }
		      }
		      
		      GetRoutingProposalResponse response = csAlgoService.doGetRoutingProposal(request);
		
		      try {
		         Money limitPrice = new Money(operation.getOrder().getCurrency(), response.getData().getTargetPrice());
		         marketOrder.setValues(operation.getOrder());
		         marketOrder.setTransactTime(DateService.newUTCDate());
		         
		         marketOrder.setMarket(marketFinder.getMarketByCode(Market.MarketCode.valueOf(response.getData().getTargetVenue().toString()), null));
		         MarketMarketMaker mmMaker = marketMakerFinder.getMarketMarketMakerByCode(marketOrder.getMarket().getMarketCode(), response.getData().getIncludeDealers().get(0));
		         marketOrder.setMarketMarketMaker(mmMaker);
		         marketOrder.setLimit(limitPrice);
		         
		         LOGGER.info("Order={}, Selecting for execution market market maker: {} and price {}", operation.getOrder().getFixOrderId(), marketOrder.getMarketMarketMaker(), limitPrice == null? "null":limitPrice.getAmount().toString());
		         marketOrder.setVenue(currentAttempt.getExecutionProposal().getVenue());
		      }
		      catch (BestXException e) {
		         e.printStackTrace();
		      }
		
		         
		      operation.onMarketOrderBuilt(this, marketOrder);
		   } catch (Exception e) {
			   LOGGER.error("Error while creating Market Order", e);
			   operation.onMarketOrderBuilt(this, null);
		   }
	   });
   }

   
   public CSAlgoRestService getCsAlgoService() {
      return csAlgoService;
   }
   
   public void setCsAlgoService(CSAlgoRestService csAlgoService) {
      this.csAlgoService = csAlgoService;
   }
   
   public MarketFinder getMarketFinder() {
      return marketFinder;
   }
   
   public void setMarketFinder(MarketFinder marketFinder) {
      this.marketFinder = marketFinder;
   }
   
   public MarketMakerFinder getMarketMakerFinder() {
      return marketMakerFinder;
   }
   
   public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
      this.marketMakerFinder = marketMakerFinder;
   }

	public Executor getExecutor() {
		return executor;
	}

	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

   public boolean getServiceStatus() {
      return csAlgoService.isConnected();
   }

   public String getDownReason() {
      return csAlgoService.getLastError();
   }

   public String getServiceName() {
      return csAlgoService.getConnectionName();
   }
}
