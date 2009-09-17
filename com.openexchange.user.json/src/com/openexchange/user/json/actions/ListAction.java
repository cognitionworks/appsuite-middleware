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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.api2.ContactInterfaceFactory;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.contact.ContactInterface;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.ldap.User;
import com.openexchange.tools.servlet.AjaxException;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.UserService;
import com.openexchange.user.json.Constants;
import com.openexchange.user.json.services.ServiceRegistry;
import com.openexchange.user.json.writer.UserWriter;

/**
 * {@link ListAction} - Maps the action to a list action.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ListAction extends AbstractUserAction {

    public static final String ACTION = AJAXServlet.ACTION_LIST;

    /**
     * Initializes a new {@link ListAction}.
     */
    public ListAction() {
        super();
    }

    public AJAXRequestResult perform(final AJAXRequestData request, final ServerSession session) throws AbstractOXException {
        try {
            /*
             * Parse parameters
             */
            final int[] userIdArray;
            {
                final JSONArray jsonArray = (JSONArray) request.getData();
                if (null == jsonArray) {
                    throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, "data");
                }
                final int len = jsonArray.length();
                userIdArray = new int[len];
                for (int a = 0; a < len; a++) {
                    userIdArray[a] = jsonArray.getInt(a);
                }
            }
            final int[] columns = parseIntArrayParameter(AJAXServlet.PARAMETER_COLUMNS, request);
            /*
             * Get services
             */
            final UserService userService = ServiceRegistry.getInstance().getService(UserService.class, true);
            final ContactInterface contactInterface =
                ServiceRegistry.getInstance().getService(ContactInterfaceFactory.class, true).create(
                    Constants.USER_ADDRESS_BOOK_FOLDER_ID,
                    session);
            /*
             * Get users/contacts
             */
            final User[] users = new User[userIdArray.length];
            final Contact[] contacts = new Contact[userIdArray.length];
            for (int i = 0; i < users.length; i++) {
                final int userId = userIdArray[i];
                users[i] = userService.getUser(userId, session.getContext());
                contacts[i] = contactInterface.getUserById(userId);
            }
            /*
             * Determine max. last-modified time stamp
             */
            Date lastModified = contacts[0].getLastModified();
            for (int i = 1; i < contacts.length; i++) {
                final Date lm = contacts[i].getLastModified();
                if (lastModified.before(lm)) {
                    lastModified = lm;
                }
            }
            /*
             * Write users as JSON arrays to JSON array
             */
            final JSONArray jsonArray = UserWriter.writeMultiple2Array(columns, users, contacts);
            /*
             * Return appropriate result
             */
            return new AJAXRequestResult(jsonArray, lastModified);
        } catch (final JSONException e) {
            throw new AjaxException(AjaxException.Code.JSONError, e, e.getMessage());
        }
    }

}
