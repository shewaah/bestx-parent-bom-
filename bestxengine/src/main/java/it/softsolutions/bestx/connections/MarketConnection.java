package it.softsolutions.bestx.connections;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Proposal.ProposalSide;

import javax.naming.OperationNotSupportedException;

public abstract class MarketConnection {
    
    public static final String BESTX_RESTART = "Bestx.restart";
    
    private boolean priceConnectionEnabled;
    private boolean buySideConnectionEnabled;
    private String disableComment;

    public boolean isPriceConnectionEnabled() {
        return priceConnectionEnabled;
    }

    public void enablePriceConnection() {
        this.priceConnectionEnabled = true;
    }

    public void disablePriceConnection() {
        this.priceConnectionEnabled = false;
    }

    public boolean isBuySideConnectionEnabled() {
        return buySideConnectionEnabled;
    }

    public void enableBuySideConnection() {
        this.buySideConnectionEnabled = true;
    }

    public void disableBuySideConnection() {
        this.buySideConnectionEnabled = false;
    }

    public MarketBuySideConnection getBuySideConnection() {
        return null;
    }

    public MarketPriceConnection getPriceConnection() {
        return null;
    }

    public void startBuySideConnection() throws BestXException {
    }

    public void stopBuySideConnection() throws BestXException {
    }

    public void startPriceConnection() throws BestXException {
    }

    public void stopPriceConnection() throws BestXException {
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof MarketConnection)) {
            return false;
        } else {
            return ((MarketConnection) o).getMarketCode().equals(this.getMarketCode());
        }
    }

    @Override
    public int hashCode() {
        return getMarketCode().hashCode();
    }

    @Override
    public String toString() {
        return getMarketCode().name();
    }

    /**
     * Specifies if there is some configuration or other that must prevent the price to be added tio the original prices book
     * 
     * @param instrument
     * @param marketMaker
     * @return false iff the proposal associated to the Market Maker for the Instrument must be discarded from Book
     */
    public abstract boolean isInstrumentTradableWithMarketMaker(Instrument instrument, MarketMarketMaker marketMaker);

    /**
     * Specify if buy side connection is provided by this market connection
     * 
     * @return A boolean
     */
    public abstract MarketCode getMarketCode();

    /**
     * Specify if buy side connection is provided by this market connection
     * 
     * @return A boolean
     */
    public abstract boolean isBuySideConnectionProvided();

    /**
     * Specify if price connection is provided by this market connection
     * 
     * @return A boolean
     */
    public abstract boolean isPriceConnectionProvided();

    /**
     * Queries status of the market for availability of order service
     * 
     * @return A boolean
     */
    public abstract boolean isBuySideConnectionAvailable();

    /**
     * Queries status of the market for availability of quoting service
     * 
     * @return A boolean
     */
    public abstract boolean isPriceConnectionAvailable();

    /**
     * @return the disableComment
     */
    public String getDisableComment() {
        return disableComment;
    }

    /**
     * @param disableComment
     *            the disableComment to set
     */
    public void setDisableComment(String disableComment) {
        this.disableComment = disableComment;
    }

    public void onNullPrices(String regulatedSessionId, String reason, ProposalSide side) throws OperationNotSupportedException {
        throw new OperationNotSupportedException();
    }

    /**
     * Method for knowing if the market supports the magnet feature.
     * 
     * @return true if the magnet feature works on this market, false otherwise
     */
    public abstract boolean isAMagnetMarket();

    /**
     * This method is used to check if the market is idle (it means that the xt2 gateway is connected and synchronized to the market). In
     * some situations, by example when an order enters BestX:FI-A while TW is sending us the securities definitions, is not enough to check if
     * the price connection is enabled to know if we can perform a price discovery on that market. In fact the price connection has been
     * enabled at the start of BestX:FI-A. While the market is still sending us the securites definitions it is not in idle and will not handle
     * a price request, so we must not send price requests to it.
     * 
     * Here we define a default implementation that returns true. If a market uses to check the price connection, it should, instead, check
     * for the idle of its connection. By example look a the TradeWebMarket implementation of this method.
     * 
     * @author ruggero.rizzo
     * @return
     * @throws BestXException
     */
    public boolean isMarketIdle() throws BestXException {
        return true;
    }
}