package it.softsolutions.bestx.exceptions;

import it.softsolutions.bestx.BestXException;

public class RTFISessionNotFoundException extends BestXException {
    
    private static final long serialVersionUID = -2578826283583126156L;

    public RTFISessionNotFoundException() {
    }

    public RTFISessionNotFoundException(String message) {
        super(message);
    }
}
