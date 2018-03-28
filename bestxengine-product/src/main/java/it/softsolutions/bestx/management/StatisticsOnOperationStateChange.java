/*
 * Project Name : BestXEngine_Akros
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author: anna.cochetti $
 * Date         : $Date: 2010-04-28 12:05:00 $
 * Header       : $Id: StatisticsOnOperationStateChange.java,v 1.3 2010-04-28 12:05:00 anna.cochetti Exp $
 * Revision     : $Revision: 1.3 $
 * Source       : $Source: /root/scripts/BestXEngine_Akros/src/it/softsolutions/bestx/management/StatisticsOnOperationStateChange.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_Akros
 */
package it.softsolutions.bestx.management;

import it.softsolutions.bestx.CommonMetricRegistry;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.OperationStateListener;
import it.softsolutions.bestx.states.BusinessValidationState;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.FormalValidationKOState;
import it.softsolutions.bestx.states.LimitFileNoPriceState;
import it.softsolutions.bestx.states.OrderNotExecutableState;
import it.softsolutions.bestx.states.OrderReceivedState;
import it.softsolutions.bestx.states.OrderRevocatedState;
import it.softsolutions.bestx.states.SendAutoNotExecutionReportState;
import it.softsolutions.bestx.states.SendExecutionReportState;
import it.softsolutions.bestx.states.SendNotExecutionReportState;
import it.softsolutions.bestx.states.StateExecuted;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.manageability.sl.monitoring.NumericValueMonitor;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;

