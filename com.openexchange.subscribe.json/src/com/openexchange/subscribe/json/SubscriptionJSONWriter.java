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
 *    trademarks of the Open-Xchange, Inc. group of companies.
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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.subscribe.json;

import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.datatypes.genericonf.DynamicFormDescription;
import com.openexchange.datatypes.genericonf.FormElement;
import com.openexchange.datatypes.genericonf.json.FormContentWriter;
import com.openexchange.datatypes.genericonf.json.ValueWriterSwitch;
import com.openexchange.subscribe.Subscription;

/**
 * {@link SubscriptionJSONWriter}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class SubscriptionJSONWriter {

    public static final int CLASS_ID = 2;

    private static final FormContentWriter formContentWriter = new FormContentWriter();

    private static final ValueWriterSwitch valueWrite = new ValueWriterSwitch();

    private static final String ID = "id";

    private static final String FOLDER = "folder";

    private static final String DISPLAYNAME = "displayName";

    private static final String SOURCE = "source";
    
    private static final String ENABLED = "enabled";

    public JSONObject write(Subscription subscription, DynamicFormDescription form, String urlPrefix) throws JSONException, SubscriptionJSONException {
        JSONObject object = new JSONObject();
        object.put(ID, subscription.getId());
        object.put(FOLDER, subscription.getFolderId());
        object.put(ENABLED, subscription.isEnabled());
        object.put(DISPLAYNAME, subscription.getDisplayName());
        object.put(SOURCE, subscription.getSource().getId());
        writeConfiguration(object, subscription.getSource().getId(), subscription.getConfiguration(), form, urlPrefix);
        return object;
    }

    private void writeConfiguration(JSONObject object, String id, Map<String, Object> configuration, DynamicFormDescription form, String urlPrefix) throws JSONException, SubscriptionJSONException {
        JSONObject config = formContentWriter.write(form, configuration, urlPrefix);
        object.put(id, config);
    }

    public JSONArray writeArray(Subscription subscription, String[] basicCols, Map<String, String[]> specialCols, List<String> specialsList, DynamicFormDescription form) throws SubscriptionJSONException {
        JSONArray array = new JSONArray();
        writeBasicCols(array, subscription, basicCols);
        for (String identifier : specialsList) {
            writeSpecialCols(array, subscription, specialCols.get(identifier), identifier, form);
        }
        return array;
    }

    private void writeSpecialCols(JSONArray array, Subscription subscription, String[] strings, String externalId, DynamicFormDescription form) throws SubscriptionJSONException {
        if (strings == null) {
            return;
        }
        boolean writeNulls = !subscription.getSource().getId().equals(externalId);
        Map<String, Object> configuration = subscription.getConfiguration();
        for (String col : strings) {
            if (writeNulls) {
                array.put(JSONObject.NULL);
            } else {
                Object value = configuration.get(col);
                FormElement field = form.getField(col);
                value = field.doSwitch(valueWrite, value);
                array.put(value);
            }
        }
    }

    private void writeBasicCols(JSONArray array, Subscription subscription, String[] basicCols) throws SubscriptionJSONException {
        for (String basicCol : basicCols) {
            if (ID.equals(basicCol)) {
                array.put(subscription.getId());
            } else if (FOLDER.equals(basicCol)) {
                array.put(subscription.getFolderId());
            } else if (SOURCE.equals(basicCol)) {
                array.put(subscription.getSource().getId());
            } else if (DISPLAYNAME.equals(basicCol)) {
                array.put(subscription.getDisplayName());
            } else if (ENABLED.equals(basicCol)) {
                array.put(subscription.isEnabled());
            } else {
                SubscriptionJSONErrorMessages.UNKNOWN_COLUMN.throwException(basicCol);
            }
        }
    }

}
