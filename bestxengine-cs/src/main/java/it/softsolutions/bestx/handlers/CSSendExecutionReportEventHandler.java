package it.softsolutions.bestx.handlers;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.CSPOBexExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.services.CommissionService;

public class CSSendExecutionReportEventHandler extends SendExecutionReportEventHandler {
	
	private static final long serialVersionUID = 6628132626341746242L;
	private int pobExMaxSize;
	
	public CSSendExecutionReportEventHandler(Operation operation, CommissionService commissionService, int sendExecRepTimeout, int pobExMaxSize) {
        super(operation, commissionService, sendExecRepTimeout);
        this.pobExMaxSize = pobExMaxSize;
    }
	
	@Override
	public void sendExecutionReport(ExecutionReport executionReport) throws BestXException {
		ExecutionReport csExecutionReport = null;
		if(CSExecutionReportHelper.isPOBex(operation)) {
			csExecutionReport = new CSPOBexExecutionReport(executionReport, pobExMaxSize);
			((CSPOBexExecutionReport)csExecutionReport).fillFromOperation(operation);			
			operation.getExecutionReports().remove(operation.getExecutionReports().size()-1);
			operation.getExecutionReports().add(csExecutionReport);
			super.sendExecutionReport(csExecutionReport);
		} else {
			super.sendExecutionReport(executionReport);
		}
	}
}
