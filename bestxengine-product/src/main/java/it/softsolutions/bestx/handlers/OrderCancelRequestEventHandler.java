package it.softsolutions.bestx.handlers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.Operation.RevocationState;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationRegistry;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.connections.CustomerConnection.ErrorCode;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.OrderRevocatedState;
import it.softsolutions.bestx.states.WarningState;

//BESTX-483 TDR 20190828
public class OrderCancelRequestEventHandler extends BaseOperationEventHandler {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4087324684533219794L;
	private static final Logger LOGGER = LoggerFactory.getLogger(OrderCancelRequestEventHandler.class);
	private SerialNumberService serialNumberService;
	private OperationRegistry operationRegistry;
	private InstrumentFinder instrumentFinder;
	/**
	 * Instantiates a new order revocated event handler.
	 *
	 * @param operation the operation
	 */
	public OrderCancelRequestEventHandler(Operation operation, SerialNumberService serialNumberService, InstrumentFinder instrumentFinder, OperationRegistry operationRegistry) {
		super(operation);
		this.serialNumberService = serialNumberService;
		this.instrumentFinder = instrumentFinder;
		this.operationRegistry = operationRegistry;
	}

	@Override
	public void onNewState(OperationState currentState) {
		Order order = operation.getOrder();
		LOGGER.debug("OperationState = {}, order = {}", currentState, order);
		try {
			//[RR20150119] CRSBXTEM-146 add a cancellation execution report, it will be needed while creating the
			//fix message to be sent to the customer
			ExecutionReport newExecution = ExecutionReportHelper.createExecutionReport(operation.getOrder().getInstrument(), 
					operation.getOrder().getSide(), serialNumberService);
			newExecution.setState(ExecutionReportState.CANCELLED);
			List<ExecutionReport> executionReports = operation.getExecutionReports();
			executionReports.add(newExecution);
			operation.setExecutionReports(executionReports);

			operation.setRevocationState(RevocationState.MANUAL_ACCEPTED);
			//send the revoke acceptance report
			customerConnection.sendRevokeReport(operation, order, RevocationState.MANUAL_ACCEPTED, currentState.getComment());

			//[RR20150119] CRSBXTEM-146 send the order cancellation back to the customer connection
			customerConnection.sendOrderReject(operation, 
					operation.getOrder(), 
					operation.getIdentifier(OperationIdType.ORDER_ID), 
					operation.getExecutionReports().get(operation.getExecutionReports().size()-1),
					ErrorCode.UNKNOWN_ORDER,
					currentState.getComment());

			//persist the new execution report
			operationRegistry.updateOperation(operation);
		} catch (BestXException e) {
			LOGGER.error("An error occurred while sending Revoke Accepted to Customer Connection", e);
			operation.setStateResilient(new WarningState(currentState, e, Messages.getString("ERROR_IN_SENDING_REVOKE_ACCEPT", operation.getIdentifier(OperationIdType.CUSTOMER_CHANNEL))), ErrorState.class);
		}
	}

	//BXMNT-428 AMC 20160309
	@Override
	public void onCustomServiceResponse(boolean error, String securityId) {
		LOGGER.info("Order {} , Security ID {}: Custom Service reply received - Error: {}.", operation.getOrder().getFixOrderId(), securityId, error);
		if (!error) {
			//update the order instrument with the info uploaded by Custom Service
			Instrument instrument = instrumentFinder.getInstrumentByIsin(securityId);
			operation.getOrder().setInstrument(instrument);
		}
	}

	//BXMNT-428 AMC 20160309
	@Override
	public void onTimerExpired(String jobName, String groupName) {
		LOGGER.info("Order {} : Timer: {} expired.", operation.getOrder().getFixOrderId(), jobName);
	}
	
	//BESTX-483 TDR 20190828
	@Override
	public void onCustomerExecutionReportAcknowledged(CustomerConnection source, ExecutionReport executionReport) {
		LOGGER.info("Received ACK for execution report : {}", executionReport);
		operation.setStateResilient(new OrderRevocatedState(), ErrorState.class);
	}

}
