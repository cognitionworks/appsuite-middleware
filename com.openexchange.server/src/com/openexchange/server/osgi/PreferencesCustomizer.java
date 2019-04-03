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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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
import org.slf4j.Logger;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.groupware.settings.impl.ConfigTree;

/**
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class PreferencesCustomizer implements ServiceTrackerCustomizer<PreferencesItemService,PreferencesItemService> {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(PreferencesCustomizer.class);
    }

    private final BundleContext context;

    public PreferencesCustomizer(final BundleContext context) {
        super();
        this.context = context;
    }

    @Override
    public PreferencesItemService addingService(final ServiceReference<PreferencesItemService> reference) {
        final PreferencesItemService preferencesItem = context.getService(reference);
        try {
            ConfigTree.getInstance().addPreferencesItem(preferencesItem);
            return preferencesItem;
        } catch (OXException e) {
            Object arg = new Object() {
                @Override
                public String toString() {
                    String[] path = preferencesItem.getPath();
                    int length = path.length;
                    if (length <= 0) {
                        return "";
                    }

                    StringBuilder sb = new StringBuilder(length << 2);
                    sb.append(path[0]);
                    for (int i = 1; i < length; i++) {
                        sb.append('/').append(path[i]);
                    }
                    return sb.toString();
                }
            };
            LoggerHolder.LOG.error("Can't add service for preferences item. Path: {}", arg, e);
        }
        return null;
    }

    @Override
    public void modifiedService(final ServiceReference<PreferencesItemService> reference, final PreferencesItemService preferencesItem) {
        // Nothing to do.
    }

    @Override
    public void removedService(final ServiceReference<PreferencesItemService> reference, final PreferencesItemService preferencesItem) {
        ConfigTree.getInstance().removePreferencesItem(preferencesItem);
        context.ungetService(reference);
    }
}
