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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.softsolutions.bestx.fix.field.CommType;
import it.softsolutions.bestx.fix.field.ExecTransType;
import it.softsolutions.bestx.fix.field.ExecType;
import it.softsolutions.bestx.fix.field.MsgType;
import it.softsolutions.bestx.fix.field.OrdStatus;
import it.softsolutions.bestx.fix.field.PT_ExecAttmptNo;
import it.softsolutions.bestx.fix.field.PT_ExecAttmptTime;
import it.softsolutions.bestx.fix.field.PT_MultiDealerID;
import it.softsolutions.bestx.fix.field.PriceType;
import it.softsolutions.bestx.fix.field.Side;
import quickfix.FieldNotFound;
import quickfix.Group;
import quickfix.IntField;
import quickfix.field.ClOrdID;
import quickfix.field.ExecBroker;
import quickfix.field.ExecID;
import quickfix.field.LastMkt;
import quickfix.field.MsgSeqNum;
import quickfix.field.OnBehalfOfCompID;
import quickfix.field.OrderID;
import quickfix.field.OrigSendingTime;
import quickfix.field.PossDupFlag;
import quickfix.field.Price;
import quickfix.field.SecurityID;
import quickfix.field.SenderSubID;
import quickfix.field.Symbol;
import quickfix.field.TradeDate;
import quickfix.field.TransactTime;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-fix-client First created by: davide.rossoni Creation date: Jun 11, 2012
 * 
 **/
public class BXExecutionReport extends BXMessage<quickfix.fix41.ExecutionReport, quickfix.fix42.ExecutionReport> {

    private Double price;
    private Double avgPx;
    private String clOrdID;
    private Double cumQty;
    private String execID;
    private ExecTransType execTransType;
    private Double lastPx;
    private Double lastShares;
    private String orderID;
    private Double orderQty;
    private OrdStatus ordStatus;
    private Side side;
    private String symbol;
    private String text;
    private ExecType execType;
    private Double leavesQty;
    private Double commission;
    private CommType commType;
    private PriceType priceType;
    private Date transactTime;
    private String idSource;
    private String securityID;
    private Date origSendingTime;
    private Boolean possDupFlag;
    private String senderSubID;
    private String onBehalfOfCompID;
    private String tradeDate;
    private String execBroker;
    private String lastMkt;
    
    private Integer ptExecAttemptNo;
    private String ptMultiDealerID;
    private Date ptExecAttemptTime;
    
    private List<CSDealer> dealers = new ArrayList<CSDealer>();
    
    public BXExecutionReport() {
        super(MsgType.ExecutionReport);
    }

