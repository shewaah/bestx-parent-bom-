package it.softsolutions.bestx.handlers.marketaxess;
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





import java.math.BigDecimal;
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
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.markets.marketaxess.MarketAxessExecutionReport;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ExecutablePrice;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal.PriceType;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.model.Venue.VenueType;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.marketaxess.MA_CancelledState;
import it.softsolutions.bestx.states.marketaxess.MA_ExecutedState;
import it.softsolutions.bestx.states.marketaxess.MA_RejectedState;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.CompetitiveStatus;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.DealerQuotePriceType;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.component.Dealers.NoDealers;
import quickfix.FieldNotFound;

/**
 * 
 * Purpose: order sending event handler. Manages all the possible cases resulting from an order sent to the market.
 * 
 * Project Name : bestx-tradeweb-market 
 * First created by: ruggero.rizzo 
 * Creation date: 27/gen/2015
 * 
 **/
public class MA_SendOrderEventHandler extends BaseOperationEventHandler {

	private static final long serialVersionUID = -1021253573602664973L;

	private static final Logger LOGGER = LoggerFactory.getLogger(MA_SendOrderEventHandler.class);
	
	public static final String REVOKE_TIMER_SUFFIX="#REVOKE";
	private SerialNumberService serialNumberService;
	protected long waitingExecutionDelay;
	protected MarketBuySideConnection connection;
	protected long orderCancelDelay;
	@SuppressWarnings("unused")
	private BestXConfigurationDao bestXConfigurationDao;

	private MarketMakerFinder marketMakerFinder;
	private Market market;
	private VenueFinder venueFinder;

	public MA_SendOrderEventHandler(Operation operation, MarketBuySideConnection connection, SerialNumberService serialNumberService, long waitingExecutionDelay, long orderCancelDelay, BestXConfigurationDao bestXConfigurationDao, MarketMakerFinder marketMakerFinder, Market market, VenueFinder venueFinder) {
		super(operation);
		this.serialNumberService = serialNumberService;
		this.waitingExecutionDelay = waitingExecutionDelay;
		this.connection = connection;
		this.orderCancelDelay = orderCancelDelay;
		this.bestXConfigurationDao = bestXConfigurationDao;
		this.marketMakerFinder = marketMakerFinder;
		this.market = market;
		this.venueFinder = venueFinder;
	}

	@Override
	public void onNewState(OperationState currentState) {
		try {
			setupDefaultTimer(waitingExecutionDelay, false);
			connection.sendFokOrder(operation, operation.getLastAttempt().getMarketOrder());
		} catch (BestXException e) {
			LOGGER.error("An error occurred while sending FOK Order to MarketAxess", e);
			operation.setStateResilient(new WarningState(currentState, e, Messages.getString("MARKETAXESS_MarketSendOrderError.0")), ErrorState.class);
			stopDefaultTimer();
		}
	}

