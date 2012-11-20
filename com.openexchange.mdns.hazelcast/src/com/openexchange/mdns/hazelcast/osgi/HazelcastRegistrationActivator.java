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

package com.openexchange.mdns.hazelcast.osgi;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.logging.Log;
import org.osgi.framework.BundleException;
import com.openexchange.config.ConfigurationService;
import com.openexchange.mdns.MDNSService;
import com.openexchange.mdns.MDNSServiceInfo;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.threadpool.AbstractTask;
import com.openexchange.threadpool.Task;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.behavior.CallerRunsBehavior;


/**
 * {@link HazelcastRegistrationActivator} - The only purpose is to register a Hazelcast MDNS service that identifies the node as Hazelcast-capable.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class HazelcastRegistrationActivator extends HousekeepingActivator {

    static final Log LOG = com.openexchange.log.Log.loggerFor(HazelcastRegistrationActivator.class);

    private final AtomicReference<MDNSService> mdnsServiceRef;

    volatile MDNSServiceInfo serviceInfo;

    /**
     * Initializes a new {@link HazelcastRegistrationActivator}.
     */
    public HazelcastRegistrationActivator() {
        super();
        mdnsServiceRef = new AtomicReference<MDNSService>();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { MDNSService.class, ThreadPoolService.class, ConfigurationService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        final long st = System.currentTimeMillis();
        final MDNSService service = getService(MDNSService.class);
        if (mdnsServiceRef.compareAndSet(null, service)) {
            final ThreadPoolService poolService = getService(ThreadPoolService.class);
            String name = getService(ConfigurationService.class).getProperty("com.openexchange.cluster.name");
            if (null == name || 0 == name.trim().length()) {
                throw new IllegalStateException(new BundleException(
                    "Cluster name is mandatory. Please set a valid identifier through property \"com.openexchange.cluster.name\".", 
                    BundleException.ACTIVATOR_ERROR));
            }
            Dictionary<?, ?> headers = context.getBundle().getHeaders();
            String bundleVersion = (String)headers.get("Bundle-Version");
            final String serviceID = name + "-v" + bundleVersion;
            final Task<Void> task = new AbstractTask<Void>() {

                @Override
                public Void call() throws Exception {
                    serviceInfo = service.registerService(serviceID, 7001, new StringBuilder(
                            "open-xchange hazelcast service @").append(getHostName()).toString());
                    return null;
                }
            };
            poolService.submit(task, CallerRunsBehavior.<Void> getInstance());
        }
        final long dur = System.currentTimeMillis() - st;
        LOG.info("\n\tBundle \"com.openexchange.mdns.hazelcast\" started in " + dur + "msec.\n");
    }

    @Override
    protected void stopBundle() throws Exception {
        final MDNSService mdnsService = getService(MDNSService.class);
        if (mdnsServiceRef.compareAndSet(mdnsService, null)) {
            final MDNSServiceInfo serviceInfo = this.serviceInfo;
            if (null != serviceInfo) {
                try {
                    mdnsService.unregisterService(serviceInfo);
                } catch (final Exception e) {
                    LOG.error(e.getMessage(), e);
                }
                this.serviceInfo = null;
            }
        }
        super.stopBundle();
    }

    static String getHostName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (final UnknownHostException e) {
            return "<unknown>";
        }
    }

}
