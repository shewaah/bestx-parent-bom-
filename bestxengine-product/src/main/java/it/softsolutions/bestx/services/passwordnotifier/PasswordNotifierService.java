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

/**
 * Email notification service used to alert that a password is near expiration.
 *
 * Project Name : passwordadvisor First created by: william.younang Creation
 * date: 11/feb/2015
 * 
 **/
public interface PasswordNotifierService {

    /**
     * Load the configurations from database and control that mandatory fields
     * are set correctly.
     */
    void init() throws Exception;

    /**
     * Download the users from database. For each user, control the number of
     * days to expiration of password. According to the number of days pre-alert,
     * send a mail to advise that the password is near to expiration or has expired at all.
     * 
     * @throws NotificationException
     */
    void performPasswordExpiryNotification() throws Exception;

}
