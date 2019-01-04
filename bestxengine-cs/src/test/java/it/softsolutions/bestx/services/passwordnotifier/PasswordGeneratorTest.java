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

import static org.junit.Assert.*;
import it.softsolutions.bestx.web.usermanagement.password.PasswordGenerator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs
 * First created by: william.younang
 * Creation date: 02/mar/2015
 * 
 **/
public class PasswordGeneratorTest {

    private static PasswordGenerator passwordGenerator;
    private static ClassPathXmlApplicationContext context;
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
       context= new ClassPathXmlApplicationContext("cs-spring.xml");
       passwordGenerator = (PasswordGenerator) context.getBean("passwordGenerator");
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        context.close();
        Thread.sleep(1000);
    }

    @Test
    public void unexisitingUserName() throws Exception {
        String userName = "Pinco palino";
        boolean isadministrator = passwordGenerator.isAdmin(userName);
        assertFalse(isadministrator);
    }
    
    @Test
    public void exisitingUserNotAdministrator() throws Exception {
        String userName = "paolo";
        boolean isadministrator = passwordGenerator.isAdmin(userName);
        assertFalse(isadministrator);
    }
    
    @Test
    public void exisitingUserAdministrator() throws Exception {
        String userName = "paolo";
        boolean isadministrator = passwordGenerator.isAdmin(userName);
        assertTrue(isadministrator);
    }

}
