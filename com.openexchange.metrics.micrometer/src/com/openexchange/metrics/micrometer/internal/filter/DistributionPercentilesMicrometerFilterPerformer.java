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

package com.openexchange.metrics.micrometer.internal.filter;

import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.java.Strings;
import com.openexchange.metrics.micrometer.internal.property.MicrometerFilterProperty;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;

/**
 * {@link DistributionPercentilesMicrometerFilterPerformer} - Applies metric filters for
 * properties <code>com.openexchange.metrics.micrometer.distribution.percentiles.*</code>
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.4
 */
public class DistributionPercentilesMicrometerFilterPerformer extends AbstractMicrometerFilterPerformer {

    private static final Logger LOG = LoggerFactory.getLogger(DistributionPercentilesMicrometerFilterPerformer.class);

    /**
     * Initializes a new {@link DistributionPercentilesMicrometerFilterPerformer}.
     */
    public DistributionPercentilesMicrometerFilterPerformer() {
        super(MicrometerFilterProperty.PERCENTILES);
    }

    @Override
    public void applyFilter(MeterRegistry meterRegistry, ConfigurationService configurationService) {
        applyFilterFor(configurationService, (entry) -> configure(meterRegistry, entry));
    }

    @Override
    DistributionStatisticConfig applyConfig(Id id, Entry<String, String> entry, String metricId, DistributionStatisticConfig config) {
        if (!id.getName().startsWith(metricId)) {
            return config;
        }

        if (Strings.isEmpty(entry.getValue())) {
            return DistributionStatisticConfig.builder().percentiles(new double[0]).build().merge(config);
        }

        String[] p = Strings.splitByComma(entry.getValue());
        double[] percentiles = new double[p.length];
        int index = 0;
        for (String s : p) {
            try {
                double value = Double.parseDouble(s);
                if (value < 0 || value > 1) {
                    LOG.error("Invalid percentile '{}' for '{}'. Only values between 0 and 1 are allowed.", Double.valueOf(value), metricId);
                    return config;
                }
                percentiles[index++] = value;
            } catch (NumberFormatException e) {
                LOG.error("Percentile '{}' cannot be parsed as double. Ignoring percentiles configuration.", s, e);
                return config;
            }
        }
        return DistributionStatisticConfig.builder().percentiles(percentiles).build().merge(config);
    }
}
