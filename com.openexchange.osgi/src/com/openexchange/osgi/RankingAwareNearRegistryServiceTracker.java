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

package com.openexchange.osgi;

import static com.openexchange.osgi.util.RankedService.getRanking;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.java.SortableConcurrentList;
import com.openexchange.osgi.util.RankedService;

/**
 * {@link RankingAwareNearRegistryServiceTracker} - A {@link NearRegistryServiceTracker} that sorts tracked services by their ranking
 * (highest ranking first).
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RankingAwareNearRegistryServiceTracker<S> extends ServiceTracker<S, S> implements ServiceListing<S> {

    private final SortableConcurrentList<RankedService<S>> services;
    private final int defaultRanking;
    private volatile boolean empty;

    /**
     * Initializes a new {@link RankingAwareNearRegistryServiceTracker} with <tt>0</tt> (zero) as default ranking.
     *
     * @param context The bundle context
     * @param clazz The service's class
     */
    public RankingAwareNearRegistryServiceTracker(final BundleContext context, final Class<S> clazz) {
        this(context, clazz, 0);
    }

    /**
     * Initializes a new {@link RankingAwareNearRegistryServiceTracker}.
     *
     * @param context The bundle context
     * @param clazz The service's class
     * @param defaultRanking The default ranking
     */
    public RankingAwareNearRegistryServiceTracker(final BundleContext context, final Class<S> clazz, final int defaultRanking) {
        super(context, clazz, null);
        services = new SortableConcurrentList<RankedService<S>>();
        this.defaultRanking = defaultRanking;
        empty = true; // Initially empty
    }

    /**
     * Called when a service gets added.
     *
     * @param service The added service
     */
    protected void onServiceAdded(S service) {
        // Nothing
    }

    /**
     * Called when a service gets removed.
     *
     * @param service The removed service
     */
    protected void onServiceRemoved(S service) {
        // Nothing
    }

    /**
     * Gets the rank-wise sorted service list
     *
     * @return The rank-wise sorted service list
     */
    @Override
    public List<S> getServiceList() {
        if (empty) {
            return Collections.emptyList();
        }

        List<S> ret = new ArrayList<S>(services.size());
        for (RankedService<S> rs : services) {
            ret.add(rs.service);
        }
        return ret;
    }

    @Override
    public S addingService(final ServiceReference<S> reference) {
        final S service = context.getService(reference);
        final int ranking = getRanking(reference, defaultRanking);
        final RankedService<S> rankedService = new RankedService<S>(service, ranking);
        if (services.addAndSort(rankedService)) { // Append
            empty = false;
            onServiceAdded(service);
            return service;
        }
        context.ungetService(reference);
        return null;
    }

    @Override
    public void removedService(final ServiceReference<S> reference, final S service) {
        if (services.remove(new RankedService<S>(service, getRanking(reference)))) {
            empty = services.isEmpty();
            onServiceRemoved(service);
        }
        context.ungetService(reference);
    }

}
