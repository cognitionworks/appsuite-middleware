/*
 *
 *    OPEN-XCHANGE legal information
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    In some countries OX, OX Open-Xchange, open xchange and OXtender
 *    as well as the corresponding Logos OX Open-Xchange and OX are registered
 *    trademarks of the Open-Xchange, Inc. group of companies.
 *    The use of the Logos is not covered by the GNU General Public License.
 *    Instead, you are allowed to use these Logos according to the terms and
 *    conditions of the Creative Commons License, Version 2.5, Attribution,
 *    Non-commercial, ShareAlike, and the interpretation of the term
 *    Non-commercial applicable to the aforementioned license is published
 *    on the web site http://www.open-xchange.com/EN/legal/index.html.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original and derivative code shall remain
 *    under the copyright of the copyright holder(s) and/or original author(s)per
 *    the Attribution and Assignment Agreement that can be located at
 *    http://www.open-xchange.com/EN/developer/. The contributing author shall be
 *    given Attribution for the derivative code and a license granting use.
 *
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
 *     Mail: info@open-xchange.com
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License, Version 2 as published
 *     by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *     or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *     for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc., 59
 *     Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package com.openexchange.tools.servlet.ratelimit;

import static com.openexchange.java.Strings.asciiLowerCase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.javacodegeeks.concurrent.ConcurrentLinkedHashMap;
import com.javacodegeeks.concurrent.LRUPolicy;
import com.openexchange.config.ConfigurationService;
import com.openexchange.dispatcher.DispatcherPrefixService;
import com.openexchange.java.Strings;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;
import com.openexchange.tools.images.ImageTransformationUtility;
import com.openexchange.tools.servlet.CountingHttpServletRequest;
import com.openexchange.tools.servlet.http.Cookies;
import com.openexchange.tools.servlet.ratelimit.Rate.Result;
import com.openexchange.tools.servlet.ratelimit.impl.RateImpl;

/**
 * {@link RateLimiter}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class RateLimiter {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimiter.class);

    /**
     * Initializes a new {@link RateLimiter}.
     */
    private RateLimiter() {
        super();
    }

    private static final KeyPartProvider HTTP_SESSION_KEY_PART_PROVIDER = new KeyPartProvider() {

        @Override
        public String getValue(final HttpServletRequest servletRequest) {
            final Map<String, Cookie> cookies = Cookies.cookieMapFor(servletRequest);
            if (null == cookies) {
                return null;
            }
            final Cookie cookie = cookies.get("JSESSIONID");
            return null == cookie ? null : cookie.getValue();
        }
    };

    private static final class CookieKeyPartProvider implements KeyPartProvider {

        private final String cookieName;

        CookieKeyPartProvider(final String cookieName) {
            super();
            this.cookieName = asciiLowerCase(cookieName);
        }

        @Override
        public String getValue(final HttpServletRequest servletRequest) {
            final Map<String, Cookie> cookies = Cookies.cookieMapFor(servletRequest);
            if (null == cookies) {
                return null;
            }
            final Cookie cookie = cookies.get(cookieName);
            return null == cookie ? null : cookie.getValue();
        }

    }

    private static final class HeaderKeyPartProvider implements KeyPartProvider {

        private final String headerName;

        HeaderKeyPartProvider(final String headerName) {
            super();
            this.headerName = headerName;
        }

        @Override
        public String getValue(final HttpServletRequest servletRequest) {
            return servletRequest.getHeader(headerName);
        }

    }

    private static final class ParameterKeyPartProvider implements KeyPartProvider {

        private final String paramName;

        ParameterKeyPartProvider(final String paramName) {
            super();
            this.paramName = paramName;
        }

        @Override
        public String getValue(final HttpServletRequest servletRequest) {
            return servletRequest.getParameter(paramName);
        }

    }

    private static volatile List<KeyPartProvider> keyPartProviders;

    static List<KeyPartProvider> keyPartProviders() {
        List<KeyPartProvider> tmp = keyPartProviders;
        if (null == tmp) {
            synchronized (CountingHttpServletRequest.class) {
                tmp = keyPartProviders;
                if (null == tmp) {
                    final ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    if (null == service) {
                        // Service not yet available
                        return Collections.emptyList();
                    }
                    final String sProviders = service.getProperty("com.openexchange.servlet.maxRateKeyPartProviders");
                    if (com.openexchange.java.Strings.isEmpty(sProviders)) {
                        tmp = Collections.emptyList();
                    } else {
                        final List<KeyPartProvider> list = new LinkedList<KeyPartProvider>();
                        for (final String sProvider : Strings.splitByComma(sProviders)) {
                            final String s = asciiLowerCase(sProvider);
                            if ("http-session".equals(s)) {
                                list.add(HTTP_SESSION_KEY_PART_PROVIDER);
                            } else if (s.startsWith("cookie-")) {
                                list.add(new CookieKeyPartProvider(s.substring(7)));
                            } else if (s.startsWith("header-")) {
                                list.add(new HeaderKeyPartProvider(s.substring(7)));
                            } else if (s.startsWith("parameter-")) {
                                list.add(new ParameterKeyPartProvider(s.substring(10)));
                            }
                        }
                        tmp = Collections.unmodifiableList(list);
                    }
                    keyPartProviders = tmp;
                }
            }
        }
        return tmp;
    }

    private static volatile Boolean considerRemotePort;

    static boolean considerRemotePort() {
        Boolean tmp = considerRemotePort;
        if (null == tmp) {
            synchronized (CountingHttpServletRequest.class) {
                tmp = considerRemotePort;
                if (null == tmp) {
                    final ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    if (null == service) {
                        return false;
                    }
                    tmp = Boolean.valueOf(service.getProperty("com.openexchange.servlet.maxRateConsiderRemotePort", "false"));
                    considerRemotePort = tmp;
                }
            }
        }
        return tmp.booleanValue();
    }

    // ----------------------------------------------------------------------------------- //



    // ----------------------------------------------------------------------------------- //

    private static volatile ScheduledTimerTask timerTask;
    private static volatile ConcurrentMap<Key, Rate> bucketMap;

    /**
     * Gets the bucket map for rate limit slots.
     *
     * @return The bucket map or <code>null</code> if not yet initialized
     */
    private static ConcurrentMap<Key, Rate> bucketMap() {
        ConcurrentMap<Key, Rate> tmp = bucketMap;
        if (null == tmp) {
            synchronized (RateLimiter.class) {
                tmp = bucketMap;
                if (null == tmp) {
                    final ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    final TimerService timerService = ServerServiceRegistry.getInstance().getService(TimerService.class);
                    if (null == service || null == timerService) {
                        LOG.info(RateLimiter.class.getSimpleName() + " not yet fully initialized; awaiting " + (null == service ? ConfigurationService.class.getSimpleName() : "") + " " + (null == timerService ? TimerService.class.getSimpleName() : ""));
                        return null;
                    }
                    final int maxCapacity = service.getIntProperty("com.openexchange.servlet.maxActiveSessions", 250000);
                    final ConcurrentLinkedHashMap<Key, Rate> tmp2 = new ConcurrentLinkedHashMap<Key, Rate>(256, 0.75F, 16, maxCapacity, new LRUPolicy());
                    tmp = tmp2;
                    bucketMap = tmp;

                    final int delay = maxRateTimeWindow(); // delay of window size (default: 5 minutes)
                    final Runnable r = new Runnable() {

                        @Override
                        public void run() {
                            try {
                                final long mark = System.currentTimeMillis() - (delay << 1); // Older than doubled delay -- 10 minutes
                                // Iterator obeys LRU policy therefore stop after first non-elapsed entry
                                for (final Iterator<Entry<Key, Rate>> iterator = tmp2.entrySet().iterator(); iterator.hasNext();) {
                                    final Entry<Key, Rate> entry = iterator.next();
                                    if (!entry.getValue().markDeprecatedIfElapsed(mark)) {
                                        break;
                                    }
                                    iterator.remove();
                                }
                            } catch (final Exception e) {
                                // Ignore
                            }
                        }
                    };
                    timerTask = timerService.scheduleWithFixedDelay(r, delay, delay);
                }
            }
        }
        return tmp;
    }

    private static volatile Integer maxRate;

    private static int maxRate() {
        Integer tmp = maxRate;
        if (null == tmp) {
            synchronized (RateLimiter.class) {
                tmp = maxRate;
                if (null == tmp) {
                    final ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    if (null == service) {
                        // Service not yet available
                        return 500;
                    }
                    tmp = Integer.valueOf(service.getProperty("com.openexchange.servlet.maxRate", "500"));
                    maxRate = tmp;
                }
            }
        }
        return tmp.intValue();
    }

    private static volatile Integer maxRateTimeWindow;

    private static int maxRateTimeWindow() {
        Integer tmp = maxRateTimeWindow;
        if (null == tmp) {
            synchronized (RateLimiter.class) {
                tmp = maxRateTimeWindow;
                if (null == tmp) {
                    final ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    if (null == service) {
                        // Service not yet available
                        return 300000;
                    }
                    tmp = Integer.valueOf(service.getProperty("com.openexchange.servlet.maxRateTimeWindow", "300000"));
                    maxRateTimeWindow = tmp;
                }
            }
        }
        return tmp.intValue();
    }

    private static volatile Boolean omitLocals;

    private static boolean omitLocals() {
        Boolean tmp = omitLocals;
        if (null == tmp) {
            synchronized (RateLimiter.class) {
                tmp = omitLocals;
                if (null == tmp) {
                    final ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    if (null == service) {
                        // Service not yet available
                        return false;
                    }
                    tmp = Boolean.valueOf(service.getProperty("com.openexchange.servlet.maxRateOmitLocals", "false"));
                    omitLocals = tmp;
                }
            }
        }
        return tmp.booleanValue();
    }

    // ----------------------------------------------------------------------------------- //

    /**
     * Stops scheduled time task.
     */
    public static void stop() {
        final ScheduledTimerTask t = timerTask;
        if (null != t) {
            t.cancel();
            timerTask = null;
        }
    }

    // ----------------------------------------------------------------------------------- //

    private static final Set<String> LOCALS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("localhost", "127.0.0.1", "::1")));

    private static final String LINE_SEP = System.getProperty("line.separator");
    private static final long LAST_RATE_LIMIT_LOG_THRESHOLD = 60000L;

    private static AtomicLong processedRequests = new AtomicLong();

    /**
     * Gets the number of requests that have been processed in the rate limiter.
     *
     * @return The processed requests
     */
    public static long getProcessedRequests() {
        return processedRequests.get();
    }

    /**
     * Gets the number of currently tracked rate limit slots held in the internal bucket map.
     *
     * @return The slot count
     */
    public static long getSlotCount() {
        return getSlotCount(false);
    }

    /**
     * Gets the number of currently tracked rate limit slots held in the internal bucket map.
     *
     * @param purge <code>true</code> to trigger pending cleanup operations prior getting the size, <code>false</code>, otherwise
     * @return The slot count
     */
    static long getSlotCount(boolean purge) {
        ConcurrentMap<Key, Rate> bucketMap = bucketMap();
        if (null == bucketMap) {
            return 0L;
        }
        if (purge) {
            long mark = System.currentTimeMillis() - 2 * maxRateTimeWindow(); // Older than doubled delay -- (default: 10 minutes);
            for (Iterator<Entry<Key, Rate>> iterator = bucketMap.entrySet().iterator(); iterator.hasNext();) {
                Entry<Key, Rate> entry = iterator.next();
                if (false == entry.getValue().markDeprecatedIfElapsed(mark)) {
                    break;
                }
                iterator.remove();
            }
        }
        return bucketMap.size();
    }

    /**
     * Clears the internal bucket map.
     */
    public static void clear() {
        ConcurrentMap<Key, Rate> bucketMap = bucketMap();
        if (null != bucketMap) {
            bucketMap.clear();
        }
    }

    /**
     * Checks given request if possibly rate limited.
     *
     * @param httpRequest The request to check
     * @throws RateLimitedException If associated request is rate limited
     */
    public static void checkRequest(HttpServletRequest httpRequest) {
        // Any request...
        int maxRate = maxRate();
        if (maxRate <= 0) {
            return;
        }
        int maxRateTimeWindow = maxRateTimeWindow();
        if (maxRateTimeWindow <= 0) {
            return;
        }
        if (omitLocals() && LOCALS.contains(httpRequest.getServerName())) {
            return;
        }
        if (lenientCheckForRequest(httpRequest)) {
            return;
            //maxRatePerMinute <<= 2;
        }

        // Do the rate limit check
        checkRateLimitFor(new Key(httpRequest), maxRate, maxRateTimeWindow, httpRequest);
    }

    /**
     * Performs the actual rate limit check.
     *
     * @param key The key calculated from associated HTTP request used to determine the appropriate rate limit bucket
     * @param maxRate The associated rate
     * @param maxRateTimeWindow The associated time window
     * @param optRequest The checked HTTP request (rather for logging purposes); may be <code>null</code>
     * @throws RateLimitedException If rate limit is exceeded
     */
    public static void checkRateLimitFor(Key key, int maxRate, int maxRateTimeWindow, HttpServletRequest optRequest) {
        checkRateLimitForRequest(key, maxRate, maxRateTimeWindow, true, optRequest);
    }

    /**
     * Performs the actual rate limit check (only if a rate limit trace is already available).
     *
     * @param key The key calculated from associated HTTP request used to determine the appropriate rate limit bucket
     * @param maxRate The associated rate
     * @param maxRateTimeWindow The associated time window
     * @param createIfAbsent Whether to create the rate limit trace or not
     * @param optRequest The checked HTTP request (rather for logging purposes); may be <code>null</code>
     * @return <code>true</code> if a rate permit was consumed; otherwise <code>false</code>
     * @throws RateLimitedException If rate limit is exceeded
     */
    public static boolean optRateLimitFor(Key key, int maxRate, int maxRateTimeWindow, HttpServletRequest optRequest) {
        return checkRateLimitForRequest(key, maxRate, maxRateTimeWindow, false, optRequest);
    }

    /**
     * Performs the actual rate limit check.
     *
     * @param key The key calculated from associated HTTP request used to determine the appropriate rate limit bucket
     * @param maxRate The associated rate
     * @param maxRateTimeWindow The associated time window
     * @param createIfAbsent Whether to create the rate limit trace or not
     * @param optRequest The checked HTTP request (rather for logging purposes); may be <code>null</code>
     * @return <code>true</code> if a rate permit was consumed; otherwise <code>false</code>
     * @throws RateLimitedException If rate limit is exceeded
     */
    private static boolean checkRateLimitForRequest(Key key, int maxRate, int maxRateTimeWindow, boolean createIfAbsent, HttpServletRequest optRequest) {
        ConcurrentMap<Key, Rate> bucketMap = bucketMap();
        if (null == bucketMap) {
            // Not yet fully initialized
            return false;
        }
        processedRequests.incrementAndGet();
        while (true) {
            Rate rate = bucketMap.get(key);
            if (null == rate) {
                if (false == createIfAbsent) {
                    // No rate limit trace available
                    return false;
                }

                // Requested to create a rate limit trace, hence do so
                Rate newRate = new RateImpl(maxRate, maxRateTimeWindow, TimeUnit.MILLISECONDS);
                rate = bucketMap.putIfAbsent(key, newRate);
                if (null == rate) {
                    rate = newRate;
                }
            }
            // Acquire or fail to do so
            Rate.Result res = rate.consume(System.currentTimeMillis());
            if (Result.DEPRECATED == res) {
                // Deprecated
                bucketMap.remove(key, rate);
            } else {
                // Success or failure?
                if (Result.SUCCESS == res) {
                    return true;
                }

                // Rate limit exceeded
                if (null != optRequest) {
                    logRateLimitExceeded(rate, optRequest);
                }
                throw new RateLimitedException("429 Too Many Requests", maxRateTimeWindow / 1000);
            }
            // Otherwise retry
        }
    }

    private static void logRateLimitExceeded(Rate rate, HttpServletRequest servletRequest) {
        AtomicLong lastAtomicLogStamp = rate.getLastLogStamp();
        long lastLogStamp = lastAtomicLogStamp.get();
        long now = System.currentTimeMillis();
        if (now - lastLogStamp > LAST_RATE_LIMIT_LOG_THRESHOLD && lastAtomicLogStamp.compareAndSet(lastLogStamp, now)) {
            LOG.info("Request with IP '{}' to path '{}' has been rate limited.{}", servletRequest.getRemoteAddr(), servletRequest.getServletPath(), LINE_SEP);
        } else {
            LOG.debug("Request with IP '{}' to path '{}' has been rate limited.{}", servletRequest.getRemoteAddr(), servletRequest.getServletPath(), LINE_SEP);
        }
    }

    /**
     * Removes the rate limit trace
     *
     * @param httpRequest The HHTP request for which to delete the rate limit trace
     */
    public static void removeRateLimit(HttpServletRequest httpRequest) {
        removeRateLimit(new Key(httpRequest));
    }

    /**
     * Removes the rate limit trace
     *
     * @param key The key associated with the rate limit trace
     */
    public static void removeRateLimit(Key key) {
        if (null == key) {
            return;
        }
        ConcurrentMap<Key, Rate> bucketMap = bucketMap();
        if (null == bucketMap) {
            // Not yet fully initialized
            return;
        }

        bucketMap.remove(key);
    }

    /**
     * Doubles time windows for associated rate limit trace
     *
     * @param key The key associated with the rate limit trace
     */
    public static void doubleRateLimitWindow(Key key) {
        if (null == key) {
            return;
        }
        ConcurrentMap<Key, Rate> bucketMap = bucketMap();
        if (null == bucketMap) {
            // Not yet fully initialized
            return;
        }

        Rate rate = bucketMap.get(key);
        if (null != rate) {
            rate.setTimeInMillis(rate.getTimeInMillis() << 1);
        }
    }

    // ------------------------- Lenient clients ----------------------------------------- //

    private static interface StringChecker {

        boolean matches(String identifier);
    }

    private static final class StartsWithStringChecker implements StringChecker {

        private final String[] prefixes;

        StartsWithStringChecker(final List<String> prefixes) {
            super();
            final int size = prefixes.size();
            final String[] newArray = new String[size];
            for (int i = 0; i < size; i++) {
                newArray[i] = asciiLowerCase(prefixes.get(i));
            }
            this.prefixes = newArray;
        }

        @Override
        public boolean matches(final String identifier) {
            final String lc = asciiLowerCase(identifier);
            for (final String prefix : prefixes) {
                if (lc.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }

    }

    private static final class IgnoreCaseStringChecker implements StringChecker {

        private final String userAgent;

        IgnoreCaseStringChecker(final String userAgent) {
            super();
            this.userAgent = asciiLowerCase(userAgent);
        }

        @Override
        public boolean matches(final String identifier) {
            return this.userAgent.equals(asciiLowerCase(identifier));
        }

    }

    private static final class PatternUserAgentChecker implements StringChecker {

        private final Pattern pattern;

        PatternUserAgentChecker(final String wildcard) {
            super();
            pattern = Pattern.compile(wildcardToRegex(wildcard), Pattern.CASE_INSENSITIVE);
        }

        @Override
        public boolean matches(final String identifier) {
            return pattern.matcher(identifier).matches();
        }

    }

    private static volatile List<StringChecker> userAgentCheckers;

    private static List<StringChecker> userAgentCheckers() {
        List<StringChecker> tmp = userAgentCheckers;
        if (null == tmp) {
            synchronized (RateLimiter.class) {
                tmp = userAgentCheckers;
                if (null == tmp) {
                    final ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    if (null == service) {
                        return Collections.emptyList();
                    }
                    final String sProviders;
                    {
                        final String defaultValue = "\"Open-Xchange .NET HTTP Client*\", \"Open-Xchange USM HTTP Client*\", \"Jakarta Commons-HttpClient*\"";
                        sProviders = service.getProperty("com.openexchange.servlet.maxRateLenientClients", defaultValue);
                    }
                    if (com.openexchange.java.Strings.isEmpty(sProviders)) {
                        tmp = Collections.emptyList();
                    } else {
                        final List<StringChecker> list = new LinkedList<StringChecker>();
                        final List<String> startsWiths = new LinkedList<String>();
                        for (final String sChecker : Strings.splitByComma(sProviders)) {
                            String s = unquote(sChecker);
                            if (!com.openexchange.java.Strings.isEmpty(s)) {
                                s = s.trim();
                                if (isStartsWith(s)) {
                                    // Starts-with
                                    startsWiths.add(s.substring(0, s.length() - 1));
                                } else if (s.indexOf('*') >= 0 || s.indexOf('?') >= 0) {
                                    // Pattern
                                    list.add(new PatternUserAgentChecker(s));
                                } else {
                                    list.add(new IgnoreCaseStringChecker(s));
                                }
                            }
                        }
                        if (!startsWiths.isEmpty()) {
                            list.add(0, new StartsWithStringChecker(startsWiths));
                        }
                        tmp = list.isEmpty() ? Collections.<StringChecker> emptyList() : (1 == list.size() ? Collections.singletonList(list.get(0)) : Collections.unmodifiableList(list));
                    }
                    userAgentCheckers = tmp;
                }
            }
        }
        return tmp;
    }

    private static volatile List<StringChecker> remoteAddressCheckers;

    private static List<StringChecker> remoteAddressCheckers() {
        List<StringChecker> tmp = remoteAddressCheckers;
        if (null == tmp) {
            synchronized (RateLimiter.class) {
                tmp = remoteAddressCheckers;
                if (null == tmp) {
                    final ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    if (null == service) {
                        return Collections.emptyList();
                    }
                    final String sRemoteAddrs = service.getProperty("com.openexchange.servlet.maxRateLenientRemoteAddresses");
                    if (com.openexchange.java.Strings.isEmpty(sRemoteAddrs)) {
                        tmp = Collections.emptyList();
                    } else {
                        final List<StringChecker> list = new LinkedList<StringChecker>();
                        final List<String> startsWiths = new LinkedList<String>();
                        for (final String sChecker : Strings.splitByComma(sRemoteAddrs)) {
                            String s = unquote(sChecker);
                            if (!com.openexchange.java.Strings.isEmpty(s)) {
                                s = s.trim();
                                if (isStartsWith(s)) {
                                    // Starts-with
                                    startsWiths.add(s.substring(0, s.length() - 1));
                                } else if (s.indexOf('*') >= 0 || s.indexOf('?') >= 0) {
                                    // Pattern
                                    list.add(new PatternUserAgentChecker(s));
                                } else {
                                    list.add(new IgnoreCaseStringChecker(s));
                                }
                            }
                        }
                        if (!startsWiths.isEmpty()) {
                            list.add(0, new StartsWithStringChecker(startsWiths));
                        }
                        tmp = list.isEmpty() ? Collections.<StringChecker> emptyList() : (1 == list.size() ? Collections.singletonList(list.get(0)) : Collections.unmodifiableList(list));
                    }
                    remoteAddressCheckers = tmp;
                }
            }
        }
        return tmp;
    }

    private static final Cache<String, Boolean> CACHE_AGENTS = CacheBuilder.newBuilder().maximumSize(250).expireAfterWrite(30, TimeUnit.MINUTES).build();

    private static boolean lenientCheckForUserAgent(final String userAgent) {
        if (null != userAgent) {
            Boolean result = CACHE_AGENTS.getIfPresent(userAgent);
            if (null == result) {
                for (final StringChecker checker : userAgentCheckers()) {
                    if (checker.matches(userAgent)) {
                        result = Boolean.TRUE;
                        break;
                    }
                }
                if (null == result) {
                    result = Boolean.FALSE;
                }
                CACHE_AGENTS.put(userAgent, result);
            }

            return result.booleanValue();
        }
        return false;
    }

    private static final Cache<String, Boolean> CACHE_REMOTE_ADDRS = CacheBuilder.newBuilder().maximumSize(2500).expireAfterWrite(30, TimeUnit.MINUTES).build();

    private static boolean lenientCheckForRemoteAddress(final String remoteAddress) {
        if (null != remoteAddress) {
            Boolean result = CACHE_REMOTE_ADDRS.getIfPresent(remoteAddress);
            if (null == result) {
                for (final StringChecker checker : remoteAddressCheckers()) {
                    if (checker.matches(remoteAddress)) {
                        result = Boolean.TRUE;
                        break;
                    }
                }
                if (null == result) {
                    result = Boolean.FALSE;
                }
                CACHE_REMOTE_ADDRS.put(remoteAddress, result);
            }

            return result.booleanValue();
        }
        return false;
    }

    private static volatile List<String> modules;

    private static List<String> modules() {
        List<String> tmp = modules;
        if (null == tmp) {
            synchronized (RateLimiter.class) {
                tmp = modules;
                if (null == tmp) {
                    final ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    if (null == service) {
                        return Arrays.asList("rt", "system");
                    }
                    final String sModules;
                    {
                        final String defaultValue = "rt, system";
                        sModules = service.getProperty("com.openexchange.servlet.maxRateLenientModules", defaultValue);
                    }
                    if (com.openexchange.java.Strings.isEmpty(sModules)) {
                        tmp = Collections.emptyList();
                    } else {
                        final Set<String> set = new LinkedHashSet<String>();
                        for (final String sModule : Strings.splitByComma(sModules)) {
                            String s = unquote(sModule);
                            if (!com.openexchange.java.Strings.isEmpty(s)) {
                                s = asciiLowerCase(s.trim());
                                set.add(s);
                            }
                        }
                        tmp = set.isEmpty() ? Collections.<String> emptyList() : (1 == set.size() ? Collections.singletonList(set.iterator().next()) : Collections.unmodifiableList(new ArrayList<String>(set)));
                    }
                    modules = tmp;
                }
            }
        }
        return tmp;
    }

    private static final Cache<String, Boolean> CACHE_PATHS = CacheBuilder.newBuilder().maximumSize(1500).expireAfterWrite(2, TimeUnit.HOURS).build();

    private static boolean lenientCheckForRequest(HttpServletRequest servletRequest) {
        // Servlet path check
        {
            String requestURI = servletRequest.getRequestURI();
            Boolean result = CACHE_PATHS.getIfPresent(requestURI);
            if (null == result) {
                // Check modules
                {
                    StringBuilder sb = new StringBuilder(asciiLowerCase(ServerServiceRegistry.getServize(DispatcherPrefixService.class).getPrefix()));
                    int reslen = sb.length();
                    String lcRequestURI = asciiLowerCase(requestURI);
                    for (String module : modules()) {
                        sb.setLength(reslen);
                        if (lcRequestURI.startsWith(sb.append(module).toString())) {
                            result = Boolean.TRUE;
                            break;
                        }
                    }
                }
                if (null == result) {
                    result = Boolean.FALSE;
                }
                CACHE_PATHS.put(requestURI, result);
            }

            if (result.booleanValue()) {
                return true;
            }
        }

        // User-Agent check
        if (lenientCheckForUserAgent(servletRequest.getHeader("User-Agent"))) {
            return true;
        }

        // Remote address check
        if (lenientCheckForRemoteAddress(servletRequest.getRemoteAddr())) {
            return true;
        }

        // A thumbnail request
        if (ImageTransformationUtility.seemsLikeThumbnailRequest(servletRequest)) {
            return true;
        }

        return false;
    }

    /** Converts specified wild-card string to a regular expression */
    static String wildcardToRegex(final String wildcard) {
        final StringBuilder s = new StringBuilder(wildcard.length());
        s.append('^');
        final int len = wildcard.length();
        for (int i = 0; i < len; i++) {
            final char c = wildcard.charAt(i);
            if (c == '*') {
                s.append(".*");
            } else if (c == '?') {
                s.append('.');
            } else if (c == '(' || c == ')' || c == '[' || c == ']' || c == '$' || c == '^' || c == '.' || c == '{' || c == '}' || c == '|' || c == '\\') {
                s.append('\\');
                s.append(c);
            } else {
                s.append(c);
            }
        }
        s.append('$');
        return (s.toString());
    }

    /** Checks for starts-with notation */
    private static boolean isStartsWith(final String s) {
        if (!s.endsWith("*")) {
            return false;
        }
        final int mlen = s.length() - 1;
        int pos = s.indexOf("?");
        if (pos >= 0) {
            return false;
        }
        pos = s.indexOf("*");
        if (pos >= 0 && pos < mlen) {
            return false;
        }
        return true;
    }

    /** Removes single or double quotes from a string if its quoted. */
    private static String unquote(final String s) {
        if (!com.openexchange.java.Strings.isEmpty(s) && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

}