	@Override
	public void onMarketOrderReject(MarketBuySideConnection source, Order order, String reason, String sessionId) {

		if (source.getMarketCode() != MarketCode.MARKETAXESS) {
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
					operation.setStateResilient(new MA_RejectedState(Messages.getString("MARKETAXESS_RejectPrefix", reason)), ErrorState.class);
				} else {
					operation.setStateResilient(new MA_RejectedState(""), ErrorState.class);
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
			operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("MARKETAXESS_MarketAttemptNotFoundError.0")), ErrorState.class);
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
			operation.setStateResilient(new WarningState(operation.getState(), e1, Messages.getString("MARKETAXESS_MarketExecutionReportError.0")), ErrorState.class);
			return;
		}

		long executionReportId = serialNumberService.getSerialNumber("EXEC_REP");

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
				// MA market execution report may have the MarketMaker code inside
				if(marketExecutionReport.getMarketMaker() != null){
					executionReport.setExecBroker(marketExecutionReport.getMarketMaker().getCode());
					executionReport.setCounterPart(marketExecutionReport.getMarketMaker().getCode());
				} else {
					String execBroker =  marketExecutionReport.getExecBroker();
					executionReport.setExecBroker(execBroker);
					executionReport.setCounterPart(execBroker);
				}
				executionReport.setMarketOrderID(marketExecutionReport.getMarketOrderID());
				// set quotes
				addQuotesToAttempt(currentAttempt, marketExecutionReport);

				operation.setStateResilient(new MA_CancelledState(), ErrorState.class);
				break;
			case REJECTED:
				stopDefaultTimer();             
				if(marketExecutionReport.getMarketMaker() != null){
					executionReport.setExecBroker(marketExecutionReport.getMarketMaker().getCode());
					executionReport.setCounterPart(marketExecutionReport.getMarketMaker().getCode());
				} else {
					String execBroker =  marketExecutionReport.getExecBroker();
					executionReport.setExecBroker(execBroker);
					executionReport.setCounterPart(execBroker);
				}
				executionReport.setMarketOrderID(marketExecutionReport.getMarketOrderID());
				// set quotes
				addQuotesToAttempt(currentAttempt, marketExecutionReport);

				operation.setStateResilient(new MA_RejectedState(marketExecutionReport.getText()), ErrorState.class);
				break;
			case FILLED:
				stopDefaultTimer();
				try {
					//this timer could not exist, in this case nothing will happen with this call
					stopTimer(ordCancelRejTimer);
				} catch (SchedulerException e) {
					LOGGER.warn("Error while stopping timer {}", ordCancelRejTimer, e);
				}
				executionReport.setLastPx(executionReport.getPrice().getAmount());
				marketExecutionReport.setLastPx(executionReport.getPrice().getAmount());
				executionReport.setSequenceId(Long.toString(executionReportId));

				// MA market execution report has the MarketMaker code inside
				if(marketExecutionReport.getMarketMaker() != null){
					executionReport.setExecBroker(marketExecutionReport.getMarketMaker().getCode());
					executionReport.setCounterPart(marketExecutionReport.getMarketMaker().getCode());
				} else {
					LOGGER.info("Unknown MarketMaker code for ExecutionReport {}", marketExecutionReport);
					String execBroker =  marketExecutionReport.getExecBroker();
					executionReport.setExecBroker(execBroker);
					executionReport.setCounterPart(execBroker);
				}
				executionReport.setMarketOrderID(marketExecutionReport.getMarketOrderID());
				operation.getExecutionReports().add(executionReport);

				// set quotes
				addQuotesToAttempt(currentAttempt, marketExecutionReport);
				operation.setStateResilient(new MA_ExecutedState(), ErrorState.class);
					break;
				default:
					LOGGER.error("Order {}, received unexpected exec report state {}", operation.getOrder().getFixOrderId(), execRepState);
					break;
				}
			}

		}

	/**
	 * @param marketExecutionReport
	 * @param currentAttempt
	 * @param maExecutionReport
	 * @throws NumberFormatException
	 */
	private void addQuotesToAttempt(Attempt currentAttempt,
			MarketExecutionReport marketExecutionReport) throws NumberFormatException {
		MarketAxessExecutionReport maExecutionReport = (MarketAxessExecutionReport) marketExecutionReport;
		for (NoDealers dealer : maExecutionReport.getDealers()) {
			ExecutablePrice quote = new ExecutablePrice();
			quote.setMarket(maExecutionReport.getMarket());
			MarketMarketMaker mmm = null;
			String quotingDealer = "Unknown";
			quote.setType(ProposalType.COUNTER);
			try {
				quotingDealer = dealer.getDealerID().getValue();
				mmm = marketMakerFinder.getMarketMarketMakerByCode(maExecutionReport.getMarket().getMarketCode(), quotingDealer);
				if(mmm == null) {
					LOGGER.info("IMPORTANT! MarketAxess returned dealer {} not configured in BestX!. Please configure it", quotingDealer);
					quote.setOriginatorID(quotingDealer);
				} else {
					quote.setMarketMarketMaker(mmm);
				}
				quote.setAuditQuoteState(dealer.getCompetitiveStatus().getValue());
				boolean foundStatus = convertState(quote, dealer.getCompetitiveStatus());
				if (! foundStatus) {
					LOGGER.info("Competitive Status for MarketAxess not found: {}. Setting proposal status to dropped", dealer.getCompetitiveStatus());
				}
				try {
					quote.setReason(dealer.getDealerQuoteText().getValue());
				} catch (@SuppressWarnings("unused") Exception e) {
				; // no text from dealer
				}
			} catch (FieldNotFound e) {
				LOGGER.info("Field not found", e);
				quote = null;
				continue;
			} catch(@SuppressWarnings("unused") BestXException e1) {
				LOGGER.info("Can't Be!");
				quote.setOriginatorID(quotingDealer);
			}
				try {  //BESTX-314 get dealer quote from alternative tag ReferencePrice (5691) if original was not in percentage of par
					if(PriceType.PRICE == convertPriceType(dealer.getDealerQuotePriceType())) {
						try {
							quote.setPrice(new Money(maExecutionReport.getInstrument().getCurrency(), new BigDecimal(Double.toString(dealer.getDealerQuotePrice().getValue()))));
							quote.setPriceType(convertPriceType(dealer.getDealerQuotePriceType()));
							quote.setQty(new BigDecimal(Double.toString(dealer.getDealerQuoteOrdQty().getValue())));
						} catch (@SuppressWarnings("unused") FieldNotFound e) {
							quote.setPrice(new Money(currentAttempt.getMarketOrder().getCurrency(), new BigDecimal("0")));
							quote.setQty(currentAttempt.getMarketOrder().getQty());
						}
					} else { //let's hope tag 7761 equals 1 
						try {
							quote.setPrice(new Money(maExecutionReport.getInstrument().getCurrency(), new BigDecimal(Double.toString(dealer.getReferencePrice().getValue()))));
							quote.setPriceType(convertPriceType(dealer.getDealerQuotePriceType()));
							quote.setQty(new BigDecimal(Double.toString(dealer.getDealerQuoteOrdQty().getValue())));
						} catch (@SuppressWarnings("unused") FieldNotFound e) {
							quote.setPrice(new Money(currentAttempt.getMarketOrder().getCurrency(), new BigDecimal("0")));
							quote.setQty(currentAttempt.getMarketOrder().getQty());
						}
					}
					quote.setSide(maExecutionReport.getSide() == OrderSide.BUY ? ProposalSide.ASK : ProposalSide.BID);
					quote.setTimestamp(DateService.convertUTCToLocal(currentAttempt.getMarketOrder().getTransactTime())); // there is no timestamp in MA returned values - MarketOrder TransactTime is in UTC
					quote.setQuoteReqId(currentAttempt.getMarketOrder().getFixOrderId());
					int rank = Integer.parseInt(dealer.getQuoteRank().getValue());
					currentAttempt.addExecutablePrice(quote, rank);
//					if(mmm != null && mmm.getMarketMaker() != null)
						quote.setVenue(venueFinder.getMarketMakerVenue(mmm.getMarketMaker()));
				} catch (@SuppressWarnings("unused") FieldNotFound e) {
					LOGGER.info("Quote not valid for dealer {}", quotingDealer);
				} catch (@SuppressWarnings("unused") NullPointerException|BestXException e) {
					// no venue available for this MM
					if(mmm != null) {
						LOGGER.info("Unable to find venue for Market code of Market Maker {}", mmm.getMarketSpecificCode());
						Venue dummyVenue = new Venue();
						dummyVenue.setCode(mmm.getMarketMaker().getCode());
						dummyVenue.setMarket(market);
						dummyVenue.setMarketMaker(mmm.getMarketMaker());
						dummyVenue.setVenueType(VenueType.MARKET_MAKER);
						quote.setVenue(dummyVenue);
					} else {
						LOGGER.info("Unable to find Market Maker for MarketAxess dealer code {}", quote.getOriginatorID());
					}
				}
			}
		}
