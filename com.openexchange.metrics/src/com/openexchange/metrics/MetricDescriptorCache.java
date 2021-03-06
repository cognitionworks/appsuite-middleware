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

package com.openexchange.metrics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * {@link MetricDescriptorCache}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since 7.10.1
 */
@SuppressWarnings("deprecation")
public class MetricDescriptorCache {

    private final ConcurrentMap<String, MetricDescriptor> metricDescriptors;
    private final MetricService metricService;
    private final String group;

    /**
     * Initialises a new {@link MetricDescriptorCache}.
     *
     * @param metricService The instance of the {@link MetricService}
     * @param group The name of the group for with the metric descriptor cache shall be initialised.
     */
    public MetricDescriptorCache(MetricService metricService, String group) {
        super();
        this.metricService = metricService;
        this.group = group;
        metricDescriptors = new ConcurrentHashMap<>(8);
    }

    /**
     * Retrieves the metric descriptor for the metric type
     *
     * @param metricType The {@link MetricType}
     * @param name the method name
     * @param description The metric's description
     * @param unit The metric's unit
     * @return The {@link MetricDescriptor} for the specified metric
     */
    public MetricDescriptor getMetricDescriptor(MetricType metricType, String name, String description, String unit) {
        return getMetricDescriptor(metricType, name, description, unit, TimeUnit.SECONDS);
    }

    /**
     * Retrieves the metric descriptor for the metric type
     *
     * @param metricType The {@link MetricType}
     * @param name the method name
     * @param description The metric's description
     * @param unit The metric's unit
     * @param rate The rate unit
     * @return The {@link MetricDescriptor} for the specified metric
     */
    public MetricDescriptor getMetricDescriptor(MetricType metricType, String name, String description, String unit, TimeUnit rate) {
        MetricDescriptor metricDescriptor = metricDescriptors.get(name);
        if (metricDescriptor != null) {
            return metricDescriptor;
        }

        metricDescriptor = MetricDescriptor.newBuilder(group, name, metricType).withUnit(unit).withRate(rate).withDescription(String.format(description, name)).build();
        MetricDescriptor raced = metricDescriptors.putIfAbsent(name, metricDescriptor);
        if (raced == null) {
            return metricDescriptor;
        }
        return raced;
    }

    /**
     * Unregisters all metrics and clears the cache
     */
    public void clear() {
        metricDescriptors.clear();
        MetricService metricService = this.metricService;
        if (metricService == null) {
            return;
        }
        for (MetricDescriptor metricDescriptor : metricDescriptors.values()) {
            metricService.removeMetric(metricDescriptor);
        }
    }
}
