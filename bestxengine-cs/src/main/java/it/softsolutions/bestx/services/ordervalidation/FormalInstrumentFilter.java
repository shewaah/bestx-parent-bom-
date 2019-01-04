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
package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.customservice.CustomService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
 *
 * Purpose: this class checks if the order's instrument is known to BestX!
 * If not the order must be rejected. 
 *
 * Project Name : bestxengine-cs 
 * First created by: ruggero.rizzo 
 * Creation date: 09/set/2013 
 * 
 **/
public class FormalInstrumentFilter implements OrderValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FormalInstrumentFilter.class);
    private CustomService grdLiteService;
    private boolean rejectWhenBloombergIsBest;
    
    @Override
    public OrderResult validateOrder(Operation operation, Order order) {
        OrderResultBean result = new OrderResultBean();
        result.setOrder(order);
        result.setReason("");
        Instrument instrument = order.getInstrument();
        if (instrument == null || !instrument.isInInventory()) {
            if (!grdLiteService.isActive() || !grdLiteService.isAvailable()){
                LOGGER.info("Order {} rejected, instrument {} not found and grdLite service not available", order.getFixOrderId(), order.getInstrumentCode());
                result.setReason(Messages.getString("FormalInstrumentNotAvailable"));
            } else {
                result.setValid(true);
                LOGGER.info("Order {} without a known instrument, will be looked for through grdLite, isin {}", order.getFixOrderId(), order.getInstrumentCode());
            }
        } else {
            //instrument available, when Bloomberg is active check if the BBSettlementDate exists, it will be needed later
            if (!rejectWhenBloombergIsBest && instrument.getBBSettlementDate() == null){
                result.setReason(Messages.getString("FormalInstrumentBBSettlementDateNull", order.getInstrumentCode()));
            } else {
                //instrument available, check the ISIN (in place of the previous FormalISINFilter)
                result.setValid(instrument.getIsin() != null && instrument.getIsin().length() == 12);
                if (!result.isValid()) {
                    result.setReason(Messages.getString("FormalISIN.0", instrument));
                }
            }
        }
        return result;
    }

    @Override
    public boolean isDbNeeded() {
        return false;
    }

    @Override
    public boolean isInstrumentDbCheck() {
        return false;
    }

    /**
     * @return the grdLiteService
     */
    public CustomService getGrdLiteService() {
        return grdLiteService;
    }

    /**
     * @param grdLiteService the grdLiteService to set
     */
    public void setGrdLiteService(CustomService grdLiteService) {
        this.grdLiteService = grdLiteService;
    }

    /**
     * @param rejectWhenBloombergIsBest the rejectWhenBloombergIsBest to set
     */
    public void setRejectWhenBloombergIsBest(boolean rejectWhenBloombergIsBest) {
        this.rejectWhenBloombergIsBest = rejectWhenBloombergIsBest;
    }
}
