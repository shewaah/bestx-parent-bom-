/*
 * Copyright 1997- 2015 SoftSolutions! srl 
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
package it.softsolutions.bestx.services.passwordnotifier;

import it.softsolutions.bestx.web.usermanagement.password.BestXPasswordManager;
import it.softsolutions.bestx.web.usermanagement.password.PasswordGenerator;
import it.softsolutions.bestx.web.usermanagement.util.BestXConfigurationAccess;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs
 * First created by: william.younang
 * Creation date: 02/mar/2015
 * 
 **/
public class MyPasswordGenerator {

    /**
     * @param args
     * @throws Exception 
     */
    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context= new ClassPathXmlApplicationContext("cs-spring.xml");
        PasswordGenerator passwordGenerator = (PasswordGenerator) context.getBean("passwordGenerator");
        JdbcTemplate jdbcTemplate = (JdbcTemplate) context.getBean("jdbcTemplate");
        String userName = "Davide R.";
        boolean isadministrator = passwordGenerator.isAdmin(userName);

        if (isadministrator) {
        	Integer maxNumOfPrevPwds = BestXConfigurationAccess.loadIntegerProperty(jdbcTemplate.getDataSource(), BestXPasswordManager.MAX_NUMBER_OF_PREVIOUS_PASSWORDS_PROPERTY);
        	if (maxNumOfPrevPwds != null) {
        		System.out.println(passwordGenerator.generatePassword(userName, maxNumOfPrevPwds));
        	} else {
        		System.err.print("Cannot find property " + BestXPasswordManager.MAX_NUMBER_OF_PREVIOUS_PASSWORDS_PROPERTY+ " in the database");
        	}
        } else {
            System.out.println("Not Administrator");
        }
    }

}
