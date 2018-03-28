package it.softsolutions.bestx.validators;

import java.util.ResourceBundle;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Rfq;

public class RfqValidatorInstrument implements RfqValidator {
    private static final ResourceBundle messages = ResourceBundle.getBundle("messages");
    public RfqResult validateRfq(Operation operation, Rfq rfq) {
        RfqResultBean result = new RfqResultBean();
        result.setRfq(rfq);
        if (!rfq.getInstrument().isInInventory()) {
            result.setReason(messages.getString("RFQ_VALIDATOR_INSTRUMENT_NOT_IN_INVENTORY"));
            result.setValid(false);
        } else {
            result.setValid(true);
        }
        return result;
    }
}
