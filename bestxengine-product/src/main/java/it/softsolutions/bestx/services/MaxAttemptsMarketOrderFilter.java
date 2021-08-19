package it.softsolutions.bestx.services;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.MifidConfig;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.executionflow.RejectOrderAction;
import it.softsolutions.bestx.model.MarketOrder;

public class MaxAttemptsMarketOrderFilter implements MarketOrderFilter {

	private MifidConfig mifidConfig;

	@Override
	public void filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		if (operation.hasPassedMaxAttempt(this.mifidConfig.getNumRetry())) {
			operation.getLastAttempt().setByPassableForVenueAlreadyTried(true);
			operation.getLastAttempt().setNextAction(new RejectOrderAction(Messages.getString("EventNoMoreRetry.0")));
		}
	}

	public MifidConfig getMifidConfig() {
		return mifidConfig;
	}

	public void setMifidConfig(MifidConfig mifidConfig) {
		this.mifidConfig = mifidConfig;
	}

}
