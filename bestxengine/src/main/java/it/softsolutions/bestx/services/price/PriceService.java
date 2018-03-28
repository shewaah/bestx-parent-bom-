
package it.softsolutions.bestx.services.price;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.MarketConnection;
import it.softsolutions.bestx.exceptions.MaxLatencyExceededException;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.Service;
import it.softsolutions.jsscommon.Money;

/**
 * Service for price discovery and dispatch
 * @author lsgro
 *
 */
public interface PriceService extends Service {
   
   public static enum PriceDiscoveryType {
      BBG_PRICEDISCOVERY,
      NATIVE_PRICEDISCOVERY,
      LIMIT_FILE_PRICEDISCOVERY,
      NORMAL_PRICEDISCOVERY,
      ONLY_PRICEDISCOVERY
   }
   
    /**
    * Asynchronous request for a price book for a given order. This request calculates the book based on
    * previous attempts:
    * <ul>
    * <li>If a Venue attempt was used (and therefore rejected) discard this Venue</li>
    * <li>If Venue replied with a counter-offer, and the price for this Venue didn't change, we assume that the
    * counter-offer represent a more recent price for this Venue.</li>
    * </ul>
    * @param requestor The {@link PriceServiceListener} issuing the request
    * @param order The order for which prices are queried and against which the prices should be validated
    * @param previousAttempts Previous attempts of execution of this order
    * @param venues [optional] Set of Venues to be queried for prices
    * @param maxLatency [0=null] If != 0 indicates the maximum delay in mSec that
    * the requestor is willing to wait in order to receive the information.
    * It is not mandatory to satisfy this requirement. Implementation can ignore it.
    * 0 -> wait forever, -1 -> no wait: if prices already available return them
    * @throws BestXException 
    */ 
    void requestPrices(PriceServiceListener requestor,
            Order order,
            List<Attempt> previousAttempts,
            Set<Venue> venues,
            long maxLatency) throws MaxLatencyExceededException, BestXException;

    /**
     * Asynchronous request for a price book for a given instrument
     * @param requestor The {@link PriceServiceListener} issuing the request
     * @param order the order for which the priceRequest is requested
     * @param maxLatency [0=null] If != 0 indicates the maximum delay in mSec that
     * @param position -1 requires no special case in priority queue. Else position
     * the requestor is willing to wait in order to receive the information.
     * It is not mandatory to satisfy this requirement. Implementation can ignore it.
     * 0 -> wait forever, -1 -> no wait: if prices already available return them
     */
    void requestPrices(PriceServiceListener requestor, 
          Order order, 
          List<Attempt> previousAttempts,
          Set<Venue> venues, 
          long maxLatency, 
          int position,
          List<MarketConnection> markets) throws MaxLatencyExceededException, BestXException;
    
    /**
     * Synchronous request for a price book for a given instrument
     * @param instrument The instrument for which the price is being requested
     * @param orderSide [optional] The side of the request: BUY/SELL. Useful for filtering out quantities
     * @param qty [optional] The quantity needed
     * @param futSettDate [optional] Desired settlement date
     * @param venues [optional] Set of Venues to be queried for prices
     * @param maxLatency [0=null] If != 0 indicates the maximum delay in mSec that
     * the requestor is willing to wait in order to receive the information.
     * It is not mandatory to satisfy this requirement. Implementation can ignore it.
     * 0 -> wait forever, -1 -> no wait: if prices already available return them
     * @return A {@link PriceResult} object
     * @throws MaxLatencyExceededException when maxLatency expired without finding all the prices
     */
    PriceResult getPrices(Instrument instrument,
            OrderSide orderSide,
            BigDecimal qty,
            Date futSettDate,
            Set<Venue> venues,
            long maxLatency, 
            Money priceLimit) throws MaxLatencyExceededException;
    
    public List<String> getMarketsNotEnabled();
	public Map<MarketConnection, Boolean> getMagnetMarketsEnabled();
	public Map<String, String> getMarketsStartingState();
	public String getPriceServiceName();

	public abstract void addNewTimePriceDiscovery(long newDiscoveryTime);
}
