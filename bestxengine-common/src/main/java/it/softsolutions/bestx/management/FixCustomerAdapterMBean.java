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
package it.softsolutions.bestx.management;

/**
 * 
 * Purpose: this JMX interface exposes statistics methods for the customer connection
 * 
 * Project Name : bestxengine-common First created by: ruggero.rizzo Creation date: 15/giu/2012
 * 
 **/
public interface FixCustomerAdapterMBean {
	
	// commands
	void connect();

	void disconnect();

	// queries
	String getConnectionName();

	boolean isConnected();

	// statistics
	/**
	 * Get customer adapter uptime
	 * 
	 * @return uptime
	 */
	long getUptime();

	/**
	 * Get number of exceptions occured
	 * 
	 * @return numer of exceptions
	 */
	long getNumberOfExceptions();

	/**
	 * Get average number of orders received per second
	 * 
	 * @return average number of orders/second
	 */
	double getAvgInputPerSecond();

	/**
	 * Get average number of response sent per second
	 * 
	 * @return avg number of response per second
	 */
	double getAvgOutputPerSecond();

	/**
	 * Get percentage of broken orders
	 * 
	 * @return percentage of not valid order
	 */
	double getPctOfFailedInput();

	/**
	 * Get number of orders in input
	 * 
	 * @return number of orders received
	 */
	long getInFixOrderNotifications();

	/**
	 * Get number of RFQs in input
	 * 
	 * @return number of RFQs received
	 */
	long getInFixRfqNotifications();

	/**
	 * Get number of trades acknowledged
	 * 
	 * @return number of trades acknowledged
	 */
	long getInFixTradesAcknowledged();

	/**
	 * Get number of trades not acknowledged
	 * 
	 * @return number of trades not acknowledged
	 */
	long getInFixTradesNotAcknowledged();

	/**
	 * Get the number of responses sent to RFQs
	 * 
	 * @return number of responses sent to RFQs
	 */
	long getOutRfqResponses();

	/**
	 * Get number of order rejects sent
	 * 
	 * @return number of order rejects sent
	 */
	long getOutOrderRejects();

	/**
	 * Get number of RFQs rejects sent
	 * 
	 * @return number of RFQs rejects sent
	 */
	long getOutRfqRejects();

	/**
	 * Get number of rejected execution reports
	 * 
	 * @return number of rejected execution reports
	 */
	long getOutRejectExecutionReports();

	/**
	 * Get number of fill execution reports
	 * 
	 * @return number of fill execution reports
	 */
	long getOutFillExecutionReports();

	/**
	 * Reset statistics
	 */
	void resetStats();

	/**
	 * Get number of order received in an interval
	 * 
	 * @return number of order received in an interval
	 */
	int getOrderNumberInLastInterval();

	/**
	 * Set the probing interval time expressed in seconds
	 * 
	 * @param intervalTimeInSecs
	 *            : interval seconds
	 */
	void setIntervalTimeInSecs(long intervalTimeInSecs);

	/**
	 * Get the interval time
	 * 
	 * @return interval time
	 */
	long getIntervalTimeInSecs();

	/**
	 * Get fix gateway connection status
	 * 
	 * @return
	 */
	String getFixGatewayConnectionStatus();
	// TODO Monitoring-BX
}
