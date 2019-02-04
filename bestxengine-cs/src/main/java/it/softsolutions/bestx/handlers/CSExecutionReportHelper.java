package it.softsolutions.bestx.handlers;

import java.util.Date;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.CSPOBexExecutionReport;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.price.PriceService.PriceDiscoveryType;
import it.softsolutions.bestx.services.serial.SerialNumberService;

public class CSExecutionReportHelper extends ExecutionReportHelper {

	public static boolean isPOBex(Operation operation) {
		// AMC 20190124 LF orders are managed as POBEX too
		return true;
/*		if(operation.getOrder() != null) {
			return operation.getOrder().getPriceDiscoveryType() == PriceDiscoveryType.NORMAL_PRICEDISCOVERY;
		}
		return false;
*/	}

	public static boolean isPOBex(Order order) {
		// AMC 20190124 LF orders are managed as POBEX too
		return true;
		//return order.getPriceDiscoveryType() == PriceDiscoveryType.NORMAL_PRICEDISCOVERY;
	}

	public static CSPOBexExecutionReport createCSPOBexExecutionReport(Instrument instrument,
			OrderSide side, SerialNumberService serialNumberService, Date transTime, int pobExMaxSize){

		CSPOBexExecutionReport executionReport = new CSPOBexExecutionReport(pobExMaxSize);
		executionReport.setInstrument(instrument);
		executionReport.setSide(side);
		executionReport.setTransactTime(transTime);
		long executionReportId = serialNumberService.getSerialNumber("EXEC_REP");
		executionReport.setSequenceId(Long.toString(executionReportId));
		return executionReport;
	}
}
