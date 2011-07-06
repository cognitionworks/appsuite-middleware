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

package com.openexchange.file.storage.json.actions.files;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.file.storage.FileStorageFileAccess.SortDirection;
import com.openexchange.file.storage.composition.IDBasedFileAccess;
import com.openexchange.file.storage.json.FileMetadataParser;
import com.openexchange.file.storage.json.actions.files.AbstractFileAction.Param;
import com.openexchange.file.storage.json.services.Services;
import com.openexchange.groupware.attach.AttachmentBase;
import com.openexchange.groupware.infostore.utils.InfostoreConfigUtils;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.groupware.upload.impl.UploadSizeExceededException;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link AJAXInfostoreRequest}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class AJAXInfostoreRequest implements InfostoreRequest {

    protected AJAXRequestData data;

    private List<Field> columns;

    private Field sortingField;

    private final ServerSession session;

    private final Map<String, String> folderMapping = new HashMap<String, String>();

    private List<String> ids = null;

    private int[] versions;

    private static final FileMetadataParser parser = FileMetadataParser.getInstance();

    private static final String JSON = "json";
    
    private File file;
    
    private List<File.Field> fields;

    private IDBasedFileAccess files;

    private List<String> folders = null;
    
    public AJAXInfostoreRequest(final AJAXRequestData requestData, final ServerSession session) {
        this.data = requestData;
        this.session = session;
    }

    public InfostoreRequest require(final Param... params) throws OXException {
        final String[] names = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            names[i] = params[i].getName();
        }
        final List<String> missingParameters = data.getMissingParameters(names);
        if (!missingParameters.isEmpty()) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create( missingParameters.toString());
        }
        return this;
    }

    public InfostoreRequest requireBody() throws OXException {
        if (data.getData() == null && !data.hasUploads() && data.getParameter("json") == null) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create( "data");
        }
        return this;
    }
    
    public boolean has(final String paramName) {
        return data.getParameter(paramName) != null;
    }
    
    public InfostoreRequest requireFileMetadata() throws OXException {
        return requireBody(); 
    }

    public String getFolderId() throws OXException {
        return data.getParameter(Param.FOLDER_ID.getName());
    }

    public List<Field> getColumns() throws OXException {
        if (columns != null) {
            return columns;
        }

        final String parameter = data.getParameter(Param.COLUMNS.getName());
        if (parameter == null || parameter.equals("")) {
            return columns = Arrays.asList(File.Field.values());
        }
        final String[] columnStrings = parameter.split("\\s*,\\s*");
        final List<Field> fields = new ArrayList<Field>(columnStrings.length);
        final List<String> unknownColumns = new ArrayList<String>(columnStrings.length);

        for (final String columnNumberOrName : columnStrings) {
            final Field field = Field.get(columnNumberOrName);
            if (field == null) {
                unknownColumns.add(columnNumberOrName);
            } else {
                fields.add(field);
            }
        }

        if (!unknownColumns.isEmpty()) {
            throw AjaxExceptionCodes.InvalidParameterValue.create( Param.COLUMNS.getName(), unknownColumns.toString());
        }

        return columns = fields;
    }

    public Field getSortingField() throws OXException {
        if (sortingField != null) {
            return sortingField;
        }
        final String sort = data.getParameter(Param.SORT.getName());
        if (sort == null) {
            return null;
        }
        final Field field = sortingField = Field.get(sort);
        if (field == null) {
            throw AjaxExceptionCodes.InvalidParameterValue.create( Param.SORT.getName(), sort);
        }
        return field;
    }

    public SortDirection getSortingOrder() throws OXException {
        final SortDirection sortDirection = SortDirection.get(data.getParameter(Param.ORDER.getName()));
        if (sortDirection == null) {
            throw AjaxExceptionCodes.InvalidParameterValue.create( Param.ORDER.getName(), sortDirection);
        }
        return sortDirection;
    }

    public TimeZone getTimezone() throws OXException {
        String parameter = data.getParameter(Param.TIMEZONE.getName());
        if (parameter == null) {
            parameter = getSession().getUser().getTimeZone();
        }
        return TimeZone.getTimeZone(parameter);
    }

    public IDBasedFileAccess getFileAccess() {
        if(files != null) {
            return files;
        }
        return files = Services.getFileAccessFactory().createAccess(session);
    }
    
    public AttachmentBase getAttachmentBase() {
        return Services.getAttachmentBase();
    }

    public ServerSession getSession() {
        return session;
    }

    public String getId() {
        return data.getParameter(Param.ID.getName());
    }

    public int getVersion() {
        final String parameter = data.getParameter(Param.VERSION.getName());
        if (parameter == null) {
            return FileStorageFileAccess.CURRENT_VERSION;
        }
        return Integer.parseInt(parameter);
    }

    public Set<String> getIgnore() {
        final String parameter = data.getParameter(Param.IGNORE.getName());
        if (parameter == null) {
            return Collections.emptySet();
        }

        return new HashSet<String>(Arrays.asList(parameter.split("\\s*,\\s*")));
    }

    public long getTimestamp() {
        final String parameter = data.getParameter(Param.TIMESTAMP.getName());
        if (parameter == null) {
            return FileStorageFileAccess.UNDEFINED_SEQUENCE_NUMBER;
        }

        return Long.parseLong(parameter);
    }

    public List<String> getIds() throws OXException {
        parseIDList();
        return ids;
    }

    public String getFolderForID(final String id) throws OXException {
        parseIDList();
        return folderMapping.get(id);
    }

    private void parseIDList() throws OXException {
        try {
            if (ids != null) {
                return;
            }
            final JSONArray array = (JSONArray) data.getData();
            ids = new ArrayList<String>(array.length());
            folders = new ArrayList<String>(array.length());
            for (int i = 0, size = array.length(); i < size; i++) {
                final JSONObject tuple = array.getJSONObject(i);
                final String id = tuple.getString(Param.ID.getName());
                ids.add(id);
                folders.add(tuple.optString(Param.FOLDER_ID.getName()));
                folderMapping.put(id, tuple.optString(Param.FOLDER_ID.getName()));
            }
        } catch (final JSONException x) {
            throw AjaxExceptionCodes.JSONError.create( x.getMessage());
        }

    }
    
    public String getFolderAt(final int index) {
        return folders.get(index);
    }
    
    public List<String> getFolders() {
        return folders;
    }

    public int[] getVersions() throws OXException {
        if (versions != null) {
            return versions;
        }
        final JSONArray body = (JSONArray) data.getData();

        try {
            versions = new int[body.length()];
            for (int i = 0; i < versions.length; i++) {
                versions[i] = body.getInt(i);
            }
        } catch (final JSONException x) {
            throw AjaxExceptionCodes.JSONError.create( x.getMessage());
        }
        return versions;
    }

    public long getDiff() {
        final String parameter = data.getParameter(Param.DIFF.getName());
        if (parameter == null) {
            return -1;
        }
        return Long.parseLong(parameter);
    }

    public int getStart() {
        final String parameter = data.getParameter("start");
        if(parameter == null ) {
            if(data.getParameter("limit") != null){
                return 0;
            }
            return FileStorageFileAccess.NOT_SET;
        }
        return Integer.valueOf(parameter);
    }

    public int getEnd() {
        String parameter = data.getParameter("end");
        if(parameter == null) {
            parameter = data.getParameter("limit");
            if(parameter == null) {
                return FileStorageFileAccess.NOT_SET;
            }
            return Integer.valueOf(parameter)-1;
        }
        return Integer.valueOf(parameter);
    }

    public String getSearchFolderId() throws OXException {
        return getFolderId();
    }

    public String getSearchQuery() throws OXException {
        final Object data2 = data.getData();
        if(data2 == null) {
            return "";
        }
        final JSONObject queryObject = (JSONObject) data2;
        
        try {
            return queryObject.getString("pattern");
        } catch (final JSONException x) {
            throw AjaxExceptionCodes.JSONError.create( x.getMessage());
        }
    }
    
    protected void parseFile() throws OXException {
        if(file != null) {
            return;
        }
        requireFileMetadata();
        
        JSONObject object = (JSONObject) data.getData();
        if(object == null) {
            try {
                object = new JSONObject(data.getParameter(JSON));
            } catch (final JSONException e) {
                throw AjaxExceptionCodes.JSONError.create( e.getMessage());
            }
        }

        UploadFile uploadFile = null;
        if(data.hasUploads()) {
            uploadFile = data.getFiles().get(0);
        }    
        
        if(data.getUploadEvent() != null && data.getUploadEvent().getUploadFileByFieldName("file") != null) {
            uploadFile = data.getUploadEvent().getUploadFileByFieldName("file");
        }
        
        file = parser.parse(object); 
        fields = parser.getFields(object);
        if(uploadFile != null) {
            if(!fields.contains(File.Field.FILENAME) || file.getFileName() == null || file.getFileName().trim().length() == 0) {
                file.setFileName(uploadFile.getPreparedFileName());
                fields.add(File.Field.FILENAME);
            }
            
            if(!fields.contains(File.Field.FILE_MIMETYPE)) {
                file.setFileMIMEType(uploadFile.getContentType());
                fields.add(File.Field.FILE_MIMETYPE);
            }
            // TODO: Guess Content-Type
        }
        
        final String fileDisplay = data.getParameter("filedisplay");
        if(fileDisplay != null && fileDisplay.trim().length() > 0 && (file.getFileName() == null || file.getFileName().trim().length() == 0)) {
            file.setFileName(fileDisplay);
            fields.add(File.Field.FILENAME);
        }
        
        if(has("id") && ! fields.contains(File.Field.ID)) {
            file.setId(getId());
            fields.add(File.Field.ID);
        }
    }
    
    public File getFile() throws OXException {
        parseFile();
        return file;
    }
    
    public List<Field> getSentColumns() throws OXException {
        parseFile();
        return fields;
    }
    
    public boolean hasUploads() throws OXException {
        return data.hasUploads();
    }
    
    public InputStream getUploadedFileData() throws OXException {
        if(data.hasUploads()) {
            try {
                final UploadFile uploadFile = data.getFiles().get(0);
                checkSize( uploadFile );
                return new FileInputStream(uploadFile.getTmpFile());
            } catch (final FileNotFoundException e) {
                throw AjaxExceptionCodes.IOError.create(  e.getMessage());
            }
        }
        return null;
    }

    private void checkSize(final UploadFile uploadFile) throws OXException{
        final long maxSize = InfostoreConfigUtils.determineRelevantUploadSize();
        if (maxSize == 0) {
            return;
        }

        final long size = uploadFile.getSize();
        if (size > maxSize) {
            throw UploadSizeExceededException.create(size, maxSize, true);
        }
    }

    public int getAttachedId() {
        return getInt(Param.ATTACHED_ID);
    }

    public int getAttachment() {
        return getInt(Param.ATTACHMENT);
    }

    public int getModule() {
        return getInt(Param.MODULE);
    }

    private int getInt(final Param param) {
        return Integer.parseInt(data.getParameter(param.getName()));
    }
    

}
