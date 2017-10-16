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

package com.openexchange.chronos.json.converter.mapper;

import java.util.Collection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.conversion.ConversionResult;
import com.openexchange.conversion.Data;
import com.openexchange.conversion.DataArguments;
import com.openexchange.conversion.DataExceptionCodes;
import com.openexchange.conversion.DataHandler;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.tools.mappings.json.JsonMapper;
import com.openexchange.session.Session;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link Object2JsonDataHandler}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class Object2JsonDataHandler<O, E extends Enum<E>> implements DataHandler {

    private final JsonMapper<O, E> mapper;
    private final Class<O> clazz;

    /**
     * Initializes a new {@link Object2JsonDataHandler}.
     *
     * @param mapper The underlying JSON mapper
     * @param clazz The object class
     */
    public Object2JsonDataHandler(JsonMapper<O, E> mapper, Class<O> clazz) {
        super();
        this.mapper = mapper;
        this.clazz = clazz;
    }

    @Override
    public String[] getRequiredArguments() {
        return new String[0];
    }

    @Override
    public Class<?>[] getTypes() {
        return new Class<?>[] { clazz, Collection.class };
    }

    @Override
    public ConversionResult processData(Data<? extends Object> data, DataArguments dataArguments, Session session) throws OXException {
        ConversionResult result = new ConversionResult();
        Object sourceData = data.getData();
        if (null == sourceData) {
            result.setData(null);
        } else if (clazz.isInstance(sourceData)) {
            result.setData(serialize(clazz.cast(sourceData), optTimeZoneID(dataArguments, session)));
        } else if (Collection.class.isInstance(sourceData)) {
            result.setData(serialize((Collection<?>) sourceData, optTimeZoneID(dataArguments, session)));
        } else {
            throw DataExceptionCodes.TYPE_NOT_SUPPORTED.create(sourceData.getClass().toString());
        }
        return result;
    }

    private JSONObject serialize(O object, String timeZoneID) throws OXException {
        try {
            return mapper.serialize(object, mapper.getAssignedFields(object), timeZoneID);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private JSONArray serialize(Collection<?> objects, String timeZoneID) throws OXException {
        if (null == objects) {
            return null;
        }
        JSONArray jsonArray = new JSONArray(objects.size());
        for (Object object : objects) {
            try {
                jsonArray.put(serialize(clazz.cast(object), timeZoneID));
            } catch (ClassCastException e) {
                throw DataExceptionCodes.TYPE_NOT_SUPPORTED.create(e, object.getClass().toString());
            }
        }
        return jsonArray;
    }

    private static String optTimeZoneID(DataArguments dataArguments, Session session) {
        String timeZoneID = dataArguments.get(CalendarParameters.PARAMETER_TIMEZONE);
        if (null == timeZoneID && null != session) {
            try {
                timeZoneID = ServerSessionAdapter.valueOf(session).getUser().getTimeZone();
            } catch (OXException e) {
                org.slf4j.LoggerFactory.getLogger(Object2JsonDataHandler.class).warn("Error getting user timezone", e);
            }
        }
        return timeZoneID;
    }

}
