package it.softsolutions.bestx.connections.xt2;

import it.softsolutions.xt2.protocol.XT2Msg;

public abstract class XT2OutputLazyBean {
    
    protected String name;
    
    public XT2OutputLazyBean() {
        
    }
    
    public XT2OutputLazyBean(String name) {
        this.name = name;
    }
    
    public XT2Msg getMsg() {
        XT2Msg msg = null;
        if (name == null) {
            msg = new XT2Msg();
        } else {
            msg = new XT2Msg(name);
        }
        return msg;
    }
}
