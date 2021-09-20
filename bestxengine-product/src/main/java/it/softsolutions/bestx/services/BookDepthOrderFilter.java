package it.softsolutions.bestx.services;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.executionflow.ExecutionInMarketAction;
import it.softsolutions.bestx.executionflow.FreezeOrderAction;
import it.softsolutions.bestx.executionflow.RejectOrderAction;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;

public class BookDepthOrderFilter implements MarketOrderFilter {

	private BookDepthValidator bookDepthValidator;
	
	@Override
	public void filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		Order customerOrder = operation.getOrder();
		Attempt currentAttempt = operation.getLastAttempt();
		if (!customerOrder.isLimitFile() && !(currentAttempt.getNextAction() instanceof FreezeOrderAction) 
				&& (marketOrder == null || marketOrder.getBuilderType() != MarketOrderBuilder.BuilderType.CUSTOM)) {
			if(!bookDepthValidator.isBookDepthValid(operation.getLastAttempt(), customerOrder)) {
				operation.getLastAttempt().setNextAction(new RejectOrderAction(Messages.getString("RejectInsufficientBookDepth.0", bookDepthValidator.getMinimumRequiredBookDepth())));
			} else {
				operation.getLastAttempt().setNextAction(new ExecutionInMarketAction());					
			}
		}
//		else {
//			if(customerOrder.isLimitFile() 
//					&& (marketOrder != null && marketOrder.getBuilderType() != MarketOrderBuilder.BuilderType.CUSTOM)) {
//				if(operation.getLastAttempt().getSortedBook().getBestProposalBySide(marketOrder.getSide()) == null) {
//					operation.getLastAttempt().setNextAction(new RejectOrderAction(Messages.getString("RejectInsufficientBookDepth.0", bookDepthValidator.getMinimumRequiredBookDepth())));					
//				} else {
//					operation.getLastAttempt().setNextAction(new ExecutionInMarketAction());					
//				}
//			}
//		}
	}

	public BookDepthValidator getBookDepthValidator() {
		return bookDepthValidator;
	}

	public void setBookDepthValidator(BookDepthValidator bookDepthValidator) {
		this.bookDepthValidator = bookDepthValidator;
	}
	
}
