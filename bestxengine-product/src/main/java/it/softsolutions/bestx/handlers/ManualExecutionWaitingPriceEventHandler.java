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
package it.softsolutions.bestx.handlers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.RegulatedMktIsinsLoader;
import it.softsolutions.bestx.appstatus.ApplicationStatus;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder;
import it.softsolutions.bestx.dao.OperationStateAuditDao;
import it.softsolutions.bestx.datacollector.DataCollector;
import it.softsolutions.bestx.exceptions.CustomerRevokeReceivedException;
import it.softsolutions.bestx.finders.CustomerFinder;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.model.Venue.VenueType;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.ExecutionDestinationService;
import it.softsolutions.bestx.services.MarketOrderFilterChain;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.price.PriceService;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.ManualManageState;
import it.softsolutions.bestx.states.WarningState;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: davide.rossoni Creation date: 19/ott/2012
 * 
 **/
public class ManualExecutionWaitingPriceEventHandler extends WaitingPriceEventHandler {

    private static final long serialVersionUID = 7118232704250606529L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ManualExecutionWaitingPriceEventHandler.class);

    private static long manualExecutionPriceRequests;

    /**
     * @param operation
     * @throws BestXException
     */
    public ManualExecutionWaitingPriceEventHandler(Operation operation, PriceService priceService, CustomerFinder customerFinder,
            SerialNumberService serialNumberService, RegulatedMktIsinsLoader regulatedMktIsinsLoader,
            List<String> regulatedMarketPolicies, long waitingPriceDelay, int maxAttemptNo, long marketPriceTimeout,
            ExecutionDestinationService executionDestinationService, boolean doNotExecute, 
            OperationStateAuditDao operationStateAuditDao, ApplicationStatus applicationStatus, 
            DataCollector dataCollector, MarketOrderBuilder marketOrderBuilder, MarketOrderFilterChain marketOrderFilterChain) throws BestXException {

        super(operation, priceService, customerFinder, serialNumberService, regulatedMktIsinsLoader, 
                regulatedMarketPolicies, waitingPriceDelay, maxAttemptNo, marketPriceTimeout, executionDestinationService, 
                doNotExecute, null, operationStateAuditDao, applicationStatus, dataCollector, marketOrderBuilder, marketOrderFilterChain);
            }

    @Override
    public void onNewState(OperationState currentState) {
        LOGGER.debug("{} ManualExecutionWaitingPriceState entry action", operation.getOrder().getFixOrderId());
        operation.getLastAttempt().setByPassableForVenueAlreadyTried(true);
        operation.addAttempt();
        operation.setNoProposalsOrderOnBook(false);

        Instrument instrument = operation.getOrder().getInstrument();
        if (instrument != null) {
            Set<Venue> venues = null;
            Customer customer = operation.getOrder().getCustomer();
            if (customer.getPolicy() != null) {
                venues = new HashSet<Venue>();
                for (Venue venue : customer.getPolicy().getVenues()) {
                    if (venue.getVenueType() == VenueType.MARKET || venue.getMarketMaker() != null && venue.getMarketMaker().isEnabled()) {
                        venues.add(venue);
                    }
                }
            } else {
                LOGGER.error("Customer {} with no policy assigned.", customer.getFixId());
                operation.removeLastAttempt();
                operation.setStateResilient(new WarningState(currentState, null, Messages.getString("CustomerWithoutPolicy.0", customer.getName(), customer.getFixId())), ErrorState.class);
                return;
            }
            LOGGER.info("[COUNTER_PRICE_REQ]");
            // reset della flag dei counter multipli di RTFI
            LOGGER.debug("[RTFICNT] Setting flag to false");
            operation.setProcessingCounter(false);
            Order order = operation.getOrder();

            try {
                // 20110407 AMC il caso della gestione manuale non deve passare dalla accettazione automatica della revoca.
                LOGGER.debug("{} ManualExecutionWaitingPrice Price service call.", operation.getOrder().getFixOrderId());
                priceService.requestPrices(operation, order, operation.getValidAttempts(), venues, marketPriceTimeout, 0, null);
                LOGGER.debug("{} ManualExecutionWaitingPrice Price service call exited.", operation.getOrder().getFixOrderId());
            } catch (CustomerRevokeReceivedException crre) { // must not be thrown
                LOGGER.info("We received a customer revoke while starting the price discovery: ignoring it and leaving the user to take any proper action.");
            } catch (BestXException e) {
                LOGGER.error("An error occurred while calling Price Service", e);
                operation.removeLastAttempt();
                operation.setStateResilient(new WarningState(currentState, e, Messages.getString("PriceService.14")), ErrorState.class);
                return;
            }
            if (waitingPriceDelay == 0) {
                LOGGER.error("No delay set for price wait. Risk of stale state");
            } else {
                setupDefaultTimer(waitingPriceDelay, false);
            }
            manualExecutionPriceRequests++;
            LOGGER.info("[MONITOR] Manual execution price requests: {}", manualExecutionPriceRequests);
        } else {
            operation.setStateResilient(new ManualManageState(false), ErrorState.class);
        }
    }

    @Override
    public void onPricesResult(PriceService source, PriceResult priceResult) {
        LOGGER.debug("{} ManualExecutionWaitingPrice Price result received: {}", operation.getOrder().getFixOrderId(), priceResult.getState());

        long time = DateService.currentTimeMillis() - operation.getState().getEnteredTime().getTime();
        LOGGER.info("[STATISTICS],Order={},OrderArrival={},PriceDiscoverStart={},PriceDiscoverStop={},TimeDiffMillis={}",
                operation.getOrder().getFixOrderId(), DateService.format(DateService.timeFIX, operation.getOrder().getTransactTime()), 
                	DateService.format(DateService.timeFIX, operation.getState().getEnteredTime()), 
                		DateService. format(DateService.timeFIX, DateService.newLocalDate()), time);
        priceService.addNewTimePriceDiscovery(time);

        stopDefaultTimer();
        
        Attempt currentAttempt = operation.getLastAttempt();
        currentAttempt.setSortedBook(priceResult.getSortedBook());
        if (priceResult.getState() != PriceResult.PriceResultState.INCOMPLETE) {
        	operation.setStateResilient(new ManualManageState(true), ErrorState.class);
        } else {
        	operation.removeLastAttempt();
        	operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("EventPriceTimeout.0", priceResult.getReason())), ErrorState.class);
        }

    }
}
