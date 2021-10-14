package it.softsolutions.bestx.services;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder.BuilderType;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.executionflow.FreezeOrderAction;
import it.softsolutions.bestx.executionflow.FreezeOrderAction.NextPanel;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceFactory;
import it.softsolutions.bestx.services.instrument.BondTypesService;

public class CSUSTTargetPriceOrderFilter implements MarketOrderFilter {

	@Override
	public void filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		if (BondTypesService.isUST(operation.getOrder().getInstrument())) {
			if (marketOrder != null) {
				marketOrder.setLimit(operation.getOrder().getLimit());
				
				Order order = operation.getOrder();
				SortedBook sortedBook = operation.getLastAttempt().getSortedBook();
				
				if (sortedBook != null) {
					
					List<ClassifiedProposal> validProposals = sortedBook.getValidSideProposals(order.getSide());
					List<ClassifiedProposal> proposalsDiscardedByLimitPrice = sortedBook.getProposalBySubState(
							Arrays.asList(ProposalSubState.PRICE_WORST_THAN_LIMIT), order.getSide());

					if (validProposals.isEmpty() && !proposalsDiscardedByLimitPrice.isEmpty()) {
						int centsLFTolerance = ExecutionStrategyServiceFactory.getInstance().getCentsLFTolerance();
						
						BigDecimal targetPrice = proposalsDiscardedByLimitPrice.get(0).getPrice().getAmount();
						BigDecimal limitPrice = operation.getOrder().getLimit().getAmount();
						BigDecimal differenceAbs = targetPrice.subtract(limitPrice).abs();
						BigDecimal differenceCents = differenceAbs.multiply(new BigDecimal(100));
						if (differenceCents.compareTo(new BigDecimal(centsLFTolerance)) > 0) { // Price is NOT inside tolerance
						   if (marketOrder.getBuilderType() == BuilderType.CUSTOM) {
	                     operation.getLastAttempt().setNextAction(new FreezeOrderAction(NextPanel.LIMIT_FILE, Messages.getString("LimitFile.Rest", 
	                           (operation.getLastAttempt().getMarketOrder().getLimit() != null ? operation.getLastAttempt().getMarketOrder().getLimit().getAmount() : "NA"),
                              (operation.getLastAttempt().getMarketOrder().getLimitMonitorPrice() != null ? operation.getLastAttempt().getMarketOrder().getLimitMonitorPrice().getAmount() : "NA"),
                              operation.getOrder().getLimit().getAmount(),
                              ExecutionStrategyServiceFactory.getInstance().getCentsLFTolerance()), true));
			            } else {
	                     operation.getLastAttempt().setNextAction(new FreezeOrderAction(NextPanel.LIMIT_FILE, Messages.getString("LimitFile"), true));
			            }						   
						}
					}
				}
			}
		}
	}

}
