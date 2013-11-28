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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.mail.smal.impl.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.exception.OXException;
import com.openexchange.mail.Protocol;
import com.openexchange.mail.api.MailProvider;
import com.openexchange.mail.smal.impl.SmalMailProviderRegistry;

/**
 * Service tracker for mail providers
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SmalProviderServiceTracker implements ServiceTrackerCustomizer<MailProvider, MailProvider> {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(SmalProviderServiceTracker.class);

    private final BundleContext context;

    /**
     * Initializes a new {@link SmalProviderServiceTracker}
     */
    public SmalProviderServiceTracker(final BundleContext context) {
        super();
        this.context = context;
    }

    @Override
    public MailProvider addingService(final ServiceReference<MailProvider> reference) {
        final MailProvider addedService = context.getService(reference);
        final Object protocol = reference.getProperty("protocol");
        if (null == protocol) {
            LOG.error("Missing protocol in mail provider service: " + addedService.getClass().getName());
            context.ungetService(reference);
            return null;
        }
        try {
            if (SmalMailProviderRegistry.registerMailProvider(protocol.toString(), addedService)) {
                LOG.info(new StringBuilder(64).append("Mail provider for protocol '").append(protocol.toString()).append(
                    "' successfully registered in SMAL registry.").toString());
            } else {
                if (!Protocol.ALL.equals(protocol.toString())) {
                    LOG.warn(new StringBuilder(64).append("Mail provider for protocol '").append(protocol.toString()).append(
                        "' could not be added to SMAL registry.").append(" Another provider which supports the protocol has already been registered.").toString());
                    context.ungetService(reference);
                }
                context.ungetService(reference);
                return null;
            }
        } catch (final OXException e) {
            LOG.error(e.getMessage(), e);
            context.ungetService(reference);
            return null;
        }
        return addedService;
    }

    @Override
    public void modifiedService(final ServiceReference<MailProvider> reference, final MailProvider service) {
        // Nothing to do
    }

    @Override
    public void removedService(final ServiceReference<MailProvider> reference, final MailProvider service) {
        if (null != service) {
            try {
                final MailProvider provider = service;
                SmalMailProviderRegistry.unregisterMailProvider(provider);
                LOG.info(new StringBuilder(64).append("Mail provider for protocol '").append(provider.getProtocol().toString()).append(
                    "' successfully unregistered from SMAL registry.").toString());
            } finally {
                context.ungetService(reference);
            }
        }
    }

}
