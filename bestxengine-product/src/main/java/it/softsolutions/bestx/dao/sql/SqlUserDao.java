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
package it.softsolutions.bestx.dao.sql;

import it.softsolutions.bestx.dao.UserDao;
import it.softsolutions.bestx.dao.bean.User;

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
 * Project Name : bestxengine-product First created by: ruggero.rizzo Creation
 * date: 12/set/2013
 * 
 **/
public class SqlUserDao implements UserDao {
    
	private static final Logger LOGGER = LoggerFactory.getLogger(SqlUserDao.class);
	
	private static final String SELECT_ALL_USERS = "SELECT * FROM users";

	private JdbcTemplate jdbcTemplate;

	/**
	 * Set the jdbcTemplate used to execute queries
	 * 
	 * @param jdbcTemplate
	 *            the JDBCTemplate to set
	 */
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

    @Override
    public List<User> retrieveAllUsers() throws Exception {
        LOGGER.debug("");
        
        final List<User> users = new ArrayList<>();
        
        jdbcTemplate.query(SELECT_ALL_USERS, new RowCallbackHandler() {
            public void processRow(ResultSet rset) throws SQLException {
                User user = new User();
                user.setEmail(rset.getString("email"));
                user.setName(rset.getString("name"));
                user.setSurname(rset.getString("surname"));
                user.setPasswordExpirationDate(rset.getDate("expirationDate"));
                user.setUserName(rset.getString("username"));
                user.setDescription(rset.getString("description"));
                user.setSpecialist(rset.getString("specialist"));
                user.setIsLocked(rset.getInt("isLocked"));
                
                users.add(user);
            }}
        );
        
        return users;
    }

}
