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
package it.softsolutions.bestx.connections.bloomberg;

import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_ASK_PRICE;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_ASK_QTY;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_BID_PRICE;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_BID_QTY;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_CODE_SEPARATOR;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_DATE;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_ERRORCODE_LABEL;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_ERRORMSG_LABEL;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_ISIN_LABEL;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_TIME;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_TIMETS;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.connections.xt2.XT2InputLazyBean;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.xt2.protocol.XT2Msg;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: davide.rossoni Creation date: 11/ott/2012
 * 
 **/
public class BloombergProposalInputLazyBean extends XT2InputLazyBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(BloombergProposalInputLazyBean.class);

    private final ProposalSide proposalSide;
    private final long timeZone;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    /**
	 * 
	 */
    public BloombergProposalInputLazyBean(XT2Msg msg, ProposalSide side, long timeZone) {
        this.msg = msg;
        this.proposalSide = side;
        this.timeZone = timeZone;
    }

    /**
     * @return the proposalSide
     */
    public ProposalSide getProposalSide() {
        return proposalSide;
    }

    public String getBloombergMarketMaker() {
        int keyPos = msg.getSubject().indexOf(BLOOM_CODE_SEPARATOR);
        if (keyPos >= 0) {
            return msg.getSubject().substring(keyPos + 1, msg.getSubject().length());
        }
        return null;
    }

    public BigDecimal getQty() {
        Double qty = null;

        try {
            switch (proposalSide) {
            case ASK:
                qty = (msg.getValue(BLOOM_ASK_QTY) != null) ? msg.getDouble(BLOOM_ASK_QTY) : null;
                break;
            case BID:
                qty = (msg.getValue(BLOOM_BID_QTY) != null) ? msg.getDouble(BLOOM_BID_QTY) : null;
                break;
            default:
                break;
            }
        } catch (Exception e) {
            LOGGER.warn("Error while extracting {} qty from message {}: {}", proposalSide, msg, e.getMessage());
        }

        return qty != null ? new BigDecimal(qty).setScale(5, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getPrice() {
        Double price = null;

        try {
            switch (proposalSide) {
            case ASK:
                price = (msg.getValue(BLOOM_ASK_PRICE) != null) ? msg.getDouble(BLOOM_ASK_PRICE) : null;
                break;
            case BID:
                price = (msg.getValue(BLOOM_BID_PRICE) != null) ? msg.getDouble(BLOOM_BID_PRICE) : null;
                break;
            default:
                break;
            }
        } catch (Exception e) {
            LOGGER.warn("Error while extracting {} price from message {}: {}", proposalSide, msg, e.getMessage());
        }

        return price != null ? new BigDecimal(price).setScale(5, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public Date getTimeStamp() {
        long time = 0;
        long fuso = 0;

        try {
            time = msg.getLong(BLOOM_TIME);
            if (time != 0) {
                fuso = this.timeZone; // For production purpose (add 6 hours to NY time)
            } else {
                time = msg.getLong(BLOOM_TIMETS);
            }
        } catch (Exception e1) {
            time = 0;
        }

        if (time == 0) {
            return DateService.newLocalDate();
        } else {
            try {
                return cDateTime(msg.getLong(BLOOM_DATE), time, fuso);
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * Extract Bloomberg Date field, its format is as in this example : Date=20110809. We return a Long because it is easy to compare with
     * another date in the same format, to find out which one is before or after the other. Furthermore it is a Long in the infobus message.
     * 
     * @return the Date found
     */
    public Long getBloombergDate() {
        Long date = null;
        try {
            date = msg.getLong(BLOOM_DATE);
        } catch (Exception e) {
            if (getErrorCode() == 0)
                LOGGER.error("Error while extracting field {} from message {}: {}", BLOOM_DATE, msg, e.getMessage());
            else {
                LOGGER.info("ErrorCode = {}: field 'date' not available", getErrorCode());
            }
        }
        return date;
    }

    private Date cDateTime(long message_date, long message_time, long fuso) {
        try {
            simpleDateFormat.setLenient(false);
            // the 6:10:12.888 AM will arrive as 61012888, for the parser we need two digits for the
            // hour, here we have only one, so we must check the length of the time and place a 0
            // ahead of the long if needed
            int timeLength = Long.toString(message_time).length();
            Date timeStmp = simpleDateFormat.parse(Long.toString(message_date) + (timeLength == 9 ? "" : "0") + Long.toString(message_time));
            Date localDate = new Date(timeStmp.getTime() + fuso);
            return localDate;
        } catch (Exception e) {
            return null;
        }
    }

    public String getIsin() {
        return msg.getString(BLOOM_ISIN_LABEL);
    }

    public int getErrorCode() {
        try {
            return msg.getInt(BLOOM_ERRORCODE_LABEL);
        } catch (Exception e) {
            return 0;
        }
    }

    public String getErrorMsg() {
        try {
            return msg.getString(BLOOM_ERRORMSG_LABEL);
        } catch (Exception e) {
            return "";
        }
    }
}
