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

//import static org.junit.Assert.fail;
import java.util.Date;

import it.softsolutions.bestx.dao.bean.User;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * Purpose: this class is mainly for ...
 *
 * Project Name : bestxengine-cs First created by: william.younang Creation
 * date: 18/feb/2015
 * 
 **/
public class PasswordNotifierServiceImplTest {

    private static PasswordNotifierServiceImpl passwordNotifierService;
    private static ClassPathXmlApplicationContext context;

    @BeforeClass
    public static void setUp() throws Exception {
        context = new ClassPathXmlApplicationContext("passwordnotification-spring.xml");
        passwordNotifierService = (PasswordNotifierServiceImpl) context.getBean("passwordNotifierService");

    }

    @Test(expected = Exception.class)
    public void incorrectUserMailFormat() throws Exception {
        User user = new User();
        user.setEmail("william@mimi");
        passwordNotifierService.sendMail(user, false);
    }

    @Test//(expected = Exception.class)
    public void anotherIncorrectUserMailFormat() throws Exception {
        User user = new User();
        user.setEmail("william@yahoo.f");
        passwordNotifierService.sendMail(user, false);
    }
    
    @Test(expected = Exception.class)
    public void tooShortUserMail() throws Exception{
        User user = new User();
        user.setEmail("1@.fr");
        passwordNotifierService.sendMail(user, false);
    }
    
    @Test(expected = Exception.class)
    public void nullUserMail() throws Exception{
        User user = new User();
        user.setEmail(null);
        passwordNotifierService.sendMail(user, false);
    }
    
    @Test(expected = Exception.class)
    public void nullUser() throws Exception{
        passwordNotifierService.sendMail(null, false);
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        context.close();
        //Thread.sleep(1000);
    }

    /**
     * In this specific test, some configurations of the database is not correct (incorrect bccEmail, etc). Uncomment this if all the configurations are correct
     * @throws Exception
     */
    @Test//(expected = Exception.class)
    public void incorrectConfigurations() throws Exception{
        User user = new User();
        user.setEmail("williamyounang@yahoo.fr");
        user.setName("William");
        user.setSurname("Younang");
        user.setPasswordExpirationDate(new Date());
        passwordNotifierService.sendMail(user, false);
    }
    
}
