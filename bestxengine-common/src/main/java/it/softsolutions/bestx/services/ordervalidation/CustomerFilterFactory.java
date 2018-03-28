package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.CustomerFilterRow;

import java.util.List;

public interface CustomerFilterFactory {

    List<OrderValidator> createFilterList(CustomerFilterRow filterRow, Customer customer);
}