    @Override
    public quickfix.fix42.ExecutionReport toFIX42Message() {
        quickfix.fix42.ExecutionReport res = new quickfix.fix42.ExecutionReport();

        if (avgPx != null) {
            res.set(new quickfix.field.AvgPx(avgPx));
        }
        if (clOrdID != null) {
            res.set(new ClOrdID(clOrdID));
        }
        if (cumQty != null) {
            res.set(new quickfix.field.CumQty(cumQty));
        }
        if (execID != null) {
            res.set(new ExecID(execID));
        }
        if (execTransType != null) {
            res.set(new quickfix.field.ExecTransType(execTransType.getFIXValue()));
        }
        if (lastPx != null) {
            res.set(new quickfix.field.LastPx(lastPx));
        }
        if (lastShares != null) {
            res.set(new quickfix.field.LastShares(lastShares));
        }
        if (orderID != null) {
            res.set(new OrderID(orderID));
        }
        if (orderQty != null) {
            res.set(new quickfix.field.OrderQty(orderQty));
        }
        if (ordStatus != null) {
            res.set(new quickfix.field.OrdStatus(ordStatus.getFIXValue()));
        }
        if (side != null) {
            res.set(new quickfix.field.Side(side.getFIXValue()));
        }
        if (symbol != null) {
            res.set(new Symbol(symbol));
        }
        if (text != null) {
            res.set(new quickfix.field.Text(text));
        }
        if (execType != null) {
            res.set(new quickfix.field.ExecType(execType.getFIXValue()));
        }
        if (leavesQty != null) {
            res.set(new quickfix.field.LeavesQty(leavesQty));
        }
        if (commission != null) {
            res.set(new quickfix.field.Commission(commission));
        }
        if (commType != null) {
            res.set(new quickfix.field.CommType(commType.getFIXValue()));
        }
        if (idSource != null) {
            res.set(new quickfix.field.IDSource(idSource));
        }
        if (price != null) {
            res.set(new Price(price));
        }
        if (getSecurityID() != null) {
            res.set(new SecurityID(getSecurityID()));
        }
        if (transactTime != null) {
            res.set(new TransactTime(transactTime));
        }
        if (possDupFlag != null) {
            res.getHeader().setBoolean(PossDupFlag.FIELD, possDupFlag);
        }
        if (getOrigSendingTime() != null) {
            res.getHeader().setUtcTimeStamp(OrigSendingTime.FIELD, getOrigSendingTime());
        }
        if (senderSubID != null) {
            res.getHeader().setString(SenderSubID.FIELD, senderSubID);
        }
        if (onBehalfOfCompID != null) {
            res.getHeader().setString(OnBehalfOfCompID.FIELD, onBehalfOfCompID);
        }
        if (tradeDate != null) {
            res.set(new TradeDate(tradeDate));
        }
        if (execBroker != null) {
            res.set(new ExecBroker(execBroker));
        }
        if (lastMkt != null) {
            res.set(new LastMkt(lastMkt));
        }
        
        return res;
    }

    @Override
    public quickfix.fix41.ExecutionReport toFIX41Message() {
        quickfix.fix41.ExecutionReport res = new quickfix.fix41.ExecutionReport();

        if (avgPx != null) {
            res.set(new quickfix.field.AvgPx(avgPx));
        }
        if (clOrdID != null) {
            res.set(new ClOrdID(clOrdID));
        }
        if (cumQty != null) {
            res.set(new quickfix.field.CumQty(cumQty));
        }
        if (execID != null) {
            res.set(new ExecID(execID));
        }
        if (execTransType != null) {
            res.set(new quickfix.field.ExecTransType(execTransType.getFIXValue()));
        }
        if (lastPx != null) {
            res.set(new quickfix.field.LastPx(lastPx));
        }
        if (lastShares != null) {
            res.set(new quickfix.field.LastShares(lastShares));
        }
        if (orderID != null) {
            res.set(new OrderID(orderID));
        }
        if (orderQty != null) {
            res.set(new quickfix.field.OrderQty(orderQty));
        }
        if (ordStatus != null) {
            res.set(new quickfix.field.OrdStatus(ordStatus.getFIXValue()));
        }
        if (side != null) {
            res.set(new quickfix.field.Side(side.getFIXValue()));
        }
        if (symbol != null) {
            res.set(new Symbol(symbol));
        }
        if (text != null) {
            res.set(new quickfix.field.Text(text));
        }
        if (execType != null) {
            res.set(new quickfix.field.ExecType(execType.getFIXValue()));
        }
        if (leavesQty != null) {
            res.set(new quickfix.field.LeavesQty(leavesQty));
        }
        if (commission != null) {
            res.set(new quickfix.field.Commission(commission));
        }
        if (commType != null) {
            res.set(new quickfix.field.CommType(commType.getFIXValue()));
        }
        if (idSource != null) {
            res.set(new quickfix.field.IDSource(idSource));
        }
        if (price != null) {
            res.set(new Price(price));
        }
        if (getSecurityID() != null) {
            res.set(new SecurityID(getSecurityID()));
        }
        if (transactTime != null) {
            res.set(new TransactTime(transactTime));
        }
        if (possDupFlag != null) {
            res.getHeader().setBoolean(PossDupFlag.FIELD, possDupFlag);
        }
        if (getOrigSendingTime() != null) {
            res.getHeader().setUtcTimeStamp(OrigSendingTime.FIELD, getOrigSendingTime());
        }
        if (getSenderSubID() != null) {
            res.getHeader().setString(SenderSubID.FIELD, getSenderSubID());
        }
        if (onBehalfOfCompID != null) {
            res.getHeader().setString(OnBehalfOfCompID.FIELD, onBehalfOfCompID);
        }
        if (tradeDate != null) {
            res.set(new TradeDate(tradeDate));
        }
        if (execBroker != null) {
            res.set(new ExecBroker(execBroker));
        }
        if (lastMkt != null) {
            res.set(new LastMkt(lastMkt));
        }

        return res;
    }

