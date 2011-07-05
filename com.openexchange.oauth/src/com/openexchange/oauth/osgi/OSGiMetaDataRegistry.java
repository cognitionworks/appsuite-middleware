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

package com.openexchange.oauth.osgi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.exception.OXException;
import com.openexchange.oauth.OAuthExceptionCodes;
import com.openexchange.oauth.OAuthServiceMetaData;
import com.openexchange.oauth.OAuthServiceMetaDataRegistry;

/**
 * {@link OSGiMetaDataRegistry}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class OSGiMetaDataRegistry implements OAuthServiceMetaDataRegistry {

    private static volatile OSGiMetaDataRegistry instance;

    /**
     * Gets the registry instance.
     * 
     * @return The instance
     */
    public static OSGiMetaDataRegistry getInstance() {
        OSGiMetaDataRegistry tmp = instance;
        if (null == tmp) {
            synchronized (OSGiMetaDataRegistry.class) {
                tmp = instance;
                if (null == tmp) {
                    instance = tmp = new OSGiMetaDataRegistry();
                }
            }
        }
        return tmp;
    }

    /**
     * Releases the instance.
     */
    public static void releaseInstance() {
        synchronized (OSGiMetaDataRegistry.class) {
            final OSGiMetaDataRegistry tmp = instance;
            if (null != tmp) {
                tmp.stop();
                tmp.map.clear();
                instance = null;
            }
        }
    }

    final ConcurrentMap<String, OAuthServiceMetaData> map;

    private ServiceTracker tracker;

    /**
     * Initializes a new {@link OSGiMetaDataRegistry}.
     */
    public OSGiMetaDataRegistry() {
        super();
        map = new ConcurrentHashMap<String, OAuthServiceMetaData>();
    }

    public List<OAuthServiceMetaData> getAllServices() {
        return new ArrayList<OAuthServiceMetaData>(map.values());
    }

    public OAuthServiceMetaData getService(final String id) throws OXException {
        final OAuthServiceMetaData service = map.get(id);
        if (null == service) {
            throw OAuthExceptionCodes.UNKNOWN_OAUTH_SERVICE_META_DATA.create(id);
        }
        return service;
    }

    public boolean containsService(final String id) {
        return null == id ? false : map.containsKey(id);
    }

    /**
     * Starts the tracker.
     * 
     * @param context The bundle context
     */
    void start(final BundleContext context) {
        if (null == tracker) {
            tracker = new ServiceTracker(context, OAuthServiceMetaData.class.getName(), new Customizer(context));
            tracker.open();
        }
    }

    /**
     * Stops the tracker.
     */
    private void stop() {
        if (null != tracker) {
            tracker.close();
            tracker = null;
        }
    }

    private final class Customizer implements ServiceTrackerCustomizer {

        private final BundleContext context;

        /**
         * Initializes a new {@link Customizer}.
         */
        public Customizer(final BundleContext context) {
            super();
            this.context = context;
        }

        public Object addingService(final ServiceReference reference) {
            final Object service = context.getService(reference);
            if ((service instanceof OAuthServiceMetaData)) {
                final OAuthServiceMetaData addMe = (OAuthServiceMetaData) service;
                if (null == map.putIfAbsent(addMe.getId(), addMe)) {
                    return service;
                }
                final org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory.getLog(Customizer.class);
                if (logger.isWarnEnabled()) {
                    logger.warn(new StringBuilder(128).append("OAuth service meta data ").append(addMe.getDisplayName()).append(
                        " could not be added to registry. Another service meta data is already registered with identifier: ").append(
                        addMe.getId()).toString());
                }
            }
            /*
             * Adding to registry failed
             */
            context.ungetService(reference);
            return null;
        }

        public void modifiedService(final ServiceReference reference, final Object service) {
            // Nothing to do
        }

        public void removedService(final ServiceReference reference, final Object service) {
            if (null != service) {
                try {
                    if (service instanceof OAuthServiceMetaData) {
                        final OAuthServiceMetaData removeMe = (OAuthServiceMetaData) service;
                        map.remove(removeMe.getId());
                    }
                } finally {
                    context.ungetService(reference);
                }
            }
        }

    }

}
