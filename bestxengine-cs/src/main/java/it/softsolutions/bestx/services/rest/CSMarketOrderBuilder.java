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

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder;
import it.softsolutions.bestx.bestexec.MarketOrderBuilderListener;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest;
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

   @Override
   public void buildMarketOrder(Operation operation) {
	   this.executor.execute(() -> {
	      MarketOrder marketOrder = new MarketOrder();
	      Attempt currentAttempt = operation.getLastAttempt();
	
	      GetRoutingProposalRequest request = new GetRoutingProposalRequest();
	      request.setIsin(operation.getOrder().getInstrumentCode());
	      
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


@Override
   public boolean getServiceStatus() {
      return csAlgoService.isConnected();
   }
}
