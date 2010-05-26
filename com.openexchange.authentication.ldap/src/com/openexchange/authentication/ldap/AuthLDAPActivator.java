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

package com.openexchange.authentication.ldap;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceRegistration;

import com.openexchange.authentication.AuthenticationService;
import com.openexchange.authentication.LoginException;
import com.openexchange.config.ConfigurationService;
import com.openexchange.server.osgiservice.DeferredActivator;

/**
 * {@link AuthLDAPActivator}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class AuthLDAPActivator extends DeferredActivator {
	
	private static final Log LOG = LogFactory.getLog(AuthLDAPActivator.class);

	private ServiceRegistration registration;

	public AuthLDAPActivator() {
	    super();
	}

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigurationService.class };
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        // Nothing to do.
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        // Nothing to do.
    }

    /**
     * {@inheritDoc}
     * @throws LoginException if the authentication class can not be initialized.
     */
    @Override
    protected void startBundle() throws LoginException {
        LOG.info("Starting ldap authentication service.");
        final ConfigurationService config = getService(ConfigurationService.class);
        final Properties props = config.getFile("ldapauth.properties");

        if (null == registration) {
            final LDAPAuthentication impl = new LDAPAuthentication(props);
            registration = context.registerService(AuthenticationService.class
                .getName(), impl, null);
        } else {
            LOG.error("Duplicate startup of deferred activator.");
        }
    }

    @Override
    protected void stopBundle() {
        LOG.info("Stopping ldap authentication service.");
        if (null != registration) {
            registration.unregister();
            registration = null;
        }
    }
}
