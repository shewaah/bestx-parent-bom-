package it.softsolutions.bestx.services;

import java.util.List;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.appstatus.ApplicationStatus;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.executionflow.ExecutionInMarketAction;
import it.softsolutions.bestx.executionflow.RejectOrderAction;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.MarketOrder;

public class ApplicationStatusOrderFilter implements MarketOrderFilter {

	private ApplicationStatus applicationStatus;
	
	@Override
	public void filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		if (operation.getLastAttempt().getNextAction() instanceof ExecutionInMarketAction) {
			if (this.applicationStatus.getType() == ApplicationStatus.Type.MONITOR) {
				List<ClassifiedProposal> validBook = operation.getLastAttempt().getSortedBook().getValidSideProposals(operation.getOrder().getSide());
				if (!validBook.isEmpty()) {
					String bestMarketCode = validBook.get(0).getMarket().getMicCode();
					operation.getLastAttempt().setNextAction(new RejectOrderAction(Messages.getString("Monitor.RejectMessage", bestMarketCode)));
				} else {
					operation.getLastAttempt().setNextAction(new RejectOrderAction(Messages.getString("RejectInsufficientBookDepth.0", 1)));
				}
			}
		}
	}

	public ApplicationStatus getApplicationStatus() {
		return applicationStatus;
	}

	public void setApplicationStatus(ApplicationStatus applicationStatus) {
		this.applicationStatus = applicationStatus;
	}
	
}
