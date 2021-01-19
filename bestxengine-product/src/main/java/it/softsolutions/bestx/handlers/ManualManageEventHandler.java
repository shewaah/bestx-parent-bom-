/**
 * 
 */
package it.softsolutions.bestx.handlers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketConnection;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Instrument.InstrumentAttributes;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Portfolio;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.price.PriceService;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.ManualExecutionWaitingPriceState;
import it.softsolutions.bestx.states.SendExecutionReportState;
import it.softsolutions.bestx.states.SendNotExecutionReportState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.jsscommon.Money;

/**
 * @author anna
 * 
 */
public class ManualManageEventHandler extends BaseOperationEventHandler {

	private static final long serialVersionUID = 3531508632950285951L;

	private static final Logger LOGGER = LoggerFactory.getLogger(ManualManageEventHandler.class);
	public static final String AKROS_CONTO_BEST_MARKET_MAKER = "RTAKROS";
	private final SerialNumberService serialNumberService;
	private final MarketFinder marketFinder;

	/**
	 * @param operation
	 */
	public ManualManageEventHandler(Operation operation, SerialNumberService serialNumberService, MarketFinder marketFinder, MarketConnection bbgConnection) {
		super(operation);
		this.serialNumberService = serialNumberService;
		this.marketFinder = marketFinder;
	}

	@Override
	public void onNewState(OperationState currentState) {
		super.onNewState(currentState);
	}

	@Override
	public void onOperatorForceNotExecution(OperatorConsoleConnection source, String comment) {
		// Order life ended : remove all pending timers
		ExecutionReport newExecution = ExecutionReportHelper.createExecutionReport(operation.getOrder().getInstrument(), operation.getOrder().getSide(), this.serialNumberService);
		newExecution.setState(ExecutionReportState.REJECTED);
		List<ExecutionReport> executionReports = operation.getExecutionReports();
		executionReports.add(newExecution);
		operation.setExecutionReports(executionReports);
		operation.setStateResilient(new SendNotExecutionReportState(comment), ErrorState.class);
	}

