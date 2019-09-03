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

package com.openexchange.mail.compose.impl.storage.db.mapping;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link VarCharJsonCustomHeadersMapping}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.2
 */
public abstract class VarCharJsonCustomHeadersMapping<O> extends AbstractVarCharJsonObjectMapping<Map<String, String>, O> {

    private static final Logger LOG = LoggerFactory.getLogger(VarCharJsonCustomHeadersMapping.class);

    public VarCharJsonCustomHeadersMapping(String columnLabel, String readableName) {
        super(columnLabel, readableName);
    }

    @Override
    public int set(PreparedStatement statement, int parameterIndex, O object) throws SQLException {
        if (!isSet(object)) {
            statement.setNull(parameterIndex, getSqlType());
            return 1;
        }

        Map<String, String> value = get(object);
        if (value == null) {
            statement.setNull(parameterIndex, getSqlType());
        } else {
            JSONObject jsonMeta = new JSONObject(value.size());
            try {
                for (Map.Entry<String, String> customHeader : value.entrySet()) {
                    jsonMeta.put(customHeader.getKey(), customHeader.getValue());
                }
            } catch (JSONException e) {
                LOG.error("Unable to generate JSONObject.", e);
            }
            statement.setString(parameterIndex, jsonMeta.toString());
        }
        return 1;
    }

    @Override
    public Map<String, String> get(ResultSet resultSet, String columnLabel) throws SQLException {
        String value = resultSet.getString(columnLabel);
        if (value == null || value.isEmpty()) {
            return null;
        }

        Map<String, String> retval = null;
        try {
            JSONObject jsonCustomHeaders = new JSONObject(value);
            retval = new LinkedHashMap<>(jsonCustomHeaders.length());
            for (Map.Entry<String, Object> jsonCustomHeader : jsonCustomHeaders.entrySet()) {
                retval.put(jsonCustomHeader.getKey(), jsonCustomHeader.getValue().toString());
            }
        } catch (JSONException | ClassCastException | NumberFormatException e) {
            LOG.error("Unable to parse {} to custom headers information", value, e);
        }
        return retval;
    }

}
