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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.i18n.osgi;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.ConfigurationServiceHolder;
import com.openexchange.exceptions.osgi.ComponentRegistration;
import com.openexchange.i18n.I18nService;
import com.openexchange.i18n.impl.CompositeI18nTools;
import com.openexchange.i18n.impl.I18nImpl;
import com.openexchange.i18n.impl.POTranslationsDiscoverer;
import com.openexchange.i18n.impl.ResourceBundleDiscoverer;
import com.openexchange.i18n.impl.TranslationsI18N;
import com.openexchange.i18n.parsing.I18NErrorMessages;
import com.openexchange.i18n.parsing.Translations;
import com.openexchange.server.ServiceHolderListener;
import com.openexchange.server.osgiservice.BundleServiceTracker;

public class I18nActivator implements BundleActivator {

    private ComponentRegistration componentRegistration;

    /**
     * {@link I18nServiceHolderListener} - Properly registers all I18n services defined through property <code>"i18n.language.path"</code>
     * when configuration service is available
     */
    private static final class I18nServiceHolderListener implements ServiceHolderListener<ConfigurationService> {

        private final BundleContext context;

        private final ConfigurationServiceHolder csh;

        private ServiceRegistration[] serviceRegistrations;

        public I18nServiceHolderListener(final BundleContext context, final ConfigurationServiceHolder csh) {
            super();
            this.context = context;
            this.csh = csh;
        }

        public void onServiceAvailable(final ConfigurationService service) throws Exception {
            unregisterAll();
            final ConfigurationService config = csh.getService();
            try {
                serviceRegistrations = initI18nServices(context, config);
            } finally {
                csh.ungetService(config);
            }
        }

        public void onServiceRelease() throws Exception {
        }

        /**
         * Unregisters all registered I18n services and resets them to <code>null</code>.
         */
        public void unregisterAll() {
            if (null == serviceRegistrations) {
                return;
            }
            for (int i = 0; i < serviceRegistrations.length; i++) {
                serviceRegistrations[i].unregister();
                serviceRegistrations[i] = null;
            }
            serviceRegistrations = null;
            if (LOG.isInfoEnabled()) {
                LOG.info("All I18n services unregistered");
            }
        }
    }

    private static final Log LOG = LogFactory.getLog(I18nActivator.class);

    private static final boolean DEBUG = LOG.isDebugEnabled();

    /**
     * Reads in all I18n services configured through property <code>"i18n.language.path"</code>, registers them, and returns corresponding
     * service registrations for future unregistration.
     * 
     * @param context The current valid bundle context
     * @return The corresponding service registrations of registered I18n services
     * @throws FileNotFoundException If directory referenced by <code>"i18n.language.path"</code> does not exist
     */
    private static ServiceRegistration[] initI18nServices(final BundleContext context, final ConfigurationService config) throws FileNotFoundException {

        final File dir = new File(config.getProperty("i18n.language.path"));

        final List<ResourceBundle> resourceBundles = new ResourceBundleDiscoverer(dir).getResourceBundles();
        final List<Translations> translations = new POTranslationsDiscoverer(dir).getTranslations();
        final List<ServiceRegistration> serviceRegistrations = new ArrayList<ServiceRegistration>();

        final Map<Locale, List<I18nService>> locales = new HashMap<Locale, List<I18nService>>();

        for (final Translations tr : translations) {

            List<I18nService> list = locales.get(tr.getLocale());
            if (list == null) {
                list = new ArrayList<I18nService>();
                locales.put(tr.getLocale(), list);
            }

            list.add(new TranslationsI18N(tr));
        }

        for (final ResourceBundle rc : resourceBundles) {

            List<I18nService> list = locales.get(rc.getLocale());
            if (list == null) {
                list = new ArrayList<I18nService>();
                locales.put(rc.getLocale(), list);
            }

            list.add(new I18nImpl(rc));

            final Properties prop = new Properties();
            prop.put("language", rc.getLocale());

        }

        for (final Locale locale : locales.keySet()) {
            final List<I18nService> list = locales.get(locale);

            final Properties prop = new Properties();
            prop.put("language", locale);

            I18nService i18n = null;
            if (list.size() == 1) {
                i18n = list.get(0);
            } else {
                i18n = new CompositeI18nTools(list);
            }

            serviceRegistrations.add(context.registerService(I18nService.class.getName(), i18n, prop));

        }

        if (LOG.isInfoEnabled()) {
            LOG.info("All I18n services registered");
        }
        return serviceRegistrations.toArray(new ServiceRegistration[serviceRegistrations.size()]);
    }

    private ConfigurationServiceHolder csh;

    private I18nServiceHolderListener listener;

    private final List<ServiceTracker> serviceTrackerList = new ArrayList<ServiceTracker>();

    public void start(final BundleContext context) throws Exception {

        if (DEBUG) {
            LOG.debug("I18n Starting");
        }
        componentRegistration = new ComponentRegistration(context, "I18N", "com.openexchange.i18n", I18NErrorMessages.FACTORY);
        try {
            csh = ConfigurationServiceHolder.newInstance();

            serviceTrackerList.add(new ServiceTracker(
                context,
                ConfigurationService.class.getName(),
                new BundleServiceTracker<ConfigurationService>(context, csh, ConfigurationService.class)));

            for (final ServiceTracker tracker : serviceTrackerList) {
                tracker.open();
            }

            listener = new I18nServiceHolderListener(context, csh);
            csh.addServiceHolderListener(listener);

        } catch (final Throwable e) {
            throw e instanceof Exception ? (Exception) e : new Exception(e);
        }

        if (DEBUG) {
            LOG.debug("I18n Started");
        }
    }

    public void stop(final BundleContext context) throws Exception {
        if (DEBUG) {
            LOG.debug("Stopping I18n");
        }

        try {
            csh.removeServiceHolderListenerByName(listener.getClass().getName());
            /*
             * Unregister through listener
             */
            if (null != listener) {
                listener.unregisterAll();
                listener = null;
            }
            csh = null;
            /*
             * Close service trackers
             */
            for (final ServiceTracker tracker : serviceTrackerList) {
                tracker.close();
            }
            serviceTrackerList.clear();
            componentRegistration.unregister();
        } catch (final Throwable e) {
            LOG.error("I18nActivator: stop: ", e);
            throw e instanceof Exception ? (Exception) e : new Exception(e);
        }
        if (DEBUG) {
            LOG.debug("I18n stopped");
        }
    }

}
