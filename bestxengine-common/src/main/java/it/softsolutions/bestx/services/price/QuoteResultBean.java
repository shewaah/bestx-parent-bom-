package it.softsolutions.bestx.services.price;

import it.softsolutions.bestx.model.Quote;

public class QuoteResultBean implements QuoteResult {
    private Quote quote;
    private boolean valid;
    private String reason;

    public void setQuote(Quote quote) {
        this.quote = quote;
    }

    public Quote getQuote() {
        return quote;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isValid() {
        return valid;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
