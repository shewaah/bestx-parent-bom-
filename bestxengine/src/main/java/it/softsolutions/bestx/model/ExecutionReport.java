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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;

import it.softsolutions.bestx.model.Commission.CommissionType;
import it.softsolutions.jsscommon.Money;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class ExecutionReport implements Cloneable {

    /**
     * 
     * ExecType FIX 4.2 '0' New '1' Partial fill '2' Fill '3' Done for day '4' Cancelled '5' Replace '6' Pending Cancel '7' Stopped '8'
     * Rejected '9' Suspended 'A' Pending New 'B' Calculated 'C' Expired 'D' Restated 'E' Pending Replace
     * 
     */
    public static enum ExecutionReportState {
        NEW("" + quickfix.field.OrdStatus.NEW), 
        PART_FILL("" + quickfix.field.OrdStatus.PARTIALLY_FILLED), 
        FILLED("" + quickfix.field.OrdStatus.FILLED), 
        DONE_FOR_DAY("" + quickfix.field.OrdStatus.DONE_FOR_DAY), 
        CANCELLED("" + quickfix.field.OrdStatus.CANCELED), 
        REPLACE("" + quickfix.field.OrdStatus.REPLACED), 
        PENDING_CANCEL("" + quickfix.field.OrdStatus.PENDING_CANCEL), 
        STOPPED("" + quickfix.field.OrdStatus.STOPPED), 
        REJECTED("" + quickfix.field.OrdStatus.REJECTED), 
        SUSPENDED("" + quickfix.field.OrdStatus.SUSPENDED), 
        PENDING_NEW("" + quickfix.field.OrdStatus.PENDING_NEW), 
        CLOSURE("" + quickfix.field.OrdStatus.CALCULATED), 
        EXPIRED("" + quickfix.field.OrdStatus.EXPIRED), 
        RESTATED("" + quickfix.field.OrdStatus.ACCEPTED_FOR_BIDDING), 
        PENDING_REPLACE("" + quickfix.field.OrdStatus.PENDING_REPLACE);

        private final String value;

        private ExecutionReportState(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static final String CONTO_PROPRIO = "P";
    public static final String CONTO_TERZI = "T";
    public static final String PROPERTY_SPREAD = "SPREAD";
    public static final String PROPERTY_BEST = "BEST";

    private Long id;
    private Long attemptId;
    private Instrument instrument;
    private Rfq.OrderSide side;
    private Market market;
    private String lastMkt;
    private Money price;
    private BigDecimal orderQty;
    private BigDecimal actualQty;
    private ExecutionReport.ExecutionReportState state;
    private Date transactTime;
    private String sequenceId;
    private String ticket;
    private Money accruedInterestAmount;
    private BigDecimal accruedInterestRate;
    private Integer accruedInterestDays;
    private Date futSettDate;

    private String account;
    private String text;
    private BigDecimal lastPx;
    private BigDecimal averagePrice;
    private BigDecimal commission;
    private CommissionType commissionType;
    private String tipoConto;
    private String execBroker;
    private String counterpart;
    protected String marketOrderID;
    private String property = PROPERTY_SPREAD;

    private BigDecimal remainingQty;
    private Date sendingTime;
    private BigDecimal amountCommission;
    private Integer priceType;
    
    private String execType;
    private String execTransType;
    
    private String executionReportId;
    
    private Character lastCapacity;
    private Character orderCapacity;
    
    // BESTX-385: SP manage the Factor (228) field
    private BigDecimal factor;

    

    public BigDecimal getRemainingQty() {
        return remainingQty;
    }

    public void setRemainingQty(BigDecimal remainingQty) {
        this.remainingQty = remainingQty;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public void setMarket(Market market) {
        this.market = market;
    }

    public Market getMarket() {
        return market;
    }

    public void setPrice(Money price) {
        this.price = price;
    }

    public Money getPrice() {
        return price;
    }

    public void setOrderQty(BigDecimal orderQty) {
        this.orderQty = orderQty;
    }

    public BigDecimal getOrderQty() {
        return orderQty;
    }

    public void setActualQty(BigDecimal actualQty) {
        this.actualQty = actualQty;
    }

    public BigDecimal getActualQty() {
        return actualQty;
    }

    public void setState(ExecutionReport.ExecutionReportState state) {
        this.state = state;
    }

    public ExecutionReport.ExecutionReportState getState() {
        return state;
    }

    public void setTransactTime(Date transactTime) {
        this.transactTime = transactTime;
    }

    public Date getTransactTime() {
        return transactTime;
    }

    public void setSide(Rfq.OrderSide side) {
        this.side = side;
    }

    public Rfq.OrderSide getSide() {
        return side;
    }

    public void setSequenceId(String sequenceId) {
        this.sequenceId = sequenceId;
    }

    public String getSequenceId() {
        return sequenceId;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getTicket() {
        return ticket;
    }

    public Money getAccruedInterestAmount() {
        return accruedInterestAmount;
    }

    public void setAccruedInterestAmount(Money accruedInterestAmount) {
        this.accruedInterestAmount = accruedInterestAmount;
    }

    public BigDecimal getAccruedInterestRate() {
        return accruedInterestRate;
    }

    public void setAccruedInterestRate(BigDecimal accruedInterestRate) {
        this.accruedInterestRate = accruedInterestRate;
    }

    @Override
    public ExecutionReport clone() throws CloneNotSupportedException {
        ExecutionReport cloned = new ExecutionReport();
        copyFields(this, cloned);
        Date transactTime = getTransactTime();
        if (transactTime == null) {
            transactTime = new Date();
        }
        cloned.setTransactTime(transactTime);
        return cloned;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the account
     */
    public String getAccount() {
        return account;
    }

    /**
     * @param account
     *            the account to set
     */
    public void setAccount(String account) {
        this.account = account;
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
     * @return the lastPx
     */
    public BigDecimal getLastPx() {
        return lastPx;
    }

    /**
     * @param lastPx
     *            the lastPx to set
     */
    public void setLastPx(BigDecimal lastPx) {
        this.lastPx = lastPx;
    }

    /**
     * @return the averagePrice
     */
    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    /**
     * @param averagePrice
     *            the averagePrice to set
     */
    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }

    /**
     * @return the commission
     */
    public BigDecimal getCommission() {
        return commission;
    }

    /**
     * @param commission
     *            the commission to set
     */
    public void setCommission(BigDecimal commission) {
        this.commission = commission;
    }

    /**
     * @return the commissionType
     */
    public CommissionType getCommissionType() {
        return commissionType;
    }

    /**
     * @param commissionType
     *            the commissionType to set
     */
    public void setCommissionType(CommissionType commissionType) {
        this.commissionType = commissionType;
    }

    /**
     * @return the tipoConto
     */
    public String getTipoConto() {
        return tipoConto;
    }

    /**
     * @param tipoConto
     *            the tipoConto to set
     */
    public void setTipoConto(String tipoConto) {
        this.tipoConto = tipoConto;
    }

    /**
     * @return the execBroker
     */
    public String getExecBroker() {
        return execBroker;
    }

    /**
     * @param execBroker
     *            the execBroker to set
     */
    public void setExecBroker(String execBroker) {
        this.execBroker = execBroker;
    }

    public Date getFutSettDate() {
        return futSettDate;
    }

    public void setFutSettDate(Date futSettDate) {
        this.futSettDate = futSettDate;
    }

    public Integer getAccruedInterestDays() {
        return accruedInterestDays;
    }

    public void setAccruedInterestDays(Integer accruedInterestDays) {
        this.accruedInterestDays = accruedInterestDays;
    }

    public String getCounterPart() {
        return counterpart;
    }

    public void setCounterPart(String counterpPart) {
        counterpart = counterpPart;
    }

    public String getMarketOrderID() {
        return marketOrderID;
    }

    public void setMarketOrderID(String marketOrderID) {
        this.marketOrderID = marketOrderID;
    }

    public Date getSendingTime() {
        return sendingTime;
    }

    public void setSendingTime(Date sendingTime) {
        this.sendingTime = sendingTime;
    }
        
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ExecutionReport [id=");
        builder.append(id);
        builder.append(", instrument=");
        builder.append(instrument);
        builder.append(", side=");
        builder.append(side);
        builder.append(", market=");
        builder.append(market);
        builder.append(", price=");
        builder.append(price != null ? price.getAmount().toPlainString() : null);
        builder.append(", orderQty=");
        builder.append(orderQty != null ? orderQty.toPlainString() : null);
        builder.append(", actualQty=");
        builder.append(actualQty != null ? actualQty.toPlainString() : null);
        builder.append(", state=");
        builder.append(state);
        builder.append(", transactTime=");
        builder.append(transactTime);
        builder.append(", sequenceId=");
        builder.append(sequenceId);
        builder.append(", ticket=");
        builder.append(ticket);
        builder.append(", accruedInterestAmount=");
        builder.append(accruedInterestAmount != null ? accruedInterestAmount.getAmount().toPlainString() : null);
        builder.append(", accruedInterestRate=");
        builder.append(accruedInterestRate != null ? accruedInterestRate.toPlainString() : null);
        builder.append(", accruedInterestDays=");
        builder.append(accruedInterestDays);
        builder.append(", futSettDate=");
        builder.append(futSettDate);
        builder.append(", account=");
        builder.append(account);
        builder.append(", text=");
        builder.append(text);
        builder.append(", lastPx=");
        builder.append(lastPx != null ? lastPx.toPlainString() : null);
        builder.append(", factor=");
        builder.append(factor != null ? factor.toPlainString() : null);
        builder.append(", averagePrice=");
        builder.append(averagePrice != null ? averagePrice.toPlainString() : null);
        builder.append(", commission=");
        builder.append(commission != null ? commission.toPlainString() : null);
        builder.append(", commissionType=");
        builder.append(commissionType);
        builder.append(", tipoConto=");
        builder.append(tipoConto);
        builder.append(", execBroker=");
        builder.append(execBroker);
        builder.append(", counterpart=");
        builder.append(counterpart);
        builder.append(", marketOrderID=");
        builder.append(marketOrderID);
        builder.append(", property=");
        builder.append(property);
        builder.append(", remainingQty=");
        builder.append(remainingQty != null ? remainingQty.toPlainString() : null);
        builder.append(", sendingTime=");
        builder.append(sendingTime);
        builder.append(", amountCommission=");
        builder.append(amountCommission != null ? amountCommission.toPlainString() : null);
        builder.append(", priceType=");
        builder.append(priceType);
        builder.append("]");
        return builder.toString();
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public void setAmountCommission(BigDecimal amountCommission) {
        this.amountCommission = amountCommission;
    }

    public BigDecimal getAmountCommission() {
        return amountCommission;
    }

    /**
     * @return the priceType
     */
    public Integer getPriceType() {
        return priceType;
    }

    /**
     * @param priceType
     *            the priceType to set
     */
    public void setPriceType(Integer priceType) {
        this.priceType = priceType;
    }

    /**
     * @return the attemptId
     */
    public Long getAttemptId() {
        return attemptId;
    }

    /**
     * @param attemptId the attemptId to set
     */
    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

	public String getExecType() {
		return execType;
	}
	
   public void setExecType(String execType) {
      this.execType = execType;
   }

	public void setExecType(char execType) {
		this.execType = execType+"";
	}

	public String getExecutionReportId() {
		return executionReportId;
	}

	public void setExecutionReportId(String executionReportId) {
		this.executionReportId = executionReportId;
	}
	
	public void copyFields(Object src, Object dest) {
		Field[] fields = ExecutionReport.class.getDeclaredFields();
		for (Field f : fields) {
			f.setAccessible(true);
			copyFieldValue(src, dest, f);
		}
	}

	public void copyFieldValue(Object src, Object dest, Field f) {
	    try {
	        Object value = f.get(src);
	        f.set(dest, value);
	    } catch (ReflectiveOperationException e) {  }
	}

	public String getLastMkt() {
		return lastMkt;
	}

	public void setLastMkt(String lastMkt) {
		this.lastMkt = lastMkt;
	}

   
   public Character getLastCapacity() {
      return lastCapacity;
   }

   
   public void setLastCapacity(Character lastCapacity) {
      this.lastCapacity = lastCapacity;
   }

   
   public Character getOrderCapacity() {
      return orderCapacity;
   }
   
   public void setOrderCapacity(Character orderCapacity) {
      this.orderCapacity = orderCapacity;
   }

public String getExecTransType() {
	return execTransType;
}

public void setExecTransType(String execTransType) {
	this.execTransType = execTransType;
}
   public BigDecimal getFactor() {
      return factor;
   }
   
   public void setFactor(BigDecimal factor) {
      this.factor = factor;
   }

}
