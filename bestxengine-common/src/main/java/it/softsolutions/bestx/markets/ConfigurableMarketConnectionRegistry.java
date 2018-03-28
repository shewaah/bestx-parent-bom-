package it.softsolutions.bestx.markets;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.MarketConnectionRegistry;
import it.softsolutions.bestx.connections.MarketConnection;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.price.PriceService.PriceDiscoveryType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurableMarketConnectionRegistry implements MarketConnectionRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableMarketConnectionRegistry.class);
    
    private List<MarketConnection> marketConnectionList;
    private final Map<MarketCode, MarketConnection> marketConnectionMap = new ConcurrentHashMap<MarketCode, MarketConnection>();

    public void setMarketConnectionList(List<MarketConnection> marketConnectionList) {
        this.marketConnectionList = marketConnectionList;
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (marketConnectionList == null) {
            throw new ObjectNotInitializedException("Market list not set");
        }
    }

    public void init() throws BestXException {
        checkPreRequisites();
        for (MarketConnection marketConnection : marketConnectionList) {
            marketConnectionMap.put(marketConnection.getMarketCode(), marketConnection);
        }
        LOGGER.info("Loaded in memory " + marketConnectionMap.size() + " markets");
    }

    @Override
    public List<MarketConnection> getAllMarketConnections() {
        ArrayList<MarketConnection> marketList = new ArrayList<MarketConnection>(this.marketConnectionList);
        return marketList;
    }

    @Override
    public List<MarketConnection> getAllMarketConnections(Order order) {
        ArrayList<MarketConnection> marketList = new ArrayList<MarketConnection>(this.marketConnectionList);

        List<MarketConnection> removeNotAllowedMarkets = new ArrayList<MarketConnection>();
        LOGGER.debug("Order {}, check the price discovery type and eventually remove markets from those we will enquire for prices.", order.getFixOrderId());
        for (MarketConnection market : marketList) {
            if (order.getPriceDiscoverySelected().equals(PriceDiscoveryType.NATIVE_PRICEDISCOVERY)) {

                if (market.getMarketCode() == MarketCode.BLOOMBERG || market.getMarketCode() == MarketCode.TW) {
                    LOGGER.debug("Order {}, price discovery with native markets, using only direct price discoveries, do not request prices for the market {}", order.getFixOrderId(), market.getMarketCode());
                    removeNotAllowedMarkets.add(market);
                }
            }
        }
        marketList.removeAll(removeNotAllowedMarkets);
        return marketList;
    }

    @Override
    public MarketConnection getMarketConnection(MarketCode marketCode) {
        LOGGER.debug("Find market by code: " + marketCode.name());
        MarketConnection marketConnection = null;
        marketConnection = marketConnectionMap.get(marketCode);
        if (marketConnection == null) {
            LOGGER.error("Market not found for id: " + marketCode.name());
        }
        return marketConnection;
    }
}
