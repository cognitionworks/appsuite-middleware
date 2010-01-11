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

package com.openexchange.folder.json.actions;

import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.folder.json.Constants;
import com.openexchange.folder.json.FolderField;
import com.openexchange.folder.json.Tools;
import com.openexchange.folder.json.services.ServiceRegistry;
import com.openexchange.folder.json.writer.FolderWriter;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.ContentTypeDiscoveryService;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.folderstorage.FolderServiceDecorator;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.tools.servlet.AjaxException;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link UpdatesAction} - Maps the action to a UPDATES action.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class UpdatesAction extends AbstractFolderAction {

    public static final String ACTION = AJAXServlet.ACTION_UPDATES;

    /**
     * Initializes a new {@link UpdatesAction}.
     */
    public UpdatesAction() {
        super();
    }

    public AJAXRequestResult perform(final AJAXRequestData request, final ServerSession session) throws AbstractOXException {
        /*
         * Parse parameters
         */
        String treeId = request.getParameter("tree");
        if (null == treeId) {
            /*
             * Fallback to default tree identifier
             */
            treeId = FolderStorage.REAL_TREE_ID;
        }
        final Date timestamp;
        {
            final String timestampStr = request.getParameter("timestamp");
            if (null == timestampStr) {
                throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, "timestamp");
            }
            try {
                timestamp = new Date(Long.parseLong(timestampStr));
            } catch (final NumberFormatException e) {
                throw new AjaxException(AjaxException.Code.InvalidParameterValue, "timestamp", timestampStr);
            }
        }
        final int[] columns = parseIntArrayParameter(AJAXServlet.PARAMETER_COLUMNS, request);
        final boolean ignoreDeleted = "deleted".equalsIgnoreCase(request.getParameter(AJAXServlet.PARAMETER_IGNORE));
        final boolean includeMail;
        {
            final String parameter = request.getParameter("mail");
            includeMail = "1".equals(parameter) || Boolean.parseBoolean(parameter);
        }
        final String timeZoneId = request.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
        /*
         * Request subfolders from folder service
         */
        final FolderService folderService = ServiceRegistry.getInstance().getService(FolderService.class, true);
        final UserizedFolder[][] result = folderService.getUpdates(
            treeId,
            timestamp,
            ignoreDeleted,
            includeMail ? new ContentType[] { ServiceRegistry.getInstance().getService(ContentTypeDiscoveryService.class).getByString(
                "mail") } : null,
            session, timeZoneId == null ? null : new FolderServiceDecorator().setTimeZone(Tools.getTimeZone(timeZoneId)));
        /*
         * Determine last-modified time stamp
         */
        long lastModified = timestamp.getTime();
        for (final UserizedFolder userizedFolder : result[0]) {
            final Date modified = userizedFolder.getLastModifiedUTC();
            if (modified != null) {
                final long time = modified.getTime();
                lastModified = ((lastModified >= time) ? lastModified : time);
            }
        }
        for (final UserizedFolder userizedFolder : result[1]) {
            final Date modified = userizedFolder.getLastModifiedUTC();
            if (modified != null) {
                final long time = modified.getTime();
                lastModified = ((lastModified >= time) ? lastModified : time);
            }
        }
        /*
         * Write subfolders as JSON arrays to JSON array
         */
        final JSONArray resultArray = FolderWriter.writeMultiple2Array(columns, result[0], session, Constants.ADDITIONAL_FOLDER_FIELD_LIST);
        try {
            final JSONArray jsonArray2 = FolderWriter.writeMultiple2Array(new int[] { FolderField.ID.getColumn() }, result[1], session, Constants.ADDITIONAL_FOLDER_FIELD_LIST);
            final int len = jsonArray2.length();
            for (int i = 0; i < len; i++) {
                resultArray.put(jsonArray2.getJSONArray(i));
            }
        } catch (final JSONException e) {
            throw new AjaxException(AjaxException.Code.JSONError, e, e.getMessage());
        }
        /*
         * Return appropriate result
         */
        return new AJAXRequestResult(resultArray, 0 == lastModified ? null : new Date(lastModified));
    }

}
