/*
 * Copyright 1997-2015 SoftSolutions! srl 
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
package it.softsolutions.bestx.handlers.tradeweb;

import java.util.ArrayList;
import java.util.List;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.Operation.RevocationState;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.dao.BestXConfigurationDao;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.markets.tradeweb.TradewebMarket;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketExecutionReport.RejectReason;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.tradeweb.TW_CancelledState;
import it.softsolutions.bestx.states.tradeweb.TW_ExecutedState;
import it.softsolutions.bestx.states.tradeweb.TW_RejectedState;
import it.softsolutions.bestx.states.tradeweb.TW_SendOrderState;

/**
 * 
 * Purpose: order sending event handler. Manages all the possible cases resulting from an order sent to the market.
 * 
 * Project Name : bestx-tradeweb-market 
 * First created by: ruggero.rizzo 
 * Creation date: 27/gen/2015
 * 
 **/
public class TW_SendOrderEventHandler extends BaseOperationEventHandler {
    
    private static final long serialVersionUID = -1021253573602664973L;

    private static final Logger LOGGER = LoggerFactory.getLogger(TW_SendOrderEventHandler.class);
    public static final String REVOKE_TIMER_SUFFIX="#REVOKE";
    private SerialNumberService serialNumberService;
    protected long waitingExecutionDelay;
	protected MarketBuySideConnection twConnection;
	protected long orderCancelDelay;
	private BestXConfigurationDao bestXConfigurationDao;
	
    public TW_SendOrderEventHandler(Operation operation, MarketBuySideConnection twConnection, SerialNumberService serialNumberService, long waitingExecutionDelay, long orderCancelDelay, BestXConfigurationDao bestXConfigurationDao) {
    	super(operation);
        this.serialNumberService = serialNumberService;
        this.waitingExecutionDelay = waitingExecutionDelay;
        this.twConnection = twConnection;
        this.orderCancelDelay = orderCancelDelay;
        this.bestXConfigurationDao = bestXConfigurationDao;
    }

    @Override
    public void onNewState(OperationState currentState) {
        try {
            setupDefaultTimer(waitingExecutionDelay, false);
            twConnection.sendFokOrder(operation, operation.getLastAttempt().getMarketOrder());
        } catch (BestXException e) {
            LOGGER.error("An error occurred while sending FOK Order to TW", e);
            operation.setStateResilient(new WarningState(currentState, e, Messages.getString("TWMarketSendOrderError.0")), ErrorState.class);
            stopDefaultTimer();
        }
    }

	@Override
	public void onMarketOrderReject(MarketBuySideConnection source, Order order, String reason, String sessionId) {

		if (source.getMarketCode() != MarketCode.TW) {
			return;
		}
		stopDefaultTimer();
		// we have sent an order related to the last attempt, thus we could
		// receive rejects only for it
		String executionProposalQuoteId = operation.getLastAttempt().getMarketOrder().getMarketSessionId();
		if (executionProposalQuoteId != null && executionProposalQuoteId.equals(sessionId)) {
			stopDefaultTimer();
			if (!checkCustomerRevoke(order)) {
				if (reason != null && reason.length() > 0) {
					operation.setStateResilient(new TW_RejectedState(Messages.getString("TWRejectPrefix", reason)), ErrorState.class);
				} else {
					operation.setStateResilient(new TW_RejectedState(""), ErrorState.class);
				}
			}
		} else {
			LOGGER.warn("Received reject with sessionId {}, expected sessionId {}. Ignore the reject.", sessionId, executionProposalQuoteId);
		}
	}

    @Override
    public void onMarketExecutionReport(MarketBuySideConnection source, Order order, MarketExecutionReport marketExecutionReport) {
        Attempt currentAttempt = operation.getLastAttempt();
        if (currentAttempt == null) {
            LOGGER.error("No current Attempt found");
            operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("TWMarketAttemptNotFoundError.0")), ErrorState.class);
            return;
        }
        
        List<MarketExecutionReport> marketExecutionReports = currentAttempt.getMarketExecutionReports();
        if (marketExecutionReports == null) {
            marketExecutionReports = new ArrayList<MarketExecutionReport>();
            currentAttempt.setMarketExecutionReports(marketExecutionReports);
        }
        marketExecutionReports.add(marketExecutionReport);
        
        // Create Customer Execution Report from Market Execution Report
        ExecutionReport executionReport;
        try {
            executionReport = marketExecutionReport.clone();
        } catch (CloneNotSupportedException e1) {
            LOGGER.error("Error while trying to create Execution Report from Market Execution Report");
            operation.setStateResilient(new WarningState(operation.getState(), e1, Messages.getString("TWMarketExecutionReportError.0")), ErrorState.class);
            return;
        }
        
        long executionReportId = serialNumberService.getSerialNumber("EXEC_REP");
        
        executionReport.setLastPx(executionReport.getPrice().getAmount());
        marketExecutionReport.setLastPx(executionReport.getPrice().getAmount());
        executionReport.setSequenceId(Long.toString(executionReportId));

