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
package it.softsolutions.bestx.fix;

import it.softsolutions.bestx.fix.field.Currency;
import it.softsolutions.bestx.fix.field.HandlInst;
import it.softsolutions.bestx.fix.field.IDSource;
import it.softsolutions.bestx.fix.field.MsgType;
import it.softsolutions.bestx.fix.field.OrdType;
import it.softsolutions.bestx.fix.field.OrderSource;
import it.softsolutions.bestx.fix.field.PriceType;
import it.softsolutions.bestx.fix.field.Rule80A;
import it.softsolutions.bestx.fix.field.SettlmntTyp;
import it.softsolutions.bestx.fix.field.Side;
import it.softsolutions.bestx.fix.field.TimeInForce;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import quickfix.CharField;
import quickfix.FieldNotFound;
import quickfix.IntField;
import quickfix.StringField;
import quickfix.field.Account;
import quickfix.field.ClOrdID;
import quickfix.field.ClientID;
import quickfix.field.EffectiveTime;
import quickfix.field.FutSettDate;
import quickfix.field.MsgSeqNum;
import quickfix.field.Price;
import quickfix.field.SecurityExchange;
import quickfix.field.Symbol;
import quickfix.field.Text;
import quickfix.field.TransactTime;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-fix-client First created by: davide.rossoni Creation date: June 11, 2012
 * 
 **/
public class BXNewOrderSingle extends BXMessage<quickfix.fix41.NewOrderSingle, quickfix.fix42.NewOrderSingle> {

    private String clOrdID;
    private String account;
    private Currency currency;
    private Double price;
    private String securityID;
    private Double orderQty;
    private SettlmntTyp settlmntTyp;
    private Date futSettDate;
    private Side side;
    private String symbol;
    private IDSource idSource;
    private HandlInst handlInst;
    private OrdType ordType;
    private TimeInForce timeInForce;
    private PriceType priceType;
    private Date transactTime;
    private String text;
    
    private String clientID;
    private String securityExchange;
    private Rule80A rule80a;
    private OrderSource orderSource;

    // custom fields
    private Boolean bestExecutionVenue;
    private String ticketOwner;
    
    private Date effectiveTime;
    
    public BXNewOrderSingle() {
        super(MsgType.OrderSingle);
    }

    /**
     * Bind Fix standard message fileds to custom TS message fileds
     * 
     * @param message
     * @return
     * @throws FieldNotFound
     * @throws ParseException
     */
    public static BXNewOrderSingle fromFIX42Message(quickfix.fix42.NewOrderSingle message) throws FieldNotFound, ParseException {
        BXNewOrderSingle res = new BXNewOrderSingle();

        if (message.getHeader().isSetField(MsgSeqNum.FIELD)) {
            res.setMsgSeqNum(message.getHeader().getInt(MsgSeqNum.FIELD));
        }
        if (message.isSetClOrdID()) {
            res.clOrdID = message.getClOrdID().getValue();
        }
        if (message.isSetAccount()) {
            res.account = message.getAccount().getValue();
        }
        if (message.isSetPrice() && !message.getString(Price.FIELD).isEmpty()) {
            res.price = message.getPrice().getValue();
        }
        if (message.isSetCurrency()) {
            res.currency = Currency.getInstanceForFIXValue(message.getCurrency().getValue());
        }
        if (message.isSetSecurityID()) {
            res.securityID = message.getSecurityID().getValue();
        }
        if (message.isSetOrderQty()) {
            res.orderQty = message.getOrderQty().getValue();
        }
        if (message.isSetSettlmntTyp()) {
            res.settlmntTyp = SettlmntTyp.getInstanceForFIXValue(message.getSettlmntTyp().getValue());
        }
        if (message.isSetFutSettDate()) {
            res.futSettDate = DateUtils.parseDate(message.getFutSettDate().getValue(), DataTypes.patterns);
        }
        if (message.isSetSide()) {
            res.side = Side.getInstanceForFIXValue(message.getSide().getValue());
        }
        if (message.isSetSymbol()) {
            res.symbol = message.getSymbol().getValue();
        }
        if (message.isSetIDSource()) {
            res.idSource = IDSource.getInstanceForFIXValue(message.getIDSource().getValue());
        }
        if (message.isSetHandlInst()) {
            res.handlInst = HandlInst.getInstanceForFIXValue(message.getHandlInst().getValue());
        }
        if (message.isSetOrdType()) {
            res.ordType = OrdType.getInstanceForFIXValue(message.getOrdType().getValue());
        }
        if (message.isSetTimeInForce()) {
            res.timeInForce = TimeInForce.getInstanceForFIXValue(message.getTimeInForce().getValue());
        }
        // if (message.isSetPriceType()) {
        // res.priceType = PriceType.getInstanceForFIXValue(message.getPriceType().getValue());
        // }
        if (message.isSetTransactTime()) {
            res.transactTime = message.getTransactTime().getValue();
        }
        if (message.isSetRule80A()) {
            res.rule80a = Rule80A.getInstanceForFIXValue(message.getRule80A().getValue());
        }
        if (message.isSetText()) {
            res.text = message.getText().getValue();
        }
        if (message.isSetClientID()) {
            res.clientID = message.getClientID().getValue();
        }
        if (message.isSetSecurityExchange()) {
            res.securityExchange = message.getSecurityExchange().getValue();
        }
        //TicketOwner, CS custom field
        if (message.isSetField(5715)) {
            res.ticketOwner = message.getString(5715);
        }
        if (message.isSetEffectiveTime()) {
            res.effectiveTime = message.getEffectiveTime().getValue();
        }
        return res;
    }

