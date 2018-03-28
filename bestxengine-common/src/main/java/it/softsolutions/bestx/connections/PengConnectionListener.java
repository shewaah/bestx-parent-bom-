package it.softsolutions.bestx.connections;

import it.softsolutions.bestx.connections.peng.PengQuote;

public interface PengConnectionListener {
    void onPriceReceived(String isin, PengQuote quote);
}
