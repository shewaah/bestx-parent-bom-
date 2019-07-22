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
package it.softsolutions.bestx.automatictest;

// TODO: Auto-generated Javadoc
/**
 * Purpose: Main interface for interface components for communicating with operations.
 *
 * Project Name : bestxengine 
 * First created by: acquafresca/Gonzalez 
 * Creation date: 27/06/2019 
 * 
 **/
public interface AutomaticTestMBean {
    
   
    /**
     * Gets the simple order operation by id.
     *
     * @param id the id
     * @return the simple order operation by id
     */
    String getSimpleOrderOperationById(String id);
    
    /**
     * Creates the new order.
     *
     * @param isin the isin
     * @param date the date
     * @param settlementDate the settlement date
     * @return the id of the order
     */
    String createNewOrder(String isin, String date, String settlementDate, int quantity, String currency);
  
    /**
     * Creates the new order.
     * 
     * It calculates the dates automatically by setting:
     * * The date of the order to today
     * * The date of the order to today + 2 working days (skipping Sat and Sun)
     *
     * @param isin the isin
     * @return the id of the order
     */
    String createNewOrder(String isin, int quantity, String currency);
    
    /**
     * Returns the current status of the order.
     * 
     * @param id The ID of the order returned by createNewOrder
     * @return A human-readable description of the current state.
     */
    String getOrderStatus(String id);
    
    /**
     * Returns the status history of the order along with the timestamp of every change.
     * 
     * @param id The ID of the order returned by createNewOrder
     * @return A JSON string with all the information
     */
    String getOrderHistory(String id);
    
    /**
     * Returns the prices associated with a single operation for all attempts.
     * 
     * @param id The ID of the order returned by createNewOrder
     * @return A JSON string with all the information
     */
    String getPriceBook(String id);
    
    /**
     * Returns the market status of the orderfor all attempts.
     * 
     * @param id The ID of the order returned by createNewOrder
     * @return A JSON string with all the information
     */
    String getMarketStatus(String id);
}