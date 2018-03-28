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
package it.softsolutions.bestx.model;

import java.util.Date;
import java.util.Set;

import it.softsolutions.bestx.UserIFC;

/**  
 *
 * Purpose: this class is mainly for ...  
 * 
 **/
public class UserModel implements UserIFC {
   
   public static final String SUPERTRADER_GROUP = "supertrader";
    
   public void setPassword(String password) {
      this.password = password;
   }
   
   public void setIsLocked(Integer isLocked) {
      this.isLocked = isLocked;
   }

   private String email;
    private String name;
    private String surname;
    private Date passwordExpirationDate;
    private String userName;
    private String password;
    private String description;
    private String specialist;
    private Integer isLocked;
    private Set<GroupModel> groupModels;
    

   /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
    /**
     * @return the specialist
     */
    public String getSpecialist() {
        return specialist;
    }
    /**
     * @param specialist the specialist to set
     */
    public void setSpecialist(String specialist) {
        this.specialist = specialist;
    }
    /**
     * @return the isLocked
     */
    public Integer getIsLocked() {
        return isLocked;
    }
    /**
     * @param isLocked the isLocked to set
     */
    public void setIsLocked(int isLocked) {
        this.isLocked = isLocked;
    }
    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }
    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the surname
     */
    public String getSurname() {
        return surname;
    }
    /**
     * @param surname the surname to set
     */
    public void setSurname(String surname) {
        this.surname = surname;
    }
    /**
     * @return the passwordExpirationDate
     */
    public Date getPasswordExpirationDate() {
        return passwordExpirationDate;
    }
    /**
     * @param passwordExpirationDate the passwordExpirationDate to set
     */
    public void setPasswordExpirationDate(Date passwordExpirationDate) {
        this.passwordExpirationDate = passwordExpirationDate;
    }
    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }
    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }
    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }
    
   public Set<GroupModel> getGroupModels() {
      return groupModels;
   }
   
   public void setGroupModels(Set<GroupModel> groupModels) {
      this.groupModels = groupModels;
   }
   
   public boolean isSuperTrader() {
      for (GroupModel groupModel:groupModels) {
         if (SUPERTRADER_GROUP.equals(groupModel.getGroupName().toLowerCase())) {
            return true;
         }
      }
      return false;
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
