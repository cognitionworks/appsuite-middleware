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

package com.openexchange.chat.json.rest.conversation;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.chat.json.rest.AbstractMethodHandler;
import com.openexchange.exception.OXException;
import com.openexchange.tools.servlet.AjaxExceptionCodes;

/**
 * {@link MethodHandlerImplementation}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class PostMethodHandler extends AbstractMethodHandler {

    /**
     * Initializes a new {@link PostMethodHandler}.
     */
    public PostMethodHandler() {
        super();
    }

    @Override
    protected String getModule() {
        return "conversation";
    }

    @Override
    protected void parseByPathInfo(final AJAXRequestData retval, final String pathInfo, final HttpServletRequest req) throws IOException, OXException {
        if (isEmpty(pathInfo)) {
            retval.setAction("new");
        } else {
            final String[] pathElements = SPLIT_PATH.split(pathInfo);
            final int length = pathElements.length;
            if (0 == length) {
                retval.setAction("new");
            } else if (1 == length) {
                throw AjaxExceptionCodes.BAD_REQUEST.create();
            } else if ("message".equals(pathElements[1])) {
                if (2 == length) {
                    throw AjaxExceptionCodes.BAD_REQUEST.create();
                }
                /*-
                 * "Post new message for Conv. #11"
                 *  POST /conversation/11/message
                 */
                retval.putParameter("id", pathElements[0]);
                retval.setAction("newMessage");
            } else {
                throw AjaxExceptionCodes.UNKNOWN_ACTION.create(pathInfo);
            }
        }
    }

    @Override
    protected boolean shouldApplyBody() {
        return true;
    }

}
