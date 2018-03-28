package it.softsolutions.bestx.services.price;

import it.softsolutions.bestx.model.Quote;

public interface QuoteResult {
    Quote getQuote();
    boolean isValid();
    String getReason();
}
