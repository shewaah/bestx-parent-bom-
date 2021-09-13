/*
* Copyright 1997-2021 SoftSolutions! srl 
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
* Project Name : bestxengine 
* First created by: stefano.pontillo 
* Creation date: 28 lug 2021 
* 
**/
public class ServiceStatus {

   private String serviceCode;
   private boolean disabled;
   private String downCause;
   /**
    * @param serviceCode
    * @param disabled
    * @param downCause
    */
   public ServiceStatus(String serviceCode, boolean disabled, String downCause){
      this.serviceCode = serviceCode;
      this.disabled = disabled;
      this.downCause = downCause;
   }
   
   public String getServiceCode() {
      return serviceCode;
   }
   
   public void setServiceCode(String serviceCode) {
      this.serviceCode = serviceCode;
   }
   
   public boolean isDisabled() {
      return disabled;
   }
   
   public void setDisabled(boolean disabled) {
      this.disabled = disabled;
   }
   
   public String getDownCause() {
      return downCause;
   }
   
   public void setDownCause(String downCause) {
      this.downCause = downCause;
   }
   
   

}
