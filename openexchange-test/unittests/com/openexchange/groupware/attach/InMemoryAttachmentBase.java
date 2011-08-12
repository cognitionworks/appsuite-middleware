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
package com.openexchange.groupware.attach;

import static com.openexchange.java.Autoboxing.I;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.results.Delta;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.session.Session;

/**
 * @author Francisco Laguna <francisco.laguna@open-xchange.com>
 */
public class InMemoryAttachmentBase implements AttachmentBase{
    private final Map<Context, Map<Integer, AttachmentMetadata>> data = new HashMap<Context, Map<Integer, AttachmentMetadata>>();
    private final Map<Context, List<AttachmentMetadata>> changes = new HashMap<Context, List<AttachmentMetadata>>();
    private final Map<Context, List<AttachmentMetadata>> deletions = new HashMap<Context, List<AttachmentMetadata>>();

    public long attachToObject(final AttachmentMetadata attachment, final InputStream input, Session session, final Context ctx, final User user, final UserConfiguration userConfig) {
        throw new UnsupportedOperationException();
    }

    public long detachFromObject(final int folderId, final int objectId, final int moduleId, final int[] ids, Session session, final Context ctx, final User user, final UserConfiguration userConfig) {
        throw new UnsupportedOperationException();
    }

    public AttachmentMetadata getAttachment(final int folderId, final int objectId, final int moduleId, final int id, final Context ctx, final User user, final UserConfiguration userConfig) {
        throw new UnsupportedOperationException();
    }

    public InputStream getAttachedFile(final int folderId, final int attachedId, final int moduleId, final int id, final Context context, final User user, final UserConfiguration userConfig) {
        throw new UnsupportedOperationException();
    }

    public SortedSet<String> getAttachmentFileStoreLocationsperContext(final Context ctx) {
        final SortedSet<String> locations = new TreeSet<String>();
        for(final AttachmentMetadata metadata : getCtxMap(ctx).values()) {
            locations.add(metadata.getFileId());
        }
        return locations;
    }

    public TimedResult<AttachmentMetadata> getAttachments(final int folderId, final int attachedId, final int moduleId, final Context context, final User user, final UserConfiguration userConfig) {
        throw new UnsupportedOperationException();
    }

    public TimedResult<AttachmentMetadata> getAttachments(final int folderId, final int attachedId, final int moduleId, final AttachmentField[] columns, final AttachmentField sort, final int order, final Context context, final User user, final UserConfiguration userConfig) {
        throw new UnsupportedOperationException();
    }

    public TimedResult<AttachmentMetadata> getAttachments(final int folderId, final int attachedId, final int moduleId, final int[] idsToFetch, final AttachmentField[] fields, final Context context, final User user, final UserConfiguration userConfig) {
        throw new UnsupportedOperationException();
    }

    public Delta<AttachmentMetadata> getDelta(final int folderId, final int attachedId, final int moduleId, final long ts, final boolean ignoreDeleted, final Context context, final User user, final UserConfiguration userConfig) {
        throw new UnsupportedOperationException();
    }

    public Delta<AttachmentMetadata> getDelta(final int folderId, final int attachedId, final int moduleId, final long ts, final boolean ignoreDeleted, final AttachmentField[] fields, final AttachmentField sort, final int order, final Context context, final User user, final UserConfiguration userConfig) {
        throw new UnsupportedOperationException();
    }

    public void registerAttachmentListener(final AttachmentListener listener, final int moduleId) {
        throw new UnsupportedOperationException();
    }

    public void removeAttachmentListener(final AttachmentListener listener, final int moduleId) {
        throw new UnsupportedOperationException();
    }

    public int[] removeAttachment(final String file_id, final Context ctx) {
        for(final AttachmentMetadata attachment : getCtxMap(ctx).values()) {
            final String location = attachment.getFileId();
            if(location != null && location.equals(file_id)){
                deletions.get(ctx).add(attachment);
                return new int[]{1,1};
            }
        }
        return new int[]{1,1};
    }

    public int modifyAttachment(final String file_id, final String new_file_id, final String new_comment, final String new_mime, final Context ctx) {
        for(final AttachmentMetadata attachment : getCtxMap(ctx).values()) {
            final String location = attachment.getFileId();
            if(location != null && location.equals(file_id)){
                attachment.setFileId(new_file_id);
                attachment.setComment(new_comment);
                attachment.setFileMIMEType(new_mime);
                changes.get(ctx).add(attachment);
                return attachment.getId();
            }
        }
        return -1;
    }

    public void addAuthorization(final AttachmentAuthorization authz, final int moduleId) {
        throw new UnsupportedOperationException();
    }

    public void removeAuthorization(final AttachmentAuthorization authz, final int moduleId) {
        throw new UnsupportedOperationException();
    }

    public void deleteAll(final Context context) {
        throw new UnsupportedOperationException();
    }

    public void startTransaction() {
        //IGNORE
    }

    public void commit() {
        //IGNORE
    }

    public void rollback() {
        throw new UnsupportedOperationException();
    }

    public void finish() {
        //IGNORE
    }

    public void setTransactional(final boolean transactional) {
        //IGNORE
    }

    public void setRequestTransactional(final boolean transactional) {
        throw new UnsupportedOperationException();
    }

    public void setCommitsTransaction(final boolean commits) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void put(final Context ctx, final AttachmentMetadata attachment) {
        getCtxMap(ctx).put(I(attachment.getId()), attachment);
    }

    private Map<Integer, AttachmentMetadata> getCtxMap(final Context ctx) {
        if(data.containsKey(ctx)) {
            return data.get(ctx);
        }
        final Map<Integer, AttachmentMetadata> attachments = new HashMap<Integer, AttachmentMetadata>();
        data.put(ctx, attachments);
        return attachments;
    }

    public void forgetChanges(final Context ctx) {
        changes.put(ctx, new ArrayList<AttachmentMetadata>());
    }

    public List<AttachmentMetadata> getChanges(final Context ctx) {
        if(!changes.containsKey(ctx)) {
            return new ArrayList<AttachmentMetadata>();
        }
        return changes.get(ctx);
    }

    public void forgetDeletions(final Context ctx) {
        deletions.put(ctx, new ArrayList<AttachmentMetadata>());
    }

    public List<AttachmentMetadata> getDeletions(final Context ctx) {
        return deletions.get(ctx);
    }

    public Date getNewestCreationDate(Context ctx, int moduleId, int attachedId) {
        throw new UnsupportedOperationException();
    }

    public Map<Integer, Date> getNewestCreationDates(Context ctx, int moduleId, int[] attachedIds) {
        throw new UnsupportedOperationException();
    }
}
