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

package it.softsolutions.bestx.audit;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.MarketConnectionRegistry;
import it.softsolutions.bestx.MarketExecutionListener;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.MifidConfig;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.OperationStateListener;
import it.softsolutions.bestx.RegulatedMktIsinsLoader;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.MarketConnection;
import it.softsolutions.bestx.dao.OperationStateAuditDao;
import it.softsolutions.bestx.dao.OperationStateAuditDao.Action;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.SaveBookException;
import it.softsolutions.bestx.finders.CustomerFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.handlers.LimitFileHelper;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.CSPOBexExecutionReport;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.CustomerAttributesIFC;
import it.softsolutions.bestx.model.ExecutablePrice;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.ServiceStatus;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.ManualManageState;
import it.softsolutions.bestx.states.RejectedState;
import it.softsolutions.bestx.states.WarningState;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-cs First created by: Creation date: 19-ott-2012
 * 
 **/

public class CSOperationStateAudit implements OperationStateListener, MarketExecutionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CSOperationStateAudit.class);

    private static final Action[] warningActions = { Action.NOT_EXECUTED, Action.FORCE_NOT_EXECUTED, Action.FORCE_EXECUTED, Action.FORCE_RECEIVED, Action.MANUAL_MANAGE,
        Action.COMMAND_RESEND_EXECUTIONREPORT };
    private static final Action[] warningInternalProductActions = { Action.NOT_EXECUTED, Action.MANUAL_MANAGE };
    private static final Action[] errorActions = { Action.NOT_EXECUTED, Action.FORCE_NOT_EXECUTED, Action.FORCE_EXECUTED, Action.FORCE_RECEIVED, Action.MANUAL_MANAGE,
        Action.COMMAND_RESEND_EXECUTIONREPORT };
    private static final Action[] rejectableActions = { Action.NOT_EXECUTED, Action.MANUAL_MANAGE, Action.SEND_DES_DATA };
    private static final Action[] bbgStandByActions = { Action.MANUAL_MANAGE, Action.NOT_EXECUTED, Action.AUTO_MANUAL_ORDER, Action.ORDER_RETRY, Action.SEND_DDE_DATA };
    private static final Action[] matchBaseActions = { Action.ORDER_RETRY, Action.MANUAL_MANAGE };
    private static final Action[] matchFoundActions = { Action.ORDER_RETRY, Action.MANUAL_MANAGE, Action.MERGE_ORDER };
    private static final Action[] curandoActions = { Action.SEND_DDE_DATA };
    private static final Action[] limitFileNoPriceActions = { Action.MANUAL_MANAGE, Action.NOT_EXECUTED, Action.SEND_MSG_TO_OTEX, Action.ORDER_RETRY, Action.SEND_DDE_DATA };

    private MifidConfig mifidConfig;
    private MarketFinder marketFinder;
    private CustomerFinder customerFinder;
    private MarketConnectionRegistry marketConnectionRegistry;
    private OperationStateAuditDao operationStateAuditDao;
    private static final DecimalFormat df = new DecimalFormat("#,##0.000#");
    private RegulatedMktIsinsLoader regulatedMktIsinsLoader;
    private List<String> regulatedMarketPolicies;
    private String internalMMcodes;
    private List<String> internalMMcodesList;
    static private String dateFormat = "dd/MM/yyyy";
    
    private boolean isLogEnabled = false;

    private final Map<String, Long> startSaveTimeInMillis = new ConcurrentHashMap<String, Long>();

    /**
     * Sets the mifid config.
     * 
     * @param mifidConfig
     *            the new mifid config
     */
    public void setMifidConfig(MifidConfig mifidConfig) {
        this.mifidConfig = mifidConfig;
    }

    /**
     * Sets the operation state audit dao.
     * 
     * @param operationStateAuditDao
     *            the new operation state audit dao
     */
    public void setOperationStateAuditDao(OperationStateAuditDao operationStateAuditDao) {
        this.operationStateAuditDao = operationStateAuditDao;
    }

    /**
     * Sets the market connection registry.
     * 
     * @param marketConnectionRegistry
     *            the new market connection registry
     */
    public void setMarketConnectionRegistry(MarketConnectionRegistry marketConnectionRegistry) {
        this.marketConnectionRegistry = marketConnectionRegistry;
    }
    
    public MarketConnectionRegistry getMarketConnectionRegistry() {
		return marketConnectionRegistry;
	}

    /**
     * Sets the market finder.
     * 
     * @param marketFinder
     *            the new market finder
     */
    public void setMarketFinder(MarketFinder marketFinder) {
        this.marketFinder = marketFinder;
    }

    /**
     * Gets the internal m mcodes.
     * 
     * @return the internal m mcodes
     */
    public String getInternalMMcodes() {
        return internalMMcodes;
    }

    /**
     * Sets the internal m mcodes.
     * 
     * @param internalMMcodes
     *            the new internal m mcodes
     */
    public void setInternalMMcodes(String internalMMcodes) {
        this.internalMMcodes = internalMMcodes;
        /*
         * Initialize the List, it's a better and more reliable method to check for the presence of a given string. With the list we check
         * the object, so RAKRO doesn't contain AKRO, instead with the string RAKRO contains AKRO and it's not correct (it seems to be only
         * a test environment's problem).
         */
        internalMMcodesList = new ArrayList<String>();
        String[] mmSplit = internalMMcodes.split(",");
        for (int count = 0; count < mmSplit.length; count++) {
            internalMMcodesList.add(mmSplit[count]);
            LOGGER.debug("Internal MM added: {}", mmSplit[count]);
        }
    }

    
    /**
     * Gets the regulated mkt isins loader.
     * 
     * @return the regulated mkt isins loader
     */
    public RegulatedMktIsinsLoader getRegulatedMktIsinsLoader() {
        return regulatedMktIsinsLoader;
    }

    /**
     * Sets the regulated mkt isins loader.
     * 
     * @param regulatedMktIsinsLoader
     *            the new regulated mkt isins loader
     */
    public void setRegulatedMktIsinsLoader(RegulatedMktIsinsLoader regulatedMktIsinsLoader) {
        this.regulatedMktIsinsLoader = regulatedMktIsinsLoader;
    }

    /**
     * Gets the regulated market policies.
     * 
     * @return the regulated market policies
     */
    public List<String> getRegulatedMarketPolicies() {
        return regulatedMarketPolicies;
    }

    /**
     * Sets the regulated market policies.
     * 
     * @param regulatedMarketPolicies
     *            the new regulated market policies
     */
    public void setRegulatedMarketPolicies(List<String> regulatedMarketPolicies) {
        this.regulatedMarketPolicies = regulatedMarketPolicies;
    }

    /**
     * Gets the customer finder.
     * 
     * @return the customer finder
     */
    public CustomerFinder getCustomerFinder() {
        return customerFinder;
    }

    /**
     * Sets the customer finder.
     * 
     * @param customerFinder
     *            the new customer finder
     */
    public void setCustomerFinder(CustomerFinder customerFinder) {
        this.customerFinder = customerFinder;
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.OperationStateListener#onOperationStateChanged(it.softsolutions.bestx.Operation,
     * it.softsolutions.bestx.OperationState, it.softsolutions.bestx.OperationState)
     */
    @Override
    public void onOperationStateChanged(Operation operation, OperationState oldState, OperationState newState) throws SaveBookException {
        LOGGER.trace("{}, {}, {}", operation, oldState, newState);

        String orderID = operation.getIdentifier(OperationIdType.ORDER_ID);
        if (orderID != null) {
            startSaveTimeInMillis.put(orderID, DateService.currentTimeMillis());
        }

        checkPreRequisites();

        if (newState.getType() == OperationState.Type.Initial || operation.isVolatile()) {
            return; // No Audit - Technical state
        }
        

        // Step 1. first of all, update the order
        Order order = operation.getOrder();

        if (order == null) {
            // situazione inconsistente, non proseguire!!!
            Exception exception = new Exception();
            throw new SaveBookException("Unable to retrieve a valid order for operation [" + operation + "]", "N/A", -1, null, -1, exception);
        }

        String comment = retrieveComment(operation, oldState, newState);
        //remove the prefix, here it is not needed
        String cleanComment = LimitFileHelper.getInstance().removePrefix(comment);
        if (comment != null) {
            LOGGER.info("[INT-TRACE] operationID={}, audit comment: {}", operation.getId(), cleanComment);
        }
        
        //NO MORE if the order is a limit file we must update the already existing comment (there could have been one when we received the order from OTEX)
        /*
        if (order.isLimitFile() && order.getText() != null && LimitFileHelper.getInstance().isLimitFileComment(order.getText())) {
            //order.setText(LimitFileHelper.getInstance().updateComment(comment, order));
            order.setText(LimitFileHelper.getInstance().removePrefix((comment);
            LOGGER.debug("Order comment, Text fix field, set to [{}]", order.getText());
        }
        */
        
        Action[] availableActions = retrieveActions(operation, oldState, newState, cleanComment);

        String orderText = order.getText();
        if (orderText != null) {
            //if there is a Text field, we must remove the prefix, it must only be sent to the customer through execution reports
            orderText = LimitFileHelper.getInstance().removePrefix(orderText);
        }
        operationStateAuditDao.updateOrder(order, operation.getState(), false, order.isLaw262Passed(), cleanComment, availableActions, orderText, operation.isNotAutoExecute());

        // Step 2. further processing
        furtherProcessing(operation, oldState, newState);

        int attemptNo = operation.getAttemptNo();
        
        // Save new TabHistoryStati only after the corresponding TabHistoryOrdini
        if ( newState.getType() != OperationState.Type.CurandoRetry 
        		&& newState.getType() != OperationState.Type.CurandoAuto) {
        		operationStateAuditDao.saveNewState(order.getFixOrderId(), oldState, newState, attemptNo, cleanComment);
         }
    }

    private Action[] retrieveActions(Operation operation, OperationState oldState, OperationState newState, String comment) {
        Action[] actions = null;

        OperationState.Type type = newState.getType();
        Market.MarketCode marketCode = newState.getMarketCode();

        switch (type) {
        case Curando:
        case InternalInCurando:
        case OrderNotExecutable:
            actions = curandoActions;
            break;
        case OrderRejectable:
            actions = rejectableActions;
            break;
        case Error:
            actions = errorActions;
            break;
        case Standby:
            if (marketCode == MarketCode.BLOOMBERG) {
                actions = bbgStandByActions;
            }
            break;
        case StartExecution:
            break;
        case MatchFound:
            actions = matchFoundActions;
            break;
        case Warning:
            if (comment.indexOf(Messages.getString("InternalizationSystemOrderFilter.0")) > -1 || comment.indexOf(Messages.getString("InternalizationSystemOrderFilter.1")) > -1
                            || comment.indexOf(Messages.getString("InternalizationSystemOrderFilter.2")) > -1 || comment.indexOf(Messages.getString("InternalizationSystemOrderFilter.3")) > -1) {
                actions = warningInternalProductActions;
            } else {
                actions = warningActions;
            }
            break;
        case LimitFileNoPrice:
            actions = limitFileNoPriceActions;
            break;
        default:
            break;
        }

        return actions;
    }

    private void furtherProcessing(Operation operation, OperationState oldState, OperationState newState) throws SaveBookException {
        Order order = operation.getOrder();
        int attemptNo = operation.getAttempts() != null ? operation.getAttempts().size() - 1 : 0;
        int executionReportNo = operation.getExecutionReports() != null ? operation.getExecutionReports().size() - 1 : 0;
        String tsn = operation.getIdentifier(OperationIdType.BTS_TSN);

        OperationState.Type newStateType = newState.getType();
        OperationState.Type oldStateType = oldState.getType();
        Market.MarketCode marketCode = newState.getMarketCode();

        switch (newStateType) {
        case Curando: {
            try {
            		operation.lastSavedAttempt = operationStateAuditDao.saveNewAttempt(order.getFixOrderId(), operation.getLastAttempt(), null, attemptNo, null, operation.lastSavedAttempt);
                	auditMarketStatus(order.getFixOrderId(), attemptNo);
                	auditServicesStatus(order.getFixOrderId(), operation.getLastAttempt(), attemptNo);
                if ((oldStateType == OperationState.Type.WaitingPrice || oldStateType == OperationState.Type.CurandoRetry) && (operation.getLastAttempt().getSortedBook() != null)) {
                    operationStateAuditDao.saveNewBook(order.getFixOrderId(), attemptNo, operation.getLastAttempt().getSortedBook());
                }
            } catch (Exception e) {
                LOGGER.error("Unable to save the attempt, probably already present on DB: {}", e.getMessage());
            }
        }
        break;
        case Executed: {
        	List<Attempt> attemptList = operation.getAttempts();
        	if(attemptList.size() > 0 ) {

        		Attempt attempt = attemptList.get(attemptList.size() - 1);
        		if (operation.getExecutionReports().size() > 0) {
        			try {
        				ExecutionReport executionReport = operation.getExecutionReports().get(operation.getExecutionReports().size() - 1);
        				operationStateAuditDao.finalizeOrder(order, operation.getLastAttempt(), operation.getExecutionReports().get(executionReportNo), executionReport.getTransactTime());
        				operationStateAuditDao.updateAttempt(order.getFixOrderId(), attempt, tsn, attemptNo, executionReport.getTicket(), executionReport);
        			} catch (Exception e) {
        				LOGGER.info("Unable to update order: {}", e.getMessage(), e);
        			}
        		}
        	} else {
        		LOGGER.info("No previous execution reports received for order {} - Going to Warning State", order.getFixOrderId());
        		operation.setStateResilient(new WarningState(operation.getState(), new Exception("Created by chance"), "No previous execution reports received for order"), ErrorState.class);
        	}
        }
        break;
        case FormalValidationOK: {
            // 20111026 - Ruggero - AKR-1207
            // in the formal validation there could be an update of the order field futSettDate because we could receive it empty.
            // Here we save on the db the eventual change, rewriting the same value if the field has been received with a valid date.
           
           //20200722 - SP - BESTX-703 - No need to update because we don't recalculate empty settlement date
            //operationStateAuditDao.updateOrderSettlementDate(order);
        }
        break;
        case ManualManage:
        {
            if (oldState.getType() == OperationState.Type.ManualExecutionWaitingPrice) {
                operation.lastSavedAttempt = operationStateAuditDao.saveNewAttempt(order.getFixOrderId(), operation.getLastAttempt(), null, attemptNo, null, operation.lastSavedAttempt);
                auditMarketStatus(order.getFixOrderId(), attemptNo);
                auditServicesStatus(order.getFixOrderId(), operation.getLastAttempt(), attemptNo);
                if ((oldStateType == OperationState.Type.WaitingPrice || oldStateType == OperationState.Type.CurandoRetry) && (operation.getLastAttempt().getSortedBook() != null)) {
                	operationStateAuditDao.saveNewBook(order.getFixOrderId(), attemptNo, operation.getLastAttempt().getSortedBook());
                }
            }
        }
            break;
        case MatchFound:
        {
            if (operation.getMatchingOperation() != null) {
                operationStateAuditDao.updateMatchingOrderAttempt(order.getFixOrderId(), attemptNo, operation.getMatchingOperation().getOrder().getFixOrderId());
            }
        }
            break;
        case OrderNotExecutable: {
            //[RR20130807] we must always save the new attempt when we reach this status
            operation.lastSavedAttempt = operationStateAuditDao.saveNewAttempt(order.getFixOrderId(), operation.getLastAttempt(), null, attemptNo, null, operation.lastSavedAttempt);
            auditMarketStatus(order.getFixOrderId(), attemptNo);
            auditServicesStatus(order.getFixOrderId(), operation.getLastAttempt(), attemptNo);
            if ((oldStateType == OperationState.Type.WaitingPrice || oldStateType == OperationState.Type.CurandoRetry) && (operation.getLastAttempt().getSortedBook() != null)) {
                operationStateAuditDao.saveNewBook(order.getFixOrderId(), attemptNo, operation.getLastAttempt().getSortedBook());
            }
        }
        break;
        case OrderNotExecuted: {
//            List<Attempt> attemptList = operation.getAttempts();
//            Attempt attempt = attemptList.get(attemptList.size() - 1);
           	Attempt attempt = operation.getLastAttempt();
           	if (operation.getExecutionReports().size() > 0) {
           	   ExecutionReport executionReport = operation.getExecutionReports().get(operation.getExecutionReports().size() - 1);
               if (operation.lastSavedAttempt == attemptNo) {
            	   operationStateAuditDao.updateAttempt(order.getFixOrderId(), attempt, tsn, attemptNo, executionReport.getTicket(), executionReport);
               } else {
            	   operation.lastSavedAttempt = operationStateAuditDao.saveNewAttempt(order.getFixOrderId(), attempt, tsn, attemptNo, executionReport.getTicket(), operation.lastSavedAttempt);
               }
            }
        }
        break;
        case OrderReceived: {
            String comment = getComment(false, marketCode, newStateType, newState.getComment());
            operationStateAuditDao.addOrderCount();
            operationStateAuditDao.saveNewOrder(order, operation.getState(), mifidConfig.getPropName(), mifidConfig.getPropCode(), "", comment, new Action[0],
                            order.getTransactTime(), operation.getIdentifier(OperationIdType.FIX_SESSION), operation.isNotAutoExecute());
            operation.lastSavedAttempt = operationStateAuditDao.saveNewAttempt(order.getFixOrderId(), operation.getLastAttempt(), null, attemptNo, null, operation.lastSavedAttempt);
        }
        break;
        case OrderRevocated: {
            if (oldState.getType() == OperationState.Type.WaitingPrice || oldStateType == OperationState.Type.CurandoRetry) {
                operation.lastSavedAttempt = operationStateAuditDao.saveNewAttempt(order.getFixOrderId(), operation.getLastAttempt(), null, attemptNo, null, operation.lastSavedAttempt);
                
                if (operation.getLastAttempt().getSortedBook() != null) {
                   operationStateAuditDao.saveNewBook(order.getFixOrderId(), attemptNo, operation.getLastAttempt().getSortedBook());
                }
            }
        }
        break;
        case InternalGetExecutableQuote: {
            operation.lastSavedAttempt = operationStateAuditDao.saveNewAttempt(order.getFixOrderId(), operation.getLastAttempt(), null, attemptNo, null, operation.lastSavedAttempt);
            auditMarketStatus(order.getFixOrderId(), attemptNo);
            auditServicesStatus(order.getFixOrderId(), operation.getLastAttempt(), attemptNo);
            if (oldStateType == OperationState.Type.WaitingPrice || oldStateType == OperationState.Type.CurandoRetry) {
            	operationStateAuditDao.saveNewBook(order.getFixOrderId(), attemptNo, operation.getLastAttempt().getSortedBook());
            }
        }
            break;
        case StartExecution: {
            switch (marketCode) {
            case BLOOMBERG:
            case TW: 
            case MARKETAXESS: {
                operation.lastSavedAttempt = operationStateAuditDao.saveNewAttempt(order.getFixOrderId(), operation.getLastAttempt(), null, attemptNo, null, operation.lastSavedAttempt);
                auditMarketStatus(order.getFixOrderId(), attemptNo);
                auditServicesStatus(order.getFixOrderId(), operation.getLastAttempt(), attemptNo);
//                if (oldStateType == OperationState.Type.WaitingPrice || oldStateType == OperationState.Type.CurandoRetry || newState.mustSaveBook()) {
                  if(operation.getLastAttempt().getSortedBook() != null) {
                	operationStateAuditDao.saveNewBook(order.getFixOrderId(), attemptNo, operation.getLastAttempt().getSortedBook());
                }
            }
            break;
            default:
                break;
            }

        }
        break;
        case InternalSendRfqToInternal: 
        	break;
        case LimitFileNoPrice: {
            try {
            	operation.lastSavedAttempt = operationStateAuditDao.saveNewAttempt(order.getFixOrderId(), operation.getLastAttempt(), null, attemptNo, null, operation.lastSavedAttempt);
                auditMarketStatus(order.getFixOrderId(), attemptNo);
                auditServicesStatus(order.getFixOrderId(), operation.getLastAttempt(), attemptNo);
                if ((oldStateType == OperationState.Type.WaitingPrice || oldStateType == OperationState.Type.CurandoRetry || oldStateType == OperationState.Type.Rejected) && (operation.getLastAttempt().getSortedBook() != null)) {
                	operationStateAuditDao.saveNewBook(order.getFixOrderId(), attemptNo, operation.getLastAttempt().getSortedBook());
                }
            } catch (Exception e) {
            	LOGGER.error("Unable to save the attempt, probably already present on DB: {}", e.getMessage());
            }
        }
        break;
        case RejectQuote: // there are no markets allowing quote rejection
            break;
        case SendAutoNotExecutionReport: {
            try {
            	//if(oldStateType != Type.Rejected) {   When state is rejected the attempt has no more been saved already 
            		// typical scenario where the save must not be attempted is when the market rejected handler detect a wide spread condition
               operation.lastSavedAttempt = operationStateAuditDao.saveNewAttempt(order.getFixOrderId(), operation.getLastAttempt(), null, attemptNo, null, operation.lastSavedAttempt);	
            	auditMarketStatus(order.getFixOrderId(), attemptNo);
               auditServicesStatus(order.getFixOrderId(), operation.getLastAttempt(), attemptNo);
            	//} 
               if(oldStateType != OperationState.Type.Rejected) { 
	            	if (oldStateType == OperationState.Type.WaitingPrice || oldStateType == OperationState.Type.CurandoRetry) {
	            		operationStateAuditDao.saveNewBook(order.getFixOrderId(), attemptNo, operation.getLastAttempt().getSortedBook());
	            	}
            	}
            } catch (Exception e) {
            	LOGGER.error("Unable to save the attempt, probably already present on DB: {}", e.getMessage());
            }
        }
        break;
        case SendExecutionReport: {
            ExecutionReport executionReport = operation.getExecutionReports().get(operation.getExecutionReports().size() - 1);
            String marketMaker = (executionReport != null) ? executionReport.getExecBroker() : null;
            if (marketMaker == null) {
                marketMaker = "";
            }
            LOGGER.debug("Checking if property should be BEST or remain SPREAD");
            LOGGER.debug("The last execution report's market maker is {}", marketMaker);
            if (oldState instanceof ManualManageState) {
                // [RR20110610] We must know if the customer has been granted the access to IS (Internalizzatore Sistematico) thus we
                // extract
                // the related customer attribute if available, if not we assume that the customer has not been allowed to access the IS.
                Customer customer = order.getCustomer();
                CustomerAttributesIFC custAttr = customer.getCustomerAttributes();
                boolean internalCust = false;
                if (custAttr != null) {
                    internalCust = ((CustomerAttributes) custAttr).getInternalCustomer();
                    LOGGER.debug("Order {}, customer {}, extracted the attribute InternalCustomer, value {}", order.getFixOrderId(), customer.getName(), internalCust);
                }
                Market execMkt = executionReport != null ? executionReport.getMarket() : null;
                boolean executingOnInternalMarket = false;
                // 20110610 - Ruggero
                // The execution destination can be AKIS only for customer for which the IS has been allowed
                // and ONLY WHEN we are executing on the internal market
                if (executionReport != null && internalCust && executingOnInternalMarket) {
                    operationStateAuditDao.updateOrderExecutionDestination(order.getFixOrderId(), Order.IS_EXECUTION_DESTINATION);
                }
            }

            // A manual execution can set the property to BEST, here we save it on the audit table
            if (executionReport != null && ExecutionReport.PROPERTY_BEST.equals(executionReport.getProperty())) {
                LOGGER.debug("Execution report property {}, update the audit table.", ExecutionReport.PROPERTY_BEST);
                operationStateAuditDao.updateOrderPropName(order.getFixOrderId(), executionReport.getProperty());
            }
            LOGGER.debug("Property check DONE");

            if (executionReport == null || executionReport.getMarket() == null) {
                LOGGER.error("Managing an execution report with no market associated");
            }
            MarketCode executionReportMarketCode = executionReport != null && executionReport.getMarket() != null ? executionReport.getMarket().getMarketCode() : null;
            if (executionReport != null && executionReport.getTicket() != null && executionReportMarketCode == MarketCode.BLOOMBERG) {
                operationStateAuditDao.updateOrderFill(order, executionReport.getTicket(), executionReport.getAccruedInterestAmount(), executionReport.getAccruedInterestDays());
            }
        }
        break;
        case Rejected:
        	if (newState instanceof RejectedState) {
            	try {
            		this.operationStateAuditDao.saveExecutablePrices(order.getFixOrderId(), attemptNo, order.getInstrument().getIsin(), operation.getLastAttempt().getExecutablePrices());
            	} catch (Exception e) {
            		LOGGER.error("Not able to save executable prices", e);
            	}
        	}
        	break;
        case MarketExecuted:
        	try {
        		this.operationStateAuditDao.saveExecutablePrices(order.getFixOrderId(), attemptNo, order.getInstrument().getIsin(), operation.getLastAttempt().getExecutablePrices());
        	} catch (Exception e) {
        		LOGGER.error("Not able to save executable prices", e);
        	}
        	break;
        default:
            break;
        }
    }

    /**
     * Gets fr4om Message resource the market comment. Requires that the comments are replicated for each market with market name
     * @param append flag
     * @param marketCode market code as 
     * @param type type of the state managed
     * @param comment additional comment
     * @param params params required by the comment retrieved in the message resources
     * @return
     */
    private String getComment(boolean append, MarketCode marketCode, OperationState.Type type, String comment, Object... params) {
        if (type == null) {
            throw new IllegalArgumentException("type is null");
        }

        String key = marketCode != null ? marketCode + "_" + type : type.toString();
        String res = params != null ? Messages.getString(key, params) : Messages.getString(key);

        if (comment != null && comment.length() > 0) {
            res = append ? (comment + " - " + res) : comment;
        }

        return res;
    }

    /**
     * Restituisce il commento di Messages, e fa l'append se il comment passato è valorizzato
     */
    private String getComment(MarketCode marketCode, OperationState.Type type, String comment, Object... params) {
        return getComment(true, marketCode, type, comment, params);
    }

    private String getProposalMarketMakerCode(Attempt lastAttempt) {

        String res = null;
        if (lastAttempt == null
                        || lastAttempt.getExecutionProposal() == null
                        || lastAttempt.getExecutionProposal().getMarketMarketMaker() == null
                        || lastAttempt.getExecutionProposal().getMarketMarketMaker().getMarketMaker() == null
                        ) {
            return res;
        }
        try {
            res = lastAttempt.getExecutionProposal().getMarketMarketMaker().getMarketMaker().getCode();
        } catch (NullPointerException e) {
        }
        return res;
    }

    private String getMarketOrderMarketMakerCode(Attempt lastAttempt) {

        String res = null;
        try {
            res = lastAttempt.getMarketOrder().getMarketMarketMaker().getMarketMaker().getCode();
        } catch (NullPointerException e) {
        	; // leave res to null value
        }
        return res;
    }

    private String getExecutionReportPrice(List<ExecutionReport> executionReports) {
        String res = null;
        if (executionReports == null 
                        || executionReports.isEmpty()
                        || executionReports.get(executionReports.size() - 1).getLastPx() == null
                        ) {
            return res;
        }
        try {
            res = df.format(executionReports.get(executionReports.size() - 1).getLastPx());
        } catch (NullPointerException e) {
        }
        return res;
    }

    private String getExecutionReportMarketMaker(List<ExecutionReport> executionReports) {
        String res = null;
        if (executionReports != null 
                        && !executionReports.isEmpty()
                        && executionReports.get(executionReports.size() - 1) != null
                        ) {
        	res = executionReports.get(executionReports.size() - 1).getExecBroker();
        }
        return res;
    }

    private String retrieveComment(Operation operation, OperationState oldState, OperationState newState) throws SaveBookException {
        Order order = operation.getOrder();
        String orderText = order.getText();
        if (orderText != null) {
            //if there is a Text field, we must remove the prefix, it must only be sent to the customer through execution reports
            orderText = LimitFileHelper.getInstance().removePrefix(orderText);
        }
        String comment = newState.getComment();

        if (oldState.getType() == OperationState.Type.BusinessValidation) {
            LOGGER.debug("Updating order coming from BusinessValidationState.");
            operationStateAuditDao.updateOrder(order, operation.getState(), false, order.isLaw262Passed(), comment, null, orderText, operation.isNotAutoExecute());
        }
        // In queste 2 chiamate potrebbe non esserci sempre tutta la catena (potremmo avere NullPointerException)
        String proposalMarketMaker = getProposalMarketMakerCode(operation.getLastAttempt());
        String orderMarketMaker = getMarketOrderMarketMakerCode(operation.getLastAttempt());
        String executionMarketMaker = getExecutionReportMarketMaker(operation.getExecutionReports());
        String executionReportPrice = getExecutionReportPrice(operation.getExecutionReports());
        String executionProposalAmount = getLimitAttemptPrice(operation.getLastAttempt());
        String executionProposalFutSettDate = null; 
        
        if (operation.getLastAttempt() != null && operation.getLastAttempt().getMarketOrder() != null && operation.getLastAttempt().getMarketOrder().getFutSettDate() != null) {
           executionProposalFutSettDate = DateService.format(dateFormat, operation.getLastAttempt().getMarketOrder().getFutSettDate());
        }

        OperationState.Type type = newState.getType();
        Market.MarketCode marketCode = newState.getMarketCode();

        LOGGER.debug("FIXOrderID = {}, marketCode = {}, type = {}", order.getFixOrderId(), marketCode, type);

        switch (type) {
        case BusinessValidation:
        case Curando:
        case DifferentDatesExecuted:
        case Error:
        case Executed:
        case FormalValidationOK:
        case FormalValidationKO:
        case InternalInCurando:
        case LimitFileNoPrice:
        case ManualManage:
        case MatchFound:
        case OrderNotExecutable:
        case OrderNotExecuted:
        case OrderRejectable:
        case OrderRevocated:
        case RejectQuote:
        case RejectQuoteAndAutoNotExecutionReport:
        case SendAutoNotExecutionReport:
        case SendExecutionReport:
        case SendNotExecutionReport:
        case SendOrder:
        case SendRfq:
        case ValidateByPunctualFilter:
        case WaitingPrice:
        case Warning:
            comment = getComment(false, marketCode, type, comment);
            break;
        case Cancelled:
        	comment = getComment(false, marketCode, type, comment, createPobexInformation(operation));
        	break;
        case Rejected:
        	String reason = null;
        	if(orderMarketMaker == null) {
        		if(marketCode == null) {
        			reason = proposalMarketMaker;
        		}
        		else {
        			reason = marketCode.toString();
        		}
        	}
        	else {
        		reason = orderMarketMaker;
        	}
        	//BESTX-725 - SP03092020 - also BBG send POBEX in the reject state
        	Boolean isPOBEXMkt = marketCode != null && (marketCode.equals(Market.MarketCode.TW) || marketCode.equals(Market.MarketCode.BLOOMBERG));
            comment = getComment(isPOBEXMkt, marketCode, type, comment, isPOBEXMkt ? createPobexInformation(operation) : reason);
            break;
        case ManageCounter: {
            String counterOfferAmount = df.format(operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal().getPrice().getAmount());
            comment = getComment(false, marketCode, type, comment, counterOfferAmount);
        }
        break;
        case ManualWaitingFill: {
            // richiesta Tullio - 8 settembre 2008
            BigDecimal executionReportLastPx = operation.getExecutionReports().get(operation.getExecutionReports().size() - 1).getLastPx();
            comment = getComment(marketCode, type, comment, executionReportLastPx);
        }
        break;
        case MarketExecuted:
            switch (marketCode) {
            case BLOOMBERG:
                if (oldState.getType() == OperationState.Type.InternalAcceptInternalAndRejectBestState) {
                    comment = Messages.getString("InternalMM_MarketExecuted");
                }
                else {
                	Object[] params = { executionMarketMaker, executionReportPrice };
                    comment = getComment(false, marketCode, type, comment, params);
                }
                break;
            case TW: 
            case MARKETAXESS: {
            	Object[] params = { executionMarketMaker, executionReportPrice };
            	comment = getComment(false, marketCode, type, comment, params);
            }
            default:
                comment = getComment(false, marketCode, type, comment);
                break;
            }
            break;
        case AcceptQuote:
        	switch (marketCode) {
        	case BLOOMBERG: {
        		Object[] params = { proposalMarketMaker, executionProposalAmount };
        		comment = getComment(marketCode, type, comment, params);

        		if (oldState.getType() == OperationState.Type.InternalAcceptInternalAndRejectBestState) {
        			comment = "Internal MM rejected request - " + comment;
        		}
        	}
        		break;
        	default:
        		break;
        	}
        	break;
        case ReceiveQuote: {
            String counterOfferMarketMakerCode = operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal().getMarketMarketMaker().getMarketMaker().getCode();
            String counterOfferAmount = df.format(operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal().getPrice().getAmount());
            String counterOfferFutSettDate = DateService.format( dateFormat, operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal().getFutSettDate());

            Object[] params = { counterOfferMarketMakerCode, counterOfferAmount, counterOfferFutSettDate };
            comment = getComment(false, marketCode, type, comment, params);
        }
        break;
        case InternalReceiveExecutableQuote: 
        {
            String counterOfferMarketMakerCode = operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal().getMarketMarketMaker().getMarketMaker().getCode();
            String counterOfferAmount = df.format(operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal().getPrice().getAmount());
            String counterOfferFutSettDate = DateService.format(dateFormat, operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal().getFutSettDate());

            Object[] params = { counterOfferMarketMakerCode, counterOfferAmount, counterOfferFutSettDate };
            comment = getComment(false, marketCode, type, comment, params);
        }
        break;
        case InternalReceiveInternalQuote: 
        {
            String counterOfferMarketMakerCode = operation.getInternalAttempt().getCounterOffer().getMarketMarketMaker().getMarketMaker().getCode();
            String counterOfferAmount = df.format(operation.getInternalAttempt().getCounterOffer().getPrice().getAmount());
            String counterOfferFutSettDate = DateService.format(dateFormat, operation.getInternalAttempt().getCounterOffer().getFutSettDate());

            Object[] params = { counterOfferMarketMakerCode, counterOfferAmount, counterOfferFutSettDate };
            comment = getComment(false, marketCode, type, comment, params);
        }
        break;
        case Standby: {
            // 20111026 - Ruggero Ticket AKR-1207 : due to the shutdown of Bloomberg some instruments have not the BBSettlementDate or
            // have an old one. The ORDER_BBG_STANDBY message will no more include the settlement date.
            Object[] params = { proposalMarketMaker, executionProposalAmount };
            comment = getComment(marketCode, type, comment, params);
        }
        break;
        case StartExecution:
            switch (marketCode) {
            case BLOOMBERG:
            case TW:
            case MARKETAXESS: {
                Object[] params = { orderMarketMaker, executionProposalAmount, executionProposalFutSettDate };
                comment = getComment(marketCode, type, comment, params);
            }
            break;
            default:
                break;
            }
            break;
        case InternalSendRfqToInternal:
            switch (marketCode) {
           case BLOOMBERG: {
                Object[] params = { getBestExecutableAmount(operation.getLastAttempt()) };
                comment = getComment(marketCode, type, comment, params);
            }
            break;
            default:
                break;
            }
            break;
        case InternalSendRfqToBest:
            break;
        case WaitingFill: {
            Object[] params = { orderMarketMaker, executionReportPrice };
            comment = getComment(false, marketCode, type, comment, params);
        }
        break;
        default:
            break;
        }

        return comment;
    }

