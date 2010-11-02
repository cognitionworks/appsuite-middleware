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

package com.openexchange.file.storage.json.actions.files;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.file.storage.AbstractFileFieldHandler;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.composition.IDBasedFileAccess;
import com.openexchange.file.storage.meta.FileFieldSet;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.attach.AttachmentBase;
import com.openexchange.groupware.attach.AttachmentField;
import com.openexchange.groupware.attach.AttachmentMetadata;
import com.openexchange.groupware.attach.util.GetSwitch;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link SaveAsAction}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class SaveAsAction extends AbstractWriteAction {

    @Override
    public AJAXRequestResult handle(InfostoreRequest request) throws AbstractOXException {
        request.require(Param.FOLDER_ID, Param.ATTACHED_ID, Param.MODULE, Param.ATTACHMENT).requireFileMetadata();

        final int folderId = Integer.parseInt(request.getFolderId());
        final int attachedId = request.getAttachedId();
        final int moduleId = request.getModule();
        final int attachment = request.getAttachment();

        final File file = request.getFile();
        final List<Field> sentColumns = request.getSentColumns();

        AttachmentBase attachments = request.getAttachmentBase();
        IDBasedFileAccess fileAccess = request.getFileAccess();

        ServerSession session = request.getSession();

        final AttachmentMetadata att = attachments.getAttachment(
            folderId,
            attachedId,
            moduleId,
            attachment,
            session.getContext(),
            session.getUser(),
            session.getUserConfiguration());

        final FileFieldSet fileSet = new FileFieldSet();
        final GetSwitch attGet = new GetSwitch(att);

        File.Field.forAllFields(new AbstractFileFieldHandler() {

            public Object handle(Field field, Object... args) {

                if (sentColumns.contains(field)) {
                    return null; // SKIP
                }

                // Otherwise copy from attachment

                AttachmentField matchingAttachmentField = getMatchingAttachmentField(field);
                if (matchingAttachmentField == null) {
                    return null; // Not a field to copy
                }

                final Object value = matchingAttachmentField.doSwitch(attGet);
                field.doSwitch(fileSet, file, value);

                return null;
            }

        });

        file.setId(FileStorageFileAccess.NEW);
        InputStream fileData = attachments.getAttachedFile(
            folderId,
            attachedId,
            moduleId,
            attachment,
            session.getContext(),
            session.getUser(),
            session.getUserConfiguration());

        fileAccess.saveDocument(file, fileData, FileStorageFileAccess.UNDEFINED_SEQUENCE_NUMBER);
        
        return new AJAXRequestResult(file.getId(), new Date(file.getSequenceNumber()));
    }

    protected AttachmentField getMatchingAttachmentField(File.Field fileField) {
        switch(fileField) {
        case FILENAME : return AttachmentField.FILENAME_LITERAL;
        case FILE_SIZE : return AttachmentField.FILE_SIZE_LITERAL;
        case FILE_MIMETYPE : return AttachmentField.FILE_MIMETYPE_LITERAL;
        case TITLE : return AttachmentField.FILENAME_LITERAL;
        case DESCRIPTION : return AttachmentField.COMMENT_LITERAL;
        default: return null;
        }
    }

}
