package it.softsolutions.bestx.connections.xt2;

import it.softsolutions.xt2.jpapi.XT2ConnectionIFC;
import it.softsolutions.xt2.jpapi.exceptions.XT2PapiException;

public interface XT2ConnectionProvider {
    XT2ConnectionIFC getConnection(String service) throws XT2PapiException;
}