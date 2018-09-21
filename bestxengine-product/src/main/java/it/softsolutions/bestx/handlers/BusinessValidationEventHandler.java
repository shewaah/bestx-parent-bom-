/**
 * 
 */
package it.softsolutions.bestx.handlers;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.services.OrderValidationService;
import it.softsolutions.bestx.services.logutils.ApplicationStatisticsHelper;
import it.softsolutions.bestx.services.ordervalidation.FilterOnPortfolioBasis;
import it.softsolutions.bestx.services.ordervalidation.LimitFileParkTagFilter;
import it.softsolutions.bestx.services.ordervalidation.OrderResult;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.OrderRejectableState;
import it.softsolutions.bestx.states.ParkedOrderState;
import it.softsolutions.bestx.states.WaitingPriceState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.autocurando.AutoCurandoStatus;

/**
 * @author Stefano
 *
 */
public class BusinessValidationEventHandler extends BaseOperationEventHandler {
   
	private final OrderValidationService orderValidationService;
	private AutoCurandoStatus autoCurandoStatus;
	
	
	/**
	 * @param operation
	 */	
	public BusinessValidationEventHandler(Operation operation, OrderValidationService orderValidationService, AutoCurandoStatus autoCurandoStatus) {
		super(operation);
		this.orderValidationService = orderValidationService;
		this.autoCurandoStatus = autoCurandoStatus;
	}
	
	@Override
	public void onNewState(OperationState currentState) {
		OrderResult orderResult = orderValidationService.validateOrderByCustomer(operation, operation.getOrder(), operation.getOrder().getCustomer());
		if (orderResult.isValid()) {
	      //SP20180920 - check autocurando flag and stop price discovery if needed
	      if (operation.getOrder().isLimitFile() && AutoCurandoStatus.SUSPENDED.equalsIgnoreCase(autoCurandoStatus.getAutoCurandoStatus())) {
	         operation.setStateResilient(new ParkedOrderState(Messages.getString("LimitFileParkedOrder.0")), ErrorState.class);  
	      } else {
   		   ApplicationStatisticsHelper.logStringAndUpdateOrderIds(operation.getOrder(), "Order.Validation.Valid." + operation.getOrder().getInstrumentCode(), this.getClass().getName());
   		   if (orderResult.getReason() != null &&
   		         orderResult.getReason().indexOf(FilterOnPortfolioBasis.INTERNAL_PRODUCT_STR) > -1)
   		   {
   		      //	        	orderResult.getReason().replaceAll(FilterOnPortfolioBasis.INTERNAL_PRODUCT_STR, "");
   		      operation.setStateResilient(new WaitingPriceState(Messages.getString("FilterOverridedDueToInternalProduct.0")), ErrorState.class);	
   		   }
   		   else if (orderResult.getReason() != null &&
                  orderResult.getReason().indexOf(LimitFileParkTagFilter.TO_PARK_VAL) > -1)
   		   {
               operation.setStateResilient(new ParkedOrderState(Messages.getString("LimitFileParkedOrder.0")), ErrorState.class);  
   		   }
   		   else
   		   {
   		      operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
   		   }
	      }
		}  
		else // orderResult is not valid
		{
		   ApplicationStatisticsHelper.logStringAndUpdateOrderIds(operation.getOrder(), "Order.Validation.NotValid." + operation.getOrder().getInstrumentCode(), this.getClass().getName());
		   if (orderResult.getReason() != null)
		   {
		      if((orderResult.getReason().indexOf(Messages.getString("InternalizationSystemOrderFilter.0")) > -1))
		      {
		         operation.setStateResilient(new WarningState(currentState, null, Messages.getString("InternalizationSystemOrderFilter.0")), ErrorState.class);
		      }
		      else if((orderResult.getReason().indexOf(Messages.getString("InternalizationSystemOrderFilter.1")) > -1))
		      {
		         operation.setStateResilient(new WarningState(currentState, null, Messages.getString("InternalizationSystemOrderFilter.1")), ErrorState.class);
		      }
		      else if((orderResult.getReason().indexOf(Messages.getString("InternalizationSystemOrderFilter.2")) > -1))
		      {
		         operation.setStateResilient(new WarningState(currentState, null, Messages.getString("InternalizationSystemOrderFilter.2")), ErrorState.class);
		      }
		      else if((orderResult.getReason().indexOf(Messages.getString("InternalizationSystemOrderFilter.3")) > -1))
		      {
		         operation.setStateResilient(new WarningState(currentState, null, Messages.getString("InternalizationSystemOrderFilter.3")), ErrorState.class);
		      }
		      else // reason does not contain one of the previous strings
		      {
		         operation.setStateResilient(new OrderRejectableState(orderResult.getReason()), ErrorState.class);
		      }
		   }
		}
	}
}