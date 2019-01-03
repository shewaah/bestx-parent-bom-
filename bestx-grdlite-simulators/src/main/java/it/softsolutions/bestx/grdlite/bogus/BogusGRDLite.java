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
package it.softsolutions.bestx.grdlite.bogus;

import it.softsolutions.bestx.grdlite.LoadRequest;
import it.softsolutions.bestx.grdlite.LoadResponse;
import it.softsolutions.bestx.grdlite.SecurityType;
import it.softsolutions.bestx.mq.MQConfig;
import it.softsolutions.bestx.mq.MQMessageListener;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Random;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mq.jms.MQQueue;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.mq.jms.MQQueueReceiver;
import com.ibm.mq.jms.MQQueueSender;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-mq-service First created by: davide.rossoni Creation date: 30/gen/2013
 * 
 **/
public class BogusGRDLite implements MQMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BogusGRDLite.class);

    private Random random = new Random(System.currentTimeMillis());

//    private Configuration configuration;
    private Connection jdbcConnection;
    private QueueConnection queueConnection;

    private QueueSession publisherQueueSession;
    private QueueSession subscriberQueueSession;
    
    private MQQueueReceiver queueReceiver;
    private MQQueueSender queueSender; 
    
    private int discardOneOf;
    private int randomSleepBeforeReply;
    
    private void init() throws ConfigurationException, SQLException, ClassNotFoundException, JMSException {
        Configuration configuration = new PropertiesConfiguration("BogusGRDLite.properties");
        LOGGER.info("Loaded configuration:\n{}", ConfigurationUtils.toString(configuration));
        
        // Configuration queueConfig = configuration.subset("mq") 

        discardOneOf = configuration.getInt("discardOneOf");
        randomSleepBeforeReply = configuration.getInt("randomSleepBeforeReply");
        
        // -- JDBC Connection -------------------------------- 
        String jdbcDriverClass = configuration.getString("jdbc.driver_class");
        String jdbcURL = configuration.getString("jdbc.url");
        String jdbcUsername = configuration.getString("jdbc.username");
        String jdbcPassword = configuration.getString("jdbc.password");
        
        Class.forName(jdbcDriverClass);
        
        jdbcConnection = DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
        
        // -- GRDLite Connection --------------------------------
        MQConfig grdLiteConfig = MQConfig.fromConfiguration(configuration.subset("mq"));
        
        MQQueueConnectionFactory mqQueueConnectionFactory = new MQQueueConnectionFactory();
        mqQueueConnectionFactory.setHostName(grdLiteConfig.getHost());
        mqQueueConnectionFactory.setPort(grdLiteConfig.getPort());
        mqQueueConnectionFactory.setTransportType(grdLiteConfig.getTransportType());
        mqQueueConnectionFactory.setQueueManager(grdLiteConfig.getQueueManager());
        mqQueueConnectionFactory.setChannel(grdLiteConfig.getChannel());
        
        queueConnection = mqQueueConnectionFactory.createQueueConnection(grdLiteConfig.getUsername(), grdLiteConfig.getPassword());
        publisherQueueSession = queueConnection.createQueueSession(grdLiteConfig.getTransacted(), grdLiteConfig.getAcknowledge());
        subscriberQueueSession = queueConnection.createQueueSession(grdLiteConfig.getTransacted(), grdLiteConfig.getAcknowledge());
        
        MQQueue publisherQueue = (MQQueue) publisherQueueSession.createQueue(grdLiteConfig.getPublisherQueue());
        MQQueue subscriberQueue = (MQQueue) subscriberQueueSession.createQueue(grdLiteConfig.getSubscriberQueue());
        
        queueSender = (MQQueueSender) publisherQueueSession.createSender(subscriberQueue);
        queueReceiver = (MQQueueReceiver) subscriberQueueSession.createReceiver(publisherQueue);
        queueReceiver.setMessageListener(this);
    }

    private void start() throws JMSException {
        LOGGER.debug("");

        queueConnection.start();
        
    }

    /**
     * Loads instruments data into BestX database
     * 
     * ** InstrumentsTable **************************
     * - ISIN (Key) ID005 ISIN code
     * - Currency Code DS004 ISO Currency code. Currency non ISO
     * - MinimumPiece MM023 Minimum size accepted by BestX!
     * - UpdateDate - Date of last update of this instrument
     * - CountryCode DS458 ISO Country code (2 char)
     * - Description DS156 Instrument description
     * - InInventory - Always 1 for Instruments loaded
     * - StdSettlementDays DAYS_TO_SETTLE
     *
     * ** InstrumentAttributes **********************
     * - ISIN (Key) ID005 ISIN code
     * - internal - Always 0 for CS installation
     * - PortfolioId - Always 0 for CS installation
     * 
     * @param securityID
     * @throws SQLException 
     */
    private int loadInstrumentToDB(String securityID, String currency, double minimumPiece, String countryCode, String description, Boolean inInventory, Integer stdSettlementDays, Date bbSettlementDate) throws SQLException {
        LOGGER.debug("{}, {}, {}, {}, {}, {}, {}, {}", securityID, currency, minimumPiece, countryCode, description, inInventory, stdSettlementDays, bbSettlementDate);
        int res = 0;
        
        PreparedStatement pstmt = jdbcConnection.prepareStatement("INSERT INTO InstrumentsTable (ISIN, CurrencyCode, MinimumPiece, UpdateDate, CountryCode, Description, InInventory, StdSettlementDays, bbSettlementDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
        try {
            pstmt.setString(1, securityID);
            pstmt.setString(2, currency);
            pstmt.setDouble(3, minimumPiece);
            pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(5, countryCode);
            pstmt.setString(6, description);
            pstmt.setBoolean(7, inInventory);
            pstmt.setInt(8, stdSettlementDays);
            pstmt.setDate(9, bbSettlementDate);
            try
            {
                res = pstmt.executeUpdate();
            } catch (SQLException se) {
                return updateInstrumentToDB(securityID, currency, minimumPiece, countryCode, description, inInventory, stdSettlementDays, bbSettlementDate);
            }
        } finally {
            pstmt.close();
        }
        
        PreparedStatement pstmt2 = jdbcConnection.prepareStatement("INSERT INTO InstrumentAttributes (ISIN, internal, PortfolioId) VALUES (?, ?, ?)");
        try {
            pstmt2.setString(1, securityID);
            pstmt2.setBoolean(2, false);
            pstmt2.setInt(3, 0);
            
            res = pstmt2.executeUpdate();
        } finally {
            pstmt2.close();
        }
        
        return res;
    }
    
    private int updateInstrumentToDB(String securityID, String currency, double minimumPiece, String countryCode, String description, Boolean inInventory, Integer stdSettlementDays, Date bbSettlementDate) throws SQLException {
        LOGGER.debug("{}, {}, {}, {}, {}, {}, {}, {}", securityID, currency, minimumPiece, countryCode, description, inInventory, stdSettlementDays,bbSettlementDate);
        int res = 0;
        
        PreparedStatement pstmt = jdbcConnection.prepareStatement("UPDATE InstrumentsTable SET CurrencyCode=?, MinimumPiece=?, UpdateDate=?, CountryCode=?, Description=?, InInventory=?, StdSettlementDays=?, BBSettlementDate=? WHERE ISIN=?");
        try {
            pstmt.setString(1, currency);
            pstmt.setDouble(2, minimumPiece);
            pstmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(4, countryCode);
            pstmt.setString(5, description);
            pstmt.setBoolean(6, inInventory);
            pstmt.setInt(7, stdSettlementDays);
            pstmt.setString(8, securityID);
            pstmt.setDate(9, bbSettlementDate);
            
            res = pstmt.executeUpdate();
        } finally {
            pstmt.close();
        }
        
        return res;
    }

    /**
     * @param securityIDs
     * @throws InterruptedException 
     */
    private void processInstruments(boolean initialLoad, SecurityType securityType, String... securityIDs) throws InterruptedException {
        LOGGER.trace("[{}, {}] {}", initialLoad, securityType, securityIDs != null ? Arrays.asList(securityIDs) : securityIDs);

        if (securityIDs == null) {
            throw new IllegalArgumentException("null param");
        }

        // discard one of 5 (configurable)
        for (String securityID : securityIDs) {
            LoadResponse.Status responseStatus;
            
            if (random.nextInt(discardOneOf) != 0 && !securityID.contains("DISCARD")) {

                try {
                    String currency = "EUR";
                    double minimumPiece = 1000.0;
                    String countryCode = "IT";
                    String description = "Description for " + securityID;
                    Boolean inInventory = true;
                    Integer stdSettlementDays = 3;
                    Date bbSettlementDate = new Date(DateUtils.addDays(new java.util.Date(), 3).getTime());
                    
                    int res = 0;
                    if (initialLoad) {
                        res = updateInstrumentToDB(securityID, currency, minimumPiece, countryCode, description, inInventory, stdSettlementDays, bbSettlementDate);
                        LOGGER.debug("Instrument {}: database UPDATE {}", securityID, res == 1 ? "done" : "failed");
                    } else {
                        res = loadInstrumentToDB(securityID, currency, minimumPiece, countryCode, description, inInventory, stdSettlementDays, bbSettlementDate);
                        LOGGER.debug("Instrument {}: database INSERT {}", securityID, res == 1 ? "done" : "failed");
                    }
                    
                    responseStatus = res == 1 ? LoadResponse.Status.Received : LoadResponse.Status.Error;
                } catch (Exception e) {
                    LOGGER.error("Error processing securityID {}: {}", securityID, e.getMessage());
                    responseStatus = LoadResponse.Status.Error;
                }
                
            } else {
            	LOGGER.info("Replace securityID [{}] with []", securityID);
            	securityID = ""; 
                LOGGER.info("Random discarded securityID [{}]", securityID);
                responseStatus = LoadResponse.Status.Error;
            }
            
            if (initialLoad) {
                LOGGER.debug("Skip sending acknoledge for securityID {}, responseStatus = {}", securityID, responseStatus);
            } else {
                int sleepMillis = random.nextInt(randomSleepBeforeReply); 
                if (securityID.contains("TIMEOUT")) {
                    sleepMillis = 20000;
                }
                
                LOGGER.info("Sleep for {} millis...", sleepMillis);
                Thread.sleep(sleepMillis);
                
                sendAcknoledge(securityType, responseStatus, securityID);
            }
        }
    }

    /**
     * Sends acknowledgment to BestX engine
     * 
     * @param securityID
     */
    private void sendAcknoledge(SecurityType securityType, LoadResponse.Status status, String securityID) {
        LOGGER.debug("{}, {}", securityID, status);
        
        LoadResponse loadResponse = new LoadResponse(securityType, status, securityID);
        
        TextMessage message = null;
        try {
            message = publisherQueueSession.createTextMessage();
            String xml = loadResponse.toXml();
            message.setText(xml);
            LOGGER.info("message = {}", xml);
            queueSender.send(message);
            publisherQueueSession.commit();
        } catch (JMSException e) {
            LOGGER.error("Error publishing message [{}] : {}", message, e.getMessage(), e);
        }
        
//        queueSender.send(new message)
    }
    
    @Override
    public void onMessage(Message message) {
//        LOGGER.trace("{}", message);
        
        try {
            TextMessage textMsg = (TextMessage) message;
            String text = textMsg.getText();
            LOGGER.debug("{}", text);
            LoadRequest loadRequest = LoadRequest.fromXml(text);
            LOGGER.debug("{}", loadRequest);
            
            boolean initialLoad = loadRequest.isInitialLoad();
            SecurityType securityType = loadRequest.getSecurityType();
            String[] securityIDs = loadRequest.getSecurityIDs();
            
            processInstruments(initialLoad, securityType, securityIDs);

            subscriberQueueSession.commit();
        } catch (Exception e) {
            LOGGER.error("Error reading message [{}]: {}", message, e.getMessage(), e);
        }
    }

    @Override
    public void acknowledge(Message message) throws JMSException {
        LOGGER.debug("{}", message);
        
    }


    public static void main(String[] args) throws ConfigurationException, SQLException, ClassNotFoundException, JMSException, InterruptedException {
        BogusGRDLite bogusGRDLite = new BogusGRDLite();
        bogusGRDLite.init();
        bogusGRDLite.start();
        
        do {
            Thread.sleep(10000);
        } while (true);
    }

}
