/**
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
package it.softsolutions.bestx;

import static com.codahale.metrics.MetricRegistry.name;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.EhcacheDecoratorAdapter;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-common
 * First created by: davide.rossoni
 * Creation date: 17/dic/2013
 * 
 * An instrumented {@link Ehcache} instance.
 */
public class InstrumentedEhcache extends EhcacheDecoratorAdapter {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(InstrumentedEhcache.class);
	
    /**
     * Instruments the given {@link Ehcache} instance with get and put timers
     * and a set of gauges for Ehcache's built-in statistics:
     * <p/>
     * <table>
     * <tr>
     * <td>{@code hits}</td>
     * <td>The number of times a requested item was found in the
     * cache.</td>
     * </tr>
     * <tr>
     * <td>{@code misses}</td>
     * <td>Number of times a requested item was not found in the
     * cache.</td>
     * </tr>
     * <tr>
     * <td>{@code objects}</td>
     * <td>Number of elements stored in the cache.</td>
     * </tr>
     * <tr>
     * <td>{@code mean-get-time}</td>
     * <td>The average get time. Because ehcache support JDK1.4.2, each
     * get time uses {@link System#currentTimeMillis()}, rather than
     * nanoseconds. The accuracy is thus limited.</td>
     * </tr>
     * <tr>
     * <td>{@code mean-search-time}</td>
     * <td>The average execution time (in milliseconds) within the last
     * sample period.</td>
     * </tr>
     * <tr>
     * <td>{@code eviction-count}</td>
     * <td>The number of cache evictions, since the cache was created,
     * or statistics were cleared.</td>
     * </tr>
     * <tr>
     * <td>{@code searches-per-second}</td>
     * <td>The number of search executions that have completed in the
     * last second.</td>
     * </tr>
     * <tr>
     * <td>{@code accuracy}</td>
     * <td>A human readable description of the accuracy setting. One of
     * "None", "Best Effort" or "Guaranteed".</td>
     * </tr>
     * </table>
     *
     * <b>N.B.: This enables Ehcache's sampling statistics with an accuracy
     * level of "none."</b>
     *
     * @param cache       an {@link Ehcache} instance
     * @param registry    a {@link MetricRegistry}
     * @return an instrumented decorator for {@code cache}
     * @see Statistics
     */
    public static Ehcache instrument(MetricRegistry registry, final Ehcache cache) {
    	try {
            final String prefix = name(cache.getClass(), cache.getName());
            registry.register(name(prefix, "hits"),
                              new Gauge<Long>() {
                                  @Override
                                  public Long getValue() {
                                      return cache.getStatistics().cacheHitCount();
                                  }
                              });

            registry.register(name(prefix, "misses"),
                              new Gauge<Long>() {
                                  @Override
                                  public Long getValue() {
                                      return cache.getStatistics().cacheMissCount();
                                  }
                              });

            registry.register(name(prefix, "objects"),
                              new Gauge<Long>() {
                                  @Override
                                  public Long getValue() {
                                      return cache.getStatistics().cachePutCount();
                                  }
                              });

            registry.register(name(prefix, "mean-get-time"),
                              new Gauge<Float>() {
                                  @Override
                                  public Float getValue() {
                                      return Float.valueOf("" + cache.getStatistics().cacheHitRatio());
                                  }
                              });

//            registry.register(name(prefix, "mean-search-time"),
//                              new Gauge<Long>() {
//                                  @Override
//                                  public Long getValue() {
//                                      return cache.getStatistics().getAverageSearchTime();
//                                  }
//                              });

            registry.register(name(prefix, "eviction-count"),
                              new Gauge<Long>() {
                                  @Override
                                  public Long getValue() {
                                      return cache.getStatistics().cacheEvictedCount();
                                  }
                              });

//            registry.register(name(prefix, "searches-per-second"),
//                              new Gauge<Long>() {
//                                  @Override
//                                  public Long getValue() {
//                                      return cache.getStatistics().getSearchesPerSecond();
//                                  }
//                              });
    //
//            registry.register(name(prefix, "writer-queue-size"),
//                              new Gauge<Long>() {
//                                  @Override
//                                  public Long getValue() {
//                                      return cache.getStatistics().getWriterQueueSize();
//                                  }
//                              });
    //
//            registry.register(name(prefix, "accuracy"),
//                              new Gauge<String>() {
//                                  @Override
//                                  public String getValue() {
//                                      return cache.getStatistics()
//                                                  .getStatisticsAccuracyDescription();
//                                  }
//                              });

            return new InstrumentedEhcache(registry, cache);
        } catch (Exception e) {
        	LOGGER.error("Error instrumenting cache, use the plain ehCache: {}", e.getMessage(), e);
        }
    	
    	// on error, return the plain not instrumented ehCache 
    	return cache;
    }

    private final Timer getTimer, putTimer;

    private InstrumentedEhcache(MetricRegistry registry, Ehcache cache) {
        super(cache);
        this.getTimer = registry.timer(name(cache.getClass(), cache.getName(), "gets"));
        this.putTimer = registry.timer(name(cache.getClass(), cache.getName(), "puts"));
    }

    @Override
    public Element get(Object key) throws IllegalStateException, CacheException {
        final Timer.Context ctx = getTimer.time();
        try {
            return underlyingCache.get(key);
        } finally {
            ctx.stop();
        }
    }

    @Override
    public Element get(Serializable key) throws IllegalStateException, CacheException {
        final Timer.Context ctx = getTimer.time();
        try {
            return underlyingCache.get(key);
        } finally {
            ctx.stop();
        }
    }

    @Override
    public void put(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        final Timer.Context ctx = putTimer.time();
        try {
            underlyingCache.put(element);
        } finally {
            ctx.stop();
        }
    }

    @Override
    public void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException, IllegalStateException, CacheException {
        final Timer.Context ctx = putTimer.time();
        try {
            underlyingCache.put(element, doNotNotifyCacheReplicators);
        } finally {
            ctx.stop();
        }
    }

    @Override
    public Element putIfAbsent(Element element) throws NullPointerException {
        final Timer.Context ctx = putTimer.time();
        try {
            return underlyingCache.putIfAbsent(element);
        } finally {
            ctx.stop();
        }
    }
}
