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
package it.softsolutions.bestx.markets.regulated;

import it.softsolutions.bestx.connections.regulated.RegulatedFillInputBean;
import it.softsolutions.bestx.markets.regulated.RegulatedMarket;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
*
* Purpose: This class works as a magnet market selector. It has a map composed by an order number and the magnet market where we've tried to execute it.
* 
* Here we have the same methods called in the RegulatedConnector and we redirect the execution towards the correct regulated magnet market.
*
* Project Name : bestxengine-common 
* First created by: ruggero.rizzo
* Creation date: 12/ott/2012 
* 
**/
public class RegulatedMarketHelper {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RegulatedMarketHelper.class);
    private Map<String, RegulatedMarket> orderToMarket;

    public RegulatedMarketHelper() {
        orderToMarket = new HashMap<String, RegulatedMarket>();
    }

    public void saveOrderAndMarket(String orderId, RegulatedMarket regMarket) {
        LOGGER.debug("Saving sending order {} to the market {}", orderId, regMarket != null ? regMarket.getMarketCode() : null);
        
        if (orderToMarket.containsKey(orderId)) {
            LOGGER.error("Order {} already sent to the market {}", orderId, regMarket != null ? regMarket.getMarketCode() : null);
        } else {
            orderToMarket.put(orderId, regMarket);
        }
    }

    public void onFasCancelFillAndBook(String regSessionId, final String reason) {
        RegulatedMarket regMarket = orderToMarket.get(regSessionId);
        LOGGER.info("sessionID [{}], market = {}, reason = {}", regSessionId, regMarket != null ? regMarket.getMarketCode() : null, reason);

        removeOrder(regSessionId);
        
        if (regMarket != null) {
            regMarket.onFasCancelFillAndBook(regSessionId, reason);
        }
    }

    public void onFasCancelFillNoBook(String regSessionId, final String reason) {
        RegulatedMarket regMarket = orderToMarket.get(regSessionId);
        LOGGER.info("sessionID [{}], market = {}, reason = {}", regSessionId, regMarket != null ? regMarket.getMarketCode() : null, reason);

        removeOrder(regSessionId);
        
        if (regMarket != null) {
            regMarket.onFasCancelFillNoBook(regSessionId, reason);
        }
    }

    public void onFasCancelNoFill(String regSessionId, final String reason) {
        RegulatedMarket regMarket = orderToMarket.get(regSessionId);
        LOGGER.info("sessionID [{}], market = {}, reason = {}", regSessionId, regMarket != null ? regMarket.getMarketCode() : null, reason);
        
        removeOrder(regSessionId);

        if (regMarket != null) {
            regMarket.onFasCancelNoFill(regSessionId, reason);
        }
    }

    public void onFasCancelNoFill(String regSessionId, final String reason, final ExecutionReportState executionReportState, final RegulatedFillInputBean regulatedFillInputBean) {
        RegulatedMarket regMarket = orderToMarket.get(regSessionId);
        LOGGER.info("sessionID [{}], market = {}, reason = {}", regSessionId, regMarket != null ? regMarket.getMarketCode() : null, reason);
        
        removeOrder(regSessionId);

        if (regMarket != null) {
            regMarket.onFasCancelNoFill(regSessionId, reason, executionReportState, regulatedFillInputBean);
        }
    }

    public void onMarketPriceReceived(final String regSessionId, final BigDecimal marketPrice, final RegulatedFillInputBean regulatedFillInputBean) {
        RegulatedMarket regMarket = orderToMarket.get(regSessionId);
        LOGGER.info("sessionID [{}], market = {}, price = {}", regSessionId, regMarket != null ? regMarket.getMarketCode() : null, marketPrice);

        removeOrder(regSessionId);

        if (regMarket != null) {
            regMarket.onMarketPriceReceived(regSessionId, marketPrice, regulatedFillInputBean);
        } else {
            LOGGER.info("RegulatedMarket not found for sessionID [{}]", regSessionId);
        }
    }

    private void removeOrder(String orderId) {
        RegulatedMarket regMarket = orderToMarket.remove(orderId);
        LOGGER.debug("Removing order {} from the list of the order sent to the market {}", orderId, regMarket != null ? regMarket.getMarketCode() : null);

        if (regMarket == null) {
            LOGGER.info("Order not found for orderId {}", orderId);
        }
    }
}
