package it.softsolutions.bestx.connections;

import java.util.Date;
import java.util.Set;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Venue;

public interface MarketPriceConnection {
	
	void ensurePriceAvailable()  throws MarketNotAvailableException;

    /**
     * Query price, now passing the whole Order object.
     * 
     * @param listener
     * @param venues
     * @param maxLatency
     * @param order
     * @throws BestXException
     */
	void queryPrice(MarketPriceConnectionListener listener, Set<Venue> venues, long maxLatency, Order order) throws BestXException;

    /**
     * Market/SubMarket that is quoting the instrument on this market connection. Only one SubMarket is returned for every Instrument
     * 
     * @param instrument
     *            Instrument
     * @return A Market object, or null if the Instrument is not quoted at this moment from any of the SubMarkets handled by this
     *         MarketConnection
     * @throws BestXException
     */
    Market getQuotingMarket(Instrument instrument) throws BestXException;

    /**
     * Standard settlement date for the instrument on this market connection
     * 
     * @param instrument
     *            Instrument
     * @return A java.util.Date object, or null if the market connection has no standard settlement date at this moment
     * @throws BestXException
     */
    Date getMarketInstrumentSettlementDate(Instrument instrument) throws BestXException;

    /**
     * Quoting status of an instrument on this market connection at this moment
     * 
     * @param instrument
     *            Instrument
     * @return The current quoting status
     * @throws BestXException
     */
    Instrument.QuotingStatus getInstrumentQuotingStatus(Instrument instrument) throws BestXException;

    /**
     * @return
     */
    MarketCode getMarketCode();

    /**
     * Check if the instrument is quoted on the marekt
     * @param instrument : instrument to check
     * @return true if the market quotes this instrument, false otherwise
     */
    boolean isInstrumentQuotedOnMarket(Instrument instrument);
}
