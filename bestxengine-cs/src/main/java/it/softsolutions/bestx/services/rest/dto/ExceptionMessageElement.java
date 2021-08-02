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

public class ExceptionMessageElement {
	private String exceptionCode;
	private String exceptionMessage;
	private String exceptionSeverity;

	public String getExceptionCode() {
		return exceptionCode;
	}

	public void setExceptionCode(String exceptionCode) {
		this.exceptionCode = exceptionCode;
	}

	public String getExceptionMessage() {
		return exceptionMessage;
	}

	public void setExceptionMessage(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}

	public String getExceptionSeverity() {
		return exceptionSeverity;
	}

	public void setExceptionSeverity(String exceptionSeverity) {
		this.exceptionSeverity = exceptionSeverity;
	}

	@Override
	public String toString() {
		return "ExceptionMessage [exceptionCode=" + exceptionCode + ", exceptionMessage=" + exceptionMessage
				+ ", exceptionSeverity=" + exceptionSeverity + "]";
	}

}
