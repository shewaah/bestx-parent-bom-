/*
 * Copyright 1997-2012 SoftSolutions! srl 
 * All Rights Reserved. 
 * 
 * NOTICE: All information contained herein is, and remains the property of SoftSolutions! srl 
 * The intellectual and technical concepts contained herein are proprietary to SoftSolutions! srl and 
 * may be covered by EU, U.S. and other Foreign Patents, patents in process, and 
 * are protected by trade secret or copyright law. 
 * Dissemination of this information or reproduction of this material is strictly forbidden 
 * unless prior written permission is obtained from SoftSolutions! srl.
 * Any additional licenses, terms and conditions, if any, are defined in file 'LICENSE.txt', 
 * which may be part of this software package.
 */

package it.softsolutions.bestx.services.tradematching;

import it.softsolutions.bestx.dao.BloombergFeedTradeDao;
import it.softsolutions.bestx.dao.OperationStateAuditDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.management.PersistentTradeMatchingServiceMBean;
import it.softsolutions.bestx.markets.bloomberg.model.BloombergFeedTrade;
import it.softsolutions.bestx.markets.bloomberg.services.BloombergTradeMatchingService;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.TradeFill;
import it.softsolutions.bestx.services.ServiceListener;
import it.softsolutions.jsscommon.Money;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class PersistentTradeMatchingServer implements BloombergTradeMatchingService, PersistentTradeMatchingServiceMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentTradeMatchingServer.class);

    private BloombergFeedTradeDao bloombergFeedTradeDao;
    private OperationStateAuditDao auditDao;
    private ServiceListener serviceListener;
    private String name;
    private volatile boolean connected;
    private final Map<String, BloombergFeedTrade> tradeMap = new ConcurrentHashMap<String, BloombergFeedTrade>();

    /**
     * Sets the bloomberg feed trade dao.
     *
     * @param bloombergFeedTradeDao the new bloomberg feed trade dao
     */
    public void setBloombergFeedTradeDao(BloombergFeedTradeDao bloombergFeedTradeDao) {
        this.bloombergFeedTradeDao = bloombergFeedTradeDao;
    }

    /**
     * Sets the audit dao.
     *
     * @param auditDao the new audit dao
     */
    public void setAuditDao(OperationStateAuditDao auditDao) {
        this.auditDao = auditDao;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.services.Service#setServiceListener(it.softsolutions.bestx.services.ServiceListener)
     */
    public void setServiceListener(ServiceListener listener) {
        serviceListener = listener;
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Inits the.
     */
    public void init() {
        checkPreRequisites();
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.management.PersistentTradeMatchingServiceMBean#restoreFromPersistence()
     */
    @Override
    public synchronized void restoreFromPersistence() {
        LOGGER.info("Restore Trade Fills from persistence");
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        tradeMap.clear();
        for (BloombergFeedTrade trade : bloombergFeedTradeDao.getAllNotAssignedTrades(today.getTime())) {
            tradeMap.put(getUniqueId(trade), trade);
        }
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.markets.bloomberg.services.BloombergTradeMatchingService#addTrade(it.softsolutions.bestx.markets.bloomberg.model.BloombergFeedTrade)
     */
    @Override
    public synchronized void addTrade(BloombergFeedTrade bloombergFeedTrade) {
        LOGGER.trace("{}", bloombergFeedTrade);

        if (tradeMap.containsKey(getUniqueId(bloombergFeedTrade))) {
            return;
        }

        tradeMap.put(getUniqueId(bloombergFeedTrade), bloombergFeedTrade);
        try {
            bloombergFeedTradeDao.saveTrade(bloombergFeedTrade);
        } catch (Exception e) {
            LOGGER.error("An error occurred while persisting trade", e);
        }
    }

    // TODO change return type
    /* (non-Javadoc)
     * @see it.softsolutions.bestx.markets.bloomberg.services.BloombergTradeMatchingService#matchTrade(it.softsolutions.bestx.model.Order, it.softsolutions.jsscommon.Money, it.softsolutions.bestx.model.MarketMaker, java.util.Date)
     */
    @Override
    public synchronized TradeFill matchTrade(Order order, Money executionPrice, MarketMaker marketMaker, Date minArrivalDate) {
        LOGGER.info("{}, {}, {}, {}", order, executionPrice, marketMaker, minArrivalDate);
        // Iterator<BloombergFeedTrade> iterator = tradeMap.values().iterator();
        // while (iterator.hasNext()) {
        // BloombergFeedTrade trade = iterator.next();
        // LOGGER.debug("BBG BloombergFeedTrade {}", trade.toString());
        //
        // if (trade.getInstrument() != null && trade.getInstrument().equals(order.getInstrument()) && trade.getActualQty() != null &&
        // trade.getActualQty().compareTo(order.getQty()) == 0
        // && trade.getSide() != null && trade.getSide().equals(order.getSide()) && trade.getFutSettDate() != null &&
        // trade.getFutSettDate().compareTo(order.getFutSettDate()) == 0
        // && trade.getMarketMaker() != null && trade.getMarketMaker().equals(marketMaker) && trade.getPrice() != null
        // && trade.getPrice().getAmount().compareTo(executionPrice.getAmount()) == 0 && (minArrivalDate == null ||
        // minArrivalDate.compareTo(trade.getTransactTime()) < 0)) {
        // LOGGER.info("Match found. Ticket number: {}", trade.getTicket());
        // iterator.remove();
        // try {
        // auditDao.assignBloombergTradeToOrder(trade, order);
        // } catch (Exception e) {
        // LOGGER.error("An error occurred while persisting trade assignment to audit", e);
        // }
        // try {
        // bloombergFeedTradeDao.setTradeAssigned(trade);
        // } catch (Exception e) {
        // LOGGER.error("An error occurred while deleting trade", e);
        // }
        // return trade;
        // }
        // }
        LOGGER.info("Match not found for order {}", order);
        return null;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.services.Service#getName()
     */
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.services.Service#isUp()
     */
    public boolean isUp() {
        return connected;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.services.Service#start()
     */
    public void start() {
        LOGGER.info("Starting service");
        connected = true;
        if (serviceListener != null) {
            serviceListener.onServiceStarted(this);
        }
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.services.Service#stop()
     */
    public void stop() {
        LOGGER.info("Stopping service");
        connected = false;
        if (serviceListener != null) {
            serviceListener.onServiceStopped(this, "");
        }
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (bloombergFeedTradeDao == null) {
            throw new ObjectNotInitializedException("Trade DAO not set");
        }
        if (auditDao == null) {
            throw new ObjectNotInitializedException("Audit DAO not set");
        }
    }

    private String getUniqueId(BloombergFeedTrade trade) {
        // TODO [DR20121004] Metto execID al posto di ticket ma non so se è giusto
        return trade.getSecurityId() + "-" + trade.getExecId();
    }
}