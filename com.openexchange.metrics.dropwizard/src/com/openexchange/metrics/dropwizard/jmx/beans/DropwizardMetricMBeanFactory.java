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

package com.openexchange.metrics.dropwizard.jmx.beans;

import java.util.concurrent.TimeUnit;
import javax.management.NotCompliantMBeanException;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.openexchange.metrics.descriptors.MeterDescriptor;
import com.openexchange.metrics.descriptors.MetricDescriptor;
import com.openexchange.metrics.dropwizard.types.DropwizardCounter;
import com.openexchange.metrics.dropwizard.types.DropwizardGauge;
import com.openexchange.metrics.dropwizard.types.DropwizardHistogram;
import com.openexchange.metrics.dropwizard.types.DropwizardMeter;
import com.openexchange.metrics.dropwizard.types.DropwizardTimer;
import com.openexchange.metrics.jmx.CounterMBean;
import com.openexchange.metrics.jmx.GaugeMBean;
import com.openexchange.metrics.jmx.HistogramMBean;
import com.openexchange.metrics.jmx.MeterMBean;
import com.openexchange.metrics.jmx.MetricMBeanFactory;
import com.openexchange.metrics.jmx.TimerMBean;
import com.openexchange.metrics.types.Metric;

/**
 * {@link DropwizardMetricMBeanFactory}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class DropwizardMetricMBeanFactory implements MetricMBeanFactory {

    /**
     * Initialises a new {@link DropwizardMetricMBeanFactory}.
     */
    public DropwizardMetricMBeanFactory() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.metrics.jmx.MetricMBeanFactory#counter(com.openexchange.metrics.types.Metric)
     */
    @Override
    public CounterMBean counter(Metric counter) {
        checkInstance(counter, DropwizardCounter.class);
        try {
            return new CounterMBeanImpl((DropwizardCounter) counter);
        } catch (NotCompliantMBeanException e) {
            throw new IllegalArgumentException("The CounterMBean is not a compliant MBean");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.metrics.jmx.MetricMBeanFactory#timer(com.openexchange.metrics.types.Metric, com.openexchange.metrics.descriptors.MetricDescriptor)
     */
    @Override
    public TimerMBean timer(Metric timer, MetricDescriptor metricDescriptor) {
        checkInstance(timer, DropwizardTimer.class);
        try {
            return new TimerMBeanImpl((DropwizardTimer) timer, "events", TimeUnit.SECONDS); //TODO: pass the metric descriptor time unit
        } catch (NotCompliantMBeanException e) {
            throw new IllegalArgumentException("The TimerMBean is not a compliant MBean");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.metrics.jmx.MetricMBeanFactory#meter(com.openexchange.metrics.types.Metric, com.openexchange.metrics.descriptors.MetricDescriptor)
     */
    @Override
    public MeterMBean meter(Metric meter, MetricDescriptor metricDescriptor) {
        checkInstance(meter, DropwizardMeter.class);
        MeterDescriptor meterDescriptor = (MeterDescriptor) metricDescriptor;
        try {
            return new MeterMBeanImpl((DropwizardMeter) meter, meterDescriptor.getUnit(), meterDescriptor.getRate());
        } catch (NotCompliantMBeanException e) {
            throw new IllegalArgumentException("The MeterMBean is not a compliant MBean");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.metrics.jmx.MetricMBeanFactory#histogram(com.openexchange.metrics.types.Metric)
     */
    @Override
    public HistogramMBean histogram(Metric histogram) {
        checkInstance(histogram, Histogram.class);
        try {
            return new HistogramMBeanImpl((DropwizardHistogram) histogram);
        } catch (NotCompliantMBeanException e) {
            throw new IllegalArgumentException("The HistogramMBean is not a compliant MBean");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.metrics.jmx.MetricMBeanFactory#gauge(com.openexchange.metrics.types.Metric)
     */
    @Override
    public GaugeMBean gauge(Metric gauge) {
        if (!(gauge instanceof Gauge)) {
            throw new IllegalArgumentException("Invalid metric specified for 'Gauge' mbean: '" + gauge.getClass() + "'");
        }
        //checkInstance(metric, Gauge.class);
        try {
            return new GaugeMBeanImpl((DropwizardGauge) gauge);
        } catch (NotCompliantMBeanException e) {
            throw new IllegalArgumentException("The GaugeMBean is not a compliant MBean");
        }
    }

    /**
     * Checks if the instance of the specified {@link Metric} is assignable from the specified {@link Class}
     * 
     * @param metric The {@link Metric} to check
     * @param clazz The expected assignable {@link Class}
     * @throws IllegalArgumentException if the specified {@link Metric} is not assignable from the specified {@link Class}
     */
    private static void checkInstance(Metric metric, Class<?> clazz) {
        if (false == metric.getClass().isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Invalid metric specified for '" + clazz.getSimpleName() + "' mbean: '" + metric.getClass() + "'");
        }
    }
}
