package it.softsolutions.bestx.services.price;

import java.math.BigDecimal;
import java.util.Date;

import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.model.Rfq.OrderSide;

public interface QuoteProcessor {
    
    Quote process(Quote bean, Instrument instrument, OrderSide orderSide, BigDecimal qty, Date futSettDate);
}
