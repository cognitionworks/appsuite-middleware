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

package com.openexchange.capabilities.json;

import java.util.Collection;
import java.util.Iterator;
import org.json.ImmutableJSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.capabilities.Capability;

/**
 * {@link CapabilitiesJsonWriter} - A simple JSON writer for capabilities.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.6.2
 */
public class CapabilitiesJsonWriter {

    /**
     * Initializes a new {@link CapabilitiesJsonWriter}.
     */
    private CapabilitiesJsonWriter() {
        super();
    }

    private static final JSONObject EMPTY_JSON = ImmutableJSONObject.immutableFor(new JSONObject(0));

    /**
     * Converts given capability to its JSON representation.
     *
     * @param capability The capability
     * @return The capability's JSON representation
     * @throws JSONException If JSON representation cannot be returned
     */
    public static JSONObject toJson(Capability capability) throws JSONException {
        final JSONObject object = new JSONObject(3);
        object.put("id", capability.getId());
        object.put("attributes", EMPTY_JSON);
        return object;
    }

    /**
     * Converts given capabilities collection to its JSON representation.
     *
     * @param capabilities The capabilities collection
     * @return The JSON representation for the capabilities collection
     * @throws JSONException If JSON representation cannot be returned
     */
    public static JSONArray toJson(Collection<Capability> capabilities) throws JSONException {
        int size = capabilities.size();
        JSONArray array = new JSONArray(size);
        Iterator<Capability> iterator = capabilities.iterator();
        for (int i = size; i-- > 0;) {
            array.put(toJson(iterator.next()));
        }
        return array;
    }

}
