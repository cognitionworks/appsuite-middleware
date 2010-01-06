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

package com.openexchange.voipnow.json.actions;

import java.util.regex.Pattern;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.config.ConfigurationService;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.tools.servlet.AjaxException;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.voipnow.json.services.ServiceRegistry;

/**
 * {@link AbstractVoipNowAction} - An abstract VoipNow action.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class AbstractVoipNowAction implements AJAXActionService {

    /**
     * The HTTPS identifier constant.
     */
    protected static final String HTTPS = "https";

    /**
     * Initializes a new {@link AbstractVoipNowAction}.
     */
    protected AbstractVoipNowAction() {
        super();
    }

    /**
     * Gets the VoipNow setting for specified session.
     * 
     * @param session The session
     * @return The VoipNow setting
     * @throws AbstractOXException If returning VoipNow setting fails
     */
    protected static VoipNowServerSetting getVoipNowServerSetting(final ServerSession session) throws AbstractOXException {
        final ConfigurationService service = ServiceRegistry.getInstance().getService(ConfigurationService.class, true);

        final VoipNowServerSetting retval = new VoipNowServerSetting();
        retval.setPort(service.getIntProperty("com.4psa.voipnow.port", 443));
        retval.setHost(service.getProperty("com.4psa.voipnow.host", "localhost").trim());
        retval.setSecure(Boolean.parseBoolean(service.getProperty("com.4psa.voipnow.secure", "true").trim()));
        retval.setLogin(service.getProperty("com.4psa.voipnow.adminLogin", "").trim());
        retval.setPassword(service.getProperty("com.4psa.voipnow.adminPassword", "").trim());

        return retval;
    }

    /**
     * Parses specified parameter into an <code>Long</code>.
     * 
     * @param request The request
     * @param parameterName The parameter name
     * @return The parsed <code>Long</code> value or <code>null</code> if not present
     * @throws AjaxException If parameter is invalid in given request
     */
    protected static Long parseLongParameter(final AJAXRequestData request, final String parameterName) throws AjaxException {
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
     * @param request The request
     * @param parameterName The parameter name
     * @return The parsed <code>long</code> value
     * @throws AjaxException If parameter is invalid in given request
     */
    protected static long checkLongParameter(final AJAXRequestData request, final String parameterName) throws AjaxException {
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
     * @param request The request
     * @param parameterName The parameter name
     * @param defaultValue The default value to return if parameter is missing
     * @return The parsed <code>int</code> value
     * @throws AjaxException If parameter is invalid in given request
     */
    protected static int parseIntParameter(final AJAXRequestData request, final String parameterName, final int defaultValue) throws AjaxException {
        final int i = parseIntParameter(request, parameterName);
        return i < 0 ? defaultValue : i;
    }

    /**
     * Parses specified parameter into an <code>int</code>.
     * 
     * @param request The request
     * @param parameterName The parameter name
     * @return The parsed <code>int</code> value or <code>-1</code> if not present
     * @throws AjaxException If parameter is invalid in given request
     */
    protected static int parseIntParameter(final AJAXRequestData request, final String parameterName) throws AjaxException {
        String tmp = request.getParameter(parameterName);
        if (null == tmp) {
            return -1;
        }
        tmp = tmp.trim();
        try {
            return ActionUtility.getUnsignedInteger(tmp);
        } catch (final NumberFormatException e) {
            throw new AjaxException(AjaxException.Code.InvalidParameterValue, parameterName, tmp);
        }
    }

    /**
     * Parses specified parameter into an <code>int</code>.
     * 
     * @param request The request
     * @param parameterName The parameter name
     * @return The parsed <code>int</code> value
     * @throws AjaxException If parameter is not present or invalid in given request
     */
    protected static int checkIntParameter(final AJAXRequestData request, final String parameterName) throws AjaxException {
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

    /**
     * Parses specified parameter into an <code>String</code>.
     * 
     * @param request The request
     * @param parameterName The parameter name
     * @return The parsed <code>String</code> value
     * @throws AjaxException If parameter is not present or invalid in given request
     */
    protected static String checkStringParameter(final AJAXRequestData request, final String parameterName) throws AjaxException {
        final String tmp = request.getParameter(parameterName);
        if (null == tmp || 0 == tmp.length()) {
            throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, parameterName);
        }
        return tmp;
    }

    /**
     * Gets either of provided parameters.
     * 
     * @param request The request
     * @param parameterNames The parameter names
     * @return The parameter value at the same position as its name; others are <code>null</code>
     * @throws AjaxException If more than one or none parameter is not present
     */
    protected static String[] checkEitherOfStringParameter(final AJAXRequestData request, final String... parameterNames) throws AjaxException {
        final int length = parameterNames.length;
        final String[] retval = new String[length];
        String foundName = null;
        for (int i = 0; i < length; i++) {
            final String name = parameterNames[i];
            final String parameter = request.getParameter(name);
            if (null != parameter) {
                if (null != foundName) {
                    // There was already one of specified choices
                    throw new AjaxException(AjaxException.Code.EitherParameterConflict, name, foundName);
                }
                foundName = name;
            }
            retval[i] = parameter;
        }
        if (null == foundName) {
            throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, parameterNames[0]);
        }
        return retval;
    }

    /**
     * Parses specified parameter into an <code>String</code>.
     * 
     * @param request The request
     * @param parameterName The parameter name
     * @return The parsed <code>String</code> value
     */
    protected static String getStringParameter(final AJAXRequestData request, final String parameterName) {
        return request.getParameter(parameterName);
    }

    /**
     * The pattern to split a comma-separated string.
     */
    protected static final Pattern PAT = Pattern.compile(" *, *");

    /**
     * Parses specified parameter into an array of <code>int</code>.
     * 
     * @param request The request
     * @param parameterName The parameter name
     * @return The parsed array of <code>int</code>
     * @throws AjaxException If parameter is not present in given request
     */
    protected static int[] parseIntArrayParameter(final AJAXRequestData request, final String parameterName) throws AjaxException {
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
     * @param request The request
     * @param parameterName The parameter name
     * @return The parsed array of <code>int</code>; a zero length array is returned if parameter is missing
     */
    protected static int[] parseOptionalIntArrayParameter(final AJAXRequestData request, final String parameterName) {
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