    /**
     * Bind Fix standard message fileds to custom TS message fileds
     * 
     * @param message
     * @return
     * @throws FieldNotFound
     * @throws ParseException
     */
    public static BXNewOrderSingle fromFIX41Message(quickfix.fix41.NewOrderSingle message) throws FieldNotFound, ParseException {
        BXNewOrderSingle res = new BXNewOrderSingle();

        if (message.getHeader().isSetField(MsgSeqNum.FIELD)) {
            res.setMsgSeqNum(message.getHeader().getInt(MsgSeqNum.FIELD));
        }
        if (message.isSetClOrdID()) {
            res.clOrdID = message.getClOrdID().getValue();
        }
        if (message.isSetAccount()) {
            res.account = message.getAccount().getValue();
        }
        if (message.isSetPrice()) {
            res.price = message.getPrice().getValue();
        }
        if (message.isSetCurrency()) {
            res.currency = Currency.getInstanceForFIXValue(message.getCurrency().getValue());
        }
        if (message.isSetSecurityID()) {
            res.securityID = message.getSecurityID().getValue();
        }
        if (message.isSetOrderQty()) {
            res.orderQty = message.getOrderQty().getValue();
        }
        if (message.isSetSettlmntTyp()) {
            res.settlmntTyp = SettlmntTyp.getInstanceForFIXValue(message.getSettlmntTyp().getValue());
        }
        if (message.isSetFutSettDate()) {
            res.futSettDate = DateUtils.parseDate(message.getFutSettDate().getValue(), DataTypes.patterns);
        }
        if (message.isSetSide()) {
            res.side = Side.getInstanceForFIXValue(message.getSide().getValue());
        }
        if (message.isSetSymbol()) {
            res.symbol = message.getSymbol().getValue();
        }
        if (message.isSetIDSource()) {
            res.idSource = IDSource.getInstanceForFIXValue(message.getIDSource().getValue());
        }
        if (message.isSetHandlInst()) {
            res.handlInst = HandlInst.getInstanceForFIXValue(message.getHandlInst().getValue());
        }
        if (message.isSetOrdType()) {
            res.ordType = OrdType.getInstanceForFIXValue(message.getOrdType().getValue());
        }
        if (message.isSetTimeInForce()) {
            res.timeInForce = TimeInForce.getInstanceForFIXValue(message.getTimeInForce().getValue());
        }
        if (message.isSetRule80A()) {
            res.rule80a = Rule80A.getInstanceForFIXValue(message.getRule80A().getValue());
        }
        if (message.isSetText()) {
            res.text = message.getText().getValue();
        }
        if (message.isSetClientID()) {
            res.clientID = message.getClientID().getValue();
        }
        if (message.isSetSecurityExchange()) {
            res.securityExchange = message.getSecurityExchange().getValue();
        }
        if (message.isSetField(OrderSource.FIELD)) {
            res.orderSource = OrderSource.getInstanceForFIXValue(message.getField(new CharField(OrderSource.FIELD)).getValue());
        }

        return res;
    }

