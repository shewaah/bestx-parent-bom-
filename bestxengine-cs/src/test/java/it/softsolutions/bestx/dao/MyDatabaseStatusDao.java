/**
 * Copyright 1997-2015 SoftSolutions! srl 
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
package it.softsolutions.bestx.dao;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs
 * First created by: davide.rossoni
 * Creation date: 24/feb/2015
 * 
 */
public class MyDatabaseStatusDao {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(MyDatabaseStatusDao.class);
    
    private static DatabaseStatusDao databaseStatusDao;
    private static ClassPathXmlApplicationContext context;
    
    public void init() {
    	LOGGER.debug(""); 
    	
		context = new ClassPathXmlApplicationContext("cs-spring.xml");
		databaseStatusDao = (DatabaseStatusDao) context.getBean("sqlDatabaseStatusDao");
    }
    
    public void run() {
    	LOGGER.debug("");
    	
    	String databaseStatus = databaseStatusDao.getDatabaseStatus();
    	System.out.println("databaseStatus =" + databaseStatus);
    	
    	String[] tableNames = new String[] { "Rfq_Order", "ExecutionReport", "Attempt", "Book", "MarketExecutionReport", "MarketOrder", "MarketSecurityStatus", "Operation", "OperationBinding", "OperationState", "PriceTable", "Proposal", "RankingHistory", "TabHistoryOrdini", "TabHistoryStati", "TabOrderFill", "TabTentativi", "TentativiStatoMercato" };
    	
    	for (String tableName : tableNames) {
    		Integer count = databaseStatusDao.selectCountAs(tableName);
        	System.out.println("[" + tableName + "] " + count + " rows");
        }
        
    	Date oldestRecordInPriceTable = databaseStatusDao.selectOldestRecordInPriceTable();
    	System.out.println("oldestRecordInPriceTable = " + oldestRecordInPriceTable);
    	
    	Date oldestRecordInProposal = databaseStatusDao.selectOldestRecordInProposal();
    	System.out.println("oldestRecordInProposal = " + oldestRecordInProposal);
        
    }
    
    public static void main(String[] args) {
	    MyDatabaseStatusDao myDatabaseStatusDao = new MyDatabaseStatusDao();
	    myDatabaseStatusDao.init();
	    myDatabaseStatusDao.run();
    }

}
