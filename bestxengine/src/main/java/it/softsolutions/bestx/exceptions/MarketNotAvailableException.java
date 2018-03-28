package it.softsolutions.bestx.exceptions;

import it.softsolutions.bestx.BestXException;

import java.util.List;

public class MarketNotAvailableException extends BestXException {
    private static final long serialVersionUID = 3504350219749040675L;
    
    private List<String> marketsNotEnabled;

    public MarketNotAvailableException() {
    }

    public MarketNotAvailableException(String message) {
        super(message);
    }

    public List<String> getMarketsNotEnabled() {
        return marketsNotEnabled;
    }

    public void setMarketsNotEnabled(List<String> marketsNotEnabled) {
        this.marketsNotEnabled = marketsNotEnabled;
    }
}
