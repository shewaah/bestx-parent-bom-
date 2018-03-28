/*
* Project Name : bestxengine-common
* First created by: matteo.salis
* Creation date: 10/mag/2012
*
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
*
*/

package it.softsolutions.bestx.dao;

/**
 * Purpose : interface for a DAO that works on the ranking tables.
 */
public interface RankingLoaderDao
{
   /**
    * Method that will load the date requested ranking in the ranking table
    * and eventually update the currently used one.
    * @param date : ranking date requested
    * @param updateLiveRanking : true to updated the live ranking, false to leave it untouched.
    * @throws Exception
    */
   public void loadNewRanking(String date, boolean updateLiveRanking) throws Exception;
   
   /**
    * Fetch the ranking for a given date returning it as an array of strings.
    * @param date : the day whose ranking we have been requested.
    * @return the ranking represented as a list of market makers codes.
    * @throws Exception
    */
   public String[] getRanking(String date) throws Exception;
}
