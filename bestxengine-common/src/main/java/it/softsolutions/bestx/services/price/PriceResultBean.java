package it.softsolutions.bestx.services.price;

import java.util.ArrayList;
import java.util.List;

import it.softsolutions.bestx.model.SortedBook;

public class PriceResultBean implements PriceResult {
    private SortedBook book;
    private PriceResultState state;
    private String reason;
    private List<String> error_report = new ArrayList<String>();

    public void setSortedBook(SortedBook book) {
        this.book = book;
    }

    public SortedBook getSortedBook() {
        return book;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setState(PriceResultState state) {
        this.state = state;
    }

    public PriceResultState getState() {
        return state;
    }
    
    public List<String> getErrorReport() {
		return error_report;
	}
    
    public void addError(String error) {
    	this.error_report.add(error);
    }

	public void addErrors(List<String> errors) {
		this.error_report.addAll(errors);
	}
    
}
