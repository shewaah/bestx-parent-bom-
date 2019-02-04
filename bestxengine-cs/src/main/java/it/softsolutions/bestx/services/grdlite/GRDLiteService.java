/*
 * Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx.services.grdlite;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jms.JMSException;

import org.apache.commons.configuration.ConfigurationException;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationRegistry;
import it.softsolutions.bestx.connections.BaseOperatorConsoleAdapter;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;
import it.softsolutions.bestx.grdlite.LoadRequest;
import it.softsolutions.bestx.grdlite.LoadResponse;
import it.softsolutions.bestx.grdlite.SecurityType;
import it.softsolutions.bestx.mq.BXMQConnectionFactory;
import it.softsolutions.bestx.mq.MQCallback;
import it.softsolutions.bestx.mq.MQConfig;
import it.softsolutions.bestx.mq.MQConfigHelper;
import it.softsolutions.bestx.mq.MQConnection;
import it.softsolutions.bestx.services.customservice.CustomService;
import it.softsolutions.bestx.services.customservice.CustomServiceException;

/**
 * 
 * Purpose: send instrument requests towards GRDLite
 * 
 * Project Name : bestxengine-cs First created by: ruggero.rizzo Creation date: 31/gen/2013
 * 
 **/
public class GRDLiteService extends BaseOperatorConsoleAdapter implements MQCallback, CustomService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GRDLiteService.class);
    private static final String GRDLITE_CONFIG_FILE = "MQServices.properties";
    private static final String GRDLITE_MQ_CONFIG_PREFIX = "grdlite";
    
    private static int grdLiteConnectionTries = 1;
    private MQConnection grdLiteConnection = null;
    private Map<String, List<String>> callbackMap = new TreeMap<String, List<String>>();
    private OperationRegistry operationRegistry;
    private boolean active = true;
    private boolean available = false;
    private int reconnectionMaxTries = -1;
    private StandardPBEStringEncryptor encryptor;
    
    public GRDLiteService(OperationRegistry operationRegistry) throws CustomServiceException {
        if (operationRegistry == null) {
            throw new IllegalArgumentException("null operationRegistry");
        }
        this.operationRegistry = operationRegistry;
    }
    
    public void init() throws BestXException {
       try {
          connectGRDLite();
      } catch (ConfigurationException e) {
          String message= "Cannot load the GRDLite configuration: " + e.getMessage();
          LOGGER.error(message, e);
          available = false;
          throw new BestXException(message);
      }
    }
    
    @Override
    public void sendRequest(String operationId, boolean initialLoad, String securityId) throws CustomServiceException {
        if (operationId == null || securityId == null) {
            throw new IllegalArgumentException("null parameter");
        }
        
        // add the operationId to those mapped to the securityId
        // we will send the request only if THERE IS NOT already one for the given securityId
        List<String> operations = callbackMap.get(securityId);
        
        if (operations == null) {
            operations = new CopyOnWriteArrayList<String>();
            callbackMap.put(securityId, operations);
            
            LoadRequest loadRequest = new LoadRequest(SecurityType.ISIN, initialLoad, securityId);
            LOGGER.info("GRDLite request sent {} for operation {}", loadRequest, operationId);
            try {
				grdLiteConnection.publish(loadRequest);
			} catch (Exception e) {
				throw new CustomServiceException(e);
			}
        } else {
            LOGGER.info("Skip sending GRDLite request for operation {}: a LoadRequest for the specified ISIN {} is already in progress", operationId, securityId);
        }
        
        operations.add(operationId);
    }

    protected void connectGRDLite() throws ConfigurationException {
        LOGGER.debug("GRDLite connection attempt {}", grdLiteConnectionTries);
        if (reconnectionMaxTries == -1 || grdLiteConnectionTries <= reconnectionMaxTries) {
            final MQConfig grdLiteConfig = MQConfigHelper.getConfigurationFromFile(GRDLITE_CONFIG_FILE, GRDLITE_MQ_CONFIG_PREFIX, encryptor);

            try {
                grdLiteConnection = BXMQConnectionFactory.getConnection(grdLiteConfig, this);
                available = true;
                LOGGER.debug("GRDLite connection attempt {} succeded", grdLiteConnectionTries);
            } catch (JMSException e) {
                LOGGER.info("Cannot connect to the JMS queue: {}", e.getMessage(), e);
                retryGRDLiteConnection();
            } catch (Exception ge) {
                LOGGER.info("Cannot connect to the MQ: {}", ge.getMessage(), ge);
                retryGRDLiteConnection();
            } finally {
                grdLiteConnectionTries++;
            }
        }
    }
    
    private void retryGRDLiteConnection() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    connectGRDLite();
                } catch (Exception e) {
                    LOGGER.error("{}", e.getMessage(), e);
                }
            }
        }, 5000);
    }
    
    @Override
    public void onResponse(String response) {
       LOGGER.debug("Response message received: {}", response);

       LoadResponse loadResponse = null;
       try {
          loadResponse = LoadResponse.fromXml(response);
       } catch (Exception e) {
          LOGGER.error("Error while managing the response [{}]: {}", loadResponse, e.getMessage(), e);
       }

       if (loadResponse != null) {
          LOGGER.info("GRDLite response received: {}", loadResponse);
          List<String> operationIds = callbackMap.remove(loadResponse.getSecurityId());

          if (operationIds != null) {
             for (String operationId : operationIds) {
                try {
                   Operation operation = operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, operationId);
                   operation.onCustomServiceResponse((loadResponse.getStatus() == LoadResponse.Status.Error), loadResponse.getSecurityId());
                } catch (OperationNotExistingException e) {
                   LOGGER.info("Cannot find the operation with id {}. This is normal if this answer has been triggered by a Price Discovery Request on MQ.", operationId);
                   continue;
                } catch (BestXException e) {
                   LOGGER.error("Error while loading the operation with id {}", operationId, e);
                   continue;
                }
             }
          } else {
             LOGGER.warn("Unexpected securityID, no operationIDs found for [{}]", loadResponse.getSecurityId());
          }
       } else {
          LOGGER.warn("Unexpected message received, cannot create loadResponse from received message: {}", response);          
       }
    }

    @Override
    public void onException(String message) {
        LOGGER.info("Error received from GRDLite: {}", message);
        available = false;
        grdLiteConnectionTries = 1;
        retryGRDLiteConnection();
    }

    @Override
    public boolean isActive() {
        if (!active) {
            LOGGER.info("GRDLite service not active. Check the configuration.");
        }
        return active;
    }

    /**
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    /**
     * @param reconnectionMaxTries the reconnectionMaxTries to set
     */
    public void setReconnectionMaxTries(int reconnectionMaxTries) {
        this.reconnectionMaxTries = reconnectionMaxTries;
    }

    public StandardPBEStringEncryptor getEncryptor() {
        return encryptor;
    }

    public void setEncryptor(StandardPBEStringEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public void resetRequest(String securityId, String operationId) {
        if (securityId != null) {
        	boolean removed = false;
        	synchronized (callbackMap) {
        		List<String> operations = callbackMap.get(securityId);
        		if(operations != null) {
	        		removed = operations.remove(operationId);
	        		if(operations.isEmpty())
	        			callbackMap.remove(securityId);
        		}
        	}
        	if(removed)
        		LOGGER.info("Operation's callback removed for securityID {}", securityId);
        	else
        		LOGGER.info("Operation's callback not found for securityID {}", securityId);
        } else {
            LOGGER.debug("SecurityID is null");
        }
    }

   @Override
   public void connect() {
      try {
         connectGRDLite();
     } catch (ConfigurationException e) {
         String message= "Cannot load the GRDLite configuration: " + e.getMessage();
         LOGGER.error(message, e);
         available = false;
     }
   }

   @Override
   public void disconnect() {
      LOGGER.info("Closing Connection to MQ");
      if (grdLiteConnection != null) {
         try {
            grdLiteConnection.close();
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

   @Override
   public boolean isConnected() {
      return active && available;
   }
}
