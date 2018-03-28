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
package it.softsolutions.bestx.connections.xt2;

import it.softsolutions.xt2.protocol.XT2Msg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: this class is mainly for manage generic input messages from market
 * 
 * Project Name : bestxengine-common First created by: stefano.pontillo Creation date: 06/giu/2012
 * 
 **/
public class XT2InputLazyBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(XT2InputLazyBean.class);
    
    protected XT2Msg msg;

    /**
     * Return a String field from the message, given its id
     * 
     * @param id
     *            ID of the field
     * @return the field value
     */
    protected String getStringField(String id) {
        try {
            return msg.getString(id);
        } catch (Exception e) {
            LOGGER.debug("Field '{}' not found: {}", id, e.getMessage());
            return null;
        }
    }

    /**
     * Return an int value from the message, given its id
     * 
     * @param id
     *            the id of the field to retrieve
     * @return the value of the field
     */
    protected Integer getIntField(String id) {
        try {
            return msg.getInt(id);
        } catch (Exception e) {
            LOGGER.debug("Field '{}' not found: {}", id, e.getMessage());
            return null;
        }
    }

    /**
     * Return a Double value of the given field id
     * 
     * @param id
     *            the id of the field to retrieve
     * @return the value of the field
     */
    protected Double getDoubleField(String id) {
        try {
            return msg.getDouble(id);
        } catch (Exception e) {
            LOGGER.debug("Field '{}' not found: {}", id, e.getMessage());
            return null;
        }
    }

    /**
     * Return the long value of the given field id
     * 
     * @param id
     *            the id of the field to retrieve
     * @return the value of the field
     */
    protected Long getLongField(String id) {
        try {
            return msg.getLong(id);
        } catch (Exception e) {
            LOGGER.debug("Field '{}' not found: {}", id, e.getMessage());
            return null;
        }
    }
}
