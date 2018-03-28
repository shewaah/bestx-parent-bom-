package it.softsolutions.bestx.exceptions;

import it.softsolutions.bestx.BestXException;

/**
 * This exception has the purpose to allow to pass the processing of a customer revoke to the right class depending on the exceptions
 * management policy.
 * 
 * @author ruggero.rizzo
 * 
 */
public class CustomerRevokeReceivedException extends BestXException {
    private static final long serialVersionUID = 3504350219749040675L;

    public CustomerRevokeReceivedException() {
    }

    public CustomerRevokeReceivedException(String message) {
        super(message);
    }

}
