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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.Operation.RevocationState;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.model.Commission;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.CommissionService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.StateExecuted;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.jsscommon.Money;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: davide.rossoni Creation date: 19/ott/2012
 * 
 **/
public class SendExecutionReportEventHandler extends BaseOperationEventHandler {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(SendExecutionReportEventHandler.class);
	
    // FIXME AMC 20100505 attenzione perche' il calcolo delle commissioni e' replicato in TLX_SendReportsEventHandler,
    // MOT_SendReportsEventHandler e SendExecutionReportEventHandler

    private static final long serialVersionUID = 6528789682264435894L;
	private static BigDecimal HUNDRED = new BigDecimal(100);
    private final int sendExecRepTimeout;
    
    // totalFills : number of fills to be sent through AMOS connection
    private AtomicInteger totalFills = new AtomicInteger();
    
    // arrivedAcksOrNacks : number of acks or nacks arrived
    private AtomicInteger arrivedAcksOrNacks = new AtomicInteger();
    
    // nackArrived : true if we received a nack on a fill or exec report
    private AtomicBoolean nackArrived = new AtomicBoolean();

    /**
     * @param operation
     */
    public SendExecutionReportEventHandler(Operation operation, CommissionService commissionService, int sendExecRepTimeout) {
        super(operation, commissionService);
        this.sendExecRepTimeout = sendExecRepTimeout;
    }

