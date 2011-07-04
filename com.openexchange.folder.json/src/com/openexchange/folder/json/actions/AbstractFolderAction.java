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

package com.openexchange.folder.json.actions;

import static com.openexchange.folder.json.Tools.getUnsignedInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.folder.json.services.ServiceRegistry;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.tools.servlet.AjaxException;
import com.openexchange.tools.servlet.AjaxExceptionCodes;

/**
 * {@link AbstractFolderAction} - An abstract folder action.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class AbstractFolderAction implements AJAXActionService {

    /**
     * Initializes a new {@link AbstractFolderAction}.
     */
    protected AbstractFolderAction() {
        super();
    }

    /**
     * Gets the default tree identifier to use if request does not provide any.
     * 
     * @return The default tree identifier
     */
    protected static String getDefaultTreeIdentifier() {
        return FolderStorage.REAL_TREE_ID;
    }

    /**
     * Gets the default allowed modules.
     * 
     * @return The default allowed modules
     */
    protected static List<ContentType> getDefaultAllowedModules() {
        return Collections.emptyList();
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
            throw AjaxExceptionCodes.MISSING_PARAMETER.create( parameterName);
        }
        final String[] sa = PAT.split(tmp, 0);
        final int[] columns = new int[sa.length];
        for (int i = 0; i < sa.length; i++) {
            columns[i] = getUnsignedInteger(sa[i]);
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
            columns[i] = getUnsignedInteger(sa[i]);
        }
        return columns;
    }

    /**
     * Parses the optional content type array parameter. Return {@link #getDefaultAllowedModules()} if not present.
     * 
     * @param parameterName The parameter name
     * @param request The request
     * @return The parsed array of {@link ContentType} as a list.
     * @throws AbstractOXException If an invalid content type is denoted
     */
    protected static List<ContentType> parseOptionalContentTypeArrayParameter(final String parameterName, final AJAXRequestData request) throws AbstractOXException {
        final String tmp = request.getParameter(parameterName);
        if (null == tmp) {
            return getDefaultAllowedModules();
        }
        final String[] sa = PAT.split(tmp, 0);
        final List<ContentType> ret = new ArrayList<ContentType>(sa.length);
        /*
         * Get available content types
         */
        final Map<Integer, ContentType> availableContentTypes =
            ServiceRegistry.getInstance().getService(FolderService.class, true).getAvailableContentTypes();
        Map<String, ContentType> tmpMap = null;
        for (final String str : sa) {
            final int module = getUnsignedInteger(str);
            if (module < 0) {
                /*
                 * Not a number
                 */
                if (null == tmpMap) {
                    tmpMap = new HashMap<String, ContentType>(availableContentTypes.size());
                    for (final ContentType ct : availableContentTypes.values()) {
                        tmpMap.put(ct.toString(), ct);
                    }
                }
                final ContentType ct = tmpMap.get(str);
                if (null == ct) {
                    org.apache.commons.logging.LogFactory.getLog(AbstractFolderAction.class).error("No content type for string: " + str);
                    throw AjaxExceptionCodes.InvalidParameterValue.create( parameterName, tmp);
                }
                ret.add(ct);
            } else {
                final Integer key = Integer.valueOf(module);
                final ContentType ct = availableContentTypes.get(key);
                if (null == ct) {
                    org.apache.commons.logging.LogFactory.getLog(AbstractFolderAction.class).error("No content type for module: " + key);
                    throw AjaxExceptionCodes.InvalidParameterValue.create( parameterName, tmp);
                }
                ret.add(ct);
            }
        }
        return ret;
    }

    protected static ContentType parseContentTypeParameter(final String parameterName, final AJAXRequestData request) throws AbstractOXException {
        final String tmp = request.getParameter(parameterName);
        if (null == tmp) {
            return null;
        }
        /*
         * Get available content types
         */
        final Map<Integer, ContentType> availableContentTypes =
            ServiceRegistry.getInstance().getService(FolderService.class, true).getAvailableContentTypes();
        final int module = getUnsignedInteger(tmp);
        if (module < 0) {
            /*
             * Not a number
             */
            for (final Map.Entry<Integer, ContentType> entry : availableContentTypes.entrySet()) {
                final ContentType ct = entry.getValue();
                if (ct.toString().equals(tmp)) {
                    return ct;
                }
            }
            /*
             * Not found
             */
            org.apache.commons.logging.LogFactory.getLog(AbstractFolderAction.class).error("No content type for module: " + tmp);
            throw AjaxExceptionCodes.InvalidParameterValue.create( parameterName, tmp);
        }
        /*
         * A number
         */
        final Integer key = Integer.valueOf(module);
        final ContentType ct = availableContentTypes.get(key);
        if (null == ct) {
            org.apache.commons.logging.LogFactory.getLog(AbstractFolderAction.class).error("No content type for module: " + key);
            throw AjaxExceptionCodes.InvalidParameterValue.create( parameterName, tmp);
        }
        return ct;
    }

}
