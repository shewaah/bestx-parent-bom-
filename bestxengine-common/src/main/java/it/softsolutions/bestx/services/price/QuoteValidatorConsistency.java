package it.softsolutions.bestx.services.price;

import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.validators.QuoteValidator;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuoteValidatorConsistency implements QuoteValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuoteValidatorConsistency.class);

    public boolean validateQuote(Quote quote) {
        if (quote.getAskProposal() == null || quote.getAskProposal().getPrice() == null || quote.getAskProposal().getPrice().getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            LOGGER.error("Quote ask price invalid for isin: {}", quote.getInstrument().getIsin());
            return false;
        } else if (quote.getBidProposal() == null || quote.getBidProposal().getPrice() == null || quote.getBidProposal().getPrice().getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            LOGGER.error("Quote bid price invalid for isin: {}", quote.getInstrument().getIsin());
            return false;
        }
        return true;
    }
}
