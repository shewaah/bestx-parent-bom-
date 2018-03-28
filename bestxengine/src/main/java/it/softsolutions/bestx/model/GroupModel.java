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

/**  
 *
 * Purpose: this class is mainly for ...  
 * 
 **/
public class GroupModel {
   
   private String groupName;
   private String description;
   private boolean locked;
    
   public String getGroupName() {
      return groupName;
   }
   
   public void setGroupName(String groupName) {
      this.groupName = groupName;
   }

   public String getDescription() {
      return description;
   }
   
   public void setDescription(String description) {
      this.description = description;
   }
   
   public boolean isLocked() {
      return locked;
   }

   public void setLocked(boolean locked) {
      this.locked = locked;
   }


   @Override
    public String toString() {
       
       StringBuilder builder = new StringBuilder();
       builder.append("Group [groupName=");
       builder.append(groupName);
       builder.append(", description=");
       builder.append(description);
       builder.append(", isLocked=");
       builder.append(isLocked() ? "true" : "false");
       builder.append("]");
       return builder.toString();
    }
    
}
