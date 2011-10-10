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

package com.openexchange.imap.osgi;

import static com.openexchange.imap.services.IMAPServiceRegistry.getServiceRegistry;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import org.osgi.framework.BundleActivator;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import com.openexchange.caching.CacheService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.database.DatabaseService;
import com.openexchange.imap.IMAPProvider;
import com.openexchange.imap.cache.ListLsubCache;
import com.openexchange.imap.config.IMAPProperties;
import com.openexchange.imap.notify.IMAPNotifierRegistryService;
import com.openexchange.imap.notify.internal.IMAPNotifierRegistry;
import com.openexchange.imap.services.IMAPServiceRegistry;
import com.openexchange.mail.api.MailProvider;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.secret.SecretService;
import com.openexchange.secret.osgi.tools.WhiteboardSecretService;
import com.openexchange.server.osgiservice.HousekeepingActivator;
import com.openexchange.server.osgiservice.ServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondEventConstants;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.textxtraction.TextXtractService;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.timer.TimerService;
import com.openexchange.user.UserService;

/**
 * {@link IMAPActivator} - The {@link BundleActivator activator} for IMAP bundle.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class IMAPActivator extends HousekeepingActivator {

    protected static final org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(IMAPActivator.class));

    private WhiteboardSecretService secretService;

    /**
     * Initializes a new {@link IMAPActivator}
     */
    public IMAPActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] {
            ConfigurationService.class, CacheService.class, UserService.class, MailAccountStorageService.class, ThreadPoolService.class,
            TimerService.class, SessiondService.class, DatabaseService.class, TextXtractService.class };
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        /*
         * Never stop the server even if a needed service is absent
         */
        if (LOG.isWarnEnabled()) {
            LOG.warn("Absent service: " + clazz.getName());
        }
        getServiceRegistry().removeService(clazz);
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Re-available service: " + clazz.getName());
        }
        getServiceRegistry().addService(clazz, getService(clazz));
    }

    @Override
    public void startBundle() throws Exception {
        try {
            /*
             * (Re-)Initialize service registry with available services
             */
            {
                final ServiceRegistry registry = getServiceRegistry();
                registry.clearRegistry();
                final Class<?>[] classes = getNeededServices();
                for (int i = 0; i < classes.length; i++) {
                    final Object service = getService(classes[i]);
                    if (null != service) {
                        registry.addService(classes[i], service);
                    }
                }

                registry.addService(SecretService.class, secretService = new WhiteboardSecretService(context));
                secretService.open();
            }
            /*
             * Register IMAP mail provider
             */
            final Dictionary<String, String> dictionary = new Hashtable<String, String>(1);
            dictionary.put("protocol", IMAPProvider.PROTOCOL_IMAP.toString());
            registerService(MailProvider.class, IMAPProvider.getInstance(), dictionary);
            if (IMAPProperties.getInstance().notifyRecent()) {
                registerService(IMAPNotifierRegistryService.class, IMAPNotifierRegistry.getInstance());
            }
            /*
             * Register event handle
             */
            {
                final Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>(1);
                serviceProperties.put(EventConstants.EVENT_TOPIC, SessiondEventConstants.getAllTopics());
                registerService(EventHandler.class, new EventHandler() {

                    @Override
                    public void handleEvent(final Event event) {
                        final String topic = event.getTopic();
                        if (SessiondEventConstants.TOPIC_REMOVE_DATA.equals(topic)) {
                            @SuppressWarnings("unchecked") final Map<String, Session> container =
                                (Map<String, Session>) event.getProperty(SessiondEventConstants.PROP_CONTAINER);
                            final IMAPNotifierRegistryService notifierRegistry = IMAPNotifierRegistry.getInstance();
                            for (final Session session : container.values()) {
                                handleSession(session);
                                notifierRegistry.handleRemovedSession(session);
                            }
                        } else if (SessiondEventConstants.TOPIC_REMOVE_SESSION.equals(topic)) {
                            final Session session = (Session) event.getProperty(SessiondEventConstants.PROP_SESSION);
                            handleSession(session);
                            IMAPNotifierRegistry.getInstance().handleRemovedSession(session);
                        } else if (SessiondEventConstants.TOPIC_REMOVE_CONTAINER.equals(topic)) {
                            @SuppressWarnings("unchecked") final Map<String, Session> container =
                                (Map<String, Session>) event.getProperty(SessiondEventConstants.PROP_CONTAINER);
                            final IMAPNotifierRegistryService notifierRegistry = IMAPNotifierRegistry.getInstance();
                            for (final Session session : container.values()) {
                                handleSession(session);
                                notifierRegistry.handleRemovedSession(session);
                            }
                        }
                    }

                    private void handleSession(final Session session) {
                        try {
                            final SessiondService service = IMAPServiceRegistry.getService(SessiondService.class);
                            if (null != service && service.getAnyActiveSessionForUser(session.getUserId(), session.getContextId()) == null) {
                                ListLsubCache.dropFor(session);
                            }
                        } catch (final Exception e) {
                            // Failed handling session
                            LOG.warn("Failed handling tracked removed session for LIST/LSUB cache.", e);
                        }
                    }

                },
                    serviceProperties);
            }
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void stopBundle() throws Exception {
        try {
            cleanUp();
            /*
             * Clear service registry
             */
            getServiceRegistry().clearRegistry();
            if (secretService != null) {
                secretService.close();
                secretService = null;
            }
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

}
