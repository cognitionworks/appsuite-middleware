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

package com.openexchange.oauth.osgi;

import java.util.ArrayList;
import java.util.List;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.exceptions.osgi.ComponentRegistration;
import com.openexchange.oauth.OAuthException;
import com.openexchange.oauth.OAuthService;
import com.openexchange.oauth.exception.OAuthExceptionFactory;
import com.openexchange.oauth.internal.OAuthServiceImpl;

/**
 * {@link OAuthActivator}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class OAuthActivator implements BundleActivator {

    private ComponentRegistration componentRegistration;

    private List<ServiceRegistration> registrations;

    private List<ServiceTracker> trackers;

    /**
     * Initializes a new {@link OAuthActivator}.
     */
    public OAuthActivator() {
        super();
    }

    public void start(final BundleContext context) throws Exception {
        final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(OAuthActivator.class);
        try {
            if (log.isInfoEnabled()) {
                log.info("starting bundle: com.openexchange.oauth");
            }
            /*
             * Register component
             */
            componentRegistration =
                new ComponentRegistration(
                    context,
                    OAuthException.COMPONENT,
                    "com.openexchange.oauth",
                    OAuthExceptionFactory.getInstance());
            /*
             * Collect OAuth services
             */
            final MetaDataRegistry registry = MetaDataRegistry.getInstance();
            registry.start(context);
            /*
             * Start other trackers
             */
            trackers = new ArrayList<ServiceTracker>(4);
            //trackers.add();
            for (final ServiceTracker tracker : trackers) {
                tracker.open();
            }
            /*
             * Register
             */
            registrations = new ArrayList<ServiceRegistration>(2);
            registrations.add(context.registerService(OAuthService.class.getName(), new OAuthServiceImpl(), null));
        } catch (final Exception e) {
            log.error("Starting bundle \"com.openexchange.oauth\" failed.", e);
            throw e;
        }
    }

    public void stop(final BundleContext context) throws Exception {
        final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(OAuthActivator.class);
        try {
            if (log.isInfoEnabled()) {
                log.info("stopping bundle: com.openexchange.oauth");
            }
            MetaDataRegistry.releaseInstance();
            if (null != trackers) {
                while (!trackers.isEmpty()) {
                    trackers.remove(0).close();
                }
                trackers = null;
            }
            if (null != registrations) {
                while (!registrations.isEmpty()) {
                    registrations.remove(0).unregister();
                }
                registrations = null;
            }
            /*
             * Unregister component
             */
            if (null != componentRegistration) {
                componentRegistration.unregister();
                componentRegistration = null;
            }
        } catch (final Exception e) {
            log.error("Stopping bundle \"com.openexchange.oauth\" failed.", e);
            throw e;
        }
    }

}
