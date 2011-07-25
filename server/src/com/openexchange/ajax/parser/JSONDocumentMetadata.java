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

package com.openexchange.ajax.parser;

import java.util.Date;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.infostore.InfostoreFacade;
import com.openexchange.groupware.infostore.utils.Metadata;
import com.openexchange.groupware.infostore.utils.URLHelper;

public class JSONDocumentMetadata implements DocumentMetadata {

    private static final long serialVersionUID = -5016635593135118691L;
    
    private static final URLHelper helper = new URLHelper();
    
    
    private final JSONObject jsonObject;

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(JSONDocumentMetadata.class));
    private static final String DEFAULT_MIMETYPE = "application/octet-stream";
    //private static final InfostoreExceptionFactory EXCEPTIONS = new InfostoreExceptionFactory(JSONDocumentMetadata.class);
    
    public JSONDocumentMetadata(){
        this.jsonObject = new JSONObject();
    }
    
    public JSONDocumentMetadata(final String json) throws JSONException {
        this.jsonObject = new JSONObject(json);
        
        //Test parsing of complex objects
        if(jsonObject.has(Metadata.URL_LITERAL.getName())) {
            String url = jsonObject.getString(Metadata.URL_LITERAL.getName());
            if(!"".equals(url.trim())) {
                url = helper.process(url);
                jsonObject.put(Metadata.URL_LITERAL.getName(),url);
            }
        }
    }
    
    public String getProperty(final String key) {
        if(Metadata.get(key) == null) {
            return jsonObject.optString(key);
        }
        return null;
    }

    public Set<String> getPropertyNames() {
        return null;
    }

    public Date getLastModified() {
        if(!jsonObject.has(Metadata.LAST_MODIFIED_LITERAL.getName())) {
            return null;
        }
        return new Date(jsonObject.optLong(Metadata.LAST_MODIFIED_LITERAL.getName()));
    }

    public void setLastModified(final Date now) {
        try {
            jsonObject.put(Metadata.LAST_MODIFIED_LITERAL.getName(), now.getTime());
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public Date getCreationDate() {
        if(!jsonObject.has(Metadata.CREATION_DATE_LITERAL.getName())) {
            return null;
        }
        return new Date(jsonObject.optLong(Metadata.CREATION_DATE_LITERAL.getName()));
    }

    public void setCreationDate(final Date creationDate) {
        try {
            jsonObject.put(Metadata.CREATION_DATE_LITERAL.getName(), creationDate.getTime());
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public int getModifiedBy() {
        if(!jsonObject.has(Metadata.MODIFIED_BY_LITERAL.getName())) {
            return -1;
        }
        return jsonObject.optInt(Metadata.MODIFIED_BY_LITERAL.getName());
    
    }

    public void setModifiedBy(final int lastEditor) {
        try {
            jsonObject.put(Metadata.MODIFIED_BY_LITERAL.getName(), lastEditor);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public long getFolderId() {
        if(!jsonObject.has(Metadata.FOLDER_ID_LITERAL.getName())) {
            return -1;
        }
        return jsonObject.optLong(Metadata.FOLDER_ID_LITERAL.getName());
    }

    public void setFolderId(final long folderId) {
        try {
            jsonObject.put(Metadata.FOLDER_ID_LITERAL.getName(), folderId);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public String getTitle() {
        if(!jsonObject.has(Metadata.TITLE_LITERAL.getName())) {
            return null;
        }
        return jsonObject.optString(Metadata.TITLE_LITERAL.getName());    
    }

    public void setTitle(final String title) {
        try {
            jsonObject.put(Metadata.TITLE_LITERAL.getName(), title);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public int getVersion() {
        if(!jsonObject.has(Metadata.VERSION_LITERAL.getName())) {
            return 0;
        }
        return jsonObject.optInt(Metadata.VERSION_LITERAL.getName());
    }

    public void setVersion(final int version) {
        try {
            jsonObject.put(Metadata.VERSION_LITERAL.getName(), version);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public String getContent() {
        return getDescription();
    }

    public long getFileSize() {
        if(!jsonObject.has(Metadata.FILE_SIZE_LITERAL.getName())) {
            return -1;
        }
        return jsonObject.optLong(Metadata.FILE_SIZE_LITERAL.getName());
    }

    public void setFileSize(final long length) {
        try {
            jsonObject.put(Metadata.FILE_SIZE_LITERAL.getName(), length);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public String getFileMIMEType() {
        if(!jsonObject.has(Metadata.FILE_MIMETYPE_LITERAL.getName())) {
            return DEFAULT_MIMETYPE;
        }
        return jsonObject.optString(Metadata.FILE_MIMETYPE_LITERAL.getName());
    }

    public void setFileMIMEType(final String type) {
        try {
            jsonObject.put(Metadata.FILE_MIMETYPE_LITERAL.getName(), type);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public String getFileName() {
        if(!jsonObject.has(Metadata.FILENAME_LITERAL.getName())) {
            return null;
        }
        return jsonObject.optString(Metadata.FILENAME_LITERAL.getName());
    }

    public void setFileName(final String fileName) {
        try {
            jsonObject.put(Metadata.FILENAME_LITERAL.getName(), fileName);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public int getId() {
        if(!jsonObject.has(Metadata.ID_LITERAL.getName())) {
            return InfostoreFacade.NEW;
        }
        return jsonObject.optInt(Metadata.ID_LITERAL.getName());
    }

    public void setId(final int id) {
        try {
            jsonObject.put(Metadata.ID_LITERAL.getName(), id);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public int getCreatedBy() {
        if(!jsonObject.has(Metadata.CREATED_BY_LITERAL.getName())) {
            return -1;
        }
        return jsonObject.optInt(Metadata.CREATED_BY_LITERAL.getName());
    }

    public void setCreatedBy(final int creator) {
        try {
            jsonObject.put(Metadata.CREATED_BY_LITERAL.getName(), creator);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public String getDescription() {
        if(!jsonObject.has(Metadata.DESCRIPTION_LITERAL.getName())) {
            return null;
        }
        return jsonObject.optString(Metadata.DESCRIPTION_LITERAL.getName());
    }

    public void setDescription(final String description) {
        try {
            jsonObject.put(Metadata.DESCRIPTION_LITERAL.getName(), description);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public String getURL() {
        if(!jsonObject.has(Metadata.URL_LITERAL.getName())) {
            return null;
        }
        return jsonObject.optString(Metadata.URL_LITERAL.getName());
    }

    public void setURL(final String url) {
        try {
            jsonObject.put(Metadata.URL_LITERAL.getName(), url);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public long getSequenceNumber() {
        if(getLastModified()==null) {
            return 0;
        }
        return getLastModified().getTime();
    }

    public String getCategories() {
        if(!jsonObject.has(Metadata.CATEGORIES_LITERAL.getName())) {
            return null;
        }
        return jsonObject.optString(Metadata.CATEGORIES_LITERAL.getName());
    }

    public void setCategories(final String categories) {
        try {
            jsonObject.put(Metadata.CATEGORIES_LITERAL.getName(), categories);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public Date getLockedUntil() {
        if(!jsonObject.has(Metadata.LOCKED_UNTIL_LITERAL.getName())) {
            return null;
        }
        return new Date(jsonObject.optLong(Metadata.LOCKED_UNTIL_LITERAL.getName()));
    }

    public void setLockedUntil(final Date lockedUntil) {
        try {
            jsonObject.put(Metadata.LOCKED_UNTIL_LITERAL.getName(), lockedUntil);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public String getFileMD5Sum() {
        if(!jsonObject.has(Metadata.FILE_MD5SUM_LITERAL.getName())) {
            return null;
        }
        return jsonObject.optString(Metadata.FILE_MD5SUM_LITERAL.getName());
    }

    public void setFileMD5Sum(final String sum) {
        try {
            jsonObject.put(Metadata.FILE_MD5SUM_LITERAL.getName(), sum);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }
    
    public int getColorLabel() {
        return jsonObject.optInt(Metadata.COLOR_LABEL_LITERAL.getName());
    }

    public void setColorLabel(final int color) {
        try {
            jsonObject.put(Metadata.COLOR_LABEL_LITERAL.getName(), color);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public boolean isCurrentVersion() {
        return jsonObject.optBoolean(Metadata.CURRENT_VERSION_LITERAL.getName());
    }

    public void setIsCurrentVersion(final boolean bool) {
        try {
            jsonObject.put(Metadata.CURRENT_VERSION_LITERAL.getName(), bool);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }

    public String getVersionComment() {
        if(!jsonObject.has(Metadata.VERSION_COMMENT_LITERAL.getName())) {
            return null;
        }
        return jsonObject.optString(Metadata.VERSION_COMMENT_LITERAL.getName());
    }

    public void setVersionComment(final String string) {
        try {
            jsonObject.put(Metadata.VERSION_COMMENT_LITERAL.getName(), string);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }
    
    @Override
    public String toString(){
        return jsonObject.toString();
    }
    
    public String toJSONString(){
        return jsonObject.toString();
    }

    public String getFilestoreLocation() {
        if(!jsonObject.has(Metadata.FILESTORE_LOCATION_LITERAL.getName())) {
            return null;
        }
        return jsonObject.optString(Metadata.FILESTORE_LOCATION_LITERAL.getName());
    }



    public void setFilestoreLocation(final String string) {
        try {
            jsonObject.put(Metadata.FILESTORE_LOCATION_LITERAL.getName(), string);
        } catch (final JSONException e) {
            LOG.error("",e);
        }
    }
    
    public void setNumberOfVersions(final int numberOfVersions) {
        try {
            jsonObject.put(Metadata.NUMBER_OF_VERSIONS_LITERAL.getName(), numberOfVersions);
        } catch (final JSONException e) {
            LOG.error("", e);
        }
    }

    public int getNumberOfVersions() {
        if(jsonObject.has(Metadata.NUMBER_OF_VERSIONS_LITERAL.getName())) {
            return jsonObject.optInt(Metadata.NUMBER_OF_VERSIONS_LITERAL.getName());
        }
        return -1;
    }

}
