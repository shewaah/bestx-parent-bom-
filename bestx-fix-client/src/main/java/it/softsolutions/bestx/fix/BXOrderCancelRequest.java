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

import it.softsolutions.bestx.fix.field.MsgType;
import it.softsolutions.bestx.fix.field.Side;

import java.util.Date;

import quickfix.FieldNotFound;
import quickfix.field.ClOrdID;
import quickfix.field.MsgSeqNum;
import quickfix.field.OrderID;
import quickfix.field.OrigClOrdID;
import quickfix.field.SecurityID;
import quickfix.field.SenderSubID;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-fix-client 
 * First created by: davide.rossoni 
 * Creation date: Jun 12, 2012 
 * 
 **/
public class BXOrderCancelRequest extends BXMessage<quickfix.fix41.OrderCancelRequest, quickfix.fix42.OrderCancelRequest> {

    private String clOrdID;
    private String orderID;
    private String origClOrdID;
    private String securityID;
    private String symbol;
    private Double orderQty;
    private Side side;
    private Date transactTime;
    private String senderSubID;
    private String idSource;
    private String securityExchange;
    
    public BXOrderCancelRequest() {
        super(MsgType.OrderCancelRequest);
    }

    /**
     * Bind Fix standard message fileds to custom TS message fileds
     * 
     * @param message
     * @return
     * @throws FieldNotFound
     */
    public static BXOrderCancelRequest fromFIX42Message(quickfix.fix42.OrderCancelRequest message) throws FieldNotFound {
        BXOrderCancelRequest res = new BXOrderCancelRequest();

        if (message.getHeader().isSetField(MsgSeqNum.FIELD)) {
            res.setMsgSeqNum(message.getHeader().getInt(MsgSeqNum.FIELD));
        }
        if (message.isSetClOrdID()) {
            res.clOrdID = message.getClOrdID().getValue();
        }
        if (message.isSetOrderID()) {
            res.orderID = message.getOrderID().getValue();
        }
        if (message.isSetOrigClOrdID()) {
            res.origClOrdID = message.getOrigClOrdID().getValue();
        }
        if (message.getSecurityID() != null) {
            res.securityID = message.getSecurityID().getValue();
        }
        if (message.getSymbol() != null) {
            res.symbol = message.getSymbol().getValue();
        }
        if (message.isSetSide()) {
            res.side = Side.getInstanceForFIXValue(message.getSide().getValue());
        }
        if (message.isSetTransactTime()) {
            res.transactTime = message.getTransactTime().getValue();
        }
        else {
        	res.transactTime = new Date();
        }
        if (message.isSetOrderQty()) {
            res.orderQty = message.getOrderQty().getValue();
        }
        if (message.getHeader().isSetField(SenderSubID.FIELD)) {
            res.senderSubID = message.getHeader().getString(SenderSubID.FIELD);
        }
        if (message.isSetIDSource()) {
            res.idSource = message.getIDSource().getValue();
        }
        if (message.isSetSecurityExchange()) {
            res.securityExchange = message.getSecurityExchange().getValue();
        }
        return res;
    }
    
    /**
     * Bind Fix standard message fileds to custom TS message fileds
     * 
     * @param message
     * @return
     * @throws FieldNotFound
     */
    public static BXOrderCancelRequest fromFIX41Message(quickfix.fix41.OrderCancelRequest message) throws FieldNotFound {
        BXOrderCancelRequest res = new BXOrderCancelRequest();

        if (message.getHeader().isSetField(MsgSeqNum.FIELD)) {
            res.setMsgSeqNum(message.getHeader().getInt(MsgSeqNum.FIELD));
        }
        if (message.isSetClOrdID()) {
            res.clOrdID = message.getClOrdID().getValue();
        }
        if (message.isSetOrderID()) {
            res.orderID = message.getOrderID().getValue();
        }
        if (message.isSetOrigClOrdID()) {
            res.origClOrdID = message.getOrigClOrdID().getValue();
        }
        if (message.getSecurityID() != null) {
            res.securityID = message.getSecurityID().getValue();
        }
        if (message.getSymbol() != null) {
            res.symbol = message.getSymbol().getValue();
        }
        if (message.isSetSide()) {
            res.side = Side.getInstanceForFIXValue(message.getSide().getValue());
        }
//        if (message.isSetTransactTime()) {
//            res.transactTime = message.getTransactTime().getValue();
//        }
        res.transactTime = new Date();
        if (message.isSetOrderQty()) {
            res.orderQty = message.getOrderQty().getValue();
        }
        if (message.getHeader().isSetField(SenderSubID.FIELD)) {
            res.senderSubID = message.getHeader().getString(SenderSubID.FIELD);
        }
        if (message.isSetIDSource()) {
            res.idSource = message.getIDSource().getValue();
        }
        if (message.isSetSecurityExchange()) {
            res.securityExchange = message.getSecurityExchange().getValue();
        }
        
        return res;
    }

