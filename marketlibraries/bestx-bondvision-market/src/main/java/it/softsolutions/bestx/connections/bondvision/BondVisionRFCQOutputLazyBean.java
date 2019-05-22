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

package it.softsolutions.bestx.connections.bondvision;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.connections.mts.MTSMessageFields;
import it.softsolutions.bestx.connections.xt2.XT2OutputLazyBean;
import it.softsolutions.bestx.markets.bondvision.BondVisionMarket;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.price.PriceService.PriceDiscoveryType;
import it.softsolutions.xt2.protocol.XT2Msg;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: paolo.midali Creation date: 13-nov-2012
 * 
 **/

public class BondVisionRFCQOutputLazyBean extends XT2OutputLazyBean {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BondVisionRFCQOutputLazyBean.class);

    private final MarketOrder marketOrder;
    private final BigDecimal qtyMultiplier;
    static final int BUY = 0;
    static final int SELL = 1;
//    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    /**
     * Instantiates a new bond vision rfq output lazy bean.
     * 
     * @param marketOrder
     *            the market order
     * @param qtyMultiplier
     *            the qty multiplier
     */
    public BondVisionRFCQOutputLazyBean(MarketOrder marketOrder, BigDecimal qtyMultiplier) {
        super(MTSMessageFields.RFQ_REQ);
        this.marketOrder = marketOrder;
        if (qtyMultiplier == null) {
            this.qtyMultiplier = BigDecimal.ONE;
        } else {
            this.qtyMultiplier = qtyMultiplier;
        }
    }

    @Override
    public XT2Msg getMsg() {
        XT2Msg msg = new XT2Msg(this.name);
        try {
            msg.setSourceMarketName("EBM_Mkt_Server");
            msg.setValue("Market", BondVisionMarket.REAL_MARKET_NAME);
            msg.setValue("InstrumentCode", marketOrder.getInstrument().getIsin());
            msg.setValue("QuotingMode", 0);
            msg.setValue("QuotingType", 1);
            msg.setValue("Type", 0);
            msg.setValue("Side", marketOrder.getSide().equals(OrderSide.BUY) ? BUY : SELL);
            msg.setValue("OfferQuantity", marketOrder.getQty().divide(qtyMultiplier).doubleValue());
            msg.setValue("Conterpart", marketOrder.getMarketMarketMaker().getMarketSpecificCode());
            msg.setValue("SettlementDate", DateService.formatAsLong(DateService.dateISO, marketOrder.getFutSettDate()));
            if (marketOrder.getPriceDiscoverySelected() == PriceDiscoveryType.NATIVE_PRICEDISCOVERY) {
                msg.setValue("BVSOrigin", 1); // 0=From Scratch, 1=From Single Dealer, 2=From Inventory
            } else {
                msg.setValue("BVSOrigin", 0); // 0=From Scratch, 1=From Single Dealer, 2=From Inventory
            }
        } catch (Exception e) {
            LOGGER.error("Unable to create RFQ message to BV due to exception " + e.getMessage());
        }
        return msg;
    }

}
