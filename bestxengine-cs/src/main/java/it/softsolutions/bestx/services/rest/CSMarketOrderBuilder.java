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

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder;
import it.softsolutions.bestx.bestexec.MarketOrderBuilderListener;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketMarketMakerSpec;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.rest.dto.ExceptionMessageElement;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest.BidAsk;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest.ConsolidatedBookElement;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest.MarketDataSource;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest.PriceQuality;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest.Side;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalResponse;
import it.softsolutions.jsscommon.Money;

/**
 *
 * Purpose: this class is creating the MarketOrder with the results of the Algo REST service
 * NOTE - While it uses market makers code specific to the market, it is possible that some market makers associated to a dealer code in BLP have more dealer codes, possibly different, in TSOX
 * This is managed adding all the TSOX dealer code associated to a MarketMaker for Bloomberg/TSOX. The duplications that can arise from this management are also managed by deleting from the include list the codes already present in the exclude list
 * 
 * Project Name : bestxengine-cs First created by: stefano.pontillo Creation
 * date: 27 lug 2021
 * 
 **/
public class CSMarketOrderBuilder extends MarketOrderBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(CSMarketOrderBuilder.class);

	private CSAlgoRestService csAlgoService;
	private MarketFinder marketFinder;

	private Executor executor;
	
	public CSMarketOrderBuilder() {
		super();
	}

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
		elem.setDataSource(
				MarketDataSource.valueOf(proposal.getMarket().getEffectiveMarket().getMarketCode().toString()));
		elem.setMarketMakerCode(proposal.getMarketMarketMaker().getMarketMaker().getCode());
		if (proposal.getType() == ProposalType.TRADEABLE && proposal.getAuditQuoteState() != null) {
			elem.setQuoteStatus(Optional.of(proposal.getAuditQuoteState()));
		}
		return elem;
	}

	@Override
	public void buildMarketOrder(Operation operation, MarketOrderBuilderListener listener) {
		this.executor.execute(() -> {
			try {
				MarketOrder marketOrder = new MarketOrder();
				Attempt currentAttempt = operation.getLastAttempt();
				Market market;

				GetRoutingProposalRequest request = new GetRoutingProposalRequest();
				request.setIsin(operation.getOrder().getInstrumentCode());
				request.setSide(OrderSide.isBuy(operation.getOrder().getSide()) ? Side.BUY : Side.SELL);
				request.setPriceTypeFIX(1); // Only price type supported for the moment
				request.setSize(operation.getOrder().getQty());
				request.setLegalEntity(this.csAlgoService.getAlgoServiceName());

				for (ClassifiedProposal proposal : currentAttempt.getSortedBook().getAskProposals()) {
					if (proposal.getProposalState() == ProposalState.ACCEPTABLE
							|| proposal.getProposalState() == ProposalState.VALID) {
						request.getConsolidatedBook().add(this.buildConsolidatedBookElement(BidAsk.ASK, proposal));
					}
				}
				for (ClassifiedProposal proposal : currentAttempt.getSortedBook().getBidProposals()) {
					if (proposal.getProposalState() == ProposalState.ACCEPTABLE
							|| proposal.getProposalState() == ProposalState.VALID) {
						request.getConsolidatedBook().add(this.buildConsolidatedBookElement(BidAsk.BID, proposal));
					}
				}

				GetRoutingProposalResponse response = csAlgoService.doGetRoutingProposal(request);  //TODO should not propagate the exception if there is an error in the message

				List<String> errors = new ArrayList<>();

				if (response.getData().getExceptions() != null) {
					for (ExceptionMessageElement elem : response.getData().getExceptions()) {
						if ("WARN".equalsIgnoreCase(elem.getExceptionSeverity())) {
							LOGGER.warn("Warning received from ALGO REST Service for order {}: {} {}",
									operation.getOrder().getFixOrderId(), elem.getExceptionCode(), elem.getExceptionMessage());
						} else if ("ERROR".equalsIgnoreCase(elem.getExceptionSeverity())) {
							errors.add(elem.getExceptionCode() + ": "+ elem.getExceptionMessage());
						}
					}
				}

				if (errors.isEmpty()) {

					Money limitPrice = new Money(operation.getOrder().getCurrency(),
							response.getData().getTargetPrice());
					marketOrder.setValues(operation.getOrder());
					marketOrder.setTransactTime(DateService.newUTCDate());

					market = marketFinder.getMarketByCode(Market.MarketCode.valueOf(response.getData().getTargetVenue().toString()), null);
					marketOrder.setMarket(market);

					// Manage include dealers and exclude dealers
					List<String> includeDealersResp = response.getData().getIncludeDealers();
					List<String> excludeDealersResp = response.getData().getExcludeDealers();
					List<MarketMarketMaker> excludeDealers = new ArrayList<MarketMarketMaker>(); // list to use to exclude from includeDealers the MM specified in excludeDealers list 
					// - Note may be due especially for TSOX dealer codes
					List<MarketMarketMakerSpec> includeDealersSpecs = new ArrayList<MarketMarketMakerSpec>();
					List<MarketMarketMakerSpec> excludeDealersSpecs = new ArrayList<MarketMarketMakerSpec>();
					
					// add all dealer codes returned as excludeDealers to the marketOrder.excludeDealers list
					// note that if the returned market is Bloomberg we need to be smart ad add all the dealer codes in TSOX for that dealer
					for(int i = 0; i < excludeDealersResp.size();i++) {
						MarketMarketMaker mmm = this.getMarketMakerFinder().getSmartMarketMarketMakerByCode(
								market.getMarketCode(), excludeDealersResp.get(i));
						if(mmm == null) continue;
						if(!Market.isABBGMarket(market.getMarketCode())) {
							excludeDealers.add(mmm);
						}
						else {
							List<MarketMarketMaker> mmList = mmm.getMarketMaker().getMarketMarketMakerForMarket(MarketCode.TSOX);
							java.util.function.Consumer<? super MarketMarketMaker> addmmsToExcludeList = mmm2 -> excludeDealers.add(mmm2);
							mmList.forEach(addmmsToExcludeList);
						}
					}
					excludeDealers.forEach(marketMarketMaker ->excludeDealersSpecs.add(new MarketMarketMakerSpec(marketMarketMaker.getMarketSpecificCode(), marketMarketMaker.getMarketSpecificCodeSource())));
					
					// add all dealer codes returned as includeDealers to the marketOrder.dealers list
					// note that if the returned market is Bloomberg we need to be smart ad add all the dealer codes in TSOX for that dealer
					for(int i = 0; i < includeDealersResp.size();i++) {
						MarketMarketMaker mmm = this.getMarketMakerFinder().getSmartMarketMarketMakerByCode(
								market.getMarketCode(), includeDealersResp.get(i));
						if(mmm == null) continue;
						if(excludeDealers.contains(mmm)) {
							LOGGER.info("Duplication found in list of MarketMakers between include and exclude lists. Code: {}", mmm.getMarketSpecificCode());
							continue;
						}
						if(!Market.isABBGMarket(market.getMarketCode())) {
							includeDealersSpecs.add(new MarketMarketMakerSpec(mmm.getMarketSpecificCode(), mmm.getMarketSpecificCodeSource()));
							}
							else {
								List<MarketMarketMaker> mmList = mmm.getMarketMaker().getMarketMarketMakerForMarket(MarketCode.TSOX);
								java.util.function.Consumer<? super MarketMarketMaker> addmmsToIncludeList = mmm1 -> {
												if(!excludeDealers.contains(mmm1))  // Add only dealer codes of dealers that are not already in the exclude list
													includeDealersSpecs.add(new MarketMarketMakerSpec(mmm1.getMarketSpecificCode(), mmm1.getMarketSpecificCodeSource()));
								};
								mmList.forEach(addmmsToIncludeList);
							}
					}
					//remove dealer codes relative to composite prices
					this.removeCompositePricesFromList(excludeDealersSpecs, market.getMarketCode());
					marketOrder.setExcludeDealers(excludeDealersSpecs);			
					//remove dealer codes relative to composite prices
					this.removeCompositePricesFromList(includeDealersSpecs, market.getMarketCode());
					marketOrder.setDealers(includeDealersSpecs);

					
					marketOrder.setLimit(limitPrice);

					LOGGER.info("Order={}, Selecting for execution market market makers: {} and price {}. Excluding dealers {}",
							operation.getOrder().getFixOrderId(), marketOrder.beautify(marketOrder.getDealers()),
							limitPrice == null ? "null" : limitPrice.getAmount(),  marketOrder.beautify(marketOrder.getExcludeDealers()));
					listener.onMarketOrderBuilt(this, marketOrder);
				} else {
					listener.onMarketOrderErrors(this, errors);
				}

			} catch (Exception e) {
				if (e.getCause() instanceof SocketTimeoutException) {
					LOGGER.error("Timeout to call ALGO REST Service", e);
					listener.onMarketOrderTimeout(this);
				} else {
					LOGGER.error("Error while creating Market Order", e);
					listener.onMarketOrderException(this, e);
				}
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
