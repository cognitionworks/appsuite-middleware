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

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableMap;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.metrics.micrometer.internal.property.MicrometerFilterProperty;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;

/**
 * {@link AbstractMicrometerFilterPerformer}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.4
 */
abstract class AbstractMicrometerFilterPerformer implements MicrometerFilterPerformer {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMicrometerFilterPerformer.class);

    static final AtomicReference<Map<String, Filter>> filterRegistryReference = new AtomicReference<Map<String, Filter>>(Collections.emptyMap());

    private final MicrometerFilterProperty property;

    /**
     * Initializes a new {@link AbstractMicrometerFilterPerformer}.
     */
    AbstractMicrometerFilterPerformer(MicrometerFilterProperty property) {
        super();
        this.property = property;
    }

    /**
     * This is only called when filtering new timers and distribution summaries
     * (i.e. those meter types that use DistributionStatisticConfig).
     *
     * @param meterRegistry The meter registry
     * @param entry The configuration entry
     */
    void configure(MeterRegistry meterRegistry, Entry<String, String> entry) {
        meterRegistry.config().meterFilter(new MeterFilter() {

            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                return AbstractMicrometerFilterPerformer.this.configure(id, entry, config);
            }
        });
    }

    /**
     * Applies the filter for the specified metric
     *
     * @param id The metric identifier
     * @param entry The configuration entry
     * @param config The distribution statistics configuration
     * @return if the filter is applied then it returns the merged config, otherwise the passed config unaltered
     */
    DistributionStatisticConfig configure(Meter.Id id, Entry<String, String> entry, DistributionStatisticConfig config) {
        LOG.debug("Applying filter for '{}'", id);
        String key = entry.getKey();
        String metricId = extractMetricId(key, property);
        Filter filter = filterRegistryReference.get().get(metricId);
        if (filter == null) {
            return applyConfig(id, entry, metricId, config);
        }
        return matchTags(id, filter) ? applyConfig(id, entry, filter.getMetricName(), config) : config;
    }

    /**
     * Applies the configuration of the specified entry to the specified {@link DistributionStatisticConfig}.
     * 
     * @param id The metric identifier
     * @param entry The entry with the configuration
     * @param metricId the metric identifier
     * @param config The {@link DistributionStatisticConfig}
     *
     * @return The merged config
     */
    @SuppressWarnings("unused")
    DistributionStatisticConfig applyConfig(Id id, Entry<String, String> entry, String metricId, DistributionStatisticConfig config) {
        return config;
    }

    /**
     * Applies the MeterFilter by the specified meter filter consumer to the specified meter registry.
     *
     * @param configurationService The configuration service to read the property's value
     * @param meterFilterConsumer The consumer which dictates the application of the meter filter
     */
    void applyFilterFor(ConfigurationService configurationService, Consumer<Entry<String, String>> meterFilterConsumer) {
        Map<String, String> properties = getPropertiesStartingWith(configurationService, property);
        properties.entrySet().stream().forEach(entry -> meterFilterConsumer.accept(entry));
    }

    /**
     * Extracts the metric id from the specified propertyName
     * by using the property as a base.
     *
     * @param propertyName The property name
     * @param property The base
     * @return The metric id
     */
    String extractMetricId(String propertyName, MicrometerFilterProperty property) {
        String prop = property.name().toLowerCase();
        return propertyName.substring(propertyName.indexOf(prop) + prop.length() + 1);
    }

    /**
     * Matches all tags from the specified {@link Id} with the tags specified
     * in the filter.
     *
     * @param id The id
     * @param filter The {@link Filter}
     * @return <code>true</code> if all tags match the filter; <code>false</code> otherwise
     */
    boolean matchTags(Meter.Id id, Filter filter) {
        LOG.debug("Metric: {}, Filter: {}", id, filter);
        if (false == id.getName().startsWith(filter.getMetricName())) {
            return false;
        }
        int matchCount = 0;
        for (Tag t : id.getTags()) {
            if (false == filter.getConditions().containsKey(t.getKey())) {
                continue;
            }
            Condition condition = filter.getConditions().get(t.getKey());
            if (false == condition.isRegex() && condition.getValue().equals(t.getValue()) && !condition.isNegated()) {
                matchCount++;
                continue;

            }
            matchCount += checkRegex(id, t, condition) ? 1 : 0;
        }
        return matchCount == filter.getConditions().size();
    }

    ///////////////////////////////// HELPERS /////////////////////////////

    /**
     * Returns all properties that start with the specified prefix.
     *
     * @param configurationService The configuration service
     * @param property The prefix of the property
     * @return The found properties or an empty map
     */
    Map<String, String> getPropertiesStartingWith(ConfigurationService configurationService, MicrometerFilterProperty property) {
        try {
            return configurationService.getProperties((name, value) -> name.startsWith(property.getFQPropertyName()));
        } catch (OXException e) {
            LOG.error("", e);
            return ImmutableMap.of();
        }
    }

    /**
     * Checks the regex against the specified metric
     *
     * @param id The metric id
     * @param filter The filter
     * @return <code>true</code> if the regex matches at one of the tags; <code>false</code> otherwise
     */
    private boolean checkRegex(Meter.Id id, Tag tag, Condition condition) {
        String regex = condition.getValue();
        try {
            boolean match = Pattern.compile(regex).matcher(tag.getValue()).matches();
            return condition.isNegated() ? !match : match;
        } catch (PatternSyntaxException e) {
            LOG.error("The specified regex '{}' for metric '{}' is invalid.", regex, id, e);
            return false;
        }
    }
}
