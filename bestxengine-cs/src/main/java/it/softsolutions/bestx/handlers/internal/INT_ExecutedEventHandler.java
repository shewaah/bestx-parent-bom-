/**
 * 
 */
package it.softsolutions.bestx.handlers.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.handlers.CSBaseOperationEventHandler;
import it.softsolutions.bestx.handlers.ExecutionReportHelper;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.SendExecutionReportState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.jsscommon.Money;

/**
 * @author Stefano
 *
 */
public class INT_ExecutedEventHandler extends CSBaseOperationEventHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(INT_ExecutedEventHandler.class);

	private SerialNumberService serialNumberService;
	private MarketFinder marketFinder;

	/**
	 * @param operation
	 */
	public INT_ExecutedEventHandler(Operation operation, SerialNumberService serialNumberService, MarketFinder marketFinder) {
		super(operation);
		this.serialNumberService = serialNumberService;
		this.marketFinder = marketFinder;
	}

	@Override
	public void onNewState(OperationState currentState) {
		Order order = operation.getOrder();
		ExecutionReport fill;
		try {
			ExecutionReport lastMarketExecReport = operation.getLastAttempt().getMarketExecutionReports().get(operation.getLastAttempt().getMarketExecutionReports().size()-1);
			fill = ExecutionReportHelper.createExecutionReport(operation.getOrder().getInstrument(), operation.getOrder().getSide(),
					this.serialNumberService);

			fill.setMarket(marketFinder.getMarketByCode(Market.MarketCode.INTERNALIZZAZIONE, null));
			if (lastMarketExecReport.getAveragePrice() != null) {
				fill.setLastPx(lastMarketExecReport.getAveragePrice());
				fill.setPrice(new Money(operation.getOrder().getCurrency(), lastMarketExecReport.getAveragePrice()));
			} else {
				if (operation.getLastAttempt().getExecutablePrice(0) != null && operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal() != null) {
					fill.setLastPx(operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal().getPrice().getAmount());
					fill.setPrice(operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal().getPrice());
				} else {
					fill.setLastPx(operation.getLastAttempt().getExecutionProposal().getPrice().getAmount());
					fill.setPrice(operation.getLastAttempt().getExecutionProposal().getPrice());
				}
			}
			fill.setTicket(lastMarketExecReport.getTicket());
			fill.setState(ExecutionReportState.FILLED);
			fill.setTipoConto(ExecutionReport.CONTO_PROPRIO);
			lastMarketExecReport.setTipoConto(ExecutionReport.CONTO_PROPRIO);
			fill.setInstrument(order.getInstrument());
			fill.setAccount(order.getCustomer().getFixId());
			fill.setActualQty(order.getQty());
			fill.setOrderQty(order.getQty());
			fill.setExecBroker(lastMarketExecReport.getExecBroker());
			fill.setCounterPart(lastMarketExecReport.getExecBroker());
			operation.getExecutionReports().add(fill);
			operation.setStateResilient(new SendExecutionReportState(), ErrorState.class);
		} catch (BestXException e) {
			LOGGER.error("Unable to create and store ExecutionReport for internal market", e);
			operation.setStateResilient(new WarningState(operation.getState(), e, Messages.getString("INTERNAL_BUYCONNECTION_NOT_FOUND")), ErrorState.class);
			return;
		}
	}
}