//        marketExecutionReport.setExecBroker(operation.getLastAttempt().getExecutionProposal().getMarketMarketMaker().getMarketMaker().getCode());
        // AMC TW market execution report has the MarketMaker code inside
        if(marketExecutionReport.getMarketMaker() != null){
        	executionReport.setExecBroker(marketExecutionReport.getMarketMaker().getCode());
        	executionReport.setCounterPart(marketExecutionReport.getMarketMaker().getCode());
        }
        executionReport.setMarketOrderID(marketExecutionReport.getMarketOrderID());
            
		operation.getExecutionReports().add(executionReport);
		ExecutionReportState execRepState = marketExecutionReport.getState();
		if (execRepState == null) {
			String errorMsg = "Execution report with null state!"; 
            LOGGER.error(errorMsg);
            operation.setStateResilient(new WarningState(operation.getState(), null, errorMsg), ErrorState.class);
            return;
		} else {
			String ordCancelRejTimer = super.getDefaultTimerJobName() + REVOKE_TIMER_SUFFIX;
			switch (execRepState) {
			case NEW:
				LOGGER.info("Order {}, received exec report in NEW state", operation.getOrder().getFixOrderId());
				break;
			case CANCELLED:
				stopDefaultTimer();
				LOGGER.info("Order {}, received exec report in CANCELLED state with message {}", operation.getOrder().getFixOrderId(), marketExecutionReport.getText());
				try {
					//this timer could not exist, in this case nothing will happen with this call
					stopTimer(ordCancelRejTimer);
				} catch (SchedulerException e) {
					LOGGER.warn("Error while stopping timer {}", ordCancelRejTimer, e);
				}
				
		        // case 1: order is sent to dealer, who answers with a worse price: we receive ExecutionReport/Canceled/Canceled [Target price not met/Quoted: <DLRList with dealer>]: attempt fails with reject
		        // case 2: order is sent to dealer, and in the Dealers list the dealer is not present: we receive ExecutionReport/Canceled/Canceled [Target price not met/Quoted: <DLRList without dealer>]: retry if less than maxRetries
		        // case 3: order is sent to dealer, who does not answer at all : we receive ExecutionReport/Canceled/Canceled [Trading session ended]: retry if less than maxRetries
				
				//AMC case 1-reject --> quotedDealers != null && marketOrderTarget != null && quotedDealers.toLowerCase().contains(marketOrderTarget.toLowerCase())
				//AMC case 2-retry ---> quotedDealers != null && !quotedDealers.toLowerCase().contains(marketOrderTarget.toLowerCase())
				//AMC case 3-retry ---> text.toLowerCase().contains((Messages.getString("TWMarketRfqNoAnswerMessage")).toLowerCase())
				
				String text = marketExecutionReport.getText();
				String quotedDealers = null;
				int quotedIndex = text.indexOf("Quoted");
				if (quotedIndex != -1) {
					quotedDealers = text.substring(quotedIndex + "Quoted:".length());
				}
				MarketOrder marketOrder = operation.getLastAttempt().getMarketOrder();
				String marketOrderTarget = marketOrder.getMarketMarketMaker().getMarketSpecificCode();
				LOGGER.debug("Order {}, target mm {}, quoted on {}", order.getFixOrderId(), marketOrderTarget, quotedDealers);
				
                String noAnswerMsg = (Messages.getString("TWMarketRfqNoAnswerMessage")).toLowerCase();
                //AMC boolean convenient to trigger message returned to audit in setStateResilient
				boolean isNoDealerReply = text.toLowerCase().contains(noAnswerMsg);

       		 	Object maxRetries = bestXConfigurationDao.loadProperty(TradewebMarket.TW_MAX_EXEC_RETRIES_PROPERTY);
                if ( !operation.isCustomerRevokeReceived() ) {  
		        	//[RR20150408] CRSBXTWIGR-9 if the QuotedDealers list sent by the market contains the dealer with whom we tried to execute,
		        	//proceed as usual with a technical failure
		        	 if ( ( quotedDealers != null && marketOrderTarget != null && quotedDealers.toLowerCase().contains(marketOrderTarget.toLowerCase()) ) 
		        		   ) {
						    marketExecutionReport.setState(ExecutionReportState.REJECTED);
						    marketExecutionReport.setReason(RejectReason.TRADER_REJECTED);
			                operation.setStateResilient(new TW_RejectedState(text), ErrorState.class);
		        	 } else if (quotedDealers != null && !quotedDealers.toLowerCase().contains(marketOrderTarget.toLowerCase()) || isNoDealerReply) {
		        		 //[RR20150408] CRSBXTWIGR-9 if the QuotedDealers list sent by the market does not contain the dealer with whom we tried to execute,
		        		 // retry to execute the order for a configured number of times
		        		 
		        		 LOGGER.debug("Order {}, loaded maximum number of retries: {}", order.getFixOrderId(), maxRetries);
		        		 if (maxRetries == null) {
		        			 LOGGER.warn("Order {}, received a CANCEL exec report with reason [{}], cannot proceed with execution retries due to unconfigured or misconfigured maximum number of trials", order.getFixOrderId(), text);
		        			 operation.setStateResilient(new TW_RejectedState(isNoDealerReply ? Messages.getString("TWNoReply.MaxRetriesNotConfigured") : Messages.getString("TWDealerNotInQuotedList.MaxRetriesNotConfigured", marketOrderTarget, quotedDealers)), ErrorState.class);
		        		 } else {
		        			 try {
		        				 Integer maxTries;
		        				 if (maxRetries instanceof Integer) {
		        					 maxTries = (Integer) maxRetries; 
		        				 } else {
		        					 maxTries = Integer.valueOf((String) maxRetries);
		        				 }
		        				 int actualTries = currentAttempt.getConsecutiveExecutionRetries();
				        		 LOGGER.info("AAA maxTries = {}, actualTries = {}", maxRetries, actualTries);

		        				 if(actualTries < maxTries) { 
		        					 LOGGER.info("Order {}, received a CANCEL exec report, the trader did not answer, resend the order to the market.", order.getFixOrderId());
		        					 currentAttempt.addConsecutiveTry();
		        					 operation.setStateResilient(new TW_SendOrderState(isNoDealerReply ? Messages.getString("TWNoReply.Retry") : Messages.getString("TWDealerNotInQuotedList.Retry", marketOrderTarget, quotedDealers)), ErrorState.class);
		        				 } else {
		        					LOGGER.info ("Order {}, received a CANCEL exec report with reason [{}], maximum resends {} reached", order.getFixOrderId(), quotedDealers, maxRetries);
									marketExecutionReport.setState(ExecutionReportState.REJECTED);
									marketExecutionReport.setReason(RejectReason.TRADER_REJECTED);
									//currentAttempt.resetConsecutiveRetries();
									operation.setStateResilient(new TW_RejectedState(isNoDealerReply ? Messages.getString("TWNoReply.Rejected", maxRetries) : Messages.getString("TWDealerNotInQuotedList.Rejected",  marketOrderTarget, quotedDealers, maxRetries)), ErrorState.class);
		        				 }
		        			 } catch (NumberFormatException nfe) {
		        				 LOGGER.error("Order {}, Misconfigured maximum retries [{}]", order.getFixOrderId(), maxRetries, nfe);
		        				 String errorMessage = "Misconfigured maximum number of TW execution retries: " + maxRetries;
		        				 operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);
		        			 }
		        		 } 
		        	 } else {
							marketExecutionReport.setState(ExecutionReportState.REJECTED);
							marketExecutionReport.setReason(RejectReason.TRADER_REJECTED);
							//currentAttempt.resetConsecutiveRetries();
							operation.setStateResilient(new TW_RejectedState(Messages.getString("TWRejectPrefix", marketExecutionReport.getText())), ErrorState.class);
		        	 }
		        } else {
		        	//currentAttempt.resetConsecutiveRetries();
					operation.setStateResilient(new TW_CancelledState(), ErrorState.class);
				}
				break;
            case REJECTED:
                stopDefaultTimer();
                //currentAttempt.resetConsecutiveRetries();               
                operation.setStateResilient(new TW_RejectedState(marketExecutionReport.getText()), ErrorState.class);
                break;
			case FILLED:
				stopDefaultTimer();
				try {
					//this timer could not exist, in this case nothing will happen with this call
					stopTimer(ordCancelRejTimer);
				} catch (SchedulerException e) {
					LOGGER.warn("Error while stopping timer {}", ordCancelRejTimer, e);
				}
				operation.setStateResilient(new TW_ExecutedState(), ErrorState.class);
				break;
			default:
				LOGGER.error("Order {}, received unexpected exec report state {}", operation.getOrder().getFixOrderId(), execRepState);
				break;
			}
		}
    }
    
    @Override
    public void onTimerExpired(String jobName, String groupName) {
    	String handlerJobName = super.getDefaultTimerJobName();
    	
        if (jobName.equals(handlerJobName)) {
            LOGGER.debug("Order {} : Timer: {} expired.", operation.getOrder().getFixOrderId(), jobName);
			try {
				//create the timer for the order cancel
				handlerJobName += REVOKE_TIMER_SUFFIX;
				setupTimer(handlerJobName, orderCancelDelay, false);
				// send order cancel message to the market
				twConnection.revokeOrder(operation, operation.getLastAttempt().getMarketOrder(), Messages.getString("TW_RevokeOrderForTimeout"));
			} catch (BestXException e) {
				LOGGER.error("An error occurred while revoking the order {}", operation.getOrder().getFixOrderId(), e);
				operation.setStateResilient(new WarningState(operation.getState(), e,
						Messages.getString("TW_MarketRevokeOrderError",  operation.getOrder().getFixOrderId())),
						ErrorState.class);	
			}            
        } else if (jobName.equals(handlerJobName + REVOKE_TIMER_SUFFIX)) {
        	//The timer created after receiving an Order Cancel Reject is expired without receiving an execution or a cancellation
        	LOGGER.debug("Order {} : Timer: {} expired.", operation.getOrder().getFixOrderId(), jobName);
        	//[AMC 20170117 BXSUP-2015] Added a more specific messge to audit
            operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("CancelRejectWithNoExecutionReportTimeout.0", operation.getLastAttempt().getMarketOrder().getMarket().getName())), ErrorState.class);
            } else {
            super.onTimerExpired(jobName, groupName);
        }
    }
    
    
	@Override
	public void onMarketOrderCancelRequestReject(MarketBuySideConnection source, Order order, String reason) {
		String handlerJobName = super.getDefaultTimerJobName() + REVOKE_TIMER_SUFFIX;
		try {
			stopTimer(handlerJobName);
			LOGGER.info("Order {} cancel rejected, waiting for the order execution or cancellation", order.getFixOrderId());
			//recreate the timer with a longer timeout (the same used for the execution)
			setupTimer(handlerJobName, waitingExecutionDelay, false);
		} catch (SchedulerException e) {
			LOGGER.error("Cannot stop timer {}", handlerJobName, e);
		}
	}
	
	@Override
   public void onRevoke() {
	   
	   //onRevoke is invoked by webapp and must only reject this market so no need to set customerRevokeReceived so that operations may continue on other markets
	   
	   String handlerJobName = super.getDefaultTimerJobName() + REVOKE_TIMER_SUFFIX;
	   
	   try {
	      //create the timer for the order cancel
	      setupTimer(handlerJobName, orderCancelDelay, false);
	      // send order cancel message to the market
	      twConnection.revokeOrder(operation, operation.getLastAttempt().getMarketOrder(), Messages.getString("TW_RevokeOrder"));
	   } catch (BestXException e) {
	      LOGGER.error("An error occurred while revoking the order {}", operation.getOrder().getFixOrderId(), e);
	      operation.setStateResilient(
	            new WarningState(
	                  operation.getState(), 
	                  e, 
	                  Messages.getString("TW_MarketRevokeOrderError", operation.getOrder().getFixOrderId())
	            ),
	            ErrorState.class
	      );   
	   }            
       
	}

   @Override
   public void onFixRevoke(CustomerConnection source) {
      
      //onFixRevoke may be invoked by customer and must cancel the order from BestX! (this should happen setting customerRevokeReceived to true)
      
      MarketOrder marketOrder = operation.getLastAttempt().getMarketOrder();

      String reason = Messages.getString("EventRevocationRequest.0");
      updateOperationToRevocated(reason);
      try {
         twConnection.revokeOrder(operation, marketOrder, reason);
      } catch (BestXException e) {
         LOGGER.error("An error occurred while revoking the order {}", operation.getOrder().getFixOrderId(), e);
         operation.setStateResilient(new WarningState(operation.getState(), e,
               Messages.getString("TW_MarketRevokeOrderError",  operation.getOrder().getFixOrderId())),
               ErrorState.class);   
      }
   }
	
	
	
}