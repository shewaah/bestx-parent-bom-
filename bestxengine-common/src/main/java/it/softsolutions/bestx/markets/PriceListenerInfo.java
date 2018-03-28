/**
 * 
 */
package it.softsolutions.bestx.markets;

import it.softsolutions.bestx.connections.MarketPriceConnectionListener;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.jsscommon.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author Stefano
 * 
 */
public class PriceListenerInfo {

    public static class VenueProposalState {
        private boolean askArrived;
        private boolean bidArrived;

        public void addProposalSide(ProposalSide side) {
            switch (side) {
            case ASK:
                askArrived = true;
                break;
            case BID:
                bidArrived = true;
                break;
            default:
                break;
            }
        }

        public boolean bothSidesArrived() {
            return askArrived & bidArrived;
        }
    }

    public List<MarketPriceConnectionListener> listeners = new ArrayList<MarketPriceConnectionListener>();
    public SortedBook sortedBook;
    public HashMap<Venue, VenueProposalState> missingProposalStates = new HashMap<Venue, VenueProposalState>();
    private BigDecimal orderQty;
    private Money priceLimit;
    private OrderSide orderSide;

    /**
     * Prices and timer expiration can occur at the same time. This variable prevents sending more than one notification.
     */
    public boolean resultSent;

    public String missingVenueCodes() {
        StringBuilder venueBuffer = new StringBuilder();
        Iterator<Venue> venueIterator = missingProposalStates.keySet().iterator();
        if (venueIterator.hasNext()) {
            venueBuffer.append(venueIterator.next().getCode());
            while (venueIterator.hasNext()) {
                venueBuffer.append(',');
                venueBuffer.append(venueIterator.next().getCode());
            }
        }
        return venueBuffer.toString();
    }

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
}
