package it.softsolutions.bestx.validators;

import it.softsolutions.bestx.model.Quote;

/**
 * Interface for validators to be used from price service to filter out invalid prices coming from the price source
 * 
 * @author lsgro
 * 
 */
public interface QuoteValidator {
    
    /**
     * Validates the quote coming from price source
     * 
     * @param quote
     *            The quote to be validated
     * @return True if the quote is valid, false otherwise
     */
    boolean validateQuote(Quote quote);
}
