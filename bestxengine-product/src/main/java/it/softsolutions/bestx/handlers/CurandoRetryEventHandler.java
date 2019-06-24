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

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.dao.OperationStateAuditDao;
import it.softsolutions.bestx.exceptions.CustomerRevokeReceivedException;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Portfolio;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.BookDepthValidator;
import it.softsolutions.bestx.services.ExecutionDestinationService;
import it.softsolutions.bestx.services.TitoliIncrociabiliService;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyService;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyService.Result;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceCallback;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceFactory;
import it.softsolutions.bestx.services.logutils.ApplicationStatisticsHelper;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.price.PriceService;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.CurandoAutoState;
import it.softsolutions.bestx.states.CurandoState;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.LimitFileNoPriceState;
import it.softsolutions.bestx.states.ManualExecutionWaitingPriceState;
import it.softsolutions.bestx.states.OrderNotExecutableState;
import it.softsolutions.bestx.states.SendAutoNotExecutionReportState;
import it.softsolutions.bestx.states.SendNotExecutionReportState;
import it.softsolutions.bestx.states.WaitingPriceState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.autocurando.AutoCurandoStatus;
import it.softsolutions.bestx.states.internal.deprecated.INT_StartExecutionState;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.manageability.sl.monitoring.NumericValueMonitor;

/**
 * 
 * 
 * Purpose: this class manages events in the CurandoRetry state
 * 
 * Project Name : bestxengine-product First created by: stefano.pontillo Creation date: 18/mag/2012
 * 
 **/
@Deprecated
public class CurandoRetryEventHandler extends BaseOperationEventHandler implements ExecutionStrategyServiceCallback {

   private static final long serialVersionUID = -5505068293102619787L;
   private static final Logger LOGGER = LoggerFactory.getLogger(CurandoRetryEventHandler.class);
   // Monitorable
   private static long curandoPriceRequests = 0;
   private static NumericValueMonitor curandoPriceRequestsMonitor = new NumericValueMonitor("curandoPriceRequests", "Price Service", true, "info", "[PRICE_SERVICE_STATISTICS]");

   private final SerialNumberService serialNumberService;
   private final PriceService priceService;
   private final long waitingPriceDelay;
   private final long marketPriceTimeout;
   @SuppressWarnings("unused")
   private final ExecutionDestinationService executionDestinationService;
   private int maxAttemptNo;
   private PriceResult priceResult;
   private boolean rejectOrderWhenBloombergIsBest;
   private BookDepthValidator bookDepthValidator;
   private OperationStateAuditDao operationStateAuditDao;
   private boolean doNotExecute;
   private int targetPriceMaxLevel;

   /**
    * Instantiates a new curando retry event handler.
    *
    * @param operation the operation
    * @param priceService the price service
    * @param titoliIncrociabiliService the titoli incrociabili service
    * @param marketFinder the market finder
    * @param venueFinder the venue finder
    * @param timerService the timer service
    * @param serialNumberService the serial number service
    * @param waitingPriceDelay the waiting price delay
    * @param maxAttemptNo the max attempt no
    * @param marketPriceTimeout the market price timeout
    * @param executionDestinationService the execution destination service
    * @param rejectOrderWhenBloombergIsBest the reject order when bloomberg is best
    * @param doNotExecute parameter used to decide if execute a LimitFile type order
    * @param bookDepthValidator the book depth validator customer specific
    * @param operationRegistry the operation registry
    * @throws BestXException the best x exception
    */
   public CurandoRetryEventHandler(Operation operation, PriceService priceService, TitoliIncrociabiliService titoliIncrociabiliService, MarketFinder marketFinder, VenueFinder venueFinder,
         SerialNumberService serialNumberService, long waitingPriceDelay, int maxAttemptNo, long marketPriceTimeout, ExecutionDestinationService executionDestinationService,
         boolean rejectOrderWhenBloombergIsBest, boolean doNotExecute, BookDepthValidator bookDepthValidator, OperationStateAuditDao operationStateAuditDao, int targetPriceMaxLevel)
         throws BestXException{
      super(operation);
      this.priceService = priceService;
      this.waitingPriceDelay = waitingPriceDelay;
      this.serialNumberService = serialNumberService;
      this.marketPriceTimeout = marketPriceTimeout;
      this.executionDestinationService = executionDestinationService;
      this.maxAttemptNo = maxAttemptNo;
      this.rejectOrderWhenBloombergIsBest = rejectOrderWhenBloombergIsBest;
      this.bookDepthValidator = bookDepthValidator;
      this.operationStateAuditDao = operationStateAuditDao;
      this.doNotExecute = doNotExecute;
      this.targetPriceMaxLevel = targetPriceMaxLevel;
   }

