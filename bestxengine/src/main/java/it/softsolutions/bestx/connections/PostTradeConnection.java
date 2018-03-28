
package it.softsolutions.bestx.connections;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Trader;
import it.softsolutions.jsscommon.Money;

public interface PostTradeConnection {
    
    void sendAmendedTrade(Operation source, Trader trader, Order order, Money price, String tsn, String orderId) throws BestXException;

    void sendCancelTrade(Operation source, Trader trader, Order order, Money price, String tsn, String orderId) throws BestXException;
}
