/*
* Project Name : ${project_name} 
* First created by: ${user} 
* Creation date: ${date} 
* 
* Copyright 1997-${year} SoftSolutions! srl 
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
* 
*/
package it.softsolutions.bestx.services.ranking;

/**
 * Service that works with the market makers ranking. 
 *
 */
public interface DealerRankingService
{
   /**
    * Load the ranking and update the currently one in use.
    * @throws Exception
    */
   public void loadRankingAndUpdate() throws Exception;

   /**
    * This method performs the operations on the database
    * needed for the loading of the day's ranking.
    * No arguments if we need to load today's ranking.
    * @throws Exception 
    */
   public void loadRanking(boolean updateLiveRanking) throws Exception;

   /**
    * This method performs the operations on the database
    * needed for the loading of the day's ranking.
    * The argument is the day whose ranking we need.
    * Date format : yyyymmdd.
    */
   public void loadRanking(String date, boolean updateLiveRanking) throws Exception;
}