    @Override
    public quickfix.fix41.NewOrderSingle toFIX41Message() {
        quickfix.fix41.NewOrderSingle res = new quickfix.fix41.NewOrderSingle();

        if (clOrdID != null) {
            res.set(new ClOrdID(clOrdID));
        }
        if (account != null) {
            res.set(new Account(account));
        }
        if (currency != null) {
            res.set(new quickfix.field.Currency(currency.getFIXValue()));
        }
        if (price != null) {
            res.set(new quickfix.field.Price(price));
        }
        if (securityID != null) {
            res.set(new quickfix.field.SecurityID(securityID));
        }
        if (orderQty != null) {
            res.set(new quickfix.field.OrderQty(orderQty));
        }
        if (settlmntTyp != null) {
            res.set(new quickfix.field.SettlmntTyp(settlmntTyp.getFIXValue()));
        }
        if (futSettDate != null) {
            res.set(new FutSettDate(DateFormatUtils.format(futSettDate, DataTypes.LocalMktDate.getPattern())));
        }
        if (side != null) {
            res.set(new quickfix.field.Side(side.getFIXValue()));
        }
        if (symbol != null) {
            res.set(new Symbol(symbol));
        }
        if (side != null) {
            res.set(new quickfix.field.Side(side.getFIXValue()));
        }
        if (idSource != null) {
            res.set(new quickfix.field.IDSource(idSource.getFIXValue()));
        }
        if (handlInst != null) {
            res.set(new quickfix.field.HandlInst(handlInst.getFIXValue()));
        }
        if (ordType != null) {
            res.set(new quickfix.field.OrdType(ordType.getFIXValue()));
        }
        if (timeInForce != null) {
            res.set(new quickfix.field.TimeInForce(timeInForce.getFIXValue()));
        }
        if (priceType != null) {
            res.setField(new quickfix.field.PriceType(priceType.getFIXValue()));
        }
        if (orderSource != null) {
            res.setField(new CharField(OrderSource.FIELD, orderSource.getFIXValue()));
        }
        if (rule80a != null) {
            res.setField(new quickfix.field.Rule80A(rule80a.getFIXValue()));
        }
        if (text != null) {
            res.setField(new Text(text));
        }
        if (clientID != null) {
            res.setField(new ClientID(clientID));
        }
        if (securityExchange != null) {
            res.setField(new SecurityExchange(securityExchange));
        }

        return res;
    }

    /**
     * Build Fix standard message from custom TS message
     */
    @Override
    public quickfix.fix42.NewOrderSingle toFIX42Message() {
        quickfix.fix42.NewOrderSingle res = new quickfix.fix42.NewOrderSingle();

        if (clOrdID != null) {
            res.set(new ClOrdID(clOrdID));
        }
        if (account != null) {
            res.set(new Account(account));
        }
        if (currency != null) {
            res.set(new quickfix.field.Currency(currency.getFIXValue()));
        }
        if (price != null) {
            res.set(new quickfix.field.Price(price));
        }
        if (securityID != null) {
            res.set(new quickfix.field.SecurityID(securityID));
        }
        if (orderQty != null) {
            res.set(new quickfix.field.OrderQty(orderQty));
        }
        if (settlmntTyp != null) {
            res.set(new quickfix.field.SettlmntTyp(settlmntTyp.getFIXValue()));
        }
        if (futSettDate != null) {
            res.set(new FutSettDate(DateFormatUtils.format(futSettDate, DataTypes.LocalMktDate.getPattern())));
        }
        if (side != null) {
            res.set(new quickfix.field.Side(side.getFIXValue()));
        }
        if (symbol != null) {
            res.set(new Symbol(symbol));
        }
        if (idSource != null) {
            res.set(new quickfix.field.IDSource(idSource.getFIXValue()));
        }
        if (handlInst != null) {
            res.set(new quickfix.field.HandlInst(handlInst.getFIXValue()));
        }
        if (ordType != null) {
            res.set(new quickfix.field.OrdType(ordType.getFIXValue()));
        }
        if (timeInForce != null) {
            res.set(new quickfix.field.TimeInForce(timeInForce.getFIXValue()));
        }
        if (priceType != null) {
            res.setField(new IntField(423, priceType.getFIXValue()));
        }
//        if (transactTime != null) {
        // force transact time to always be now
        res.set(new TransactTime(new Date()));
//        }
        if (orderSource != null) {
            res.setField(new CharField(OrderSource.FIELD, orderSource.getFIXValue()));
        }
        if (rule80a != null) {
            res.setField(new quickfix.field.Rule80A(rule80a.getFIXValue()));
        }
        if (text != null) {
            res.setField(new Text(text));
        }
        if (clientID != null) {
            res.setField(new ClientID(clientID));
        }
        if (securityExchange != null) {
            res.setField(new SecurityExchange(securityExchange));
        }
        
        if (ticketOwner != null){
            res.setField(new StringField(5715 , ticketOwner));
        }
        if(effectiveTime != null) {
        	res.setField(new EffectiveTime(effectiveTime));
        }
        
        return res;
    }

    public HandlInst getHandlInst() {
        return handlInst;
    }

