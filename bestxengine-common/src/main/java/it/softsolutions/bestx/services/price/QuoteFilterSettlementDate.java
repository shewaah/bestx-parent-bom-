package it.softsolutions.bestx.services.price;

import java.math.BigDecimal;
import java.util.Date;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.model.Rfq.OrderSide;

public class QuoteFilterSettlementDate implements QuoteFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuoteFilterSettlementDate.class);
    private static final ResourceBundle messages = ResourceBundle.getBundle("messages");

    public boolean quoteIsAppropriate(Quote quote, OrderSide orderSide, BigDecimal qty, Date futSettDate) {
        if (quote.getFutSettDate() == null || futSettDate != null && !quote.getFutSettDate().equals(futSettDate)) {
            LOGGER.error("Quote future settlement date doesn't match RFQ: " + quote.getFutSettDate());
            return false;
        }
        return true;
    }

    public String getRejectReason() {
        return messages.getString("QUOTE_FILTER_SETTLEMENT_DATE_INVALID");
    }
}
