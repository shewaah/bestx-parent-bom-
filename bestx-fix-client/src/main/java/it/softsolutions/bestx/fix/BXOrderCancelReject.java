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

import it.softsolutions.bestx.fix.field.CxlRejResponseTo;
import it.softsolutions.bestx.fix.field.MsgType;
import it.softsolutions.bestx.fix.field.OrdStatus;
import quickfix.FieldNotFound;
import quickfix.field.ClOrdID;
import quickfix.field.MsgSeqNum;
import quickfix.field.OrderID;
import quickfix.field.OrigClOrdID;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-fix-client 
 * First created by: davide.rossoni 
 * Creation date: Jun 12, 2012 
 * 
 **/
public class BXOrderCancelReject extends BXMessage<quickfix.fix41.OrderCancelReject, quickfix.fix42.OrderCancelReject> {

    private String clOrdID;
    private String orderID;
    private String origClOrdID;
    private OrdStatus ordStatus;
    private CxlRejResponseTo cxlRejResponseTo;
    private String text;
    
    public BXOrderCancelReject() {
        super(MsgType.OrderCancelReject);
    }

    /**
     * Bind Fix standard message fileds to custom BX message fileds
     * 
     * @param message
     * @return
     * @throws FieldNotFound
     */
    public static BXOrderCancelReject fromFIX42Message(quickfix.fix42.OrderCancelReject message) throws FieldNotFound {
        BXOrderCancelReject res = new BXOrderCancelReject();

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
        if (message.isSetOrdStatus()) {
            res.ordStatus = OrdStatus.getInstanceForFIXValue(message.getOrdStatus().getValue());
        }
        if (message.isSetCxlRejResponseTo()) {
            res.cxlRejResponseTo = CxlRejResponseTo.getInstanceForFIXValue(message.getCxlRejResponseTo().getValue());
        }
        if (message.isSetText()) {
            res.text = message.getText().getValue();
        }

        return res;
    }
    
    /**
     * Bind Fix standard message fileds to custom BX message fileds
     * 
     * @param message
     * @return
     * @throws FieldNotFound
     */
    public static BXOrderCancelReject fromFIX41Message(quickfix.fix41.OrderCancelReject message) throws FieldNotFound {
        BXOrderCancelReject res = new BXOrderCancelReject();

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
        if (message.isSetOrdStatus()) {
            res.ordStatus = OrdStatus.getInstanceForFIXValue(message.getOrdStatus().getValue());
        }
//        if (message.isSetCxlRejResponseTo()) {
//            res.cxlRejResponseTo = CxlRejResponseTo.getInstanceForFIXValue(message.getCxlRejResponseTo().getValue());
//        }
        if (message.isSetText()) {
            res.text = message.getText().getValue();
        }

        return res;
    }

    /**
     * Build Fix standard message from custom BX message
     */

    @Override
    public quickfix.fix41.OrderCancelReject toFIX41Message() {
        quickfix.fix41.OrderCancelReject res = new quickfix.fix41.OrderCancelReject();

        if (clOrdID != null) {
            res.set(new ClOrdID(clOrdID));
        }
        if (orderID != null) {
            res.set(new OrderID(orderID));
        }
        if (origClOrdID != null) {
            res.set(new OrigClOrdID(origClOrdID));
        }
        if (ordStatus != null) {
            res.set(new quickfix.field.OrdStatus(ordStatus.getFIXValue()));
        }
//        if (cxlRejResponseTo != null) {
//            res.set(new quickfix.field.CxlRejResponseTo(cxlRejResponseTo.getFIXValue()));
//        }
        if (text != null) {
            res.set(new quickfix.field.Text(text));
        }

        return res;
    }

    @Override
    public quickfix.fix42.OrderCancelReject toFIX42Message() {
        quickfix.fix42.OrderCancelReject res = new quickfix.fix42.OrderCancelReject();

        if (clOrdID != null) {
            res.set(new ClOrdID(clOrdID));
        }
        if (orderID != null) {
            res.set(new OrderID(orderID));
        }
        if (origClOrdID != null) {
            res.set(new OrigClOrdID(origClOrdID));
        }
        if (ordStatus != null) {
            res.set(new quickfix.field.OrdStatus(ordStatus.getFIXValue()));
        }
        if (cxlRejResponseTo != null) {
            res.set(new quickfix.field.CxlRejResponseTo(cxlRejResponseTo.getFIXValue()));
        }
        if (text != null) {
            res.set(new quickfix.field.Text(text));
        }

        return res;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BXOrderCancelReject [clOrdID=");
        builder.append(clOrdID);
        builder.append(", orderID=");
        builder.append(orderID);
        builder.append(", origClOrdID=");
        builder.append(origClOrdID);
        builder.append(", ordStatus=");
        builder.append(ordStatus);
        builder.append(", cxlRejResponseTo=");
        builder.append(cxlRejResponseTo);
        builder.append(", text=");
        builder.append(text);
        builder.append("]");
        return builder.toString();
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

    public OrdStatus getOrdStatus() {
        return ordStatus;
    }

    public void setOrdStatus(OrdStatus ordStatus) {
        this.ordStatus = ordStatus;
    }

    public CxlRejResponseTo getCxlRejResponseTo() {
        return cxlRejResponseTo;
    }

    public void setCxlRejResponseTo(CxlRejResponseTo cxlRejResponseTo) {
        this.cxlRejResponseTo = cxlRejResponseTo;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
    
}
