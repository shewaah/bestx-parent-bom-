
/*
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

package it.softsolutions.bestx;

import java.util.Date;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import it.softsolutions.bestx.dao.DatabaseStatusDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.jsscommon.versions.VersionsChecker;
import net.sf.ehcache.CacheManager;

/**
 *
 *
 * Project Name : bestxengine-common
 * First created by: anna.cochetti
 * Creation date: 16 nov 2015
 * 
 **/

public class EngineControllerImpl extends EngineController {

	public EngineControllerImpl() {
		super();
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(EngineControllerImpl.class);
	private SessionFactory sessionFactory;
	private MifidConfig mifidConfig;
	private RegulatedMktIsinsLoader regulatedMktIsinsLoader;
	private DatabaseStatusDao databaseStatusDao;
	private boolean debugMode = false;
	private JdbcTemplate jdbcTemplate;

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void setMifidConfig(MifidConfig mifidConfig) {
	    this.mifidConfig = mifidConfig;
	}

	public void setRegulatedMktIsinsLoader(RegulatedMktIsinsLoader regulatedMktIsinsLoader) {
	    this.regulatedMktIsinsLoader = regulatedMktIsinsLoader;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
	    this.sessionFactory = sessionFactory;
	}

	public void init() throws BestXException {
	        checkPreRequisites();
		    String expectedTag = null;
	        VersionsChecker vcheck = null;
	        boolean res = false;
	        try {
				vcheck = new VersionsChecker("BESTX.properties");
			} catch (Exception e1) {
				LOGGER.error("File BESTX.properties not present or parameter version.db not set");
				e1.printStackTrace();
				System.exit(-1);
			}
	        if(vcheck == null) {
				LOGGER.error("File BESTX.properties not present or parameter version.db not set");
				System.exit(-1);
	        }
	     // [BXMNT-418]
	        try {
				res = vcheck.checkDbVersion(jdbcTemplate.getDataSource().getConnection());
				expectedTag = vcheck.getTagDb();
        	} catch(Exception e) {
        		try {
        			if (!res)
        				LOGGER.error("Expected tag {} not found in table DATABASECHANGELOG. Exit application", expectedTag);
		        } catch (Exception e1) {
		        	System.exit(-1);
        	}
	
	        // [BXMNT-417] Info da mettere in testa al log di bestxengine
		        String databaseStatus = databaseStatusDao.getDatabaseStatus();
		        LOGGER.info("[STATISTICS] {}", databaseStatus);
		        
		    	String[] tableNames = new String[] { "Rfq_Order", "ExecutionReport", "Attempt", "Book", "MarketExecutionReport", "MarketOrder", "MarketSecurityStatus", "Operation", "OperationBinding", "OperationState", "PriceTable", "Proposal", "RankingHistory", "TabHistoryOrdini", "TabHistoryStati", "TabOrderFill", "TabTentativi", "TentativiStatoMercato" };
		    	
		    	for (int i = 0; i < tableNames.length; i++) {
			        Integer count = databaseStatusDao.selectCountAs(tableNames[i]);
		        	LOGGER.info("[STATISTICS] {}. {} = {} rows", i + 1, tableNames[i], count);
		        }
		        
		    	Date oldestRecordInPriceTable = databaseStatusDao.selectOldestRecordInPriceTable();
		    	LOGGER.info("[STATISTICS] Oldest record in pricetable = {}", oldestRecordInPriceTable);
		    	
		    	Date oldestRecordInProposal = databaseStatusDao.selectOldestRecordInProposal();
		    	LOGGER.info("[STATISTICS] Oldest record in proposal = {}", oldestRecordInProposal);

	        }
	    }


	@Override
	public void reloadConfiguration() {
	    LOGGER.debug("");
	    
	    checkPreRequisites();
	    boolean success = true;
	    LOGGER.info("Reload General configuration");
	    try {
	        mifidConfig.init();
	    } catch (Exception e) {
	        LOGGER.error("An error occurred while reloading General configuration: {}", e.getMessage(), e);
	        success = false;
	    }
	    LOGGER.info("Reload Market Isin Loader");
	    try {
	        regulatedMktIsinsLoader.init();
	    } catch (Exception e) {
	        LOGGER.error("An error occurred while reloading Market Isin Loader: {}", e.getMessage(), e);
	        success = false;
	    }
	    LOGGER.info("Evicts all second level cache hibernate entites");
	    try {
	        // ehCache eviction
	        CacheManager.getInstance().clearAll();
	        
	        // hibernateCache eviction
	        sessionFactory.getCache().evictEntityRegions();
	    } catch (Exception e) {
	        LOGGER.error("Error evicting 2nd level hibernate cache entities: {}", e.getMessage(), e);
	        success = false;
	    }
	
	    LOGGER.info(success ? "Configuration reloaded successfully" : "There were problems while reloading configuration");
	}

	private void checkPreRequisites() throws ObjectNotInitializedException {
		if (sessionFactory == null) {
	        throw new ObjectNotInitializedException("SessionFactory not set");
	    }
		if (jdbcTemplate == null) {
	        throw new ObjectNotInitializedException("JdbcTemplate not set");
	    }
	    if (mifidConfig == null) {
	        throw new ObjectNotInitializedException("Mifid configuration not set");
	    }
	    if (regulatedMktIsinsLoader == null) {
	        throw new ObjectNotInitializedException("regulatedMktIsinsLoader not set");
	    }
	}

	/**
	 * @param databaseStatusDao the databaseStatusDao to set
	 */
	public void setDatabaseStatusDao(DatabaseStatusDao databaseStatusDao) {
		this.databaseStatusDao = databaseStatusDao;
	}

	public boolean getDebugMode() {
		return debugMode;
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

}