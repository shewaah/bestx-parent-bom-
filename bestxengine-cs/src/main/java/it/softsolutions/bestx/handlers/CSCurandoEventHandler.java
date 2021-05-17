/**
 * 
 */
package it.softsolutions.bestx.handlers;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.CurandoTimerProfile;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.OrderHelper;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.OperationStateAuditDAOProvider;
import it.softsolutions.bestx.services.SerialNumberServiceProvider;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyService;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceFactory;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.price.PriceService;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.ManualExecutionWaitingPriceState;
import it.softsolutions.bestx.states.OrderNotExecutableState;
import it.softsolutions.bestx.states.SendNotExecutionReportState;
import it.softsolutions.bestx.states.WaitingPriceState;

/**
 * @author Stefano
 * 
 */
public class CSCurandoEventHandler extends CSBaseOperationEventHandler {

   private static final long serialVersionUID = -290774566659583650L;
   private static final Logger LOGGER = LoggerFactory.getLogger(CSCurandoEventHandler.class);
   private final SerialNumberService serialNumberService;
   private CurandoTimerProfile curandoTimerProfile;
   private int curandoRetrySec;

   // private int curandoRetrySec;

   /**
    * @param operation
    */
   public CSCurandoEventHandler(Operation operation, SerialNumberService serialNumberService, int curandoRetrySec, CurandoTimerProfile curandoTimerProfile){
      super(operation);
      this.curandoRetrySec = curandoRetrySec;
      this.serialNumberService = serialNumberService;
      this.curandoTimerProfile = curandoTimerProfile;
   }

   @Override
   public void onNewState(OperationState currentState) {
      Order order = operation.getOrder();
      Customer customer = order.getCustomer();
      long mSecDelay = getTimerInterval(order, customer);
      setupDefaultTimer(mSecDelay, false);

      OrderHelper.setOrderBestPriceDeviationFromLimit(operation);
      OperationStateAuditDAOProvider.getOperationStateAuditDao().updateOrderBestAndLimitDelta(order, order.getBestPriceDeviationFromLimit());
      OperationStateAuditDAOProvider.getOperationStateAuditDao().updateOrderNextExecutionTime(operation.getOrder(), new Date(DateService.newLocalDate().getTime() + mSecDelay));
   }

   protected long getTimerInterval(Order order, Customer customer) {
      long priceDiscoveryInterval = curandoRetrySec;
      Double deviation = order.getBestPriceDeviationFromLimit();
      priceDiscoveryInterval = curandoTimerProfile.getTimeForDeviation(deviation);
      LOGGER.info("Order {}, LimitFile timer will fire in {} seconds.", order.getFixOrderId(), priceDiscoveryInterval);
      return priceDiscoveryInterval * 1000;
   }

   @Override
   public void onPricesResult(PriceService source, PriceResult priceResult) {
      // not expected to receive any price result
   }

   @Override
   public void onOperatorPriceDiscovery(OperatorConsoleConnection source) {
      LOGGER.info("Order {}, Manual Price discovery requested.", operation.getOrder().getFixOrderId());
      operation.setStateResilient(new WaitingPriceState("Manual Price discovery requested."), ErrorState.class);
   }

   @Override
   public void onRevoke() {
      ExecutionStrategyService csExecutionStrategyService = ExecutionStrategyServiceFactory.getInstance().getExecutionStrategyService(operation.getOrder().getPriceDiscoveryType(), operation, null,
            true);
      csExecutionStrategyService.acceptOrderRevoke(operation, operation.getLastAttempt(), SerialNumberServiceProvider.getSerialNumberService());
   }

   @Override
   public void onOperatorManualManage(OperatorConsoleConnection source, String comment) {
      operation.setStateResilient(new ManualExecutionWaitingPriceState(comment), ErrorState.class);
   }

   @Override
   public void onOperatorRetryState(OperatorConsoleConnection source, String comment) {
      operation.setFirstAttemptInCurrentCycle(operation.getAttemptNo());
      operation.setFirstValidAttemptNo(operation.getAttempts().size());
      operation.setStateResilient(new WaitingPriceState(comment), ErrorState.class);

   }

   @Override
   public void onTimerExpired(String jobName, String groupName) {
      String handlerJobName = super.getDefaultTimerJobName();
      Order order = operation.getOrder();
      Customer customer = order.getCustomer();

      if (jobName.equalsIgnoreCase(handlerJobName)) {
         LOGGER.warn("Order {}: Timer OrderCurandoTimeout expired. Clean-up data.", operation.getOrder().getFixOrderId());
         // nothing to do, recreate the timer
         LOGGER.debug("Order {}, re-start the timer", operation.getOrder().getFixOrderId());
         long mSecDelay = getTimerInterval(order, customer);
         setupDefaultTimer(mSecDelay, false);
      }
      else super.onTimerExpired(jobName, groupName);
   }

   @Override
   public void onOperatorForceNotExecution(OperatorConsoleConnection source, String comment) {
      ExecutionReport newExecution = ExecutionReportHelper.createExecutionReport(operation.getOrder().getInstrument(), operation.getOrder().getSide(), this.serialNumberService);
      newExecution.setState(ExecutionReportState.REJECTED);
      List<ExecutionReport> executionReports = operation.getExecutionReports();
      executionReports.add(newExecution);
      operation.setExecutionReports(executionReports);
      operation.setStateResilient(new SendNotExecutionReportState(comment), ErrorState.class);
   }

   @Override
   public void onOperatorMoveToNotExecutable(OperatorConsoleConnection source, String comment) {
      operation.setStateResilient(new OrderNotExecutableState(), ErrorState.class);
   }

   @Override
   public void onStateRestore(OperationState currentState) {
      // DO NOTHING
   }
}