	@Override
	public void onOperatorManualExecution(OperatorConsoleConnection source, String comment, Money price, BigDecimal qty, String mercato, String marketMaker, Money prezzoRif) {

		try {
			ExecutionReport newExecution = ExecutionReportHelper.createExecutionReport(operation.getOrder().getInstrument(), operation.getOrder().getSide(), this.serialNumberService);

			// Creation of the market execution reports for AMOS (sending of fills)
			MarketExecutionReport mktExecRep = new MarketExecutionReport();
			mktExecRep.setInstrument(operation.getOrder().getInstrument());
			mktExecRep.setSide(operation.getOrder().getSide());
			mktExecRep.setTransactTime(DateService.newLocalDate());
			long mktExecutionReportId = serialNumberService.getSerialNumber("EXEC_REP");
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
				tmpMkt = marketFinder.getMarketByCode(Market.MarketCode.valueOf(mercato), Market.SubMarketCode.valueOf(marketMaker));
				newExecution.setMarket(tmpMkt);
				mktExecRep.setMarket(tmpMkt);
			} else {
				tmpMkt = marketFinder.getMarketByCode(Market.MarketCode.valueOf(mercato), null);
				newExecution.setMarket(tmpMkt);
				mktExecRep.setMarket(tmpMkt);
			}
			boolean executingOnInternalMarket = false;
			newExecution.setExecBroker(marketMaker);
			// set counterpart for AMOS
			newExecution.setCounterPart(marketMaker);

			mktExecRep.setExecBroker(marketMaker);
			mktExecRep.setCounterPart(marketMaker);

			if (price == null || price.getAmount().compareTo(prezzoRif.getAmount()) == 0) {
				newExecution.setLastPx(prezzoRif.getAmount());
				mktExecRep.setLastPx(prezzoRif.getAmount());
			} else {
				newExecution.setLastPx(price.getAmount());
				mktExecRep.setLastPx(price.getAmount());
			}

			/*
			 * 20110624 - Ruggero Revised algorithm for choosing if we must use CONTO_PROPRIO or CONTO_TERZI as the
			 * execution property.
			 */
			Order order = operation.getOrder();
			Instrument instr = order.getInstrument();
			InstrumentAttributes instrAttr = instr.getInstrumentAttributes();
			Portfolio instrPortfolio = null;
			if (instrAttr != null) {
				instrPortfolio = instrAttr.getPortfolio();
			} else {
				LOGGER.warn("Portfolio not found for the instrument {}! Operating as if it is a not internalizable portfolio.", instr.getIsin());
			}

			Customer customer = order.getCustomer();
			CustomerAttributes customerAttributes = (CustomerAttributes) customer.getCustomerAttributes();
			boolean internalCust = false;
			if (customerAttributes != null) {
				internalCust = customerAttributes.getInternalCustomer();
				LOGGER.debug("Order {}, customer {}, extracted the attribute InternalCustomer, value {}", order.getFixOrderId(), customer.getName(), internalCust);
			}
			setReportsToContoTerzi(newExecution, mktExecRep, marketMaker);
			checkPricesForProperty(price, newExecution, mktExecRep, prezzoRif, marketMaker, order);
			// First : if the market maker is an internal one, set the tipoConto as CONTO_PROPRIO
			// and check the prices to set the property to BEST or SPREAD
			// if (internalMMcodes.contains(marketMaker))
			// {
			// setReportsToContoProprio(newExecution, mktExecRep);
			// }

			// Second : do the remaining checks to eventually change tipoConto and/or the property or leave
			// them untouched
			if (instrPortfolio != null && instrPortfolio.isInternalizable()) {
				if (internalCust) {
					LOGGER.debug("Order {}. Setting the execution broker to the portfolio name : {}", order.getFixOrderId(), instrPortfolio.getDescription());

					if (executingOnInternalMarket) {
						newExecution.setExecBroker(instrPortfolio.getDescription());
						mktExecRep.setExecBroker(instrPortfolio.getDescription());
						newExecution.setCounterPart(instrPortfolio.getDescription());
						mktExecRep.setCounterPart(instrPortfolio.getDescription());
					}
				} else {
					LOGGER.debug(
					        "Order {}, the portfolio is internalizable, but the customer has not enabled the internalization. Set TIPO_CONTO to CONTO_TERZI, the exec broker remains the market maker.",
					        order.getFixOrderId());
				}
			}

			if (operation.getLastAttempt().getMarketExecutionReports() == null) {
				operation.getLastAttempt().setMarketExecutionReports(new ArrayList<MarketExecutionReport>());
			}
			operation.getLastAttempt().getMarketExecutionReports().add(mktExecRep);
			this.validateOrderExecutionDestination(executingOnInternalMarket);
			LOGGER.info("Manual execution, execution report {}", newExecution.toString());
			operation.getExecutionReports().add(newExecution);
			if (newExecution.getMarket() != null) {
                operation.setStateResilient(new SendExecutionReportState(comment), ErrorState.class);
			} else {
				operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("MANUALMarketError.0")), ErrorState.class);
			}
		} catch (BestXException e) {
			LOGGER.error("Unable to create and store ExecutionReport from manual execution state", e);
			operation.setStateResilient(new WarningState(operation.getState(), e, Messages.getString("MANUALExecutionReportError.0")), ErrorState.class);
		}
	}

	private void setReportsToContoTerzi(ExecutionReport newExecution, MarketExecutionReport mktExecRep, String marketMaker) {
		// cannot set CONTO_TERZI if the mm is an internal market maker
		// if (internalMMcodes.contains(marketMaker)){
		// LOGGER.info("The market maker {} is an internal one, we cannot set the tipoConto as CONTO_TERZI",
		// marketMaker);
		// return;
		// }

		newExecution.setTipoConto(ExecutionReport.CONTO_TERZI);
		mktExecRep.setTipoConto(ExecutionReport.CONTO_TERZI);

	}

	private void setReportsToContoProprio(ExecutionReport newExecution, MarketExecutionReport mktExecRep) {
		newExecution.setTipoConto(ExecutionReport.CONTO_PROPRIO);
		mktExecRep.setTipoConto(ExecutionReport.CONTO_PROPRIO);
	}

	@Override
	public void onStateRestore(OperationState currentState) {
		LOGGER.info("ManualManageState restore. No action");
	}

	@Override
	public void onPricesResult(PriceService source, PriceResult priceResult) {
		LOGGER.info("No action");
	}

	@Override
	public void onOperatorManualManage(OperatorConsoleConnection source, String comment) {
		operation.setStateResilient(new ManualExecutionWaitingPriceState(comment), ErrorState.class);
	}

	private void checkPricesForProperty(Money price, ExecutionReport newExecution, MarketExecutionReport mktExecRep, Money prezzoRif, String marketMaker, Order order) {
		if (price != null && price.getAmount().compareTo(prezzoRif.getAmount()) != 0) {
			setBestPropertyAndContoProprio(newExecution, mktExecRep);
		} else {
			newExecution.setProperty(ExecutionReport.PROPERTY_SPREAD);
			mktExecRep.setProperty(ExecutionReport.PROPERTY_SPREAD);
		}
	}

	private void setBestPropertyAndContoProprio(ExecutionReport newExecution, MarketExecutionReport mktExecRep) {
		LOGGER.info("Set property to BEST and the TipoConto to Conto Proprio.");
		newExecution.setProperty(ExecutionReport.PROPERTY_BEST);
		mktExecRep.setProperty(ExecutionReport.PROPERTY_BEST);
		setReportsToContoProprio(newExecution, mktExecRep);
	}

	public void validateOrderExecutionDestination(boolean executingOnInternalMarket) {
		Order order = this.operation.getOrder();
		Customer customer = order.getCustomer();
		CustomerAttributes customerAttributes = null;
		LOGGER.info("Validating if order has the correct Execution Destination...");
		if (customer != null) {
			customerAttributes = (CustomerAttributes) customer.getCustomerAttributes();
		}
		if (order.isBestExecutionRequired()) {
			if (order.getInstrument().getInstrumentAttributes() != null && order.getInstrument().getInstrumentAttributes().getPortfolio().isInternalizable() && customerAttributes != null
			        && customerAttributes.getInternalCustomer() && executingOnInternalMarket) {
				order.setExecutionDestination(Order.IS_EXECUTION_DESTINATION);
				LOGGER.info("The customer is an internal one, the isin is internalizable, forcing the execution destination to {}", Order.IS_EXECUTION_DESTINATION);
			}
		}
	}
}