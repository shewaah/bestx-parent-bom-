/**
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
package it.softsolutions.bestx;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;

import it.softsolutions.bestx.connections.Connection;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-common
 * First created by: davide.rossoni
 * Creation date: 16/dic/2013
 * 
 */
public enum CommonMetricRegistry {
	INSTANCE;
	
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonMetricRegistry.class);
	
	private final MetricRegistry monitorRegistry = new MetricRegistry();
	private final MetricRegistry healtRegistry = new MetricRegistry();
	private final HealthCheckRegistry healthChecksRegistry = new HealthCheckRegistry();

	
	public void init(final long monitorPeriod, final long healtPeriod, TimeUnit unit) {
		
		// *** MONITOR ***
		// Logs metrics to an SLF4J logger
		final Slf4jReporter slf4jReporter1 = Slf4jReporter.forRegistry(monitorRegistry)
                .outputTo(LoggerFactory.getLogger(CommonMetricRegistry.class))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
		slf4jReporter1.start(monitorPeriod, unit);
		
		// Reports metrics via JMX
		final JmxReporter jmxReporter1 = JmxReporter.forRegistry(monitorRegistry).build();
		jmxReporter1.start();
		
		// *** HEALT ***
		final Slf4jReporter slf4jReporter2 = Slf4jReporter.forRegistry(healtRegistry)
                .outputTo(LoggerFactory.getLogger(CommonMetricRegistry.class))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
		slf4jReporter2.start(healtPeriod, unit);
		
		new Thread() {
			@Override
			public void run() {
				do {
					final Map<String, HealthCheck.Result> results = healthChecksRegistry.runHealthChecks();
					for (Entry<String, HealthCheck.Result> entry : results.entrySet()) {
					    if (entry.getValue().isHealthy()) {
					        LOGGER.info("[MonitorCheck] Connection '{}' is healthy", entry.getKey());
					    } else {
					    	LOGGER.warn("[MonitorCheck] Connection '{}' is unhealthy", entry.getKey(), entry.getValue().getMessage());
					        final Throwable e = entry.getValue().getError();
					        if (e != null) {
					        	LOGGER.error(e.getMessage(), e);
					        }
					    }
					}

					try { TimeUnit.SECONDS.sleep(monitorPeriod); } catch (InterruptedException e) { }
				} while (true);
			}
		}.start();
		
	}

	public MetricRegistry getMonitorRegistry() {
		return monitorRegistry;
	}

	public MetricRegistry getHealtRegistry() {
		return healtRegistry;
	}
	
	public void registerHealtCheck(Connection connection) {
		healthChecksRegistry.register(connection.getConnectionName(), new ConnectionHealthCheck(connection));
	}
	
	private class ConnectionHealthCheck extends HealthCheck {
	    private final Connection connection;

	    public ConnectionHealthCheck(Connection connection) {
	        this.connection = connection;
	    }

	    @Override
	    public HealthCheck.Result check() throws Exception {
	        if (connection.isConnected()) {
	            return HealthCheck.Result.healthy();
	        } else {
	            return HealthCheck.Result.unhealthy("Connection is down");
	        }
	    }
	}
	
}
