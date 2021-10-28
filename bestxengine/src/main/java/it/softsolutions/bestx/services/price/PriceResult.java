package it.softsolutions.bestx.services.price;

import java.util.List;

import it.softsolutions.bestx.model.SortedBook;

public interface PriceResult {
    public static enum PriceResultState {
    	COMPLETE, // We have received a reply from all markets
    	INCOMPLETE, // We have a partial result (at least one of the markets doesn't reply all prices in time)
    	EMPTY, // We have an empty book
    	NULL, // We receive a cancel order (35=F) while starting an order price discovery
    	UNAVAILABLE // All markets (or price sources) are unavailable
    }
    SortedBook getSortedBook();
    PriceResultState getState();
    String getReason();
    List<String> getErrorReport();
}
