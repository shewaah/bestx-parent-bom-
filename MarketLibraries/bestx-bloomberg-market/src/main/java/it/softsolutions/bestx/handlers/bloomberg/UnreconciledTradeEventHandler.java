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
package it.softsolutions.bestx.handlers.bloomberg;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.handlers.ExecutionReportHelper;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.SendNotExecutionReportState;
import it.softsolutions.bestx.states.bloomberg.BBG_ExecutedState;
import it.softsolutions.jsscommon.Money;

/**  
*
* Purpose: handler for the unreconciled trades, executes the commands received from the
* dashboard.  
*
* Project Name : bestx-bloomberg-market 
* First created by: ruggero.rizzo 
* Creation date: 26/set/2012 
* 
**/
public class UnreconciledTradeEventHandler extends BaseOperationEventHandler
{
   private static final Logger LOGGER = LoggerFactory.getLogger(UnreconciledTradeEventHandler.class);
   private MarketMakerFinder marketMakerFinder;
   private final SerialNumberService serialNumberService;
   
   public UnreconciledTradeEventHandler(Operation operation, MarketMakerFinder marketMakerFinder, SerialNumberService serialNumberService)
   {
      super(operation);
      this.marketMakerFinder = marketMakerFinder;
      this.serialNumberService = serialNumberService;
   }
   
   
   @Override
   public void onOperatorUnreconciledTradeMatched(OperatorConsoleConnection source, BigDecimal executionPrice,
         String executionMarketMaker, String ticketNumber) throws BestXException
   {
      String fixOrderId = operation.getOrder().getFixOrderId();
      int executionReportIndex = operation.getExecutionReports().size() - 1;
      ExecutionReport currentExecutionReport = operation.getExecutionReports().get(executionReportIndex);
      int index = operation.getLastAttempt().getMarketExecutionReports().size() - 1;
        MarketExecutionReport marketExecutionReport = operation.getLastAttempt().getMarketExecutionReports().get(index);
        LOGGER.info("Order {}, execution successfully matched with the trade {} (execution price {}, execution market maker {})", fixOrderId, ticketNumber, executionPrice, executionMarketMaker);
      currentExecutionReport.setTicket(ticketNumber);
      currentExecutionReport.setMarketOrderID(ticketNumber);
      marketExecutionReport.setTicket(ticketNumber);
      
      Money oldPrice = currentExecutionReport.getPrice();
      Money newPrice = new Money(oldPrice.getStringCurrency(), executionPrice);
      currentExecutionReport.setPrice(newPrice);
      marketExecutionReport.setPrice(newPrice);
      
      MarketMaker marketMaker = null;
      try
      {
         marketMaker = marketMakerFinder.getMarketMakerByCode(executionMarketMaker);
      }
      catch (BestXException e)
      {
         LOGGER.error("Order {}, error while looking for the market maker {}", fixOrderId, executionMarketMaker, e);
      }
      if (marketMaker != null)
      {
         marketExecutionReport.setMarketMaker(marketMaker);
         currentExecutionReport.setExecBroker(executionMarketMaker);
      }
      else
      {
         LOGGER.error("Order {}, market maker {} not found",fixOrderId, executionMarketMaker);
         throw new BestXException("Manual matching : market maker " + executionMarketMaker + " not found");
      }
      operation.setStateResilient(new BBG_ExecutedState(Messages.getString("ManualMatchedTradeExecution.0", newPrice.getAmount().doubleValue(), executionMarketMaker)), ErrorState.class);
   }
   
   @Override
   public void onStateRestore(OperationState currentState) {
      // DO NOTHING
   }
   
   @Override
   public void onOperatorForceNotExecution(OperatorConsoleConnection source, String comment) {
	   ExecutionReport newExecution = ExecutionReportHelper.createExecutionReport(operation.getOrder().getInstrument(), operation.getOrder().getSide(), serialNumberService);
	   newExecution.setState(ExecutionReportState.REJECTED);
	   List<ExecutionReport> executionReports = operation.getExecutionReports();
	   executionReports.add(newExecution);
	   operation.setExecutionReports(executionReports);
	   operation.setStateResilient(new SendNotExecutionReportState(comment), ErrorState.class);
   }
      
}
