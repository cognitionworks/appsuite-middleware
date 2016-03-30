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

package com.openexchange.ajax.requesthandler;

import static com.openexchange.ajax.requesthandler.Dispatcher.PREFIX;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link Dispatchers} - Utility class for dispatched processing.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.6.1
 */
public class Dispatchers {

    /**
     * Initializes a new {@link Dispatchers}.
     */
    private Dispatchers() {
        super();
    }

    /**
     * Triggers the given dispatcher to perform specified AJAX request data using given session.
     *
     * @param requestData The AJAX request data
     * @param dispatcher The dispatcher to use
     * @param session The associated session
     * @return The result
     * @throws OXException If operation fails
     * @see #sendResponse(DispatcherResult, HttpServletRequest, HttpServletResponse)
     */
    public static DispatcherResult perform(AJAXRequestData requestData, Dispatcher dispatcher, ServerSession session) throws OXException {
        AJAXState ajaxState = dispatcher.begin();
        return new DispatcherResult(requestData, dispatcher.perform(requestData, ajaxState, session), ajaxState, dispatcher);
    }

    /**
     * Sends a proper response to requesting client after request has been orderly dispatched.
     *
     * @param result The dispatcher result
     * @param httpRequest The associated HTTP Servlet request
     * @param httpResponse The associated HTTP Servlet response
     * @see #perform(AJAXRequestData, Dispatcher, ServerSession)
     */
    public static void sendResponse(DispatcherResult result, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            DispatcherServlet.sendResponse(result.getRequestData(), result.getRequestResult(), httpRequest, httpResponse);
        } finally {
            result.close();
        }
    }

    /**
     * Gets the action associated with given HTTP request
     *
     * @param req The HTTP request
     * @return The associated action string
     */
    public static String getActionFrom(HttpServletRequest req) {
        String action = req.getParameter(AJAXServlet.PARAMETER_ACTION);
        return null == action ? Strings.toUpperCase(req.getMethod()) : action;
    }

    /**
     * Check if common API response is expected for specified HTTP request
     *
     * @param req The HTTP request to check
     * @return <code>true</code> if common API response is expected; otherwise <code>false</code>
     */
    public static boolean isApiOutputExpectedFor(HttpServletRequest req) {
        String prefix = getPrefix();
        if (req.getRequestURI().startsWith(prefix)) {
            // Common dispatcher action - Try to determine if JSON is expected or not
            AJAXRequestDataTools requestDataTools = AJAXRequestDataTools.getInstance();
            String module = requestDataTools.getModule(PREFIX.get(), req);
            AJAXActionServiceFactory factory = DispatcherServlet.getDispatcher().lookupFactory(module);
            if (factory != null) {
                return isApiOutputExpectedFor(optActionFor(requestDataTools.getAction(req), factory));
            }
        }
        return true;
    }

    /**
     * Gets the dispatcher's path prefix
     *
     * @return The prefix
     */
    public static String getPrefix() {
        String prefix = DispatcherServlet.getPrefix();
        return prefix == null ? DefaultDispatcherPrefixService.DEFAULT_PREFIX : prefix;
    }

    private static AJAXActionService optActionFor(String sAction, final AJAXActionServiceFactory factory) {
        try {
            return factory.createActionService(sAction);
        } catch (OXException e) {
            return null;
        }
    }

    /**
     * Check if common API response is expected for specified AJAX action.
     *
     * @param action The AJAX action to check
     * @return <code>true</code> if no common API response is expected; otherwise <code>false</code>
     */
    public static boolean isApiOutputExpectedFor(AJAXActionService action) {
        if (null == action) {
            return true;
        }

        if ((action instanceof ETagAwareAJAXActionService) || (action instanceof LastModifiedAwareAJAXActionService)) {
            return false;
        }

        DispatcherNotes dispatcherNotes = action.getClass().getAnnotation(DispatcherNotes.class);
        if (null != dispatcherNotes && "file".equals(dispatcherNotes.defaultFormat())) {
            return false;
        }

        return true;
    }

}
