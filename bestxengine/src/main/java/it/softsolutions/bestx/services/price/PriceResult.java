package it.softsolutions.bestx.services.price;

import java.util.List;

import it.softsolutions.bestx.model.SortedBook;

public interface PriceResult {
    public static enum PriceResultState { COMPLETE, INCOMPLETE, ERROR, NULL }
    SortedBook getSortedBook();
    PriceResultState getState();
    String getReason();
    List<String> getErrorReport();
}
