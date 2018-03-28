package it.softsolutions.bestx.services.price;

import it.softsolutions.bestx.connections.MarketPriceConnection;
import it.softsolutions.bestx.connections.MarketPriceConnectionListener;
import it.softsolutions.bestx.model.Book;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Order;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;

public class __FakeMarketPriceConnectionListener implements MarketPriceConnectionListener {

    private boolean active = true;
    private Order order = null;
    private Map<String, List<MarketMarketMaker>> mmmsForMarket = new HashMap<String, List<MarketMarketMaker>>();
    private Set<MarketPriceConnection> mktPriceConnections = new HashSet<MarketPriceConnection>();

    private Map<String, Book> resultBooks = new HashMap<String, Book>();

    public void reset() {
        resetMarketMarketMakers();
        resetResultBooks();
        order = null;
    }

    public Book getResultBook(String marketName) {
        return resultBooks.get(marketName);
    }

    private void resetResultBooks() {
        resultBooks.clear();
    }

    @Override
    public void onMarketBookComplete(MarketPriceConnection source, Book book) {
        resultBooks.put(source.getMarketCode().name(), book);
    }

    @Override
    public void onMarketBookNotAvailable(MarketPriceConnection source, String reason) {
        Assert.fail("Must implement method onMarketBookNotAvailable");
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    @Override
    public Order getOrder() {
        return order;
    }

    private void resetMarketMarketMakers() {
        mmmsForMarket.clear();
    }

    public void setMarketMarketMakersForMarket(List<MarketMarketMaker> mmms, String marketCode) {
        mmmsForMarket.put(marketCode, mmms);
    }

    @Override
    public List<MarketMarketMaker> getMarketMarketMakersForMarket(MarketCode marketCode) {
        return mmmsForMarket.get(marketCode.toString());
    }

    @Override
    public List<MarketMarketMaker> getMarketMarketMakersForEnabledMarkets() {
        Assert.fail("Must implement method getMarketMarketMakersForEnabledMarkets");
        return null;
    }

    @Override
    public Date getCreationDate() {
        return new Date();
    }

    @Override
    public int getNumWaitingReplyMarketPriceConnection() {
        return 0;
    }

    public void addMarketPriceConnection(MarketPriceConnection connection) {
        mktPriceConnections.add(connection);
    }

    @Override
    public Set<MarketPriceConnection> getRemainingMarketPriceConnections() {
        return mktPriceConnections;
    }

    @Override
    public synchronized boolean isActive() {
        return active;
    }

    @Override
    public synchronized boolean deactivate() {
        active = false;
        return active;
    }

    @Override
    public void onMarketBookComplete(MarketCode marketCode, Book book) {
    }

    @Override
    public void onMarketBookPartial(MarketCode marketCode, Book book, String reason, List<String> marketMakersOnBothSides) {

    }

}
