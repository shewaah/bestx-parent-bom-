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

import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ORDER_TYPE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SESSION_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_TIMEINFORCE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_ORDER_TYPE_SUBJECT_LIMIT;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_ORDER_TYPE_SUBJECT_MARKET;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_TIMEINFORCE_FAS;

import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.xt2.protocol.XT2Msg;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-common 
 * First created by: stefano 
 * Creation date: 23-ott-2012 
 * 
 **/
public class FasOrderOutputBean extends RegulatedOrderOutputBean {


    private XT2Msg fasOrderMessage;

    /**
     * Instantiates a new fas order output bean.
     *
     * @param marketOrder the market order
     * @param marketCode the market code
     * @param subMarketCode the sub market code
     * @param account the account
     * @param sessionId the session id
     */
    public FasOrderOutputBean(MarketOrder marketOrder, String marketCode, String subMarketCode, String account, String sessionId) {
        super(marketOrder, marketCode, subMarketCode, account);

        fasOrderMessage = super.getMsg();
        fasOrderMessage.setValue(LABEL_REG_SESSION_ID, sessionId);
        /* 16-06-2009 Ruggero
         * As per new ETLX feature (putting order on the book even if there're no
         * proposals) we could send market order.
         * So we've to check if it is a market or a limit order and initialize
         * properly the fix message.
         */
        fasOrderMessage.setValue(LABEL_ORDER_TYPE, isMarketOrder? VALUE_ORDER_TYPE_SUBJECT_MARKET : VALUE_ORDER_TYPE_SUBJECT_LIMIT);
        fasOrderMessage.setValue(LABEL_TIMEINFORCE, VALUE_TIMEINFORCE_FAS); // FAS - Fill And Store
    }


    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.regulated.RegulatedOrderOutputBean#getMsg()
     */
    @Override
    public XT2Msg getMsg() {
        return fasOrderMessage;
    }
}
