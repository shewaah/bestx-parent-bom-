/*
* Copyright 1997-2021 SoftSolutions! srl 
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

package it.softsolutions.bestx.services.rest.dto;

import java.util.List;

public class HealthCheckResponseData {

	private String status;
	private List<ExceptionMessage> exceptions;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public List<ExceptionMessage> getExceptions() {
		return exceptions;
	}

	public void setExceptions(List<ExceptionMessage> exceptions) {
		this.exceptions = exceptions;
	}

	@Override
	public String toString() {
		return "HealthCheckResponseData [status=" + status + ", exceptions=" + exceptions + "]";
	}

}
