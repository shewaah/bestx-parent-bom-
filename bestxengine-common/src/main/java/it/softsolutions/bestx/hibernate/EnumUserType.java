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
package it.softsolutions.bestx.hibernate;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-common 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
@SuppressWarnings("rawtypes")
public class EnumUserType implements UserType, ParameterizedType {
    

	private Class clazz = null;

    @Override
	public void setParameterValues(Properties params) {
        String enumClassName = params.getProperty("enumClassName");
        if (enumClassName == null) {
            throw new MappingException("enumClassName parameter not specified");
        }
        try {
            this.clazz = Class.forName(enumClassName);
        }
        catch (java.lang.ClassNotFoundException e) {
            throw new MappingException("enumClass " + enumClassName + " not found"+" : "+ e.toString(), e);
        }
    }
    
    private static final int[] SQL_TYPES = { Types.VARCHAR };

    @Override
	public int[] sqlTypes() {
        return SQL_TYPES;
    }

    @Override
	public Class returnedClass() {
        return clazz;
    }
    

    @Override
	public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
	public boolean isMutable() {
        return false;
    }

    @Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
	public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    @Override
	public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
	public boolean equals(Object x, Object y) throws HibernateException {
        if (x == y) {
            return true;
        }
        if (null == x || null == y) {
            return false;
        }
        return x.equals(y);
    }

   @Override
   public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
      String name = rs.getString(names[0]);
      Object result = null;
      if (!rs.wasNull()) {
          result = Enum.valueOf(clazz, name);
      }
      return result;
   }

   @Override
   public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
      if (null == value) {
         st.setNull(index, Types.VARCHAR);
     }
     else {
         st.setString(index, ((Enum) value).name());
     }
   }
}