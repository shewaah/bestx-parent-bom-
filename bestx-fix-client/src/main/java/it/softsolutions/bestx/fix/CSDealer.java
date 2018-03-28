package it.softsolutions.bestx.fix;

import quickfix.Group;

public class CSDealer {


	public String PT_DealerID;
	public String PT_DealerQuotePrice;
	public String PT_DealerQuoteOrdQty;
	public String PT_DealerQuoteTime;
	public String PT_DealerQuoteStatus;

	public CSDealer(Group group) {
		try {
			if (group.isSetField(9691)) PT_DealerID = group.getString(9691);
			if (group.isSetField(9694)) PT_DealerQuotePrice = group.getString(9694);
			if (group.isSetField(9695)) PT_DealerQuoteOrdQty = group.getString(9695);
			if (group.isSetField(9699)) PT_DealerQuoteTime = group.getString(9699);
			if (group.isSetField(9700)) PT_DealerQuoteStatus = group.getString(9700);
		} catch (Exception e) { }

	}
	
	
	
}
