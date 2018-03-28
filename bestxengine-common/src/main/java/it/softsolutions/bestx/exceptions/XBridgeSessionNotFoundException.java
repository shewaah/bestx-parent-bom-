package it.softsolutions.bestx.exceptions;

import it.softsolutions.bestx.BestXException;

public class XBridgeSessionNotFoundException extends BestXException {
    
    private static final long serialVersionUID = -2578826283583126156L;

    public XBridgeSessionNotFoundException() {
    }

    public XBridgeSessionNotFoundException(String message) {
        super(message);
    }
}
