
/*
 * Copyright 2016-2025 SoftSolutions! srl 
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.model.Venue.VenueType;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceCallback;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.price.PriceService;
import it.softsolutions.bestx.states.CurandoAutoState;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WarningState;
/**
*
* Purpose: this class is mainly for managing price discovery requests
*
* Project Name : bestxengine-cs
* First created by: simone.belleli
* Creation date: 26/lug/2016
* 
**/
public class PriceDiscoveryEventHandler extends BaseOperationEventHandler implements ExecutionStrategyServiceCallback {

	private static final long serialVersionUID = -1785784963434610799L;
	private static final Logger LOGGER = LoggerFactory.getLogger(PriceDiscoveryEventHandler.class);

	private static long priceDiscoveryRequests = 0;


	private final PriceService priceService;
	private final long marketPriceTimeout;
	private Customer priceDiscoveryCustomer;
	private Order order;
	private int bookDepth = 5;
	private int priceDecimals = 6;
	
	private OperatorConsoleConnection operatorConsoleConnection;
	
	public PriceDiscoveryEventHandler(Operation operation, PriceService priceService, long marketPriceTimeout, Customer priceDiscoveryCustomer, int bookDepth, int priceDecimals, OperatorConsoleConnection operatorConsoleConnection) {
		super(operation);
		this.priceDiscoveryCustomer = priceDiscoveryCustomer;
		this.priceService = priceService;
		this.marketPriceTimeout = marketPriceTimeout;
		this.bookDepth = bookDepth;
		this.priceDecimals = priceDecimals;
		this.operatorConsoleConnection = operatorConsoleConnection;
	}

	@Override
	public void onNewState(OperationState currentState) {
		LOGGER.debug("PriceDiscoveryEventHandler entry action");
		order = operation.getOrder();
		
		if(operation.getAttempts() == null) {
			operation.setAttempts(new ArrayList<Attempt>());
		}
		operation.addAttempt();
		operation.getOrder().setCustomer(priceDiscoveryCustomer);

		Set<Venue> venues = null;

		if (priceDiscoveryCustomer.getPolicy() != null) {
			venues = new HashSet<Venue>();
			for (Venue venue : priceDiscoveryCustomer.getPolicy().getVenues()) {
				if (venue.getVenueType().equals(VenueType.MARKET) || venue.getMarketMaker() != null && venue.getMarketMaker().isEnabled()) {
					venues.add(venue);
				}
			}
		} else {
			LOGGER.error("Customer {} with no policy assigned.", priceDiscoveryCustomer.getFixId());
			operation.removeLastAttempt();
			operation.setStateResilient(new WarningState(currentState, null, Messages.getString("CustomerWithoutPolicy.0", priceDiscoveryCustomer.getName(), priceDiscoveryCustomer.getFixId())), ErrorState.class);
			return;
		}

		try {
			priceService.requestPrices(operation, order, operation.getAttempts(), venues, marketPriceTimeout, -1, null);
		} catch (MarketNotAvailableException mnae) {
			LOGGER.error("An error occurred while calling Price Service", mnae);
			return;
		} catch (BestXException e) {
			LOGGER.error("An error occurred while calling Price Service for operationID={}: {}", operation.getId(), e.getMessage(), e);
			operation.setStateResilient(new CurandoAutoState(), ErrorState.class);
		}

		// AMC 20160729 probabilmente i valori di monitoraggio di questa PD non sono gli stessi delle altre PD
		priceDiscoveryRequests++;
		LOGGER.info("[MONITOR] price discovery requests: {}", priceDiscoveryRequests);
	}
	
	@Override
	public void onPricesResult(PriceService source, PriceResult priceResult) {
		if (priceResult.getState() == PriceResult.PriceResultState.UNAVAILABLE) {
			PriceDiscoveryHelper.publishEmptyBook(operation, operatorConsoleConnection);
		} else {
			PriceDiscoveryHelper.publishPriceDiscovery(operation, priceResult, operatorConsoleConnection, bookDepth, priceDecimals, false);
		}
	}
	
}
