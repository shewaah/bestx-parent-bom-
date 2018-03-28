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
import it.softsolutions.bestx.fix.field.SessionRejectReason;
import quickfix.FieldNotFound;
import quickfix.field.MsgSeqNum;
import quickfix.field.RefTagID;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-fix-client 
 * First created by: davide.rossoni 
 * Creation date: Jun 13, 2012 
 * 
 **/
public class BXReject extends BXMessage<quickfix.fix41.Reject, quickfix.fix42.Reject> {
    
    private Integer refTagID;
    private MsgType refMsgType;
    private SessionRejectReason sessionRejectReason;
    private String text;
    private Integer refSeqNum;
    
    public BXReject() {
        super(MsgType.Reject);
    }

    /**
     * Bind Fix standard message fileds to custom BX message fileds
     * 
     * @param message
     * @return
     * @throws FieldNotFound
     */
    public static BXReject fromFIX42Message(quickfix.fix42.Reject message) throws FieldNotFound {
        BXReject res = new BXReject();

        if (message.getHeader().isSetField(MsgSeqNum.FIELD)) {
            res.setMsgSeqNum(message.getHeader().getInt(MsgSeqNum.FIELD));
        }
        if (message.isSetRefTagID()) {
            res.refTagID = message.getRefTagID().getValue();
        }
        if (message.isSetRefMsgType()) {
            res.refMsgType = MsgType.getInstanceForFIXValue(message.getRefMsgType().getValue());
        }
        if (message.isSetSessionRejectReason()) {
            res.sessionRejectReason = SessionRejectReason.getInstanceForFIXValue(message.getSessionRejectReason().getValue());
        }
        if (message.isSetText()) {
            res.text = message.getText().getValue();
        }
        if (message.isSetRefSeqNum()) {
            res.refSeqNum = message.getRefSeqNum().getValue();
        }
        return res;
    }
    
    public static BXReject fromFIX41Message(quickfix.fix41.Reject message) throws FieldNotFound {
        BXReject res = new BXReject();

        if (message.getHeader().isSetField(MsgSeqNum.FIELD)) {
            res.setMsgSeqNum(message.getHeader().getInt(MsgSeqNum.FIELD));
        }
//        if (message.isSetRefTagID()) {
//            res.refTagID = message.getRefTagID().getValue();
//        }
//        if (message.isSetRefMsgType()) {
//            res.refMsgType = MsgType.getInstanceForFIXValue(message.getRefMsgType().getValue());
//        }
//        if (message.isSetSessionRejectReason()) {
//            res.sessionRejectReason = SessionRejectReason.getInstanceForFIXValue(message.getSessionRejectReason().getValue());
//        }
        if (message.isSetText()) {
            res.text = message.getText().getValue();
        }
        if (message.isSetRefSeqNum()) {
            res.refSeqNum = message.getRefSeqNum().getValue();
        }
        return res;
    }

    /**
     * Build Fix standard message from custom BX message
     */
    @Override
    public quickfix.fix41.Reject toFIX41Message() {
        quickfix.fix41.Reject res = new quickfix.fix41.Reject();

        if (text != null) {
            res.set(new quickfix.field.Text(text));
        }
        if (refSeqNum != null) {
            res.set(new quickfix.field.RefSeqNum(refSeqNum));
        }
        return res;
    }

    @Override
    public quickfix.fix42.Reject toFIX42Message() {
        quickfix.fix42.Reject res = new quickfix.fix42.Reject();

        if (refTagID != null) {
            res.set(new RefTagID(refTagID));
        }

        if (refMsgType != null) {
            res.set(new quickfix.field.RefMsgType(refMsgType.getFIXValue()));
        }
        if (sessionRejectReason != null) {
            res.set(new quickfix.field.SessionRejectReason(sessionRejectReason.getFIXValue()));
        }
        if (text != null) {
            res.set(new quickfix.field.Text(text));
        }
        if (refSeqNum != null) {
            res.set(new quickfix.field.RefSeqNum(refSeqNum));
        }
        return res;
    }

    public Integer getRefTagID() {
        return refTagID;
    }

    public void setRefTagID(Integer refTagID) {
        this.refTagID = refTagID;
    }

    public MsgType getRefMsgType() {
        return refMsgType;
    }

    public void setRefMsgType(MsgType refMsgType) {
        this.refMsgType = refMsgType;
    }

    public SessionRejectReason getSessionRejectReason() {
        return sessionRejectReason;
    }

    public void setSessionRejectReason(SessionRejectReason sessionRejectReason) {
        this.sessionRejectReason = sessionRejectReason;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the refSeqNum
     */
    public Integer getRefSeqNum() {
        return refSeqNum;
    }

    /**
     * @param refSeqNum the refSeqNum to set
     */
    public void setRefSeqNum(Integer refSeqNum) {
        this.refSeqNum = refSeqNum;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BXReject [refTagID=");
        builder.append(refTagID);
        builder.append(", refMsgType=");
        builder.append(refMsgType);
        builder.append(", sessionRejectReason=");
        builder.append(sessionRejectReason);
        builder.append(", text=");
        builder.append(text);
        builder.append(", refSeqNum=");
        builder.append(refSeqNum);
        builder.append("]");
        return builder.toString();
    }
}
