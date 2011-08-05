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

package com.openexchange.contact.json.actions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.fields.ContactFields;
import com.openexchange.ajax.fields.SearchFields;
import com.openexchange.ajax.parser.DataParser;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.contact.json.ContactRequest;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.ContactSearchMultiplexer;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.search.ContactSearchObject;
import com.openexchange.groupware.search.Order;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link SearchAction}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class SearchAction extends ContactAction {

    /**
     * Initializes a new {@link SearchAction}.
     * @param serviceLookup
     */
    public SearchAction(final ServiceLookup serviceLookup) {
        super(serviceLookup);
    }

    @Override
    protected AJAXRequestResult perform(final ContactRequest req) throws OXException {
        final ServerSession session = req.getSession();
        final int[] columns = req.getColumns();
        final int sort = req.getSort();
        final Order order = req.getOrder();
        final String collation = req.getCollation();
        Date lastModified = null;
        Date timestamp = new Date(0);
        final TimeZone timeZone = req.getTimeZone();
        
        final ContactSearchObject searchObject = createContactSearchObject((JSONObject) req.getData());
        final ContactSearchMultiplexer multiplexer = new ContactSearchMultiplexer(getContactInterfaceDiscoveryService());
        SearchIterator<Contact> it = null;
        final List<Contact> contacts = new ArrayList<Contact>();
        final Map<String, List<Contact>> contactMap = new HashMap<String, List<Contact>>(1);
        try {
            it = multiplexer.extendedSearch(session, searchObject, sort, order, collation, columns);
            while (it.hasNext()) {
                final Contact contact = it.next();
                lastModified = contact.getLastModified();
                
                // Correct last modified and creation date with users timezone
                contact.setLastModified(getCorrectedTime(contact.getLastModified(), timeZone));
                contact.setCreationDate(getCorrectedTime(contact.getCreationDate(), timeZone));
                contacts.add(contact);
                
                
                if (lastModified != null && timestamp.before(lastModified)) {
                    timestamp = lastModified;
                }
            }
        } finally {
            if (it != null) {
                it.close();
            }
        }
        
        contactMap.put("contacts", contacts);
        return new AJAXRequestResult(contactMap, lastModified, "contacts");
    }
    
    private ContactSearchObject createContactSearchObject(final JSONObject json) throws OXException {
        ContactSearchObject searchObject = null;
        try {
            searchObject = new ContactSearchObject();
            if (json.has("folder")) {
                if (json.get("folder").getClass().equals(JSONArray.class)) {
                    for (final int folder : DataParser.parseJSONIntArray(json, "folder")) {
                        searchObject.addFolder(folder);
                    }
                } else {
                    searchObject.addFolder(DataParser.parseInt(json, "folder"));
                }
            }
            if (json.has(SearchFields.PATTERN)) {
                searchObject.setPattern(DataParser.parseString(json, SearchFields.PATTERN));
            }
            if (json.has("startletter")) {
                searchObject.setStartLetter(DataParser.parseBoolean(json, "startletter"));
            }
            if (json.has("emailAutoComplete") && json.getBoolean("emailAutoComplete")) {
                searchObject.setEmailAutoComplete(true);
            }
            if (json.has("orSearch") && json.getBoolean("orSearch")) {
                searchObject.setOrSearch(true);
            }
            
            searchObject.setSurname(DataParser.parseString(json, ContactFields.LAST_NAME));
            searchObject.setDisplayName(DataParser.parseString(json, ContactFields.DISPLAY_NAME));
            searchObject.setGivenName(DataParser.parseString(json, ContactFields.FIRST_NAME));
            searchObject.setCompany(DataParser.parseString(json, ContactFields.COMPANY));
            searchObject.setEmail1(DataParser.parseString(json, ContactFields.EMAIL1));
            searchObject.setEmail2(DataParser.parseString(json, ContactFields.EMAIL2));
            searchObject.setEmail3(DataParser.parseString(json, ContactFields.EMAIL3));
            searchObject.setDepartment(DataParser.parseString(json, ContactFields.DEPARTMENT));
            searchObject.setStreetBusiness(DataParser.parseString(json, ContactFields.STREET_BUSINESS));
            searchObject.setCityBusiness(DataParser.parseString(json, ContactFields.CITY_BUSINESS));
            searchObject.setDynamicSearchField(DataParser.parseJSONIntArray(json, "dynamicsearchfield"));
            searchObject.setDynamicSearchFieldValue(DataParser.parseJSONStringArray(json, "dynamicsearchfieldvalue"));
            searchObject.setPrivatePostalCodeRange(DataParser.parseJSONStringArray(json, "privatepostalcoderange"));
            searchObject.setBusinessPostalCodeRange(DataParser.parseJSONStringArray(json, "businesspostalcoderange"));
            searchObject.setPrivatePostalCodeRange(DataParser.parseJSONStringArray(json, "privatepostalcoderange"));
            searchObject.setOtherPostalCodeRange(DataParser.parseJSONStringArray(json, "otherpostalcoderange"));
            searchObject.setBirthdayRange(DataParser.parseJSONDateArray(json, "birthdayrange"));
            searchObject.setAnniversaryRange(DataParser.parseJSONDateArray(json, "anniversaryrange"));
            searchObject.setNumberOfEmployeesRange(DataParser.parseJSONStringArray(json, "numberofemployee"));
            searchObject.setSalesVolumeRange(DataParser.parseJSONStringArray(json, "salesvolumerange"));
            searchObject.setCreationDateRange(DataParser.parseJSONDateArray(json, "creationdaterange"));
            searchObject.setLastModifiedRange(DataParser.parseJSONDateArray(json, "lastmodifiedrange"));
            searchObject.setCatgories(DataParser.parseString(json, "categories"));
            searchObject.setSubfolderSearch(DataParser.parseBoolean(json, "subfoldersearch"));
            searchObject.setYomiCompany(DataParser.parseString(json, ContactFields.YOMI_COMPANY));
            searchObject.setYomiFirstname(DataParser.parseString(json, ContactFields.YOMI_FIRST_NAME));
            searchObject.setYomiLastName(DataParser.parseString(json, ContactFields.YOMI_LAST_NAME));
        } catch (final JSONException e) {
            throw OXJSONExceptionCodes.JSON_READ_ERROR.create(e);
        }
        
        return searchObject;
    }

}
