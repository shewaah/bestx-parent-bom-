/*
 * Project Name : BestXEngine_Akros
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author$
 * Date         : $Date$
 * Header       : $Id$
 * Revision     : $Revision$
 * Source       : $Source$
 * Tag name     : $Name$
 * State        : $State$
 *
 * For more details see cvs web documentation for project: BestXEngine_Akros
 */
package it.softsolutions.bestx.services;

import it.softsolutions.bestx.dao.InstrumentDao;
import it.softsolutions.bestx.dao.hibernate.HibernateMarketSecurityStatusDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.markets.MarketSecurityStatus;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service gives different methods to understand if an isin is a price forge one. Depending on the price forge market we should : -
 * check the PriceForgeInstrumentsTable (RTFI market) - check the MarketSecurityStatus and manage new instruments (XBRIDGE market)
 * 
 * @author ruggero.rizzo
 * 
 */
public class PriceForgeInstrumentsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceForgeInstrumentsManager.class);

    private PriceForgeService priceForge;
    private InstrumentDao instrDao;
    private HibernateMarketSecurityStatusDao hibernateMktSecStatusDao;
    private MarketCode priceForgeMktCode;

    public void init() {
        checkPrerequisites();
    }

    private void checkPrerequisites() {
        if (priceForge == null) {
            throw new ObjectNotInitializedException("PriceForge not set");
        }
        if (instrDao == null) {
            throw new ObjectNotInitializedException("InstrumentDao not set");
        }
        if (hibernateMktSecStatusDao == null) {
            throw new ObjectNotInitializedException("HibernateMarketSecurityStatusDao not set");
        }
    }

    public boolean canProcessInstrument(Instrument instrument, Order order) {
        boolean canProcess = false;

        priceForgeMktCode = PriceForgeService.getPriceForgeMarketCode();

        if (priceForgeMktCode != null) {
            if (priceForgeMktCode.equals(MarketCode.RTFI)) {
                LOGGER.info("Order " + order.getFixOrderId() + ", the price forge market is RTFI, check if the isin is in the PriceForgeInstrumentsTable");
                canProcess = checkPriceForgeInstruments(instrument, order);
            } else if (priceForgeMktCode.equals(MarketCode.XBRIDGE)) {
                LOGGER.info("Order " + order.getFixOrderId() + ", the price forge market is XBRIDGE, check if we received the isin data from the market or if it is a new instrument for BestX!.");
                canProcess = checkPriceForgeMarketAndNewInstruments(instrument, order);
            }
        } else {
            LOGGER.error("Price Forge market NOT SET!!");
        }
        return canProcess;
    }

    /**
     * Check if the instrument is in the PriceForgeInstrumentsTable, this is a view and it is the Price Forge instruments registry.
     * 
     * @param instrument
     * @param order
     * @return available : true if found, false otherwise.
     */
    private boolean checkPriceForgeInstruments(Instrument instrument, Order order) {
        boolean available = false;
        String isin = instrument.getIsin();
        Instrument priceForgeInstr = instrDao.getPriceForgeInstrumentByIsin(isin);

        if (priceForgeInstr != null && priceForgeInstr.getIsin() != null) {
            available = true;
            LOGGER.debug("Order {}. The instrument {} is in the Price Forge instruments registry, we can use the Price Forge (save for other negative conditions).", order.getFixOrderId(), isin);
        }
        return available;
    }

    /**
     * Check if the instrument is one of those notified to us by the Price Forge market, usually XBRIDGE. If not, there is the chance that
     * it is an instrument new to the BestX! system, if so we can still use the Price Forge.
     * 
     * @param instrument
     * @param order
     * @return processable : true if notified by us or not in inventory (not available in the BestX! instruments registry)
     */
    private boolean checkPriceForgeMarketAndNewInstruments(Instrument instrument, Order order) {
        boolean processable = false;
        String isin = instrument.getIsin();
        MarketSecurityStatus mktSecStatus = hibernateMktSecStatusDao.getMarketSecurityStatus(priceForgeMktCode, null, isin);
        if (mktSecStatus == null) {

            LOGGER.debug("Order {}. The instrument {} is not a PriceForge one (security status not received on Price Forge market startup), check if it is not in inventory.", order.getFixOrderId(), isin);
            if (!instrument.isInInventory()) {
                processable = true;
                LOGGER.debug("Order {}. The instrument {} is not in inventory, we can use the Price Forge (save for other negative conditions).", order.getFixOrderId(), isin);
            } else {
                LOGGER.debug("Order {}. The instrument {} is in inventory, so we cannot use the Price Forge.", order.getFixOrderId(), isin);
            }
        } else {
            LOGGER.debug("Order {}. The instrument {} is a Price Forge one, we can use the Price Forge (save for other negative conditions).", order.getFixOrderId(), isin);
            processable = true;
        }
        return processable;
    }

    public void setPriceForge(PriceForgeService priceForge) {
        this.priceForge = priceForge;
    }

    public void setInstrDao(InstrumentDao instrDao) {
        this.instrDao = instrDao;
    }

    public void setHibernateMktSecStatusDao(HibernateMarketSecurityStatusDao hibernateMktSecStatusDao) {
        this.hibernateMktSecStatusDao = hibernateMktSecStatusDao;
    }
}