    public static BXExecutionReport fromFIX41Message(quickfix.fix41.ExecutionReport message) throws FieldNotFound {
        BXExecutionReport res = new BXExecutionReport();

        if (message.getHeader().isSetField(MsgSeqNum.FIELD)) {
            res.setMsgSeqNum(message.getHeader().getInt(MsgSeqNum.FIELD));
        }
        if (message.isSetAvgPx()) {
            res.avgPx = message.getAvgPx().getValue();
        }
        if (message.isSetClOrdID()) {
            res.clOrdID = message.getClOrdID().getValue();
        }
        if (message.isSetCumQty()) {
            res.cumQty = message.getCumQty().getValue();
        }
        if (message.isSetExecID()) {
            res.execID = message.getExecID().getValue();
        }
        if (message.isSetExecTransType()) {
            res.execTransType = ExecTransType.getInstanceForFIXValue(message.getExecTransType().getValue());
        }
        if (message.isSetLastPx()) {
            res.lastPx = message.getLastPx().getValue();
        }
        if (message.isSetLastShares()) {
            res.lastShares = message.getLastShares().getValue();
        }
        if (message.isSetOrderID()) {
            res.orderID = message.getOrderID().getValue();
        }
        if (message.isSetOrderQty()) {
            res.orderQty = message.getOrderQty().getValue();
        }
        if (message.isSetOrdStatus()) {
            res.ordStatus = OrdStatus.getInstanceForFIXValue(message.getOrdStatus().getValue());
        }
        if (message.isSetSide()) {
            res.side = Side.getInstanceForFIXValue(message.getSide().getValue());
        }
        if (message.isSetSymbol()) {
            res.symbol = message.getSymbol().getValue();
        }
        if (message.isSetText()) {
            res.text = message.getText().getValue();
        }
        if (message.isSetExecType()) {
            res.execType = ExecType.getInstanceForFIXValue(message.getExecType().getValue());
        }
        if (message.isSetLeavesQty()) {
            res.leavesQty = message.getLeavesQty().getValue();
        }
        if (message.isSetCommission()) {
            res.commission = message.getCommission().getValue();
        }
        if (message.isSetCommType()) {
            res.commType = CommType.getInstanceForFIXValue(message.getCommType().getValue());
        }
        if (message.isSetField(423)) {
            res.priceType = PriceType.getInstanceForFIXValue(message.getField(new IntField(423)).getValue());
        }
        if (message.isSetIDSource()) {
            res.idSource = message.getIDSource().getValue();
        }
        if (message.isSetPrice()) {
            res.price = message.getPrice().getValue();
        }
        if (message.isSetSecurityID()) {
            res.setSecurityID(message.getSecurityID().getValue());
        }
        if (message.isSetTransactTime()) {
            res.transactTime = message.getTransactTime().getValue();
        }
        if (message.getHeader().isSetField(PossDupFlag.FIELD)) {
            res.possDupFlag = message.getHeader().getBoolean(PossDupFlag.FIELD);
        }
        if (message.getHeader().isSetField(OrigSendingTime.FIELD)) {
            res.setOrigSendingTime(message.getHeader().getUtcTimeStamp(PossDupFlag.FIELD));
        }
        if (message.getHeader().isSetField(SenderSubID.FIELD)) {
            res.setSenderSubID(message.getHeader().getString(SenderSubID.FIELD));
        }
        if (message.getHeader().isSetField(OnBehalfOfCompID.FIELD)) {
            res.setOnBehalfOfCompID(message.getHeader().getString(OnBehalfOfCompID.FIELD));
        }
        if (message.isSetTradeDate()) {
            res.setTradeDate(message.getTradeDate().getValue());
        }
        if (message.isSetExecBroker()) {
            res.setExecBroker(message.getExecBroker().getValue());
        }
        if (message.isSetLastMkt()) {
            res.setLastMkt(message.getLastMkt().getValue());
        }
        
        
        return res;
    }

