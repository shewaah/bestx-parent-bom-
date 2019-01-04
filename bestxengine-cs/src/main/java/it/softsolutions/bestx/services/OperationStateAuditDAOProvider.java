package it.softsolutions.bestx.services;

import it.softsolutions.bestx.dao.OperationStateAuditDao;

public class OperationStateAuditDAOProvider {
	private static OperationStateAuditDao operationStateAuditDao;

	
	public static OperationStateAuditDao getOperationStateAuditDao() {
		return OperationStateAuditDAOProvider.operationStateAuditDao;
	}

	public static void setOperationStateAuditDao(OperationStateAuditDao operationStateAuditDao) {
		OperationStateAuditDAOProvider.operationStateAuditDao = operationStateAuditDao;
	}
}
