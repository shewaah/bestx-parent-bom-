package it.softsolutions.bestx.connections.fixgateway;

import it.softsolutions.bestx.connections.xt2.XT2InputLazyBean;

public abstract class FixInputLazyBean extends XT2InputLazyBean {
    public String getSessionId() {
        return msg.getString(FixMessageFields.FIX_SessionID);
    }
}
