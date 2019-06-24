/*
 * Project Name : bestxengine
 * First created by: $Author$ - SoftSolutions! Italy
 * Creation date: $Date$
 * Purpose: ""
 *
 * Copright 1997-2012 SoftSolutions! srl
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
 *
 */
package it.softsolutions.bestx.services.instrument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.model.Instrument;

public class BondTypesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BondTypesService.class);

    private static String[] enabledBondTypes;

    public static void setEnabledBondTypes(String[] enabledBondTypes) {
        BondTypesService.enabledBondTypes = enabledBondTypes;
    }

    public static boolean checkBondType(Instrument bond) {
        boolean contains = false;
        for (String enabledBondType : enabledBondTypes) {
            if (enabledBondType.equalsIgnoreCase(bond.getBondType())) {
                LOGGER.debug("Check if the market maker can trade the instrument. Instrument bond type {} is one of those allowed.", bond.getBondType());
                contains = true;
                break;
            }
        }
        return contains;
    }

    // BESTX-382 added the definition here of what is a UST AMC 20190108
    public static boolean isUST(Instrument instrument) {
    	if("US GOVERNMENT".equals(instrument.getSecurityType())
    			&& instrument.getIssuerName() != null
    			&& (instrument.getIssuerName().contains("TREASURY")
    					|| instrument.getIssuerName().contains("STRIP")
    					|| instrument.getIssuerName().contains("TSY")))
//    			&& ("US TREASURY N/B".equals(instrument.getIssuerName())
//    			|| "TREASURY BILL".equals(instrument.getIssuerName())
//    			|| "STRIP PRINC".equals(instrument.getIssuerName())
//    			|| "STRIPS".equals(instrument.getIssuerName())
//    			|| "TSY INFL IX N/B".equals(instrument.getIssuerName())))
    		return true;
    	return false;
    }
}
