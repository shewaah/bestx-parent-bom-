/*
 * Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx.model;

import java.math.BigDecimal;
import java.util.Date;

import quickfix.field.Side;
/**  
*
* Purpose: RFQ object, maps the Rfq_Order table  
*
* Project Name : bestxengine 
* First created by: ruggero.rizzo
* Creation date: 05/set/2012 
* 
**/
public class Rfq {
    @SuppressWarnings("unused")
    private Long id;
    // [DR20120613] Adopted the standard FIX constant instead of an anonymous "1" and "2"
    public static enum OrderSide {
        BUY("" + Side.BUY), 
        SELL("" + Side.SELL),
        BUY_MINUS("" + Side.BUY_MINUS),
        SELL_PLUS("" + Side.SELL_PLUS),
        SELL_SHORT("" + Side.SELL_SHORT),
        SELL_SHORT_EXEMPT("" + Side.SELL_SHORT_EXEMPT),
        SELL_UNDISCLOSED("" + Side.SELL_UNDISCLOSED)
        ;

        /**
         * Get the order side fix code
         * 
         * @return fix code for the side
         */
        public String getFixCode() {
            return mFIXValue;
        }

        private final String mFIXValue;

        private OrderSide(String inFIXValue) {
            mFIXValue = inFIXValue;
        }
        
        public static enum OrderCapacity {
            AGENCY(quickfix.field.OrderCapacity.AGENCY), 
            AGENT_FOR_OTHER_MEMBER(quickfix.field.OrderCapacity.AGENT_FOR_OTHER_MEMBER),
            INDIVIDUAL(quickfix.field.OrderCapacity.INDIVIDUAL),
            MIXED_CAPACITY(quickfix.field.OrderCapacity.MIXED_CAPACITY),
            PRINCIPAL(quickfix.field.OrderCapacity.PRINCIPAL),
            PROPRIETARY(quickfix.field.OrderCapacity.PROPRIETARY),
            RISKLESS_PRINCIPAL(quickfix.field.OrderCapacity.RISKLESS_PRINCIPAL)
            ;

            /**
             * Get the order side fix code
             * 
             * @return fix code for the side
             */
            public char getFixCode() {
                return mFIXValue;
            }

            private final char mFIXValue;

            private OrderCapacity(char inFIXValue) {
                mFIXValue = inFIXValue;
            }
        }

        static private String sellCodes[] = {OrderSide.SELL.getFixCode(), OrderSide.SELL_PLUS.getFixCode(), OrderSide.SELL_SHORT.getFixCode(), OrderSide.SELL_SHORT_EXEMPT.getFixCode(), OrderSide.SELL_UNDISCLOSED.getFixCode()};
       	static private String buyCodes[] = {OrderSide.BUY.getFixCode(), OrderSide.BUY_MINUS.getFixCode()};


        public static boolean isSell(OrderSide os) {
        	String osVal = os.getFixCode();
        	for (int i = 0; i < sellCodes.length; i++)
        		if(sellCodes[i].equals(osVal))
        			return true;
        	return false;
        }
        public static boolean isBuy(OrderSide os) {
        	String osVal = os.getFixCode();
        	for (int i = 0; i < buyCodes.length; i++)
        		if(buyCodes[i].equals(osVal))
        			return true;
        	return false;
       }
        public static boolean isSell(String  osVal) {
        	for (int i = 0; i < sellCodes.length; i++)
        		if(sellCodes[i].equals(osVal))
        			return true;
        	return false;
        }
        public static boolean isBuy(String osVal) {
        	for (int i = 0; i < buyCodes.length; i++)
        		if(buyCodes[i].equals(osVal))
        			return true;
        	return false;
       }
    }

    
    // [DR20120613] Order extends Rfq and Rfq includes the Rfq.OrderSide enum: wrong circular references!!!
    private OrderSide side;
    private Date futSettDate;
    private Customer customer;
    private Instrument instrument;
    private Date transactTime;
    private String secExchange;
    private Integer settlementType;
    private BigDecimal qty;
    
    /**
     * Initialize the RFQ values from another RFQ
     * @param source : source RFQ
     */
    public void setValues(Rfq source) {
        setCustomer(source.getCustomer());
        setInstrument(source.getInstrument());
        setSettlementType(source.getSettlementType());
        setFutSettDate(source.getFutSettDate());
        setQty(source.getQty());
        setSecExchange(source.getSecExchange());
        setSide(source.getSide());
        setTransactTime(source.getTransactTime());
    }
    /**
     * Set RFQ side
     * @param side : the side
     */
    public void setSide(OrderSide side) {
        this.side = side;
    }
    /**
     * Get RFQ side
     * @return side
     */
    public OrderSide getSide() {
        return side;
    }
    /**
     * Set FutSettDate
     * @param futSettDate : the futSettDate
     */
    public void setFutSettDate(Date futSettDate) {
        this.futSettDate = futSettDate;
    }
    /**
     * Get FutSettDate
     * @return futSettDate
     */
    public Date getFutSettDate() {
        return futSettDate;
    }
    /**
     * Set the customer
     * @param customer : the customer
     */
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    /**
     * Get the customer
     * @return customer
     */
    public Customer getCustomer() {
        return customer;
    }
    /**
     * Set the instrument
     * @param instrument : the instrument
     */
    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }
    /**
     * Get the instrument
     * @return instrument
     */
    public Instrument getInstrument() {
        return instrument;
    }
    /**
     * Set the transaction time
     * @param transactTime : transaction time
     */
    public void setTransactTime(Date transactTime) {
        this.transactTime = transactTime;
    }
    /**
     * Get the transaction time
     * @return transaction time
     */
    public Date getTransactTime() {
        return transactTime;
    }
    /**
     * Set the security exchange
     * @param secExchange : security exchange
     */
    public void setSecExchange(String secExchange) {
        this.secExchange = secExchange;
    }
    /**
     * Get the security exchange
     * @return the exchange
     */
    public String getSecExchange() {
        return secExchange;
    }
    /**
     * Set the settlement type
     * @param settlementType : the settlement type
     */
    public void setSettlementType(Integer settlementType) {
        this.settlementType = settlementType;
    }
    /**
     * Get the settlement type
     * @return the type
     */
    public Integer getSettlementType() {
        return settlementType;
    }
    /**
     * Set the RFQ quantity
     * @param quantity : RFQ quantity
     */
    public void setQty(BigDecimal quantity) {
        this.qty = quantity;
    }
    /**
     * Get the RFQ quantity
     * @return the quantity
     */
    public BigDecimal getQty() {
        return qty;
    }
}
