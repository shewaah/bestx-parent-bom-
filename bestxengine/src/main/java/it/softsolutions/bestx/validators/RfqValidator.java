package it.softsolutions.bestx.validators;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Rfq;

public interface RfqValidator {
    RfqResult validateRfq(Operation operation, Rfq rfq);
}
