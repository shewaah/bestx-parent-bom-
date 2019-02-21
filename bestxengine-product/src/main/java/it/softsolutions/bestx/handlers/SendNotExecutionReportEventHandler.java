/**
 * 
 */
package it.softsolutions.bestx.handlers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.connections.CustomerConnection.ErrorCode;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.price.PriceService;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.OrderNotExecutedState;
import it.softsolutions.bestx.states.WarningState;

/**
 * @author Stefano
 *
 */
public class SendNotExecutionReportEventHandler extends BaseOperationEventHandler {

	private final SerialNumberService serialNumberService;
	private static final Logger LOGGER = LoggerFactory.getLogger(SendNotExecutionReportEventHandler.class);
    /**
	 * @param operation
	 */
	public SendNotExecutionReportEventHandler(Operation operation, SerialNumberService serialNumberService) {
		super(operation);
		this.serialNumberService = serialNumberService;
	}
	
	@Override
	public void onNewState(OperationState currentState) {
		try {
			//isNoProposalsOrderOnBook is true for magnet orders 
			if (operation.isNoProposalsOrderOnBook()){
			//we must create the execution report that will be sent
			//notifying the not execution (partial or total)
				ExecutionReport newExecution = ExecutionReportHelper.createExecutionReport(operation.getOrder().getInstrument(), 
				        operation.getOrder().getSide(), 
				        this.serialNumberService);
				newExecution.setState(ExecutionReportState.CANCELLED);
				List<ExecutionReport> executionReports = operation.getExecutionReports();
				executionReports.add(newExecution);
				operation.setExecutionReports(executionReports);
			}
			if(operation.getExecutionReports().size() > 0) {
				operation.getExecutionReports().get(operation.getExecutionReports().size()-1).setExecBroker("");
				operation.getExecutionReports().get(operation.getExecutionReports().size()-1).setMarket(null);
				customerConnection.sendOrderReject(operation, 
						operation.getOrder(), 
						operation.getIdentifier(OperationIdType.ORDER_ID), 
						operation.getExecutionReports().get(operation.getExecutionReports().size()-1),
						ErrorCode.UNKNOWN_ORDER,
						currentState.getComment());
			} else {
				LOGGER.info("Order {} has no execution Reports, cannot send unexecution report!", operation.getOrder().getFixOrderId());
				operation.setStateResilient(new WarningState(currentState, null, Messages.getString("Order has no execution Reports, cannot send unexecution report!", operation.getIdentifier(OperationIdType.CUSTOMER_CHANNEL))), ErrorState.class);
			}
		} catch (BestXException exc) {
			operation.setStateResilient(new WarningState(currentState, null, Messages.getString("EventTasReportError.0", operation.getIdentifier(OperationIdType.CUSTOMER_CHANNEL))), ErrorState.class);
		}
	}
	
	@Override
	public void onCustomerExecutionReportAcknowledged(CustomerConnection source, ExecutionReport executionReport) {
		operation.setStateResilient(new OrderNotExecutedState(), ErrorState.class);
	}
	
	@Override
	public void onCustomerExecutionReportNotAcknowledged(CustomerConnection source, ExecutionReport executionReport, Integer errorCode, String errorMessage) {
		operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("EventTasReportError.0", operation.getIdentifier(OperationIdType.CUSTOMER_CHANNEL))), ErrorState.class);
	}

	@Override
	public void onPricesResult(PriceService source, PriceResult priceResult) {
		// DO NOTHING
	}

	@Override
	public void onTimerExpired(String jobName, String groupName) {
		if(this.customerSpecificHandler != null) 
			this.customerSpecificHandler.onTimerExpired(jobName, groupName);
		LOGGER.info("Operation: '" + operation + "' - Timer Expired event ignored in state: " + operation.getState().getClass().getSimpleName() + " Timer ID: "
				+ jobName + '-' + groupName);
	}
}