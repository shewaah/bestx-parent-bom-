/*
* Copyright 1997-2017 SoftSolutions! srl 
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

import java.util.Map;

import it.softsolutions.bestx.dao.bean.MarketExecutionStatistics;


/**  
*
* Purpose: this class is mainly for retireve volume statistics from database  
*
* Project Name : bestxengine-common 
* First created by: stefano.pontillo 
* Creation date: 27 set 2017 
* 
**/
public interface VolumeStatisticsDao {
   
   /**
    * 
    * @return list of percentage of executed orders by market
    */
   public Map<Integer, MarketExecutionStatistics> marketExecutedStatististics();
   
   
}