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
package it.softsolutions.bestx.connections.amosgateway;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.connections.csfix.CSFixMessageFields;
import it.softsolutions.bestx.connections.fixgateway.FixExecutionReportOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixMessageFields;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.CSPOBexExecutionReport;
import it.softsolutions.bestx.model.CSPOBexExecutionReport.CSDealerGroup;
import it.softsolutions.bestx.model.Commission.CommissionType;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.MICCodeService;
import it.softsolutions.bestx.services.financecalc.SettlementDateCalculator;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by: davide.rossoni 
 * Creation date: 19/ott/2012 
 * 
 * AKROS <--> OMS1
 * 
 **/
public class OMS1FixExecutionReportOutputLazyBean extends FixExecutionReportOutputLazyBean {
    
    protected final Logger LOGGER = LoggerFactory.getLogger(OMS1FixExecutionReportOutputLazyBean.class);
    private OrderSide[] shortSellArray = {OrderSide.SELL_SHORT, OrderSide.SELL_SHORT_EXEMPT, OrderSide.SELL_UNDISCLOSED};
	private List<OrderSide> shortSellSides =  Arrays.asList(shortSellArray);
   
    //
    // not used?
    //
    public OMS1FixExecutionReportOutputLazyBean(String sessionId, Order order, String orderId) {
        super(sessionId, order, orderId);
    }

    // used by AkrosCustomerAdapter.sendOrderReject (creates ExecutionReport, rejected state)
    public OMS1FixExecutionReportOutputLazyBean(String sessionId, Order order, String orderId, ExecutionReport executionReport, int errorCode, String rejectReason, MICCodeService micCodeService) {
        super(sessionId, order, orderId, executionReport, errorCode, rejectReason, micCodeService);
        // Used for not executed order
        // 31-05-2012 Stefano: Fix for rejectReasonDescription == null
        if (rejectReasonDescription == null || rejectReasonDescription.isEmpty()) 
        {
            rejectReasonDescription = "Cancelled";
        }
        
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        futSettDate = null;
        if(executionReport != null && executionReport.getFutSettDate() != null) {
           strFutSettDate =  sdf.format(executionReport.getFutSettDate());
        } else if (order != null) {
           strFutSettDate = sdf.format(order.getFutSettDate());
        }
        
        /* SP 24-05-2018 - togliere appena fix gateway accettera' la data come un long
        futSettDate =  executionReport.getFutSettDate();
        if(futSettDate == null && order != null) {
        	futSettDate = order.getFutSettDate();
        }
        */
        
        /* SP 04/09/2018 accruedInterest must be null in Cancel message */
        accruedInterest = (executionReport != null && executionReport.getAccruedInterestAmount() != null ? executionReport.getAccruedInterestAmount().getAmount() : null);
        
        tipoConto = (executionReport != null) ? executionReport.getTipoConto() : null;
        if(executionReport == null) {
        	lastMkt = "";
        }
        else {
        	lastMkt = executionReport.getLastMkt() != null ? executionReport.getLastMkt() : "";
        }
        if(executionReport != null) {
        	execBroker = executionReport.getExecBroker();
        	executionReportId = executionReport.getExecutionReportId();
        	if( executionReportId==null && order != null)
        		executionReportId = order.getFixOrderId();
        }
        execType = (executionReport != null && executionReport.getState() != null ? executionReport.getState().getValue() : ExecutionReport.ExecutionReportState.CANCELLED.getValue()); // default
                                                                                                                                                    // "4"
        state = ExecutionReportState.FILLED;
        if(order != null)
        	currency = order.getCurrency();
        commissionType = CommissionType.AMOUNT.getValue();
        if(order != null && order.getInstrument() != null)
    		symbol = order.getInstrument().getIsin();
        
        // if there is an actualQty we should be in the case of a partial execution on ETLX, thus we send the unexecution report for the remaining part
        if (executionReport != null && executionReport.getActualQty() != null && executionReport.getActualQty().compareTo(BigDecimal.ZERO) != 0) {
            actualQty = executionReport.getActualQty();
        } else {
            // 2009-09-14 Ruggero Following the post pending of _CANCEL to the ExecId, we must send to TAS, for a cancel execution report of the whole order, in the LastShares tag (fix number 32) the total quantity.
            actualQty = orderQty;
        }
        if (executionReport != null && executionReport.getExecTransType()!=null) {
     		execTransType = executionReport.getExecTransType();
     	   }

        cumQty = BigDecimal.ZERO;
        settlementType = null;
        if (executionReport != null && executionReport.getSettlType() != null) {
           strSettlementType = executionReport.getSettlType();
        }
        executionReportId += "_CANCEL";
       
        if(order != null)
        	side = order.getSide();
        if(shortSellSides.contains(side))
        	side = OrderSide.SELL;
        
        
        buildMessage();
        
        if (executionReport instanceof CSPOBexExecutionReport) {
        	addCSPOBexExecutionReportFields((CSPOBexExecutionReport) executionReport);
        }
    }

