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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.folder.json.Constants;
import com.openexchange.folder.json.Tools;
import com.openexchange.folder.json.services.ServiceRegistry;
import com.openexchange.folder.json.writer.FolderWriter;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.FolderResponse;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.folderstorage.FolderServiceDecorator;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.folderstorage.type.SharedType;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.tools.servlet.AjaxException;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link VisibleFoldersAction} - Maps the action to a <code>allVisible</code> action.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class VisibleFoldersAction extends AbstractFolderAction {

    public static final String ACTION = "allVisible";

    /**
     * Initializes a new {@link VisibleFoldersAction}.
     */
    public VisibleFoldersAction() {
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
            treeId = getDefaultTreeIdentifier();
        }
        final ContentType contentType = parseContentTypeParameter(AJAXServlet.PARAMETER_CONTENT_TYPE, request);
        if (null == contentType) {
            throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, AJAXServlet.PARAMETER_CONTENT_TYPE);
        }
        final int[] columns = parseIntArrayParameter(AJAXServlet.PARAMETER_COLUMNS, request);
        final boolean all;
        {
            final String parameter = request.getParameter(AJAXServlet.PARAMETER_ALL);
            all = "1".equals(parameter) || Boolean.parseBoolean(parameter);
        }
        final String timeZoneId = request.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
        final java.util.List<ContentType> allowedContentTypes = parseOptionalContentTypeArrayParameter("allowed_modules", request);
        /*
         * Get folder service
         */
        final FolderService folderService = ServiceRegistry.getInstance().getService(FolderService.class, true);
        /*
         * Get all private folders
         */
        final FolderResponse<UserizedFolder[]> privateResp =
            folderService.getVisibleFolders(
                treeId,
                contentType,
                PrivateType.getInstance(),
                all,
                session,
                new FolderServiceDecorator().setTimeZone(Tools.getTimeZone(timeZoneId)).setAllowedContentTypes(allowedContentTypes));
        /*
         * Get all shared folders
         */
        final FolderResponse<UserizedFolder[]> sharedResp =
            folderService.getVisibleFolders(
                treeId,
                contentType,
                SharedType.getInstance(),
                all,
                session,
                new FolderServiceDecorator().setTimeZone(Tools.getTimeZone(timeZoneId)).setAllowedContentTypes(allowedContentTypes));
        /*
         * Get all public folders
         */
        final FolderResponse<UserizedFolder[]> publicResp =
            folderService.getVisibleFolders(
                treeId,
                contentType,
                PublicType.getInstance(),
                all,
                session,
                new FolderServiceDecorator().setTimeZone(Tools.getTimeZone(timeZoneId)).setAllowedContentTypes(allowedContentTypes));
        /*
         * Determine max. last-modified time stamp
         */
        long lastModified = 0;
        final UserizedFolder[] privateFolders = privateResp.getResponse();
        if (null != privateFolders) {
            for (final UserizedFolder userizedFolder : privateFolders) {
                final Date modified = userizedFolder.getLastModifiedUTC();
                if (modified != null) {
                    final long time = modified.getTime();
                    lastModified = ((lastModified >= time) ? lastModified : time);
                }
            }
        }
        final UserizedFolder[] sharedFolders = sharedResp.getResponse();
        if (null != sharedFolders) {
            for (final UserizedFolder userizedFolder : sharedFolders) {
                final Date modified = userizedFolder.getLastModifiedUTC();
                if (modified != null) {
                    final long time = modified.getTime();
                    lastModified = ((lastModified >= time) ? lastModified : time);
                }
            }
        }
        final UserizedFolder[] publicFolders = publicResp.getResponse();
        if (null != publicFolders) {
            for (final UserizedFolder userizedFolder : publicFolders) {
                final Date modified = userizedFolder.getLastModifiedUTC();
                if (modified != null) {
                    final long time = modified.getTime();
                    lastModified = ((lastModified >= time) ? lastModified : time);
                }
            }
        }
        /*
         * Write subfolders as JSON arrays to JSON object
         */
        try {
            final JSONObject ret = new JSONObject();
            if (null != privateFolders && privateFolders.length > 0) {
                ret.put(
                    "private",
                    FolderWriter.writeMultiple2Array(columns, privateFolders, session, Constants.ADDITIONAL_FOLDER_FIELD_LIST));
            }
            if (null != publicFolders && publicFolders.length > 0) {
                ret.put("public", FolderWriter.writeMultiple2Array(columns, publicFolders, session, Constants.ADDITIONAL_FOLDER_FIELD_LIST));
            }
            if (null != sharedFolders && sharedFolders.length > 0) {
                ret.put("shared", FolderWriter.writeMultiple2Array(columns, sharedFolders, session, Constants.ADDITIONAL_FOLDER_FIELD_LIST));
            }
            /*
             * Return appropriate result
             */
            return new AJAXRequestResult(ret, 0 == lastModified ? null : new Date(lastModified)).addWarnings(gather(
                privateResp,
                publicResp,
                sharedResp));
        } catch (JSONException e) {
            throw new AjaxException(AjaxException.Code.JSONError, e, e.getMessage());
        }
    }

    private static Collection<AbstractOXException> gather(final FolderResponse<UserizedFolder[]>... folderResponses) {
        final List<AbstractOXException> ret = new ArrayList<AbstractOXException>(4);
        for (final FolderResponse<UserizedFolder[]> fr : folderResponses) {
            if (null != fr) {
                ret.addAll(fr.getWarnings());
            }
        }
        return ret;
    }

}
