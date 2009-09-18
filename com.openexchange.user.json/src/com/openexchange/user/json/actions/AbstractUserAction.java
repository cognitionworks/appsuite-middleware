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

import java.util.regex.Pattern;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.tools.servlet.AjaxException;

/**
 * {@link AbstractUserAction} - An abstract user action.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class AbstractUserAction implements AJAXActionService {

    /**
     * Initializes a new {@link AbstractUserAction}.
     */
    protected AbstractUserAction() {
        super();
    }

    /**
     * Parses specified parameter into an <code>Long</code>.
     * 
     * @param parameterName The parameter name
     * @param request The request
     * @return The parsed <code>Long</code> value or <code>null</code> if not present
     * @throws AjaxException If parameter is invalid in given request
     */
    protected static Long parseLongParameter(final String parameterName, final AJAXRequestData request) throws AjaxException {
        String tmp = request.getParameter(parameterName);
        if (null == tmp) {
            return null;
        }
        tmp = tmp.trim();
        try {
            return Long.valueOf(tmp);
        } catch (final NumberFormatException e) {
            throw new AjaxException(AjaxException.Code.InvalidParameterValue, parameterName, tmp);
        }
    }

    /**
     * Parses specified parameter into an <code>long</code>.
     * 
     * @param parameterName The parameter name
     * @param request The request
     * @return The parsed <code>long</code> value
     * @throws AjaxException If parameter is invalid in given request
     */
    protected static long checkLongParameter(final String parameterName, final AJAXRequestData request) throws AjaxException {
        String tmp = request.getParameter(parameterName);
        if (null == tmp) {
            throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, parameterName);
        }
        tmp = tmp.trim();
        try {
            return Long.parseLong(tmp);
        } catch (final NumberFormatException e) {
            throw new AjaxException(AjaxException.Code.InvalidParameterValue, parameterName, tmp);
        }
    }

    /**
     * Parses specified parameter into an <code>int</code>.
     * 
     * @param parameterName The parameter name
     * @param request The request
     * @return The parsed <code>int</code> value or <code>-1</code> if not present
     * @throws AjaxException If parameter is invalid in given request
     */
    protected static int parseIntParameter(final String parameterName, final AJAXRequestData request) throws AjaxException {
        String tmp = request.getParameter(parameterName);
        if (null == tmp) {
            return -1;
        }
        tmp = tmp.trim();
        try {
            return Integer.parseInt(tmp);
        } catch (final NumberFormatException e) {
            throw new AjaxException(AjaxException.Code.InvalidParameterValue, parameterName, tmp);
        }
    }

    /**
     * Parses specified parameter into an <code>int</code>.
     * 
     * @param parameterName The parameter name
     * @param request The request
     * @return The parsed <code>int</code> value
     * @throws AjaxException If parameter is not present or invalid in given request
     */
    protected static int checkIntParameter(final String parameterName, final AJAXRequestData request) throws AjaxException {
        String tmp = request.getParameter(parameterName);
        if (null == tmp) {
            throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, parameterName);
        }
        tmp = tmp.trim();
        try {
            return Integer.parseInt(tmp);
        } catch (final NumberFormatException e) {
            throw new AjaxException(AjaxException.Code.InvalidParameterValue, parameterName, tmp);
        }
    }

    private static final Pattern PAT = Pattern.compile(" *, *");

    /**
     * Parses specified parameter into an array of <code>int</code>.
     * 
     * @param parameterName The parameter name
     * @param request The request
     * @return The parsed array of <code>int</code>
     * @throws AjaxException If parameter is not present in given request
     */
    protected static int[] parseIntArrayParameter(final String parameterName, final AJAXRequestData request) throws AjaxException {
        final String tmp = request.getParameter(parameterName);
        if (null == tmp) {
            throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, parameterName);
        }
        final String[] sa = PAT.split(tmp, 0);
        final int[] columns = new int[sa.length];
        for (int i = 0; i < sa.length; i++) {
            columns[i] = Integer.parseInt(sa[i]);
        }
        return columns;
    }

    /**
     * Parses specified optional parameter into an array of <code>int</code>.
     * 
     * @param parameterName The parameter name
     * @param request The request
     * @return The parsed array of <code>int</code>; a zero length array is returned if parameter is missing
     */
    protected static int[] parseOptionalIntArrayParameter(final String parameterName, final AJAXRequestData request) {
        final String tmp = request.getParameter(parameterName);
        if (null == tmp) {
            return new int[0];
        }
        final String[] sa = PAT.split(tmp, 0);
        final int[] columns = new int[sa.length];
        for (int i = 0; i < sa.length; i++) {
            columns[i] = Integer.parseInt(sa[i]);
        }
        return columns;
    }

}