    // Usato per riempire il messaggio per TAS
    public OMS1FixExecutionReportOutputLazyBean(String sessionId, Quote quote, Order order, String orderId, Attempt attempt, ExecutionReport executionReport, MICCodeService micCodeService, SettlementDateCalculator settlementDateCalculator) {
        super(sessionId, quote, order, orderId, attempt, executionReport, micCodeService);
        if(order != null && executionReport != null) {
	
	        // [DR20120614] Workaround: set custom Akros field BTDSCommissionIndicator as NULL
//	        super.btdsCommissionIndicator = null;
	        //super.btdsCommissionIndicator = null;
	
	        // Used for executed order
	        // 31-05-2012 Stefano: Fix for rejectReasonDescription == null
	        if (rejectReasonDescription == null || rejectReasonDescription.isEmpty()) {// overwrite only if not already set
	            rejectReasonDescription = "Executed";
	        }
	        /*else {
	            rejectReasonDescription = rejectReasonDescription +" - Executed" ;
	        }*/
	
	        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	        futSettDate = null;
	        if(executionReport.getFutSettDate() != null) {
	           strFutSettDate =  sdf.format(executionReport.getFutSettDate());
	        } else if (order != null) {
	           //SP-20200730 - BESTX-694 Manage null order settlement date
	           if (settlementDateCalculator != null && order.getFutSettDate() == null) {
	              Integer instrumentStdSettlDays = 2;
	              if (order.getInstrument() != null) {
	                 instrumentStdSettlDays = order.getInstrument().getStdSettlementDays();
	              }
	              Date startDate = order.getTransactTime();
	              if (startDate == null) {
	                  startDate = DateService.newLocalDate();
	              }
	              strFutSettDate = sdf.format(settlementDateCalculator.getCalculatedSettlementDate(instrumentStdSettlDays, null, startDate));
	           } else {
	              strFutSettDate = sdf.format(order.getFutSettDate());
	           }
	        }
           
           /* SP 24-05-2018 - togliere appena fix gateway accettera' la data come un long
	        futSettDate =  executionReport.getFutSettDate();
	        if(futSettDate == null && order != null) {
	        	futSettDate = order.getFutSettDate();
	        }
	        */
	        //BESTX-348: SP-20180905
	        
	    	accruedInterest = (executionReport.getAccruedInterestAmount() != null ? executionReport.getAccruedInterestAmount().getAmount() : BigDecimal.ZERO);
	    	accruedDays = executionReport.getAccruedInterestDays();
	    	tipoConto = executionReport.getTipoConto();
	
	    	if (executionReport.getExecType()!=null) {
	    		execTransType = executionReport.getExecType();
	    	}
	    	execBroker = executionReport.getExecBroker();
	        lastPx = executionReport.getLastPx();
	    	if (executionReportId==null) executionReportId = order.getFixOrderId();
	        currency = order.getCurrency();
	        execType = "2";
	        state = ExecutionReportState.FILLED;
	
	        // AMC 20091105 Chiesto da Sormani di includere la commissione - sempre espressa come amount - anche nel caso
	        // sia gia' inglobata nel prezzo per il cliente solo per gli eseguiti TAS
	        if (executionReport.getCommissionType() == CommissionType.TICKER) {
	
	            // AMC 20100630 Eliminato perche' accordo con Sormani che mandare le commissioni in amount al cliente che richiede il prezzo
	            // gia' comprensivo delle commissioni sia indipendente dalla proprieta' su cui l'ordine e' stato chiuso &&
	            // "BEST".equalsIgnoreCase(executionReport.getProperty())
	            if (order.isAddCommissionToCustomerPrice()) {
	                commission = commission.setScale(5);
	                commission = commission.multiply(executionReport.getActualQty()).divide(TEN_THOUSAND, RoundingMode.HALF_DOWN);
	                commissionType = CommissionType.AMOUNT.getValue();
	            } else {
	                commission = executionReport.getCommission().divide(new BigDecimal(10.00));
	                commissionType = CommissionType.TICKER.getValue();
	            }
	
	        } else {
	            commission = executionReport.getCommission();
	            commissionType = CommissionType.AMOUNT.getValue();
	        }
	
	        
//	        price = lastPx.setScale(6, RoundingMode.HALF_UP);  //BESTX-426 AMC 20190612
	        avgPx = lastPx.setScale(6, RoundingMode.HALF_UP);
	        settlementType = null;
           strSettlementType = executionReport.getSettlType();
	
	        // Magnet partial fills
	        if (order.getQty().compareTo(executionReport.getActualQty()) != 0 && executionReport.getActualQty().compareTo(BigDecimal.ZERO) != 0 ) {
	            execType = "1";
	        }
	
	        buildMessage();
	        
	        if (executionReport instanceof CSPOBexExecutionReport) {
	        	addCSPOBexExecutionReportFields((CSPOBexExecutionReport) executionReport);
	        }
	       
        }
    }
    
