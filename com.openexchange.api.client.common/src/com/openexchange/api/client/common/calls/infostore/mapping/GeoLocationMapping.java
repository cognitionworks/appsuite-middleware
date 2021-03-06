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

package com.openexchange.api.client.common.calls.infostore.mapping;

import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.tools.mappings.json.DefaultJsonMapping;
import com.openexchange.java.GeoLocation;
import com.openexchange.session.Session;

/**
 * {@link GeoLocationMapping}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @param <O> The object type
 * @since v7.10.5
 */
public abstract class GeoLocationMapping<O> extends DefaultJsonMapping<GeoLocation, O> {

    private static Pattern pattern = Pattern.compile("\\((.*),(.*)\\)");

    /**
     * Initializes a new {@link GeoLocationMapping}.
     * 
     * @param ajaxName The name of object ID in the JSON response
     * @param columnID The The column ID in the request
     */
    public GeoLocationMapping(String ajaxName, Integer columnID) {
        super(ajaxName, columnID);
    }

    @Override
    public Object serialize(O from, TimeZone timeZone, Session session) throws JSONException, OXException {
        GeoLocation geolocation = this.get(from);
        if (geolocation != null) {
            return geolocation.toString();
        }
        return JSONObject.NULL;
    }

    @Override
    public void deserialize(JSONObject from, O to) throws JSONException, OXException {
        if (from.has("geolocation")) {
            Matcher matcher = pattern.matcher(from.getString("geolocation"));
            if (matcher.find()) {
                this.set(to, new GeoLocation(Double.parseDouble(matcher.group(1)), Double.parseDouble(matcher.group(2))));
            }
        }
    }
}
