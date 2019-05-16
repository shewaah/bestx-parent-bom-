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
package it.softsolutions.bestx.connections.mtsprime;

import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.CRD_DATE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.CRD_FILLED_QTY;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.CRD_TIME;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_CONTRACT_NO;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_COUNTERPARTMEMBER;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ORDER_NUM;
import it.softsolutions.bestx.connections.regulated.RegulatedFillInputBean;
import it.softsolutions.xt2.protocol.XT2Msg;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: this class is mainly for contains fill data coming from MTS Prime market
 * 
 * Project Name : bestxengine-product First created by: stefano.pontillo Creation date: 06/giu/2012
 * 
 **/
public class MTSPrimeFillInputBean extends RegulatedFillInputBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(MTSPrimeFillInputBean.class);

    /**
     * Constructor that need the XT2 message coming from the market gateway and the session id of the transaction to correctly forward the
     * message
     * 
     * @param msg
     *            XT2Msg message from the market gateway
     * @param sessionId
     *            The session id of the transaction
     */
    public MTSPrimeFillInputBean(XT2Msg msg, String sessionId) {
        super(msg, sessionId);
    }

    @Override
    public BigDecimal getQtyFilled() {
        try {
            return new BigDecimal(msg.getDouble(CRD_FILLED_QTY));
        } catch (Exception e) {
            LOGGER.error("Error while extracting quantity from FILL notification [" + msg.toString() + "]", e);
            return null;
        }
    }

    @Override
    public Date getTimeStamp() {
        try {
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMddHHmmssSSS");

            String strTimeStp = "" + msg.getLong(CRD_TIME);
            if (strTimeStp.length() < 9) {
                strTimeStp = "0" + strTimeStp;
            }
            strTimeStp = "" + msg.getLong(CRD_DATE) + strTimeStp;

            return dateFmt.parse(strTimeStp);
        } catch (Exception e) {
            LOGGER.error("Error while extracting timestamp from FILL notification [" + msg.toString() + "]" + " : " + e.toString(), e);
            return null;
        }
    }

    /*
     * The original method extracts the contractNo as a String, testing with MTSPrime returns an empty String. With this market the
     * contractNo is a Long.
     * 
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.regulated.RegulatedFillInputBean#getContractNumber()
     */
    @Override
    public String getContractNumber() {
        Long contractNoLong;
        try {
            contractNoLong = msg.getLong(LABEL_CONTRACT_NO);
        } catch (Exception e) {
            LOGGER.error("Cannot extract " + LABEL_CONTRACT_NO + " as a Long! Trying with a String.");
            String contractNoStr = msg.getString(LABEL_CONTRACT_NO);
            contractNoLong = Long.valueOf(contractNoStr);
        }
        return contractNoLong.toString();
    }

    /*
     * For MTSPrime market this correspond to the CounterpartMember field
     * 
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.regulated.RegulatedFillInputBean#getCounterpart()
     */
    @Override
    public String getCounterpart() {
        return msg.getString(LABEL_COUNTERPARTMEMBER);
    }

    /*
     * For MTSPrime market this correspond to the OrderNum field
     * 
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.regulated.RegulatedFillInputBean#getOrderId()
     */
    public String getOrderId() {
        Long orderNum;
        try {
            orderNum = msg.getLong(LABEL_ORDER_NUM);
        } catch (Exception e) {
            LOGGER.error("Cannot extract " + LABEL_ORDER_NUM + " as a Long! Trying with a String.");
            String orderNumStr = msg.getString(LABEL_ORDER_NUM);
            orderNum = Long.valueOf(orderNumStr);
        }
        return orderNum.toString();
    }
}
