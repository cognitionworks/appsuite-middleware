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

package com.openexchange.file.storage.json.actions.files;

import static com.openexchange.java.Autoboxing.L;
import java.io.IOException;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestDataTools;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.DispatcherNotes;
import com.openexchange.ajax.zip.ZipUtility;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.composition.IDBasedFileAccess;
import com.openexchange.file.storage.composition.IDBasedFolderAccess;
import com.openexchange.file.storage.json.ziputil.ZipMaker;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.tools.servlet.AjaxExceptionCodes;

/**
 * {@link ZipDocumentsAction}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
@DispatcherNotes(defaultFormat = "file", allowPublicSession = true)
public class ZipDocumentsAction extends AbstractFileAction {

    /**
     * Initializes a new {@link ZipDocumentsAction}.
     */
    public ZipDocumentsAction() {
        super();
    }

    @Override
    public AJAXRequestResult handle(InfostoreRequest request) throws OXException {
        // Get/parse IDs
        List<IdVersionPair> idVersionPairs;
        try {
            Object data = request.getRequestData().getData();
            if (data instanceof JSONArray) {
                idVersionPairs = parsePairs((JSONArray) data);
            } else {
                String value = request.getParameter("body");
                if (Strings.isEmpty(value)) {
                    idVersionPairs = request.getIdVersionPairs();
                } else {
                    idVersionPairs = parsePairs(new JSONArray(value));
                }
            }
        } catch (JSONException e) {
            throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create(e, "body", e.getMessage());
        }

        boolean recursive;
        {
            String tmp = request.getParameter("recursive");
            recursive = AJAXRequestDataTools.parseBoolParameter(tmp);
        }

        // Get file/folder access
        IDBasedFileAccess fileAccess = request.getFileAccess();
        IDBasedFolderAccess folderAccess = request.getFolderAccess();
        // Perform scan
        scan(request, idVersionPairs, fileAccess, folderAccess, recursive);
        // Initialize ZIP maker for folder resource
        ZipMaker zipMaker = new ZipMaker(idVersionPairs, recursive, fileAccess, folderAccess);

        // Check against size threshold
        zipMaker.checkThreshold(threshold());

        AJAXRequestData ajaxRequestData = request.getRequestData();
        if (ZipUtility.setHttpResponseHeaders("documents.zip", ajaxRequestData)) {
            // Write ZIP archive
            long bytesWritten = 0;
            try {
                bytesWritten = zipMaker.writeZipArchive(ajaxRequestData.optOutputStream());
            } catch (IOException e) {
                throw AjaxExceptionCodes.IO_ERROR.create(e, e.getMessage());
            }

            // Signal direct response
            AJAXRequestResult result = new AJAXRequestResult(AJAXRequestResult.DIRECT_OBJECT, "direct").setType(AJAXRequestResult.ResultType.DIRECT);
            if (bytesWritten != 0) {
                result.setResponseProperty("X-Content-Size", L(bytesWritten));
            }
            return result;
        }

        // No direct response possible. Create ThresholdFileHolder...
        ThresholdFileHolder fileHolder = ZipUtility.prepareThresholdFileHolder("documents.zip");
        try {
            // Create ZIP archive
            zipMaker.writeZipArchive(fileHolder.asOutputStream());

            ajaxRequestData.setFormat("file");
            AJAXRequestResult requestResult = new AJAXRequestResult(fileHolder, "file");
            fileHolder = null;
            return requestResult;
        } finally {
            Streams.close(fileHolder);
        }
    }

}
