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

package com.openexchange.file.storage.copycom;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import com.copy.api.Creator;
import com.copy.api.File;
import com.copy.api.Revision;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.DefaultFile;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.file.storage.FileStorageFolder;
import com.openexchange.file.storage.copycom.osgi.Services;
import com.openexchange.mime.MimeTypeMap;

/**
 * {@link CopyComFile}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CopyComFile extends DefaultFile {

    /**
     * Initializes a new {@link CopyComFile}.
     *
     * @param folderId The folder identifier
     * @param id The file identifier
     * @param userId The user identifier
     */
    public CopyComFile(String folderId, String id, int userId, String rootFolderId) {
        super();
        setFolderId(isRootFolder(folderId, rootFolderId) ? FileStorageFolder.ROOT_FULLNAME : folderId);
        setCreatedBy(userId);
        setModifiedBy(userId);
        setId(id);
        setFileName(id);
        setVersion(FileStorageFileAccess.CURRENT_VERSION);
        setIsCurrentVersion(true);
    }

    private static boolean isRootFolder(String id, String rootFolderId) {
        return "0".equals(id) || rootFolderId.equals(id);
    }

    @Override
    public String toString() {
        final String url = getURL();
        return url == null ? super.toString() : url;
    }

    /**
     * Parses specified Copy.com file.
     *
     * @param file The Copy.com file
     * @throws OXException If parsing Copy.com file fails
     * @return This Copy.com file
     */
    public CopyComFile parseCopyComFile(File file) throws OXException {
        return parseCopyComFile(file, null);
    }

    /**
     * Parses specified Copy.com file.
     *
     * @param file The Copy.com file
     * @param fields The fields to consider
     * @throws OXException If parsing Copy.com file fails
     * @return This Copy.com file with property set applied
     */
    public CopyComFile parseCopyComFile(File file, List<Field> fields) throws OXException {
        if (null != file) {
            try {
                String name = file.getPath();
                int pos = name.lastIndexOf('/');
                name = pos > 0 ? name.substring(pos + 1) : name;

                setTitle(file.getName());
                setFileName(name);
                final Set<Field> set = null == fields || fields.isEmpty() ? EnumSet.allOf(Field.class) : EnumSet.copyOf(fields);

                if (set.contains(Field.CREATED)) {
                    Integer createdAt = getCreatedTime(file);
                    if (null != createdAt) {
                        setCreated(new Date(createdAt.intValue() * 1000L));
                    }
                }
                if (set.contains(Field.LAST_MODIFIED) || set.contains(Field.LAST_MODIFIED_UTC)) {
                    Integer modifiedAt = getModifiedAt(file);
                    if (null != modifiedAt) {
                        setLastModified(new Date(modifiedAt.intValue() * 1000L));
                    }
                }
                if (set.contains(Field.FILE_MIMETYPE)) {
                    MimeTypeMap map = Services.getService(MimeTypeMap.class);
                    String contentType = map.getContentType(name);
                    setFileMIMEType(contentType);
                }
                if (set.contains(Field.FILE_SIZE)) {
                    Integer size = file.getSize();
                    if (null != size) {
                        setFileSize(size.longValue());
                    }
                }
                if (set.contains(Field.URL)) {
                    String link = file.getUrl();
                    if (null != link) {
                        setURL(link);
                    }
                }
                if (set.contains(Field.COLOR_LABEL)) {
                    setColorLabel(0);
                }
                if (set.contains(Field.CATEGORIES)) {
                    setCategories(null);
                }
                if (set.contains(Field.VERSION_COMMENT)) {
                    setVersionComment(null);
                }
            } catch (final RuntimeException e) {
                throw CopyComExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
        }
        return this;
    }

    private Integer getModifiedAt(File file) {
        List<Revision> revisions = file.getRevisions();
        if (null != revisions && !revisions.isEmpty()) {
            return revisions.get(0).getModifiedTime();
        }
        return null;
    }

    private Integer getCreatedTime(File file) {
        List<Revision> revisions = file.getRevisions();
        if (null != revisions && !revisions.isEmpty()) {
            Creator creator = revisions.get(0).getCreator();
            if (null != creator) {
                return creator.getCreatedTime();
            }
        }
        return null;
    }

    /**
     * Parses specified Copy.com file.
     *
     * @param file The Copy.com file
     * @param fields The fields to consider
     * @throws OXException If parsing Copy.com file fails
     * @return This Copy.com file with property set applied
     */
    public CopyComFile parseCopyComFile(JSONObject jFile) throws OXException {
        return parseCopyComFile(jFile, null);
    }

    /**
     * Parses specified Copy.com file.
     *
     * @param file The Copy.com file
     * @param fields The fields to consider
     * @throws OXException If parsing Copy.com file fails
     * @return This Copy.com file with property set applied
     */
    public CopyComFile parseCopyComFile(JSONObject jFile, List<Field> fields) throws OXException {
        if (null != jFile) {
            try {
                String name = jFile.optString("path", null);
                int pos = name.lastIndexOf('/');
                name = pos > 0 ? name.substring(pos + 1) : name;

                setTitle(jFile.optString("name", null));
                setFileName(name);
                final Set<Field> set = null == fields || fields.isEmpty() ? EnumSet.allOf(Field.class) : EnumSet.copyOf(fields);

                if (set.contains(Field.CREATED)) {
                    Integer createdAt = getCreatedTime(jFile);
                    if (null != createdAt) {
                        setCreated(new Date(createdAt.intValue() * 1000L));
                    }
                }
                if (set.contains(Field.LAST_MODIFIED) || set.contains(Field.LAST_MODIFIED_UTC)) {
                    Integer modifiedAt = getModifiedAt(jFile);
                    if (null != modifiedAt) {
                        setLastModified(new Date(modifiedAt.intValue() * 1000L));
                    }
                }
                if (set.contains(Field.FILE_MIMETYPE)) {
                    MimeTypeMap map = Services.getService(MimeTypeMap.class);
                    String contentType = map.getContentType(name);
                    setFileMIMEType(contentType);
                }
                if (set.contains(Field.FILE_SIZE)) {
                    Integer size = (Integer) jFile.opt("size");
                    if (null != size) {
                        setFileSize(size.longValue());
                    }
                }
                if (set.contains(Field.URL)) {
                    String link = (String) jFile.opt("url");
                    if (null != link) {
                        setURL(link);
                    }
                }
                if (set.contains(Field.COLOR_LABEL)) {
                    setColorLabel(0);
                }
                if (set.contains(Field.CATEGORIES)) {
                    setCategories(null);
                }
                if (set.contains(Field.VERSION_COMMENT)) {
                    setVersionComment(null);
                }
            } catch (final RuntimeException e) {
                throw CopyComExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
        }
        return this;
    }

    private Integer getModifiedAt(JSONObject jFile) {
        JSONArray revisions = jFile.optJSONArray("revisions");
        if (null != revisions && !revisions.isEmpty()) {
            return (Integer) revisions.optJSONObject(0).opt("modified_time");
        }
        return null;
    }

    private Integer getCreatedTime(JSONObject jFile) {
        JSONArray revisions = jFile.optJSONArray("revisions");
        if (null != revisions && !revisions.isEmpty()) {
            JSONObject creator = revisions.optJSONObject(0).optJSONObject("creator");
            if (null != creator) {
                return (Integer) creator.opt("created_time");
            }
        }
        return null;
    }

}
