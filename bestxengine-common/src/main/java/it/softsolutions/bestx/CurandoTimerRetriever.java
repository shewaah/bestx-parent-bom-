/*
* Copyright 1997-2018 SoftSolutions! srl 
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
 
package it.softsolutions.bestx;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-common 
* First created by: stefano.pontillo 
* Creation date: 25 set 2018 
* 
**/
public class CurandoTimerRetriever {
   private static final Logger LOGGER = LoggerFactory.getLogger(CurandoTimerRetriever.class);

   private String propertyFile;
   private String timerPropertiesRoot;
   private String currentProfileName;
   private CurandoTimerProfile currentProfile;
   private Map<String, CurandoTimerProfile> profiles;
   
   public void init() throws Exception {
      LOGGER.debug("Load CurandoTimerRetriever");
  
      profiles = new HashMap<String, CurandoTimerProfile>();
      
      LOGGER.debug("Starting to load the property file : {}", this.propertyFile);
      InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(this.propertyFile);
      if(stream == null) {
         throw new IllegalStateException("Timer profiles properties file " + this.propertyFile + " doesn't exist!!");
      }
      Properties properties = new Properties();
      properties.load(stream);
      LOGGER.debug("Properties loaded");
      
      Set<String> propNames = properties.stringPropertyNames();
      List<String> propNamesSorted = propNames.stream().collect(Collectors.toList());
      Collections.sort(propNamesSorted);
      
      for (String propName : propNamesSorted) {
         if (propName.startsWith(timerPropertiesRoot)) {
            String[] keyValues = propName.split("\\.");
            
            CurandoTimerProfile selProfile = profiles.get(keyValues[1]);
            if (selProfile == null) {
               selProfile = new CurandoTimerProfile(keyValues[1]);
               profiles.put(keyValues[1], selProfile);
            }
            if ("Threshold".equalsIgnoreCase(keyValues[2])) {
               selProfile.setThreshold(Integer.parseInt(keyValues[3]), new Double(properties.getProperty(propName)));
            } else if ("Timeout".equalsIgnoreCase(keyValues[2])) {
               if ("default".equalsIgnoreCase(keyValues[3])) {
                  selProfile.setTimeout(new Integer(properties.getProperty(propName)));
               } else {
                  selProfile.setTimeout(Integer.parseInt(keyValues[3]), new Integer(properties.getProperty(propName)));
               }
            }
         }
      }
      //Sets the currentProfile given by spring configuration
      if (currentProfileName != null) {
         currentProfile = profiles.get(currentProfileName);
      }
   }
   
   /**
    * 
    * @param profileName
    * @return the profile requested or the current  
    * @throws CloneNotSupportedException 
    */
   public CurandoTimerProfile getProfile(String profileName) {
      try {
         if (profileName != null) {
            return profiles.get(profileName).clone();
         } else {
            return currentProfile.clone();
         }
      } catch (CloneNotSupportedException e) {
         return null;
      }
   }

   public CurandoTimerProfile getProfile() {
      return getProfile(null);
   }
   
   
   
   /**
    * @param propertyFile the propertyFile to set
    */
   public void setPropertyFile(String propertyFile) {
      this.propertyFile = propertyFile;
   }
   
   /**
    * @param timerPropertiesRoot the timerPropertiesRoot to set
    */
   public void setTimerPropertiesRoot(String timerPropertiesRoot) {
      this.timerPropertiesRoot = timerPropertiesRoot;
   }

   /**
    * @param currentProfileName the currentProfileName to set
    */
   public void setCurrentProfileName(String currentProfileName) {
      this.currentProfileName = currentProfileName;
   }
}
