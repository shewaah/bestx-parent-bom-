package it.softsolutions.bestx;

import java.math.BigDecimal;

public interface MifidConfigMBean {

	/**
	 * @return the numRetry
	 */
	int getNumRetry();
	
	void setNumRetry(int numRetry) ;
	/**
	 * @return the qtyLimit
	 */
	BigDecimal getQtyLimit();

	void setQtyLimit(BigDecimal qtyLimit);
	/**
	 * @return the qtyInternal
	 */
	BigDecimal getQtyInternal();

	void setQtyInternal(BigDecimal qtyInternal);
}