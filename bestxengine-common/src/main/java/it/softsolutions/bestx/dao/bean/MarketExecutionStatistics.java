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
 
package it.softsolutions.bestx.dao.bean;

import java.math.BigDecimal;

/**  
*
* Purpose: this class is mainly for represents execution statistic for one market  
*
* Project Name : bestxengine-common 
* First created by: stefano.pontillo 
* Creation date: 04 ott 2017 
* 
**/
public class MarketExecutionStatistics {
   
   private int marketId;
   private String marketName;
   private BigDecimal totalVolume;
   private BigDecimal executedVolume;
   private int totalOrders;
   private int executedOrders;
   
   public MarketExecutionStatistics(){
      super();
      this.marketId = -1;
      this.marketName = "";
      this.totalVolume = BigDecimal.ZERO;
      this.executedVolume = BigDecimal.ZERO;
      this.totalOrders = 0;
      this.executedOrders = 0;
   }

   /**
    * @return the marketId
    */
   public int getMarketId() {
      return marketId;
   }
   
   /**
    * @param marketId the marketId to set
    */
   public void setMarketId(int marketId) {
      this.marketId = marketId;
   }
   
   /**
    * @return the marketName
    */
   public String getMarketName() {
      return marketName;
   }

   
   /**
    * @param marketName the marketName to set
    */
   public void setMarketName(String marketName) {
      this.marketName = marketName;
   }
   
   /**
    * @return the totalVolume
    */
   public BigDecimal getTotalVolume() {
      return totalVolume;
   }

   
   /**
    * @param totalVolume the totalVolume to set
    */
   public void setTotalVolume(BigDecimal totalVolume) {
      this.totalVolume = totalVolume;
   }

   
   /**
    * @return the executedVolume
    */
   public BigDecimal getExecutedVolume() {
      return executedVolume;
   }

   
   /**
    * @param executedVolume the executedVolume to set
    */
   public void setExecutedVolume(BigDecimal executedVolume) {
      this.executedVolume = executedVolume;
   }

   /**
    * @return the totalOrders
    */
   public int getTotalOrders() {
      return totalOrders;
   }
   
   /**
    * @param totalOrders the totalOrders to set
    */
   public void setTotalOrders(int totalOrders) {
      this.totalOrders = totalOrders;
   }
   
   /**
    * @return the executedOrders
    */
   public int getExecutedOrders() {
      return executedOrders;
   }
   
   /**
    * @param executedOrders the executedOrders to set
    */
   public void setExecutedOrders(int executedOrders) {
      this.executedOrders = executedOrders;
   }
}
