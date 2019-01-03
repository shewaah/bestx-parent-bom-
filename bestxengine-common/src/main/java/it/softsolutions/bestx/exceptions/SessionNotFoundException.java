package it.softsolutions.bestx.exceptions;

import it.softsolutions.bestx.BestXException;

public class SessionNotFoundException extends BestXException {
    
    private static final long serialVersionUID = -2578826283583126156L;

    public SessionNotFoundException() {
    }

    public SessionNotFoundException(String message) {
        super(message);
    }
}
