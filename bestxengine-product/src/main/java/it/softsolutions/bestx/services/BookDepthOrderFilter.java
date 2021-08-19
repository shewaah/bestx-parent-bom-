package it.softsolutions.bestx.services;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.executionflow.RejectOrderAction;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;

public class BookDepthOrderFilter implements MarketOrderFilter {

	private BookDepthValidator bookDepthValidator;
	
	@Override
	public void filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		Order customerOrder = operation.getOrder();
		if (!customerOrder.isLimitFile()) { // This we only check if 59=0
			if (marketOrder == null || marketOrder.getBuilder() instanceof BestXMarketOrderBuilder) { // And has been created with the default market order builder
				if (!bookDepthValidator.isBookDepthValid(operation.getLastAttempt(), customerOrder)) {
					operation.getLastAttempt().setNextAction(new RejectOrderAction(Messages.getString("RejectInsufficientBookDepth.0", bookDepthValidator.getMinimumRequiredBookDepth())));
				}
			}
		}	
	}

	public BookDepthValidator getBookDepthValidator() {
		return bookDepthValidator;
	}

	public void setBookDepthValidator(BookDepthValidator bookDepthValidator) {
		this.bookDepthValidator = bookDepthValidator;
	}
	
}
