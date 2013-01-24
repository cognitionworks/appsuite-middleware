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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import org.apache.commons.logging.Log;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.classloader.osgi.DynamicClassLoaderActivator;
import com.openexchange.exception.internal.I18nCustomizer;
import com.openexchange.i18n.I18nService;
import com.openexchange.log.LogFactory;
import com.openexchange.log.LogWrapperFactory;
import com.openexchange.tools.strings.BasicTypesStringParser;
import com.openexchange.tools.strings.CompositeParser;
import com.openexchange.tools.strings.DateStringParser;
import com.openexchange.tools.strings.StringParser;
import com.openexchange.tools.strings.TimeSpanParser;

/**
 * {@link GlobalActivator} - Activator for global (aka kernel) bundle
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class GlobalActivator implements BundleActivator {

    private static final Log LOG = LogFactory.getLog(GlobalActivator.class);

    private Initialization initialization;
    protected ServiceTracker<StringParser,StringParser> parserTracker = null;
    private ServiceRegistration<StringParser> parserRegistration;
    private List<ServiceTracker<?,?>> trackers;
    private volatile DynamicClassLoaderActivator dynamicClassLoaderActivator;

    /**
     * Initializes a new {@link GlobalActivator}
     */
    public GlobalActivator() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        try {
            initialization = new com.openexchange.server.ServerInitialization();
            initialization.start();
            ServiceHolderInit.getInstance().start();
            initStringParsers(context);

            trackers = new ArrayList<ServiceTracker<?,?>>(2);
            trackers.add(new ServiceTracker<I18nService, I18nService>(context, I18nService.class, new I18nCustomizer(context)));

            final ServiceTracker<LogWrapperFactory, LogWrapperFactory> logWrapperTracker = new ServiceTracker<LogWrapperFactory, LogWrapperFactory>(context, LogWrapperFactory.class, null);
			LogFactory.FACTORY.set(new LogWrapperFactory() {

				@Override
                public Log wrap(final String name, final Log log) {
                    Log retval = log;
                    for (final LogWrapperFactory factory : logWrapperTracker.getTracked().values()) {
                        retval = factory.wrap(name, retval);
                    }
                    return retval;
                }
			});

            trackers.add(logWrapperTracker);

            for (final ServiceTracker<?,?> tracker : trackers) {
                tracker.open();
            }

            final DynamicClassLoaderActivator dynamicClassLoaderActivator = new DynamicClassLoaderActivator();
            dynamicClassLoaderActivator.start(context);
            this.dynamicClassLoaderActivator = dynamicClassLoaderActivator;

            LOG.info("Global bundle successfully started");
        } catch (final Throwable t) {
            LOG.error(t.getMessage(), t);
            throw t instanceof Exception ? (Exception) t : new Exception(t.getMessage(), t);
        }
    }

    private void initStringParsers(final BundleContext context) {
        parserTracker = new ServiceTracker<StringParser,StringParser>(context, StringParser.class, null);
        final List<StringParser> standardParsers = new ArrayList<StringParser>(3);
        final StringParser standardParsersComposite = new CompositeParser() {

            @Override
            protected Collection<StringParser> getParsers() {
                return standardParsers;
            }

        };

        final StringParser allParsers = new CompositeParser() {

            @Override
            protected Collection<StringParser> getParsers() {
                final Object[] services = parserTracker.getServices();
                if(services == null) {
                    return Arrays.asList(standardParsersComposite);
                }
                final List<StringParser> parsers = new ArrayList<StringParser>(services.length);

                for (final Object object : services) {
                    if (object != this) {
                        parsers.add((StringParser) object);
                    }
                }
                parsers.add(standardParsersComposite);
                return parsers;
            }

        };

        standardParsers.add(new BasicTypesStringParser());
        standardParsers.add(new DateStringParser(allParsers));
        standardParsers.add(new TimeSpanParser());

        final Hashtable<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_RANKING, Integer.valueOf(100));

        parserTracker.open();

        parserRegistration = context.registerService(StringParser.class, allParsers, properties);

    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        try {
            final DynamicClassLoaderActivator dynamicClassLoaderActivator = this.dynamicClassLoaderActivator;
            if (null != dynamicClassLoaderActivator) {
                dynamicClassLoaderActivator.stop(context);
                this.dynamicClassLoaderActivator = null;
            }
            if (null != trackers) {
                while (!trackers.isEmpty()) {
                    trackers.remove(0).close();
                }
                trackers = null;
            }
            ServiceHolderInit.getInstance().stop();
            initialization.stop();
            initialization = null;
            shutdownStringParsers();
            LOG.debug("Global bundle successfully stopped");
        } catch (final Throwable t) {
            LOG.error(t.getMessage(), t);
            throw t instanceof Exception ? (Exception) t : new Exception(t.getMessage(), t);
        }
    }

    private void shutdownStringParsers() {
        parserRegistration.unregister();
        parserTracker.close();
    }
}
