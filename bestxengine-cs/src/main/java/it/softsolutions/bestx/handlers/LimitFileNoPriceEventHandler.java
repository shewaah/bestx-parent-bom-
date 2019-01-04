/*
 * Copyright 1997-2013 SoftSolutions! srl 
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.dao.OperationStateAuditDao;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.CSOrdersEndOfDayService;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.ManualExecutionWaitingPriceState;
import it.softsolutions.bestx.states.SendExecutionReportState;
import it.softsolutions.bestx.states.SendNotExecutionReportState;
import it.softsolutions.bestx.states.WaitingPriceState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.autocurando.AutoCurandoStatus;
import it.softsolutions.jsscommon.Money;

/**
 *
 * Purpose: handler for events happening while the operation is in the
 * LimitFileNoPriceState state
 *
 * Project Name : bestxengine-cs First created by: ruggero.rizzo Creation date:
 * 29/ott/2013
 * 
 **/
public class LimitFileNoPriceEventHandler extends CSBaseOperationEventHandler {
	private static final long serialVersionUID = 2003164485437487954L;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(LimitFileNoPriceEventHandler.class);

	private final SerialNumberService serialNumberService;
	private final AutoCurandoStatus autoCurandoStatus;
	private List<Long> pDTimes;
	private final OperationStateAuditDao operationStateAuditDao;
	
	private final MarketFinder marketFinder;

	/**
	 * Constructor
	 * 
	 * @param operation
	 *            the operation
	 * @param serialNumberService
	 *            serial number service
	 * @param autoCurandoStatus
	 *            automatic curando status (basically On or Off)
	 * @param limitFileNPPriceDiscoveryTimes
	 *            times for starting price discovery
	 */
	public LimitFileNoPriceEventHandler(Operation operation,
			SerialNumberService serialNumberService,
			AutoCurandoStatus autoCurandoStatus, final List<Long> limitFileNPPriceDiscoveryTimes,
			MarketFinder marketFinder, OperationStateAuditDao operationStateAuditDao) {
		super(operation);
		this.serialNumberService = serialNumberService;
		this.pDTimes = limitFileNPPriceDiscoveryTimes == null ? new ArrayList<Long>() : limitFileNPPriceDiscoveryTimes;
		this.autoCurandoStatus = autoCurandoStatus;
		this.marketFinder = marketFinder;
		this.operationStateAuditDao = operationStateAuditDao;
	}

	@Override
	public void onNewState(OperationState currentState) {
		Order order = operation.getOrder();
		Customer customer = order.getCustomer();
		// next datetime --> first time after this exact moment
		long nextTimer = getTimerInterval();
		if (nextTimer > 0) {
		    Date now = new Date();
		    operationStateAuditDao.updateOrderNextExecutionTime(operation.getOrder(), new Date(now.getTime() + nextTimer));
			setupDefaultTimer(nextTimer, false);
			LOGGER.debug("set next timer in {} seconds", nextTimer / 1000);
		} else {
		   operationStateAuditDao.updateOrderNextExecutionTime(operation.getOrder(), null);
		}
	}

	@Override
	public void onTimerExpired(String jobName, String groupName) {
		String handlerJobName = super.getDefaultTimerJobName();

		if (jobName.equalsIgnoreCase(handlerJobName)) {
			LOGGER.debug("Timer: " + jobName + " expired. Clean-up data.");
			if (AutoCurandoStatus.SUSPENDED.equalsIgnoreCase(autoCurandoStatus
					.getAutoCurandoStatus())) {
				// nothing to do, recreate the timer
				LOGGER.debug(
						"Order {}, LimitFile price discovery suspended, re-start the timer",
						operation.getOrder().getFixOrderId());
				setupDefaultTimer(
						getTimerInterval(), false);
			} else {
				// time to do the price discovery
				operation.setStateResilient(new WaitingPriceState(),
						ErrorState.class);
			}
		} else {
			super.onTimerExpired(jobName, groupName);
		}
	}

	protected long getTimerInterval() {
	   long now = DateService.currentTimeMillis();
		for (long tim : this.pDTimes) {
			if (tim - now > 1000) { // if timer is less than a second forward do not set it
				return tim - now;
			}
			// no time after now found, return 0 in order to avoid any timer
			// setup
		}
		return 0;
	}

	@Override
	public void onOperatorSendDDECommand(OperatorConsoleConnection source,
			String comment) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onOperatorManualManage(OperatorConsoleConnection source,
			String comment) {
		stopDefaultTimer();
		operation.setStateResilient(new ManualExecutionWaitingPriceState(
				comment), ErrorState.class);
	}

	@Override
	public void onOperatorRetryState(OperatorConsoleConnection source,
			String comment) {
		// reset contatore sempre 15/12/2008 AMC
		operation.setFirstAttemptInCurrentCycle(operation.getAttemptNo());
		/*
		 * 27-03-2009 RR As requested we've to avoid checking the old proposals
		 * when applying the DiscardTriedInEarlierAttempt proposals classifier.
		 * When the user forces a retry we mark the attempts as bypassable and
		 * later, in the classifier, we check the related attempt' flag.
		 */
		LOGGER.debug("Forcing retry on Automatic Curando");
		operation.markAttemptsToByPass(true);
		stopDefaultTimer();
		// set the first valid attempt to start checking while classifying
		// proposals
		operation.setFirstValidAttemptNo(operation.getAttempts().size());
		// RIPROVA ORDINE
		operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
	}