    // Usato per riempire il messaggio per TAS
    public OMS1FixExecutionReportOutputLazyBean(String sessionId, Quote quote, Order order, String orderId, Attempt attempt, CSPOBexExecutionReport executionReport, MICCodeService micCodeService, SettlementDateCalculator settlementDateCalculator) {
        super(sessionId, quote, order, orderId, attempt, executionReport, micCodeService);
        if(order != null && executionReport != null) {
	
	        // [DR20120614] Workaround: set custom Akros field BTDSCommissionIndicator as NULL
//	        super.btdsCommissionIndicator = null;
	        //super.btdsCommissionIndicator = null;
	
	  
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
           futSettDate = null;
           if(executionReport.getFutSettDate() != null) {
              strFutSettDate =  sdf.format(executionReport.getFutSettDate());
           } else if (order != null) {
              //SP-20200730 - BESTX-694 Manage null order settlement date
              if (settlementDateCalculator != null && order.getFutSettDate() == null) {
                 Integer instrumentStdSettlDays = 2;
                 if (order.getInstrument() != null) {
                    instrumentStdSettlDays = order.getInstrument().getStdSettlementDays();
                 }
                 Date startDate = order.getTransactTime();
                 if (startDate == null) {
                     startDate = DateService.newLocalDate();
                 }
                 strFutSettDate = sdf.format(settlementDateCalculator.getCalculatedSettlementDate(instrumentStdSettlDays, null, startDate));
              } else {
                 strFutSettDate = sdf.format(order.getFutSettDate());
              }
           }
           
           /* SP 24-05-2018 - togliere appena fix gateway accettera' la data come un long
	        futSettDate =  executionReport.getFutSettDate();
	        if(futSettDate == null && order != null) {
	        	futSettDate = order.getFutSettDate();
	        }
	        */
           //BESTX-348: SP-20180905
           
	    	accruedInterest = (executionReport.getAccruedInterestAmount() != null ? executionReport.getAccruedInterestAmount().getAmount() : BigDecimal.ZERO);
	    	accruedDays = executionReport.getAccruedInterestDays();
	    	tipoConto = executionReport.getTipoConto();
	    	
	    	if (executionReport.getExecType()!=null) {
	    		execTransType = executionReport.getExecType();
	    	}
	    	execBroker = executionReport.getExecBroker();
	        lastPx = executionReport.getLastPx();
	    	if (executionReportId==null) executionReportId = order.getFixOrderId();
	        currency = order.getCurrency();
	        execType = "0";
	        state = ExecutionReportState.FILLED;
	
	        price = lastPx.setScale(6, RoundingMode.HALF_UP);
	        avgPx = lastPx.setScale(6, RoundingMode.HALF_UP);
	        settlementType = null;
	        strSettlementType = executionReport.getSettlType();
	
        	
	        buildMessage();
	        addCSPOBexExecutionReportFields(executionReport);
	        
	    }
    }
    
