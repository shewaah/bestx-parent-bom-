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
package it.softsolutions.bestx.mq;

import it.softsolutions.bestx.grdlite.GRDLiteException;
import it.softsolutions.bestx.grdlite.LoadRequest;
import it.softsolutions.bestx.grdlite.SecurityType;

import javax.jms.JMSException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : mq-service First created by: ruggero.rizzo Creation date: 24/gen/2013
 * 
 **/
public class MyMQService {
    private static final Logger logger = LoggerFactory.getLogger(MyMQService.class); 
    private MQConnection grdLiteConnection;
    private Configuration configuration;
    
    public void init() throws Exception {
        configuration = new PropertiesConfiguration("BogusGRDLite.properties");
        logger.trace("Loaded configuration:\n{}", configuration != null ? ConfigurationUtils.toString(configuration) : configuration);
        Configuration queueConfig = configuration.subset("mq");
        if (queueConfig.isEmpty()) {
            throw new GRDLiteException("Empty configuration with 'mq' prefix");
        }        
        MQCallback grdLiteCallback = new MQCallback() {

            public void onResponse(String response) {
                System.out.println("Response: " + response);
            }

            public void onException(String message) {
                System.out.println(message);
            }
        };

        try {
            grdLiteConnection = BXMQConnectionFactory.getConnection(MQConfig.fromConfiguration(queueConfig), grdLiteCallback);
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    public void test() throws Exception {
        String messagesSchema = configuration.getString("grdlite.messagesSchema");
//        LoadRequest loadRequest = new LoadRequest(messagesSchema, SecurityType.ISIN, true, "TI0001976405", "TI0001976402", "TI0001976406", "TI0001976400");
//        grdLiteConnection.publish(loadRequest);
        LoadRequest loadRequest2 = new LoadRequest(messagesSchema, SecurityType.ISIN, false, "ZZ0001976418", "ZZ0001976412", "ZZ0001976419", "ZZ0001976410");
        grdLiteConnection.publish(loadRequest2);

    }

    public void stressTest() throws Exception {
        final int THREAD = 5;
        final int CYCLE = 10;
        final String messagesSchema = configuration.getString("grdlite.messagesSchema");
        for (int i = 0; i < THREAD; i++) {

            new Thread() {
                @Override
                public void run() {
                    for (int j = 0; j < CYCLE; j++) {
                        LoadRequest loadRequest = new LoadRequest(messagesSchema, SecurityType.ISIN, true, "TI0001976403", "IT0001976403", "IT0001976403", "IT0001976403");
                        try {
                            grdLiteConnection.publish(loadRequest);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }
            }.start();
        }
    }

    public static void main(String[] args) {
        try {

            MyMQService mqService = new MyMQService();
            mqService.init();
            mqService.test();
            
            do {
                Thread.sleep(10000);
            } while (true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