	@Override
	public void onOperatorManualExecution(OperatorConsoleConnection source,
			String comment, Money price, BigDecimal qty, String mercato,
			String marketMaker, Money prezzoRif) {
		try {
			stopDefaultTimer();

			ExecutionReport newExecution = ExecutionReportHelper.createExecutionReport(
					operation.getOrder().getInstrument(),
						operation.getOrder().getSide(),
							this.serialNumberService);

			// Creation of the market execution reports for AMOS (sending of
			// fills)
			MarketExecutionReport mktExecRep = new MarketExecutionReport();
			mktExecRep.setInstrument(operation.getOrder().getInstrument());
			mktExecRep.setSide(operation.getOrder().getSide());
			mktExecRep.setTransactTime(DateService.newLocalDate());
			long mktExecutionReportId = serialNumberService
					.getSerialNumber("EXEC_REP");
			mktExecRep.setSequenceId(Long.toString(mktExecutionReportId));

			newExecution.setPrice(prezzoRif);
			newExecution.setAveragePrice(prezzoRif.getAmount());
			newExecution.setActualQty(qty);
			newExecution.setState(ExecutionReportState.FILLED);

			mktExecRep.setPrice(prezzoRif);
			mktExecRep.setAveragePrice(prezzoRif.getAmount());
			mktExecRep.setActualQty(qty);
			mktExecRep.setState(ExecutionReportState.FILLED);

			String subMarkets = "";
			for (int i = 0; i < Market.SubMarketCode.values().length; i++) {
				subMarkets += Market.SubMarketCode.values()[i] + "_";
			}
			Market tmpMkt = null;
			if (subMarkets.indexOf(marketMaker) >= 0) {
				tmpMkt = marketFinder.getMarketByCode(
						Market.MarketCode.valueOf(mercato),
						Market.SubMarketCode.valueOf(marketMaker));
				newExecution.setMarket(tmpMkt);
				mktExecRep.setMarket(tmpMkt);
			} else {
				tmpMkt = marketFinder.getMarketByCode(
						Market.MarketCode.valueOf(mercato), null);
				newExecution.setMarket(tmpMkt);
				mktExecRep.setMarket(tmpMkt);
			}

			newExecution.setExecBroker(marketMaker);
			// set counterpart for AMOS
			newExecution.setCounterPart(marketMaker);

			mktExecRep.setExecBroker(marketMaker);
			mktExecRep.setCounterPart(marketMaker);

			if (price == null
					|| price.getAmount().compareTo(prezzoRif.getAmount()) == 0) {
				newExecution.setLastPx(prezzoRif.getAmount());
				mktExecRep.setLastPx(prezzoRif.getAmount());
			} else {
				newExecution.setLastPx(price.getAmount());
				mktExecRep.setLastPx(price.getAmount());
			}

			if (operation.getLastAttempt().getMarketExecutionReports() == null) {
				operation.getLastAttempt().setMarketExecutionReports(
						new ArrayList<MarketExecutionReport>());
			}
			operation.getLastAttempt().getMarketExecutionReports()
					.add(mktExecRep);
			LOGGER.info("Manual execution, execution report {}",
					newExecution.toString());
			operation.getExecutionReports().add(newExecution);
			if (newExecution.getMarket() != null) {
				comment = getCommentWithPrefix(comment);
				operation.getOrder().setText(comment);
				operation.setStateResilient(new SendExecutionReportState(
						getCommentWithPrefix(comment)), ErrorState.class);
			} else {
				operation.setStateResilient(
						new WarningState(operation.getState(), null, Messages
								.getString("MANUALMarketError.0")),
						ErrorState.class);
			}
		} catch (BestXException e) {
			LOGGER.error(
					"Unable to create and store ExecutionReport from manual execution state",
					e);
			operation.setStateResilient(new WarningState(operation.getState(),
					e, Messages.getString("MANUALExecutionReportError.0")),
					ErrorState.class);
		}
	}

	@Override
	public void onOperatorForceNotExecution(OperatorConsoleConnection source,
			String comment) {
		stopDefaultTimer();
		ExecutionReport newExecution = ExecutionReportHelper
				.createExecutionReport(
						operation.getOrder().getInstrument(), operation
						.getOrder().getSide(),
						this.serialNumberService);
		newExecution.setState(ExecutionReportState.REJECTED);
		List<ExecutionReport> executionReports = operation
				.getExecutionReports();
		executionReports.add(newExecution);
		operation.setExecutionReports(executionReports);
		comment = getCommentWithPrefix(comment);
		operation.getOrder().setText(comment);
		operation.setStateResilient(new SendNotExecutionReportState(
				getCommentWithPrefix(comment)), ErrorState.class);
	}

	@Override
	public void onStateRestore(OperationState currentState) {
		// Restore state timer
		// should not be needed because of the Quartz automatic recovery of
		// pending jobs
		// setupTimer(CURANDO_RETRY_ORDER, this.getClass().getSimpleName(),
		// curandoRetrySec, true);
	}

	private String getCommentWithPrefix(String comment) {
		return LimitFileHelper.getInstance()
				.getCommentWithPrefix(comment, true);
	}

}
