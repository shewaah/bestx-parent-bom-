
/*
 * Copyright 1997-2017 SoftSolutions! srl 
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
 
package it.softsolutions.bestx;

import java.util.Date;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine
 * First created by: anna.cochetti
 * Creation date: 16 ago 2017
 * 
 **/

public interface UserIFC {

	String getPassword();

	void setUserName(String userName);

	String getUserName();

	void setPasswordExpirationDate(Date passwordExpirationDate);

	Date getPasswordExpirationDate();

	void setSurname(String surname);

	String getSurname();

	void setName(String name);

	String getName();

	void setEmail(String email);

	String getEmail();

	void setIsLocked(int isLocked);

	Integer getIsLocked();

	void setSpecialist(String specialist);

	String getSpecialist();

	void setDescription(String description);

	String getDescription();

}