    @Override
    public void onNewState(OperationState currentState) {
        Customer customer = operation.getOrder().getCustomer();
        Instrument instrument = operation.getOrder().getInstrument();
        if (operation.getRevocationState() == RevocationState.ACKNOWLEDGED) {
            try {
                customerConnection.sendRevokeReport(operation, operation.getOrder(), RevocationState.AUTOMATIC_REJECTED, "");
                operation.setRevocationState(RevocationState.AUTOMATIC_REJECTED);
            } catch (BestXException e) {
                LOGGER.error("Error while sending Revoke Reject", e);
            }
        }
        /*
         * Check if the connection manages the sending of all the market fills, it's a behaviour of the AMOS connection. If so, save the
         * number of fills to be sent and send them, otherwise set the number of fills to 1 (the final exec report) and go on
         */
        // init the flag notifying NACKS
        nackArrived.set(false);
        // init the number of arrived ACKS or NACKS
        arrivedAcksOrNacks.set(0);

        setupDefaultTimer(sendExecRepTimeout, false);
        /*
         * timerHandle = timerService.setupOnceOnlyTimer( this, sendExecRepTimeout, operation.getOrder().getFixOrderId() +
         * "SEND_EXEC_REP_TIMER");
         */
        // add commissions to execution report
        // Commissioni calcolate sulla quantita'
        ExecutionReport lastExecutionReport = operation.getExecutionReports().get(operation.getExecutionReports().size() - 1);
//        ExecutionReport firstExecutionReport = operation.getExecutionReports().get(0);
        // 20110214 - Ruggero : ticket 3491
        // Currency extracted from the order, we are coherent with the negotiation source avoiding
        // problems when the instrument has the wrong currency in the database.
        String ordCurrency = operation.getOrder().getCurrency();
        Money quantity = new Money(ordCurrency, lastExecutionReport.getActualQty() == null? BigDecimal.ZERO : lastExecutionReport.getActualQty());

        Commission comm = null;

        try {
            comm = commissionService.getCommission(customer, instrument, quantity, operation.getOrder().getSide());
        } catch (BestXException e) {
            String errorMessage = e.getMessage();
            LOGGER.error("Order {}, error while extracting commissions : {}", operation.getOrder().getFixOrderId(), errorMessage);
            operation.setStateResilient(new WarningState(currentState, null, errorMessage), ErrorState.class);
            return;
        }

        if (comm == null) {
            comm = new Commission(BigDecimal.ZERO, Commission.CommissionType.AMOUNT);
            LOGGER.info("No commission has been set for customer {} for the ordered amount: {}", customer.getFixId(), quantity.getAmount());
        }

        // controls for a safe logging
        if (quantity != null && quantity.getAmount() != null && comm != null && comm.getAmount() != null && comm.getCommissionType() != null) {
            LOGGER.debug("Quantity {}, commissions found {}, type {}", quantity.getAmount(), comm.getAmount(), comm.getCommissionType());
        }
        // I-40
        try {
            setExecutionReportAmountCommission(lastExecutionReport, quantity, comm);
        } catch (BestXException e) {
            String errorMessage = e.getMessage();
            LOGGER.error("Order {}, error while setting execution report amount commissions : {}", operation.getOrder().getFixOrderId(), errorMessage);
            operation.setStateResilient(new WarningState(currentState, null, errorMessage), ErrorState.class);
            return;
        }

        lastExecutionReport.setSequenceId(lastExecutionReport.getSequenceId() + "C");

        // Gestione delle commissioni interne al prezzo per il cliente I-12/2009
        // AMC 20090604
        boolean commissionToBeAdded = false;
        boolean isBestProperty = false;
//        boolean amountCommissionWanted = false;
        try {
            CustomerAttributes customerAttributes = (CustomerAttributes) customer.getCustomerAttributes();
            commissionToBeAdded = customerAttributes.getAddCommissionToCustomerPrice();

            // 20100323 AMC I-33 Richiesta di Sormani per levare la commissione dal prezzo a BNP nel caso che la proprieta' sia BEST
            isBestProperty = "BEST".equalsIgnoreCase(operation.getExecutionReports().get(operation.getExecutionReports().size() - 1).getProperty());
//            amountCommissionWanted = ((CustomerAttributes) customer.getCustomerAttributes()).getAmountCommissionWanted();
        } catch (NullPointerException npe) {
            LOGGER.error("Unable to get market execution report property {}", customer.getName());
        } catch (Exception e) {
            LOGGER.error("Unable to set commission and price for customer {} exception was {}", customer.getName(), e.getMessage());
        }
        BigDecimal commissionedPrice = BigDecimal.ZERO;
        if (commissionToBeAdded && !isBestProperty) {
            LOGGER.debug("Customer wants commission added to the price and the order property is not BEST.");
            // caso di cliente con calcolo delle commissioni addizionate al prezzo
            // e proprieta' diversa da Best
            try {
                commissionedPrice = commissionService.calculateCommissionedPrice(lastExecutionReport.getLastPx(), operation.getOrder().getLimit() == null ? null : operation.getOrder().getLimit()
                                .getAmount(), operation.getOrder().getSide(), comm);
            } catch (Exception e) {
            	
                stopDefaultTimer();
                
                // timerService.cancelTimer(timerHandle);
                LOGGER.error("{}: for customer ", e.getMessage(), customer.getName(), e);
                operation.setStateResilient(new WarningState(currentState, null, Messages.getString("EventTasReportError.1", customer.getName(), lastExecutionReport.getActualQty())), ErrorState.class);
                return;
            }

            LOGGER.debug("Price including commissions: {}", commissionedPrice);

            lastExecutionReport.setCommissionType(Commission.CommissionType.TICKER);
            if (operation.getOrder().getSide().compareTo(OrderSide.BUY) == 0) {
                comm.setAmount(commissionedPrice.subtract(lastExecutionReport.getLastPx()));
            } else {
                comm.setAmount(lastExecutionReport.getLastPx().subtract(commissionedPrice));
            }
            // Questo moltiplica per 100 il valore della commissione attualmente espresso in interi
            comm.setAmount(comm.getAmount().multiply(HUNDRED));
            lastExecutionReport.setCommission(comm.getAmount());
            lastExecutionReport.setLastPx(commissionedPrice);
            LOGGER.debug("set commission to: {} and last px to {}", lastExecutionReport.getCommission(), commissionedPrice);

        } else if (commissionToBeAdded && isBestProperty) {
            // caso di cliente con le commissioni addizionate al prezzo,
            // e proprieta' Best, le commissioni non gliele calcolo proprio.
            comm = null;

        } else {
            // caso di cliente con calcolo semplice delle commissioni per tick o per amount e
            // invio delle commissioni cosi' calcolate
            lastExecutionReport.setCommission(comm.getAmount());
            lastExecutionReport.setCommissionType(comm.getCommissionType());
            LOGGER.debug("Set commissions to calculated values by the commission service");
        }

        // we've to send only the final execution report
        totalFills.set(1);

        lastExecutionReport.setPriceType(operation.getOrder().getPriceType());
        String executionComment = currentState.getComment();
        lastExecutionReport.setText(executionComment);
        LOGGER.info("Sending final execution report to customer {} - Fix ID: {}", customer.getName(), operation.getOrder().getCustomer().getFixId());
        operation.getExecutionReports().set(operation.getExecutionReports().size() - 1, lastExecutionReport);
        try {
            sendExecutionReport(lastExecutionReport);
        } catch (BestXException exc) {
            operation.setStateResilient(new WarningState(currentState, null, Messages.getString("EventTasReportError.0", operation.getIdentifier(OperationIdType.CUSTOMER_CHANNEL))),
                            ErrorState.class);
        }

    }
    
