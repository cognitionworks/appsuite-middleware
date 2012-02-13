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

import org.json.JSONObject;

import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.api2.ContactInterfaceFactory;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.ContactInterface;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.ldap.User;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.UserService;
import com.openexchange.user.json.Constants;
import com.openexchange.user.json.services.ServiceRegistry;
import com.openexchange.user.json.writer.UserWriter;

/**
 * {@link GetAction} - Maps the action to a <tt>get</tt> action.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@Action(method = RequestMethod.GET, name = "get", description = "Get a user.", parameters = { 
		@Parameter(name = "session", description = "A session ID previously obtained from the login module."),
		@Parameter(name = "id", optional = true, description = "Object ID of the requested user. Since v6.18.1, this parameter is optional: the default is the currently logged in user."),
}, responseDescription = "Response with timestamp: An object containing all data of the requested user. The fields of the object are listed in Common object data, Detailed contact data and Detailed user data.")
public final class GetAction extends AbstractUserAction {

    /**
     * The <tt>get</tt> action string.
     */
    public static final String ACTION = AJAXServlet.ACTION_GET;

    /**
     * Initializes a new {@link GetAction}.
     */
    public GetAction() {
        super();
    }

    @Override
    public AJAXRequestResult perform(final AJAXRequestData request, final ServerSession session) throws OXException {
        /*
         * Parse parameters
         */
        final String idParam = request.getParameter("id");
        int userId;
        if(null == idParam) {
            userId = session.getUserId();
        } else {
            userId = checkIntParameter("id", request);
        }
        /*
         * Obtain user from user service
         */
        final UserService userService = ServiceRegistry.getInstance().getService(UserService.class, true);
        final User user = userService.getUser(userId, session.getContext());
        /*
         * Obtain user's contact
         */
        final ContactInterface contactInterface =
            ServiceRegistry.getInstance().getService(ContactInterfaceFactory.class, true).create(
                Constants.USER_ADDRESS_BOOK_FOLDER_ID,
                session);
        final Contact userContact = contactInterface.getUserById(userId, false);
        final String timeZoneId = request.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
        /*
         * Write user as JSON object
         */
        censor(session, userContact);
        final JSONObject jsonObject = UserWriter.writeSingle2Object(null, null, censor(session, user), userContact, timeZoneId);
        /*
         * Return appropriate result
         */
        return new AJAXRequestResult(jsonObject, userContact.getLastModified());
    }

}