    public static BXExecutionReport fromFIX42Message(quickfix.fix42.ExecutionReport message) throws FieldNotFound {
        BXExecutionReport res = new BXExecutionReport();

        if (message.getHeader().isSetField(MsgSeqNum.FIELD)) {
            res.setMsgSeqNum(message.getHeader().getInt(MsgSeqNum.FIELD));
        }
        if (message.isSetAvgPx()) {
            res.avgPx = message.getAvgPx().getValue();
        }
        if (message.isSetClOrdID()) {
            res.clOrdID = message.getClOrdID().getValue();
        }
        if (message.isSetCumQty()) {
            res.cumQty = message.getCumQty().getValue();
        }
        if (message.isSetExecID()) {
            res.execID = message.getExecID().getValue();
        }
        if (message.isSetExecTransType()) {
            res.execTransType = ExecTransType.getInstanceForFIXValue(message.getExecTransType().getValue());
        }
        if (message.isSetLastPx()) {
            res.lastPx = message.getLastPx().getValue();
        }
        if (message.isSetLastShares()) {
            res.lastShares = message.getLastShares().getValue();
        }
        if (message.isSetOrderID()) {
            res.orderID = message.getOrderID().getValue();
        }
        if (message.isSetOrderQty()) {
            res.orderQty = message.getOrderQty().getValue();
        }
        if (message.isSetOrdStatus()) {
            res.ordStatus = OrdStatus.getInstanceForFIXValue(message.getOrdStatus().getValue());
        }
        if (message.isSetSide()) {
            res.side = Side.getInstanceForFIXValue(message.getSide().getValue());
        }
        if (message.isSetSymbol()) {
            res.symbol = message.getSymbol().getValue();
        }
        if (message.isSetText()) {
            res.text = message.getText().getValue();
        }
        if (message.isSetExecType()) {
            res.execType = ExecType.getInstanceForFIXValue(message.getExecType().getValue());
        }
        if (message.isSetLeavesQty()) {
            res.leavesQty = message.getLeavesQty().getValue();
        }
        if (message.isSetCommission()) {
            res.commission = message.getCommission().getValue();
        }
        if (message.isSetCommType()) {
            res.commType = CommType.getInstanceForFIXValue(message.getCommType().getValue());
        }
        if (message.isSetField(423)) {
            res.priceType = PriceType.getInstanceForFIXValue(message.getField(new IntField(423)).getValue());
        }
        if (message.isSetIDSource()) {
            res.idSource = message.getIDSource().getValue();
        }
        if (message.isSetPrice()) {
            res.price = message.getPrice().getValue();
        }
        if (message.isSetSecurityID()) {
            res.setSecurityID(message.getSecurityID().getValue());
        }
        if (message.isSetTransactTime()) {
            res.transactTime = message.getTransactTime().getValue();
        }
        if (message.getHeader().isSetField(PossDupFlag.FIELD)) {
            res.possDupFlag = message.getHeader().getBoolean(PossDupFlag.FIELD);
        }
        if (message.getHeader().isSetField(OrigSendingTime.FIELD)) {
            res.setOrigSendingTime(message.getHeader().getUtcTimeStamp(PossDupFlag.FIELD));
        }
        if (message.getHeader().isSetField(SenderSubID.FIELD)) {
            res.setSenderSubID(message.getHeader().getString(SenderSubID.FIELD));
        }
        if (message.getHeader().isSetField(OnBehalfOfCompID.FIELD)) {
            res.setOnBehalfOfCompID(message.getHeader().getString(OnBehalfOfCompID.FIELD));
        }
        if (message.isSetTradeDate()) {
            res.setTradeDate(message.getTradeDate().getValue());
        }
        if (message.isSetExecBroker()) {
            res.setExecBroker(message.getExecBroker().getValue());
        }
        if (message.isSetLastMkt()) {
            res.setLastMkt(message.getLastMkt().getValue());
        }
        
        if (message.isSetField(PT_ExecAttmptTime.FIELD)) {
        	res.setPtExecAttemptTime(message.getField(new PT_ExecAttmptTime()).getValue());
        }
        
        if (message.isSetField(PT_ExecAttmptNo.FIELD)) {
        	res.setPtExecAttemptNo(message.getField(new PT_ExecAttmptNo()).getValue());
        }
        
        if (message.isSetField(PT_MultiDealerID.FIELD)) {
        	res.setPtMultiDealerID(message.getField(new PT_MultiDealerID()).getValue());
        }
        
        
        try {
        	int noDealers = 0;
        	if (message.isSetField(9690)) {
        		noDealers = message.getInt(9690);
        	}
        	for (int i = 1; i<=noDealers; i++) {
        		Group dealerGrp = message.getGroup(i, new PT_NoDealers());
        		res.addDealers(new CSDealer(dealerGrp));
        	}
        	
        } catch (Exception e) {
        }
        
        return res;
    }

