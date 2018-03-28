package it.softsolutions.bestx.validators;

import it.softsolutions.bestx.model.Rfq;

public interface RfqResult {
    
    Rfq getRfq();

    boolean isValid();

    String getReason();
}
