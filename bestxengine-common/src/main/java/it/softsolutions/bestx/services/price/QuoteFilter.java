package it.softsolutions.bestx.services.price;

import java.math.BigDecimal;
import java.util.Date;

import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.model.Rfq.OrderSide;

/**
 * This optional class can be used to filter out a quote in relation to a specific RFQ. Price service can be configured to use objects
 * implementing this interface for each RFQ.
 * 
 * @author lsgro
 * 
 */
public interface QuoteFilter {
    
    /**
     * Checks if the quote found from price service is appropriate for a given RFQ
     * 
     * @param quote
     *            The quote found
     * @param orderSide
     *            Side of the RFQ
     * @param qty
     *            RFQ quantity
     * @param futSettDate
     *            RFQ future settlement date
     * @return True if this quote can be used for the input parameters, false otherwise
     */
    boolean quoteIsAppropriate(Quote quote, OrderSide orderSide, BigDecimal qty, Date futSettDate);

    String getRejectReason();
}
