/**
 * 
 */
package it.softsolutions.bestx.markets;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.softsolutions.bestx.connections.MarketPriceConnectionListener;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.jsscommon.Money;

/**
 * @author Stefano
 * 
 */
public class GenericPriceListenerInfo {

    public List<MarketPriceConnectionListener> listeners = new ArrayList<MarketPriceConnectionListener>();
    public SortedBook sortedBook;
    private BigDecimal orderQty;
    private Money priceLimit;
    private OrderSide orderSide;
    private Date creationTime;

    public GenericPriceListenerInfo() {
        creationTime = DateService.newLocalDate();
    }

    /**
     * Prices and timer expiration can occur at the same time. This variable prevents sending more than one notification.
     */
    public boolean resultSent;

    /**
     * @return the orderQty
     */
    public BigDecimal getOrderQty() {
        return orderQty;
    }

    /**
     * @param orderQty
     *            the orderQty to set
     */
    public void setOrderQty(BigDecimal orderQty) {
        this.orderQty = orderQty;
    }

    /**
     * @return the priceLimit
     */
    public Money getPriceLimit() {
        return priceLimit;
    }

    /**
     * @param priceLimit
     *            the priceLimit to set
     */
    public void setPriceLimit(Money priceLimit) {
        this.priceLimit = priceLimit;
    }

    public OrderSide getOrderSide() {
        return orderSide;
    }

    public void setOrderSide(OrderSide orderSide) {
        this.orderSide = orderSide;
    }

    public Date getCreationTime() {
        return creationTime;
    }
}
