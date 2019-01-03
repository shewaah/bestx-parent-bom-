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
package it.softsolutions.bestx.finders.db;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.dao.CustomerParamDao;
import it.softsolutions.bestx.dao.CustomerTickerDao;
import it.softsolutions.bestx.finders.CommissionFinder;
import it.softsolutions.bestx.model.Commission;
import it.softsolutions.bestx.model.Commission.CommissionType;
import it.softsolutions.bestx.model.CommissionRow;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.CustomerParam;
import it.softsolutions.bestx.model.CustomerTicker;
import it.softsolutions.bestx.model.Instrument;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-common 
 * First created by: davide.rossoni 
 * Creation date: 22/ago/2012 
 * 
 **/
public class DbCommissionFinder implements CommissionFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(DbCommissionFinder.class);

    private CustomerParamDao customerParamDao;
    private CustomerTickerDao customerTickerDao;
    
    public void init() {
        if (customerParamDao == null) {
            LOGGER.error("CustomerParamDao property not set");
        }
        if (customerTickerDao == null) {
            LOGGER.error("CustomerTickerDao property not set");
        }
    }
    
    /*
     * OUT OF DATE- Alla richiesta di commissioni vengono caricate dal DAO apposito entrambi i tipi possibili, quelle per ticker e quelle per portfolio.
     * Le commissioni per ticker sono collegate ai ticker RTFI. Se l'instrument e' abbinato ad uno dei ticker associati al cliente nella
     * CustomerTickerTable, quella commissione rientra nelle righe accettate e viene inserita nella lista. Si procede poi a controllare il
     * portfolioId associato all'instrument. La riga di commissioni prevista per quel portfolio viene caricata nella lista. Vi e' un'altra
     * condizione che va soddisfatta, ovvero un titolo se e' nel Monte Titoli deve iniziare con "IT" altrimenti no. E' un titolo da Monte
     * Titoli se la colonna isinType e' valorizzata con false.
     * 
     * La commission row ha, come penultimo valore, la commissione, espressa, in caso di prima riga e quindi come fee minina, in amount,
     * altrimenti e' un tick percentuale.
     */
    @Override
    public Collection<CommissionRow> getCommissions(Customer customer, Instrument instrument) {
        String clientCode = customer != null ? customer.getFixId() : null;
        Integer marketID = 1;
        
        Integer portfolioID = (instrument != null && instrument.getInstrumentAttributes() != null && instrument.getInstrumentAttributes().getPortfolio() != null) ? instrument.getInstrumentAttributes().getPortfolio().getId() : null;
        portfolioID = portfolioID == null ? 0 : portfolioID;
        
        String isin = instrument != null ? instrument.getIsin() : null;
        Boolean isinType = isin != null && !isin.startsWith("IT");

        // Retrieve customerParam commissions
        CustomerParam customerParam = customerParamDao.getCustomerParam(clientCode, marketID, portfolioID, isinType);
        
        List<CommissionRow> res = new ArrayList<CommissionRow>();
        
        if (customerParam != null) {
            res.add(new CommissionRow(BigDecimal.ZERO, toBigDecimal(customerParam.getMinimumFeeMaxSize()), new Commission(toBigDecimal(customerParam.getMinimumFee()), CommissionType.AMOUNT),
                    toBigDecimal(customerParam.getMinimumFee()), toBigDecimal(customerParam.getMinimumFeeMaxSize())));
            res.add(new CommissionRow(toBigDecimal(customerParam.getMinimumFeeMaxSize()), toBigDecimal(customerParam.getFirstQtyThreshold()), new Commission(toBigDecimal(customerParam.getFirstSpread()),
                    CommissionType.TICKER), toBigDecimal(customerParam.getMinimumFee()), toBigDecimal(customerParam.getMinimumFeeMaxSize())));
            res.add(new CommissionRow(toBigDecimal(customerParam.getFirstQtyThreshold()), toBigDecimal(customerParam.getSecondQtyThreshold()), new Commission(toBigDecimal(customerParam.getSecondSpread()),
                    CommissionType.TICKER), toBigDecimal(customerParam.getMinimumFee()), toBigDecimal(customerParam.getMinimumFeeMaxSize())));
            res.add(new CommissionRow(toBigDecimal(customerParam.getSecondQtyThreshold()), toBigDecimal(customerParam.getThirdQtyThreshold()), new Commission(toBigDecimal(customerParam.getThirdSpread()),
                    CommissionType.TICKER), toBigDecimal(customerParam.getMinimumFee()), toBigDecimal(customerParam.getMinimumFeeMaxSize())));
        }

        // Retrieve customerTicker commissions
        String rtfiTicker = instrument != null ? instrument.getBondType() : null;
        CustomerTicker customerTicker  = customerTickerDao.getCustomerTicker(clientCode, rtfiTicker);
        
        if (customerTicker != null) {
            res.add(new CommissionRow(BigDecimal.ZERO, null, new Commission(toBigDecimal(customerTicker.getSpread()), CommissionType.TICKER), BigDecimal.ZERO, BigDecimal.ZERO));
        }
        
        return res;
    }

    private BigDecimal toBigDecimal(Double value) {
        return (value != null) ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }
    
    public void setCustomerParamDao(CustomerParamDao customerParamDao) {
        this.customerParamDao = customerParamDao;
    }

    public void setCustomerTickerDao(CustomerTickerDao customerTickerDao) {
        this.customerTickerDao = customerTickerDao;
    }
    
    
}