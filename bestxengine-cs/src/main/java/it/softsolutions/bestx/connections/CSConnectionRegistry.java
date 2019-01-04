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

package it.softsolutions.bestx.connections;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Purpose: this class is mainly for ...
 *
 * Project Name : bestxengine-cs First created by: Creation date: 19-ott-2012
 * 
 **/
public class CSConnectionRegistry implements ConnectionRegistry, ConnectionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(CSConnectionRegistry.class);
	private CustomerConnection customerConnection;
	private TradingConsoleConnection tradingConsoleConnection;
	private OperatorConsoleConnection operatorConsoleConnection;
	private List<ConnectionStateListener> listeners;
	
	private Connection mqPriceDiscoveryConnection;
	private Connection grdLiteConnection;
	

	/**
	 * Sets the customer connection.
	 *
	 * @param customerConnection
	 *            the new customer connection
	 */
	public void setCustomerConnection(CustomerConnection customerConnection) {
		this.customerConnection = customerConnection;
		this.customerConnection.setConnectionListener(this);
	}

	/**
	 * Sets the trading console connection.
	 *
	 * @param tradingConsoleConnection
	 *            the new trading console connection
	 */
	public void setTradingConsoleConnection(TradingConsoleConnection tradingConsoleConnection) {
		this.tradingConsoleConnection = tradingConsoleConnection;
		this.tradingConsoleConnection.setConnectionListener(this);
	}

	/**
	 * Sets the operator console connection.
	 *
	 * @param operatorConsoleConnection
	 *            the new operator console connection
	 */
	public void setOperatorConsoleConnection(OperatorConsoleConnection operatorConsoleConnection) {
		this.operatorConsoleConnection = operatorConsoleConnection;
		this.operatorConsoleConnection.setConnectionListener(this);
	}

	@Override
	public CustomerConnection getCustomerConnection() {
		return customerConnection;
	}

	public TradingConsoleConnection getTradingConsoleConnection(String identifier) {
		return tradingConsoleConnection;
	}

	public OperatorConsoleConnection getOperatorConsoleConnection(String identifier) {
		return operatorConsoleConnection;
	}

	public void onConnection(Connection source) {
		if (source == customerConnection) {
			if (listeners != null) {
				for (ConnectionStateListener listener : listeners) {
					if (listener != null) {
						listener.onCustomerConnectionUp((CustomerConnection) source);
					}
				}
			}
		} else if (source == tradingConsoleConnection) {
			if (listeners != null) {
				for (ConnectionStateListener listener : listeners) {
					if (listener != null) {
						listener.onTradingConsoleConnectionUp((TradingConsoleConnection) source);
					}
				}
			}
		} else if (source == operatorConsoleConnection) {
			if (listeners != null) {
				for (ConnectionStateListener listener : listeners) {
					if (listener != null) {
						listener.onOperatorConsoleConnectionUp((OperatorConsoleConnection) source);
					}
				}
			}
		} else {
			LOGGER.error("Received connection event from unknown source");
		}
	}

	public void onDisconnection(Connection source, String reason) {
		if (source == customerConnection) {
			if (listeners != null) {
				for (ConnectionStateListener listener : listeners) {
					if (listener != null) {
						listener.onCustomerConnectionDown((CustomerConnection) source, reason);
					}
				}
			}
		} else if (source == tradingConsoleConnection) {
			if (listeners != null) {
				for (ConnectionStateListener listener : listeners) {
					if (listener != null) {
						listener.onTradingConsoleConnectionDown((TradingConsoleConnection) source, reason);
					}
				}
			}
		} else if (source == operatorConsoleConnection) {
			if (listeners != null) {
				for (ConnectionStateListener listener : listeners) {
					if (listener != null) {
						listener.onOperatorConsoleConnectionDown((OperatorConsoleConnection) source, reason);
					}
				}
			}
		} else {
			LOGGER.error("Received disconnection event from unknown source");
		}
	}

	public void setConnectionStateListeners(List<ConnectionStateListener> listeners) {
		this.listeners = listeners;
	}

   @Override
   public Connection getMqPriceDiscoveryConnection() {
      return mqPriceDiscoveryConnection;
   }

   @Override
   public Connection getGrdLiteConnection() {
      return grdLiteConnection;
   }

   
   /**
    * @param mqPriceDiscoveryConnection the mqPriceDiscoveryConnection to set
    */
   public void setMqPriceDiscoveryConnection(Connection mqPriceDiscoveryConnection) {
      this.mqPriceDiscoveryConnection = mqPriceDiscoveryConnection;
   }

   
   /**
    * @param grdLiteConnection the grdLiteConnection to set
    */
   public void setGrdLiteConnection(Connection grdLiteConnection) {
      this.grdLiteConnection = grdLiteConnection;
   }
}