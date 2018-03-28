package it.softsolutions.bestx.connections.peng;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.PengConnection;
import it.softsolutions.bestx.connections.PengConnectionListener;
import it.softsolutions.bestx.connections.xt2.XT2BaseConnector;
import it.softsolutions.xt2.jpapi.XT2ConnectionListener;
import it.softsolutions.xt2.jpapi.XT2NotificationListener;
import it.softsolutions.xt2.protocol.XT2Msg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PengConnector extends XT2BaseConnector implements PengConnection, XT2ConnectionListener, XT2NotificationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PengConnector.class);
    private PengConnectionListener listener;

    public void setPengListener(PengConnectionListener listener) {
        this.listener = listener;
    }
    
    public void connect() throws BestXException {
        super.connect();
        LOGGER.debug("Subscribing to PENG price update");
        try {
            connection.subscribe("/BLOOMBERG_PENG/PRICE_UPDATE/*");
//            connection.subscribe("PRICE_UPDATE/*"); vecchio stile
                   } catch (Exception e) {
            throw new BestXException("An error occurred while subscribing to PENG service"+" : "+ e.toString(), e);
        }
    }
    
    public void onPublish(XT2Msg msg) {
        PengQuoteBean quote = new PengQuoteBean(msg);
        LOGGER.debug("Publish received for isin: " + quote.getIsin());
        if (listener != null)
            listener.onPriceReceived(quote.getIsin(), quote);
    }
}
