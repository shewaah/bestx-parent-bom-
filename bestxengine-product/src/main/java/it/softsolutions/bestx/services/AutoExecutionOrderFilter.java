package it.softsolutions.bestx.services;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.executionflow.ExecutionInMarketAction;
import it.softsolutions.bestx.executionflow.FreezeOrderAction;
import it.softsolutions.bestx.executionflow.FreezeOrderAction.NextPanel;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.states.autocurando.AutoCurandoStatus;

public class AutoExecutionOrderFilter implements MarketOrderFilter {

	private AutoCurandoStatus autoCurandoStatus;
	
	@Override
	public void filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		if (operation.getLastAttempt().getNextAction() instanceof ExecutionInMarketAction) {

			boolean doNotExecuteLF = AutoCurandoStatus.SUSPENDED.equalsIgnoreCase(autoCurandoStatus.getAutoCurandoStatus());
			boolean doNotExecuteOrder = operation.isNotAutoExecute();
			
			if (doNotExecuteOrder || (doNotExecuteLF && operation.getOrder().isLimitFile())) {
				operation.getLastAttempt().setNextAction(new FreezeOrderAction(NextPanel.ORDERS_NO_AUTOEXECUTION));
			}
		
		}
	}

	public AutoCurandoStatus getAutoCurandoStatus() {
		return autoCurandoStatus;
	}

	public void setAutoCurandoStatus(AutoCurandoStatus autoCurandoStatus) {
		this.autoCurandoStatus = autoCurandoStatus;
	}

	
	
}
