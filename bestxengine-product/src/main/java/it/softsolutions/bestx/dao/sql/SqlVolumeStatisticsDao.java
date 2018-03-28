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
 
package it.softsolutions.bestx.dao.sql;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import it.softsolutions.bestx.dao.VolumeStatisticsDao;
import it.softsolutions.bestx.dao.bean.MarketExecutionStatistics;
import it.softsolutions.bestx.services.DateService;


/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-cs 
* First created by: stefano.pontillo 
* Creation date: 04 ott 2017 
* 
**/
public class SqlVolumeStatisticsDao implements VolumeStatisticsDao {

   private static final Logger LOGGER = LoggerFactory.getLogger(SqlVolumeStatisticsDao.class);
   private JdbcTemplate jdbcTemplate;
   private static final String dateTimeForDB = "yyyy-MM-dd";
   
   
   /* (non-Javadoc)
    * @see it.softsolutions.bestx.dao.VolumeStatisticsDao#marketExecutedStatististics()
    */
   @Override
   public Map<Integer, MarketExecutionStatistics> marketExecutedStatististics() {
      Map<Integer, MarketExecutionStatistics> marketRes = new HashMap<Integer, MarketExecutionStatistics>();
      
      Calendar c = Calendar.getInstance();
      String startDate = DateService.format(dateTimeForDB, c.getTime());
      
      c.add(Calendar.DAY_OF_YEAR, 1);
      String endDate = DateService.format(dateTimeForDB, c.getTime());
      
      String sql = "SELECT marketid, sum(quantita) as volumeTot, count(*) as numOrders FROM TabHistoryOrdini where DataOraRicezione between '" + startDate + "' and '" + endDate + "' group by marketid";
      SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql);
      while (rowSet.next()) {
         int marketId = rowSet.getInt("marketid");
         MarketExecutionStatistics stat = new MarketExecutionStatistics();
         stat.setMarketId(marketId);
         stat.setTotalOrders(rowSet.getInt("numOrders"));
         stat.setTotalVolume(rowSet.getBigDecimal("volumeTot"));
         marketRes.put(marketId, stat);
      }
      
      sql = "SELECT marketid, sum(quantita) as volumeTot, count(*) as numOrders FROM TabHistoryOrdini where Stato = 'StateExecuted' and DataOraRicezione between '" + startDate + "' and '" + endDate + "' group by marketid";
      rowSet = jdbcTemplate.queryForRowSet(sql);
      while (rowSet.next()) {
         int marketId = rowSet.getInt("marketid");
         MarketExecutionStatistics stat = marketRes.get(marketId);
         stat.setExecutedOrders(rowSet.getInt("numOrders"));
         stat.setExecutedVolume(rowSet.getBigDecimal("volumeTot"));
         marketRes.put(marketId, stat);
      }
      return marketRes;
   }
   
   /**
    * @param jdbcTemplate the jdbcTemplate to set
    */
   public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
      this.jdbcTemplate = jdbcTemplate;
   }
}
