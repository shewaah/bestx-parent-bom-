package it.softsolutions.bestx.connections.bondvision;

import java.math.BigDecimal;

import it.softsolutions.bestx.connections.mts.bondvision.BondVisionRFQOutputLazyBean;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.xt2.protocol.XT2Msg;

public class __TestableBondVisionRFQOutputLazyBean extends
		BondVisionRFQOutputLazyBean {

	public __TestableBondVisionRFQOutputLazyBean(MarketOrder marketOrder,
			BigDecimal qtyMultiplier) {
		super(marketOrder, qtyMultiplier);
	}
	
	public XT2Msg _getMsg() {
		return super.getMsg();
	}

}
