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

package com.openexchange.file.storage.webdav.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import com.openexchange.file.storage.FileStorageAccountManagerProvider;
import com.openexchange.file.storage.FileStorageException;
import com.openexchange.file.storage.FileStorageService;
import com.openexchange.file.storage.webdav.WebDAVFileStorageService;

/**
 * {@link Registerer}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class Registerer implements EventHandler {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(Registerer.class));

    private final BundleContext context;

    private volatile FileStorageService service;

    private volatile ServiceRegistration registration;

    private volatile int ranking;

    /**
     * Initializes a new {@link Registerer}.
     */
    public Registerer(final BundleContext context) {
        super();
        this.context = context;
    }

    public void handleEvent(final Event event) {
        if (FileStorageAccountManagerProvider.TOPIC.equals(event.getTopic())) {
            final int ranking;
            {
                final Integer rank = (Integer) event.getProperty(FileStorageAccountManagerProvider.PROPERTY_RANKING);
                ranking = null == rank ? 0 : rank.intValue();
            }
            synchronized (this) {
                if (null == service) {
                    /*
                     * Try to create WebDAV service
                     */
                    try {
                        service = WebDAVFileStorageService.newInstance();
                        registration = context.registerService(FileStorageService.class.getName(), service, null);
                        this.ranking = ranking;
                    } catch (final FileStorageException e) {
                        LOG.warn("Registration of \"" + WebDAVFileStorageService.class.getName() + "\" failed.", e);
                    }
                } else {
                    /*
                     * Already created before, but new
                     */
                    if (ranking > this.ranking) {
                        final FileStorageAccountManagerProvider provider =
                            (FileStorageAccountManagerProvider) event.getProperty(FileStorageAccountManagerProvider.PROPERTY_PROVIDER);
                        if (provider.supports(service)) {
                            try {
                                registration.unregister();
                                registration = null;
                                service = WebDAVFileStorageService.newInstance();
                                registration = context.registerService(FileStorageService.class.getName(), service, null);
                                this.ranking = ranking;
                            } catch (final FileStorageException e) {
                                LOG.warn("Registration of \"" + WebDAVFileStorageService.class.getName() + "\" failed.", e);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Closes this registerer.
     */
    public void close() {
        final ServiceRegistration thisReg = registration;
        if (null != thisReg) {
            thisReg.unregister();
            registration = null;
        }
        service = null;
        ranking = 0;
    }

}