/*    private String getExecutionProposalAmount(Attempt lastAttempt) {
        String res = null;

        if (lastAttempt == null) {
            return res;
        }

        try {
            if (lastAttempt.getExecutionProposal() != null
                            && lastAttempt.getExecutionProposal().getPrice() != null
                            && lastAttempt.getExecutionProposal().getPrice().getAmount() != null) {

                res = df.format(lastAttempt.getExecutionProposal().getPrice().getAmount());
            }
            else if (lastAttempt.getExecutablePrice(0) != null && lastAttempt.getExecutionProposal() != null
                            && lastAttempt.getExecutionProposal().getPrice() != null
                            && lastAttempt.getExecutionProposal().getPrice().getAmount() != null) {

                res = df.format(lastAttempt.getExecutionProposal().getPrice().getAmount());
            }
        } catch (NullPointerException e) {
        }
        return res;
    }
*/    
    private String getLimitAttemptPrice(Attempt lastAttempt) {
        String res = null;

        if (lastAttempt == null) {
            return res;
        }

        try {
            if (lastAttempt.getMarketOrder() != null
                            && lastAttempt.getMarketOrder().getLimit() != null
                            && lastAttempt.getMarketOrder().getLimit().getAmount() != null) {

                res = df.format(lastAttempt.getMarketOrder().getLimit().getAmount());
            }
        } catch (NullPointerException e) {
        	LOGGER.info("Unable to get market limit price for a NullPointerException in execution attempt: {}", lastAttempt.toString());
        }
        return res;
    }

    // gets the best executable amount
    private String getBestExecutableAmount(Attempt lastAttempt) {
        String res = null;
        if (lastAttempt == null) {
            return res;
        }
        try {
            if ( (lastAttempt.getExecutablePrice(0) != null && lastAttempt.getExecutablePrice(0).getClassifiedProposal() != null) && (lastAttempt.getExecutablePrice(0).getClassifiedProposal().getPrice() != null) ) {
                res = df.format(lastAttempt.getExecutablePrice(0).getClassifiedProposal().getPrice().getAmount());
            }
        } catch (NullPointerException e) {
        }
        return res;
    }
    
    private void auditMarketStatus(String orderId, int attemptNo) {
        for (MarketConnection marketConnection : marketConnectionRegistry.getAllMarketConnections()) {
            boolean disabled = false;
            if (marketConnection.isBuySideConnectionProvided()) {
                disabled = !(marketConnection.isBuySideConnectionEnabled() && marketConnection.isBuySideConnectionAvailable());
            }

            if (marketConnection.isPriceConnectionProvided()) {
                disabled = disabled || !(marketConnection.isPriceConnectionEnabled() && marketConnection.isPriceConnectionAvailable());
            }

            operationStateAuditDao.saveMarketAttemptStatus(orderId, attemptNo, marketConnection.getMarketCode(), disabled, marketConnection.getDisableComment());
        }
    }
    
    private void auditServicesStatus(String orderId, Attempt currentAttempt, int attemptNo) {
       Map<String, ServiceStatus> status = currentAttempt.getServicesStatus();
       
       for (String service : status.keySet()) {
          operationStateAuditDao.saveServiceAttemptStatus(orderId, attemptNo, service, status.get(service).isDisabled(), status.get(service).getDownCause());
       }
    }
    
    
    
    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (operationStateAuditDao == null) {
            throw new ObjectNotInitializedException("Operation state audit DAO not set");
        }
        if (mifidConfig == null) {
            throw new ObjectNotInitializedException("MIFID config not set");
        }
        if (marketConnectionRegistry == null) {
            throw new ObjectNotInitializedException("Market connection registry not set");
        }
        if (marketFinder == null) {
            throw new ObjectNotInitializedException("Market finder not set");
        }
        if (regulatedMktIsinsLoader == null) {
            throw new ObjectNotInitializedException("RegulatedMktIsinsLoader not set");
        }
        if (internalMMcodes == null) {
            throw new ObjectNotInitializedException("InternalMMcodes not set");
        }
    }

    // salva in tabOrderFill sse il mercato e' uno dei Regulated Markets
    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.MarketExecutionListener#onMarketExecutionReport(it.softsolutions.bestx.Operation,
     * it.softsolutions.bestx.connections.MarketBuySideConnection, it.softsolutions.bestx.model.Order,
     * it.softsolutions.bestx.model.MarketExecutionReport)
     */
    @Override
    public void onMarketExecutionReport(Operation operation, MarketBuySideConnection source, Order order, MarketExecutionReport marketExecutionReport) {
    	// nothing to do
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.MarketExecutionListener#onMarketOrderCancelNoFill(it.softsolutions.bestx.Operation,
     * it.softsolutions.bestx.connections.MarketBuySideConnection, it.softsolutions.bestx.model.Order,
     * it.softsolutions.bestx.model.MarketExecutionReport)
     */
    @Override
    public void onMarketOrderCancelNoFill(Operation operation, MarketBuySideConnection source, Order order, MarketExecutionReport marketExecutionReport) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.MarketExecutionListener#onMarketPriceReceived(java.lang.String, java.math.BigDecimal,
     * it.softsolutions.bestx.model.Order, it.softsolutions.bestx.model.MarketExecutionReport)
     */
