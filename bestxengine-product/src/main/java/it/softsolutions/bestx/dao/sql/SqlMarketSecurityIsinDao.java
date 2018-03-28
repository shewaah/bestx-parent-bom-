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
package it.softsolutions.bestx.dao.sql;

import it.softsolutions.bestx.dao.MarketSecurityIsinDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Market.SubMarketCode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

/**  
*
* Purpose : access to the regulated and regulated like markets instruments data through sql queries.
*
* Project Name : bestxengine-product 
* First created by: davide.rossoni 
* Creation date: 24/ago/2012 
* 
**/
public class SqlMarketSecurityIsinDao implements MarketSecurityIsinDao
{
   private JdbcTemplate jdbcTemplate;
   private static final String sqlFields = "ISIN, MarketCode";
   private static final String sqlSelectAll = "SELECT " + sqlFields + " FROM MarketSecurityStatus";
   private static final String sqlSelectMarket = "SELECT " + sqlFields
         + " FROM MarketSecurityStatus WHERE MarketCode = ?";
   private static final String sqlSelectSubMarketCode = "SELECT SubMarketCode FROM MarketSecurityStatus WHERE ISIN = ? AND MarketCode = ?";
   private static final String sqlSelectBOTSettlementDateForMOT = "SELECT mot.SettlementDate settlDate "
         + "FROM dbo.MarketSecurityStatus mot " + "right join dbo.MarketSecurityStatus bv "
         + "on mot.ISIN = bv.ISIN " + "where bv.MarketCode ='BV' " + "and bv.MarketBondType='BOT' "
         + "and mot.MarketCode = 'MOT' " + "and mot.ISIN = ? "
         + "and CONVERT(nvarchar(8), mot.UpdateDate, 112) = CONVERT(nvarchar(8), GETDATE(), 112)";

   /**
    * Load the settlement date from the MarketSecurityStatus table if the given isin is a BOT.
    * Check the query for further details.
    */
   public Date getBOTSettlementDate(String isin)
   {
      Date settlDate = null;
      final Object[] queryArgs = new Object[] { isin };
      List<Date> settlDateList = (List<Date>) jdbcTemplate.queryForList(sqlSelectBOTSettlementDateForMOT, queryArgs, Date.class);
      if (settlDateList != null && !settlDateList.isEmpty()) {
         settlDate = settlDateList.get(0);
      }
      return settlDate;
   }

   /*
    * (non-Javadoc)
    * @see it.softsolutions.bestx.dao.MarketSecurityIsinDao#loadInstruments()
    */
   public Map<String, List<String>> loadInstruments()
   {
      checkPreRequisites();
      final HashMap<String, List<String>> map = new HashMap<String, List<String>>();
      jdbcTemplate.query(sqlSelectAll, (Object[]) null, new RowCallbackHandler()
      {
         public void processRow(ResultSet rset) throws SQLException
         {
            String isin = rset.getString("ISIN");
            if (!map.containsKey(isin))
            {
               map.put(isin, new ArrayList<String>());
            }
            map.get(isin).add(rset.getString("MarketCode"));
         }
      });
      return map;
   }

   private void checkPreRequisites() throws ObjectNotInitializedException
   {
      if (jdbcTemplate == null)
      {
         throw new ObjectNotInitializedException("JDBC template not set");
      }
   }

   /*
    * (non-Javadoc)
    * @see it.softsolutions.bestx.dao.MarketSecurityIsinDao#addInstruments(java.lang.String, java.util.Map)
    */
   public Map<String, List<String>> addInstruments(String market, Map<String, List<String>> argMap)
   {
      final Object[] params = new Object[] { market };
      final HashMap<String, List<String>> map = new HashMap<String, List<String>>();
      jdbcTemplate.query(sqlSelectMarket, params, new RowCallbackHandler()
      {
         public void processRow(ResultSet rset) throws SQLException
         {
            String isin = rset.getString("ISIN");
            if (!map.containsKey(isin))
            {
               map.put(isin, new ArrayList<String>());
            }
            map.get(isin).add(rset.getString("MarketCode"));
         }
      });
      return merge(map, argMap);
   }

   /**
    * Merges the oldMap with the newMap
    * @param newMap the map containing values to be added to the oldMap
    * @param oldMap the map to be merged
    * @return the oldMap merged with the newMap
    */
   private Map<String, List<String>> merge(HashMap<String, List<String>> newMap, Map<String, List<String>> oldMap)
   {
      if (oldMap == null || oldMap.isEmpty())
      {
         return newMap;
      }
      if (newMap == null || newMap.isEmpty())
      {
         return oldMap;
      }
      Set<String> isins = newMap.keySet();
      // casi:
      //1) isin in newMap e non in oldMap. Aggiungere isin+list da newMap ad oldMap
      //2) isin in entrambe: fare il merge delle due liste, evitando di replicare i marketCodes
      //3) isin solo in oldMap: non c'e' niente da fare (gestione implicita perche' prendo solo le chiavi di newMap)
      for (String isin : isins)
      {
         // casi:
         //1) isin in newMap e non in oldMap. Aggiungere isin+list da newMap ad oldMap
         if (!oldMap.containsKey(isin))
         {
            oldMap.put(isin, newMap.get(isin));
         }
         else
         //2) isin in entrambe: fare il merge delle due liste, evitando di replicare i marketCodes
         {
            List<String> mktList = newMap.get(isin);
            List<String> argMktList = oldMap.get(isin);
            for (String mktCode : mktList)
            {
               if (!argMktList.contains(mktCode))
               {
                  argMktList.add(mktCode);
               }
            }
         }
      }
      return oldMap;
   }

   /*
    * (non-Javadoc)
    * @see it.softsolutions.bestx.dao.MarketSecurityIsinDao#getSubmarketCodes(java.lang.String, java.lang.String)
    */
   public List<SubMarketCode> getSubmarketCodes(String isin, String market)
   {
      checkPreRequisites();
      final List<SubMarketCode> subMarketCodes = new ArrayList<SubMarketCode>();
      final Object[] params = new Object[] { isin, market };
      jdbcTemplate.query(sqlSelectSubMarketCode, params, new RowCallbackHandler()
      {
         public void processRow(ResultSet rset) throws SQLException
         {
            subMarketCodes.add((SubMarketCode) rset.getObject("SubMarketCode"));
         }
      });
      return subMarketCodes;
   }

   /**
    * Get the jdbcTemplate used to execute queries
    * @return the JdbcTemplate object
    */
   public JdbcTemplate getJdbcTemplate()
   {
      return this.jdbcTemplate;
   }

   /**
    * Set the jdbcTemplate used to execute queries
    * @param jdbcTemplate
    */
   public void setJdbcTemplate(JdbcTemplate jdbcTemplate)
   {
      this.jdbcTemplate = jdbcTemplate;
   }
}
