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
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;

import it.softsolutions.jsscommon.Money;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-common 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class MoneyUserType implements CompositeUserType {
    
    private static final String[] propertyNames = { "currency", "amount" };
    private static final Type[] propertyTypes = { StandardBasicTypes.CURRENCY, StandardBasicTypes.BIG_DECIMAL };

    @Override
	public Object deepCopy(Object value) throws HibernateException {
        return value; // immutable
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public String[] getPropertyNames() {
        return propertyNames;
    }

    @Override
    public Type[] getPropertyTypes() {
        return propertyTypes;
    }

    @Override
    public Object getPropertyValue(Object component, int property) throws HibernateException {
        if (!(component instanceof Money)) {
            throw new HibernateException("Wrong component class");
        }
        
        Money money = (Money) component;
        switch (property) {
        case 0:
            return money.getStringCurrency();
        case 1:
            return money.getAmount();
        default:
            throw new HibernateException("Incorrect property index: " + property);
        }
    }


    @Override
    public void setPropertyValue(Object component, int property, Object value) throws HibernateException {
        throw new UnsupportedOperationException("Immutable!");
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == y) {
            return true;
        }
        if (x == null || y == null) {
            return false;
        }
        Money mx = (Money) x;
        Money my = (Money) y;
        return (mx.equals(my));
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return ((Money) x).hashCode();
    }

    @Override
    public Class<Money> returnedClass() {
        return Money.class;
    }

   @Override
   public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
      if (!rs.wasNull()) {
         String currency = (String) StandardBasicTypes.STRING.nullSafeGet(rs, names[0], session);
         BigDecimal amount = (BigDecimal) StandardBasicTypes.BIG_DECIMAL.nullSafeGet(rs, names[1], session);
         if (amount == null) {
             return null;
         }
         return new Money(currency, amount);
     } else {
         return null;
     }
   }

   @Override
   public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
      Money money = (Money) value;
      String currency = money == null ? null : money.getStringCurrency();
      BigDecimal amount = money == null ? null : money.getAmount();
      StandardBasicTypes.STRING.nullSafeSet(st, currency, index, session);
      StandardBasicTypes.BIG_DECIMAL.nullSafeSet(st, amount, index + 1, session);
   }

   @Override
   public Serializable disassemble(Object value, SharedSessionContractImplementor session) throws HibernateException {
      return (Serializable) deepCopy(value);
   }
   
   @Override
   public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
      return deepCopy(cached);
   }

   @Override
   public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner) throws HibernateException {
      // TODO Auto-generated method stub
      return null;
   }
}