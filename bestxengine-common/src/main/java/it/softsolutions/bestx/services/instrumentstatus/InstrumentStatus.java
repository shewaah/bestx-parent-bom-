package it.softsolutions.bestx.services.instrumentstatus;

import it.softsolutions.bestx.connections.regulated.RegulatedMessageFields;

/* 2009-09 Ruggero
 * 
 * This class "wraps" the isin and its status.
 * ACtually not used.
 * 
 */
public class InstrumentStatus {

    public String isin;
    public String quotingStatus;

    public InstrumentStatus(String isin) {
        this.isin = isin;
        // default status is NOT QUOTED (CAN)
        this.quotingStatus = RegulatedMessageFields.VALUE_TRADING_STATUS_NOT_QUOTED;
    }

    public InstrumentStatus(String isin, String quotingStatus) {
        this.isin = isin;
        this.quotingStatus = quotingStatus;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getQuotingStatus() {
        return quotingStatus;
    }

    public void setQuotingStatus(String quotingStatus) {
        this.quotingStatus = quotingStatus;
    }

}
