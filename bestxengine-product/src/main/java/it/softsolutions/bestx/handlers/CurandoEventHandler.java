/**
 * 
 */
package it.softsolutions.bestx.handlers;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Instrument.InstrumentAttributes;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Portfolio;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.ManualExecutionWaitingPriceState;
import it.softsolutions.bestx.states.OrderNotExecutableState;
import it.softsolutions.bestx.states.SendNotExecutionReportState;
import it.softsolutions.bestx.states.WaitingPriceState;
import it.softsolutions.bestx.states.internal.deprecated.INT_InternalInCurandoState;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stefano
 * 
 */
public class CurandoEventHandler extends BaseOperationEventHandler {

    private static final long serialVersionUID = -290774566659583650L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CurandoEventHandler.class);
    private final SerialNumberService serialNumberService;

    // private int curandoRetrySec;

    /**
     * @param operation
     */
    public CurandoEventHandler(Operation operation, SerialNumberService serialNumberService, int curandoRetrySec) {
        super(operation);
        this.serialNumberService = serialNumberService;
    }

    @Override
    public void onNewState(OperationState currentState) {
        // setupTimer(timerService, curandoRetrySec, "CURANDO_ORDER");
        Order order = operation.getOrder();
        Instrument instrument = order.getInstrument();
        if (instrument != null) {
            InstrumentAttributes instrAttr = instrument.getInstrumentAttributes();
            if (instrAttr != null) {
                Portfolio portfolio = instrAttr.getPortfolio();
                if (portfolio != null) {
                    if (portfolio.isInternalizable()) {
                        LOGGER.debug("Order {}, the instrument {} is internalizable (portfolio {}).", order.getFixOrderId(), instrument.getIsin(), portfolio.getDescription());
                        LOGGER.debug("Order {}, going into Internalized Manual Curando state.", order.getFixOrderId());
                        String comment = Messages.getString("INTMarketOrderInCurandoOrder.0");
                        operation.setStateResilient(new INT_InternalInCurandoState(comment), ErrorState.class);
                    } else {
                        LOGGER.debug("Order {}, the instrument {} is not internalizable (portfolio {}). Can go in the Manual Curando state.", order.getFixOrderId(), instrument.getIsin(), portfolio.getDescription());
                    }
                }
            }
        }
        
    }
    
	@Override
    public void onOperatorSendDDECommand(OperatorConsoleConnection source, String comment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onOperatorManualManage(OperatorConsoleConnection source, String comment) {
        operation.setStateResilient(new ManualExecutionWaitingPriceState(comment), ErrorState.class);
    }

    @Override
    public void onOperatorRetryState(OperatorConsoleConnection source, String comment) {
        // RESET CONTATORE ATTEMPT SE NECESSARIO
        // if (operation.hasReachedMaxAttempt(maxAttemptNo)) {
        // now resets every time the onOperatorRetryState is called
        operation.setFirstAttemptInCurrentCycle(operation.getAttemptNo());
        // }
        // Setta il primo attempt da cui controllare i prezzi gia' utilizzati
        operation.setFirstValidAttemptNo(operation.getAttempts().size());

        // RIPROVA ORDINE
        operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
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
