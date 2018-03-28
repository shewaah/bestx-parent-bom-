
package it.softsolutions.bestx.services.price;


public interface PriceServiceListener {
    void onQuoteResult(PriceService source, QuoteResult quoteResult);
    void onPricesResult(PriceService source, PriceResult priceResult);

}
