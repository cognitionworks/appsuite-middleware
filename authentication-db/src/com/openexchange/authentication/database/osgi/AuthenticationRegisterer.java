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

package com.openexchange.authentication.database.osgi;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.authentication.AuthenticationService;
import com.openexchange.authentication.database.impl.DatabaseAuthentication;
import com.openexchange.context.ContextService;
import com.openexchange.user.UserService;

/**
 * Dependently registers the AuthenticationService.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class AuthenticationRegisterer implements ServiceTrackerCustomizer {

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(AuthenticationRegisterer.class));

    private final BundleContext context;
    private final Lock lock = new ReentrantLock();

    private ServiceRegistration registration;
    private ContextService contextService;
    private UserService userService;

    public AuthenticationRegisterer(BundleContext context) {
        super();
        this.context = context;
    }

    public Object addingService(ServiceReference reference) {
        final Object obj = context.getService(reference);
        final boolean needsRegistration;
        lock.lock();
        try {
            if (obj instanceof ContextService) {
                contextService = (ContextService) obj;
            }
            if (obj instanceof UserService) {
                userService = (UserService) obj;
            }
            needsRegistration = null != contextService && null != userService && registration == null;
        } finally {
            lock.unlock();
        }
        if (needsRegistration) {
            LOG.info("Registering database authentication service.");
            registration = context.registerService(AuthenticationService.class.getName(),
                new DatabaseAuthentication(contextService, userService), null);
        }
        return obj;
    }

    public void modifiedService(ServiceReference reference, Object service) {
        // Nothing to do.
    }

    public void removedService(ServiceReference reference, Object service) {
        ServiceRegistration unregister = null;
        lock.lock();
        try {
            if (service instanceof ContextService) {
                contextService = null;
            }
            if (service instanceof UserService) {
                userService = null;
            }
            if (registration != null && (contextService == null || userService == null)) {
                unregister = registration;
                registration = null;
            }
        } finally {
            lock.unlock();
        }
        if (null != unregister) {
            LOG.info("Unregistering database authentication service.");
            unregister.unregister();
        }
        context.ungetService(reference);
    }
}
