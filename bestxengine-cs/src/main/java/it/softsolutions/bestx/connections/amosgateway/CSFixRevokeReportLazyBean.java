/*
 * Project Name : BestXEngine_Akros
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author: ruggero.rizzo $
 * Date         : $Date: 2010-12-24 14:34:53 $
 * Header       : $Id: AkrosFixRevokeReportLazyBean.java,v 1.2 2010-12-24 14:34:53 ruggero.rizzo Exp $
 * Revision     : $Revision: 1.2 $
 * Source       : $Source: /root/scripts/BestXEngine_Akros/src/it/softsolutions/bestx/connections/amosgateway/AkrosFixRevokeReportLazyBean.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_Akros
 */
package it.softsolutions.bestx.connections.amosgateway;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.connections.fixgateway.FixMessageFields;
import it.softsolutions.bestx.connections.fixgateway.FixRevokeReportLazyBean;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class CSFixRevokeReportLazyBean extends FixRevokeReportLazyBean {
	protected final SimpleDateFormat akrosDateTimeFormatter = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");

	@Override
	protected void buildMessage() {
		super.buildMessage();
	}

	public CSFixRevokeReportLazyBean(String fixSessionId, Operation operation, boolean accept, String comment) {
		super(fixSessionId, operation, accept, comment);
		akrosDateTimeFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
		if (transactTime != null) {
			msg.setValue(FixMessageFields.FIX_TransactTime, akrosDateTimeFormatter.format(transactTime));
		}
		if (orderId != null) {
			msg.setValue(FixMessageFields.FIX_OrigClOrdID, orderId);
		}
	}
}
