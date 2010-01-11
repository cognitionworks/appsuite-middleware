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
package com.openexchange.axis2.osgi;

import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.http.HttpService;

import com.openexchange.axis2.internal.Axis2ServletInit;
import com.openexchange.axis2.services.Axis2ServletServiceRegistry;
import com.openexchange.config.ConfigurationService;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.server.osgiservice.ServiceRegistry;

public class Activator extends DeferredActivator {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(Activator.class);

    private final AtomicBoolean started;

    //private ServiceRegistration serviceRegistration;

    /**
     * Initializes a new {@link Axis2ServletActivator}
     */
    public Activator() {
        super();
        started = new AtomicBoolean();
    }

    private static final Class<?>[] NEEDED_SERVICES = { ConfigurationService.class, HttpService.class };

    @Override
    protected Class<?>[] getNeededServices() {
        return NEEDED_SERVICES;
    }


    @Override
    protected void handleAvailability(Class<?> clazz) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Re-available service: " + clazz.getName());
        }
        Axis2ServletServiceRegistry.getServiceRegistry().addService(clazz, getService(clazz));
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        /*
         * Never stop the server even if a needed service is absent
         */
        if (LOG.isWarnEnabled()) {
            LOG.warn("Absent service: " + clazz.getName());
        }
        Axis2ServletServiceRegistry.getServiceRegistry().removeService(clazz);
    }

    @Override
    public void startBundle() throws Exception {
        try {
            /*
             * (Re-)Initialize server service registry with available services
             */
            {
                final ServiceRegistry registry = Axis2ServletServiceRegistry.getServiceRegistry();
                registry.clearRegistry();
                final Class<?>[] classes = getNeededServices();
                for (int i = 0; i < classes.length; i++) {
                    final Object service = getService(classes[i]);
                    if (null != service) {
                        registry.addService(classes[i], service);
                    }
                }
            }
            if (!started.compareAndSet(false, true)) {
                /*
                 * Don't start the server again. A duplicate call to
                 * startBundle() is probably caused by temporary absent
                 * service(s) whose re-availability causes to trigger this
                 * method again.
                 */
                LOG.info("A temporary absent service is available again");
                return;
            }

            Axis2ServletInit.getInstance().start();

            // TODO: ConfigTree may be needed or not...
            //serviceRegistration = context.registerService(PreferencesItemService.class.getName(), new MailFilterPreferencesItem(), null);
        } catch (final Throwable t) {
            LOG.error(t.getMessage(), t);
            throw t instanceof Exception ? (Exception) t : new Exception(t);
        }

    }

    @Override
    public void stopBundle() throws Exception {
        try {
//          if (null != serviceRegistration) {
//          serviceRegistration.unregister();
//          serviceRegistration = null;
//          }
            Axis2ServletInit.getInstance().stop();

            /*
             * Clear service registry
             */
            Axis2ServletServiceRegistry.getServiceRegistry().clearRegistry();
        } catch (final Throwable t) {
            LOG.error(t.getMessage(), t);
            throw t instanceof Exception ? (Exception) t : new Exception(t);
        } finally {
            started.set(false);
        }
    }

}
