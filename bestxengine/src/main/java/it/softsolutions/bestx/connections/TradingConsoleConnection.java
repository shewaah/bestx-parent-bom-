package it.softsolutions.bestx.connections;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Trader;
import it.softsolutions.jsscommon.Money;

import java.math.BigDecimal;
import java.util.Date;

public interface TradingConsoleConnection extends Connection {

    void sendTrade(Operation source, Trader trader, OrderSide orderSide, Instrument instrument, BigDecimal qty, Date futSettDate, Money price, String tsn, String orderId) throws BestXException; // CMF

    /*
     * A more rich version of the previous one
     */
    void sendTrade(Operation source, Trader trader, String btsAcc, Order order, Money price, String tsn, String orderId, String status, String reisChoice) throws BestXException;

    void sendTradeChange(Operation source, Trader trader, OrderSide orderSide, Instrument instrument, BigDecimal qty, Date futSettDate, Money price, String tsn, String orderId, String ticketNum)
            throws BestXException;

    void sendTradeDelete(Operation source, Trader trader, OrderSide orderSide, Instrument instrument, BigDecimal qty, Date futSettDate, Money price, String tsn, String orderId, String ticketNum)
            throws BestXException;

    void sendOrderPending(Operation source, Trader trader, Order order) throws BestXException; // CMF STATUS = 4

    void sendOrderForAutoExecution(Operation source, Trader trader, Order order) throws BestXException; // CMF STATUS = 4
}
