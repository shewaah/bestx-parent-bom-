package it.softsolutions.bestx.bestexec;

import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.SortedBook;

/**
 * Operations must be thread safe: i.e. methods will be used from more threads concurrently
 */
public interface BookSorter {
    SortedBook getSortedBook(ClassifiedBook classifiedBook);
}
