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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.quota.osgi;

import org.apache.commons.logging.Log;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.cascade.ConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.quota.AmountOnlyQuota;
import com.openexchange.quota.Quota;
import com.openexchange.quota.QuotaRestriction;
import com.openexchange.quota.QuotaService;
import com.openexchange.quota.Resource;
import com.openexchange.quota.ResourceDescription;
import com.openexchange.quota.ServiceProvider;
import com.openexchange.quota.UnlimitedQuota;
import com.openexchange.quota.internal.QuotaServiceImpl;
import com.openexchange.session.Session;

/**
 * {@link QuotaActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class QuotaActivator extends HousekeepingActivator {

    private static final Log LOG = com.openexchange.log.Log.loggerFor(QuotaActivator.class);

    /**
     * Initializes a new {@link QuotaActivator}.
     */
    public QuotaActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigurationService.class, ConfigViewFactory.class };
    }

    @Override
    protected void startBundle() throws Exception {
        final QuotaServiceImpl impl = new QuotaServiceImpl(this);
        registerService(QuotaService.class, impl);
        final BundleContext context = this.context;
        track(QuotaRestriction.class, new ServiceTrackerCustomizer<QuotaRestriction, QuotaRestriction>() {

            @Override
            public QuotaRestriction addingService(final ServiceReference<QuotaRestriction> reference) {
                final QuotaRestriction restriction = context.getService(reference);
                if (impl.addQuotaRestriction(restriction)) {
                    return restriction;
                }
                context.ungetService(reference);
                return null;
            }

            @Override
            public void modifiedService(final ServiceReference<QuotaRestriction> reference, final QuotaRestriction service) {
                // Ignore
            }

            @Override
            public void removedService(final ServiceReference<QuotaRestriction> reference, final QuotaRestriction service) {
                context.ungetService(reference);
            }
        });
        openTrackers();
        // Register pre-defined quota restrictions
        final Log log = LOG;
        registerService(QuotaRestriction.class, new QuotaRestriction() {

            @Override
            public Resource getResource() {
                return Resource.CALENDAR;
            }

            @Override
            public Quota getQuota(final Resource resource, final ResourceDescription desc, final Session session, final ServiceProvider serviceProvider) throws OXException {
                final ConfigView configView =
                    serviceProvider.getService(ConfigViewFactory.class).getView(session.getUserId(), session.getContextId());
                // Get property; first with "context" scope...
                ConfigProperty<String> property = configView.property("context", "com.openexchange.quota.calendar", String.class);
                if (!property.isDefined()) {
                    // ... then with "server" scope if not defined
                    property = configView.property("server", "com.openexchange.quota.calendar", String.class);
                    if (!property.isDefined()) {
                        return UnlimitedQuota.getInstance();
                    }
                }
                try {
                    return new AmountOnlyQuota(Long.parseLong(property.get().trim()));
                } catch (final RuntimeException e) {
                    log.warn(
                        "Couldn't detect quota for " + resource.toString() + " (user=" + session.getUserId() + ", context=" + session.getContextId() + ")",
                        e);
                    return UnlimitedQuota.getInstance();
                }
            }

            @Override
            public Class<?>[] getNeededServices() {
                return new Class<?>[] { ConfigurationService.class, ConfigViewFactory.class };
            }
        });
        registerService(QuotaRestriction.class, new QuotaRestriction() {

            @Override
            public Resource getResource() {
                return Resource.TASK;
            }

            @Override
            public Quota getQuota(final Resource resource, final ResourceDescription desc, final Session session, final ServiceProvider serviceProvider) throws OXException {
                final ConfigView configView =
                    serviceProvider.getService(ConfigViewFactory.class).getView(session.getUserId(), session.getContextId());
                // Get property; first with "context" scope...
                ConfigProperty<String> property = configView.property("context", "com.openexchange.quota.task", String.class);
                if (!property.isDefined()) {
                    // ... then with "server" scope if not defined
                    property = configView.property("server", "com.openexchange.quota.task", String.class);
                    if (!property.isDefined()) {
                        return UnlimitedQuota.getInstance();
                    }
                }
                try {
                    return new AmountOnlyQuota(Long.parseLong(property.get().trim()));
                } catch (final RuntimeException e) {
                    log.warn(
                        "Couldn't detect quota for " + resource.toString() + " (user=" + session.getUserId() + ", context=" + session.getContextId() + ")",
                        e);
                    return UnlimitedQuota.getInstance();
                }
            }

            @Override
            public Class<?>[] getNeededServices() {
                return new Class<?>[] { ConfigurationService.class, ConfigViewFactory.class };
            }
        });
        registerService(QuotaRestriction.class, new QuotaRestriction() {

            @Override
            public Resource getResource() {
                return Resource.CONTACT;
            }

            @Override
            public Quota getQuota(final Resource resource, final ResourceDescription desc, final Session session, final ServiceProvider serviceProvider) throws OXException {
                final ConfigView configView =
                    serviceProvider.getService(ConfigViewFactory.class).getView(session.getUserId(), session.getContextId());
                // Get property; first with "context" scope...
                ConfigProperty<String> property = configView.property("context", "com.openexchange.quota.contact", String.class);
                if (!property.isDefined()) {
                    // ... then with "server" scope if not defined
                    property = configView.property("server", "com.openexchange.quota.contact", String.class);
                    if (!property.isDefined()) {
                        return UnlimitedQuota.getInstance();
                    }
                }
                try {
                    return new AmountOnlyQuota(Long.parseLong(property.get().trim()));
                } catch (final RuntimeException e) {
                    log.warn(
                        "Couldn't detect quota for " + resource.toString() + " (user=" + session.getUserId() + ", context=" + session.getContextId() + ")",
                        e);
                    return UnlimitedQuota.getInstance();
                }
            }

            @Override
            public Class<?>[] getNeededServices() {
                return new Class<?>[] { ConfigurationService.class, ConfigViewFactory.class };
            }
        });
    }

    @Override
    public <S> ServiceTracker<S, S> trackService(final Class<S> clazz) {
        return super.trackService(clazz);
    }

}
