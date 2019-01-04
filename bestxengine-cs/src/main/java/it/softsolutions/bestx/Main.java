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

package it.softsolutions.bestx;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceFactory;
/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
import it.softsolutions.bestx.services.logutils.ApplicationMonitor;
import it.softsolutions.jsscommon.SSLog;
import it.softsolutions.licensevalidator.LicenseValidationException;
import it.softsolutions.licensevalidator.LicenseValidatorService;
import it.softsolutions.licensevalidator.LicenseValidatorServiceFactory;
import it.softsolutions.manageability.commons.Application;

/**
 * The Class Main.
 */
@SuppressWarnings("deprecation")
public class Main extends Application {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	private static final String FEATURE_NAME = "BestX";
	
	private static Main main = null;

	/**
	 * Instantiates a new main.
	 */
	public Main() {
		super(ApplicationType.CommonService);
	}


	/**
	 * Application startup.
	 * 
	 * @param args
	 *            [0] Spring configuration file
	 */
	public static void main(String[] args) {

		try {
			main = new Main();

			LOGGER.info("Validating license...");
			Properties prop = new Properties();
			try {
				@SuppressWarnings("static-access")
				InputStream bestxPropertiesReader = main.getClass().getClassLoader().getSystemResourceAsStream(args[1]);
				prop.load(bestxPropertiesReader);
				if (prop.getProperty("BestX.LicenseFile") == null) {
					LOGGER.error("No License file provided in property file BESTX.properties for LicenseFile property");
					System.exit(0);
				}
				LicenseValidatorService licenseValidatorService = LicenseValidatorServiceFactory.getLicenseValidatorService(FEATURE_NAME, prop.getProperty("BestX.LicenseFile"));
				licenseValidatorService.validate();
			} catch (LicenseValidationException e) {
				LOGGER.error("License not provided or expired. Exiting application...", e);
				System.exit(0);
			} catch (IOException e1) {
				LOGGER.error("Exception at startup", e1);
			}

			LOGGER.debug("BestX Engine starting - configuration files: ");
			for (String arg : args) {
				LOGGER.debug("-> " + arg);
			}
			SSLog.init(null);

			long monitorPeriod = Long.parseLong(prop.getProperty("MetricRegistry.monitor.period", "60"));
			long healtPeriod = Long.parseLong(prop.getProperty("MetricRegistry.health.period", "600"));
			CommonMetricRegistry.INSTANCE.init(monitorPeriod, healtPeriod, TimeUnit.SECONDS);
			
			ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(args[0]);
			
			// set context to all beans implementing ApplicationContextAware
			ExecutionStrategyServiceFactory executionStrategyServiceFactory = (ExecutionStrategyServiceFactory) context.getBean("executionStrategyServiceFactory");
			if(executionStrategyServiceFactory != null) executionStrategyServiceFactory.setApplicationContext(context);
			else LOGGER.warn("Bean executionStrategyServiceFactory not found in context. Be sure that the Spring configuration file is correct!");
			// start EngineController
			EngineController applicationController = (EngineController) context.getBean("applicationController");

			
			ApplicationMonitor appMonitor = new ApplicationMonitor();  // Default is every minute
			appMonitor.start();
			applicationController.idle(); // wait for shutdown command
			context.close();

		} catch (Throwable e) {
			LOGGER.error("Generic error caught.", e);
		} finally {
			System.exit(0);
		}
	}

	@Override
	public void shutdownApplication() {

	}

	@Override
	public void startApplication() {

	}

	@Override
	public String getServiceName() {
		return "BestX! Application";
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Hashtable getServiceProperties() {
		return new Hashtable();
	}

	@Override
	public void reload() {

	}

	@Override
	public void restart() {

	}
}
