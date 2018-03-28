/*
 * Copyright 1997-2013 SoftSolutions! srl 
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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Purpose: Load and manage messages with fair locale.
 *
 * Project Name : bestxengine 
 * First created by: davide.rossoni 
 * Creation date: 20/feb/2013 
 * 
 **/
public class Messages {
    private static String bundleName;
    private static String language;
    private static String country;
    private static ResourceBundle RESOURCE_BUNDLE;

    /**
     * STATIC: getString
     * 
     * @param key
     * @return message
     */
    public static String getString(String key) {
        try {
            checkBundle();
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
    
    /**
     * STATIC getString with params
     * 
     * @param key
     * @param params
     * @return
     */
    public static String getString(String key, Object... params) {
        MessageFormat formatter = new MessageFormat(getString(key));
        return formatter.format(params);
    }

    /**
     * Set Boundle Name
     * 
     * @param bundleName
     */
    public void setBundleName(String bundleName) {
        Messages.bundleName = bundleName;
    }

    /**
     * Set Language
     * 
     * @param language
     */
    public void setLanguage(String language) {
        Messages.language = language;
    }

    /**
     * Set Country
     * 
     * @param country
     */
    public void setCountry(String country) {
        Messages.country = country;
    }

    /**
     * STATIC Check Boundle
     */
    private static void checkBundle() {
        if (RESOURCE_BUNDLE == null) {
            RESOURCE_BUNDLE = ResourceBundle.getBundle(bundleName, new Locale(language, country));
        }
    }
}