    public void setHandlInst(HandlInst handlInst) {
        this.handlInst = handlInst;
    }

    public OrdType getOrdType() {
        return ordType;
    }

    public void setOrdType(OrdType ordType) {
        this.ordType = ordType;
    }

    public TimeInForce getTimeInForce() {
        return timeInForce;
    }

    public void setTimeInForce(TimeInForce timeInForce) {
        this.timeInForce = timeInForce;
    }

    public PriceType getPriceType() {
        return priceType;
    }

    public void setPriceType(PriceType priceType) {
        this.priceType = priceType;
    }

    public String getClOrdID() {
        return clOrdID;
    }

    public void setClOrdID(String clOrdID) {
        this.clOrdID = clOrdID;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getSecurityID() {
        return securityID;
    }

    public void setSecurityID(String securityID) {
        this.securityID = securityID;
    }

    public Double getOrderQty() {
        return orderQty;
    }

    public void setOrderQty(Double orderQty) {
        this.orderQty = orderQty;
    }

    public SettlmntTyp getSettlmntTyp() {
        return settlmntTyp;
    }

    public void setSettlmntTyp(SettlmntTyp settlmntTyp) {
        this.settlmntTyp = settlmntTyp;
    }

    public Date getFutSettDate() {
        return futSettDate;
    }

    public void setFutSettDate(Date futSettDate) {
        this.futSettDate = futSettDate;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public IDSource getIdSource() {
        return idSource;
    }

    public void setIdSource(IDSource idSource) {
        this.idSource = idSource;
    }

    public Date getTransactTime() {
        return transactTime;
    }

    public void setTransactTime(Date transactTime) {
        this.transactTime = transactTime;
    }

    public Boolean getBestExecutionVenue() {
        return bestExecutionVenue;
    }

    public void setBestExecutionVenue(Boolean bestExecutionVenue) {
        this.bestExecutionVenue = bestExecutionVenue;
    }

    /**
     * @return the rule80a
     */
    public Rule80A getRule80a() {
        return rule80a;
    }

    /**
     * @param rule80a
     *            the rule80a to set
     */
    public void setRule80a(Rule80A rule80a) {
        this.rule80a = rule80a;
    }

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param text
     *            the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the clientID
     */
    public String getClientID() {
        return clientID;
    }

    /**
     * @param clientID the clientID to set
     */
    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    /**
     * @return the securityExchange
     */
    public String getSecurityExchange() {
        return securityExchange;
    }

    /**
     * @param securityExchange the securityExchange to set
     */
    public void setSecurityExchange(String securityExchange) {
        this.securityExchange = securityExchange;
    }


    public OrderSource getOrderSource() {
        return orderSource;
    }

    public void setOrderSource(OrderSource orderSource) {
        this.orderSource = orderSource;
    }

    /**
     * @return the ticketOwner
     */
    public String getTicketOwner() {
        return ticketOwner;
    }

    /**
     * @param ticketOwner the ticketOwner to set
     */
    public void setTicketOwner(String ticketOwner) {
        this.ticketOwner = ticketOwner;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BXNewOrderSingle [clOrdID=");
        builder.append(clOrdID);
        builder.append(", account=");
        builder.append(account);
        builder.append(", currency=");
        builder.append(currency);
        builder.append(", price=");
        builder.append(price);
        builder.append(", securityID=");
        builder.append(securityID);
        builder.append(", orderQty=");
        builder.append(orderQty);
        builder.append(", settlmntTyp=");
        builder.append(settlmntTyp);
        builder.append(", futSettDate=");
        builder.append(futSettDate);
        builder.append(", side=");
        builder.append(side);
        builder.append(", symbol=");
        builder.append(symbol);
        builder.append(", idSource=");
        builder.append(idSource);
        builder.append(", handlInst=");
        builder.append(handlInst);
        builder.append(", ordType=");
        builder.append(ordType);
        builder.append(", timeInForce=");
        builder.append(timeInForce);
        builder.append(", priceType=");
        builder.append(priceType);
        builder.append(", transactTime=");
        builder.append(transactTime);
        builder.append(", text=");
        builder.append(text);
        builder.append(", clientID=");
        builder.append(clientID);
        builder.append(", securityExchange=");
        builder.append(securityExchange);
        builder.append(", rule80a=");
        builder.append(rule80a);
        builder.append(", orderSource=");
        builder.append(orderSource);
        builder.append(", bestExecutionVenue=");
        builder.append(bestExecutionVenue);
        builder.append(", ticketOwner=");
        builder.append(ticketOwner);
        builder.append("]");
        return builder.toString();
    }

}
