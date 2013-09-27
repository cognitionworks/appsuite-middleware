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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

import java.util.LinkedList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.folder.json.services.ServiceRegistry;
import com.openexchange.folderstorage.FolderService;


/**
 * {@link ClearAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@Action(method = RequestMethod.PUT, name = "clear", description = "Clearing a folder's content", parameters = {
    @Parameter(name = "session", description = "A session ID previously obtained from the login module."),
    @Parameter(name = "tree", description = "(Preliminary) The identifier of the folder tree. If missing '0' (primary folder tree) is assumed."),
    @Parameter(name = "allowed_modules", description = "(Preliminary) An array of modules (either numbers or strings; e.g. \"tasks,calendar,contacts,mail\") supported by requesting client. If missing, all available modules are considered.")
}, requestBody = "A JSON array containing the folder ID(s) whose content should be cleared. NOTE: Although the requests offers to clear multiple folders at once it is recommended to clear only one folder per request since if any exception occurs (e.g. missing permissions) the complete request is going to be aborted.",
responseDescription = "A JSON array containing the IDs of folders that could not be cleared due to a concurrent modification. Meaning you receive an empty JSON array if everything worked well.")
public final class ClearAction extends AbstractFolderAction {

    public static final String ACTION = AJAXServlet.ACTION_CLEAR;

    /**
     * Initializes a new {@link ClearAction}.
     */
    public ClearAction() {
        super();
    }

    @Override
    protected AJAXRequestResult doPerform(final AJAXRequestData request, final com.openexchange.tools.session.ServerSession session) throws OXException, JSONException {
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
        /*
         * Compose JSON array with id
         */
        final JSONArray jsonArray = (JSONArray) request.requireData();
        final int len = jsonArray.length();
        /*
         * Delete
         */
        final List<OXException> warnings = new LinkedList<OXException>();
        final JSONArray responseArray = new JSONArray();
        final FolderService folderService = ServiceRegistry.getInstance().getService(FolderService.class, true);
        for (int i = 0; i < len; i++) {
            final String folderId = jsonArray.getString(i);
            try {
                folderService.clearFolder(treeId, folderId, session);
            } catch (final OXException e) {
                final org.apache.commons.logging.Log log = com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(ClearAction.class));
                log.error(e.getMessage(), e);
                responseArray.put(folderId);
                e.setCategory(com.openexchange.exception.Category.CATEGORY_WARNING);
                warnings.add(e);
            }
        }
        /*
         * Return appropriate result
         */
        return new AJAXRequestResult(responseArray).addWarnings(warnings);
    }

}
