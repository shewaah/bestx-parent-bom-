/*
 * Copyright 1997-2016 SoftSolutions! srl 
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

package it.softsolutions.bestx.connections;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.jms.JMSException;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CommonMetricRegistry;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.connections.ib4j.PriceDiscoveryMessage;
import it.softsolutions.bestx.connections.pricediscovery.CSPriceDiscoveryOrderLazyBean;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.OperationAlreadyExistingException;
import it.softsolutions.bestx.mq.BXMQConnectionFactory;
import it.softsolutions.bestx.mq.MQCallback;
import it.softsolutions.bestx.mq.MQConfig;
import it.softsolutions.bestx.mq.MQConfigHelper;
import it.softsolutions.bestx.mq.MQConnection;
import it.softsolutions.bestx.pricediscovery.MQPriceDiscoveryMessage;
import it.softsolutions.bestx.services.customservice.CustomService;
import it.softsolutions.bestx.services.customservice.CustomServiceException;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * The Class MQPriceDiscoveryOperatorConsoleAdapter.
 */
public class MQPriceDiscoveryOperatorConsoleAdapter extends BaseOperatorConsoleAdapter implements MQCallback {

	private static final Logger LOGGER = LoggerFactory.getLogger(MQPriceDiscoveryOperatorConsoleAdapter.class);

	private static final String MQ_CONFIG_FILE = "MQServices.properties";

	private static final String MQ_CONFIG_PREFIX = "pd";

	private static int mqConnectionTries = 1;

	/* PriceDiscovery Metrics */
	private final Meter pdRequestsReceived = CommonMetricRegistry.INSTANCE.getMonitorRegistry().meter(MetricRegistry.name(MQPriceDiscoveryOperatorConsoleAdapter.class, "ReceivedPriceDiscoveryRequests"));

	private final Meter pdRequestsHandled = CommonMetricRegistry.INSTANCE.getMonitorRegistry().meter(MetricRegistry.name(MQPriceDiscoveryOperatorConsoleAdapter.class, "HandledPriceDiscoveryRequests"));

	private final Meter pdResponsePublished = CommonMetricRegistry.INSTANCE.getMonitorRegistry().meter(MetricRegistry.name(MQPriceDiscoveryOperatorConsoleAdapter.class, "PublishedPriceDiscoveryResults"));

	/* MQ Connection variables */
	private MQConnection mqConnection = null;

	private boolean active = true;

	private boolean available = false;

	private int reconnectionMaxTries = -1;

	/**
	 * Inits.
	 *
	 * @throws BestXException the best x exception
	 */
	@Override
	public void init() throws BestXException {
		checkPreRequisites();

		// Connect to MQ
		//    connect();
	}

	@Override
	protected void checkPreRequisites() throws ObjectNotInitializedException {
		super.checkPreRequisites();

	      if (this.customService == null)
	      {
	         throw new ObjectNotInitializedException("Instrument upload service not set");
	      }
	}

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.connections.Connection#connect()
	 */
	@Override
	public void connect() {

		//request first price discovery
		String intrumentCode = getSqlInstrumentDao().getLastIssuedInstrument().getIsin();
		try {
			requestPriceDiscovery(intrumentCode, "buy", null, "FOAD_"+UUID.randomUUID().toString(), "FOAD_"+UUID.randomUUID().toString());
		} catch (OperationAlreadyExistingException e) {
			LOGGER.error("Error while trying to perform first price discovery of the day.", e);
		} catch (BestXException e) {
			LOGGER.error("Error while trying to perform first price discovery of the day.", e);
		}

		try {
			if (!isConnected()) {
				connectMQService();
			}
		}
		catch (Exception e) {
			String message = "Failed to connect to MQService: " + e.getMessage();
			LOGGER.error(message, e);
			available = false;
		}

		try {
			SimpleTimerManager.getInstance().start();
		} catch (SchedulerException e) {
			LOGGER.error("Error while trying to start the timer manager.", e);
		}
	}

	@Override
	public void disconnect() {
		LOGGER.info("Closing Connection to MQ");
		if (mqConnection != null) {
			try {
				mqConnection.close();
			}
			catch (Exception e) {
				LOGGER.error("Failed to disconnect from MQService: " + e.getMessage(), e);
				available = false;
			}
			LOGGER.info("MQ Connection closed.");
		}
		else {
			LOGGER.warn("MQ Connection already closed.");
		}
	}

	@Override
	public boolean isConnected() {
		return active && available;
	}

