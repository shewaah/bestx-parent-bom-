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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.BaseOperatorConsoleAdapter;
import it.softsolutions.bestx.services.rest.dto.ExceptionMessageElement;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalRequest.ConsolidatedBookElement;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalResponse;
import it.softsolutions.bestx.services.rest.dto.GetRoutingProposalResponseData.Venue;
import it.softsolutions.bestx.services.rest.dto.MessageElement;

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
   
   private static final String CSALGOREST_JSON_KEY_ISIN = "isin";
   private static final String CSALGOREST_JSON_KEY_SIDE = "side";
   private static final String CSALGOREST_JSON_KEY_PRICE_TYPE_FIX = "priceTypeFIX";
   private static final String CSALGOREST_JSON_KEY_SIZE = "size";
   private static final String CSALGOREST_JSON_KEY_LEGAL_ENTITY = "LegalEntity";
   private static final String CSALGOREST_JSON_KEY_CONSOLIDATED_BOOK = "consolidatedBook";
   private static final String CSALGOREST_JSON_KEY_BID_ASK = "bidAsk";
   private static final String CSALGOREST_JSON_KEY_PRICE = "price";
   private static final String CSALGOREST_JSON_KEY_DATE_TIME = "dateTime";
   private static final String CSALGOREST_JSON_KEY_PRICE_QUALITY = "priceQuality";
   private static final String CSALGOREST_JSON_KEY_DEALER_AT_VENUE = "dealerAtVenue";
   private static final String CSALGOREST_JSON_KEY_DATA_SOURCE = "dataSource";
   private static final String CSALGOREST_JSON_KEY_MARKET_MAKER_CODE = "marketMakerCode";
   private static final String CSALGOREST_JSON_KEY_QUOTE_STATUS = "quoteStatus";

   private static final String CSALGOREST_JSON_KEY_MESSAGES = "messages";
   private static final String CSALGOREST_JSON_KEY_MESSAGE = "message";
   private static final String CSALGOREST_JSON_KEY_SEVERITY = "severity";
   private static final String CSALGOREST_JSON_KEY_DATA = "data";
   private static final String CSALGOREST_JSON_KEY_TARGET_PRICE = "targetPrice";
   private static final String CSALGOREST_JSON_KEY_LIMIT_MONITOR_PRICE = "limitMonitorPrice";
   private static final String CSALGOREST_JSON_KEY_INCLUDE_DEALERS = "includeDealers";
   private static final String CSALGOREST_JSON_KEY_EXCLUDE_DEALERS = "excludeDealers";
   private static final String CSALGOREST_JSON_KEY_TARGET_VENUE = "targetVenue";

   private static final String CSALGOREST_JSON_KEY_EXCEPTIONS = "exceptions";
   private static final String CSALGOREST_JSON_KEY_EXCEPTION_MESSAGE = "exceptionMessage";
   private static final String CSALGOREST_JSON_KEY_EXCEPTION_CODE = "exceptionCode";

   
   
   private String endpoint;
   private String servicePath;
   private String healthcheckPath;
   private int connectionTimeout;
   private int responseTimeout;
   private String algoServiceName;
   
   private boolean active = false;
   private boolean available = false;
   
   private static final int CHECK_PERIOD = 30000;
   private ServiceHeartbeatChecker serviceHeartbeatChecker;
   private WebClient restClient, heartbeatClient;
   private String lastError;
   
   

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
      this.heartbeatClient.path(this.healthcheckPath);
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
            lastError = "Unable to connect to CS Algo REST Service";
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
   
   public String getHealthcheckPath() {
      return healthcheckPath;
   }

   
   public void setHealthcheckPath(String healtcheckPath) {
      this.healthcheckPath = healtcheckPath;
   }
   
   public String getAlgoServiceName() {
      return algoServiceName;
   }
   
   public void setAlgoServiceName(String algoServiceName) {
      this.algoServiceName = algoServiceName;
   }
   
   public String getLastError() {
      return lastError;
   }

   private JSONObject callRestService(JSONObject objReq, WebClient client) {
      String request = objReq.toString();
      LOGGER.info("Request to REST service: {}", request);
      String response = client.post(request).readEntity(String.class);
      LOGGER.info("Response from REST service: {}", response);
      JSONObject objResp = new JSONObject(response);
      return objResp;
   }
   
   public GetRoutingProposalResponse doGetRoutingProposal(GetRoutingProposalRequest request) {
      //TODO: manage timeout
      JSONObject jsonRequest = new JSONObject();
      jsonRequest.put(CSALGOREST_JSON_KEY_ISIN, request.getIsin());
      jsonRequest.put(CSALGOREST_JSON_KEY_SIDE, request.getSide());
      jsonRequest.put(CSALGOREST_JSON_KEY_PRICE_TYPE_FIX, request.getPriceTypeFIX());
      jsonRequest.put(CSALGOREST_JSON_KEY_SIZE, request.getSize());
      jsonRequest.put(CSALGOREST_JSON_KEY_LEGAL_ENTITY, request.getLegalEntity());
      
      JSONArray consolidatedBook = new JSONArray();
      for (ConsolidatedBookElement elem : request.getConsolidatedBook()) {
    	  JSONObject jsonElem = new JSONObject();
    	  jsonElem.put(CSALGOREST_JSON_KEY_BID_ASK, elem.getBidAsk().toString());
    	  jsonElem.put(CSALGOREST_JSON_KEY_PRICE, elem.getPrice());
    	  jsonElem.put(CSALGOREST_JSON_KEY_SIZE, elem.getSize());
    	  jsonElem.put(CSALGOREST_JSON_KEY_DATE_TIME, elem.getDateTime()); // TODO Check format
    	  jsonElem.put(CSALGOREST_JSON_KEY_PRICE_QUALITY, elem.getPriceQuality().toString());
    	  jsonElem.put(CSALGOREST_JSON_KEY_DEALER_AT_VENUE, elem.getDealerAtVenue());
    	  jsonElem.put(CSALGOREST_JSON_KEY_DATA_SOURCE, elem.getDataSource().toString());
    	  jsonElem.put(CSALGOREST_JSON_KEY_MARKET_MAKER_CODE, elem.getMarketMakerCode());
    	  if (elem.getQuoteStatus().isPresent()) {
    		  jsonElem.put(CSALGOREST_JSON_KEY_QUOTE_STATUS, elem.getQuoteStatus().get());
    	  }
    	  consolidatedBook.put(jsonElem);
      }
      
      jsonRequest.put(CSALGOREST_JSON_KEY_CONSOLIDATED_BOOK, consolidatedBook);
      
      JSONObject jsonResponse = this.callRestService(jsonRequest, this.restClient);
      
	  GetRoutingProposalResponse response = new GetRoutingProposalResponse();

	  JSONArray jsonMessages = jsonResponse.optJSONArray(CSALGOREST_JSON_KEY_MESSAGES);
	  
	  if (jsonMessages != null) {
		  for (int i = 0 ; i < jsonMessages.length() ; i++) {
			  JSONObject jsonMessage = jsonMessages.getJSONObject(i);
			  MessageElement message = new MessageElement();
			  message.setMessage(jsonMessage.getString(CSALGOREST_JSON_KEY_MESSAGE));
			  message.setSeverity(jsonMessage.getString(CSALGOREST_JSON_KEY_SEVERITY));
			  response.getMessages().add(message);
		  }
	  }
	  
	  JSONObject jsonData = jsonResponse.getJSONObject(CSALGOREST_JSON_KEY_DATA);
	  
      response.getData().setTargetPrice(new BigDecimal(Double.valueOf(jsonData.getDouble(CSALGOREST_JSON_KEY_TARGET_PRICE)).toString()));
      response.getData().setLimitMonitorPrice(new BigDecimal(Double.valueOf(jsonData.getDouble(CSALGOREST_JSON_KEY_LIMIT_MONITOR_PRICE)).toString()));
      response.getData().setTargetVenue(Venue.valueOf(jsonData.getString(CSALGOREST_JSON_KEY_TARGET_VENUE)));
      
      JSONArray jsonIncludeDealers = jsonData.optJSONArray(CSALGOREST_JSON_KEY_INCLUDE_DEALERS);      
      if (jsonIncludeDealers != null) {
	      for (int i = 0 ; i < jsonIncludeDealers.length() ; i++) {
	    	  response.getData().getIncludeDealers().add(jsonIncludeDealers.getString(i));
	      }
      }

      JSONArray jsonExcludeDealers = jsonData.optJSONArray(CSALGOREST_JSON_KEY_EXCLUDE_DEALERS);      
      if (jsonExcludeDealers != null) {
	      for (int i = 0 ; i < jsonExcludeDealers.length() ; i++) {
	    	  response.getData().getExcludeDealers().add(jsonExcludeDealers.getString(i));
	      }
      }

      JSONArray jsonExceptions = jsonData.optJSONArray(CSALGOREST_JSON_KEY_EXCEPTIONS);
      if (jsonExceptions != null) {
    	  response.getData().setExceptions(new ArrayList<>());
    	  for (int i = 0 ; i < jsonExceptions.length() ; i++) {
    		  JSONObject jsonExceptionMessage = jsonExceptions.getJSONObject(i);
    		  ExceptionMessageElement exceptionMessage = new ExceptionMessageElement();
    		  exceptionMessage.setExceptionMessage(jsonExceptionMessage.getString(CSALGOREST_JSON_KEY_EXCEPTION_MESSAGE));
    		  exceptionMessage.setExceptionCode(jsonExceptionMessage.getString(CSALGOREST_JSON_KEY_EXCEPTION_CODE));
    		  exceptionMessage.setExceptionSeverity(jsonExceptionMessage.getString(CSALGOREST_JSON_KEY_SEVERITY));
    		  response.getData().getExceptions().add(exceptionMessage);
    	  }
      }
      
      return response;
   }
   

   private class ServiceHeartbeatChecker implements Runnable {
      private boolean keepChecking = true;

      @Override
      public void run() {
         while (keepChecking) {
            try {
               // Heartbeat check must be done by call GET on the healtcheck URL
               String response = heartbeatClient.get().readEntity(String.class);
               JSONObject objResp = new JSONObject(response);
               if ("GREEN".equalsIgnoreCase(objResp.getJSONObject("data").getString("status"))) {
                  lastError = "";
                  available = true;
               } else {
                  List<Object> messages = objResp.getJSONObject("data").getJSONArray("messages").toList();
                  for (Object errMsg : messages) {
                     lastError += ((JSONObject)errMsg).getString("message") + "; ";
                     LOGGER.warn("CS Algo Service status: {}", ((JSONObject)errMsg).getString("message"));
                  }
                  available = false;
               }
               Thread.sleep(CHECK_PERIOD);
            } catch (Exception e) {
               LOGGER.error("Error while trying to check connection status: ", e);
               lastError = "Error while trying to check connection status: timeout";
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
