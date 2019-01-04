/*
 * Copyright 1997-2016 SoftSolutions! srl 
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
package it.softsolutions.bestx.mq;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mq.jms.MQConnectionFactory;

public class BXMQConnectionFactory {

   private static final Logger logger = LoggerFactory.getLogger(BXMQConnectionFactory.class);

   private static final Map<String, MQConnectionFactory> mqQueueConnectionFactories = new HashMap<String, MQConnectionFactory>();

   /**
    * Obtain a MQ connection
    * @param mqConfig : connection configuration parameters
    * @param mqCallback : callback for connection events
    * @return a new BXMQConnection connection
    * @throws Exception : JMS exceptions or specific MQ Exceptions
     */
   public static MQConnection getConnection(MQConfig mqConfig, MQCallback mqCallback) throws Exception {
      logger.debug("Connection config: {}", mqConfig);
      logger.trace("Callback function: {}", mqCallback);
      final Connection queueConnection = mqConfig.getUsername() != null &&  mqConfig.getPassword() != null ? getConnectionFactory(mqConfig).createConnection(mqConfig.getUsername(), mqConfig.getPassword()) : getConnectionFactory(mqConfig).createConnection();
      return new MQConnectionImpl(queueConnection, mqCallback, mqConfig);
   }

   private static synchronized MQConnectionFactory getConnectionFactory(MQConfig mqConfig) throws JMSException {
      if (!mqQueueConnectionFactories.containsKey(mqConfig.getId())) {
         final MQConnectionFactory mqQueueConnectionFactory = new MQConnectionFactory();
         mqQueueConnectionFactory.setHostName(mqConfig.getHost());
         mqQueueConnectionFactory.setPort(mqConfig.getPort());
         if(mqConfig.getConnectionNameList() != null && mqConfig.getConnectionNameList().length() > 0)
        	 mqQueueConnectionFactory.setConnectionNameList(mqConfig.getConnectionNameList());
         mqQueueConnectionFactory.setTransportType(mqConfig.getTransportType());
         mqQueueConnectionFactory.setQueueManager(mqConfig.getQueueManager());
         mqQueueConnectionFactory.setChannel(mqConfig.getChannel());

         if (mqConfig.getSslEnabled()) {
            System.setProperty("javax.net.ssl.trustStore", mqConfig.getSslTrustStore());
            System.setProperty("javax.net.ssl.trustStoreType", mqConfig.getSslTrustStoreType());
            System.setProperty("javax.net.ssl.trustStorePassword", mqConfig.getSslTrustStorePassword());

            System.setProperty("javax.net.ssl.keyStore", mqConfig.getSslKeyStore());
            System.setProperty("javax.net.ssl.keyStoreType", mqConfig.getSslKeyStoreType());
            System.setProperty("javax.net.ssl.keyStorePassword", mqConfig.getSslKeyStorePassword());

            if (mqConfig.getSslDebug() != null) {
               System.setProperty("javax.net.debug", mqConfig.getSslDebug());
            }

            mqQueueConnectionFactory.setSSLCipherSuite(mqConfig.getSslCipherSuite());
         }

         mqQueueConnectionFactories.put(mqConfig.getId(), mqQueueConnectionFactory);
      }

      return mqQueueConnectionFactories.get(mqConfig.getId());
   }

}
