package it.softsolutions.bestx.validators;

import it.softsolutions.bestx.model.Rfq;

public class RfqResultBean implements RfqResult {
    private String reason;
    private Rfq rfq;
    private boolean isValid;
    
    public String getReason() {
        return reason;
    }

    public Rfq getRfq() {
        return rfq;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setRfq(Rfq rfq) {
        this.rfq = rfq;
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }
}