   @Override
   public void onNewState(OperationState currentState) {
      LOGGER.debug("CurandoRetryState entry action");

      int totalCurandoPriceRequests = AutoCurandoStatus.INSTANCE.incTotalCurandoPriceRequestsNumber();
      LOGGER.info("[CurandoRetry] operationID = {}, TotalCurandoPriceRequestsNumber : {}", operation.getId(), totalCurandoPriceRequests);
      if (customerSpecificHandler != null)
         customerSpecificHandler.onNewState(currentState);

      Customer customer = operation.getOrder().getCustomer();
      Set<Venue> venues = selectVenuesForPriceDiscovery(customer);
      if (venues == null) {
         LOGGER.error("Order {}, Customer {} with no policy assigned.", operation.getOrder().getFixOrderId(), customer.getFixId());
         operation.removeLastAttempt();
         operation.setStateResilient(new WarningState(currentState, null, Messages.getString("CustomerWithoutPolicy.0", customer.getName(), customer.getFixId())), ErrorState.class);
      }

      Order order = operation.getOrder();

      try {
         priceService.requestPrices(operation, order, operation.getAttempts(), venues, marketPriceTimeout, -1, null);
      }
      catch (MarketNotAvailableException mnae) {
         LOGGER.error("An error occurred while calling Price Service", mnae);
         checkOrderAndsetNotAutoExecuteOrder(operation, doNotExecute);
         // AMC verify useless code?
         //            // Create the execution strategy with a null priceResult, we did not receive any price
         //            try {
         //                ExecutionStrategyService csExecutionStrategyService = ExecutionStrategyServiceFactory.getInstance().getExecutionStrategyService(operation.getOrder().getPriceDiscoveryType(), operation, null, rejectOrderWhenBloombergIsBest);
         //            } catch (BestXException e) {
         //                LOGGER.error("Order {}, error while managing no market available situation {}", order.getFixOrderId(), e.getMessage(), e);
         //                operation.removeLastAttempt();
         //                operation.setStateResilient(new WarningState(currentState, e, Messages.getString("PriceService.15")), ErrorState.class);
         //            }
         return;
      }
      catch (CustomerRevokeReceivedException crre) {
         LOGGER.info("We received a customer revoke while starting the price discovery, we will not do it and instead start the revoking routine.");
         // if we correctly manage a revoke we can force a return to avoid the creation of the timer
         if (checkCustomerRevoke(order)) {
            return;
         }

      }
      catch (BestXException e) {
         LOGGER.error("An error occurred while calling Price Service for operationID={}: {}", operation.getId(), e.getMessage(), e);
         operation.setStateResilient(new CurandoAutoState(), ErrorState.class);
      }
      curandoPriceRequests++;
      curandoPriceRequestsMonitor.setValue(curandoPriceRequests);
      LOGGER.info("[MONITOR] curando price requests: {}", curandoPriceRequests);
   }

   @Override
   public void startTimer() {
      if (waitingPriceDelay == 0) {
         LOGGER.error("No delay set for price wait. Risk of stale state");
      }
      else {
         // this is a Price Discovery timer, not a Limit File one
         setupDefaultTimer(waitingPriceDelay, false);
      }
   }

