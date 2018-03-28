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
 *
 * Purpose: this class is mainly for ...
 *
 * Project Name : passwordadvisor First created by: william.younang Creation
 * date: 16/feb/2015
 * 
 **/
public class PasswordNotifierConfiguration {

    private String passwordNotificatorEmail;
    private String bccEmail;
    private String ccEmail;
    private String mailSubject;
    private String validUserTemplateName;
    private String expiredUserTemplateName;
    private String senderSignature;
    private Integer numberOfDaysPreAlert;
    

    /**
     * @return the numberOfDaysPreAlert
     */
    public Integer getNumberOfDaysPreAlert() {
        return numberOfDaysPreAlert;
    }

    /**
     * @param numberOfDaysPreAlert
     *            the numberOfDaysPreAlert to set
     */
    public void setNumberOfDaysPreAlert(Integer numberOfDaysPreAlert) {
        this.numberOfDaysPreAlert = numberOfDaysPreAlert;
    }

    /**
     * @return the passwordNotificatorEmail
     */
    public String getPasswordNotificatorEmail() {
        return passwordNotificatorEmail;
    }

    /**
     * @param passwordNotificatorEmail
     *            the passwordNotificatorEmail to set
     */
    public void setPasswordNotificatorEmail(String passwordNotificatorEmail) {
        this.passwordNotificatorEmail = passwordNotificatorEmail;
    }

    /**
     * @return the bccEmail
     */
    public String getBccEmail() {
        return bccEmail;
    }

    /**
     * @param bccEmail
     *            the bccEmail to set
     */
    public void setBccEmail(String bccEmail) {
        this.bccEmail = bccEmail;
    }

    /**
     * @return the ccEmail
     */
    public String getCcEmail() {
        return ccEmail;
    }

    /**
     * @param ccEmail
     *            the ccEmail to set
     */
    public void setCcEmail(String ccEmail) {
        this.ccEmail = ccEmail;
    }

    /**
     * @return the mailSubject
     */
    public String getMailSubject() {
        return mailSubject;
    }

    /**
     * @param mailSubject
     *            the mailSubject to set
     */
    public void setMailSubject(String mailSubject) {
        this.mailSubject = mailSubject;
    }

    /**
     * @return the validUserTemplateName
     */
    public String getValidUserTemplateName() {
        return validUserTemplateName;
    }

    /**
     * @param validUserTemplateName
     *            the validUserTemplateName to set
     */
    public void setValidUserTemplateName(String validUserTemplateName) {
        this.validUserTemplateName = validUserTemplateName;
    }

    /**
     * @return the expiredUserTemplateName
     */
    public String getExpiredUserTemplateName() {
        return expiredUserTemplateName;
    }

    /**
     * @param expiredUserTemplateName
     *            the expiredUserTemplateName to set
     */
    public void setExpiredUserTemplateName(String expiredUserTemplateName) {
        this.expiredUserTemplateName = expiredUserTemplateName;
    }

    /**
     * @return the senderSignature
     */
    public String getSenderSignature() {
        return senderSignature;
    }

    /**
     * @param senderSignature
     *            the senderSignature to set
     */
    public void setSenderSignature(String senderSignature) {
        this.senderSignature = senderSignature;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PasswordNotifierConfiguration [passwordNotificatorEmail=");
        builder.append(passwordNotificatorEmail);
        builder.append(", bccEmail=");
        builder.append(bccEmail);
        builder.append(", ccEmail=");
        builder.append(ccEmail);
        builder.append(", mailSubject=");
        builder.append(mailSubject);
        builder.append(", validUserTemplateName=");
        builder.append(validUserTemplateName);
        builder.append(", expiredUserTemplateName=");
        builder.append(expiredUserTemplateName);
        builder.append(", senderSignature=");
        builder.append(senderSignature);
        builder.append(", numberOfDaysPreAlert=");
        builder.append(numberOfDaysPreAlert);
        builder.append("]");
        return builder.toString();
    }

    /**
     * Control if mandatory fields of the configuration are set.
     * 
     * @param configuration
     * @return true if all the mandatory fields are set and false otherwise
     */
    public boolean isValid() {
        
        if ( isStringEmptyOrNull(expiredUserTemplateName) 
                || isStringEmptyOrNull(mailSubject)
                || isStringEmptyOrNull(passwordNotificatorEmail)
                || isStringEmptyOrNull(senderSignature) 
                || isStringEmptyOrNull(validUserTemplateName) 
                || numberOfDaysPreAlert == null) {
            return false;
        } else {
            return true;
        }
    }

    private static Boolean isStringEmptyOrNull(final String stringToControl) {
        if (stringToControl == null || stringToControl.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }
}