    private void setPtMultiDealerID(String ptMultiDealerID) {
		this.ptMultiDealerID = ptMultiDealerID;
	}

	private void setPtExecAttemptNo(int ptExecAttemptNo) {
		this.ptExecAttemptNo = ptExecAttemptNo;
	}

	private void setPtExecAttemptTime(Date ptExecAttemptTime) {
		this.ptExecAttemptTime = ptExecAttemptTime;
	}
	
	public Integer getPtExecAttemptNo() {
		return ptExecAttemptNo;
	}
	
	public Date getPtExecAttemptTime() {
		return ptExecAttemptTime;
	}
	
	public String getPtMultiDealerID() {
		return ptMultiDealerID;
	}

	public void addDealers(CSDealer dealer) {
    	this.dealers.add(dealer);
    }

    public Double getAvgPx() {
        return avgPx;
    }

    public void setAvgPx(Double avgPx) {
        this.avgPx = avgPx;
    }

    public String getClOrdID() {
        return clOrdID;
    }

    public void setClOrdID(String clOrdID) {
        this.clOrdID = clOrdID;
    }

    public Double getCumQty() {
        return cumQty;
    }

    public void setCumQty(Double cumQty) {
        this.cumQty = cumQty;
    }

    public String getExecID() {
        return execID;
    }

    public void setExecID(String execID) {
        this.execID = execID;
    }

    public ExecTransType getExecTransType() {
        return execTransType;
    }

    public void setExecTransType(ExecTransType execTransType) {
        this.execTransType = execTransType;
    }

    public Double getLastPx() {
        return lastPx;
    }

    public void setLastPx(Double lastPx) {
        this.lastPx = lastPx;
    }

    public Double getLastShares() {
        return lastShares;
    }

