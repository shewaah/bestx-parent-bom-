/*
 * Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx;

import it.softsolutions.bestx.bestexec.MarketOrderBuilderListener;
import it.softsolutions.bestx.connections.CustomerConnectionListener;
import it.softsolutions.bestx.connections.MarketBuySideConnectionListener;
import it.softsolutions.bestx.connections.OperatorConsoleConnectionListener;
import it.softsolutions.bestx.connections.TradingConsoleConnectionListener;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceCallback;
import it.softsolutions.bestx.services.price.PriceServiceListener;
import it.softsolutions.bestx.services.timer.TimerServiceListener;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public interface OperationEventListener extends TimerServiceListener, CustomerConnectionListener, TradingConsoleConnectionListener, OperatorConsoleConnectionListener, PriceServiceListener,
        MarketBuySideConnectionListener, ExecutionStrategyServiceCallback, MarketOrderBuilderListener {
    
    /**
     * Action to be performed when the operation enters a new state, like starting timers, etc. Since event handlers are state-specific,
     * this action should be implemented for a specific state. The contract for this method is:
     * <ul>
     * <li>It is called after the state validation has succeded and the state has been changed</li>
     * <li>This method must manage every exception internally</li>
     * </ul>
     * 
     * @param currentState
     *            The new state of the operation. This information is normally redundant, since the class implementing OperationEventHandler
     *            should be state-specific.
     */
    void onNewState(OperationState currentState);

    /**
     * Action to be performed when the operation re-enters a state, after being created from persistent storage. The contract for this
     * method is:
     * <ul>
     * <li>When this method is called, onNewState has been called once, in a previous application session</li>
     * <li>It is called after the state validation has succeded and the state has been changed</li>
     * <li>This method must manage every exception internally</li>
     * <li>This method can be called multiple times if the operation is restored from persistent storage</li>
     * </ul>
     * 
     * @param currentState
     *            The new state of the operation. This information is normally redundant, since the class implementing OperationEventHandler
     *            should be state-specific.
     */
    void onStateRestore(OperationState currentState);

    /**
     * Action to be performed when any event causes an Exception
     * 
     * @param currentState
     *            The state of Operation at the time of Exception
     * @param exception
     *            The exception that caused the event, or null if no exception was throwed
     * @param message
     *            An explanatory message, or null
     */
    void onApplicationError(OperationState currentState, Exception exception, String message);

    /**
     * used to allow monitor/support entities to change the status of an operation to a visible one. The implementation of each engine will
     * choose which status will be used.
     * 
     * @return a string meaningful for the monitor entity
     */
    String putInVisibleState();

    /**
     * Manage status responses from external systems, like the custom system in Clients with such a system integrated
     * @param status response status
     */
    void onCustomServiceResponse(boolean error, String securityId);
    
    /**
     * 
     *  Ask to the EventListener if an event from a market different from the handler's
     *  must be accepted
     *  
     * @param marketCode
     * 
     * @return
     */
    boolean isEventFromOtherMarketAcceptable(MarketCode marketCode);
}
