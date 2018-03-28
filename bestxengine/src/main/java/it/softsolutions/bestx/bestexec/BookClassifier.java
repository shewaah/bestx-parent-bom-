package it.softsolutions.bestx.bestexec;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Book;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;

/**
 * This interface represents objects for classification of book proposals.
 * Real proposal classification can be implemented by ProposalClassificator objects.
 * Operations must be thread safe: i.e. methods will be used from more threads concurrently
 * @author lsgro
 *
 */
public interface BookClassifier {
    /**
     * This method performs book proposal classification, based on all available information.
     * @param book The book to be classified
     * @param orderSide The side of the Order for which the Book has been collected
     * @param qty The requested quantity
     * @param futSettDate The requested future settlement date
     * @param previousAttempts The list of previous attempts to close the deal
     * @param venues The set of all the Venues available to the party which sent the order
     * @return A {@link ClassifiedBook} object, containing only classified proposals.
     */
    ClassifiedBook getClassifiedBook(Book book, OrderSide orderSide,
            BigDecimal qty, Date futSettDate, List<Attempt> previousAttempts, Set<Venue> venues);

    /**
     * This method performs book proposal classification, based on all available information.
     * @param book The book to be classified
     * @param order the Order for which the Book has been collected
     * @param previousAttempts The list of previous attempts to close the deal
     * @param venues The set of all the Venues available to the party which sent the order
     * @return A {@link ClassifiedBook} object, containing only classified proposals.
     */
    ClassifiedBook getClassifiedBook(Book book, Order order,
            List<Attempt> previousAttempts, Set<Venue> venues);
}
