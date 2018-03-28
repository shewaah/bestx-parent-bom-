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
package it.softsolutions.bestx.connections.regulated;

import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_CLIENT_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_MAX_FLOOR;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ORDER_ACCOUNT;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ORDER_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ORDER_QTY;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ORDER_SOURCE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_CURRENCY;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_ISIN;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_MARKET;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_PRICE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SIDE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SUBMARKET;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_TEXT;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_SETTLEMENT_DATE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_TRADING_SESSION_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_TRANSACT_TIME;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.SUBJECT_REG_ORDER;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_CLIENT_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_DEFAULT_ORDER_TEXT;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_MAX_FLOOR;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_TRADING_SESSION_ID;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.TimeZone;

import it.softsolutions.bestx.connections.xt2.XT2OutputLazyBean;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.xt2.protocol.XT2Msg;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-common 
 * First created by: stefano 
 * Creation date: 23-ott-2012 
 * 
 **/
public abstract class RegulatedOrderOutputBean extends XT2OutputLazyBean {
    Calendar gmtCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

    private final XT2Msg orderMsg;
    protected boolean isMarketOrder = false;

    /**
     * Instantiates a new regulated order output bean.
     *
     * @param marketOrder the market order
     * @param marketCode the market code
     * @param subMarketCode the sub market code
     * @param account the account
     */
    public RegulatedOrderOutputBean(MarketOrder marketOrder, String marketCode, String subMarketCode, String account) {
        super(SUBJECT_REG_ORDER);
        orderMsg = super.getMsg();
        orderMsg.setValue(LABEL_REG_MARKET, marketCode);
        orderMsg.setValue(LABEL_REG_SUBMARKET, subMarketCode);
        orderMsg.setValue(LABEL_ORDER_ID, marketOrder.getFixOrderId());
        orderMsg.setValue(LABEL_ORDER_ACCOUNT, account);
        orderMsg.setValue(LABEL_CLIENT_ID, VALUE_CLIENT_ID); // Firm identifier used in third party-transactions.
        orderMsg.setValue(LABEL_TRADING_SESSION_ID, VALUE_TRADING_SESSION_ID);
        orderMsg.setValue(LABEL_REG_SIDE, marketOrder.getSide().getFixCode());
        orderMsg.setValue(LABEL_SETTLEMENT_DATE, DateService.format(DateService.dateISO, marketOrder.getFutSettDate()));
        orderMsg.setValue(LABEL_TRANSACT_TIME, DateService.format(DateService.dateTimeISO, marketOrder.getTransactTime()));
        orderMsg.setValue(LABEL_REG_ISIN, marketOrder.getInstrument().getIsin());
        /*
         * Time of message transmission
         * (always expressed in UTC (Universal Time Coordinated, also known as "GMT")
         */
        try {
            orderMsg.setValue(LABEL_ORDER_QTY, marketOrder.getQty().intValueExact());
        } catch(ArithmeticException e) {
            Instrument instr = marketOrder.getInstrument();
            int scale = instr.getMinSize().scale();
            BigDecimal qty = marketOrder.getQty().setScale(scale,BigDecimal.ROUND_DOWN);
            orderMsg.setValue(LABEL_ORDER_QTY, qty.doubleValue());
        }
        if (marketOrder.getLimit() != null)
        {
            orderMsg.setValue(LABEL_REG_PRICE, marketOrder.getLimit().getAmount().doubleValue());
            orderMsg.setValue(LABEL_REG_CURRENCY, marketOrder.getLimit().getStringCurrency());
        }
        else
            isMarketOrder = true;
        orderMsg.setValue(LABEL_REG_TEXT, VALUE_DEFAULT_ORDER_TEXT);
        orderMsg.setValue(LABEL_MAX_FLOOR, VALUE_MAX_FLOOR);
        
        if (marketOrder.getOrderSource() != null) {
            orderMsg.setValue(LABEL_ORDER_SOURCE, marketOrder.getOrderSource());
        }
    }
    // orderMsg.setValue(LABEL_ORDER_QTY, 102.1)
    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.xt2.XT2OutputLazyBean#getMsg()
     */
    @Override
    public XT2Msg getMsg() {
        return orderMsg;
    }
}
