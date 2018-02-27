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
 *     Copyright (C) 2018-2020 OX Software GmbH
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

package com.openexchange.metrics.dropwizard.impl;

import com.codahale.metrics.MetricRegistry;
import com.openexchange.metrics.AbstractMetricService;
import com.openexchange.metrics.MetricDescriptor;
import com.openexchange.metrics.MetricType;
import com.openexchange.metrics.dropwizard.types.DropwizardCounter;
import com.openexchange.metrics.dropwizard.types.DropwizardGauge;
import com.openexchange.metrics.dropwizard.types.DropwizardHistogram;
import com.openexchange.metrics.dropwizard.types.DropwizardMeter;
import com.openexchange.metrics.dropwizard.types.DropwizardTimer;
import com.openexchange.metrics.types.Counter;
import com.openexchange.metrics.types.Gauge;
import com.openexchange.metrics.types.Histogram;
import com.openexchange.metrics.types.Meter;
import com.openexchange.metrics.types.Timer;

/**
 * {@link DropwizardMetricService}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class DropwizardMetricService extends AbstractMetricService {

    private final MetricRegistry registry;

    /**
     * Initialises a new {@link DropwizardMetricService}.
     */
    public DropwizardMetricService() {
        super();
        registry = new MetricRegistry();

        addRegisterer(MetricType.METER, (metricDescriptor) -> new DropwizardMeter(registry.meter(MetricRegistry.name(metricDescriptor.getGroup(), metricDescriptor.getName()))));
        addRegisterer(MetricType.TIMER, (metricDescriptor) -> new DropwizardTimer(registry.timer(MetricRegistry.name(metricDescriptor.getGroup(), metricDescriptor.getName()))));
        addRegisterer(MetricType.COUNTER, (metricDescriptor) -> new DropwizardCounter(registry.counter(MetricRegistry.name(metricDescriptor.getGroup(), metricDescriptor.getName()))));
        addRegisterer(MetricType.HISTOGRAM, (metricDescriptor) -> new DropwizardHistogram(registry.histogram(MetricRegistry.name(metricDescriptor.getGroup(), metricDescriptor.getName()))));
        addRegisterer(MetricType.GAUGE, (metricDescriptor) -> new DropwizardGauge(registry.gauge(MetricRegistry.name(metricDescriptor.getGroup(), metricDescriptor.getName()), () -> () -> metricDescriptor.getMetricSupplier().get())));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.metrics.MetricService#getHistogram(java.lang.String, java.lang.String)
     */
    @Override
    public Histogram getHistogram(MetricDescriptor descriptor) {
        return (Histogram) registerOrGet(descriptor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.metrics.MetricService#timer(java.lang.String, java.lang.String)
     */
    @Override
    public Timer getTimer(MetricDescriptor descriptor) {
        return (Timer) registerOrGet(descriptor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.metrics.MetricService#getCounter(java.lang.String, java.lang.String)
     */
    @Override
    public Counter getCounter(MetricDescriptor descriptor) {
        return (Counter) registerOrGet(descriptor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.metrics.MetricService#getGauge(java.lang.String, java.lang.String, java.util.function.Supplier)
     */
    @Override
    public <T> Gauge<T> getGauge(MetricDescriptor descriptor) {
        return (Gauge<T>) registerOrGet(descriptor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.metrics.MetricService#meter(com.openexchange.metrics.MetricDescriptor)
     */
    @Override
    public Meter getMeter(MetricDescriptor descriptor) {
        return (Meter) registerOrGet(descriptor);
    }
}
