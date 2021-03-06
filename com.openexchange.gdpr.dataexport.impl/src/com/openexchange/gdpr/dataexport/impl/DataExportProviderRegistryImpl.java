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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.gdpr.dataexport.impl;

import static com.openexchange.osgi.util.RankedService.getRanking;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.exception.OXException;
import com.openexchange.gdpr.dataexport.DataExportProvider;
import com.openexchange.gdpr.dataexport.DataExportProviderRegistry;
import com.openexchange.gdpr.dataexport.Module;
import com.openexchange.java.SortableConcurrentList;
import com.openexchange.java.Strings;
import com.openexchange.osgi.util.RankedService;
import com.openexchange.session.Session;

/**
 * {@link DataExportProviderRegistryImpl} - A registry for registered instances of {@code DataExportProvider}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.3
 */
public class DataExportProviderRegistryImpl extends ServiceTracker<DataExportProvider, DataExportProvider> implements DataExportProviderRegistry {

    private final ConcurrentMap<String, SortableConcurrentList<RankedService<DataExportProvider>>> providers;

    /**
     * Initializes a new {@link DataExportProviderRegistryImpl}.
     */
    public DataExportProviderRegistryImpl(BundleContext context) {
        super(context, DataExportProvider.class, null);
        providers = new ConcurrentHashMap<>(10, 0.9F, 1);
    }

    @Override
    public List<Module> getAvailableModules(Session session) throws OXException {
        List<Module> modules = new ArrayList<>(providers.size());
        for (SortableConcurrentList<RankedService<DataExportProvider>> list : providers.values()) {
            List<RankedService<DataExportProvider>> snapshot = list.getSnapshot();
            if (!snapshot.isEmpty()) {
                Optional<Module> optionalModule = snapshot.get(0).service.getModule(session);
                if (optionalModule.isPresent()) {
                    modules.add(optionalModule.get());
                }
            }
        }
        return modules;
    }

    @Override
    public Optional<DataExportProvider> getHighestRankedProviderFor(String moduleId) {
        if (Strings.isEmpty(moduleId)) {
            return Optional.empty();
        }

        SortableConcurrentList<RankedService<DataExportProvider>> list = providers.get(moduleId);
        if (list == null) {
            return Optional.empty();
        }

        List<RankedService<DataExportProvider>> snapshot = list.getSnapshot();
        return snapshot.isEmpty() ? Optional.empty() : Optional.of(snapshot.get(0).service);
    }

    @Override
    public DataExportProvider addingService(ServiceReference<DataExportProvider> reference) {
        DataExportProvider provider = context.getService(reference);
        String moduleId = provider.getId();

        RankedService<DataExportProvider> rankedService = new RankedService<>(provider, getRanking(provider, reference, 0));
        SortableConcurrentList<RankedService<DataExportProvider>> list = providers.get(moduleId);
        if (list == null) {
            SortableConcurrentList<RankedService<DataExportProvider>> newlist = new SortableConcurrentList<>();
            list = providers.putIfAbsent(moduleId, newlist);
            if (list == null) {
                list = newlist;
            }
        }
        list.addAndSort(rankedService);

        return provider;
    }

    @Override
    public void removedService(ServiceReference<DataExportProvider> reference, DataExportProvider provider) {
        SortableConcurrentList<RankedService<DataExportProvider>> list = providers.get(provider.getId());
        if (list != null) {
            list.remove(new RankedService<>(provider, getRanking(provider, reference, 0)));
        }
        context.ungetService(reference);
    }

}
