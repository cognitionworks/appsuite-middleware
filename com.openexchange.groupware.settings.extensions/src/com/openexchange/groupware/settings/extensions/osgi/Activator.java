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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.groupware.settings.extensions.osgi;

import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.settings.IValueHandler;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.groupware.settings.Setting;
import com.openexchange.groupware.settings.extensions.ServicePublisher;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.session.Session;

/**
 * @author Francisco Laguna <francisco.laguna@open-xchange.com>
 */
public class Activator implements BundleActivator {

    private static final String PREFERENCE_PATH = "preferencePath";

    private static final String METADATA_PREFIX = "meta";

    private ServicePublisher services;

    private BundleContext context;

    private ServiceTracker<ConfigViewFactory, ConfigViewFactory> serviceTracker;

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(Activator.class));

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        services = new OSGiServicePublisher(bundleContext);
        context = bundleContext;
        registerListenerForConfigurationService();
    }

    @Override
    public void stop(final BundleContext bundleContext) throws Exception {
        unregisterListenerForConfigurationService();
        services.removeAllServices();
    }

    public void handleConfigurationUpdate(final ConfigViewFactory viewFactory) {
        LOG.info("Updating configtree");
        try {
            final ConfigView view = viewFactory.getView();
            final Map<String, ComposedConfigProperty<String>> all = view.all();
            for (final Map.Entry<String, ComposedConfigProperty<String>> entry : all.entrySet()) {
                final String propertyName = entry.getKey();
                final ComposedConfigProperty<String> property = entry.getValue();
                if (isPreferenceItem(property)) {
                    export(viewFactory, property, propertyName);
                }
            }
        } catch (final Throwable x) {
            LOG.error(x.getMessage(), x);
        }

    }

    // Maybe that is an overuse of anonymous inner classes. Better get around to refactoring this at some point.

    private void export(final ConfigViewFactory viewFactory, final ComposedConfigProperty<String> property, final String propertyName) throws OXException {

        final String[] path = property.get(PREFERENCE_PATH).split("/");
        final String finalScope = property.get("final");
        final String isProtected = property.get("protected");
        final boolean writable =
            (finalScope == null || finalScope.equals("user")) && (isProtected == null || !property.get("protected", boolean.class).booleanValue());

        final PreferencesItemService prefItem = new PreferencesItemService() {

            private static final String UNDEFINED_STRING = "undefined";

            @Override
            public String[] getPath() {
                return path;
            }

            @Override
            public IValueHandler getSharedValue() {
                return new IValueHandler() {

                    @Override
                    public int getId() {
                        return NO_ID;
                    }

                    @Override
                    public void getValue(final Session session, final Context ctx, final User user, final UserConfiguration userConfig, final Setting setting) throws OXException {
                        try {
                            Object value = viewFactory.getView(user.getId(), ctx.getContextId()).get(propertyName, String.class);
                            if (UNDEFINED_STRING.equals(value)) {
                                setting.setSingleValue(UNDEFINED);
                                return;
                            }
                            try {
                                // Let's turn this into a nice object, if it conforms to JSON
                                value = new JSONObject("{value: " + value + "}").get("value");

                            } catch (final JSONException x) {
                                // Ah well, let's pretend it's a string.
                            }

                            setting.setSingleValue(value);
                        } catch (final OXException e) {
                            throw new OXException(e);
                        }
                    }

                    @Override
                    public boolean isAvailable(final UserConfiguration userConfig) {
                        return true;
                    }

                    @Override
                    public boolean isWritable() {
                        return writable;
                    }

                    @Override
                    public void writeValue(final Session session, final Context ctx, final User user, final Setting setting) throws OXException {
                        Object value = setting.getSingleValue();
                        if (value == null) {
                            final Object[] multiValue = setting.getMultiValue();
                            if (multiValue != null) {

                                final JSONArray arr = new JSONArray();
                                for (final Object o : multiValue) {
                                    arr.put(o);
                                }
                                value = arr.toString();
                            } else if (setting.isEmptyMultivalue()) {
                                value = "[]";
                            }
                        }
                        try {
                            final String oldValue = viewFactory.getView(user.getId(), ctx.getContextId()).get(propertyName, String.class);
                            if (value != null) {
                                // Clients have a habit of dumping the config back at us, so we only save differing values.
                                if (!value.equals(oldValue)) {
                                    viewFactory.getView(user.getId(), ctx.getContextId()).set("user", propertyName, value);
                                }

                            } else {
                                // Eh...
                            }
                        } catch (final OXException e) {
                            throw new OXException(e);
                        }

                    }

                };
            }

        };

        services.publishService(PreferencesItemService.class, prefItem);

        // And let's publish the metadata as well
        final List<String> metadataNames = property.getMetadataNames();
        for (final String metadataName : metadataNames) {
            final String[] metadataPath = new String[path.length + 2];
            System.arraycopy(path, 0, metadataPath, 1, path.length);
            metadataPath[metadataPath.length - 1] = metadataName;
            metadataPath[0] = METADATA_PREFIX;

            final PreferencesItemService metadataItem = new PreferencesItemService() {

                @Override
                public String[] getPath() {
                    return metadataPath;
                }

                @Override
                public IValueHandler getSharedValue() {
                    return new IValueHandler() {

                        @Override
                        public int getId() {
                            return NO_ID;
                        }

                        @Override
                        public void getValue(final Session session, final Context ctx, final User user, final UserConfiguration userConfig, final Setting setting) throws OXException {
                            try {
                                final ComposedConfigProperty<String> prop =
                                    viewFactory.getView(user.getId(), ctx.getContextId()).property(propertyName, String.class);
                                Object value = prop.get(metadataName);
                                try {
                                    // Let's turn this into a nice object, if it conforms to JSON
                                    value = new JSONObject("{value: " + value + "}").get("value");

                                } catch (final JSONException x) {
                                    // Ah well, let's pretend it's a string.
                                }

                                setting.setSingleValue(value);
                            } catch (final OXException e) {
                                throw new OXException(e);
                            }
                        }

                        @Override
                        public boolean isAvailable(final UserConfiguration userConfig) {
                            return true;
                        }

                        @Override
                        public boolean isWritable() {
                            return false;
                        }

                        @Override
                        public void writeValue(final Session session, final Context ctx, final User user, final Setting setting) throws OXException {
                            // IGNORE
                        }

                    };
                }

            };

            services.publishService(PreferencesItemService.class, metadataItem);
        }

        // Lastly, let's publish configurability.
        final String[] configurablePath = new String[path.length + 2];
        System.arraycopy(path, 0, configurablePath, 1, path.length);
        configurablePath[configurablePath.length - 1] = "configurable";
        configurablePath[0] = METADATA_PREFIX;

        final PreferencesItemService configurableItem = new PreferencesItemService() {

            @Override
            public String[] getPath() {
                return configurablePath;
            }

            @Override
            public IValueHandler getSharedValue() {
                return new IValueHandler() {

                    @Override
                    public int getId() {
                        return NO_ID;
                    }

                    @Override
                    public void getValue(final Session session, final Context ctx, final User user, final UserConfiguration userConfig, final Setting setting) throws OXException {
                        setting.setSingleValue(writable);
                    }

                    @Override
                    public boolean isAvailable(final UserConfiguration userConfig) {
                        return true;
                    }

                    @Override
                    public boolean isWritable() {
                        return false;
                    }

                    @Override
                    public void writeValue(final Session session, final Context ctx, final User user, final Setting setting) throws OXException {
                        // IGNORE
                    }

                };
            }

        };

        services.publishService(PreferencesItemService.class, configurableItem);
    }

    private boolean isPreferenceItem(final ComposedConfigProperty<String> property) throws OXException {
        return property.get(PREFERENCE_PATH) != null;
    }

    private void registerListenerForConfigurationService() {
        serviceTracker =
            new ServiceTracker<ConfigViewFactory, ConfigViewFactory>(context, ConfigViewFactory.class, new ConfigurationTracker(
                context,
                this));
        serviceTracker.open();
    }

    private void unregisterListenerForConfigurationService() {
        serviceTracker.close();
    }

    private static final class ConfigurationTracker implements ServiceTrackerCustomizer<ConfigViewFactory, ConfigViewFactory> {

        private final BundleContext context;

        private final Activator activator;

        public ConfigurationTracker(final BundleContext context, final Activator activator) {
            this.context = context;
            this.activator = activator;

        }

        @Override
        public ConfigViewFactory addingService(final ServiceReference<ConfigViewFactory> serviceReference) {
            final ConfigViewFactory addedService = context.getService(serviceReference);
            activator.handleConfigurationUpdate(addedService);
            return addedService;
        }

        @Override
        public void modifiedService(final ServiceReference<ConfigViewFactory> serviceReference, final ConfigViewFactory o) {
            // IGNORE
        }

        @Override
        public void removedService(final ServiceReference<ConfigViewFactory> serviceReference, final ConfigViewFactory o) {
            context.ungetService(serviceReference);
        }
    }

}
