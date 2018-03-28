package it.softsolutions.bestx.connections.regulated;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.connections.xt2.XT2OutputLazyBean;
import it.softsolutions.bestx.markets.regulated.RegulatedMarket;
import it.softsolutions.bestx.model.Instrument;

public interface RegulatedConnection extends Connection {
    void setRegulatedConnectionListener(RegulatedConnectionListener listener);
    void requestInstrumentPriceSnapshot(String motSessionId, Instrument instrument, String subMarket, String market) throws BestXException;
    void sendFokOrder(XT2OutputLazyBean order) throws BestXException;
    //TODO : da vedere se usare l'XT2OutputLazyBean anche per il FAS
    void sendFasOrder(FasOrderOutputBean order, RegulatedMarket regulatedMarket) throws BestXException;
    void revokeOrder(CancelRequestOutputBean order) throws BestXException;
}
