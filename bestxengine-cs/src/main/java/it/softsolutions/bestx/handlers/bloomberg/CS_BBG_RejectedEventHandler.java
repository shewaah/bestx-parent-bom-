/**
 * 
 */
package it.softsolutions.bestx.handlers.bloomberg;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.handlers.BookHelper;
import it.softsolutions.bestx.handlers.ExecutionReportHelper;
import it.softsolutions.bestx.handlers.LimitFileHelper;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Attempt.AttemptState;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.RejectedState;
import it.softsolutions.bestx.states.SendAutoNotExecutionReportState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.bloomberg.BBG_RejectedState;

/**
 * @author Stefano
 *
 */
public class CS_BBG_RejectedEventHandler extends BaseOperationEventHandler {
	private static final long serialVersionUID = 2840060293804045906L;
	private static final Logger LOGGER = LoggerFactory.getLogger(CS_BBG_RejectedEventHandler.class);
    private final SerialNumberService serialNumberService;

    /**
     * @param operation
     */
    public CS_BBG_RejectedEventHandler(Operation operation, SerialNumberService serialNumberService) {
        super(operation);
        this.serialNumberService = serialNumberService;
    }

    @Override
    public void onNewState(OperationState currentState) {

        
        boolean mustSendAutoNotExecution = false;
        Order order = operation.getOrder();
        Attempt lastAttempt = operation.getLastAttempt();
        String rejectReason = ""; 
        if (lastAttempt != null) {
            lastAttempt.setAttemptState(AttemptState.REJECTED);
            // set in transition from BBG_SendRfq to BBG_Rejected
            if ( ((BBG_RejectedState)currentState).mustForceAutoNotExecution() )  {
                mustSendAutoNotExecution = true;
                // get reason received from market, in case of technical reject (see BBG_SendRfq to BBG_Rejected transition) 
                rejectReason = currentState.getComment();
            }
            
            if (!mustSendAutoNotExecution) {            
                List<ClassifiedProposal> currentProposals = lastAttempt.getSortedBook().getValidSideProposals(order.getSide());
                double[] resultValues = new double[2];
/*                try {
                    if (BookHelper.isSpreadOverTheMax(currentProposals, order, resultValues)) {
                        rejectReason = Messages.getString("BestBook.21", resultValues[0], resultValues[1]);
        
                        mustSendAutoNotExecution = true;
                    }
                } catch (BestXException e) {
                    LOGGER.error("Order {}, error while verifying quote spread.", operation.getOrder().getFixOrderId(), e);
                    String errorMessage = e.getMessage();
                    operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);
    
                    return;
                }*/
            }

            if (mustSendAutoNotExecution) {
                // auto not execution
                try {
                    LOGGER.info("Order {} : spread between best and second best too wide, sending not execution report", operation.getOrder().getFixOrderId());
                    ExecutionReportHelper.prepareForAutoNotExecution(operation, serialNumberService, ExecutionReportState.REJECTED);
                    if (order.isLimitFile()) {
                        //- if null we will have only the limit file prefix
                        //- coming here the order must be a Limit File, not a Limit File No Price
                        String customerComment = order.getText();
                        customerComment = LimitFileHelper.getInstance().getCommentWithPrefix(customerComment, false);
                        order.setText(customerComment);
                    }
                    operation.setStateResilient(new SendAutoNotExecutionReportState(rejectReason), ErrorState.class);
                } catch (BestXException e) {
                    LOGGER.error("Order {}, error while starting automatic not execution.", operation.getOrder().getFixOrderId(), e);
                    String errorMessage = e.getMessage();
                    operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);
                }
            }
            else {
                operation.setStateResilient(new RejectedState(Messages.getString("GoingInRejectedOrTimeoutStateMessage")), ErrorState.class);
            }
        }
    }
}
