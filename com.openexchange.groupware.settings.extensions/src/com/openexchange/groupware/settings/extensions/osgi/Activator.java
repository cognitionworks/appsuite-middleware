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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.settings.IValueHandler;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.groupware.settings.Setting;
import com.openexchange.groupware.settings.SettingException;
import com.openexchange.groupware.settings.extensions.ServicePublisher;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.session.Session;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigCascadeException;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Francisco Laguna <francisco.laguna@open-xchange.com>
 */
public class Activator implements BundleActivator {

    private static final String PREFERENCE_PATH = "preferencePath";
    private static final String METADATA_PREFIX = "meta";
    private ServicePublisher services;
    private BundleContext context;
    private ServiceTracker serviceTracker;

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(Activator.class));

    public void start(final BundleContext bundleContext) throws Exception {
        services = new OSGiServicePublisher(bundleContext);
        context = bundleContext;
        registerListenerForConfigurationService();
    }

    public void stop(final BundleContext bundleContext) throws Exception {
        unregisterListenerForConfigurationService();
        services.removeAllServices();
    }

    public void handleConfigurationUpdate(final ConfigViewFactory viewFactory) {
        LOG.info("Updating configtree");
        try {
            ConfigView view = viewFactory.getView();
            Map<String, ComposedConfigProperty<String>> all = view.all();
            for(Map.Entry<String,ComposedConfigProperty<String>> entry : all.entrySet()) {
                String propertyName = entry.getKey();
                ComposedConfigProperty<String> property = entry.getValue();
                if (isPreferenceItem(property)) {
                    export(viewFactory, property, propertyName);
                }
            }
        } catch (Throwable x) {
            LOG.error(x.getMessage(), x);
        }
        
        
    }
    
    // Maybe that is an overuse of anonymous inner classes. Better get around to refactoring this at some point.
    
    private void export(final ConfigViewFactory viewFactory, ComposedConfigProperty<String> property, final String propertyName) throws ConfigCascadeException {
        
        final String[] path = property.get(PREFERENCE_PATH).split("/");
        String finalScope = property.get("final");
        String isProtected = property.get("protected");
        final boolean writable = (finalScope == null || finalScope.equals("user")) && (isProtected == null || ! property.get("protected", boolean.class));
        
        
        PreferencesItemService prefItem = new PreferencesItemService() {

            private static final String UNDEFINED_STRING = "undefined";
            
            public String[] getPath() {
                return path;
            }

            public IValueHandler getSharedValue() {
                return new IValueHandler() {

                    public int getId() {
                        return NO_ID;
                    }

                    public void getValue(Session session, Context ctx, User user, UserConfiguration userConfig, Setting setting) throws SettingException {
                        try {
                            Object value = viewFactory.getView(user.getId(), ctx.getContextId()).get(propertyName, String.class);
                            if (UNDEFINED_STRING.equals(value)) {
                                setting.setSingleValue(UNDEFINED);
                                return;
                            }
                            try {
                                // Let's turn this into a nice object, if it conforms to JSON
                                value = new JSONObject("{value: "+value+"}").get("value");

                            } catch (JSONException x) {
                                // Ah well, let's pretend it's a string.
                            }
                            
                            setting.setSingleValue(value);
                        } catch (ConfigCascadeException e) {
                            throw new SettingException(e);
                        }
                    }

                    public boolean isAvailable(UserConfiguration userConfig) {
                        return true;
                    }

                    public boolean isWritable() {
                        return writable;
                    }

                    public void writeValue(Session session, Context ctx, User user, Setting setting) throws SettingException {
                        Object value = setting.getSingleValue();
                        if(value == null) {
                            Object[] multiValue = setting.getMultiValue();
                            if(multiValue != null) {
                                
                                JSONArray arr = new JSONArray();
                                for (Object o : multiValue) {
                                    arr.put(o);
                                }
                                value = arr.toString();
                            } else if (setting.isEmptyMultivalue()) {
                                value = "[]";
                            }
                        }
                        try {
                            String oldValue = viewFactory.getView(user.getId(), ctx.getContextId()).get(propertyName, String.class);
                        if(value != null) {
                                // Clients have a habit of dumping the config back at us, so we only save differing values.
                                if(!value.equals(oldValue)) {
                                    viewFactory.getView(user.getId(), ctx.getContextId()).set("user", propertyName, value);
                                }
                            
                        } else {
                            
                        }} catch (ConfigCascadeException e) {
                            throw new SettingException(e);
                        }
                        
                    }
                    
                };
            }
            
        };
        
        services.publishService(PreferencesItemService.class, prefItem);
        
        // And let's publish the metadata as well
        List<String> metadataNames = property.getMetadataNames();
        for (final String metadataName : metadataNames) {
            final String[] metadataPath = new String[path.length+2];
            System.arraycopy(path, 0, metadataPath, 1, path.length);
            metadataPath[metadataPath.length-1] = metadataName;
            metadataPath[0] = METADATA_PREFIX;
            
            PreferencesItemService metadataItem = new PreferencesItemService() {

                public String[] getPath() {
                    return metadataPath;
                }

                public IValueHandler getSharedValue() {
                    return new IValueHandler() {

                        public int getId() {
                            return NO_ID;
                        }

                        public void getValue(Session session, Context ctx, User user, UserConfiguration userConfig, Setting setting) throws SettingException {
                            try {
                                ComposedConfigProperty<String> prop = viewFactory.getView(user.getId(), ctx.getContextId()).property(propertyName, String.class);
                                Object value = prop.get(metadataName);
                                try {
                                    // Let's turn this into a nice object, if it conforms to JSON
                                    value = new JSONObject("{value: "+value+"}").get("value");

                                } catch (JSONException x) {
                                    // Ah well, let's pretend it's a string.
                                }
                                
                                setting.setSingleValue(value);
                            } catch (ConfigCascadeException e) {
                                throw new SettingException(e);
                            }
                        }

                        public boolean isAvailable(UserConfiguration userConfig) {
                            return true;
                        }

                        public boolean isWritable() {
                            return false;
                        }

                        public void writeValue(Session session, Context ctx, User user, Setting setting) throws SettingException {
                            // IGNORE
                        }
                        
                    };
                }
                
            };
            
            services.publishService(PreferencesItemService.class, metadataItem);
        }
        
        // Lastly, let's publish configurability.
        final String[] configurablePath = new String[path.length+2];
        System.arraycopy(path, 0, configurablePath, 1, path.length);
        configurablePath[configurablePath.length-1] = "configurable";
        configurablePath[0] = METADATA_PREFIX;
        
        
        PreferencesItemService configurableItem = new PreferencesItemService(){

            public String[] getPath() {
                return configurablePath;
            }

            public IValueHandler getSharedValue() {
                return new IValueHandler() {

                    public int getId() {
                        return NO_ID;
                    }

                    public void getValue(Session session, Context ctx, User user, UserConfiguration userConfig, Setting setting) throws SettingException {
                        setting.setSingleValue(writable);
                    }

                    public boolean isAvailable(UserConfiguration userConfig) {
                        return true;
                    }

                    public boolean isWritable() {
                        return false;
                    }

                    public void writeValue(Session session, Context ctx, User user, Setting setting) throws SettingException {
                        // IGNORE
                    }
                    
                };
            }
            
        };
        
        services.publishService(PreferencesItemService.class, configurableItem);
    }

    private boolean isPreferenceItem(ComposedConfigProperty<String> property) throws ConfigCascadeException {
        return property.get(PREFERENCE_PATH) != null;
    }

    private void registerListenerForConfigurationService() {
        serviceTracker = new ServiceTracker(context, ConfigViewFactory.class.getName(), new ConfigurationTracker(context, this));
        serviceTracker.open();
    }

    private void unregisterListenerForConfigurationService() {
        serviceTracker.close();
    }


    private static final class ConfigurationTracker implements ServiceTrackerCustomizer {
        private final BundleContext context;
        private final Activator activator;

        public ConfigurationTracker(final BundleContext context, final Activator activator) {
            this.context = context;
            this.activator = activator;

        }

        public Object addingService(final ServiceReference serviceReference) {
            final Object addedService = context.getService(serviceReference);
            if(ConfigViewFactory.class.isAssignableFrom(addedService.getClass())) {
                activator.handleConfigurationUpdate((ConfigViewFactory) addedService);
            }
            return addedService;
        }

        public void modifiedService(final ServiceReference serviceReference, final Object o) {
            // IGNORE
        }

        public void removedService(final ServiceReference serviceReference, final Object o) {
            context.ungetService(serviceReference);
        }
    }

}
