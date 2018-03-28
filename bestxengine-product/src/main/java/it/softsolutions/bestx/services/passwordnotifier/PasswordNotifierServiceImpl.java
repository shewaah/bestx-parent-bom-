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

import java.io.StringWriter;
import java.util.List;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;

import it.softsolutions.bestx.dao.BestXConfigurationDao;
import it.softsolutions.bestx.dao.UserDao;
import it.softsolutions.bestx.dao.bean.BestXConfiguration;
import it.softsolutions.bestx.dao.bean.User;
import it.softsolutions.bestx.services.DateService;

/**
 * 
 *
 * Purpose: this class is mainly for automatically sending email notification
 * for almost expired passwords. It is based on Spring Framework, Velocity and
 * javaMail.
 *
 * Project Name : passwordadvisor First created by: william.younang Creation
 * date: 13/feb/2015
 * 
 *
 */
public class PasswordNotifierServiceImpl implements PasswordNotifierService {

    private final static String PROPERTIES_FILE = "BESTX.properties";
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordNotifierServiceImpl.class);

    private UserDao userDao;
    private BestXConfigurationDao bestXConfigurationDao;
    private JavaMailSender mailSender;
    private VelocityEngine velocityEngine;
    private PasswordNotifierConfiguration passwordNotifierConfiguration;

    @Override
    public void init() {

        try {
            this.passwordNotifierConfiguration = loadPasswordNotifierConfiguration();
        } catch (Exception e) {
            LOGGER.error("Error while setting the configurations {}", e);
        }

    }

   
    private PasswordNotifierConfiguration loadPasswordNotifierConfiguration() throws Exception {
        PasswordNotifierConfiguration passwordNotificationConfiguration = new PasswordNotifierConfiguration();
        List<BestXConfiguration> bestXConfigurations = bestXConfigurationDao.retrieveAllConfigurations();

        for (BestXConfiguration bestXConfiguration : bestXConfigurations) {
            
            String name = bestXConfiguration.getPropertyName();
            
            if (name.startsWith("PasswordNotification.")) {
                
                if (name.equals(PasswordNotifierUtils.EXPIRED_USER_TEMPLATE_NAME)) {
                    passwordNotificationConfiguration.setExpiredUserTemplateName(bestXConfiguration.getPropertyValue());
                } else if (name.equals(PasswordNotifierUtils.BCC_EMAIL)) {
                    passwordNotificationConfiguration.setBccEmail(bestXConfiguration.getPropertyValue());
                } else if (name.equals(PasswordNotifierUtils.CC_EMAIL)) {
                    passwordNotificationConfiguration.setCcEmail(bestXConfiguration.getPropertyValue());
                } else if (name.equals(PasswordNotifierUtils.MAIL_SUBJECT)) {
                    passwordNotificationConfiguration.setMailSubject(bestXConfiguration.getPropertyValue());
                } else if (name.equals(PasswordNotifierUtils.NOTIFICATOR_EMAIL)) {
                    passwordNotificationConfiguration.setPasswordNotificatorEmail(bestXConfiguration.getPropertyValue());
                } else if (name.equals(PasswordNotifierUtils.NUMBER_OF_DAYS_PRE_ALERT)) {
                    passwordNotificationConfiguration.setNumberOfDaysPreAlert(Integer.parseInt(bestXConfiguration.getPropertyValue()));
                } else if (name.equals(PasswordNotifierUtils.SENDER_SIGNATURE)) {
                    passwordNotificationConfiguration.setSenderSignature(bestXConfiguration.getPropertyValue());
                } else if (name.equals(PasswordNotifierUtils.VALID_USER_TEMPLATE_NAME)) {
                    passwordNotificationConfiguration.setValidUserTemplateName(bestXConfiguration.getPropertyValue());
                }
            }
        }
        
        Configuration configuration = null;
        try {
            configuration = new PropertiesConfiguration(PROPERTIES_FILE);
        } catch (ConfigurationException e) {
         LOGGER.error("{}, {}", e.getMessage(), e);
         return null;
        }
        
        String notificatorEmail = configuration.getString("PasswordNotifierService.passwordNotificatorEmail");
        passwordNotificationConfiguration.setPasswordNotificatorEmail(notificatorEmail);
        
        if (!passwordNotificationConfiguration.isValid()) {
            LOGGER.error("Configuration is not valid: {}", passwordNotificationConfiguration);
            throw new Exception("Some of the mandatory fields is not set correctly in the configurations.");
        }
        
        LOGGER.debug("load configurations {}", passwordNotificationConfiguration);
        
        return passwordNotificationConfiguration;
    }

    /**
     * @param mailSender
     *            the mailSender to set
     */
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * @param velocityEngine
     *            the velocityEngine to set
     */
    public void setVelocityEngine(VelocityEngine velocityEngine) {
        this.velocityEngine = velocityEngine;
    }

    @Override
    public void performPasswordExpiryNotification() throws Exception {
        // retrieve users with expired password
        List<User> allUsers = userDao.retrieveAllUsers();

        // get the users which passwords are near expiration.
        for (User user : allUsers) {
            LOGGER.debug("proceding current user {}", user);

            int daysPreAlert = passwordNotifierConfiguration.getNumberOfDaysPreAlert();
            int daysToExpirationOfPassword = PasswordNotifierUtils.daysToExpirationOfPassword(user.getPasswordExpirationDate());
            
            LOGGER.debug("daysPreAlert = {}, daysToExpirationOfPassword = {}", daysPreAlert, daysToExpirationOfPassword);
            
            if (daysToExpirationOfPassword <= daysPreAlert) {
                if (daysToExpirationOfPassword < 0) {
                    sendMail(user, true);
                } else {
                    sendMail(user, false);
                }
            }
        }        
    }
    
    protected void sendMail(final User user, final boolean isPasswordExpired) throws Exception {
        LOGGER.debug("user = {}, isPasswordExpired = {}", user, isPasswordExpired);

        MimeMessagePreparator preparator = new MimeMessagePreparator() {

            Template template = null;
            VelocityContext velocityContext = new VelocityContext();

            public void prepare(MimeMessage mimeMessage) throws Exception {
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                
                /*
                 * the message is sent to administrator with the auto-generated
                 * password. In this scenario it is assume that the password is
                 * already expired
                 */
                if (isPasswordExpired) {
                    template = velocityEngine.getTemplate(passwordNotifierConfiguration.getExpiredUserTemplateName());
                } else {
                    template = velocityEngine.getTemplate(passwordNotifierConfiguration.getValidUserTemplateName());
                }

                if (passwordNotifierConfiguration.getBccEmail() != null && passwordNotifierConfiguration.getBccEmail().length() > 0) {
                    message.setBcc(passwordNotifierConfiguration.getBccEmail());
                }

                if (passwordNotifierConfiguration.getCcEmail() != null && passwordNotifierConfiguration.getCcEmail().length() > 0) {
                    message.setCc(passwordNotifierConfiguration.getCcEmail());
                }

                message.setTo(user.getEmail());

                message.setFrom(new InternetAddress(passwordNotifierConfiguration.getPasswordNotificatorEmail()));
                message.setSubject(passwordNotifierConfiguration.getMailSubject());
                message.setSentDate(DateService.newLocalDate());

                velocityContext.put("name", user.getName());
                velocityContext.put("surname", user.getSurname());
                velocityContext.put("passwordExpirationDate", user.getPasswordExpirationDate());
                velocityContext.put("notificatorEmail", passwordNotifierConfiguration.getPasswordNotificatorEmail());
                velocityContext.put("senderSignature", passwordNotifierConfiguration.getSenderSignature());

                StringWriter stringWriter = new StringWriter();

                template.merge(velocityContext, stringWriter);

                message.setText(stringWriter.toString(), true);
            }
        };

        mailSender.send(preparator);
    }


    /**
     * @param userDao the userDao to set
     */
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * @param bestXConfigurationDao the bestXConfigurationDao to set
     */
    public void setBestXConfigurationDao(BestXConfigurationDao bestXConfigurationDao) {
        this.bestXConfigurationDao = bestXConfigurationDao;
    }
}
