package it.softsolutions.bestx.connections;

import java.util.List;

public interface ConnectionRegistry {

    CustomerConnection getCustomerConnection();

    TradingConsoleConnection getTradingConsoleConnection(String identifier);

    OperatorConsoleConnection getOperatorConsoleConnection(String identifier);

    void setConnectionStateListeners(List<ConnectionStateListener> listeners);
    
    Connection getMqPriceDiscoveryConnection();
    
    Connection getGrdLiteConnection();
    
    Connection getDatalakeConnection();
}
