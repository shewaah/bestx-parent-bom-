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
package it.softsolutions.bestx.dao.bean;

import java.util.Date;

import it.softsolutions.bestx.UserIFC;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : passwordadvisor
 * First created by: william.younang
 * Creation date: 16/feb/2015
 * 
 **/
public class User implements UserIFC {

    private String email;
    private String name;
    private String surname;
    private Date passwordExpirationDate;
    private String userName;
//    private String password;
    private String description;
    private String specialist;
    private Integer isLocked;
    
    
    /**
     * @return the description
     */
    @Override
	public String getDescription() {
        return description;
    }
    /**
     * @param description the description to set
     */
    @Override
	public void setDescription(String description) {
        this.description = description;
    }
    /**
     * @return the specialist
     */
    @Override
	public String getSpecialist() {
        return specialist;
    }
    /**
     * @param specialist the specialist to set
     */
    @Override
	public void setSpecialist(String specialist) {
        this.specialist = specialist;
    }
    /**
     * @return the isLocked
     */
    @Override
	public Integer getIsLocked() {
        return isLocked;
    }
    /**
     * @param isLocked the isLocked to set
     */
    @Override
	public void setIsLocked(int isLocked) {
        this.isLocked = isLocked;
    }
    /**
     * @return the email
     */
    @Override
	public String getEmail() {
        return email;
    }
    /**
     * @param email the email to set
     */
    @Override
	public void setEmail(String email) {
        this.email = email;
    }
    /**
     * @return the name
     */
    @Override
	public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    @Override
	public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the surname
     */
    @Override
	public String getSurname() {
        return surname;
    }
    /**
     * @param surname the surname to set
     */
    @Override
	public void setSurname(String surname) {
        this.surname = surname;
    }
    /**
     * @return the passwordExpirationDate
     */
    @Override
	public Date getPasswordExpirationDate() {
        return passwordExpirationDate;
    }
    /**
     * @param passwordExpirationDate the passwordExpirationDate to set
     */
    @Override
	public void setPasswordExpirationDate(Date passwordExpirationDate) {
        this.passwordExpirationDate = passwordExpirationDate;
    }
    /**
     * @return the userName
     */
    @Override
	public String getUserName() {
        return userName;
    }
    /**
     * @param userName the userName to set
     */
    @Override
	public void setUserName(String userName) {
        this.userName = userName;
    }
    /**
     * @return the password
     */
    @Override
	public String getPassword() {
//        return password;
    	return "";
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("User [email=");
        builder.append(email);
        builder.append(", name=");
        builder.append(name);
        builder.append(", surname=");
        builder.append(surname);
        builder.append(", passwordExpirationDate=");
        builder.append(passwordExpirationDate);
        builder.append(", userName=");
        builder.append(userName);
        builder.append(", password=");
        builder.append("*****");
        builder.append("]");
        return builder.toString();
    }
    
}
