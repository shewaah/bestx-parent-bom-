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

import it.softsolutions.bestx.grdlite.GRDLiteException;
import it.softsolutions.bestx.mq.messages.MQMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.Map;

public class MQConnectionImpl implements MQConnection, ExceptionListener, MQMessageListener {

   private static final Logger logger = LoggerFactory.getLogger(MQConnectionImpl.class);

   private final Connection queueConnection;

   private final MQCallback mqCallback;

   private final boolean isTransacted;

   private Session publisherSession;
   private Session subscriberSession;

   private MessageProducer mqPublisher;
   private MessageConsumer mqSubscriber;

   /**
    * Class constructor
    * 
    * @param queueConnection
    *            : queue connection
    * @param mqCallback
    *            : callback
    * @param mqConfig
    *            : configuration
    * @throws GRDLiteException
    *             : in case of errors
    */
   public MQConnectionImpl(Connection queueConnection, MQCallback mqCallback, MQConfig mqConfig) throws Exception{
      this.queueConnection = queueConnection;
      this.mqCallback = mqCallback;

      this.isTransacted = mqConfig.getTransacted();

      publisherSession = null;
      try {
         publisherSession = queueConnection.createSession(mqConfig.getTransacted(), mqConfig.getAcknowledge());

         // create publisher
         final Destination publisherDestination;
         if (mqConfig.getPublisherTopic() != null) {
            logger.debug("Creating TOPIC for the Publisher Session...");
            publisherDestination = publisherSession.createTopic(mqConfig.getPublisherTopic());
         } else if (mqConfig.getPublisherQueue() != null) {
            logger.debug("Creating QUEUE for the Publisher Session...");
            publisherDestination = publisherSession.createQueue(mqConfig.getPublisherQueue());
         } else {
            logger.debug("No Topic or Queue defined for the Publisher Session...");
            publisherDestination = null;
         }
         
         if (publisherDestination != null) {
            mqPublisher = publisherSession.createProducer(publisherDestination);
         } else {
            logger.error("Couldn't create Publisher. No Publisher Topic or Queue were defined, check your configuration file.");
         }

         // create subscriber if needed
         if (mqConfig.getSubscriberTopic() != null || mqConfig.getSubscriberQueue() != null) {
            subscriberSession = queueConnection.createSession(mqConfig.getTransacted(), mqConfig.getAcknowledge());

            final Destination subscriberDestination;
            if (mqConfig.getSubscriberTopic() != null) {
               logger.debug("Creating TOPIC for the Subscriber Session...");
               subscriberDestination = subscriberSession.createTopic(mqConfig.getSubscriberTopic());
            } else if (mqConfig.getSubscriberQueue() != null) {
               logger.debug("Creating QUEUE for the Subscriber Session...");
               subscriberDestination = subscriberSession.createQueue(mqConfig.getSubscriberQueue());
            } else {
               logger.debug("No Topic or Queue defined for the Subscriber Session...");
               subscriberDestination = null;
            }
            
            if (subscriberDestination != null) {
               mqSubscriber = subscriberSession.createConsumer(subscriberDestination);
               mqSubscriber.setMessageListener(this);
               queueConnection.setExceptionListener(this);
            } else {
               logger.error("Couldn't create Subscriber. No Subscriber Topic or Queue were defined, check your configuration file.");
            }
         }
         else {
            logger.info("Subscriber not configured, thus will not be started up.");
         }

         logger.info("Starting MQ Connection...");
         queueConnection.start();
         logger.info("MQ Connection started.");
      }
      catch (JMSException e) {
         logger.error("Error on initializing MQ items : {}", e.getMessage(), e);
         throw new Exception(e.getMessage());
      }
   }

   @Override
   public void close() throws Exception {
      logger.info("Stopping queueConnection...");
      if (queueConnection != null) {
         queueConnection.stop();
         queueConnection.close();
      }
      logger.info("QueueConnection stopped.");
   }

   @Override
   public void publish(MQMessage mqMessage) throws Exception {
      logger.trace("{}", mqMessage);

      if (mqPublisher == null) {
         throw new Exception("Sender not available!");
      }

      if (mqMessage == null) {
         throw new Exception("null request");
      }

      TextMessage message = null;
      try {
         final String requestMsg = mqMessage.toTextMessage();
         logger.info(requestMsg);

         message = publisherSession.createTextMessage();

         for (Map.Entry<String, String> p : mqMessage.getMessageProperties().entrySet()) {
            message.setStringProperty(p.getKey(), p.getValue());
         }
         message.setText(requestMsg);

         mqPublisher.send(message);

         if (isTransacted) {
            try {
               publisherSession.commit();
            }
            catch (JMSException e) {
               logger.error("Error while committing MQ session: {}", e.getMessage(), e);
            }
         }
      }
      catch (JMSException e) {
         logger.error("Error on publishing message [{}] : {}", message, e.getMessage(), e);
         mqCallback.onException("Error while publishing message [" + message + "] : " + e.getCause());
         throw (new Exception(e));
      }
   }

   @Override
   public void onMessage(Message message) {
      logger.debug("Response received: {}", message);

      TextMessage textMsg = (TextMessage) message;
      String text = null;
      try {
         text = textMsg.getText();
         logger.info(text);
      }
      catch (JMSException e) {
         logger.error("Error while reading text from jms text message [{}]: {}", textMsg, e.getMessage(), e);
         String errorMessage = "Error while reading text from jms text message [" + textMsg + "]: " + e.getMessage();
         mqCallback.onException(errorMessage);
      }

      if (text != null) {
         mqCallback.onResponse(text);
      }

      if (isTransacted) {
         try {
            subscriberSession.commit();
         }
         catch (JMSException e) {
            logger.error("Error while committing MQ session: {}", e.getMessage(), e);
         }
      }
   }

   @Override
   public void acknowledge(Message message) throws JMSException {
      logger.debug("Acknowledge: {}", message);
   }

   @Override
   public void onException(JMSException exception) {
      logger.error("JMXException received: {}", exception.getMessage(), exception);
      mqCallback.onException(exception.getMessage());
   }

}