public class StatisticsOnOperationStateChange implements OperationStateListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsOnOperationStateChange.class);

    // Manages statistics requiring memory on times and other stuff
    private Map<String, OperationStateChangeStatistics> changingOperations = new HashMap<String, OperationStateChangeStatistics>();

    // MONITORABLE VALUES
    private long validationTime = 0;
    private long executionTime = 0;
    private long transitionsToOrderReceived = 0;
    private long transitionsToSendExecution = 0;
    private long transitionsToSendNotExecuted = 0;
    private long transitionsToSendAutoNotExecuted = 0;
    private long transitionsToRevocated = 0;
    private long operationInCurandoAutoState = 0;
    private long operationInTechnicalIssueState = 0;
    private final NumericValueMonitor validationTimeMonitor = new NumericValueMonitor("ValidationTime", "Orders", true, "info", "[ORDER_STATISTICS]");
    private final NumericValueMonitor executionTimeMonitor = new NumericValueMonitor("ExecutionTime", "Orders", true, "info", "[ORDER_STATISTICS]");
    private final NumericValueMonitor transitionsToOrderReceivedMonitor = new NumericValueMonitor("OrderReceivedNumber", "Orders", true, "info", "[ORDER_STATISTICS]");
    private final NumericValueMonitor transitionsToSendExecutionMonitor = new NumericValueMonitor("OrderExecutedNumber", "Orders", true, "info", "[ORDER_STATISTICS]");
    private final NumericValueMonitor transitionsToSendNotExecutedMonitor = new NumericValueMonitor("OrderNotExecutedNumber", "Orders", true, "info", "[ORDER_STATISTICS]");
    private final NumericValueMonitor transitionsToSendAutoNotExecutedMonitor = new NumericValueMonitor("OrderAutoNOtExecutedNumber", "Orders", true, "info", "[ORDER_STATISTICS]");
    private final NumericValueMonitor transitionsToRevocatedMonitor = new NumericValueMonitor("OrdersRevocatedNumber", "Orders", true, "info", "[ORDER_STATISTICS]");
    private final NumericValueMonitor operationInCurandoAutoStateMonitor = new NumericValueMonitor("OrdersCurrentlyInCurandoState", "Orders", true, "info", "[ORDER_STATISTICS]");
    private final NumericValueMonitor operationInTechnicalIssueStateMonitor = new NumericValueMonitor("OrdersCurrentlyInTechnicalIssueState", "Orders", true, "info", "[ORDER_STATISTICS]");

    private Counter transitionsToOrderReceivedCounter = CommonMetricRegistry.INSTANCE.getMonitorRegistry().counter(MetricRegistry.name(StatisticsOnOperationStateChange.class, "ReceivedOrders"));
    private Counter transitionsToSendExecutionCounter = CommonMetricRegistry.INSTANCE.getMonitorRegistry().counter(MetricRegistry.name(StatisticsOnOperationStateChange.class, "ExecutedOrders"));
    private Counter transitionsToSendNotExecutedCounter = CommonMetricRegistry.INSTANCE.getMonitorRegistry().counter(MetricRegistry.name(StatisticsOnOperationStateChange.class, "NotExecutedOrders"));
    private Counter transitionsToSendAutoNotExecutedCounter = CommonMetricRegistry.INSTANCE.getMonitorRegistry().counter(MetricRegistry.name(StatisticsOnOperationStateChange.class, "AutoNotExecutedOrders"));
    private Counter transitionsToRevocatedCounter = CommonMetricRegistry.INSTANCE.getMonitorRegistry().counter(MetricRegistry.name(StatisticsOnOperationStateChange.class, "CancelledOrders"));
    private Counter transitionsToCurandoStateCounter = CommonMetricRegistry.INSTANCE.getMonitorRegistry().counter(MetricRegistry.name(StatisticsOnOperationStateChange.class, "LimitFileOrders"));
    private Counter transitionsToTechnicalIssueCounter = CommonMetricRegistry.INSTANCE.getMonitorRegistry().counter(MetricRegistry.name(StatisticsOnOperationStateChange.class, "OrdersInTechnicalIssue"));
    // [BXMNT-470]
    private Histogram validationTimeHistogram = CommonMetricRegistry.INSTANCE.getMonitorRegistry().histogram(MetricRegistry.name(StatisticsOnOperationStateChange.class, "validationTime"));
    private Histogram executionTimeHistogram = CommonMetricRegistry.INSTANCE.getMonitorRegistry().histogram(MetricRegistry.name(StatisticsOnOperationStateChange.class, "executionTime"));

    // END MONITORABLE VALUES
    private long getCurrentTimeInMillis() {
        Calendar calendar = Calendar.getInstance();
        long now = calendar.getTimeInMillis();
        return now;
    }

    public void onOperationStateChanged(Operation operation, OperationState oldState, OperationState newState) {
        try {
            String orderId = operation.getIdentifier(OperationIdType.ORDER_ID);
            if (orderId == null) {
                return;
            }
            if (!changingOperations.containsKey(orderId)) {
                changingOperations.put(orderId, new OperationStateChangeStatistics());
            }
            OperationStateChangeStatistics opstat = changingOperations.get(orderId);

            // STATISTICS based on new state value
            if (newState.getClass() == OrderReceivedState.class) {
                transitionsToOrderReceived++;
                transitionsToOrderReceivedMonitor.setValue(transitionsToOrderReceived);
                LOGGER.info("[MONITOR] Number of transitions to State " + newState.getClass().getSimpleName() + " = " + transitionsToOrderReceived);
            } else if (newState.getClass() == SendAutoNotExecutionReportState.class) {
                transitionsToSendAutoNotExecuted++;
                transitionsToSendAutoNotExecutedMonitor.setValue(transitionsToSendAutoNotExecuted);
                LOGGER.info("[MONITOR] Number of transitions to State " + newState.getClass().getSimpleName() + " = " + transitionsToSendAutoNotExecuted);
            } else if (newState.getClass() == SendNotExecutionReportState.class) {
                transitionsToSendNotExecuted++;
                transitionsToSendNotExecutedMonitor.setValue(transitionsToSendNotExecuted);
                LOGGER.info("[MONITOR] Number of transitions to State " + newState.getClass().getSimpleName() + " = " + transitionsToSendNotExecuted);
            } else if (newState.getClass() == SendExecutionReportState.class) {
                transitionsToSendExecution++;
                transitionsToSendExecutionMonitor.setValue(transitionsToSendExecution);
                LOGGER.info("[MONITOR] Number of transitions to State " + newState.getClass().getSimpleName() + " = " + transitionsToSendExecution);
            } else if (newState.getClass() == OrderRevocatedState.class) {
                transitionsToRevocated++;
                transitionsToRevocatedMonitor.setValue(transitionsToRevocated);
                LOGGER.info("[MONITOR] Number of transitions to State " + newState.getClass().getSimpleName() + " = " + transitionsToRevocated);
            } else if (newState.getClass() == OrderNotExecutableState.class || newState.getClass() == LimitFileNoPriceState.class) {
                operationInCurandoAutoState++;
                operationInCurandoAutoStateMonitor.setValue(operationInCurandoAutoState);
            } else if (newState.getClass() == StateExecuted.class) {
                if (opstat.get(OperationStateChangeStatistics.ORDER_VALIDATION_START_TIME) == null) {
                    return;
                }
                executionTime = ((getCurrentTimeInMillis() - (Long) opstat.get(OperationStateChangeStatistics.ORDER_VALIDATION_START_TIME)));
                executionTimeMonitor.setValue(executionTime);
                executionTimeHistogram.update(executionTime);
            } else if (newState.getClass() == WarningState.class || newState.getClass() == ErrorState.class) {
                operationInTechnicalIssueState++;
                operationInTechnicalIssueStateMonitor.setValue(operationInTechnicalIssueState);
            }

            // STATISTICS based on old state value
            if (oldState.getClass() == OrderReceivedState.class) {
                opstat.put(OperationStateChangeStatistics.ORDER_VALIDATION_START_TIME, oldState.getEnteredTime().getTime());

                if (newState.getClass() == FormalValidationKOState.class) {
                    validationTime = (getCurrentTimeInMillis() - oldState.getEnteredTime().getTime());
                    validationTimeMonitor.setValue(validationTime);
                    validationTimeHistogram.update(validationTime);
                }
            } else if (oldState.getClass() == BusinessValidationState.class) {
                if (opstat.get(OperationStateChangeStatistics.ORDER_VALIDATION_START_TIME) == null) {
                    return;
                }
                validationTime = (getCurrentTimeInMillis() - (Long) opstat.get(OperationStateChangeStatistics.ORDER_VALIDATION_START_TIME));
                validationTimeMonitor.setValue(validationTime);
                validationTimeHistogram.update(validationTime);
            } else if (oldState.getClass() == OrderNotExecutableState.class) {
                operationInCurandoAutoState--;
                operationInCurandoAutoStateMonitor.setValue(operationInCurandoAutoState);
            }

            if (newState.isTerminal()) {
                changingOperations.remove(orderId);
            }
        } catch (Throwable e) {
            LOGGER.info("Exception during statistics collection. Main computation was not affected.", e);
        }
    }
}
