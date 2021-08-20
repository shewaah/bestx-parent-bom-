package it.softsolutions.bestx.services;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.executionflow.ExecutionInMarketAction;
import it.softsolutions.bestx.executionflow.FreezeOrderAction;
import it.softsolutions.bestx.executionflow.FreezeOrderAction.NextPanel;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.states.autocurando.AutoCurandoStatus;

public class AutoExecutionStatusOrderFilter implements MarketOrderFilter {

	private AutoCurandoStatus autoCurandoStatus;
	
	@Override
	public void filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		if (operation.getLastAttempt().getNextAction() instanceof ExecutionInMarketAction) {

			boolean doNotExecuteLF = AutoCurandoStatus.SUSPENDED.equalsIgnoreCase(autoCurandoStatus.getAutoCurandoStatus());
			if (doNotExecuteLF && operation.getOrder().isLimitFile()) {
				
				SortedBook sortedBook = operation.getLastAttempt().getSortedBook();
				
				if (sortedBook != null && !sortedBook.getValidSideProposals(operation.getOrder().getSide()).isEmpty()) {
					operation.getLastAttempt().setNextAction(new FreezeOrderAction(NextPanel.LIMIT_FILE, Messages.getString("LimitFile.doNotExecute")));
				} else {
					operation.getLastAttempt().setNextAction(new FreezeOrderAction(NextPanel.LIMIT_FILE_NO_PRICE, Messages.getString("LimitFile.NoPrices")));
				}
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
