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

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Days;

/**
 *
 * Purpose: this class is mainly for ...
 *
 * Project Name : passwordadvisor First created by: william.younang Creation
 * date: 16/feb/2015
 * 
 **/
public class PasswordNotifierUtils {

    public static final String NOTIFICATOR_EMAIL = "PasswordNotification.passwordNotificatorEmail";
    public static final String BCC_EMAIL = "PasswordNotification.bccEmail";
    public static final String CC_EMAIL = "PasswordNotification.ccEmail";
    public static final String MAIL_SUBJECT = "PasswordNotification.mailSubject";
    public static final String VALID_USER_TEMPLATE_NAME = "PasswordNotification.validUserTemplateName";
    public static final String EXPIRED_ADMIN_TEMPLATE_NAME = "PasswordNotification.expiredAdminTemplateName";
    public static final String EXPIRED_USER_TEMPLATE_NAME = "PasswordNotification.expiredUserTemplateName";
    public static final String SENDER_SIGNATURE = "PasswordNotification.senderSignature";
    public static final String NUMBER_OF_DAYS_PRE_ALERT = "PasswordNotification.numberOfDaysPreAlert";

    /**
     * Given the user password's expiration date, returns the number of days to
     * expiration from the current date.
     * 
     * @param userPasswordExpirationdate
     *            the user password's expiration date
     * @return a positive integer if the password is not yet expire, and a
     *         negative integer otherwise
     * @throws NotificationException
     *             if an error occur during this operation
     */
    public static int daysToExpirationOfPassword(Date userPasswordExpirationDate) throws Exception {
        DateTime expirationDateTime = new DateTime(userPasswordExpirationDate);
        DateTime currentDateTime = new DateTime();
        int daysToExpiration = Days.daysBetween(currentDateTime, expirationDateTime).getDays();

        return daysToExpiration;
    }
    

}
