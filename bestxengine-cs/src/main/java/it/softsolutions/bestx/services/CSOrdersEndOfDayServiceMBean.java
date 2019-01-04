package it.softsolutions.bestx.services;

public interface CSOrdersEndOfDayServiceMBean {
	
	void invokeTimerExpired_Orders();
	
	void invokeTimerExpired_LimitFile_USandGlobal();

	void invokeTimerExpired_LimitFile_Non_USandGlobal();
}
