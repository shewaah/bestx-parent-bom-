/**
 * 
 */
package it.softsolutions.bestx.handlers;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.connections.MarketConnection;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.SendExecutionReportState;

import java.util.Calendar;
import java.util.Date;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stefano
 *
 */
public class ManualWaitingFillEventHandler extends BaseOperationEventHandler {

    private static final long serialVersionUID = -6941408323760989579L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ManualWaitingFillEventHandler.class);

    public static final String MAN_WAIT_FILL_TAG = "MAN_WAIT_FILL";
    public static final String MAN_FILL_POLLING_TAG = "MAN_FILL_POLLING";

    private long waitFillMSec;
    private long fillPollingMSec;
    private MarketConnection bbgConnection;
    private MarketMakerFinder marketMakerFinder;

    private ExecutionReport currentExecutionReport;
    private MarketMaker executionMarketMaker;
    private Date minArrivalDate;
    /**
     * @param operation
     */
    public ManualWaitingFillEventHandler(Operation operation, 
                    MarketConnection bbgConnection,
                    MarketMakerFinder marketMakerFinder,
                    long waitFillMSec, 
                    long fillPollingMSec) {
        super(operation);
        this.waitFillMSec = waitFillMSec;
        this.fillPollingMSec = fillPollingMSec;
        this.bbgConnection = bbgConnection;
        this.marketMakerFinder = marketMakerFinder;
        Calendar today = Calendar.getInstance();
        today.add(Calendar.DATE, -1);
        today.set(Calendar.HOUR, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        this.minArrivalDate = today.getTime();
    }

    @Override
    public void onNewState(OperationState currentState) {
        String jobName = super.getDefaultTimerJobName();
        //TODO: capire se gestire meglio la creazione del jobName
        setupTimer(jobName + "#" + MAN_WAIT_FILL_TAG, waitFillMSec, false);
        setupTimer(jobName + "#" + MAN_FILL_POLLING_TAG, fillPollingMSec, false);

        int executionReportIndex = operation.getExecutionReports().size() - 1;
        currentExecutionReport = operation.getExecutionReports().get(executionReportIndex);
        try {
            executionMarketMaker = marketMakerFinder.getMarketMakerByCode(currentExecutionReport.getExecBroker());
        } catch (BestXException e) {
            LOGGER.error("Unable to retrieve market maker with code " + currentExecutionReport.getExecBroker(), e);
        }
    }

    @Override
    public void onTimerExpired(String jobName, String groupName) {
        if (jobName.endsWith(MAN_WAIT_FILL_TAG)) {
            try {
                stopTimer(jobName);
            } catch (SchedulerException e) {
                LOGGER.error("Order {}: unable to remove timer", operation.getOrder().getFixOrderId(), e);
            }

            operation.setStateResilient(new SendExecutionReportState(), ErrorState.class);
        } else if (jobName.endsWith(MAN_FILL_POLLING_TAG)) {
            MarketExecutionReport trade = bbgConnection.getBuySideConnection().getMatchingTrade(operation.getOrder(), currentExecutionReport.getPrice(), executionMarketMaker, minArrivalDate);
            if (trade != null) {
                LOGGER.info("Successful matching to Bloomberg FILL: " + trade.getTicket());
                currentExecutionReport.setAccruedInterestAmount(trade.getAccruedInterestAmount());
                currentExecutionReport.setAccruedInterestDays(trade.getAccruedInterestDays());
                currentExecutionReport.setTicket(trade.getTicket());
                operation.setStateResilient(new SendExecutionReportState(), ErrorState.class);
            } else {
                setupTimer(jobName + "#" + MAN_FILL_POLLING_TAG, fillPollingMSec, false);
            }
        } else {
            super.onTimerExpired(jobName, groupName);
        }
    }

    // this is one of the few handlers to override onFixRevoke, as it sets up two non-default timers,
    // and it must kill them by itself (other handlers' timers are stopped by superclass BaseState)
    @Override
    public void onFixRevoke(CustomerConnection source) {
        LOGGER.debug("Fix revoke request received, stopping limit timer for orderId {}", operation.getOrder().getFixOrderId());

        String handlerJobName = getDefaultTimerJobName();
        try {
            stopTimer(handlerJobName + "#" + MAN_WAIT_FILL_TAG);
        } catch (SchedulerException e) {
            LOGGER.error("Cannot stop job {}-{}: {}", handlerJobName, MAN_WAIT_FILL_TAG, e.getMessage(), e);
        }

        try {
            stopTimer(handlerJobName + "#" + MAN_FILL_POLLING_TAG);
        } catch (SchedulerException e) {
            LOGGER.error("Cannot stop job {}-{}: {}", handlerJobName, MAN_FILL_POLLING_TAG, e.getMessage(), e);
        }

        super.onFixRevoke(source);
    }

}
