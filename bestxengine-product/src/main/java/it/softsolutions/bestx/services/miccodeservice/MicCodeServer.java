/*
* Copyright 1997-2012 SoftSolutions! srl 
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
package it.softsolutions.bestx.services.miccodeservice;

import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.services.MICCodeService;

/**
 * 
 * Purpose: MIC Code retrieval service
 * 
 * Project Name : bestxengine-product 
 * First created by: ruggero.rizzo 
 * Creation date: 26/ott/2012
 * 
 **/
public class MicCodeServer implements MICCodeService {

    public MicCodeServer() {
    }

    public void init() {
    }

    //FIXME is MIC code chosen still this way for BondVision?
    @Override
    public String getMICCode(Market m, Instrument i) {
        if (m == null) {
            return "XXXX";
        }
        return m.getMicCode();
    }

    @Override
    public boolean isOTCMarket(Market m) {
        return "XXXX".equalsIgnoreCase(m.getMicCode());
    }
}
