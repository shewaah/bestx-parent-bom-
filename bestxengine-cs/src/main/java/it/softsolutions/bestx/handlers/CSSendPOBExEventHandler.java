package it.softsolutions.bestx.handlers;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.fix.field.ExecTransType;
import it.softsolutions.bestx.model.CSPOBexExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.price.PriceService;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.jsscommon.Money;

public class CSSendPOBExEventHandler extends CSBaseOperationEventHandler {

	private static final long serialVersionUID = 4345177011656909323L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CSSendPOBExEventHandler.class);
	private int bookDepth = 5;
	private int priceDecimals = 6;
	private SerialNumberService serialNumberService;
	private OperatorConsoleConnection operatorConsoleConnection;
	private int pobExMaxSize = 5;
	private boolean isCancelReject = false; //BESTX-424 SP20191113 added to mange different values in attempt reject/cancel 
	
	public CSSendPOBExEventHandler(Operation operation, int bookDepth, int priceDecimals, OperatorConsoleConnection operatorConsoleConnection, SerialNumberService serialNumberService, int pobExMaxSize, boolean isCancelReject) {
		super(operation);
		this.bookDepth = bookDepth;
		this.priceDecimals = priceDecimals;
		this.operatorConsoleConnection = operatorConsoleConnection;
		this.serialNumberService = serialNumberService;
		this.pobExMaxSize = pobExMaxSize;
		this.isCancelReject = isCancelReject;
	}


	@Override
	public void onNewState(OperationState currentState) {
		LOGGER.debug("CSSendPOBExEventHandler entry action");
		
		if (operation.getAttemptNo()<=0) {
			LOGGER.debug("CSSendPOBExEventHandler skipping first attempt");
			return;
		}
		
		if(!CSExecutionReportHelper.isPOBex(operation)) {
			LOGGER.debug("CSSendPOBExEventHandler skipping for non POBex operation");
			return;
		}
		
		Order order = operation.getOrder();
		List<ExecutionReport> executionReports =  operation.getExecutionReports();
		CSPOBexExecutionReport report = CSExecutionReportHelper.createCSPOBexExecutionReport(order.getInstrument(), order.getSide(), serialNumberService, DateService.newUTCDate(), pobExMaxSize);
		report.fillFromOperation(operation);
		
		report.setPrice(new Money(order.getCurrency(), BigDecimal.ZERO));
		report.setState(ExecutionReportState.NEW);
		report.setActualQty(BigDecimal.ZERO);
		
		if (isCancelReject) {
		   report.setText("");
		} else {
		   report.setText("Order Accepted");
		}
		report.setAveragePrice(BigDecimal.ZERO);
		
		report.setExecutionReportId("0");
		
		report.setLastPx(BigDecimal.ZERO);
		
		report.setExecType(ExecTransType.Status.getFIXValue());
		
		executionReports.add(report);

		
		try {
			customerConnection.sendExecutionReport(operation, null, operation.getOrder(), operation.getIdentifier(OperationIdType.ORDER_ID), operation.getLastAttempt(), report);
		} catch (BestXException exc) {
			LOGGER.error("Cannot send execution report for attempt no: {} Fix ID: {}", operation.getAttemptNo(),  operation.getOrder().getCustomer().getFixId());
		}

	}
	
	
	@Override
	public void onUnexecutionDefault(String executionMarket) {
		if (executionMarket==null) {
			PriceDiscoveryHelper.publishEmptyBook(operation, operatorConsoleConnection);
		} else {
			LOGGER.info("PriceDiscoveryEventHandler: received onSuccess with execution market = {}", executionMarket);
		}
	}

	

	@Override
	public void onPricesResult(PriceService source, PriceResult priceResult) {
		PriceDiscoveryHelper.publishPriceDiscovery(operation, priceResult, operatorConsoleConnection, bookDepth, priceDecimals, true);
	}


	
	
}
