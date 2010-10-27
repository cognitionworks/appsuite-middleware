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

package com.openexchange.file.storage.webdav;

import static com.openexchange.file.storage.webdav.WebDAVFileStorageResourceUtil.parseDateProperty;
import static com.openexchange.file.storage.webdav.WebDAVFileStorageResourceUtil.parseIntProperty;
import static com.openexchange.file.storage.webdav.WebDAVFileStorageResourceUtil.parseStringProperty;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import com.openexchange.file.storage.DefaultFile;
import com.openexchange.file.storage.FileStorageException;
import com.openexchange.file.storage.FileStorageFileAccess;

/**
 * {@link WebDAVFileStorageFile}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class WebDAVFileStorageFile extends DefaultFile {

    /**
     * Initializes a new {@link WebDAVFileStorageFile}.
     * 
     * @param folderId The folder identifier; e.g. "http://webdav-server.com/Telephone%20Lines"
     * @param id The file identifier; e.g. "lines.pdf"
     * @param userId The user identifier
     */
    public WebDAVFileStorageFile(final String folderId, final String id, final int userId) {
        super();
        setFolderId(folderId);
        setCreatedBy(userId);
        setModifiedBy(userId);
        setId(id);
        setVersion(FileStorageFileAccess.CURRENT_VERSION);
        setIsCurrentVersion(true);
        setURL(folderId + '/' + id);
    }

    /**
     * Parses specified DAV property set of associated MultiStatus response.
     * 
     * @param propertySet The DAV property set of associated MultiStatus response
     * @throws FileStorageException If parsing DAV property set fails
     * @return This WebDAV file with property set applied
     */
    public WebDAVFileStorageFile parseDavPropertySet(final DavPropertySet propertySet) throws FileStorageException {
        return parseDavPropertySet(propertySet, null);
    }

    /**
     * Parses specified DAV property set of associated MultiStatus response.
     * 
     * @param propertySet The DAV property set of associated MultiStatus response
     * @param fields The fields to consider
     * @throws FileStorageException If parsing DAV property set fails
     * @return This WebDAV file with property set applied
     */
    public WebDAVFileStorageFile parseDavPropertySet(final DavPropertySet propertySet, final List<Field> fields) throws FileStorageException {
        if (null != propertySet) {
            final Set<Field> set = null == fields || fields.isEmpty() ? EnumSet.allOf(Field.class) : EnumSet.copyOf(fields);
            if (set.contains(Field.CREATED)) {
                setCreated(parseDateProperty(DavConstants.PROPERTY_CREATIONDATE, propertySet));
            }
            if (set.contains(Field.LAST_MODIFIED) || set.contains(Field.LAST_MODIFIED_UTC)) {
                setLastModified(parseDateProperty(DavConstants.PROPERTY_GETLASTMODIFIED, propertySet));
            }
            if (set.contains(Field.FILENAME)) {
                setFileName(parseStringProperty(DavConstants.PROPERTY_DISPLAYNAME, propertySet));
            }
            if (set.contains(Field.FILE_MIMETYPE)) {
                setFileMIMEType(parseStringProperty(DavConstants.PROPERTY_GETCONTENTTYPE, propertySet));
            }
            if (set.contains(Field.FILE_SIZE)) {
                setFileSize(parseIntProperty(DavConstants.PROPERTY_GETCONTENTLENGTH, propertySet));
            }
            if (set.contains(Field.TITLE)) {
                setTitle(getFileName());
            }
            /*
             * Add other DAV properties as file properties
             */
            setProperty(DavConstants.PROPERTY_GETETAG, parseStringProperty(DavConstants.PROPERTY_GETETAG, propertySet));
            setProperty(DavConstants.PROPERTY_GETCONTENTLANGUAGE, parseStringProperty(DavConstants.PROPERTY_GETCONTENTLANGUAGE, propertySet));
        }
        return this;
    }

}
