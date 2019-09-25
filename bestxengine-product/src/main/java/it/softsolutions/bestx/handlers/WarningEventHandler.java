/**
 * 
 */
package it.softsolutions.bestx.handlers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.ManualManageState;
import it.softsolutions.bestx.states.OrderNotExecutedState;
import it.softsolutions.bestx.states.SendExecutionReportState;
import it.softsolutions.bestx.states.SendNotExecutionReportState;
import it.softsolutions.bestx.states.StateExecuted;
import it.softsolutions.bestx.states.WaitingPriceState;

/**
 * @author Stefano
 *
 */
public class WarningEventHandler extends BaseOperationEventHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(WarningEventHandler.class);
	private SerialNumberService serialNumberService;

	/**
	 * @param operation
	 */
	public WarningEventHandler(Operation operation, SerialNumberService serialNumberService) {
		super(operation);
		this.serialNumberService = serialNumberService;
	}

	@Override
	public void onOperatorForceReceived(OperatorConsoleConnection source, String comment) {
		//		operation.setStateResilient(((WarningState)operation.getState()).getPreviousState(), ErrorState.class);
		// reset contatore sempre 15/12/2008 AMC
		operation.setFirstAttemptInCurrentCycle(operation.getAttemptNo());
		operation.setStateResilient(new WaitingPriceState(comment), ErrorState.class);
	}

	@Override
	public void onOperatorAcceptOrder(OperatorConsoleConnection source, String comment) {

		operation.setStateResilient(new WaitingPriceState(comment), ErrorState.class);
	}

	@Override
	public void onOperatorManualManage(OperatorConsoleConnection source, String comment) {
		operation.setStateResilient(new ManualManageState(comment), ErrorState.class);
	}

	@Override
	public void onOperatorForceExecutedWithoutExecutionReport(OperatorConsoleConnection source, String comment) {
		operation.setStateResilient(new StateExecuted(), ErrorState.class);
	}

	@Override
	public void onOperatorForceNotExecutedWithoutExecutionReport(OperatorConsoleConnection source, String comment) {
		operation.setStateResilient(new OrderNotExecutedState(), ErrorState.class);
	}

	@Override
	public void onOperatorResendExecutionReport(OperatorConsoleConnection source, String comment) {
		operation.setStateResilient(new SendExecutionReportState(), ErrorState.class);
	}

	@Override
	public void onOperatorForceNotExecution(OperatorConsoleConnection source, String comment) {
		ExecutionReport newExecution = ExecutionReportHelper.createExecutionReport(operation.getOrder().getInstrument(), 
				operation.getOrder().getSide(), 
				this.serialNumberService);
		newExecution.setState(ExecutionReportState.REJECTED);
		List<ExecutionReport> executionReports = operation.getExecutionReports();
		executionReports.add(newExecution);
		operation.setExecutionReports(executionReports);
		operation.setStateResilient(new SendNotExecutionReportState(comment), ErrorState.class);
	}

//	@Override
//	public void onMarketExecutionReport(MarketBuySideConnection source, Order order,
//			MarketExecutionReport marketExecutionReport)
//	{
//		super.onMarketExecutionReport(source, order, marketExecutionReport);
//		operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("WARNINGExecutionReportArrived.0", source.getMarketCode().name())), ErrorState.class);
//	}

	@Override
	public void onStateRestore(OperationState currentState) {
		// DO NOTHING
	}
	
	@Override
   public void onFixRevoke(CustomerConnection source) {
      if (customerConnection == null) {
         LOGGER.error("Revoke received but no Customer Connection available");
      } else {
         // stop default timer, if any
         stopDefaultTimer();
         String comment = Messages.getString("AutomaticRevokeDefaultMessage.0");
         updateOperationToRevocated(comment);
      }
   }	
}	