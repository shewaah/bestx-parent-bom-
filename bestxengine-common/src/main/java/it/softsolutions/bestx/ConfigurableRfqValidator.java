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

import it.softsolutions.bestx.model.Rfq;
import it.softsolutions.bestx.validators.RfqResult;
import it.softsolutions.bestx.validators.RfqValidator;

import java.util.List;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-common First created by: davide.rossoni Creation date: 19/feb/2013
 * 
 **/
public class ConfigurableRfqValidator implements RfqValidator {

    private List<RfqValidator> rfqValidators;

    public void setRfqValidators(List<RfqValidator> rfqValidators) {
        this.rfqValidators = rfqValidators;
    }

    public RfqResult validateRfq(Operation operation, Rfq rfq) {
        RfqResult result = null;
        for (RfqValidator validator : rfqValidators) {
            result = validator.validateRfq(operation, rfq);
            if (!result.isValid()) {
                break;
            }
        }
        return result;
    }
}
