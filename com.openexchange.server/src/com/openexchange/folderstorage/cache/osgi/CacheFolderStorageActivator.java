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

package com.openexchange.folderstorage.cache.osgi;

import static com.openexchange.folderstorage.cache.CacheServiceRegistry.getServiceRegistry;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.caching.CacheService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.FolderEventConstants;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.cache.CacheFolderStorage;
import com.openexchange.folderstorage.cache.lock.LockManagement;
import com.openexchange.folderstorage.cache.memory.FolderMapManagement;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.push.PushEventConstants;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.server.osgiservice.ServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondEventConstants;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.threadpool.ThreadPoolService;

/**
 * {@link CacheFolderStorageActivator} - {@link BundleActivator Activator} for cache folder storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CacheFolderStorageActivator extends DeferredActivator {

    static final org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(CacheFolderStorageActivator.class));

    private List<ServiceRegistration<?>> registrations;

    private CacheFolderStorage cacheFolderStorage;

    private List<ServiceTracker<?,?>> serviceTrackers;

    /**
     * Initializes a new {@link CacheFolderStorageActivator}.
     */
    public CacheFolderStorageActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { CacheService.class, ThreadPoolService.class, ConfigurationService.class, SessiondService.class };
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Re-available service: " + clazz.getName());
        }
        getServiceRegistry().addService(clazz, getService(clazz));
        if (CacheService.class.equals(clazz)) {
            try {
                initCacheFolderStorage();
            } catch (final OXException e) {
                LOG.error(e.getMessage(), e);
                unregisterCacheFolderStorage();
            }
        }
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        if (LOG.isWarnEnabled()) {
            LOG.warn("Absent service: " + clazz.getName());
        }
        if (CacheService.class.equals(clazz)) {
            try {
                disposeCacheFolderStorage();
            } catch (final OXException e) {
                LOG.error(e.getMessage(), e);
                unregisterCacheFolderStorage();
            }
        }
        getServiceRegistry().removeService(clazz);
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            /*
             * (Re-)Initialize service registry with available services
             */
            {
                final ServiceRegistry registry = getServiceRegistry();
                registry.clearRegistry();
                final Class<?>[] classes = getNeededServices();
                for (final Class<?> classe : classes) {
                    final Object service = getService(classe);
                    if (null != service) {
                        registry.addService(classe, service);
                    }
                }
            }
            initCacheFolderStorage();
            // Register service trackers
            serviceTrackers = new ArrayList<ServiceTracker<?,?>>(4);
            serviceTrackers.add(new ServiceTracker<FolderStorage,FolderStorage>(context, FolderStorage.class, new CacheFolderStorageServiceTracker(context)));
            for (final ServiceTracker<?,?> serviceTracker : serviceTrackers) {
                serviceTracker.open();
            }
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        try {
            // Drop service trackers
            if (null != serviceTrackers) {
                for (final ServiceTracker<?,?> serviceTracker : serviceTrackers) {
                    serviceTracker.close();
                }
                serviceTrackers.clear();
                serviceTrackers = null;
            }
            disposeCacheFolderStorage();
            /*
             * Clear service registry
             */
            getServiceRegistry().clearRegistry();
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    private void disposeCacheFolderStorage() throws OXException {
        // Unregister folder storage
        unregisterCacheFolderStorage();
        // Shut-down folder storage
        if (null != cacheFolderStorage) {
            cacheFolderStorage.onCacheAbsent();
            cacheFolderStorage = null;
        }
    }

    private void initCacheFolderStorage() throws OXException {
        // Start-up folder storage
        cacheFolderStorage = new CacheFolderStorage();
        cacheFolderStorage.onCacheAvailable();
        // Register folder storage
        final Dictionary<String, String> dictionary = new Hashtable<String, String>();
        dictionary.put("tree", FolderStorage.ALL_TREE_ID);
        registrations = new ArrayList<ServiceRegistration<?>>(4);
        registrations.add(context.registerService(FolderStorage.class, cacheFolderStorage, dictionary));
        // Register event handler for content-related changes on a mail folder
        final CacheFolderStorage tmp = cacheFolderStorage;
        {
            final EventHandler eventHandler = new EventHandler() {

                @Override
                public void handleEvent(final Event event) {
                    final Session session = ((Session) event.getProperty(PushEventConstants.PROPERTY_SESSION));
                    final String folderId = (String) event.getProperty(PushEventConstants.PROPERTY_FOLDER);
                    final Boolean contentRelated = (Boolean) event.getProperty(PushEventConstants.PROPERTY_CONTENT_RELATED);
                    try {
                        tmp.removeFromCache(sanitizeFolderId(folderId), FolderStorage.REAL_TREE_ID, null != contentRelated && contentRelated.booleanValue(), session);
                    } catch (final OXException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            };
            final Dictionary<String, Object> dict = new Hashtable<String, Object>(1);
            dict.put(EventConstants.EVENT_TOPIC, PushEventConstants.getAllTopics());
            registrations.add(context.registerService(EventHandler.class, eventHandler, dict));
        }
        {
            final EventHandler eventHandler = new EventHandler() {

                @Override
                public void handleEvent(final Event event) {
                    final Session session = ((Session) event.getProperty(FolderEventConstants.PROPERTY_SESSION));
                    final Integer contextId = ((Integer) event.getProperty(FolderEventConstants.PROPERTY_CONTEXT));
                    final Integer userId = ((Integer) event.getProperty(FolderEventConstants.PROPERTY_USER));
                    final String folderId = (String) event.getProperty(FolderEventConstants.PROPERTY_FOLDER);
                    final Boolean contentRelated = (Boolean) event.getProperty(FolderEventConstants.PROPERTY_CONTENT_RELATED);
                    try {
                        if (null == session) {
                            tmp.removeSingleFromCache(sanitizeFolderId(folderId), FolderStorage.REAL_TREE_ID, null == userId ? -1 : userId.intValue(), contextId.intValue(), true);
                        } else {
                            tmp.removeFromCache(sanitizeFolderId(folderId), FolderStorage.REAL_TREE_ID, null != contentRelated && contentRelated.booleanValue(), session);
                        }
                    } catch (final OXException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            };
            final Dictionary<String, Object> dict = new Hashtable<String, Object>(1);
            dict.put(EventConstants.EVENT_TOPIC, FolderEventConstants.getAllTopics());
            registrations.add(context.registerService(EventHandler.class, eventHandler, dict));
        }
        {
            final EventHandler eventHandler = new EventHandler() {

                @Override
                public void handleEvent(final Event event) {
                    final String topic = event.getTopic();
                    if (SessiondEventConstants.TOPIC_REMOVE_SESSION.equals(topic)) {
                        handleDroppedSession((Session) event.getProperty(SessiondEventConstants.PROP_SESSION));
                    } else if (SessiondEventConstants.TOPIC_REMOVE_CONTAINER.equals(topic) || SessiondEventConstants.TOPIC_REMOVE_DATA.equals(topic)) {
                        @SuppressWarnings("unchecked")
                        final Map<String, Session> map = (Map<String, Session>) event.getProperty(SessiondEventConstants.PROP_CONTAINER);
                        for (final Session session : map.values()) {
                            handleDroppedSession(session);
                        }
                    }
                }

                private void handleDroppedSession(final Session session) {
                    if (null == getService(SessiondService.class).getAnyActiveSessionForUser(session.getUserId(), session.getContextId())) {
                        FolderMapManagement.getInstance().dropFor(session);
                        LockManagement.getInstance().dropFor(session);
                    }
                }
            };
            final Dictionary<String, Object> dict = new Hashtable<String, Object>(1);
            dict.put(EventConstants.EVENT_TOPIC, SessiondEventConstants.getAllTopics());
            registrations.add(context.registerService(EventHandler.class, eventHandler, dict));
        }
    }

    private static final String DEFAULT = MailFolder.DEFAULT_FOLDER_ID;

    private static final Pattern PAT_FIX = Pattern.compile(Pattern.quote(DEFAULT) + "[0-9]+" + Pattern.quote(DEFAULT));

    protected static String sanitizeFolderId(final String id) {
        String fid = id;
        if (fid.startsWith(DEFAULT)) {
            final Matcher matcher = PAT_FIX.matcher(fid);
            if (matcher.matches()) {
                fid = DEFAULT + matcher.group(1);
            }
        }
        return fid;
    }

    private void unregisterCacheFolderStorage() {
        // Unregister
        if (null != registrations) {
            while (!registrations.isEmpty()) {
                registrations.remove(0).unregister();
            }
            registrations = null;
        }
    }

}
