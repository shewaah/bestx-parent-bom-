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
package it.softsolutions.bestx.services;

import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

/**  

 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-common-3.0.13-SNAPSHOT 
 * First created by: paolo.midali 
 * Creation date: 20/mar/2013 
 * 
 **/
public class StateDescriptionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateDescriptionService.class);

    private static final String sqlStateDescription = "SELECT DescrizioneStato "
                    + " FROM CodiciStatiOrdine WHERE CodiceStato = ?";
    private static JdbcTemplate jdbcTemplate;

    /**
     * Set JdbcTemplate
     * @param jdbcTemplate
     */
    public static void setJdbcTemplate(JdbcTemplate theJdbcTemplate) {
        jdbcTemplate = theJdbcTemplate;
    }

    private static void checkPreRequisites() throws ObjectNotInitializedException {
        if (jdbcTemplate == null) {
            throw new ObjectNotInitializedException("JDBC template not set");
        }
    }
    
    public static String getStateDescription(String stateCode)
    {
       checkPreRequisites();
       LOGGER.debug("Loading description for state {}", stateCode);
       
       final List<String> descriptionList = new ArrayList<String>();
       
       final Object[] params = new Object[] { stateCode };
       
       jdbcTemplate.query(sqlStateDescription, params, new RowCallbackHandler() {
           public void processRow(ResultSet rset) throws SQLException {
               descriptionList.add(rset.getString(1));
           }});
       
       if (descriptionList.size() == 0) {
           return "<" + stateCode + ">";
       }
       else {
           return descriptionList.get(0);
       }
    }}
