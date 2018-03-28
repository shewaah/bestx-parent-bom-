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
import quickfix.FieldNotFound;
import quickfix.UnsupportedMessageType;
import quickfix.field.BusinessRejectReason;
import quickfix.field.BusinessRejectRefID;
import quickfix.field.MsgSeqNum;
import quickfix.field.RefMsgType;
import quickfix.field.RefSeqNum;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-fix-client First created by: davide.rossoni Creation date: Jun 12, 2012
 * 
 **/
public class BXBusinessMessageReject extends BXMessage<quickfix.fix41.Message, quickfix.fix42.BusinessMessageReject> {

    private BusinessRejectReason businessRejectReason;
    private String text;
    private RefSeqNum refSeqNum;
    private RefMsgType refMsgType;
    private BusinessRejectRefID businessRejectRefID;

    public BXBusinessMessageReject() {
        super(MsgType.BusinessMessageReject);
    }

    /**
     * Bind Fix standard message fileds to custom BX message fileds
     * 
     * @param message
     * @return
     * @throws FieldNotFound
     */
    public static BXBusinessMessageReject fromFIX42Message(quickfix.fix42.BusinessMessageReject message) throws FieldNotFound {
        BXBusinessMessageReject res = new BXBusinessMessageReject();

        if (message.getHeader().isSetField(MsgSeqNum.FIELD)) {
            res.setMsgSeqNum(message.getHeader().getInt(MsgSeqNum.FIELD));
        }
        if (message.isSetBusinessRejectReason()) {
            res.setBusinessRejectReason(message.getBusinessRejectReason());
        }
        if (message.isSetRefMsgType()) {
            res.setRefMsgType(message.getRefMsgType());
        }
        if (message.isSetRefSeqNum()) {
            res.setRefSeqNum(message.getRefSeqNum());
        }
        if (message.isSetBusinessRejectRefID()) {
            res.setBusinessRejectRefID(message.getBusinessRejectRefID());
        }
        if (message.isSetText()) {
            res.text = message.getText().getValue();
        }

        return res;
    }

    /**
     * BusinessReject not implemented in Fix4.1, it has been introduced in the 4.2 version.
     * 
     * @param message
     *            unexpected BusinessReject
     * @return nothing
     * @throws FieldNotFound
     * @throws UnsupportedMessageType
     */

    public static BXBusinessMessageReject fromFIX41Message(quickfix.fix41.Message message) throws FieldNotFound, UnsupportedMessageType {
        throw new UnsupportedMessageType();
    }

    /**
     * Build Fix standard message from custom BX message
     */

    @Override
    public quickfix.fix41.Message toFIX41Message() {
        return null;
    }

    @Override
    public quickfix.fix42.BusinessMessageReject toFIX42Message() {
        quickfix.fix42.BusinessMessageReject res = new quickfix.fix42.BusinessMessageReject();
        if (refSeqNum != null) {
            res.set(new quickfix.field.RefSeqNum(refSeqNum.getValue()));
        }
        if (refMsgType != null) {
            res.set(new quickfix.field.RefMsgType(refMsgType.getValue()));
        }
        if (businessRejectRefID != null) {
            res.set(new quickfix.field.BusinessRejectRefID(businessRejectRefID.getValue()));
        }
        if (businessRejectReason != null) {
            res.set(new quickfix.field.BusinessRejectReason(businessRejectReason.getValue()));
        }
        if (text != null) {
            res.set(new quickfix.field.Text(text));
        }

        return res;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the businessRejectReason
     */
    public BusinessRejectReason getBusinessRejectReason() {
        return businessRejectReason;
    }

    /**
     * @param businessRejectReason
     *            the businessRejectReason to set
     */
    public void setBusinessRejectReason(BusinessRejectReason businessRejectReason) {
        this.businessRejectReason = businessRejectReason;
    }

    /**
     * @return the refSeqNum
     */
    public RefSeqNum getRefSeqNum() {
        return refSeqNum;
    }

    /**
     * @param refSeqNum
     *            the refSeqNum to set
     */
    public void setRefSeqNum(RefSeqNum refSeqNum) {
        this.refSeqNum = refSeqNum;
    }

    /**
     * @return the refMsgType
     */
    public RefMsgType getRefMsgType() {
        return refMsgType;
    }

    /**
     * @param refMsgType
     *            the refMsgType to set
     */
    public void setRefMsgType(RefMsgType refMsgType) {
        this.refMsgType = refMsgType;
    }

    /**
     * @return the businessRejectRefID
     */
    public BusinessRejectRefID getBusinessRejectRefID() {
        return businessRejectRefID;
    }

    /**
     * @param businessRejectRefID
     *            the businessRejectRefID to set
     */
    public void setBusinessRejectRefID(BusinessRejectRefID businessRejectRefID) {
        this.businessRejectRefID = businessRejectRefID;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BXBusinessMessageReject [businessRejectReason=");
        builder.append(businessRejectReason);
        builder.append(", text=");
        builder.append(text);
        builder.append(", refSeqNum=");
        builder.append(refSeqNum);
        builder.append(", refMsgType=");
        builder.append(refMsgType);
        builder.append(", businessRejectRefID=");
        builder.append(businessRejectRefID);
        builder.append("]");
        return builder.toString();
    }
}
