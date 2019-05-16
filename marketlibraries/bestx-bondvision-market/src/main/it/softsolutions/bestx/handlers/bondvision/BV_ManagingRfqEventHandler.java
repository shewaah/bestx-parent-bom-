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
 * Author       : $Author: ruggero.rizzo $
 * Date         : $Date: 2010-12-24 08:45:56 $
 * Header       : $Id: BV_ManagingRfqEventHandler.java,v 1.3 2010-12-24 08:45:56 ruggero.rizzo Exp $
 * Revision     : $Revision: 1.3 $
 * Source       : $Source: /root/scripts/BestXEngine_Akros/src/it/softsolutions/bestx/handlers/bondvision/BV_ManagingRfqEventHandler.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_Akros
 */
package it.softsolutions.bestx.handlers.bondvision;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.connections.mts.MTSConnector;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutablePrice;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketExecutionReport.RejectReason;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.bondvision.BV_RejectedState;

public class BV_ManagingRfqEventHandler extends BaseOperationEventHandler
{
	protected final String WAIT_EXECUTION_LABEL = operation.getOrder().getFixOrderId() + "WAIT_BV_RFQ_EXECUTION";
	private final Log logger = LogFactory.getLog(BV_ManagingRfqEventHandler.class);

	BV_ManagingRfqEventHandler(Operation operation) {
		super(operation);
	}
	

	@Override
	public void onMarketProposal(MarketBuySideConnection source, Instrument instrument, ClassifiedProposal proposal) {
		// add proposal to ExecutablePrices as the last ranked one
		operation.getLastAttempt().addExecutablePrice(new ExecutablePrice(proposal), operation.getLastAttempt().getExecutablePrices().size());
	}

	@Override
	public void onMarketOrderReject(MarketBuySideConnection source, Order order, String reason, String sessionId) {
		if (source.getMarketCode() != MarketCode.BV) return;

		stopDefaultTimer();

		if (!checkCustomerRevoke(order))
		{
			for (Attempt attempt : operation.getAttempts()) {
				ClassifiedProposal counterOffer = attempt.getExecutablePrice(Attempt.BEST) == null ? null : attempt.getExecutablePrice(Attempt.BEST).getClassifiedProposal();
				if (counterOffer != null && 
						counterOffer.getVenue().getCode().equalsIgnoreCase(operation.getLastAttempt().getExecutionProposal().getVenue().getCode())) {
					ClassifiedProposal counter = (ClassifiedProposal)attempt.getExecutablePrice(Attempt.BEST).getClassifiedProposal();
					counter.setProposalState(Proposal.ProposalState.REJECTED);
					counter.setReason(Messages.getString("DiscardTriedInEarlierAttemptProposalClassifier.0"));
				}
			}
			operation.setStateResilient(new BV_RejectedState(""), ErrorState.class);
		}

	}

	@Override
	public void onTimerExpired(String jobName, String groupName) {
		if(this.customerSpecificHandler != null) 
			this.customerSpecificHandler.onTimerExpired(jobName, groupName);
		Attempt attempt = operation.getLastAttempt();
		if(jobName.equalsIgnoreCase(getDefaultTimerJobName())) {

			ClassifiedProposal counter = attempt.getExecutablePrice(Attempt.BEST) == null ? null : attempt.getExecutablePrice(Attempt.BEST).getClassifiedProposal();
			counter.setProposalState(Proposal.ProposalState.REJECTED);
			counter.setReason(Messages.getString("DiscardTriedInEarlierAttemptProposalClassifier.0"));
			operation.setStateResilient(new BV_RejectedState(Messages.getString("BV_RFQ_TIMEOUT_WITHOUT_ANSWER")), ErrorState.class);
		}
	}

	@Override
	public void onMarketOrderTechnicalReject(MarketBuySideConnection source, Order order, String reason) {

		stopDefaultTimer();

		if (!checkCustomerRevoke(order))
		{
			MarketExecutionReport marketExecutionReport = new MarketExecutionReport();
			marketExecutionReport.setState(ExecutionReportState.REJECTED);
			marketExecutionReport.setReason(RejectReason.TECHNICAL_FAILURE);
			Attempt currentAttempt = operation.getLastAttempt();
			/* 20110811 - Ruggero
			 * Ticket 7596/AKR-1149 : a venue used in this attempt, ended with a technical reject, must be
			 * available for successive attempts.
			 * Set the current attempt as bypassable when checking proposals for venue already used in
			 * previous attempts.
			 * 
			 * 20110907 - Ruggero
			 * we act in a more specific way: 
			 * if we are in the tech reject specified in the ticket, "Transaction failed Maximum number of proposal exceeded",
			 * we set this attempt as bypassable. Only in this situation. To be sure we check the message,
			 * in fact, upon reception, we wrote at its start a distinctive phrase. It is saved in the
			 * static variable BondVisionConnector.BV_TECH_REJ_DISTINCTIVE_MSG.
			 */
			if (reason.startsWith(MTSConnector.BV_TECH_REJ_DISTINCTIVE_MSG))
			{
				if (logger.isDebugEnabled())
					logger.debug("Order " + order.getFixOrderId() + ", BV technical reject with the message [" + reason +"]. In this situation we make the attempt bypassable in order to allow the sending of an RFQ to the same market maker.");
				currentAttempt.setByPassableForVenueAlreadyTried(true);
			}

			List<MarketExecutionReport> marketExecutionReports = currentAttempt.getMarketExecutionReports();
			if (marketExecutionReports == null) {
				marketExecutionReports = new ArrayList<MarketExecutionReport>();
				currentAttempt.setMarketExecutionReports(marketExecutionReports);
			}
			marketExecutionReports.add(marketExecutionReport);
			operation.setStateResilient(new BV_RejectedState(reason), ErrorState.class);
		}
	}	
}