	@Override
	public void publishPriceDiscoveryResult(final Operation operation, final String priceDiscoveryResult) {
		executeTask(new Runnable() {
			@Override
			public void run() {
				try {
					final MQPriceDiscoveryMessage mqMsg = new MQPriceDiscoveryMessage(priceDiscoveryResult);
					String traceId = "";
					if (operation != null && operation.getOrder() != null) {
						traceId = operation.getOrder().getCustomerOrderId();
						if (traceId!=null && traceId.startsWith("FOAD_")) return;
					}
					mqMsg.getMessageProperties().put("trace_id", traceId);
					if (mqConnection!=null) mqConnection.publish(mqMsg);
					pdResponsePublished.mark();
				}
				catch (Exception e) {
					LOGGER.error("publishPriceDiscoveryResult failed", e);
				}
			}
		});
	}

	// ================================================================================
	//  MQ Connection
	// ================================================================================
	protected void connectMQService() throws Exception {
		LOGGER.debug("MQ Service connection attempt {}", mqConnectionTries);
		if (reconnectionMaxTries == -1 || mqConnectionTries <= reconnectionMaxTries) {

			final MQConfig mqCfg = MQConfigHelper.getConfigurationFromFile(MQ_CONFIG_FILE, MQ_CONFIG_PREFIX);

			try {
				mqConnection = BXMQConnectionFactory.getConnection(mqCfg, this);
				available = true;
				LOGGER.debug("MQ connection attempt {} succeded", mqConnectionTries);
			}
			catch (JMSException e) {
				LOGGER.info("Cannot connect to the JMS queue: {}", e.getMessage(), e);
				retryMQConnection();
			}
			catch (Exception ge) {
				LOGGER.info("Cannot connect to the MQ service: {}", ge.getMessage(), ge);
				retryMQConnection();
			}
			finally {
				mqConnectionTries++;
			}
		}
	}

	private void retryMQConnection() {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				try {
					connectMQService();
				}
				catch (Exception e) {
					LOGGER.error("{}", e.getMessage(), e);
				}
			}
		}, 5000);
	}

	@Override
	public void onResponse(String response) {
		// Count received requests
		pdRequestsReceived.mark();

		// Manage Price Discovery Request JSON 
		final JSONObject pdRequest = JSONObject.fromObject(response);

		try {
			String instrumentCode = pdRequest.getString(PriceDiscoveryMessage.FLD_INSTRUMENT);
			String side = pdRequest.getString(PriceDiscoveryMessage.FLD_SIDE);

			Double quantity = null;
			try {
				quantity = pdRequest.getDouble(PriceDiscoveryMessage.FLD_QUANTITY);
			}
			catch (Exception e) {
				quantity = null;
			}

			String orderID = pdRequest.getString(PriceDiscoveryMessage.FLD_ORDER_ID);
			String traceID = pdRequest.getString(PriceDiscoveryMessage.FLD_TRACE_ID);

			requestPriceDiscovery(instrumentCode, side, quantity, orderID, traceID);
		}
		catch (Exception e) {
			LOGGER.error("Failed to handle PriceDiscoveryRequest: " + response + " : ", e);
			return;
		}

		pdRequestsHandled.mark();
	}

    private CustomService customService;

    public CustomService getCustomService() {
		return customService;
	}

	public void setCustomService(CustomService customService) {
		this.customService = customService;
	}

	private void requestPriceDiscovery(String instrumentCode, String side, Double quantity, String orderID, String traceID)
			throws OperationAlreadyExistingException, BestXException {

		final Operation operation = getOperationRegistry().getNewOperationById(OperationIdType.PRICE_DISCOVERY_ID, traceID, true);
		CSPriceDiscoveryOrderLazyBean order = new CSPriceDiscoveryOrderLazyBean(instrumentCode, side, quantity, orderID, null, null, traceID);
		order.setSqlInstrumentDao(getSqlInstrumentDao());
		order.setInstrumentFinder(getInstrumentFinder());
		operation.setOrder(order);

		if (order.getInstrument() == null) {
            try {
				customService.sendRequest(order.getFixOrderId(), false, instrumentCode);
			} catch (CustomServiceException e) {
				throw new BestXException(e);
			}
			JSONArray errorReport = new JSONArray();
			errorReport.add(PriceDiscoveryMessage.LABEL_INSTRUMENT_NOT_SUPPORTED);
			JSONObject jsonBook = PriceDiscoveryMessage.createEmptyBook(order, errorReport);
			publishPriceDiscoveryResult(operation, jsonBook.toString());
			return;
		}

		executeTask(new Runnable() {
			@Override
			public void run() {
				operation.onOperatorPriceDiscovery(MQPriceDiscoveryOperatorConsoleAdapter.this);
			}
		});
	}

	@Override
	public void onException(String message) {
		LOGGER.info("Error received from MQ Service: {}", message);
		available = false;
		mqConnectionTries = 1;
		retryMQConnection();
	}

}
