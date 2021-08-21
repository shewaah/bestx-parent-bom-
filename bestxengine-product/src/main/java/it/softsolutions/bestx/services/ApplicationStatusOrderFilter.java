package it.softsolutions.bestx.services;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.appstatus.ApplicationStatus;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.executionflow.ExecutionInMarketAction;
import it.softsolutions.bestx.executionflow.FreezeOrderAction;
import it.softsolutions.bestx.executionflow.RejectOrderAction;
import it.softsolutions.bestx.executionflow.FreezeOrderAction.NextPanel;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceFactory;

public class ApplicationStatusOrderFilter implements MarketOrderFilter {

	private ApplicationStatus applicationStatus;
	
	private BookDepthValidator bookDepthValidator;
	
	@Override
	public void filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		if (operation.getLastAttempt().getNextAction() instanceof ExecutionInMarketAction) {
			if (this.applicationStatus.getType() == ApplicationStatus.Type.MONITOR) {
				List<ClassifiedProposal> validBook = operation.getLastAttempt().getSortedBook().getValidSideProposals(operation.getOrder().getSide());
				List<ClassifiedProposal> worsePriceBook = operation.getLastAttempt().getSortedBook().getProposalBySubState(Arrays.asList(ProposalSubState.PRICE_WORST_THAN_LIMIT), operation.getOrder().getSide());
				if (operation.getOrder().isLimitFile()) {
					if (!(marketOrder.getBuilder() instanceof FixedMarketMarketOrderBuilder)) { // If LF we need to check only that book is not empty
						operation.getLastAttempt().setNextAction(new RejectOrderAction(Messages.getString("Monitor.RejectMessage", marketOrder.getMarket().getMicCode()), true));
					} else if (!validBook.isEmpty()) {
						operation.getLastAttempt().setNextAction(new RejectOrderAction(Messages.getString("Monitor.RejectMessage", validBook.get(0).getMarket().getMicCode()), true));
					} else if (!worsePriceBook.isEmpty()) {
						int centsLFTolerance = ExecutionStrategyServiceFactory.getInstance().getCentsLFTolerance();
						
						BigDecimal targetPrice = worsePriceBook.get(0).getPrice().getAmount();
						BigDecimal limitPrice = operation.getOrder().getLimit().getAmount();
						BigDecimal differenceAbs = targetPrice.subtract(limitPrice).abs();
						BigDecimal differenceCents = differenceAbs.multiply(new BigDecimal(100));
						if (differenceCents.compareTo(new BigDecimal(centsLFTolerance)) > 0) { // Price is NOT inside tolerance
							operation.getLastAttempt().setNextAction(new FreezeOrderAction(NextPanel.LIMIT_FILE, Messages.getString("LimitFile"), true));
						} else {
							operation.getLastAttempt().setNextAction(new RejectOrderAction(Messages.getString("Monitor.RejectMessage", worsePriceBook.get(0).getMarket().getMicCode()), true));
						}
					} else {
						operation.getLastAttempt().setNextAction(new FreezeOrderAction(NextPanel.LIMIT_FILE_NO_PRICE, Messages.getString("LimitFile.NoPrices"), true));
					}
				} else { // If ALGO we need to check book depth
					if (this.bookDepthValidator.isBookDepthValid(operation.getLastAttempt(), operation.getOrder())) {
						String bestMarketCode = validBook.get(0).getMarket().getMicCode();
						operation.getLastAttempt().setNextAction(new RejectOrderAction(Messages.getString("Monitor.RejectMessage", bestMarketCode), true));
					} else {
						operation.getLastAttempt().setNextAction(new RejectOrderAction(Messages.getString("RejectInsufficientBookDepth.0", this.bookDepthValidator.getMinimumRequiredBookDepth()), true));
					}
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

	public BookDepthValidator getBookDepthValidator() {
		return bookDepthValidator;
	}

	public void setBookDepthValidator(BookDepthValidator bookDepthValidator) {
		this.bookDepthValidator = bookDepthValidator;
	}
	
	
	
}
