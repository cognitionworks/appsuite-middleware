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

package com.openexchange.user.json.actions;

import static com.openexchange.user.json.Utility.checkForRequiredField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.fields.ContactFields;
import com.openexchange.ajax.fields.OrderFields;
import com.openexchange.ajax.fields.SearchFields;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.api2.OXException;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.contact.ContactInterfaceDiscoveryService;
import com.openexchange.groupware.contact.ContactSearchMultiplexer;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.search.ContactSearchObject;
import com.openexchange.groupware.search.Order;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.servlet.AjaxException;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.servlet.OXJSONException;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.UserService;
import com.openexchange.user.json.Constants;
import com.openexchange.user.json.field.UserField;
import com.openexchange.user.json.services.ServiceRegistry;
import com.openexchange.user.json.writer.UserWriter;

/**
 * {@link SearchAction} - Maps the action to a <tt>search</tt> action.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SearchAction extends AbstractUserAction {

    private static final Contact[] EMPTY_CONTACTS = new Contact[0];

    /**
     * The <tt>search</tt> action string.
     */
    public static final String ACTION = AJAXServlet.ACTION_SEARCH;

    /**
     * Initializes a new {@link SearchAction}.
     */
    public SearchAction() {
        super();
    }

    private static final Set<String> EXPECTED_NAMES =
        Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            AJAXServlet.PARAMETER_COLUMNS,
            AJAXServlet.PARAMETER_SORT,
            AJAXServlet.PARAMETER_ORDER,
            AJAXServlet.PARAMETER_TIMEZONE,
            AJAXServlet.PARAMETER_SESSION,
            AJAXServlet.PARAMETER_ACTION)));

    public AJAXRequestResult perform(final AJAXRequestData request, final ServerSession session) throws AbstractOXException {
        try {
            /*
             * Parse parameters
             */
            final int[] columns = parseIntArrayParameter(AJAXServlet.PARAMETER_COLUMNS, request);
            final int orderBy = parseIntParameter(AJAXServlet.PARAMETER_SORT, request);
            final Order order = OrderFields.parse(request.getParameter(AJAXServlet.PARAMETER_ORDER));
            final String collation = request.getParameter(AJAXServlet.PARAMETER_COLLATION);
            final String timeZoneId = request.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
            /*
             * Get remaining parameters
             */
            final Map<String, List<String>> attributeParameters = getAttributeParameters(EXPECTED_NAMES, request);
            final JSONObject jData = (JSONObject) request.getData();
            /*
             * Contact search object
             */
            final ContactSearchObject searchObj = new ContactSearchObject();
            searchObj.addFolder(Constants.USER_ADDRESS_BOOK_FOLDER_ID);
            if (jData.has(SearchFields.PATTERN)) {
                searchObj.setPattern(parseString(jData, SearchFields.PATTERN));
            }
            if (jData.has("startletter")) {
                searchObj.setStartLetter(parseBoolean(jData, "startletter"));
            }
            if (jData.has("emailAutoComplete") && jData.getBoolean("emailAutoComplete")) {
                searchObj.setEmailAutoComplete(true);
            }
            if (jData.has("orSearch") && jData.getBoolean("orSearch")) {
                searchObj.setOrSearch(true);
            }
            searchObj.setSurname(parseString(jData, ContactFields.LAST_NAME));
            searchObj.setDisplayName(parseString(jData, ContactFields.DISPLAY_NAME));
            searchObj.setGivenName(parseString(jData, ContactFields.FIRST_NAME));
            searchObj.setCompany(parseString(jData, ContactFields.COMPANY));
            searchObj.setEmail1(parseString(jData, ContactFields.EMAIL1));
            searchObj.setEmail2(parseString(jData, ContactFields.EMAIL2));
            searchObj.setEmail3(parseString(jData, ContactFields.EMAIL3));
            searchObj.setDynamicSearchField(parseJSONIntArray(jData, "dynamicsearchfield"));
            searchObj.setDynamicSearchFieldValue(parseJSONStringArray(jData, "dynamicsearchfieldvalue"));
            searchObj.setPrivatePostalCodeRange(parseJSONStringArray(jData, "privatepostalcoderange"));
            searchObj.setBusinessPostalCodeRange(parseJSONStringArray(jData, "businesspostalcoderange"));
            searchObj.setPrivatePostalCodeRange(parseJSONStringArray(jData, "privatepostalcoderange"));
            searchObj.setOtherPostalCodeRange(parseJSONStringArray(jData, "otherpostalcoderange"));
            searchObj.setBirthdayRange(parseJSONDateArray(jData, "birthdayrange"));
            searchObj.setAnniversaryRange(parseJSONDateArray(jData, "anniversaryrange"));
            searchObj.setNumberOfEmployeesRange(parseJSONStringArray(jData, "numberofemployee"));
            searchObj.setSalesVolumeRange(parseJSONStringArray(jData, "salesvolumerange"));
            searchObj.setCreationDateRange(parseJSONDateArray(jData, "creationdaterange"));
            searchObj.setLastModifiedRange(parseJSONDateArray(jData, "lastmodifiedrange"));
            searchObj.setCatgories(parseString(jData, "categories"));
            searchObj.setSubfolderSearch(parseBoolean(jData, "subfoldersearch"));
            /*
             * Multiplex search
             */
            final UserField sortField = UserField.getUserOnlyField(orderBy);
            final SearchIterator<Contact> it;
            if (null == sortField) {
                // Sort field is a contact field: pass as it is
                final ContactSearchMultiplexer multiplexer =
                    new ContactSearchMultiplexer(ServiceRegistry.getInstance().getService(ContactInterfaceDiscoveryService.class));
                final int[] checkedCols = checkForRequiredField(columns, UserField.INTERNAL_USERID.getColumn());
                it = multiplexer.extendedSearch(session, searchObj, orderBy, order, collation, checkedCols);
            } else {
                // Get contact iterator with dummy search fields
                final ContactSearchMultiplexer multiplexer =
                    new ContactSearchMultiplexer(ServiceRegistry.getInstance().getService(ContactInterfaceDiscoveryService.class));
                final int[] checkedCols = checkForRequiredField(columns, UserField.INTERNAL_USERID.getColumn());
                it = multiplexer.extendedSearch(session, searchObj, UserField.DISPLAY_NAME.getColumn(), Order.ASCENDING, collation, checkedCols);
            }
            /*
             * Collect contacts from iterator
             */
            final Contact[] contacts;
            try {
                final List<Contact> list = new ArrayList<Contact>(128);
                while (it.hasNext()) {
                    list.add(it.next());
                }
                contacts = list.toArray(EMPTY_CONTACTS);
            } finally {
                try {
                    it.close();
                } catch (final Exception e) {
                    final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SearchAction.class);
                    LOG.error(e.getMessage(), e);
                }
            }
            /*
             * Get corresponding users
             */
            final UserService userService = ServiceRegistry.getInstance().getService(UserService.class, true);
            final User[] users = new User[contacts.length];
            for (int i = 0; i < users.length; i++) {
                users[i] = userService.getUser(contacts[i].getInternalUserId(), session.getContext());
            }
            /*
             * TODO: Sort users if a user field was denoted by sort field
             */
            // Determine max. last-modified time stamp
            Date lastModified = new Date(0);
            for (int i = 1; i < contacts.length; i++) {
                final Date lm = contacts[i].getLastModified();
                if (lastModified.before(lm)) {
                    lastModified = lm;
                }
            }
            /*
             * Write users as JSON arrays to JSON array
             */
            censor(session, contacts);
            censor(session, users);
            final JSONArray jsonArray = UserWriter.writeMultiple2Array(columns, attributeParameters, users, contacts, timeZoneId);
            /*
             * Return appropriate result
             */
            return new AJAXRequestResult(jsonArray, lastModified);
        } catch (final OXException e) {
            throw new AjaxException(e);
        } catch (final JSONException e) {
            throw new AjaxException(AjaxExceptionCodes.JSONError, e, e.getMessage());
        }
    }

    /**
     * Parses optional field out of specified JSON object.
     * 
     * @param jsonObj The JSON object to parse
     * @param name The optional field name
     * @return The optional field's value or <code>null</code> if there's no such field
     * @throws JSONException If a JSON error occurs
     */
    private static String parseString(final JSONObject jsonObj, final String name) throws JSONException {
        String retval = null;
        if (jsonObj.hasAndNotNull(name)) {
            final String test = jsonObj.getString(name);
            if (0 != test.length()) {
                retval = test;
            }
        }
        return retval;
    }

    private static boolean parseBoolean(final JSONObject jsonObj, final String name) throws JSONException {
        if (!jsonObj.has(name)) {
            return false;
        }

        return jsonObj.getBoolean(name);
    }

    private static int[] parseJSONIntArray(final JSONObject jsonObj, final String name) throws JSONException, OXJSONException {
        if (!jsonObj.has(name)) {
            return null;
        }

        final JSONArray tmp = jsonObj.getJSONArray(name);
        if (tmp == null) {
            return null;
        }

        try {
            final int i[] = new int[tmp.length()];
            for (int a = 0; a < tmp.length(); a++) {
                i[a] = tmp.getInt(a);
            }

            return i;
        } catch (final NumberFormatException exc) {
            throw new OXJSONException(OXJSONException.Code.INVALID_VALUE, exc, name, tmp);
        }
    }

    /**
     * Parses optional array field out of specified JSON object
     * 
     * @param jsonObj The JSON object to parse
     * @param name The optional array field's name
     * @return The optional array field's value as an array of {@link String} or <code>null</code> if there's no such field
     * @throws JSONException If a JSON error occurs
     */
    private static String[] parseJSONStringArray(final JSONObject jsonObj, final String name) throws JSONException {
        if (!jsonObj.hasAndNotNull(name)) {
            return null;
        }
        final JSONArray tmp = jsonObj.getJSONArray(name);
        final String s[] = new String[tmp.length()];
        for (int a = 0; a < tmp.length(); a++) {
            s[a] = tmp.getString(a);
        }
        return s;
    }

    private static Date[] parseJSONDateArray(final JSONObject jsonObj, final String name) throws JSONException, OXJSONException {
        if (!jsonObj.has(name)) {
            return null;
        }

        final JSONArray tmp = jsonObj.getJSONArray(name);
        if (tmp == null) {
            return null;
        }

        try {
            final Date d[] = new Date[tmp.length()];
            for (int a = 0; a < tmp.length(); a++) {
                d[a] = new Date(tmp.getLong(a));
            }

            return d;
        } catch (final NumberFormatException exc) {
            throw new OXJSONException(OXJSONException.Code.INVALID_VALUE, exc, name, tmp);
        }
    }

}