    /**
     * Build Fix standard message from custom TS message
     */
    @Override
    public quickfix.fix41.OrderCancelRequest toFIX41Message() {
        quickfix.fix41.OrderCancelRequest res = new quickfix.fix41.OrderCancelRequest();

        if (clOrdID != null) {
            res.set(new ClOrdID(clOrdID));
        }
        if (orderID != null) {
            res.set(new OrderID(orderID));
        }
        if (origClOrdID != null) {
            res.set(new OrigClOrdID(origClOrdID));
        }
        if (securityID != null) {
            res.set(new SecurityID(securityID));
        }
        if (symbol != null) {
            res.set(new Symbol(symbol));
        }
        if (side != null) {
            res.set(new quickfix.field.Side(side.getFIXValue()));
        }
//        if (transactTime != null) {
//            res.setField(new TransactTime(transactTime));
//       }
//        else {
//        	res.setField(new TransactTime(new Date()));
//        }
        if (orderQty != null) {
            res.set(new quickfix.field.OrderQty(orderQty));
        }
        if (senderSubID != null) {
            res.setString(SenderSubID.FIELD, senderSubID);
        }
        if (idSource != null) {
            res.set(new quickfix.field.IDSource(idSource));
        }
        if (securityExchange != null) {
            res.set(new quickfix.field.SecurityExchange(securityExchange));
        }
        return res;
    }

    @Override
    public quickfix.fix42.OrderCancelRequest toFIX42Message() {
        quickfix.fix42.OrderCancelRequest res = new quickfix.fix42.OrderCancelRequest();

        if (clOrdID != null) {
            res.set(new ClOrdID(clOrdID));
        }
        if (orderID != null) {
            res.set(new OrderID(orderID));
        }
        if (origClOrdID != null) {
            res.set(new OrigClOrdID(origClOrdID));
        }
        if (securityID != null) {
            res.set(new SecurityID(securityID));
        }
        if (symbol != null) {
            res.set(new Symbol(symbol));
        }
        if (side != null) {
            res.set(new quickfix.field.Side(side.getFIXValue()));
        }
        if (transactTime != null) {
            res.set(new TransactTime(transactTime));
        } else {
        	res.set(new TransactTime(new Date()));
        }
        if (orderQty != null) {
            res.set(new quickfix.field.OrderQty(orderQty));
        }
        if (senderSubID != null) {
            res.setString(SenderSubID.FIELD, senderSubID);
        }
        if (idSource != null) {
            res.set(new quickfix.field.IDSource(idSource));
        }
        if (securityExchange != null) {
            res.set(new quickfix.field.SecurityExchange(securityExchange));
        }
        
        return res;
    }

    public String getClOrdID() {
        return clOrdID;
    }

    public void setClOrdID(String clOrdID) {
        this.clOrdID = clOrdID;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public String getOrigClOrdID() {
        return origClOrdID;
    }

    public void setOrigClOrdID(String origClOrdID) {
        this.origClOrdID = origClOrdID;
    }

    public String getSecurityID() {
        return securityID;
    }

    public void setSecurityID(String securityID) {
        this.securityID = securityID;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Double getOrderQty() {
        return orderQty;
    }

    public void setOrderQty(Double orderQty) {
        this.orderQty = orderQty;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public Date getTransactTime() {
        return transactTime;
    }

    public void setTransactTime(Date transactTime) {
        this.transactTime = transactTime;
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

    /**
     * @return the idSource
     */
    public String getIdSource() {
        return idSource;
    }

    /**
     * @param idSource the idSource to set
     */
    public void setIdSource(String idSource) {
        this.idSource = idSource;
    }

    /**
     * @return the senderSubID
     */
    public String getSenderSubID() {
        return senderSubID;
    }

    /**
     * @param senderSubID the senderSubID to set
     */
    public void setSenderSubID(String senderSubID) {
        this.senderSubID = senderSubID;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BXOrderCancelRequest [clOrdID=");
        builder.append(clOrdID);
        builder.append(", orderID=");
        builder.append(orderID);
        builder.append(", origClOrdID=");
        builder.append(origClOrdID);
        builder.append(", securityID=");
        builder.append(securityID);
        builder.append(", symbol=");
        builder.append(symbol);
        builder.append(", orderQty=");
        builder.append(orderQty);
        builder.append(", side=");
        builder.append(side);
        builder.append(", transactTime=");
        builder.append(transactTime);
        builder.append(", senderSubID=");
        builder.append(senderSubID);
        builder.append(", idSource=");
        builder.append(idSource);
        builder.append(", securityExchange=");
        builder.append(securityExchange);
        builder.append("]");
        return builder.toString();
    }
}