   @Override
   public void onTimerExpired(String jobName, String groupName) {
      String handlerJobName = super.getDefaultTimerJobName();

      if (jobName.equals(handlerJobName)) {
         if (operation.isStopped())
            return;
         //price discovery timer expiration
         LOGGER.debug("Order {} : Timer: {} expired.", operation.getOrder().getFixOrderId(), jobName);
         //CS: CurandoAuto will manage the order with the OrderNotExecutableEventHandler
         operation.setStateResilient(new CurandoAutoState(), ErrorState.class);
      }
      else {
         super.onTimerExpired(jobName, groupName);
      }
   }

   @Override
   public void onPricesResult(PriceService source, PriceResult priceResult) {
      LOGGER.debug("Price result received: {}", priceResult.getState());

      stopDefaultTimer();
      if (operation.isStopped())
         return;

      this.priceResult = priceResult;
      
      LOGGER.debug("End of the price discovery, check if we received a customer revoke for this order and, if so, start the revoking routine.");
      if (checkCustomerRevoke(operation.getOrder())) {
         //BESTX-377: add always book to attempt
         operation.addAttempt();
         Attempt currentAttempt = operation.getLastAttempt();
         currentAttempt = operation.getLastAttempt();
         // update the currentAttempt variable
         currentAttempt.setSortedBook(priceResult.getSortedBook());
         
         LOGGER.info("Order {}, end of the price discovery, customer revoke received for this order. Start the cancel routine.", operation.getOrder().getFixOrderId());
         return;
      }
      LOGGER.debug("Order {}, No customer revoke received.", operation.getOrder().getFixOrderId());

      // [RR20120912] BXM-109 initializing the execution strategy service
      ExecutionStrategyService csExecutionStrategyService = ExecutionStrategyServiceFactory.getInstance().getExecutionStrategyService(operation.getOrder().getPriceDiscoveryType(), operation,
            priceResult, rejectOrderWhenBloombergIsBest);
      //        CSNormalExecutionStrategyService csExecutionStrategyService = new CSNormalExecutionStrategyService(this, priceResult, rejectOrderWhenBloombergIsBest);
      Order order = operation.getOrder();
      
      /* BXMNT-327 */
      if (!bookDepthValidator.isBookDepthValid(operation.getLastAttempt(), order) && !order.isLimitFile()) {
         try {
            ExecutionReportHelper.prepareForAutoNotExecution(operation, serialNumberService, ExecutionReportState.REJECTED);
            operation.setStateResilient(new SendAutoNotExecutionReportState(Messages.getString("RejectInsufficientBookDepth.0", bookDepthValidator.getMinimumRequiredBookDepth())), ErrorState.class);
         }
         catch (BestXException e) {
            LOGGER.error("Order {}, error while starting automatic not execution.", operation.getOrder().getFixOrderId(), e);
            String errorMessage = e.getMessage();
            operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);
         }
         return;
      }

      operation.addAttempt();
      Attempt currentAttempt = operation.getLastAttempt();
      currentAttempt = operation.getLastAttempt();
      // update the currentAttempt variable
      currentAttempt.setSortedBook(priceResult.getSortedBook());

      
      
      
      /*
       * BXMNT-326: go to Manual Curando, instead of limit file (a.k.a. automatic curando a.k.a. OrderNotExecutable)
       * TO BE REMOVED WHEN RELEASED, ONLY FOR TEST!
       */
      //        if (operation.getAttemptNo() > 10000) {
      //            LOGGER.info("Max number of attempts reached.");
      //            currentAttempt.setByPassableForVenueAlreadyTried(true);
      //            operation.setStateResilient(new CurandoState(Messages.getString("EventNoMoreRetry.0")), ErrorState.class);
      //            return;
      //        }