    protected void addCSPOBexExecutionReportFields(CSPOBexExecutionReport executionReport) {
    	
    	if (executionReport.getActualQty()!=null && executionReport.getActualQty().compareTo(BigDecimal.ZERO)>0) {
    		msg.setValue(FixMessageFields.FIX_ExecTransType, "0");
    	} else {
    		msg.setValue(FixMessageFields.FIX_ExecTransType, "3");
    	}
    	if (executionReport.getExecAttmptNo()!=null) {
    		msg.setValue(CSFixMessageFields.CS_FIX_PT_ExecAttmptNo, executionReport.getExecAttmptNo());
    	}
    	if (executionReport.getExecAttmptTime()!=null) {
    		msg.setValue(CSFixMessageFields.CS_FIX_PT_ExecAttmptTime, DateService.format("yyyyMMdd-HH:mm:ss.SSS", executionReport.getExecAttmptTime()));
    	}
    	if (executionReport.getMultiDealerID()!=null) {
    		msg.setValue(CSFixMessageFields.CS_FIX_PT_MultiDealerID, executionReport.getMultiDealerID());
    	}
    	if (executionReport.getNoDealers()!=null) {
    		msg.setValue(CSFixMessageFields.CS_FIX_PT_NoDealers, executionReport.getNoDealers());
    	}
    	int i = 0;
    	for (CSDealerGroup dealer: executionReport.getDealerGroups()) {
    		if (dealer.getDealerID() != null) {
        		msg.setValue(CSFixMessageFields.CS_FIX_PT_DealerID+"."+i, dealer.getDealerID());
    		
            if (dealer.getDealerQuotePrice()!=null) {
               msg.setValue(CSFixMessageFields.CS_FIX_PT_DealerQuotePrice+"."+i, dealer.getDealerQuotePrice().doubleValue());
            }
            if (dealer.getDealerQuoteOrdQty()!=null) {
               msg.setValue(CSFixMessageFields.CS_FIX_PT_DealerQuoteOrdQty+"."+i, dealer.getDealerQuoteOrdQty().doubleValue());
            }
            if (dealer.getDealerQuoteTime()!=null) {
               msg.setValue(CSFixMessageFields.CS_FIX_PT_DealerQuoteTime+"."+i, DateService.formatAsUTC("yyyyMMdd-HH:mm:ss.SSS", dealer.getDealerQuoteTime()));
            }
            if (dealer.getDealerQuoteStatusString()!=null) {
               msg.setValue(CSFixMessageFields.CS_FIX_PT_DealerQuoteStatus+"."+i, dealer.getDealerQuoteStatusString());
            } else if (dealer.getDealerQuoteStatus()!=null) {
               msg.setValue(CSFixMessageFields.CS_FIX_PT_DealerQuoteStatus+"."+i, dealer.getDealerQuoteStatus().name());
            }
            i++;
    		}
        }
    }

    @Override
    protected void buildMessage() {
        super.buildMessage();

        // [RR20120614] Prevent FIX validation error: Reject sent for Message 3: Tag specified without a value:76
        if (execBroker != null && execBroker.isEmpty()) {
            msg.deleteValue(FixMessageFields.FIX_ExecBroker);
        }
        if (transactTime != null) {
            msg.setValue(FixMessageFields.FIX_TransactTime, DateService.formatAsUTC("yyyyMMdd-HH:mm:ss", transactTime));
        }
    }
    
}
