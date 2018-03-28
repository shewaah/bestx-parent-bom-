/*
 * Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx.dao;


import it.softsolutions.bestx.model.UserModel;


/**
 * Purpose : user data access interface.
*
* Project Name : bestxengine 
* First created by: alberto.plebani 
* Creation date: 30/aug/2017 
* 
**/

public interface UserModelDao {

	
   UserModel getUserByUserName(String userName);
   
   //String getGroupForUser(String userName);


}
