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

package com.openexchange.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import com.openexchange.authentication.exception.LoginExceptionFactory;
import com.openexchange.exceptions.ComponentRegistry;
import com.openexchange.exceptions.impl.ComponentRegistryImpl;
import com.openexchange.exceptions.osgi.ComponentRegistration;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.id.IDException;
import com.openexchange.id.exception.IDExceptionFactory;
import com.openexchange.sessiond.exception.SessionExceptionFactory;

/**
 * {@link GlobalActivator} - Activator for global (aka kernel) bundle
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class GlobalActivator implements BundleActivator {

    private static final Log LOG = LogFactory.getLog(GlobalActivator.class);

    private ServiceRegistration componentRegistryRegistration;

    private ComponentRegistration loginComponent;

    private ComponentRegistration sessionComponent;

    private Initialization initialization;

    private ComponentRegistration idComponent;

    /**
     * Initializes a new {@link GlobalActivator}
     */
    public GlobalActivator() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public void start(final BundleContext context) throws Exception {
        try {
            initialization = new com.openexchange.server.ServerInitialization();
            initialization.start();
            ServiceHolderInit.getInstance().start();
            componentRegistryRegistration = context.registerService(ComponentRegistry.class.getName(), new ComponentRegistryImpl(), null);
            loginComponent = new ComponentRegistration(
                context,
                EnumComponent.LOGIN,
                "com.openexchange.authentication",
                LoginExceptionFactory.getInstance());
            sessionComponent = new ComponentRegistration(context, EnumComponent.SESSION, "com.openexchange.sessiond", SessionExceptionFactory.getInstance());
            idComponent = new ComponentRegistration(context, IDException.COMPONENT, "com.openexchange.id", IDExceptionFactory.getInstance());
            LOG.debug("Global bundle successfully started");
        } catch (final Throwable t) {
            LOG.error(t.getMessage(), t);
            throw t instanceof Exception ? (Exception) t : new Exception(t.getMessage(), t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop(final BundleContext context) throws Exception {
        try {
            idComponent.unregister();
            idComponent = null;
            sessionComponent.unregister();
            sessionComponent = null;
            loginComponent.unregister();
            loginComponent = null;
            componentRegistryRegistration.unregister();
            ServiceHolderInit.getInstance().stop();
            initialization.stop();
            initialization = null;
            LOG.debug("Global bundle successfully stopped");
        } catch (final Throwable t) {
            LOG.error(t.getMessage(), t);
            throw t instanceof Exception ? (Exception) t : new Exception(t.getMessage(), t);
        }
    }
}
