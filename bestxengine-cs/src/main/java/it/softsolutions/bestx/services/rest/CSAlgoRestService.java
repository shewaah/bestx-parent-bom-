/*
* Copyright 1997-2021 SoftSolutions! srl 
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

package it.softsolutions.bestx.services.rest;

import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.BaseOperatorConsoleAdapter;

/**  
*
* Purpose: this class is mainly for manage CS Algo REST service connection
*
* Project Name : bestxengine-cs 
* First created by: stefano.pontillo 
* Creation date: 22 lug 2021 
* 
**/
public class CSAlgoRestService extends BaseOperatorConsoleAdapter {

   private static final Logger LOGGER = LoggerFactory.getLogger(CSAlgoRestService.class);

   private String endpoint;
   private String servicePath;
   private String healtcheckPath;
   private int connectionTimeout;
   private int responseTimeout;
   private String algoServiceName;
   
   private boolean active = false;
   private boolean available = false;
   
   private static final int CHECK_PERIOD = 30000;
   private ServiceHeartbeatChecker serviceHeartbeatChecker;
   private WebClient restClient, heartbeatClient;
   
   

   @Override
   public void init() throws BestXException {
      this.restClient = WebClient.create(this.endpoint);
      this.restClient.path(this.servicePath);
      this.restClient.type(MediaType.APPLICATION_JSON);
      this.restClient.accept(MediaType.APPLICATION_JSON);
      
      HTTPConduit conduit = WebClient.getConfig(this.restClient).getHttpConduit();
      conduit.getClient().setConnectionTimeout(this.connectionTimeout);
      conduit.getClient().setReceiveTimeout(responseTimeout);
      
      this.heartbeatClient = WebClient.create(this.endpoint);
      this.heartbeatClient.path(this.healtcheckPath);
      this.heartbeatClient.type(MediaType.APPLICATION_JSON);
      this.heartbeatClient.accept(MediaType.APPLICATION_JSON);
      
      HTTPConduit hbConduit = WebClient.getConfig(this.heartbeatClient).getHttpConduit();
      hbConduit.getClient().setConnectionTimeout(this.connectionTimeout);
      hbConduit.getClient().setReceiveTimeout(responseTimeout);
   }
   
   @Override
   public void connect() {
      LOGGER.info("Received request to connect to REST service");
      if (!available) {
         try {
            this.active = true;
            this.serviceHeartbeatChecker = new ServiceHeartbeatChecker();
         
            (new Thread(this.serviceHeartbeatChecker)).start();
            LOGGER.info("Request to connect to REST service completed successfully");
         } catch (Exception e) {
            this.active = false;
            LOGGER.error("Unable to connect to REST service", e);
         }
      }
   }

   @Override
   public void disconnect() {
      LOGGER.info("Request received to disconnect from REST Service");
      try {
         if (this.serviceHeartbeatChecker != null) {
            this.serviceHeartbeatChecker.stop();
         }
         LOGGER.info("Request to disconnect from REST Service completed successfully");
      } finally {
         this.serviceHeartbeatChecker = null;
         this.active = false;
         this.available = false;
      }
   }

   @Override
   public boolean isConnected() {
      return active && available;
   }
   
   /**
    * Return  true whenever the service has been activated by init or connect
    * 
    * @return
    */
   public boolean isActive() {
      return active;
   }
   
   /**
    * Return true whenever the heartbeat return a positive response
    * 
    * @return
    */
   public boolean isAvailable() {
      return available;
   }
   
   public String getEndpoint() {
      return endpoint;
   }
   
   public void setEndpoint(String endpoint) {
      this.endpoint = endpoint;
   }
   
   public int getConnectionTimeout() {
      return connectionTimeout;
   }
   
   public void setConnectionTimeout(int connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
   }

   public int getResponseTimeout() {
      return responseTimeout;
   }
   
   public void setResponseTimeout(int responseTimeout) {
      this.responseTimeout = responseTimeout;
   }
   
   public String getServicePath() {
      return servicePath;
   }
   
   public void setServicePath(String servicePath) {
      this.servicePath = servicePath;
   }
   
   public String getHealtcheckPath() {
      return healtcheckPath;
   }

   
   public void setHealtcheckPath(String healtcheckPath) {
      this.healtcheckPath = healtcheckPath;
   }
   
   public String getAlgoServiceName() {
      return algoServiceName;
   }
   
   public void setAlgoServiceName(String algoServiceName) {
      this.algoServiceName = algoServiceName;
   }

   private JSONObject callRestService(JSONObject objReq, WebClient client) {
      String request = objReq.toString();
      LOGGER.info("Request to REST service: {}", request);
      String response = client.post(request).readEntity(String.class);
      LOGGER.info("Response from REST service: {}", response);
      JSONObject objResp = new JSONObject(response);
      return objResp;
   }
   

   private class ServiceHeartbeatChecker implements Runnable {
      private boolean keepChecking = true;

      @Override
      public void run() {
         while (keepChecking) {
            try {
               // Heartbeat check must be done by call GET on the healtcheck URL
               String response = restClient.get().readEntity(String.class);
               JSONObject objResp = new JSONObject(response);
               if ("GREEN".equalsIgnoreCase(objResp.getJSONObject("data").getString("status"))) {
                  available = true;
               } else {
                  available = false;
               }
               Thread.sleep(CHECK_PERIOD);
            } catch (Exception e) {
               LOGGER.error("Error while trying to check connection status: ", e);
               if (keepChecking) available = false;
            }
         }
         LOGGER.info("Stopping checking the connection to Datalake!");
         available = false;
      }
      
      public void stop() {
         this.keepChecking = false;
      }
   }
}
