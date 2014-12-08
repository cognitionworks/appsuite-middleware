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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.http.requestwatcher.internal;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.config.ConfigurationService;
import com.openexchange.http.requestwatcher.osgi.Services;
import com.openexchange.http.requestwatcher.osgi.services.RequestRegistryEntry;
import com.openexchange.http.requestwatcher.osgi.services.RequestWatcherService;
import com.openexchange.log.LogProperties;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.sessiond.SessiondServiceExtended;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;

/**
 * {@link RequestWatcherServiceImpl}
 *
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
public class RequestWatcherServiceImpl implements RequestWatcherService {

    /** The logger. */
    protected static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RequestWatcherServiceImpl.class);

    /** The request number */
    private static final AtomicLong NUMBER = new AtomicLong();

    /** Navigable set, entries ordered by age(youngest first), weakly consistent iterator */
    private final ConcurrentSkipListSet<RequestRegistryEntry> requestRegistry;

    /** The watcher task */
    private volatile ScheduledTimerTask requestWatcherTask;

    /**
     * Initializes a new {@link RequestWatcherServiceImpl}.
     */
    public RequestWatcherServiceImpl() {
        super();
        requestRegistry = new ConcurrentSkipListSet<RequestRegistryEntry>();
        // Get Configuration
        final ConfigurationService configService = Services.getService(ConfigurationService.class);
        final boolean isWatcherEnabled = configService.getBoolProperty("com.openexchange.requestwatcher.isEnabled", true);
        final int watcherFrequency = configService.getIntProperty("com.openexchange.requestwatcher.frequency", 30000);
        final int requestMaxAge = configService.getIntProperty("com.openexchange.requestwatcher.maxRequestAge", 60000);
        if (isWatcherEnabled) {
            // Create ScheduledTimerTask to watch requests
            final ConcurrentSkipListSet<RequestRegistryEntry> requestRegistry = this.requestRegistry;
            final String lineSeparator = System.getProperty("line.separator");
            final ScheduledTimerTask requestWatcherTask = Services.getService(TimerService.class).scheduleAtFixedRate(new Runnable() {

                /*
                 * Start at the end of the navigable Set to get the oldest request first. Then proceed to the younger requests. Stop
                 * processing at the first still valid request.
                 */
                @Override
                public void run() {
                    try {
                        final boolean debugEnabled = LOG.isDebugEnabled();
                        final Iterator<RequestRegistryEntry> descendingEntryIterator = requestRegistry.descendingIterator();
                        final StringBuilder sb = new StringBuilder(256);
                        boolean stillOldRequestsLeft = true;
                        while (stillOldRequestsLeft && descendingEntryIterator.hasNext()) {
                            if (debugEnabled) {
                                sb.setLength(0);
                                for (final RequestRegistryEntry entry : requestRegistry) {
                                    sb.append(lineSeparator).append("RegisteredThreads:").append(lineSeparator).append("    age: ").append(entry.getAge()).append(" ms").append(
                                        ", thread: ").append(entry.getThreadInfo());
                                }
                                final String entries = sb.toString();
                                if (!entries.isEmpty()) {
                                    LOG.debug(sb.toString());
                                }
                            }
                            final RequestRegistryEntry requestRegistryEntry = descendingEntryIterator.next();
                            if (requestRegistryEntry.getAge() > requestMaxAge) {
                                sb.setLength(0);
                                logRequestRegistryEntry(requestRegistryEntry, sb);
                                // Don't remove
                                // requestRegistry.remove(requestRegistryEntry);
                            } else {
                                stillOldRequestsLeft = false;
                            }
                        }
                    } catch (final Exception e) {
                        LOG.error("Request watcher run failed", e);
                    }
                }

                private void logRequestRegistryEntry(final RequestRegistryEntry entry, final StringBuilder logBuilder) {
                    final Throwable trace = new FastThrowable();
                    {
                        final StackTraceElement[] stackTrace = entry.getStackTrace();
                        if (dontLog(stackTrace)) {
                            return;
                        }
                        trace.setStackTrace(stackTrace);
                    }
                    logBuilder.append("Request").append(lineSeparator);

                    // If we have additional log properties from the ThreadLocal add it to the logBuilder
                    {
                        // Sort the properties for readability
                        Map<String, String> sorted = new TreeMap<String, String>();
                        for (Entry<String, String> propertyEntry : entry.getPropertyMap().entrySet()) {
                            String propertyName = propertyEntry.getKey();
                            String value = propertyEntry.getValue();
                            if (null != value) {
                                if (LogProperties.Name.SESSION_SESSION_ID.getName().equals(propertyName) && !isValidSession(value.toString())) {
                                    // Non-existent or elapsed session
                                    entry.getThread().interrupt();
                                    requestRegistry.remove(entry);
                                    return;
                                }
                                sorted.put(propertyName, value);
                            }
                        }
                        logBuilder.append("with properties:").append(lineSeparator);
                        // And add them to the logBuilder
                        for (Map.Entry<String, String> propertyEntry : sorted.entrySet()) {
                            logBuilder.append(propertyEntry.getKey()).append('=').append(propertyEntry.getValue()).append(lineSeparator);
                        }
                    }

                    LOG.info(logBuilder.append("with age: ").append(entry.getAge()).append("ms exceeds max. age of: ").append(requestMaxAge).append("ms.").toString(), trace);
                }

                private boolean isValidSession(final String sessionId) {
                    final SessiondService sessiondService = SessiondService.SERVICE_REFERENCE.get();
                    return sessiondService instanceof SessiondServiceExtended ? ((SessiondServiceExtended) sessiondService).isActive(sessionId) : true;
                }

                private boolean dontLog(final StackTraceElement[] stackTrace) {
                    for (final StackTraceElement ste : stackTrace) {
                        final String className = ste.getClassName();
                        if (null != className) {
                            if (className.startsWith("org.apache.commons.fileupload.MultipartStream$ItemInputStream")) {
                                // A long-running file upload. Ignore
                                return true;
                            }
                        }
                    }
                    return false;
                }
            },
                1000,
                watcherFrequency,
                TimeUnit.MILLISECONDS);
            this.requestWatcherTask = requestWatcherTask;
        }
    }

    @Override
    public RequestRegistryEntry registerRequest(final HttpServletRequest request, final HttpServletResponse response, final Thread thread, final Map<String, String> propertyMap) {
        final RequestRegistryEntry registryEntry = new RequestRegistryEntry(NUMBER.incrementAndGet(), request, response, thread, propertyMap);
        requestRegistry.add(registryEntry);
        return registryEntry;
    }

    @Override
    public boolean unregisterRequest(final RequestRegistryEntry registryEntry) {
        return requestRegistry.remove(registryEntry);
    }

    @Override
    public boolean stopWatching() {
        final ScheduledTimerTask requestWatcherTask = this.requestWatcherTask;
        if (null != requestWatcherTask) {
            return requestWatcherTask.cancel();
        }
        return true;
    }

    // ----------------------------------------------------------------------------- //

    static final class FastThrowable extends Throwable {

        FastThrowable() {
            super("tracked request");
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

}
