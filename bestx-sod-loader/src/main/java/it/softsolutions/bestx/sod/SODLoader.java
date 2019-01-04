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
package it.softsolutions.bestx.sod;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.jms.JMSException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.grdlite.GRDLiteException;
import it.softsolutions.bestx.grdlite.LoadRequest;
import it.softsolutions.bestx.grdlite.SecurityType;
import it.softsolutions.bestx.mq.BXMQConnectionFactory;
import it.softsolutions.bestx.mq.MQCallback;
import it.softsolutions.bestx.mq.MQConfig;
import it.softsolutions.bestx.mq.MQConfigHelper;
import it.softsolutions.bestx.mq.MQConnection;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-sod-loader First created by: davide.rossoni Creation date: 30/gen/2013
 * 
 **/
public final class SODLoader {

	private static final Logger logger = LoggerFactory.getLogger(SODLoader.class);
	private static final int DEFAULT_MAX_SECURITIES_PER_MESSAGE = 1000;
	private static final int DEFAULT_MAX_GRDLITE_CONN_RETRIES = -1;
	private static final int DEFAULT_MAX_DB_CONN_RETRIES = -1;
	private Connection jdbcConnection;
	private MQConnection grdLiteConnection;
	private int maxSecuritiesPerMessage;
	private Boolean sendInPackets = false;
	private static int dbConnectionTries = 1;
	private static int grdLiteConnectionTries = 1;
	private boolean dbConnected = false;
	private boolean grdLiteConnected = false;
	private boolean stop = false;
	private int grdLiteReconnectionMaxTries;
	private int dbReconnectionMaxTries;
	private StandardPBEStringEncryptor encryptor;

	// Sonar- Hide Utility Class Constructor: utility classes should not have a public or default constructor.
	private SODLoader() {
	}

