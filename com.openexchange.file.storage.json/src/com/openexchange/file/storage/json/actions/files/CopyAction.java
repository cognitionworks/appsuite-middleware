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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Actions;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.DefaultFileStorageFolder;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.file.storage.FileStorageFolder;
import com.openexchange.file.storage.FileStoragePermission;
import com.openexchange.file.storage.composition.FolderID;
import com.openexchange.file.storage.composition.IDBasedFileAccess;
import com.openexchange.file.storage.composition.IDBasedFolderAccess;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;


/**
 * {@link CopyAction}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
@Actions({@Action(method = RequestMethod.PUT, name = "copy", description = "Copy an infoitem via PUT", parameters = {
    @Parameter(name = "session", description = "A session ID previously obtained from the login module."),
    @Parameter(name = "id", description = "Object ID of the updated infoitem."),
    @Parameter(name = "folder", description = "The Folder of the Object."),
    @Parameter(name = "timestamp", description = "Timestamp of the infostore object.")
}, requestBody = "Infoitem object as described in Common object data and Detailed infoitem data. Only modified fields are present.",
responseDescription = "The id of the newly created object."),
@Action(method = RequestMethod.POST, name = "copy", description = "Copy an infoitem via POST", parameters = {
    @Parameter(name = "session", description = "A session ID previously obtained from the login module."),
    @Parameter(name = "id", description = "Object ID of the updated infoitem."),
    @Parameter(name = "timestamp", description = "Timestamp of the updated infoitem. If the infoitem was modified after the specified timestamp, then the update must fail."),
    @Parameter(name = "json", description = "Infoitem object as described in Common object data and Detailed infoitem data. Only modified fields are present."),
    @Parameter(name = "file", description = "File metadata as per <input type=\"file\" />")
}, requestBody = "Body of content-type \"multipart/form-data\" or \"multipart/mixed\" containing the above mentioned fields and file-data.",
responseDescription = "The response is sent as a HTML document (see introduction).")})
public class CopyAction extends AbstractWriteAction {

    @Override
    public AJAXRequestResult handle(InfostoreRequest request) throws OXException {
        List<IdVersionPair> pairs = request.optIdVersionPairs();
        if (null != pairs) {
            return handlePairs(pairs, request);
        }

        // The old way...
        request.require(Param.ID).requireFileMetadata();

        IDBasedFileAccess fileAccess = request.getFileAccess();
        String id = request.getId();
        File file = request.getFile();
        String folder = null != file.getFolderId() ? file.getFolderId() : request.getFolderId();
        String version = request.getVersion();

        String newId;
        if (request.hasUploads()) {
            newId = fileAccess.copy(id, version, folder, file, request.getUploadedFileData(), request.getSentColumns());
        } else {
            newId = fileAccess.copy(id, version, folder, file, null, request.getSentColumns());
        }

        return new AJAXRequestResult(newId, new Date(file.getSequenceNumber()));
    }

    private AJAXRequestResult handlePairs(List<IdVersionPair> pairs, InfostoreRequest request) throws OXException {
        request.require(Param.FOLDER_ID);

        IDBasedFileAccess fileAccess = request.getFileAccess();
        IDBasedFolderAccess folderAccess = request.getFolderAccess();
        String destFolder = request.getFolderId();

        List<String> newFiles = new LinkedList<String>();
        List<String> newFolders = new LinkedList<String>();

        boolean error = true;
        try {
            for (IdVersionPair pair : pairs) {
                if (pair.getIdentifier() == null) {
                    // Resource denotes a folder
                    String folderId = pair.getFolderId();
                    FileStorageFolder srcFolder = folderAccess.getFolder(new FolderID(folderId));

                    DefaultFileStorageFolder newFolder = new DefaultFileStorageFolder();
                    newFolder.setName(srcFolder.getName());
                    newFolder.setParentId(destFolder);
                    newFolder.setSubscribed(srcFolder.isSubscribed());
                    for (FileStoragePermission permission : srcFolder.getPermissions()) {
                        newFolder.addPermission(permission);
                    }

                    String newFolderID = folderAccess.createFolder(srcFolder);
                    newFolders.add(newFolderID);

                    TimedResult<File> documents = fileAccess.getDocuments(folderId);
                    SearchIterator<File> iter = documents.results();
                    try {
                        while (iter.hasNext()) {
                            File file = iter.next();
                            fileAccess.copy(file.getId(), file.getVersion(), newFolderID, null, null, null);
                        }
                    } finally {
                        SearchIterators.close(iter);
                    }
                } else {
                    // Resource denotes a file
                    String id = pair.getIdentifier();
                    String version = pair.getVersion();
                    String newFileId = fileAccess.copy(id, version, destFolder, null, null, null);
                    newFiles.add(newFileId);
                }
            }
            error = false;
            return new AJAXRequestResult(Boolean.TRUE, "native");
        } finally {
            if (error) {
                for (String folderId : newFolders) {
                    try { folderAccess.deleteFolder(folderId, true); } catch (Exception e) {/* ignore */}
                }
                for (String fileId : newFiles) {
                    try { fileAccess.removeDocument(Collections.singletonList(fileId), FileStorageFileAccess.DISTANT_FUTURE, true); } catch (Exception e) {/* ignore */}
                }
            }
        }
    }

}
