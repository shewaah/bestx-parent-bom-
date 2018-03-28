package it.softsolutions.bestx.connections.fixgateway;

import it.softsolutions.bestx.connections.xt2.XT2OutputLazyBean;
import it.softsolutions.xt2.protocol.XT2Msg;

public class FixOutputLazyBean extends XT2OutputLazyBean {
    protected String fixSessionId;
    
    public FixOutputLazyBean(String fixSessionId) {
        this.fixSessionId = fixSessionId;
    }
    
    protected XT2Msg buildMsg() {
        XT2Msg msg = super.getMsg();
        msg.setValue(FixMessageFields.FIX_SessionID, fixSessionId);
        return msg;
    }
    
    @Override
    public XT2Msg getMsg() {
        return buildMsg();
    }
}
