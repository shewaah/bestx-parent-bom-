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

import static org.junit.Assert.assertNotNull;
import it.softsolutions.bestx.mq.MQConfig;

import java.util.NoSuchElementException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-mq-service 
 * First created by: davide.rossoni 
 * Creation date: 20/feb/2013 
 * 
 **/
public class GRDLiteConfigTest {

    @Test(expected = IllegalArgumentException.class)
    public void fromConfigurationNull(){
        MQConfig.fromConfiguration(null);
    }
    
    @Test(expected = NoSuchElementException.class)
    public void fromConfigurationInvalid(){
        Configuration configuration = new PropertiesConfiguration();
        MQConfig.fromConfiguration(configuration);
    }
    
    @Test
    public void fromConfigurationValid() throws ConfigurationException{
        Configuration configuration = new PropertiesConfiguration("BogusGRDLite.properties");
        MQConfig grdLiteConfig = MQConfig.fromConfiguration(configuration.subset("mq"));
        
        assertNotNull(grdLiteConfig.getHost());
        assertNotNull(grdLiteConfig.getPort());
        assertNotNull(grdLiteConfig.getTransportType());
        assertNotNull(grdLiteConfig.getQueueManager());
        assertNotNull(grdLiteConfig.getChannel());
        assertNotNull(grdLiteConfig.getTransacted());
        assertNotNull(grdLiteConfig.getAcknowledge());
        assertNotNull(grdLiteConfig.getPublisherQueue());
        assertNotNull(grdLiteConfig.getSubscriberQueue());
        assertNotNull(grdLiteConfig.getExpiry());
        
        grdLiteConfig.toString();
    }
    
}
