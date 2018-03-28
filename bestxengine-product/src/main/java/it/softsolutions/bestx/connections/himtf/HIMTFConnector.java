/*
 * Project Name : BestXEngine_common
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author: anna.cochetti $
 * Date         : $Date: 2010-02-18 10:29:44 $
 * Header       : $Id: HIMTFConnector.java,v 1.3 2010-02-18 10:29:44 anna.cochetti Exp $
 * Revision     : $Revision: 1.3 $
 * Source       : $Source: /root/scripts/BestXEngine_common/src/it/softsolutions/bestx/connections/himtf/HIMTFConnector.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_common
 */
package it.softsolutions.bestx.connections.himtf;

import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ERROR_CODE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ERROR_MESSAGE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SESSION_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_TEXT;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REJECT_REASON;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.NAME_PRICE_RESPONSE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.SUBJECT_REG_ORDER_RESP;
import it.softsolutions.bestx.connections.regulated.RegulatedConnector;
import it.softsolutions.bestx.connections.regulated.RegulatedMessageFields;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.xt2.protocol.XT2Msg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HIMTFConnector extends RegulatedConnector {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HIMTFConnector.class);

    @Override
    public void onReply(XT2Msg msg) {
        if (NAME_PRICE_RESPONSE.equalsIgnoreCase(msg.getName())) {
            try {
                String regSessionId = msg.getString(LABEL_REG_SESSION_ID);
                if (msg.getInt(LABEL_ERROR_CODE) != 0) {
                    LOGGER.info("Negative reply result received from module: " + serviceName + " - msg: " + msg);
                    String baseRegSessionId;
                    if (regSessionId.startsWith("DPB") || regSessionId.startsWith("DPS"))
                        baseRegSessionId = regSessionId.substring(3);
                    else {
                        LOGGER.error("Error while retrieving data from Market price notification. Invalid session ID [" + msg.toString() + "]");
                        return;
                    }
                    ProposalSide side = ProposalSide.BID;
                    if (regSessionId.startsWith("DPS")) {
                        side = ProposalSide.ASK;
                    }
                    String reason = msg.getString(LABEL_ERROR_MESSAGE);
                    listener.onNullPrices(baseRegSessionId, reason, side);
                }
            } catch (Exception e) {
                LOGGER.info("Impossible to get result from reply received from module: " + serviceName + " - msg: " + msg);
            }
        } else if (SUBJECT_REG_ORDER_RESP.equalsIgnoreCase(msg.getName())) {
            try {
                if (msg.getInt(LABEL_ERROR_CODE) != 0) {
                    LOGGER.info("Negative reply result received from module: " + serviceName + " - msg: " + msg);
                    listener.onOrderTechnicalReject(msg.getString(LABEL_REG_SESSION_ID), msg.getString(LABEL_ERROR_MESSAGE));
                }
            } catch (Exception e) {
                LOGGER.info("Impossible to get result from reply received from module: " + serviceName + " - msg: " + msg);
            }
        } else {
            super.onReply(msg);
        }
    }

    protected boolean isTechnicalErrorInText(String text) {
        // list of all non technical reject reasons. Default reject is technical.
        if (text.indexOf("Invalid Security Phase") >= 0) {
            return false;
        }
        if (text.indexOf("Invalid Section Phase") >= 0) {
            return false;
        }
        if (text.indexOf("Bid Price has violated") >= 0) {
            return false;
        }
        if (text.indexOf("Ask Price has violated") >= 0) {
            return false;
        }
        if (text.indexOf("Best Price Threshold Violated") >= 0) {
            return false;
        }
        if (text.indexOf("Section Not Active") >= 0) {
            return false;
        }
        if (text.indexOf("Order can not match") >= 0 )  {
            return false;
        }
        if (text.indexOf("Best Price Missing") >= 0 )  {
            return false;
        }
        
        return true;
    }

    @Override
    protected boolean isTechnicalErrorInMsg(XT2Msg msg) {
        if (msg.getSubject().indexOf(RegulatedMessageFields.SUBJECT_ORDER_REJECT) >= 0) {
            String note = msg.getString(LABEL_REG_TEXT);
            if ("HIMTFFIX".equalsIgnoreCase(msg.getSourceMarketName())) {
                try {
                    int rejReason = msg.getInt(LABEL_REJECT_REASON);
                    if (rejReason == 1 || rejReason == 4 || rejReason == 6 || rejReason == 99 && isTechnicalErrorInText(note)) {
                        return true;
                    } else {
                        return false;
                    }
                } catch (Exception e) {
                    LOGGER.error("Received a Notification message not containing expected fields: " + msg);
                    return false;
                }
            }
        }
        return false;
    }
}
