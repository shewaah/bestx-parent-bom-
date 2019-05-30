/*
 * Copyright 2019-2028 SoftSolutions! srl 
 * All Rights Reserved. 
 * 
 * NOTICE: All information contained herein is, and remains the property of SoftSolutions! srl 
 * The intellectual and technical concepts contained herein are proprietary to SoftSolutions! srl and 
 * may be covered by EU, U.S. and other Foreign Patents, patents in process, and 
 * are protected by trade secret or copyright law. 
 * Dissemination of this information or reproduction of this material is strictly forbidden 
 * unless prior written permission is obtained from SoftSolutions! srl.
 * Any additional licenses, terms and conditions, if any, are defined in file 'LICENSE.txt', 
 * which may be part of this software package.
 */
package it.softsolutions.bestx.handlers.bondvision;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WaitingPriceState;
import it.softsolutions.bestx.states.WarningState;

/**
 * @author Stefano
 *
 */
public class BV_RejectQuoteEventHandler extends BV_ManagingRfqEventHandler {

	private final MarketBuySideConnection bvMarket;

	/**
	 * @param operation
	 */
	public BV_RejectQuoteEventHandler(Operation operation, MarketBuySideConnection bvMarket) {
		super(operation);
		
		this.bvMarket = bvMarket;
	}
	
	@Override
	public void onNewState(OperationState currentState) {
		try {
			ClassifiedProposal counter = operation.getLastAttempt().getExecutablePrice(Attempt.BEST) == null ? null : operation.getLastAttempt().getExecutablePrice(Attempt.BEST).getClassifiedProposal();
			bvMarket.rejectProposal(operation, operation.getLastAttempt().getSortedBook().getInstrument(), counter);
			operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
		} catch (BestXException exc) {
			operation.setStateResilient(new WarningState(currentState, exc, Messages.getString("BVMarketSendQuoteRejectError.0") + exc.getMessage()), ErrorState.class);
		}
	}
}
