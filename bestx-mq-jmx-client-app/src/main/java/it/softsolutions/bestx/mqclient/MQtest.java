package it.softsolutions.bestx.mqclient;

import java.lang.management.ManagementFactory;
import java.util.LinkedList;
import java.util.Queue;

import javax.jms.JMSException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.mq.BXMQConnectionFactory;
import it.softsolutions.bestx.mq.MQCallback;
import it.softsolutions.bestx.mq.MQConfig;
import it.softsolutions.bestx.mq.MQConfigHelper;
import it.softsolutions.bestx.mq.MQConnection;
import it.softsolutions.bestx.mq.messages.MQMessage;
import it.softsolutions.bestx.pricediscovery.MQPriceDiscoveryMessage;
import net.sf.json.JSONObject;

/**
 * Hello world!
 *
 */
public class MQtest implements MQtestMBean, MQCallback {

   private static final Logger LOGGER = LoggerFactory.getLogger(MQtest.class);

   private static final String MQ_CONFIG_FILE = "bestx-test-mq.properties";

   private static final String MQ_CONFIG_PREFIX = "test.mq";

   /* MQ Connection variables */
   private MQConnection mqConnection = null;

   private boolean available = false;

   private ObjectName jmxName;
   
   private Queue<String> messages;

   
   
   public String connectMQService() {
      LOGGER.debug("MQ Service connection attempt");
      try {
         final MQConfig mqCfg = MQConfigHelper.getConfigurationFromFile(MQ_CONFIG_FILE, MQ_CONFIG_PREFIX);
         
         mqConnection = BXMQConnectionFactory.getConnection(mqCfg, this);
         available = true;
         LOGGER.debug("MQ connection attempt succeded");
      }
      catch (JMSException e) {
         LOGGER.info("Cannot connect to the JMS queue: {}", e.getMessage(), e);
         return "ERROR - " + e.getMessage();
      }
      catch (Exception ge) {
         LOGGER.info("Cannot connect to the MQ service: {}", ge.getMessage(), ge);
         return "ERROR - " + ge.getMessage();
      }
      return "OK";
   }


   public void disconnect() {
      LOGGER.info("Closing Connection to MQ");
      if (mqConnection != null) {
         try {
            mqConnection.close();
         }
         catch (Exception e) {
            LOGGER.error("Failed to disconnect from MQService: " + e.getMessage(), e);
            available = false;
         }
         LOGGER.info("MQ Connection closed.");
      }
      else {
         LOGGER.warn("MQ Connection already closed.");
      }
   }

   public boolean isConnected() {
      return available;
   }
   
   public void initJMX() {
      //Start JMX connection
      try {
         messages = new LinkedList<String>();
         
         jmxName = new ObjectName("BestxMQClient:name=MQclient");
         StandardMBean feedReplayerBean = new StandardMBean(this, MQtestMBean.class);
         ManagementFactory.getPlatformMBeanServer().registerMBean(feedReplayerBean, jmxName);
      }
      catch (Exception e) {
         LOGGER.error("Error occurs during bean registration {}", e.getMessage(), e);
      }
   }
   

   public static void main(String[] args) {
      MQtest test = new MQtest();
      test.initJMX();
      
      // 3. Wait for ever
      try {
          Thread.currentThread().join();
      } catch (Exception e) {
         LOGGER.error("Error on start application", e);
      }
   }
   
   /**
    * Specific BestX price discovery message publish
    * 
    */
   public String sendPriceDiscoveryRequest(String instrumentCode, String side, double qty) {
      try {
         String orderId = "MQOI_" + System.currentTimeMillis();
         String traceId = "MQTI_" + System.currentTimeMillis();
         
         JSONObject pdRequest = new JSONObject();
         pdRequest.put("instrument", instrumentCode);
         pdRequest.put("side", side);
         pdRequest.put("quantity", qty);
         pdRequest.put("order_id", orderId);
         pdRequest.put("trace_id", traceId);
         
         MQMessage message = new MQPriceDiscoveryMessage(pdRequest.toString());
         
         mqConnection.publish(message);
         return "OK;" + orderId + ";" + traceId;
      } catch(Exception e){
         LOGGER.error("Exception when publishing request to GRDLite");
         return "ERROR - " + e.getMessage();
      }
   }

   /**
    * Generic message MQ publish
    * 
    * @param messageStr
    * @return
    */
   public String sendMessage(String messageStr) {
      try {
         MQMessage message = new MQPriceDiscoveryMessage(messageStr);
         
         mqConnection.publish(message);
         return "OK";
      } catch(Exception e){
         LOGGER.error("Exception when publishing request to GRDLite");
         return "ERROR - " + e.getMessage();
      }
   }
   
   /**
    * Return the first message in the queue
    * 
    * @return
    */
   public String readMessage() {
      return messages.poll();
   }
   
   public void onResponse(String response) {
      LOGGER.info("RESPONSE MESSAGE: " + response);
      messages.add(response);
   }

   public void onException(String message) {
      LOGGER.error("MQ exception : " + message);
   }
}
