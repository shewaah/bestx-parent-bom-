package it.softsolutions.bestx.services.price;

import java.math.BigDecimal;
import java.util.Date;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.model.Rfq;
import it.softsolutions.bestx.model.Rfq.OrderSide;

public class QuoteFilterRfqQuantity implements QuoteFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuoteFilterRfqQuantity.class);
    private static final ResourceBundle messages = ResourceBundle.getBundle("messages");

    public boolean quoteIsAppropriate(Quote quote, Rfq.OrderSide orderSide, BigDecimal qty, Date futSettDate) {
        BigDecimal offeredQty = orderSide == Rfq.OrderSide.BUY ? quote.getAskProposal().getQty() : quote.getBidProposal().getQty();
        if (orderSide == Rfq.OrderSide.BUY && quote.getAskProposal().getQty().compareTo(qty) < 0 || orderSide == OrderSide.SELL && quote.getBidProposal().getQty().compareTo(qty) < 0) {
            LOGGER.info("Quote quantity does not fulfill the RFQ quantity: requested qty: " + qty.toString() + " offered qty: " + offeredQty.toString());
            return false;
        }
        return true;
    }

    public String getRejectReason() {
        return messages.getString("QUOTE_FILTER_RFQ_QUANTITY_QTY_NOT_FULFILL");
    }
}
