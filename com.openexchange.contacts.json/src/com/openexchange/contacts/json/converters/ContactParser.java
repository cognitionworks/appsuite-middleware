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

package com.openexchange.contacts.json.converters;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.fields.ContactFields;
import com.openexchange.ajax.fields.DistributionListFields;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.contact.helpers.ContactSetter;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;
import com.openexchange.groupware.container.LinkEntryObject;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;


/**
 * {@link ContactParser}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class ContactParser {

    private static final Map<Integer, String> specialColumns = new HashMap<Integer, String>();

    static {
        specialColumns.put(Contact.ANNIVERSARY, "date");
        specialColumns.put(Contact.BIRTHDAY, "date");
        specialColumns.put(Contact.DISTRIBUTIONLIST, "distributionlist");
        specialColumns.put(Contact.LINKS, "links");
    }

    protected boolean isSpecial(final int column) {
        return specialColumns.containsKey(column);
    }

    public Contact parse(final JSONObject json) throws OXException {
        final ContactSetter cs = new ContactSetter();
        final Contact contact = new Contact();
        for (final int column : Contact.JSON_COLUMNS) {
            final ContactField field = ContactField.getByValue(column);
            if (field != null) {
                final String key = field.getAjaxName();
                if (key != null && !key.isEmpty() && json.hasAndNotNull(key)) {
                    try {
                        final Object value = json.get(key);
                        if (isSpecial(column)) {
                            parseSpecial(field, cs, contact, value);
                        } else {
                            field.doSwitch(cs, contact, value);
                        }
                    } catch (final JSONException e) {
                        throw OXJSONExceptionCodes.JSON_READ_ERROR.create(e, json);
                    }
                }
            }
        }

        return contact;
    }

    private void parseSpecial(final ContactField field, final ContactSetter cs, final Contact contact, final Object value) throws OXException {
        final String type = specialColumns.get(field.getNumber());
        if (type.equals("date")) {
            final String timeStr = String.valueOf(value);
            try {
                final long time = Long.parseLong(timeStr);
                field.doSwitch(cs, contact, new Date(time));
            } catch (final NumberFormatException e) {
                throw OXJSONExceptionCodes.NUMBER_PARSING.create(e, timeStr, field.getAjaxName());
            }
        } else if (type.equals("distributionlist")) {
            JSONArray jsonDistributionList = null;
            try {
                jsonDistributionList = new JSONArray(String.valueOf(value));
                final DistributionListEntryObject[] distributionList = parseDistributionList(jsonDistributionList);
                field.doSwitch(cs, contact, distributionList);
            } catch (final JSONException e) {
                throw OXJSONExceptionCodes.JSON_READ_ERROR.create(e, jsonDistributionList);
            }
        } else if (type.equals("links")) {
            JSONArray jsonLinks = null;
            try {
                jsonLinks = new JSONArray(String.valueOf(value));
                final LinkEntryObject[] links = parseLinks(jsonLinks);
                field.doSwitch(cs, contact, links);
            } catch (final JSONException e) {
                throw OXJSONExceptionCodes.JSON_READ_ERROR.create(e, jsonLinks);
            }
        }
    }

    private DistributionListEntryObject[] parseDistributionList(final JSONArray jsonDistributionList) throws OXException {
        final DistributionListEntryObject[] distributionList = new DistributionListEntryObject[jsonDistributionList.length()];
        for (int i = 0; i < jsonDistributionList.length(); i++) {
            JSONObject entry = null;
            try {
                entry = jsonDistributionList.getJSONObject(i);
                distributionList[i] = new DistributionListEntryObject();
                if (hasAndNotEmptyOrNull(entry, DistributionListFields.ID)) {
                    distributionList[i].setEntryID(entry.getInt(DistributionListFields.ID));
                }

                if (hasAndNotEmptyOrNull(entry, DistributionListFields.FIRST_NAME)) {
                    distributionList[i].setFirstname(entry.getString(DistributionListFields.FIRST_NAME));
                }

                if (hasAndNotEmptyOrNull(entry, DistributionListFields.LAST_NAME)) {
                    distributionList[i].setFirstname(entry.getString(DistributionListFields.LAST_NAME));
                }

                if (hasAndNotEmptyOrNull(entry, DistributionListFields.DISPLAY_NAME)) {
                    distributionList[i].setDisplayname(entry.getString(DistributionListFields.DISPLAY_NAME));
                }
                
                if (hasAndNotEmptyOrNull(entry, DistributionListFields.MAIL)) {
                    distributionList[i].setEmailaddress(entry.getString(DistributionListFields.MAIL));
                }                
                
                if (hasAndNotEmptyOrNull(entry, DistributionListFields.MAIL_FIELD)) {
                    distributionList[i].setEmailfield(entry.getInt(DistributionListFields.MAIL_FIELD));
                }
            } catch (final JSONException e) {
                throw OXJSONExceptionCodes.JSON_READ_ERROR.create(e, entry);
            }
        }

        return distributionList;
    }
    
    private boolean hasAndNotEmptyOrNull(JSONObject json, String key) throws JSONException {
        if (json.hasAndNotNull(key)) {
            String str = json.getString(key);   
            if (!str.isEmpty()) {
                return true;
            }
        }
        
        return false;
    }

    private LinkEntryObject[] parseLinks(final JSONArray jsonLinks) throws JSONException, OXException {
        final LinkEntryObject[] links = new LinkEntryObject[jsonLinks.length()];
        for (int i = 0; i < links.length; i++) {
            links[i] = new LinkEntryObject();
            final JSONObject entry = jsonLinks.getJSONObject(i);
            if (hasAndNotEmptyOrNull(entry, ContactFields.ID)) {
                links[i].setLinkID(entry.getInt(ContactFields.ID));
            }
            
            if (hasAndNotEmptyOrNull(entry, DistributionListFields.DISPLAY_NAME)) {
                links[i].setLinkDisplayname(entry.getString(DistributionListFields.DISPLAY_NAME));
            }
        }

        return links;
    }

}
