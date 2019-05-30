/*
 * Copyright 2019-2028 SoftSolutions! srl 
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
package it.softsolutions.bestx.handlers.bondvision;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.markets.bondvision.exceptions.BVBestXFlyingException;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutablePrice;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketExecutionReport.RejectReason;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.bondvision.BV_ReceiveQuoteState;
import it.softsolutions.bestx.states.bondvision.BV_RejectedState;

/**
 * @author Stefano
 *
 */
public class BV_SendRfqEventHandler extends BV_ManagingRfqEventHandler {
	private final Log logger = LogFactory.getLog(BV_SendRfqEventHandler.class);
	private final String WAIT_EXECUTION_LABEL = operation.getOrder().getFixOrderId() + "WAIT_BV_RFQ_EXECUTION";

	private final MarketBuySideConnection bvMarket;
	private final long waitingExecutionDelay;
	private final int ordersPerMinute;
	private final Random generator = new Random();

	private long sleepTime;

	/**
	 * @param operation
	 */
	public BV_SendRfqEventHandler(Operation operation, 
			MarketBuySideConnection bvMarket,
			long waitingExecutionDelay,
			int ordersPerMinute)
	{
		super(operation);
		this.bvMarket = bvMarket;
		this.waitingExecutionDelay = waitingExecutionDelay;
		this.ordersPerMinute = ordersPerMinute;
		this.sleepTime = (60/ordersPerMinute)*1000;
	}


	@Override
	public void onNewState(OperationState currentState) {
		try {
			setupDefaultTimer(waitingExecutionDelay, false);
			bvMarket.sendRfq(operation, operation.getLastAttempt().getMarketOrder());
		} catch (BVBestXFlyingException e) {
			//Reset Timer
			stopDefaultTimer();

			//20120208 MSA - Insert of a random sleep before to send rfq again on the same isin.
			try {

				long mills = sleepTime*generator.nextInt(ordersPerMinute);
				if (logger.isDebugEnabled()){
					StringBuilder message = new StringBuilder(512);
					message.append("Waiting mills ").append(mills).append(" before sending in reject state, CAUSE: ").append(e.getMessage());
					logger.debug(message.toString());
				}

				Thread.sleep(mills);
			} catch(InterruptedException ie) {
				//Ignore Exception
			}

			MarketExecutionReport marketExecutionReport = new MarketExecutionReport();
			marketExecutionReport.setState(ExecutionReportState.REJECTED);
			marketExecutionReport.setReason(RejectReason.SYNCHRONIZATION_PROBLEM);
			Attempt currentAttempt = operation.getLastAttempt();
			List<MarketExecutionReport> marketExecutionReports = currentAttempt.getMarketExecutionReports();
			if (marketExecutionReports == null) {
				marketExecutionReports = new ArrayList<MarketExecutionReport>();
				currentAttempt.setMarketExecutionReports(marketExecutionReports);
				currentAttempt.setByPassableForVenueAlreadyTried(false);
			}
			marketExecutionReports.add(marketExecutionReport);
			operation.setStateResilient(new BV_RejectedState(Messages.getString("BVMarketSendRFQRejectError.1")), ErrorState.class);

		} catch (BestXException e) {
			logger.error("An error occurred while sending RFQ to BV", e);
			operation.setStateResilient(new WarningState(currentState, e, Messages.getString("BVMarketSendRFQRejectError.0")), ErrorState.class);
		}
	}


	@Override
	public void onMarketProposal(MarketBuySideConnection source, Instrument instrument, ClassifiedProposal quote) {
		stopDefaultTimer();
		operation.getLastAttempt().addExecutablePrice(new ExecutablePrice(quote), Attempt.BEST);
		operation.setStateResilient(new BV_ReceiveQuoteState(), ErrorState.class);
	}


	@Override
	public void onTimerExpired(String jobName, String groupName) {
		if(this.customerSpecificHandler != null) 
			this.customerSpecificHandler.onTimerExpired(jobName, groupName);
		if(jobName.equalsIgnoreCase(getDefaultTimerJobName())) {

			for (Attempt attempt : operation.getAttempts()) {
				ClassifiedProposal counter = attempt.getExecutablePrice(Attempt.BEST) == null ? null : attempt.getExecutablePrice(Attempt.BEST).getClassifiedProposal();
				if (counter != null && 
						counter.getVenue().getCode().equalsIgnoreCase(operation.getLastAttempt().getExecutionProposal().getVenue().getCode())) {
					counter.setProposalState(Proposal.ProposalState.REJECTED);
					counter.setReason(Messages.getString("DiscardTriedInEarlierAttemptProposalClassifier.0"));
				}
			}
			operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("EventStateTimeout.0")), ErrorState.class);
		}
	}
}



