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

package com.openexchange.server.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.groupware.contact.ContactInterface;
import com.openexchange.groupware.contact.ContactInterfaceProvider;
import com.openexchange.groupware.contact.ContactInterfaceProviderRegistry;

/**
 * {@link ContactServiceListener} - The {@link ServiceTrackerCustomizer} for {@link ContactInterface} instances.
 * 
 * @author <a href="mailto:ben.pahne@open-xchange.com">Ben Pahne</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ContactServiceListener implements ServiceTrackerCustomizer {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(ContactServiceListener.class));

    private final BundleContext context;

    /**
     * Initializes a new {@link ContactServiceListener}.
     * 
     * @param context The bundle context
     */
    public ContactServiceListener(final BundleContext context) {
        super();
        this.context = context;
    }

    public Object addingService(final ServiceReference reference) {
        final Object id = reference.getProperty(ContactInterface.OVERRIDE_FOLDER_ATTRIBUTE);
        final Object ctx = reference.getProperty(ContactInterface.OVERRIDE_CONTEXT_ATTRIBUTE);
        if (id != null && ctx != null) {
            final int folderId = Integer.parseInt(id.toString());
            final int contextId = Integer.parseInt(ctx.toString());
            final ContactInterfaceProviderRegistry contactServices = ContactInterfaceProviderRegistry.getInstance();
            if (!contactServices.containsService(folderId, contextId)) {
                final ContactInterfaceProvider provider = (ContactInterfaceProvider) context.getService(reference);
                if (contactServices.addService(folderId, contextId, provider)) {
                    return provider;
                }
                context.ungetService(reference);
            }
        }
        /*
         * Nothing to track
         */
        return null;
    }

    public void modifiedService(final ServiceReference reference, final Object service) {
        // Nothing to do
    }

    public void removedService(final ServiceReference reference, final Object service) {
        if (null != service) {
            try {
                final Object overRiding = reference.getProperty(ContactInterface.OVERRIDE_FOLDER_ATTRIBUTE);
                final Object ctx = reference.getProperty(ContactInterface.OVERRIDE_CONTEXT_ATTRIBUTE);
                if (overRiding != null && ctx != null) {
                    final int folderId = Integer.parseInt(overRiding.toString());
                    final int contextId = Integer.parseInt(ctx.toString());
                    LOG.info(new StringBuilder("Removing Service Bundle Contact Interface Provider: ").append(
                        reference.getBundle().getSymbolicName()).append(" for folder ").append(folderId).append(" and context ").append(
                        contextId));
                    ContactInterfaceProviderRegistry.getInstance().removeService(
                        folderId,
                        contextId,
                        (ContactInterfaceProvider) context.getService(reference));
                }
            } finally {
                context.ungetService(reference);
            }
        }
    }

}