      if (priceResult.getState() == PriceResult.PriceResultState.COMPLETE) {
         // Fill Attempt and Build MarketOrder
         currentAttempt.setExecutionProposal(currentAttempt.getSortedBook().getBestProposalBySide(operation.getOrder().getSide()));
         MarketOrder marketOrder = new MarketOrder();
         currentAttempt.setMarketOrder(marketOrder);
         marketOrder.setValues(operation.getOrder());
         marketOrder.setMarket(currentAttempt.getExecutionProposal().getMarket());
         marketOrder.setMarketMarketMaker(currentAttempt.getExecutionProposal().getMarketMarketMaker());
         LOGGER.info("Selecting for execution market market maker: {}", currentAttempt.getMarketOrder().getMarketMarketMaker());

         if (currentAttempt.getExecutionProposal() != null) {
            marketOrder.setMarketMarketMaker(currentAttempt.getExecutionProposal().getMarketMarketMaker());
            Money limitPrice = null;
            Money ithbest = null;
            ClassifiedProposal ithBestProp = null;
            Money best = null;
            try {
               best = currentAttempt.getSortedBook().getBestProposalBySide(operation.getOrder().getSide()).getPrice();
               ithBestProp = BookHelper.getValidIthProposal(currentAttempt.getSortedBook().getSideProposals(operation.getOrder().getSide()), this.targetPriceMaxLevel);
               ithbest = ithBestProp.getPrice();
            }
            catch (NullPointerException e) {
               LOGGER.debug("NullPointerException trying to manage widen best or get {}-th best for order {}", this.targetPriceMaxLevel, order.getFixOrderId());
            }
            try {
               double spread = BookHelper.getQuoteSpread(currentAttempt.getSortedBook().getValidSideProposals(operation.getOrder().getSide()), this.targetPriceMaxLevel);
               CustomerAttributes custAttr = (CustomerAttributes) order.getCustomer().getCustomerAttributes();
               BigDecimal customerMaxWideSpread = custAttr.getWideQuoteSpread();
               if (customerMaxWideSpread != null && customerMaxWideSpread.doubleValue() < spread) { // must use the spread, not the i-th best
                  limitPrice = BookHelper.widen(best, customerMaxWideSpread, operation.getOrder().getSide(), order.getLimit() == null ? null : order.getLimit().getAmount());
                  LOGGER.info("Order {}: widening market order limit price {}. Max wide spread is {} and spread between best {} and {}-th best {} has been calculated as {}", order.getFixOrderId(),
                        limitPrice == null ? " N/A" : limitPrice.getAmount().toString(), customerMaxWideSpread == null ? " N/A" : customerMaxWideSpread.toString(),
                        best == null ? " N/A" : best.getAmount().toString(), this.targetPriceMaxLevel, ithbest == null ? " N/A" : ithbest.getAmount().toString(), spread);
               }
               else {// use i-th best
                  limitPrice = ithbest;
               }
               if (limitPrice == null) { // necessary to avoid null limit price. See the book depth minimum for execution 
                  if (currentAttempt.getExecutionProposal().getWorstPriceUsed() != null) {
                     limitPrice = currentAttempt.getExecutionProposal().getWorstPriceUsed();
                     LOGGER.debug("Use worst price of consolidated proposal as market order limit price: {}", limitPrice.getAmount().toString());
                  }
                  else {
                     limitPrice = currentAttempt.getExecutionProposal() == null ? null : currentAttempt.getExecutionProposal().getPrice();
                     if(limitPrice == null) {
                    	 LOGGER.error("No i-th best - limitPrice is null");
                     }
                     else {
                    	 LOGGER.debug("No i-th best - Use proposal as market order limit price: {}", limitPrice.getAmount().toString());
                     }
                  }
               }
               else LOGGER.debug("Use less wide between i best proposal and best widened by {} as market order limit price: {}", customerMaxWideSpread, limitPrice.getAmount().toString());
            }
            catch (Exception e) {

               e.printStackTrace();
            }
            marketOrder.setLimit(limitPrice);
            LOGGER.info("Order={}, Selecting for execution market market maker: {} and price {}", operation.getOrder().getFixOrderId(), marketOrder.getMarketMarketMaker(), limitPrice);
         }

         // Is this an instrument to be internalized?
         Portfolio portfolio = null;
         if (operation.getOrder().getInstrument().getInstrumentAttributes() != null) {
            portfolio = operation.getOrder().getInstrument().getInstrumentAttributes().getPortfolio();
         }

         ApplicationStatisticsHelper.logStringAndUpdateOrderIds(operation.getOrder(), "Order.Execution_" + source.getPriceServiceName() + "." + operation.getOrder().getInstrument().getIsin(),
               this.getClass().getName());

         // if operation must be internalized
         if (portfolio != null && portfolio.isInternalizable() && !operation.hasBeenInternalized()) {
            operation.setStateResilient(new INT_StartExecutionState(Messages.getString("Curando_Venue_Found.0")), ErrorState.class);
         }
         else {
            // operation must not be internalized and is a limit file with no autoexecution capabilities
            if (order.isLimitFile() && doNotExecute) {
               LOGGER.info("Order {} could be executed, but BestX is configured to no execute limit file orders.", order.getFixOrderId());
               currentAttempt.getMarketOrder().setVenue(currentAttempt.getExecutionProposal().getVenue());
               setNotAutoExecuteOrder(operation);
               //reset the delta, we are executing
               order.setBestPriceDeviationFromLimit(null);
               operationStateAuditDao.updateOrderBestAndLimitDelta(order, null);
               operation.setStateResilient(new OrderNotExecutableState(Messages.getString("LimitFile.doNotExecute")), ErrorState.class);
            }
            else {
               setNotAutoExecuteOrder(operation);
               csExecutionStrategyService.startExecution(operation, currentAttempt, serialNumberService);
            }
         }
      }
      else if (priceResult.getState() == PriceResult.PriceResultState.INCOMPLETE) {
         checkOrderAndsetNotAutoExecuteOrder(operation, doNotExecute);
         try {
            csExecutionStrategyService.manageAutomaticUnexecution(order, order.getCustomer());
         }
         catch (BestXException e) {
            LOGGER.error("Order {}, error while managing INCOMPLETE price result state {}", order.getFixOrderId(), e.getMessage(), e);
            this.operation.removeLastAttempt();
            this.operation.setStateResilient(new WarningState(operation.getState(), e, Messages.getString("PriceService.16")), ErrorState.class);
         }
      }
      else if (priceResult.getState() == PriceResult.PriceResultState.NULL) {
         checkOrderAndsetNotAutoExecuteOrder(operation, doNotExecute);
         try {
            csExecutionStrategyService.manageAutomaticUnexecution(order, order.getCustomer());
         }
         catch (BestXException e) {
            LOGGER.error("Order {}, error while managing NULL price result state {}", order.getFixOrderId(), e.getMessage(), e);
            this.operation.removeLastAttempt();
            this.operation.setStateResilient(new WarningState(operation.getState(), e, Messages.getString("PriceService.16")), ErrorState.class);
         }
      }
      else if (priceResult.getState() == PriceResult.PriceResultState.ERROR) {
         checkOrderAndsetNotAutoExecuteOrder(operation, doNotExecute);
         try {
            csExecutionStrategyService.manageAutomaticUnexecution(order, order.getCustomer());
         }
         catch (BestXException e) {
            LOGGER.error("Order {}, error while managing ERROR price result state {}", order.getFixOrderId(), e.getMessage(), e);
            this.operation.removeLastAttempt();
            this.operation.setStateResilient(new WarningState(operation.getState(), e, Messages.getString("PriceService.16")), ErrorState.class);
         }
      }
   }

   @Override
   public void onOperatorSendDDECommand(OperatorConsoleConnection source, String comment) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void onOperatorManualManage(OperatorConsoleConnection source, String comment) {
      stopDefaultTimer();
      operation.setStateResilient(new ManualExecutionWaitingPriceState(comment), ErrorState.class);
   }

   @Override
   public void onOperatorRetryState(OperatorConsoleConnection source, String comment) {
      // RIPROVA ORDINE
      stopDefaultTimer();

      operation.setFirstAttemptInCurrentCycle(operation.getAttemptNo());

      /*
       * 27-03-2009 Ruggero As requested we've to avoid checking the old proposals when applying the DiscardTriedInEarlierAttempt proposals classifier. When the user forces a retry we mark the
       * attempts as bypassable and later, in the classifier, we check the related attempt' flag.
       */
      LOGGER.debug("Forcing retry on Automatic Curando");
      operation.markAttemptsToByPass(true);
      // set the first valid attempt to start checking while classifying proposals
      operation.setFirstValidAttemptNo(operation.getAttempts().size());
      operation.setStateResilient(new WaitingPriceState(), ErrorState.class);

   }

   @Override
   public void onOperatorForceNotExecution(OperatorConsoleConnection source, String comment) {
      stopDefaultTimer();

      ExecutionReport newExecution = ExecutionReportHelper.createExecutionReport(operation.getOrder().getInstrument(), operation.getOrder().getSide(), serialNumberService);
      newExecution.setState(ExecutionReportState.REJECTED);
      List<ExecutionReport> executionReports = operation.getExecutionReports();
      executionReports.add(newExecution);
      operation.setExecutionReports(executionReports);
      operation.setStateResilient(new SendNotExecutionReportState(comment), ErrorState.class);
   }

   @Override
   public void onStateRestore(OperationState currentState) {
      // This is a search prices state, operation must be in an operator state
      operation.setStateResilient(new CurandoAutoState(), ErrorState.class);
   }

   @Deprecated
   @Override
   public void onUnexecutionResult(Result result, String message) {
      switch (result) {
         case MaxDeviationLimitViolated:
            // need to add a new attempt to save the new book with the updated prices
            // in the WaitingPriceEventHandler the attempt has already been added
            operation.addAttempt();
            Attempt currentAttempt = operation.getLastAttempt();
            currentAttempt.setSortedBook(priceResult.getSortedBook());
         case CustomerAutoNotExecution:
            try {
               ExecutionReportHelper.prepareForAutoNotExecution(operation, serialNumberService, ExecutionReportState.REJECTED);
               operation.setStateResilient(new SendAutoNotExecutionReportState(message), ErrorState.class);
            }
            catch (BestXException e) {
               LOGGER.error("Order {}, error while starting automatic not execution.", operation.getOrder().getFixOrderId(), e);
               String errorMessage = e.getMessage();
               operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);
            }
         break;
         case Failure:
            LOGGER.error(message);
            operation.setStateResilient(new WarningState(operation.getState(), null, message), ErrorState.class);
         break;
         case LimitFileNoPrice:
            operation.setStateResilient(new LimitFileNoPriceState(message), ErrorState.class);
         break;
         case LimitFile:
            //Update the BestANdLimitDelta field on the TabHistoryOrdini table
            Order order = operation.getOrder();
            operationStateAuditDao.updateOrderBestAndLimitDelta(order, order.getBestPriceDeviationFromLimit());
            operation.setStateResilient(new OrderNotExecutableState(message), ErrorState.class);
         break;
         default:
            LOGGER.error("Order {}, unexpected behaviour while checking for automatic not execution or magnet.", operation.getOrder().getFixOrderId());
            operation.setStateResilient(new WarningState(operation.getState(), null, message), ErrorState.class);
         break;
      }
   }

   @Override
   public void onUnexecutionDefault(String executionMarket) {
      //The LimitFile Execution Strategy does not call onSuccess
      if (operation.hasReachedMaxAttempt(maxAttemptNo)) {
         operation.setStateResilient(new OrderNotExecutableState(Messages.getString("EventNoMoreRetry.0")), ErrorState.class);
      }
      else {
         operation.setStateResilient(new CurandoState(), ErrorState.class);
      }
   }

}
