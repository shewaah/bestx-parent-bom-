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

package it.softsolutions.bestx.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder;
import it.softsolutions.bestx.bestexec.MarketOrderBuilderListener;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.jsscommon.Money;

/**
 *
 * Purpose: this class is the standard product way BestX adopts to create a MarketOrder for execution attempts
 * It uses the consolidated book created in the price discovery step as the only source of data
 *
 * Project Name : bestxengine-product First created by: stefano.pontillo
 * Creation date: 27 lug 2021
 * 
 **/
public class BestXMarketOrderBuilder extends MarketOrderBuilder {
	
	public BestXMarketOrderBuilder() {
	   super();
	   //super("Default");
	}


	private static final Logger LOGGER = LoggerFactory.getLogger(BestXMarketOrderBuilder.class);

	private TargetPriceCalculator targetPriceCalculator;

	@Override
	public void buildMarketOrder(Operation operation, MarketOrderBuilderListener listener) {
		MarketOrder marketOrder = null;
		Attempt currentAttempt = operation.getLastAttempt();

		SortedBook sortedBook = currentAttempt.getSortedBook();
		
		List<ClassifiedProposal> consideredProposals = new ArrayList<>();
		consideredProposals.addAll(sortedBook.getValidSideProposals(operation.getOrder().getSide()));
		consideredProposals.addAll(sortedBook.getProposalBySubState(Arrays.asList(ProposalSubState.PRICE_WORST_THAN_LIMIT), operation.getOrder().getSide()));

		ClassifiedProposal marketOrderProposal = !consideredProposals.isEmpty() ? consideredProposals.get(0) : null;
		
		if (marketOrderProposal != null) {
			marketOrder = new MarketOrder();
			marketOrder.setValues(operation.getOrder());
			marketOrder.setTransactTime(DateService.newUTCDate());
			marketOrder.setMarket(marketOrderProposal.getMarket());
			Money limitPrice = this.targetPriceCalculator.calculateTargetPrice(operation);
			marketOrder.setLimit(limitPrice != null ? limitPrice : marketOrderProposal.getPrice());
			String cleanMarketName = marketOrder.getMarket().getName().indexOf("_HIST") >= 0
					? marketOrder.getMarket().getName().substring(0, marketOrder.getMarket().getName().indexOf("_HIST"))
					: marketOrder.getMarket().getName(); // TODO Probably use effective market?
			
			if (limitPrice != null) {
				 if(limitPrice.getStringCurrency() != null) {
						limitPrice = new Money(limitPrice.getStringCurrency(), MarketOrder.beautifyBigDecimal(limitPrice.getAmount(), 1, 5));
					} else if (limitPrice.getCurrency() != null) {
					limitPrice = new Money(limitPrice.getCurrency(), MarketOrder.beautifyBigDecimal(limitPrice.getAmount(), 1, 5));
				} 
			}
			
			marketOrder.setLimitMonitorPrice(marketOrder.getLimit());
			
			LOGGER.info("Order={}, Selecting for execution market: {}, and price {}", operation.getOrder().getFixOrderId(),
					cleanMarketName, marketOrder.getLimit() == null ? "null" :  marketOrder.getLimit().getAmount());
			marketOrder.setBuilder(this);
		}
		
		listener.onMarketOrderBuilt(this, marketOrder);
	}

	public TargetPriceCalculator getTargetPriceCalculator() {
		return targetPriceCalculator;
	}

	public void setTargetPriceCalculator(TargetPriceCalculator targetPriceCalculator) {
		this.targetPriceCalculator = targetPriceCalculator;
	}

}
