package it.softsolutions.bestx.connections;

public interface TradeFeedConnection extends Connection {
    void setTradeFeedListener(TradeFeedConnectionListener listener);
}