    public void setLastShares(Double lastShares) {
        this.lastShares = lastShares;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public Double getOrderQty() {
        return orderQty;
    }

    public void setOrderQty(Double orderQty) {
        this.orderQty = orderQty;
    }

    public OrdStatus getOrdStatus() {
        return ordStatus;
    }

    public void setOrdStatus(OrdStatus ordStatus) {
        this.ordStatus = ordStatus;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ExecType getExecType() {
        return execType;
    }

    public void setExecType(ExecType execType) {
        this.execType = execType;
    }

    public Double getLeavesQty() {
        return leavesQty;
    }

    public void setLeavesQty(Double leavesQty) {
        this.leavesQty = leavesQty;
    }

    public Double getCommission() {
        return commission;
    }

    public void setCommission(Double commission) {
        this.commission = commission;
    }

    public CommType getCommType() {
        return commType;
    }

    public void setCommType(CommType commType) {
        this.commType = commType;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public PriceType getPriceType() {
        return priceType;
    }

    public void setPriceType(PriceType priceType) {
        this.priceType = priceType;
    }

    public Date getTransactTime() {
        return transactTime;
    }

    public void setTransactTime(Date transactTime) {
        this.transactTime = transactTime;
    }

    /**
     * @return the idSource
     */
    public String getIdSource() {
        return idSource;
    }

    /**
     * @param idSource
     *            the idSource to set
     */
    public void setIdSource(String idSource) {
        this.idSource = idSource;
    }

    /**
     * @return the possDupFlag
     */
    public Boolean getPossDupFlag() {
        return possDupFlag;
    }

    /**
     * @param possDupFlag
     *            the possDupFlag to set
     */
    public void setPossDupFlag(Boolean possDupFlag) {
        this.possDupFlag = possDupFlag;
    }

    /**
     * @return the onBehalfOfCompID
     */
    public String getOnBehalfOfCompID() {
        return onBehalfOfCompID;
    }

    /**
     * @param onBehalfOfCompID the onBehalfOfCompID to set
     */
    public void setOnBehalfOfCompID(String onBehalfOfCompID) {
        this.onBehalfOfCompID = onBehalfOfCompID;
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

    /**
     * @return the tradeDate
     */
    public String getTradeDate() {
        return tradeDate;
    }

    /**
     * @param tradeDate the tradeDate to set
     */
    public void setTradeDate(String tradeDate) {
        this.tradeDate = tradeDate;
    }

    /**
     * @return the execBroker
     */
    public String getExecBroker() {
        return execBroker;
    }

    /**
     * @param execBroker the tradeDate to set
     */
    public void setExecBroker(String execBroker) {
        this.execBroker = execBroker;
    }

    /**
     * @return the lastMkt
     */
    public String getLastMkt() {
        return lastMkt;
    }

    /**
     * @param lastMkt the lastMkt to set
     */
    public void setLastMkt(String lastMkt) {
        this.lastMkt = lastMkt;
    }
    
    /**
     * @return the securityID
     */
    public String getSecurityID() {
        return securityID;
    }

    /**
     * @param securityID the securityID to set
     */
    public void setSecurityID(String securityID) {
        this.securityID = securityID;
    }

    /**
     * @return the origSendingTime
     */
    public Date getOrigSendingTime() {
        return origSendingTime;
    }

    /**
     * @param origSendingTime the origSendingTime to set
     */
    public void setOrigSendingTime(Date origSendingTime) {
        this.origSendingTime = origSendingTime;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BXExecutionReport [price=");
        builder.append(price);
        builder.append(", avgPx=");
        builder.append(avgPx);
        builder.append(", clOrdID=");
        builder.append(clOrdID);
        builder.append(", cumQty=");
        builder.append(cumQty);
        builder.append(", execID=");
        builder.append(execID);
        builder.append(", execTransType=");
        builder.append(execTransType);
        builder.append(", lastPx=");
        builder.append(lastPx);
        builder.append(", lastShares=");
        builder.append(lastShares);
        builder.append(", orderID=");
        builder.append(orderID);
        builder.append(", orderQty=");
        builder.append(orderQty);
        builder.append(", ordStatus=");
        builder.append(ordStatus);
        builder.append(", side=");
        builder.append(side);
        builder.append(", symbol=");
        builder.append(symbol);
        builder.append(", text=");
        builder.append(text);
        builder.append(", execType=");
        builder.append(execType);
        builder.append(", leavesQty=");
        builder.append(leavesQty);
        builder.append(", commission=");
        builder.append(commission);
        builder.append(", commType=");
        builder.append(commType);
        builder.append(", priceType=");
        builder.append(priceType);
        builder.append(", transactTime=");
        builder.append(transactTime);
        builder.append(", idSource=");
        builder.append(idSource);
        builder.append(", securityID=");
        builder.append(securityID);
        builder.append(", origSendingTime=");
        builder.append(getOrigSendingTime());
        builder.append(", possDupFlag=");
        builder.append(possDupFlag);
        builder.append(", senderSubID=");
        builder.append(senderSubID);
        builder.append(", onBehalfOfCompID=");
        builder.append(onBehalfOfCompID);
        builder.append(", tradeDate=");
        builder.append(tradeDate);
        builder.append("]");
        return builder.toString();
    }
    
    public List<CSDealer> getDealers() {
		return dealers;
	}
    
}
