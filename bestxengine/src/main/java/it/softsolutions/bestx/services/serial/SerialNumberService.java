package it.softsolutions.bestx.services.serial;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.services.Service;

import java.util.Date;

public interface SerialNumberService extends Service {
    
    /**
     * Returns a unique identifier appending the serialNumber and formatted using the pattern specified 
     * 
     * @param identifier the identifier used in order to retrieve the serialNumber 
     * @param pattern the pattern to be used
     * @return a unique identifier 
     * @throws BestXException when an error occurs
     */
    String getUniqueIdentifier(String identifier, String pattern) throws BestXException;

    long getSerialNumber(String identifier);

    long getSerialNumber(String identifier, Date date) throws BestXException;

    void init() throws BestXException;
}
