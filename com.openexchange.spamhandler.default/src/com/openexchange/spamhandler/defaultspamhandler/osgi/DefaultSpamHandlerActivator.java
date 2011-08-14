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

package com.openexchange.spamhandler.defaultspamhandler.osgi;

import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;
import com.openexchange.config.ConfigurationService;
import com.openexchange.mail.service.MailService;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.spamhandler.SpamHandler;
import com.openexchange.spamhandler.defaultspamhandler.ConfigurationServiceSupplier;
import com.openexchange.spamhandler.defaultspamhandler.DefaultSpamHandler;
import com.openexchange.spamhandler.defaultspamhandler.MailServiceSupplier;

/**
 * {@link DefaultSpamHandlerActivator} - {@link BundleActivator Activator} for default spam handler bundle.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class DefaultSpamHandlerActivator extends DeferredActivator {

    private static final org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(DefaultSpamHandlerActivator.class));

    private final Dictionary<String, String> dictionary;

    private ServiceRegistration<SpamHandler> serviceRegistration;

    /**
     * Initializes a new {@link DefaultSpamHandlerActivator}
     */
    public DefaultSpamHandlerActivator() {
        super();
        dictionary = new Hashtable<String, String>();
        dictionary.put("name", DefaultSpamHandler.getInstance().getSpamHandlerName());
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { MailService.class, ConfigurationService.class };
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        if (MailService.class.equals(clazz)) {
            MailServiceSupplier.getInstance().setMailService(null);
        }
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        if (MailService.class.equals(clazz)) {
            MailServiceSupplier.getInstance().setMailService(getService(MailService.class));
        }
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            MailServiceSupplier.getInstance().setMailService(getService(MailService.class));
            ConfigurationServiceSupplier.getInstance().setConfigurationService(getService(ConfigurationService.class));
            serviceRegistration = context.registerService(SpamHandler.class, DefaultSpamHandler.getInstance(), dictionary);
        } catch (final Throwable t) {
            LOG.error(t.getMessage(), t);
            throw t instanceof Exception ? (Exception) t : new Exception(t);
        }

    }

    @Override
    protected void stopBundle() throws Exception {
        try {
            if (null != serviceRegistration) {
                serviceRegistration.unregister();
                serviceRegistration = null;
            }
            MailServiceSupplier.getInstance().setMailService(null);
            ConfigurationServiceSupplier.getInstance().setConfigurationService(null);
        } catch (final Throwable t) {
            LOG.error(t.getMessage(), t);
            throw t instanceof Exception ? (Exception) t : new Exception(t);
        }
    }

}
