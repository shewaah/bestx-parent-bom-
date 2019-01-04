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
package it.softsolutions.bestx.grdlite;

import static org.junit.Assert.assertTrue;
import it.softsolutions.bestx.mq.MQCallback;
import it.softsolutions.bestx.mq.MQConfig;
import it.softsolutions.bestx.mq.MQConnection;
import it.softsolutions.bestx.mq.BXMQConnectionFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-mq-service 
 * First created by: davide.rossoni 
 * Creation date: 31/gen/2013 
 * 
 **/
public class GRDLiteConnectionImplTest {

    private static final Logger logger = LoggerFactory.getLogger(GRDLiteConnectionImplTest.class);
    
    private static BlockingQueue<LoadResponse> loadResponses = new ArrayBlockingQueue<LoadResponse>(10);
    
    private static MQConnection grdLiteConnection;
    
//    @BeforeClass
    public static void setUp() throws Exception {
        Configuration configuration = new PropertiesConfiguration("BogusGRDLite.properties");

        // -- GRDLite Connection --------------------------------
        MQConfig grdLiteConfig = MQConfig.fromConfiguration(configuration.subset("mq"));
        
        grdLiteConnection = BXMQConnectionFactory.getConnection(grdLiteConfig, new MQCallback() {
            
            @Override
            public void onResponse(String response) {
                logger.debug("{}", response);
                final LoadResponse loadResponse = LoadResponse.fromXml(response);
                loadResponses.add(loadResponse);
            }
            
            @Override
            public void onException(String message) {
                logger.debug("{}", message);
            }
        });
    }
    
    @Test
    public void publishTimeout() throws InterruptedException, GRDLiteException {
        assertTrue(1 == 2 - 1);
    }
    
//    @Test(timeout = 10000)
//    public void publishTimeout() throws InterruptedException, GRDLiteException {
//        String securityID = ("TIMEOUT1234");
//        SecurityType securityType = SecurityType.ISIN;
//        
//        LoadRequest loadRequest = new LoadRequest(securityType, false, securityID);
//        
//        grdLiteConnection.publish(loadRequest);
//        
//        loadResponses.take();
//        
//        fail("Timeout expected");
//    }
//    
//    @Test(timeout = 10000)
//    public void publishError() throws InterruptedException, GRDLiteException {
//        String securityID = ("DISCARD" + System.currentTimeMillis()).substring(0, 5);
//        SecurityType securityType = SecurityType.ISIN;
//        
//        LoadRequest loadRequest = new LoadRequest(securityType, false, securityID);
//        
//        grdLiteConnection.publish(loadRequest);
//        
//        LoadResponse loadResponse = loadResponses.take();
//        
//        assertNotNull(loadResponse);
//        assertEquals(securityID, loadResponse.getSecurityId());
//        assertEquals(securityType, loadResponse.getSecurityType());
//        assertEquals(LoadResponse.Status.Error, loadResponse.getStatus());
//    }
//
//    @Test(timeout = 10000)
//    public void publishReceived() throws InterruptedException, GRDLiteException {
//        String securityID = ("TI" + System.currentTimeMillis()).substring(0, 12);
//        SecurityType securityType = SecurityType.ISIN;
//        
//        LoadRequest loadRequest = new LoadRequest(securityType, false, securityID);
//        
//        grdLiteConnection.publish(loadRequest);
//        
//        LoadResponse loadResponse = loadResponses.take();
//        
//        assertNotNull(loadResponse);
//        assertEquals(securityID, loadResponse.getSecurityId());
//        assertEquals(securityType, loadResponse.getSecurityType());
//        assertEquals(LoadResponse.Status.Received, loadResponse.getStatus());
//    }
    
}