	private void init() throws ConfigurationException, SQLException, ClassNotFoundException, JMSException, GRDLiteException {
		Configuration configuration = new PropertiesConfiguration("bestx-sod-loader.properties");
		logger.info("Loaded configuration:\n{}", configuration != null ? ConfigurationUtils.toString(configuration) : configuration);

		maxSecuritiesPerMessage = configuration.getInteger("sod.packetSize", DEFAULT_MAX_SECURITIES_PER_MESSAGE);
		sendInPackets = configuration.getBoolean("sod.sendInPackets");
		grdLiteReconnectionMaxTries = configuration.getInteger("sod.grdLiteReconnectionMaxTries", DEFAULT_MAX_GRDLITE_CONN_RETRIES);
		dbReconnectionMaxTries = configuration.getInteger("sod.dbReconnectionMaxTries", DEFAULT_MAX_DB_CONN_RETRIES);

		String pbeAlgorithm = configuration.getString("sod.pbe.algorithm");
		String pbePassword = configuration.getString("sod.pbe.password");
		encryptor = new StandardPBEStringEncryptor();
		encryptor.setPassword(pbePassword);
		encryptor.setAlgorithm(pbeAlgorithm);

		connectDb();
		try {
			connectGRDLite();
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}

	protected void connectDb() throws ConfigurationException, ClassNotFoundException {
		logger.info("Database connection attempt {}", dbConnectionTries);
		if (dbReconnectionMaxTries == -1 || grdLiteConnectionTries <= dbReconnectionMaxTries) {
			Configuration configuration = new PropertiesConfiguration("bestx-sod-loader.properties");

			// -- JDBC Connection --------------------------------
			String jdbcDriverClass = configuration.getString("sod.jdbc.driver_class");
			String jdbcURL = configuration.getString("sod.jdbc.url");
			String jdbcUsername = configuration.getString("sod.jdbc.username");
			String jdbcPassword = decryptPassword(configuration.getString("sod.jdbc.password"));

			Class.forName(jdbcDriverClass);

			try {
				jdbcConnection = DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
				dbConnected = true;
				logger.info("Database connection attempt {} succeded", dbConnectionTries);
			} catch (SQLException e) {
				logger.error("Cannot connect to the database : {}", e.getMessage(), e);
				retryDBConnection();
			} finally {
				dbConnectionTries++;
			}
		} else {
			logger.error("Maximum number of database connection attempts reached, stop trying.");
			stop = true;
		}
	}

	protected void connectGRDLite() throws Exception {
		logger.info("GRDLite connection attempt {}", grdLiteConnectionTries);
		if (grdLiteReconnectionMaxTries == -1 || grdLiteConnectionTries <= grdLiteReconnectionMaxTries) {
			// -- GRDLite Connection --------------------------------
			MQConfig grdLiteConfig = MQConfigHelper.getConfigurationFromFile("bestx-sod-loader.properties", "sod.mq");

			// Try to decrypt fields with encrypted passwords
			String keyStorePassword = decryptPassword(grdLiteConfig.getSslKeyStorePassword());
			grdLiteConfig.setSslKeyStorePassword(keyStorePassword);
			String trustStorePassword = decryptPassword(grdLiteConfig.getSslTrustStorePassword());
			grdLiteConfig.setSslTrustStorePassword(trustStorePassword);

			try {
				String userName= System.getProperty("user.name");
				grdLiteConnection = BXMQConnectionFactory.getConnection(grdLiteConfig, new GRDLiteCallbackImpl());
				grdLiteConnected = true;
				logger.info("GRDLite connection attempt {} succeded", grdLiteConnectionTries);
			} catch (JMSException e) {
				logger.error("Cannot connect to the JMS queue: {}", e.getMessage(), e); 
				e.printStackTrace();
				retryGRDLiteConnection();
			} catch (GRDLiteException ge) {
				logger.error("Cannot connect to the GRDLite : {}", ge.getMessage(), ge);
				retryGRDLiteConnection();
			} finally {
				grdLiteConnectionTries++;
			}
		} else {
			logger.error("Maximum number of GRDLite connection attempts reached, stop trying.");
			stop = true;
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
					e.printStackTrace();
				}
			}
		}, 5000);
	}

	protected void retryDBConnection() {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					connectDb();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 5000);
	}

	private void run() throws Exception {
		logger.info("");

		while(!connected() && !stopExecution()) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (stopExecution()) {
			throw new Exception("One or both the connections failed to initialize correctly, cannot execute SODLoader.");
		}
		boolean finished = false;
		logger.info("SODLoader, starting execution");
		int i = 1;
		List<String> res = null;
		while(!finished) {

			if (sendInPackets) {
				do {
					res = loadInstruments(i, maxSecuritiesPerMessage);

					// Just 4 testing
					//                for (int j = 0; j < res.size(); j++) {
					//                    logger.debug("{}.{} {}", i - 1, j + 1, res.get(j));
					//                }

					if (res.size() > 0) {
						try {
							LoadRequest loadRequest = new LoadRequest(SecurityType.ISIN, true, res.toArray(new String[0]));
							grdLiteConnection.publish(loadRequest);
							// incremented only when publish succeeded
							i++;
						} catch(GRDLiteException e){
							logger.info("Exception when publishing request to GRDLite");
						}
					}
				} while (res.size() > 0);
				finished = true;
			} else {
				try {
					res = loadInstruments();
					LoadRequest loadRequest = new LoadRequest(SecurityType.ISIN, true, res.toArray(new String[0]));
					grdLiteConnection.publish(loadRequest);
					finished = true;
				} catch(GRDLiteException e){
					logger.info("Exception when publishing request to GRDLite");
				}
			}
		}
	}

	/**
	 * Send paged load request
	 * 
	 * @param pageNumber
	 *            page to load
	 * @param pageSize
	 *            page size
	 * @return a list of instruments to request
	 * @throws SQLException
	 *             in case of errors
	 */
	private List<String> loadInstruments(int pageNumber, int pageSize) throws SQLException {
		logger.debug("{}, {}", pageNumber, pageSize);

		List<String> res = new ArrayList<String>();

		String sql = "SELECT TOP (?) isin FROM" + "(" + "SELECT ROW_NUMBER() OVER (ORDER BY isin) As rowID, isin FROM InstrumentsTable" + ") TmpTable " + "WHERE TmpTable.RowID > ((?-1)*?)";

		PreparedStatement pstmt = jdbcConnection.prepareStatement(sql);
		pstmt.setInt(1, pageSize);
		pstmt.setInt(2, pageNumber);
		pstmt.setInt(3, pageSize);

		ResultSet resultSet = null;
		try {
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				res.add(resultSet.getString(1));
			}

		} finally {
			pstmt.close();
			if (resultSet != null) {
				resultSet.close();
			}
		}

		return res;
	}

	public boolean connected() {
		return dbConnected && grdLiteConnected;
	}

	public boolean stopExecution() {
		return stop;
	}

	public void setStopExecution(boolean stop) {
		this.stop = stop;
	}

	/**
	 * Bulk load request
	 * 
	 * @return all the instruments to be requested
	 * @throws SQLException
	 *             in case of errors
	 */
	private List<String> loadInstruments() throws SQLException {
		List<String> res = new ArrayList<String>();
		String sql = "SELECT isin FROM InstrumentsTable";
		PreparedStatement pstmt = jdbcConnection.prepareStatement(sql);
		ResultSet resultSet = null;
		try {
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				res.add(resultSet.getString(1));
			}
		} finally {
			pstmt.close();
			if (resultSet != null) {
				resultSet.close();
			}
		}

		logger.info("Sending one bulk request of {} instruments: {}", res.size(), res);
		return res;
	}

	public static void main(String[] args) {

		try {
			SODLoader sodLoader = new SODLoader();
			sodLoader.init();
			sodLoader.run();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private class GRDLiteCallbackImpl implements MQCallback {

		@Override
		public void onResponse(String loadResponse) {
			logger.info("{}", loadResponse);

		}

		@Override
		public void onException(String message) {
			logger.info("Error received from GRDLite: {}", message);
			if(grdLiteConnected) {
				grdLiteConnected = false;
				grdLiteConnectionTries = 1;
				retryGRDLiteConnection();
			}
		}
	}

	private String decryptPassword(String encryptedPassword) {
		String password = null;
		if (encryptedPassword.startsWith("ENC(")) {
			try {
				encryptedPassword = encryptedPassword.substring(4, encryptedPassword.length() - 1);
				password = encryptor.decrypt(encryptedPassword);  
			} catch (Exception e) {
				logger.warn("Unable to decrypy password, use the plain value. Reason: {}", e.getMessage());
				password = encryptedPassword;
			}
		} else {
			logger.info("Password '{}' not encrypted, use the plain value.", encryptedPassword);
			password = encryptedPassword;
		}
		logger.trace("encryptedPassword = '{}', password = '*****'", encryptedPassword, password);
		return password;
	}
}
