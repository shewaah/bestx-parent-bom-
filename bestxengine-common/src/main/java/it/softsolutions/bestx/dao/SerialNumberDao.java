/*
* Project Name : bestengine-common 
* First created by: ruggero.rizzo 
* Creation date: 10/mag/2012 
* 
* Copright 1997-2012 SoftSolutions! srl 
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
* 
*/
package it.softsolutions.bestx.dao;

import java.util.Date;

/**
 * Purpose : number sequence generator DAO.
 * @author ruggero.rizzo
 *
 */
public interface SerialNumberDao {
   /**
    * Get the next number in the sequence.
    * @param identifier : the sequence identifier
    * @return the new number
    */
    long getNextNumber(String identifier);
    /**
     * Get the next number in the sequence but with a valid date.
     * @param identifier : the sequence identifier
     * @param date : the validity date
     * @return the number
     */
    long getNextNumber(String identifier, Date date);
    /**
     * Reset the number sequence.
     * @param identifier : the sequence identifier
     */
    void resetNumber(String identifier);
}
