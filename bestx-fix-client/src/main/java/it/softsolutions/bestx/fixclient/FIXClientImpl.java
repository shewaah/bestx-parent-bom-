/*
 * Copyright 1997-2012 SoftSolutions! srl 
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
package it.softsolutions.bestx.fixclient;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.fix.BXNewOrderSingle;
import it.softsolutions.bestx.fix.BXOrderCancelRequest;
import it.softsolutions.bestx.fix.field.ApplVerID;
import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.DefaultMessageFactory;
import quickfix.Initiator;
import quickfix.LogFactory;
import quickfix.MemoryStoreFactory;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.SLF4JLogFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.ThreadedSocketInitiator;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-fix-client 
 * First created by: davide.rossoni 
 * Creation date: Jun 11, 2012 
 * 
 **/
public class FIXClientImpl implements FIXClient, FIXInitiatorListener {
    
    private static final Logger logger = LoggerFactory.getLogger(FIXClientImpl.class);
    
    private SessionID sessionID;
    private ApplVerID applVerID;
    private Initiator initiator;
    
    @Override
    public void init(String filename, String foldername, FIXClientCallback fixClientCallback) throws FIXClientException {
    	try {
			init(new FileInputStream(new File(filename)), new File(foldername), fixClientCallback);
		} catch (FileNotFoundException e) {
			logger.error("Failed Fix connection initialization using configuration  " ,e);
			throw new FIXClientException(e);
		}
    }
    
    @Override
    public void init(InputStream settings, File settingFolder, FIXClientCallback fixClientCallback) throws FIXClientException {
        
        logger.debug("{}", fixClientCallback);

        try {// Initialization Fix connection using configuration file
            logger.info("Begin Fix connection initialization using configuration file");
            SessionSettings sessionSettings = new SessionSettings(settings);

            MessageStoreFactory messageStoreFactory = new MemoryStoreFactory();
            MessageFactory messageFactory = new DefaultMessageFactory();
            
            final String dataDictionaryFileName = sessionSettings.getDefaultProperties().getProperty("AppDataDictionary");
            DataDictionary dataDictionary = null;
            //Retrieving property file
    		File[] xmlList = settingFolder.listFiles(new FileFilter() {
    			@Override
    			public boolean accept(File pathname) {
     				if (pathname != null && pathname.getName() != null && (pathname.getName()).toUpperCase() != null && 
     						(pathname.getName()).toUpperCase().contains(dataDictionaryFileName.toUpperCase())) 
    					return true;
     				else
     					return false;
    			}
    		});
    		if (xmlList==null || xmlList.length==0) return;

    		try {
    		//Loading properties from file found
    		dataDictionary = new DataDictionary(new FileInputStream(xmlList[0]));
    		} catch (Exception e) { }
            
            LogFactory logFactory = new SLF4JLogFactory(sessionSettings);
            
            // Get a new FIX Application
            FIXInitiator fixInitiator = new FIXInitiator(this, dataDictionary, fixClientCallback);

            logger.info("End Fix connection initialization using configuration file");
            
            initiator = new ThreadedSocketInitiator(fixInitiator, messageStoreFactory, sessionSettings, logFactory, messageFactory);
            logger.info("Fix configuration loaded! Start connection/s");

            initiator.start();
            
        } catch (ConfigError e) {
            logger.error("Failed Fix connection initialization using configuration  " ,e);
            throw new FIXClientException(e);
        }
    }
    

    @Override
    public void manageNewOrderSingle(BXNewOrderSingle bxNewOrderSingle) throws FIXClientException {
        logger.debug("{}", bxNewOrderSingle);
        
        if (bxNewOrderSingle == null) {
            throw new IllegalArgumentException("bxNewOrderSingle can't be null");
        }
        
        Message newOrderSingle = null;
        
        switch (applVerID) {
        case FIX41:
            newOrderSingle = bxNewOrderSingle.toFIX41Message();
            break;
        case FIX42:
            newOrderSingle = bxNewOrderSingle.toFIX42Message();
            break;
        default:
            break;
        }

        try {
            Session.sendToTarget(newOrderSingle, sessionID);
        } catch (SessionNotFound e) {
            logger.error("{}", e.getMessage(), e);
        }
    }
    
    @Override
    public void manageOrderCancelRequest(BXOrderCancelRequest bxOrderCancelRequest) throws FIXClientException {
        logger.debug("{}", bxOrderCancelRequest);
        
        if (bxOrderCancelRequest == null) {
            throw new IllegalArgumentException("bxOrderCancelRequest can't be null");
        }
        
        Message orderCancelRequest = null;
        
        switch (applVerID) {
        case FIX41:
            orderCancelRequest = bxOrderCancelRequest.toFIX41Message();
            break;
        case FIX42:
            orderCancelRequest = bxOrderCancelRequest.toFIX42Message();
            break;
        default:
            break;
        }
        
        try {
            Session.sendToTarget(orderCancelRequest, sessionID);
        } catch (SessionNotFound e) {
            logger.error("{}", e.getMessage(), e);
        }
    }

    @Override
    public void onLogout(SessionID sessionID) {
        logger.info("{}", sessionID);
        this.sessionID = null;
    }
    
    @Override
    public void onLogon(SessionID sessionID) {
        logger.info("{}", sessionID);
        this.sessionID = sessionID;
        
        String beginString = sessionID.getBeginString();
        if (beginString.equals("FIX.4.1")) {
            applVerID = ApplVerID.FIX41;
        } else if (beginString.equals("FIX.4.2")) {
            applVerID = ApplVerID.FIX42;
        } else {
            logger.warn("Unexpected beginString [{}]", beginString);
            applVerID = ApplVerID.Unknown;
        }
    }


    @Override
    public void stop() {
        initiator.stop(true);   // true = force, do not wait for logout before disconnect
    }
    
}
