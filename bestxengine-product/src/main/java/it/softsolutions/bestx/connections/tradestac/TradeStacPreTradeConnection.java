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
package it.softsolutions.bestx.connections.tradestac;

import java.util.List;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.model.Instrument;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-product 
* First created by: fabrizio.aponte 
* Creation date: 22/mag/2012 
* 
**/
public interface TradeStacPreTradeConnection extends Connection {
	
	TradeStacPreTradeConnectionListener getTradeStacPreTradeConnectionListener();
	
	/**
	 * Set the connectionListener
	 * 
	 * @param connectionListener the connectionListener
	 */
	void setTradeStacPreTradeConnectionListener(TradeStacPreTradeConnectionListener connectionListener);

    /**
     * Sends a security list request in order to know the current list of
     * securities for all the authorized product groups.
     * 
     * @throws BestXException
     *  if an error occurred during this static request of data.
     */
    void requestInstrumentStatus() throws BestXException;

	/**
	 * Send a priceSnapshot request for the specified instrument and marketMaker 
	 * 
	 * @param instrument the instrument 
	 * @param marketMakerCode the marketMaker
	 * @throws BestXException if an error occurred during the request subscription
	 */
	void requestInstrumentPriceSnapshot(Instrument instrument, String marketMakerCode) throws BestXException;

	// [DR20120424] blp-api v3 (and TradeStac too) permits multiple isin@marketMaker requests in the same message
	/**
	 * Send a priceSnapshot request for the specified instrument and marketMakers
	 * 
	 * @param instrument the instrument 
     * @param marketMakerCodes the set of marketMakers
     * @throws BestXException if an error occurred during the request subscription
	 */
	void requestInstrumentPriceSnapshot(Instrument instrument, List<String> marketMakerCodes) throws BestXException;
	
}