//	}

	// FIXME substitute with fix values
		private PriceType convertPriceType(DealerQuotePriceType dealerQuotePriceType) {
			if (dealerQuotePriceType == null)
				return null;
			switch(dealerQuotePriceType.getValue()) {
			case "1":
				return PriceType.PRICE;
			case "6":
				return PriceType.SPREAD;
			case "9":
				return PriceType.YIELD;
			default:
				return null;
			}
		}

		/**
		 * 
		 * @param proposal
		 * @param competitiveStatus
		 * @return true iff the competitiveStatus is one of the recognised strings
		 */
		private boolean convertState(ExecutablePrice proposal, CompetitiveStatus competitiveStatus) {
			if(competitiveStatus.getValue().startsWith("Done-"))
				proposal.setAuditQuoteState("Done");					
			else
				proposal.setAuditQuoteState(competitiveStatus.getValue());
			switch (proposal.getAuditQuoteState()) {
			case "Done":
			case "Covered":
			case "Tied for Best":
			case "Missed":
			case "EXP-Price":
				// do not change audit status, since it is acceptable value
				proposal.setProposalState(ProposalState.NEW);
				proposal.setProposalSubState(ProposalSubState.NONE);
				return true;
			case "Order Accepted":	// suspecting this is for dealer only
			case "Resp Req": 		//suspecting this is for dealer only
				proposal.setAuditQuoteState("Done");
				proposal.setProposalState(ProposalState.NEW);
				proposal.setProposalSubState(ProposalSubState.NONE);
				return true;
			case "Tied for Cover":
				proposal.setAuditQuoteState("Tied-For-Cover");
				proposal.setProposalState(ProposalState.NEW);
				proposal.setProposalSubState(ProposalSubState.NONE);
				return true;
			case "EXP-DNQ":
			case "DNT":
				proposal.setAuditQuoteState("EXP-DNQ");
				proposal.setProposalState(ProposalState.REJECTED);
				proposal.setProposalSubState(ProposalSubState.NOT_TRADING);
				return true;
			case "Client CXL":
			case "Timed Out":
			case "Timed Out (R)":
				proposal.setAuditQuoteState("Timed Out");
				proposal.setProposalState(ProposalState.REJECTED);
				proposal.setProposalSubState(ProposalSubState.NOT_TRADING);
				return true;
			case "Passed":
				// do not change audit status, since it is acceptable value
				proposal.setProposalState(ProposalState.REJECTED);
				proposal.setProposalSubState(ProposalSubState.REJECTED_BY_DEALER);
				return true;
			case "Withdrawn":
			case "Cancelled":
				proposal.setAuditQuoteState("Cancelled");
				proposal.setProposalState(ProposalState.REJECTED);
				proposal.setProposalSubState(ProposalSubState.REJECTED_BY_DEALER);
				return true;
			case "Cxl-Amended":
			case "You CXL":
			default:
				proposal.setAuditQuoteState("Cancelled");
				proposal.setProposalState(ProposalState.DROPPED);
				proposal.setProposalSubState(ProposalSubState.NOT_TRADING);
				return false;
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
					connection.revokeOrder(operation, operation.getLastAttempt().getMarketOrder(), Messages.getString("MARKETAXESS_RevokeOrderForTimeout"));
				} catch (BestXException e) {
					LOGGER.error("Error {} while revoking the order {}", e.getMessage(), operation.getOrder().getFixOrderId(), e);
					operation.setStateResilient(new WarningState(operation.getState(), e,
							Messages.getString("MARKETAXESS_MarketRevokeOrderError",  operation.getOrder().getFixOrderId())),
							ErrorState.class);	
				}            
			} else if (jobName.equals(handlerJobName + REVOKE_TIMER_SUFFIX)) {
				//The timer created after receiving an Order Cancel Reject is expired without receiving an execution or a cancellation
				LOGGER.debug("Order {} : Timer: {} expired.", operation.getOrder().getFixOrderId(), jobName);
				operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("CancelRejectWithNoExecutionReportTimeout.0", operation.getLastAttempt().getMarketOrder().getMarket().getName())), ErrorState.class);
			} else {
				super.onTimerExpired(jobName, groupName);
			}
		}

		@Override
		public void onFixRevoke(CustomerConnection source) {
			MarketOrder marketOrder = operation.getLastAttempt().getMarketOrder();

			String reason = Messages.getString("EventRevocationRequest.0");
			updateOperationToRevocated(reason);
			try {
				connection.revokeOrder(operation, marketOrder, reason);
			} catch (BestXException e) {
				LOGGER.error("An error occurred while revoking the order {}", operation.getOrder().getFixOrderId(), e);
				operation.setStateResilient(new WarningState(operation.getState(), e,
						Messages.getString("MARKETAXESS_MarketRevokeOrderError",  operation.getOrder().getFixOrderId())),
						ErrorState.class);	
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
		
//		@Override
	   public void onRevoke() {
	      
	      String handlerJobName = super.getDefaultTimerJobName() + REVOKE_TIMER_SUFFIX;
	      
	      try {
	         //create the timer for the order cancel
	         setupTimer(handlerJobName, orderCancelDelay, false);

	         // send order cancel message to the market
	         connection.revokeOrder(operation, operation.getLastAttempt().getMarketOrder(), Messages.getString("TW_RevokeOrder"));
	      } catch (BestXException e) {
	         LOGGER.error("An error occurred while revoking the order {}", operation.getOrder().getFixOrderId(), e);
	         operation.setStateResilient(
	               new WarningState(
	                     operation.getState(), 
	                     e, 
	                     Messages.getString("MA_MarketRevokeOrderError", operation.getOrder().getFixOrderId())
	               ),
	               ErrorState.class
	         );   
	      }            
	       
	   }
		
	}