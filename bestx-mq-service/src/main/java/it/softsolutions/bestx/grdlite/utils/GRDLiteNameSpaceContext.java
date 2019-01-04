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
package it.softsolutions.bestx.grdlite.utils;

import java.util.HashMap;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

/**  
 *
 * Purpose: custom namespace context, needed by xpath to resolve queries on xml documents
 * with namespaces. With a prefix associated to the namespace we can do xpath queries without
 * problems.  
 *
 * Project Name : bestx-mq-service 
 * First created by: ruggero.rizzo 
 * Creation date: 31/gen/2013 
 * 
 **/
/**
 * @author ruggero.rizzo
 *
 */
public class GRDLiteNameSpaceContext implements NamespaceContext {
    
    private HashMap<String, String> namespaces;
    
    public GRDLiteNameSpaceContext(HashMap<String, String> namespaces) {
        if (namespaces == null) {
            throw new IllegalArgumentException("null namespaces");
        }
        this.namespaces = namespaces;
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return namespaces.get(prefix);
    }
   
    // Dummy implementation - not used!
    @Override
    public Iterator<?> getPrefixes(String val) {
        return null;
    }
   
    // Dummy implemenation - not used!
    @Override
    public String getPrefix(String uri) {
        return null;
    }

}
