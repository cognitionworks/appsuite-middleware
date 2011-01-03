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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

import static com.openexchange.subscribe.json.SubscriptionJSONErrorMessages.JSONEXCEPTION;
import static com.openexchange.subscribe.json.SubscriptionJSONErrorMessages.MISSING_FIELD;
import static com.openexchange.subscribe.json.SubscriptionJSONErrorMessages.MISSING_FORM_FIELD;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.datatypes.genericonf.DynamicFormDescription;
import com.openexchange.datatypes.genericonf.FormElement;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.i18n.Translator;
import com.openexchange.subscribe.SubscriptionSource;

/**
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 */
public class SubscriptionSourceJSONWriter implements SubscriptionSourceJSONWriterInterface {

    public static final int CLASS_ID = 2;

    private final Translator translator;

    public SubscriptionSourceJSONWriter(Translator translator) {
        super();
        this.translator = translator;
    }

    public JSONObject writeJSON(SubscriptionSource source) throws SubscriptionJSONException {
        validate(source);
        JSONObject retval = null;
        try {
            retval = parse(source);
        } catch (JSONException e) {
            JSONEXCEPTION.throwException(e);
        }
        return retval;
    }

    public JSONArray writeJSONArray(List<SubscriptionSource> sourceList, String[] fields) throws SubscriptionJSONException {
        JSONArray retval = new JSONArray();
        for (SubscriptionSource source : sourceList) {
            JSONArray row = new JSONArray();
            for(String field : fields) {
                if(ID.equals(field)) {
                    row.put(source.getId());
                } else if (DISPLAY_NAME.equals(field)) {
                    row.put(source.getDisplayName());
                } else if (ICON.equals(field)) {
                    row.put(source.getIcon());
                } else if (FORM_DESCRIPTION.equals(field)) {
                    try {
                        row.put(parseFormElements(source.getFormDescription()));
                    } catch (JSONException e) {
                        JSONEXCEPTION.throwException(e);
                    }
                } else if (MODULE.equals(field)) {
                    row.put(getModuleAsString(source));
                } else {
                    SubscriptionJSONErrorMessages.UNKNOWN_COLUMN.throwException(field);
                }
            }
            retval.put(row);
        }
        return retval;
    }

    private JSONObject parse(SubscriptionSource source) throws JSONException {
        JSONObject retval = new JSONObject();

        retval.put(ID, source.getId());
        retval.put(DISPLAY_NAME, source.getDisplayName());
        retval.put(FORM_DESCRIPTION, parseFormElements(source.getFormDescription()));
        if (source.getIcon() != null) {
            retval.put(ICON, source.getIcon());
        }
        retval.put(MODULE, getModuleAsString(source));

        return retval;
    }

    private String getModuleAsString(SubscriptionSource source) {
        int module = source.getFolderModule();
        switch(module) {
        case FolderObject.CONTACT : return "contacts";
        case FolderObject.CALENDAR : return "calendar";
        case FolderObject.TASK : return "tasks";
        case FolderObject.INFOSTORE: return "infostore";
        default : return null;
        }
    }

    private JSONArray parseFormElements(DynamicFormDescription formDescription) throws JSONException {
        JSONArray retval = new JSONArray();
        for (Iterator<FormElement> iter = formDescription.iterator(); iter.hasNext();) {
            FormElement element = iter.next();
            JSONObject jsonElement = new JSONObject();
            jsonElement.put(NAME, element.getName());
            jsonElement.put(DISPLAY_NAME, translator.translate(element.getDisplayName()));
            jsonElement.put(WIDGET, element.getWidget().getKeyword());
            jsonElement.put(MANDATORY, element.isMandatory());
            if (element.getDefaultValue() != null) {
                jsonElement.put(DEFAULT, element.getDefaultValue());
            }
            retval.put(jsonElement);
        }
        return retval;
    }

    private void validate(SubscriptionSource source) throws SubscriptionJSONException {
        List<String> missingFields = new ArrayList<String>();
        if (source.getId() == null) {
            missingFields.add(ID);
        }
        if (source.getDisplayName() == null) {
            missingFields.add(DISPLAY_NAME);
        }
        if (source.getFormDescription() == null) {
            missingFields.add(FORM_DESCRIPTION);
        }
        if (missingFields.size() > 0) {
            MISSING_FIELD.throwException(buildStringList(missingFields,", "));
        }

        for (Iterator<FormElement> iter = source.getFormDescription().iterator(); iter.hasNext();) {
            FormElement element = iter.next();
            List<String> missingFormFields = new ArrayList<String>();
            if (element.getName() == null) {
                missingFormFields.add(NAME);
            }
            if (element.getDisplayName() == null) {
                missingFormFields.add(DISPLAY_NAME);
            }
            if (element.getWidget() == null) {
                missingFormFields.add(WIDGET);
            }
            // TODO: check for mandatory field "mandatory"
            if (missingFormFields.size() > 0) {
                MISSING_FORM_FIELD.throwException(buildStringList(missingFormFields, ", "));
            }
        }
    }

    private String buildStringList(List<String> strings, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> iter = strings.iterator(); iter.hasNext();) {
            sb.append(iter.next());
            if (iter.hasNext()) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }
}
