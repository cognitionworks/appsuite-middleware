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

package com.openexchange.ajax.framework;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.ajax.framework.config.util.ChangePropertiesRequest;
import com.openexchange.ajax.framework.config.util.ChangePropertiesResponse;
import com.openexchange.ajax.writer.ResponseWriter;

/**
 * {@link AbstractConfigAwareAPIClientSession} extends the AbstractAPIClientSession with methods to preconfigure reloadable configurations before executing the tests.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.1
 */
public abstract class AbstractConfigAwareAPIClientSession extends AbstractAPIClientSession {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractConfigAwareAPIClientSession.class);

    /**
     * Initializes a new {@link AbstractConfigAwareAPIClientSession}.
     *
     * @param name
     */
    protected AbstractConfigAwareAPIClientSession() {}

    Map<AJAXClient, JSONObject> oldData = new HashMap<>(3, 0.9f);

    /**
     * Changes the configurations given by {@link #getNeededConfigurations()}.
     *
     * @throws Exception if changing the configuration fails
     */
    protected void setUpConfiguration() throws Exception {
        setUpConfiguration(getClient());
    }

    /**
     * Changes the configurations given by {@link #getNeededConfigurations()}.
     *
     * @throws Exception if changing the configuration fails
     */
    protected void setUpConfiguration(AJAXClient client) throws Exception {
        Map<String, String> map = getNeededConfigurations();
        if (!map.isEmpty()) {
            // change configuration to new values
            ChangePropertiesRequest req = new ChangePropertiesRequest(map, getScope(), getReloadables());
            ChangePropertiesResponse response = client.execute(req);
            oldData.put(client, ResponseWriter.getJSON(response.getResponse()).getJSONObject("data"));
        }
    }

    /**
     * Roll back the configuration based in the old data
     *
     * @param client The client to use
     * @param oldData The data to restore
     * @throws Exception In case of error
     */
    protected void rollbackConfiguration(AJAXClient client, JSONObject oldData) throws Exception {
        Map<String, Object> map = oldData.asMap();
        if (map.isEmpty()) {
            return;
        }
        Map<String, String> newMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            try {
                Object value = entry.getValue();
                if (value == JSONObject.NULL) {
                    value = null;
                }
                newMap.put(entry.getKey(), (String) value);
            } catch (ClassCastException cce) {
                //should never be the case
                LOG.error("Cannot revert the old values", cce);
                continue;
            }
        }
        ChangePropertiesRequest req = new ChangePropertiesRequest(newMap, getScope(), getReloadables());
        ChangePropertiesResponse response = client.execute(req);
        LOG.info("Restored capabilities, etc. back to:\n{}", ResponseWriter.getJSON(response.getResponse()));
    }

    @Override
    public void tearDown() throws Exception {
        try {
            if (oldData.isEmpty()) {
                return;
            }
            for (Entry<AJAXClient, JSONObject> entry : oldData.entrySet()) {
                rollbackConfiguration(entry.getKey(), entry.getValue());
            }
        } finally {
            super.tearDown();
        }
    }

    /**
     *
     * Retrieves all needed configurations.
     *
     * Should be overwritten by child implementations to define necessary configurations.
     *
     * @return Needed configurations.
     */
    protected Map<String, String> getNeededConfigurations() {
        return Collections.emptyMap();
    }

    /**
     * Retrieves the scope to use for the configurations.
     *
     * Can be overwritten by child implementations to change the scope of the configurations. Defaults to "user".
     *
     * @return The scope for the configuration.
     */
    protected String getScope() {
        return "user";
    }

    /**
     * Retrieves the the names of the reloadable classes which should be reloaded.
     *
     * Can be overwritten by child implementations. Defaults to null.
     *
     * @return A comma separated list of reloadable class names or null.
     */
    protected String getReloadables() {
        return null;
    }

    /**
     * Gets the default user id
     *
     * @return The user id
     */
    protected int getUserId() {
        return apiClient.getUserId().intValue();
    }
}