//    @Override
//    public void onMarketPriceReceived(String orderId, BigDecimal marketPrice, Order order, MarketExecutionReport marketExecutionReport) {
//        /*
//         * 29-06-2009 Ruggero Price received, it's also a new fill, we save it and then update the order state description including details
//         * about the market provided price.
//         */
//        if (marketExecutionReport != null) {
//            operationStateAuditDao.savePriceFill(order, marketExecutionReport);
//        }
//
//        operationStateAuditDao.updateOrderStatusDescription(order, marketPrice);
//    }
    
    public String createPobexInformation(Operation operation) {
    	Attempt currentAttempt = operation.getLastAttempt();
    	int size = currentAttempt.getExecutablePrices().size();
		int index = 0;
		if(currentAttempt.getExecutablePrice(0) == null) {
		   index =1;
		   size++;
		}
		String ret = new String();
		String quoteData;
		for(; index < size; index++) {
			ExecutablePrice quote = currentAttempt.getExecutablePrice(index);
			if(quote != null && (quote.getMarketMarketMaker() != null || quote.getOriginatorID() != null)) {
				quoteData = "";
				try {
					quoteData += quote.getMarketMarketMaker().getMarketMaker().getCode();
				} catch (@SuppressWarnings("unused") Exception e) {
				   if (quote.getOriginatorID() != null) {
					   quoteData += quote.getOriginatorID();
				   } else {
				      continue;
				   }
				}
				quoteData += " ";
				if(quote.getPrice() != null){
					quoteData += quote.getPrice().getAmount();
				} else {
					quoteData += "0";
				}
				quoteData += " " + CSPOBexExecutionReport.convertAuditState(quote.getAuditQuoteState());
				ret += quoteData + "; ";
			}
		}
		return ret;
    }
}
