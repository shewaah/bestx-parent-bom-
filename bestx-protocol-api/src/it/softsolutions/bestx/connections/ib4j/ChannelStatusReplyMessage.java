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
package it.softsolutions.bestx.connections.ib4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.MarketConnectionRegistry;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.connections.ConnectionRegistry;
import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.connections.MarketConnection;
import it.softsolutions.ib4j.clientserver.IBcsMessage;

/**  
*
* Purpose: message sent by Bestx engine with the connection status  
*
* Project Name : bestxengine-protocol-api 
* First created by: anna cochetti 
* Creation date: 12/06/2016 
* 
**/
public class ChannelStatusReplyMessage extends IBcsMessage {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ChannelStatusReplyMessage.class);

    private static final long serialVersionUID = -6062875247598715718L;
    
    public static enum MqQueues {
       GRDLITE, PRICEDISCOVERY 
    }
    

    /**
     * Instantiates a new akros channel status reply message.
     * 
     * @param sessionId
     *            the session id
     * @param marketConnectionRegistry
     *            the market connection registry
     */
    public ChannelStatusReplyMessage(String sessionId, MarketConnectionRegistry marketConnectionRegistry, ConnectionRegistry connectionRegistry) {
        setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
        setStringProperty(IB4JOperatorConsoleMessage.TYPE_STATUS, IB4JOperatorConsoleMessage.STATUS_OK);
        setStringProperty(IB4JOperatorConsoleMessage.RESP_TYPE, IB4JOperatorConsoleMessage.RESP_TYPE_QUERY);
        setStringProperty(IB4JOperatorConsoleMessage.TYPE_QUERY, IB4JOperatorConsoleMessage.QUERY_GET_CHANNEL_STATUS);
        setIntProperty(IB4JOperatorConsoleMessage.FLD_PRICE_SERVICE_STATUS, 0);
        for (MarketConnection marketConnection : marketConnectionRegistry.getAllMarketConnections()) {
            int priceConnectionStatus = 0, buySideConnectionStatus = 0;
            int priceConnectionEnabled = 0, buySideConnectionEnabled = 0;

            if (!marketConnection.isPriceConnectionProvided()) {
                priceConnectionStatus = 2;
            } else {
                if (marketConnection.isPriceConnectionAvailable()) {
                    priceConnectionStatus = 1;
                }
            }

            if (!marketConnection.isPriceConnectionProvided()) {
                priceConnectionEnabled = 2;
            } else {
                if (marketConnection.isPriceConnectionEnabled()) {
                    priceConnectionEnabled = 1;
                }
            }

            if (!marketConnection.isBuySideConnectionProvided()) {
                buySideConnectionStatus = 2;
            } else {
                if (marketConnection.isBuySideConnectionAvailable()) {
                    buySideConnectionStatus = 1;
                }
            }

            if (!marketConnection.isBuySideConnectionProvided()) {
                buySideConnectionEnabled = 2;
            } else {
                if (marketConnection.isBuySideConnectionEnabled()) {
                    buySideConnectionEnabled = 1;
                }
            }

            setIntProperty(IB4JOperatorConsoleMessage.FLD_MKT_PRICE_CONNECTION_STATUS + marketConnection.getMarketCode(), priceConnectionStatus);
            setIntProperty(IB4JOperatorConsoleMessage.FLD_MKT_ORDER_CONNECTION_STATUS + marketConnection.getMarketCode(), buySideConnectionStatus);
            setIntProperty(IB4JOperatorConsoleMessage.FLD_MKT_PRICE_CONNECTION_ENABLED + marketConnection.getMarketCode(), priceConnectionEnabled);
            setIntProperty(IB4JOperatorConsoleMessage.FLD_MKT_ORDER_CONNECTION_ENABLED + marketConnection.getMarketCode(), buySideConnectionEnabled);
        }
        
        // if multiple connections this should be modified
        CustomerConnection customerConnection = connectionRegistry.getCustomerConnection();
        setStringProperty(IB4JOperatorConsoleMessage.FLD_FIX_CHANNEL_NAME, customerConnection.getChannelName());
        setIntProperty(IB4JOperatorConsoleMessage.FLD_FIX_CHANNEL_STATUS, customerConnection.isConnected() ? 1 : 0);
        
        Connection mqPriceDiscoveryConnection = connectionRegistry.getMqPriceDiscoveryConnection();
        if (mqPriceDiscoveryConnection != null) {
        	setStringProperty(IB4JOperatorConsoleMessage.FLD_MQ_CHANNEL_NAME + MqQueues.PRICEDISCOVERY, mqPriceDiscoveryConnection.getConnectionName());
        	setIntProperty(IB4JOperatorConsoleMessage.FLD_MQ_CHANNEL_STATUS + MqQueues.PRICEDISCOVERY, mqPriceDiscoveryConnection.isConnected() ? 1 : 0);
        } else {
        	setStringProperty(IB4JOperatorConsoleMessage.FLD_MQ_CHANNEL_NAME + MqQueues.PRICEDISCOVERY, "mqPriceDiscoveryConnection");
        	setIntProperty(IB4JOperatorConsoleMessage.FLD_MQ_CHANNEL_STATUS + MqQueues.PRICEDISCOVERY, 0);
        }
        Connection grdLiteConnection = connectionRegistry.getGrdLiteConnection();
        if(grdLiteConnection != null) {
	        setStringProperty(IB4JOperatorConsoleMessage.FLD_MQ_CHANNEL_NAME + MqQueues.GRDLITE, grdLiteConnection.getConnectionName());
	        setIntProperty(IB4JOperatorConsoleMessage.FLD_MQ_CHANNEL_STATUS + MqQueues.GRDLITE, grdLiteConnection.isConnected() ? 1 : 0);
        }   else {
        	setStringProperty(IB4JOperatorConsoleMessage.FLD_MQ_CHANNEL_NAME + MqQueues.GRDLITE, "grdLiteConnection");
        	setIntProperty(IB4JOperatorConsoleMessage.FLD_MQ_CHANNEL_STATUS + MqQueues.GRDLITE, 0);
        }     
        
        //Kafka status
        Connection datalakeConnection = connectionRegistry.getDatalakeConnection();
        if (datalakeConnection != null) {
           setStringProperty(IB4JOperatorConsoleMessage.FLD_DL_CHANNEL_NAME, datalakeConnection.getConnectionName());
           setIntProperty(IB4JOperatorConsoleMessage.FLD_DL_CHANNEL_STATUS, datalakeConnection.isConnected() ? 1 : 0);
        } else {
           setStringProperty(IB4JOperatorConsoleMessage.FLD_DL_CHANNEL_NAME, "kafkaDatalakeConnection");
           setIntProperty(IB4JOperatorConsoleMessage.FLD_DL_CHANNEL_STATUS, 0);
        }
        
        //CS ALGO Rest Service status
        Connection csAlgoConnection = connectionRegistry.getCsAlgoRestServiceConnection();
        if (csAlgoConnection != null) {
           setStringProperty(IB4JOperatorConsoleMessage.FLD_ALGO_REST_CHANNEL_NAME, csAlgoConnection.getConnectionName());
           setIntProperty(IB4JOperatorConsoleMessage.FLD_ALGO_REST_CHANNEL_STATUS, csAlgoConnection.isConnected() ? 1 : 0);
        } else {
           setStringProperty(IB4JOperatorConsoleMessage.FLD_ALGO_REST_CHANNEL_NAME, "RestServiceConnection");
           setIntProperty(IB4JOperatorConsoleMessage.FLD_ALGO_REST_CHANNEL_STATUS, 0);
        }
        
        LOGGER.debug("ChannelStatusReplyMessage = {}", this);
    }
}
