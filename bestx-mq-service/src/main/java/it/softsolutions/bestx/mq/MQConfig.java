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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import com.ibm.msg.client.wmq.WMQConstants;

/**
    Configuration class.
    Properties example :
        userName=XTA
        password=xta 
        host=windb 
        port=1414
        queueManager=QM_windb
        channel=S_windb
        transacted=true 
        acknowledge=false
        publisherQueue=Dev2_To_OPTES
        subscriberQueue=Dev2_From_OPTES
        expiry=false
*/
public class MQConfig {

   private String username;
   private String password;
   private String host;
   private String connectionNameList;
   private int port;
   private int transportType;
   private String queueManager;
   private String channel;
   private Boolean transacted;
   private int acknowledge;

   private String publisherQueue;
   private String subscriberQueue;

   private String publisherTopic;
   private String subscriberTopic;

   private Boolean expiry;

   private Boolean sslEnabled;
   private String sslCipherSuite;
   private String sslDebug;

   private String sslTrustStore;
   private String sslTrustStoreType;
   private String sslTrustStorePassword;

   private String sslKeyStore;
   private String sslKeyStoreType;
   private String sslKeyStorePassword;

   /**
    * Build the GRDLite config starting from a configuration
    * 
    * @param serviceCfg
    *            an Apache commons configuration
    * @return the new GRDLite config
    */
   public static MQConfig fromConfiguration(Configuration serviceCfg) {
      if (serviceCfg == null) {
         throw new IllegalArgumentException("configuration is null");
      }

      MQConfig res = new MQConfig();
      res.username = serviceCfg.getString("username", null);
      res.password = serviceCfg.getString("password", null);

      res.host = serviceCfg.getString("host", null);
      res.port = serviceCfg.getInt("port", 0);

      res.connectionNameList = serviceCfg.getString("connectionNameList", null);

      res.transportType = WMQConstants.WMQ_CM_CLIENT;

      res.queueManager = serviceCfg.getString("queueManager", null);
      res.channel = serviceCfg.getString("channel", null);
      res.transacted = serviceCfg.getBoolean("transacted", true);
      res.acknowledge = serviceCfg.getInt("acknowledge", 1);
      
      res.publisherTopic = serviceCfg.getString("publisherTopic", null);
      res.subscriberTopic = serviceCfg.getString("subscriberTopic", null);

      res.publisherQueue = serviceCfg.getString("publisherQueue", null);
      res.subscriberQueue = serviceCfg.getString("subscriberQueue", null);

      res.expiry = serviceCfg.getBoolean("expiry", false);

      res.sslEnabled = serviceCfg.getBoolean("sslEnabled", false);
      res.sslCipherSuite = serviceCfg.getString("sslCipherSuite", null);
      res.sslDebug = serviceCfg.getString("sslDebug", null);

      res.sslTrustStore = serviceCfg.getString("sslTrustStoreFile", null);
      res.sslTrustStoreType = serviceCfg.getString("sslTrustStoreType", null);
      res.sslTrustStorePassword = serviceCfg.getString("sslTrustStorePassword", null);

      res.sslKeyStore = serviceCfg.getString("sslKeyStoreFile", null);
      res.sslKeyStoreType = serviceCfg.getString("sslKeyStoreType", null);
      res.sslKeyStorePassword = serviceCfg.getString("sslKeyStorePassword", null);
      return res;
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getHost() {
      return host;
   }

   public void setHost(String host) {
      this.host = host;
   }

   public int getPort() {
      return port;
   }

   public void setPort(int port) {
      this.port = port;
   }

   public int getTransportType() {
      return transportType;
   }

   public void setTransportType(int transportType) {
      this.transportType = transportType;
   }

   public String getQueueManager() {
      return queueManager;
   }

   public void setQueueManager(String queueManager) {
      this.queueManager = queueManager;
   }

   public String getChannel() {
      return channel;
   }

   public void setChannel(String channel) {
      this.channel = channel;
   }

   /**
    * @return the transacted
    */
   public Boolean getTransacted() {
      return transacted;
   }

   /**
    * @param transacted
    *            the transacted to set
    */
   public void setTransacted(Boolean transacted) {
      this.transacted = transacted;
   }

   /**
    * @return the acknowledge
    */
   public int getAcknowledge() {
      return acknowledge;
   }

   /**
    * @param acknowledge
    *            the acknowledge to set
    */
   public void setAcknowledge(int acknowledge) {
      this.acknowledge = acknowledge;
   }

   /**
    * @return the publisherQueue
    */
   public String getPublisherQueue() {
      return publisherQueue;
   }

   /**
    * @param publisherQueue
    *            the publisherQueue to set
    */
   public void setPublisherQueue(String publisherQueue) {
      this.publisherQueue = publisherQueue;
   }

   public String getPublisherTopic() {
      return publisherTopic;
   }

   public void setPublisherTopic(String publisherTopic) {
      this.publisherTopic = publisherTopic;
   }
   
   /**
    * @return the subscriberQueue
    */
   public String getSubscriberQueue() {
      return subscriberQueue;
   }
   
   /**
    * @param subscriberQueue
    *            the subscriberQueue to set
    */
   public void setSubscriberQueue(String subscriberQueue) {
      this.subscriberQueue = subscriberQueue;
   }

   /**
    * @return the subscriberTopic
    */
   public String getSubscriberTopic() {
      return subscriberTopic;
   }

   /**
    * @param subscriberTopic
    *            the subscriberTopic to set
    */
   public void setSubscriberTopic(String subscriberTopic) {
      this.subscriberTopic = subscriberTopic;
   }

   /**
    * @return the expiry
    */
   public Boolean getExpiry() {
      return expiry;
   }

   /**
    * @param expiry
    *            the expiry to set
    */
   public void setExpiry(Boolean expiry) {
      this.expiry = expiry;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("MQConfig [username=");
      builder.append(username);
      builder.append(", password=");
      builder.append(obfuscatePassword(password));
      builder.append(", host=");
      builder.append(host);
      builder.append(", port=");
      builder.append(port);
      builder.append(", transportType=");
      builder.append(transportType);
      builder.append(", queueManager=");
      builder.append(queueManager);
      builder.append(", channel=");
      builder.append(channel);
      builder.append(", transacted=");
      builder.append(transacted);
      builder.append(", acknowledge=");
      builder.append(acknowledge);
      builder.append(", publisherTopic=");
      builder.append(publisherTopic);
      builder.append(", subscriberTopic=");
      builder.append(subscriberTopic);
      builder.append(", publisherQueue=");
      builder.append(publisherQueue);
      builder.append(", subscriberQueue=");
      builder.append(subscriberQueue);
      builder.append(", expiry=");
      builder.append(expiry);
      builder.append(", sslEnabled=");
      builder.append(sslEnabled);
      builder.append(", sslCipherSuite=");
      builder.append(sslCipherSuite);
      builder.append(", sslDebug=");
      builder.append(sslDebug);
      builder.append(", sslTrustStore=");
      builder.append(sslTrustStore);
      builder.append(", sslTrustStoreType=");
      builder.append(sslTrustStoreType);
      builder.append(", sslTrustStorePassword=");
      builder.append(obfuscatePassword(sslTrustStorePassword));
      builder.append(", sslKeyStore=");
      builder.append(sslKeyStore);
      builder.append(", sslKeyStoreType=");
      builder.append(sslKeyStoreType);
      builder.append(", sslKeyStorePassword=");
      builder.append(obfuscatePassword(sslKeyStorePassword));
      builder.append("]");
      return builder.toString();
   }

   private String obfuscatePassword(final String basePassword) {
      return basePassword != null ? StringUtils.overlay(basePassword, StringUtils.repeat("*", basePassword.length() - 4), 2, basePassword.length() - 2) : null;
   }

   public Boolean getSslEnabled() {
      return sslEnabled;
   }

   public void setSslEnabled(Boolean sslEnabled) {
      this.sslEnabled = sslEnabled;
   }

   public String getSslCipherSuite() {
      return sslCipherSuite;
   }

   public void setSslCipherSuite(String sslCipherSuite) {
      this.sslCipherSuite = sslCipherSuite;
   }

   public String getSslDebug() {
      return sslDebug;
   }

   public void setSslDebug(String sslDebug) {
      this.sslDebug = sslDebug;
   }

   public String getSslTrustStore() {
      return sslTrustStore;
   }

   public void setSslTrustStore(String sslTrustStore) {
      this.sslTrustStore = sslTrustStore;
   }

   public String getSslTrustStoreType() {
      return sslTrustStoreType;
   }

   public void setSslTrustStoreType(String sslTrustStoreType) {
      this.sslTrustStoreType = sslTrustStoreType;
   }

   public String getSslTrustStorePassword() {
      return sslTrustStorePassword;
   }

   public void setSslTrustStorePassword(String sslTrustStorePassword) {
      this.sslTrustStorePassword = sslTrustStorePassword;
   }

   public String getSslKeyStore() {
      return sslKeyStore;
   }

   public void setSslKeyStore(String sslKeyStore) {
      this.sslKeyStore = sslKeyStore;
   }

   public String getSslKeyStoreType() {
      return sslKeyStoreType;
   }

   public void setSslKeyStoreType(String sslKeyStoreType) {
      this.sslKeyStoreType = sslKeyStoreType;
   }

   public String getSslKeyStorePassword() {
      return sslKeyStorePassword;
   }

   public void setSslKeyStorePassword(String sslKeyStorePassword) {
      this.sslKeyStorePassword = sslKeyStorePassword;
   }

   public String getId() {
      return "[MQConfig:" + host + port + transportType + queueManager + channel + "]";
   }

public String getConnectionNameList() {
	return connectionNameList;
}

public void setConnectionNameList(String connectionNameList) {
	this.connectionNameList = connectionNameList;
}
}
