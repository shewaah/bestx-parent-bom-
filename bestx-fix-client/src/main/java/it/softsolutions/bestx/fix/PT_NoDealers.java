package it.softsolutions.bestx.fix;

import quickfix.FieldNotFound;
import quickfix.Group;

public  class PT_NoDealers extends Group {
	static final long serialVersionUID = 20050617;
	public PT_NoDealers() {
		super(9690, 9691,
			new int[] {9691, 9694, 9695, 9699, 9700,  0 } );
	}
	
  
	  public void set(it.softsolutions.bestx.fix.field.PT_DealerID value)
	  {
		setField(value);
	  }

	  public it.softsolutions.bestx.fix.field.PT_DealerID get(it.softsolutions.bestx.fix.field.PT_DealerID  value) throws FieldNotFound
	  {
		getField(value);
		return value;
	  }

	  public it.softsolutions.bestx.fix.field.PT_DealerID getPT_DealerID() throws FieldNotFound
	  {
		it.softsolutions.bestx.fix.field.PT_DealerID value = new it.softsolutions.bestx.fix.field.PT_DealerID();
		getField(value);
		return value;
	  }

	  public boolean isSet(it.softsolutions.bestx.fix.field.PT_DealerID field)
	  {
		return isSetField(field);
	  }

	  public boolean isSetPT_DealerID()
	  {
		return isSetField(9691);
	  }

  
	  public void set(it.softsolutions.bestx.fix.field.PT_DealerQuotePrice value)
	  {
		setField(value);
	  }

	  public it.softsolutions.bestx.fix.field.PT_DealerQuotePrice get(it.softsolutions.bestx.fix.field.PT_DealerQuotePrice  value) throws FieldNotFound
	  {
		getField(value);
		return value;
	  }

	  public it.softsolutions.bestx.fix.field.PT_DealerQuotePrice getPT_DealerQuotePrice() throws FieldNotFound
	  {
		it.softsolutions.bestx.fix.field.PT_DealerQuotePrice value = new it.softsolutions.bestx.fix.field.PT_DealerQuotePrice();
		getField(value);
		return value;
	  }

	  public boolean isSet(it.softsolutions.bestx.fix.field.PT_DealerQuotePrice field)
	  {
		return isSetField(field);
	  }

	  public boolean isSetPT_DealerQuotePrice()
	  {
		return isSetField(9694);
	  }

  
	  public void set(it.softsolutions.bestx.fix.field.PT_DealerQuoteOrdQty value)
	  {
		setField(value);
	  }

	  public it.softsolutions.bestx.fix.field.PT_DealerQuoteOrdQty get(it.softsolutions.bestx.fix.field.PT_DealerQuoteOrdQty  value) throws FieldNotFound
	  {
		getField(value);
		return value;
	  }

	  public it.softsolutions.bestx.fix.field.PT_DealerQuoteOrdQty getPT_DealerQuoteOrdQty() throws FieldNotFound
	  {
		it.softsolutions.bestx.fix.field.PT_DealerQuoteOrdQty value = new it.softsolutions.bestx.fix.field.PT_DealerQuoteOrdQty();
		getField(value);
		return value;
	  }

	  public boolean isSet(it.softsolutions.bestx.fix.field.PT_DealerQuoteOrdQty field)
	  {
		return isSetField(field);
	  }

	  public boolean isSetPT_DealerQuoteOrdQty()
	  {
		return isSetField(9695);
	  }

  
	  public void set(it.softsolutions.bestx.fix.field.PT_DealerQuoteTime value)
	  {
		setField(value);
	  }

	  public it.softsolutions.bestx.fix.field.PT_DealerQuoteTime get(it.softsolutions.bestx.fix.field.PT_DealerQuoteTime  value) throws FieldNotFound
	  {
		getField(value);
		return value;
	  }

	  public it.softsolutions.bestx.fix.field.PT_DealerQuoteTime getPT_DealerQuoteTime() throws FieldNotFound
	  {
		it.softsolutions.bestx.fix.field.PT_DealerQuoteTime value = new it.softsolutions.bestx.fix.field.PT_DealerQuoteTime();
		getField(value);
		return value;
	  }

	  public boolean isSet(it.softsolutions.bestx.fix.field.PT_DealerQuoteTime field)
	  {
		return isSetField(field);
	  }

	  public boolean isSetPT_DealerQuoteTime()
	  {
		return isSetField(9699);
	  }

  
	  public void set(it.softsolutions.bestx.fix.field.PT_DealerQuoteStatus value)
	  {
		setField(value);
	  }

	  public it.softsolutions.bestx.fix.field.PT_DealerQuoteStatus get(it.softsolutions.bestx.fix.field.PT_DealerQuoteStatus  value) throws FieldNotFound
	  {
		getField(value);
		return value;
	  }

	  public it.softsolutions.bestx.fix.field.PT_DealerQuoteStatus getPT_DealerQuoteStatus() throws FieldNotFound
	  {
		it.softsolutions.bestx.fix.field.PT_DealerQuoteStatus value = new it.softsolutions.bestx.fix.field.PT_DealerQuoteStatus();
		getField(value);
		return value;
	  }

	  public boolean isSet(it.softsolutions.bestx.fix.field.PT_DealerQuoteStatus field)
	  {
		return isSetField(field);
	  }

	  public boolean isSetPT_DealerQuoteStatus()
	  {
		return isSetField(9700);
	  }

  }