    public void sendExecutionReport(ExecutionReport executionReport) throws BestXException {
    	customerConnection.sendExecutionReport(operation, null, operation.getOrder(), operation.getIdentifier(OperationIdType.ORDER_ID), operation.getLastAttempt(), executionReport);
    }

    /*
     * The following two methods manage the multiple acks or nacks related to the fills and the exec report sent towards the AMOS interface.
     * They also manage the TAS behaviour, only 1 exec report sent and only 1 ack or nack expected. Here we wait for the arrival of ALL the
     * acks or nacks and then decide what to do. We also have a timer running, so if it expires the control goes to the method
     * onTimerExpired.
     */
    @Override
    public void onCustomerExecutionReportAcknowledged(CustomerConnection source, ExecutionReport executionReport) {
        if (totalFills.get() == arrivedAcksOrNacks.get()) {
            // problema, sono gia' arrivate tutte!!!
            LOGGER.error("An Ack notification received, but we have already received all the acks/nacks for {}", operation);
        } else {
        	arrivedAcksOrNacks.incrementAndGet();
        }

        // all the acks or nacks arrived
        if (totalFills.get() == arrivedAcksOrNacks.get()) {
            LOGGER.debug("All nacks or acks received for {}", operation);
            
            stopDefaultTimer();

            // timerService.cancelTimer(timerHandle);
            // check if we received a nack and act as needed
            if (nackArrived.get()) {
                LOGGER.debug("There has been at least one nack, going to WarningState");
                operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("EventNackReceived.0", operation.getIdentifier(OperationIdType.CUSTOMER_CHANNEL))),
                                ErrorState.class);
            } else {
                LOGGER.debug("No Nacks received, all good, let's go to StateExecuted");
                operation.setStateResilient(new StateExecuted(), ErrorState.class);
            }
        }
    }

    @Override
    public void onCustomerExecutionReportNotAcknowledged(CustomerConnection source, ExecutionReport executionReport, Integer errorCode, String errorMessage) {
    	
        if (totalFills.get() == arrivedAcksOrNacks.get()) {
            // problema, sono gia' arrivate tutte!!!
            LOGGER.error("A Nack notification received, but we have already received all the acks/nacks for {}", operation);
        } else {
            nackArrived.set(true);
            arrivedAcksOrNacks.incrementAndGet();
        }
        
        if (totalFills.get() == arrivedAcksOrNacks.get()) {
            // no need to check for possible nacks, we are sure we've at least one nack
            LOGGER.debug("All nacks or acks received for {}", operation);
            LOGGER.debug("There has been at least one nack, going to WarningState");

            stopDefaultTimer();
            
            // timerService.cancelTimer(timerHandle);
            operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("EventNackReceived.0", operation.getIdentifier(OperationIdType.CUSTOMER_CHANNEL))),
                            ErrorState.class);
        }
    }

    @Override
    public void onTimerExpired(String jobName, String groupName) {
    	String handlerJobName = super.getDefaultTimerJobName();
    	
        if (jobName.equals(handlerJobName)) {
            LOGGER.warn("Timer expiration while trying to send execution report or waiting for acknowledges or not acknowledges from {} interface",
                            operation.getIdentifier(OperationIdType.CUSTOMER_CHANNEL));
            LOGGER.warn("Acks or nacks expected : {}, arrived: {}", totalFills.get(), arrivedAcksOrNacks.get());
            operation.setStateResilient(
                            new WarningState(operation.getState(), null, Messages.getString("EventTimeOutSendReportError.0", operation.getIdentifier(OperationIdType.CUSTOMER_CHANNEL))),
                            ErrorState.class);
        } else {
            super.onTimerExpired(jobName, groupName);
        }
    }

}