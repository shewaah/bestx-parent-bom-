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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.bondvision.BV_ExecutedState;

/**
 * @author Stefano
 *
 */
public class BV_AcceptQuoteEventHandler extends BV_ManagingRfqEventHandler {
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

private final Log logger = LogFactory.getLog(BV_AcceptQuoteEventHandler.class);

	private final MarketBuySideConnection bvMarket;
	private final SerialNumberService serialNumberService;
	private final long waitingExecutionDelay;
	private final List<String> internalMMcodes;
	
	/**
	 * @param operation
	 */
	public BV_AcceptQuoteEventHandler(Operation operation, 
	      MarketBuySideConnection bvMarket, 
			SerialNumberService serialNumberService,
			long waitingExecutionDelay,
			List<String> internalMMcodes) {
		super(operation);
		this.bvMarket = bvMarket;
      this.serialNumberService = serialNumberService;
		this.waitingExecutionDelay = waitingExecutionDelay;
		this.internalMMcodes = internalMMcodes;
	}
	
	@Override
	public void onNewState(OperationState currentState) {
		try {
			setupDefaultTimer(waitingExecutionDelay, false);
			ClassifiedProposal counter = operation.getLastAttempt().getExecutablePrice(Attempt.BEST).getClassifiedProposal();
			bvMarket.acceptProposal(operation, operation.getLastAttempt().getSortedBook().getInstrument(), counter);
		} catch(BestXException exc) {
			operation.setStateResilient(new WarningState(currentState, exc, Messages.getString("BVMarketAcceptQuoteError.0")), ErrorState.class);
		}
	}
	
	@Override
	public void onMarketExecutionReport(MarketBuySideConnection source, Order order, MarketExecutionReport marketExecutionReport) {
      super.onMarketExecutionReport(source, order, marketExecutionReport);
		stopDefaultTimer();
		Attempt currentAttempt = operation.getLastAttempt();
        if (currentAttempt == null) {
            logger.error("No current Attempt found");
            operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("BVMarketAttemptNotFoundError.0")), ErrorState.class);
            return;
        }
        List<MarketExecutionReport> marketExecutionReports = currentAttempt.getMarketExecutionReports();
        if (marketExecutionReports == null) {
            marketExecutionReports = new ArrayList<MarketExecutionReport>();
            currentAttempt.setMarketExecutionReports(marketExecutionReports);
        }
        if (marketExecutionReports.size() > 0)  // AMC 20091212 Assuming only one valid Market Execution report is sent by BV for each execution.
        {
           logger.info("Duplicated execution report received from BV market. Discarding it.");
           return;
        }
        marketExecutionReports.add(marketExecutionReport);
        // Create Customer Execution Report from Market Execution Report
        ExecutionReport executionReport;
        try {
            executionReport = marketExecutionReport.clone();
        }
        catch (CloneNotSupportedException e1) {
            logger.error("Error while trying to create Execution Report from Market Execution Report");
            operation.setStateResilient(new WarningState(operation.getState(), e1, Messages.getString("BVMarketSendOrderError.0")), ErrorState.class);
            return;
        }
        long executionReportId = serialNumberService.getSerialNumber("EXEC_REP");
        
        executionReport.setLastPx(executionReport.getPrice().getAmount());
        marketExecutionReport.setLastPx(executionReport.getPrice().getAmount());
        executionReport.setSequenceId(Long.toString(executionReportId));
        marketExecutionReport.setExecBroker(operation.getLastAttempt().getExecutionProposal().getMarketMarketMaker().getMarketMaker().getCode());               
        executionReport.setExecBroker(marketExecutionReport.getExecBroker());
// AMC Uso dei codici interni MM per determinare se CP o CT        	
        if (internalMMcodes.contains(executionReport.getExecBroker())) {
           executionReport.setTipoConto(ExecutionReport.CONTO_PROPRIO);
           marketExecutionReport.setTipoConto(ExecutionReport.CONTO_PROPRIO);
        } else {
           executionReport.setTipoConto(ExecutionReport.CONTO_TERZI);
           marketExecutionReport.setTipoConto(ExecutionReport.CONTO_TERZI);
        }
/*
        executionReport.setTipoConto(ExecutionReport.CONTO_TERZI);
		  marketExecutionReport.setTipoConto(ExecutionReport.CONTO_TERZI);
*/
        executionReport.setCounterPart(marketExecutionReport.getExecBroker());
        executionReport.setMarketOrderID(marketExecutionReport.getMarketOrderId());
        logger.info("BV market - new exec report added : " + executionReport.toString());
        operation.getExecutionReports().add(executionReport);
        operation.setStateResilient(new BV_ExecutedState(), ErrorState.class);
	}

   @Override
   public void onStateRestore(OperationState currentState)
   {
      logger.info("BV_AcceptQuoteState restore operation");
      // DO NOTHING;
   }


}
