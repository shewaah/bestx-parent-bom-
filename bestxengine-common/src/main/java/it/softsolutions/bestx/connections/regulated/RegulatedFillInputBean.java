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

import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_CONTRACT_NO;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_COUNTERPART;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_LAST_PRICE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_LAST_QUANTITY;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ORDER_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ORDER_TRADER;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_PRICE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SESSION_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_SECURITY_ID_SOURCE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_SETTLEMENT_DATE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_TIMESTAMP;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_TRANSACT_TIME;
import it.softsolutions.bestx.connections.xt2.XT2InputLazyBean;
import it.softsolutions.xt2.protocol.XT2Msg;

import java.math.BigDecimal;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
*
* Purpose: this class is mainly for contains fill data coming from regulated market 
*
* Project Name : bestxengine-common
* First created by: stefano.pontillo
* Creation date: 06/giu/2012
*
**/
public class RegulatedFillInputBean extends XT2InputLazyBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegulatedFillInputBean.class);

	private final String sessionId;
	
	/**
    * Constructor that need the XT2 message coming from the market gateway 
    * and the session id of the transaction to correctly forward the message
    * 
    * @param msg XT2Msg message from the market gateway
    * @param sessionId The session id of the transaction
	 */
	public RegulatedFillInputBean(XT2Msg msg, String sessionId) {
		this.msg = msg;
		this.sessionId = sessionId;
	}
	
	/**
	 * Return the session id of this fill message
	 * 
	 * @return Session ID
	 */
	public String getSessionId() {
		return sessionId;
	}
	
	/**
	 * Return the filled quantity of thi fill, corresponding to the LastQty field 
	 * 
	 * @return BigDecimal quantity filled
	 */
	public BigDecimal getQtyFilled() {
		try {
			return new BigDecimal(msg.getDouble(LABEL_LAST_QUANTITY));
		} catch (Exception e) {
			LOGGER.error("Error while extracting quantity from FILL notification [" + msg.toString() + "]", e);
			return null;
		}
	}
	
	/**
	 * Return  the price of this fill, corresponding to the LastPx field 
	 * 
	 * @return BigDecimal price of the fill
	 */
	public BigDecimal getFillPrice() {	
		BigDecimal lastPrice;
		try {
			lastPrice = new BigDecimal(msg.getDouble(LABEL_LAST_PRICE));
			return lastPrice.setScale(5, BigDecimal.ROUND_HALF_UP);
		} catch (Exception e) {
			LOGGER.error("Error while extracting price (LastPrice) from FILL notification [" + msg.toString() + "]"+" : "+ e.toString(), e);
			return null;
		}
	}

	/**
	 * Return the fill price for ETLX market, corresponding to the Price field
	 * 
	 * @return BigDecimal price of the fill
	 */
	public BigDecimal getPrice() {	
		/* 29-06-2009 Ruggero
		 * I need to extract the Price field because the fill from the ETLX with the price given by
		 * the market to the order (placed on the book) is sent in the Price fix field.
		 */
		BigDecimal price;
		try {
			price = new BigDecimal(msg.getDouble(LABEL_REG_PRICE));
			return price.setScale(5, BigDecimal.ROUND_HALF_UP);
		} catch (Exception e) {
			LOGGER.error("Error while extracting price (Price) from FILL notification [" + msg.toString() + "]"+" : "+ e.toString(), e);
			return null;
		}
	}
	
	/**
    * Return the fill id, corresponding to the CD field
	 * 
	 * @return String the fill id
	 */
	public String getFillId() {
		return msg.getString(LABEL_REG_SESSION_ID);
	}
	
   /**
    * Return the contract number of this fill, corresponding to the ContractNo field
    * 
    * @return String the contract number
    */
	public String getContractNumber () {
		return msg.getString(LABEL_CONTRACT_NO);
	}
	
   /**
    * Return the order trader of this fill, corresponding to the OrderTrader field
    * 
    * @return String the order trader
    */
	public String getOrderTrader() {
		return msg.getString(LABEL_ORDER_TRADER);
	}
	
   /**
    * Return the timestamp of this fill, corresponding to the TimeStamp field
    * 
    * @return Date the timestamp
    */
	public Date getTimeStamp() {
		try {
		   Double d = msg.getDouble(LABEL_TIMESTAMP)*1000; // AMC 20101013 LABEL_TIMESTAMP is set by XT2Server to _GetMillisecs() reurning a double value with milliseconds in the decimal part
		   return new Date(d.longValue());
		} catch (Exception e) {
			LOGGER.error("Error while extracting timestamp from FILL notification [" + msg.toString() + "]"+" : "+ e.toString(), e);
			return null;
		}
	}

   /**
    * Return the security id of this fill, corresponding to the SecurityIDSource field
    * 
    * @return String the security id
    */
	public String getSecurityIdSource() {
		return msg.getString(LABEL_SECURITY_ID_SOURCE);
	}
	
   /**
    * Return the order id of this fill, corresponding to the OrderID field
    * 
    * @return String the order id
    */
	public String getOrderId() {
		return msg.getString(LABEL_ORDER_ID);
	}
	
   /**
    * Return the counterpart code of this fill, corresponding to the ExecBroker field
    * 
    * @return String the counterpart code
    */
	public String getCounterpart(){
		return msg.getString(LABEL_COUNTERPART);
	}
	
   /**
    * Return the settlement date of this fill, corresponding to the SettlmntDate field
    * 
    * @return String the settlement date
    */
	public String getSettlementDate() {
	   return msg.getString(LABEL_SETTLEMENT_DATE);
	}

   /**
    * Return the time of the transaction fill, corresponding to the TransactTime field
    * 
    * @return String the time of the transaction
    */
   public String getTransactTime() {
      return msg.getString(LABEL_TRANSACT_TIME);
   }

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
   @Override
   public String toString(){
      return "FILL id: " + getFillId() +
            " order id: " + getOrderId() +
            " counterpart: " + getCounterpart() +
            " security id source: " + getSecurityIdSource();
   }
}
