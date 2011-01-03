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

package com.openexchange.file.storage.infostore.internal;



import java.io.InputStream;
import com.openexchange.api2.OXException;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.infostore.InfostoreExceptionCodes;
import com.openexchange.groupware.infostore.InfostoreFacade;
import com.openexchange.groupware.infostore.utils.Metadata;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.results.AbstractTimedResult;
import com.openexchange.groupware.results.Delta;
import com.openexchange.groupware.results.DeltaImpl;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.sessiond.impl.SessionHolder;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorAdapter;
import com.openexchange.tools.session.ServerSession;

public class VirtualFolderInfostoreFacade implements InfostoreFacade {

    public int countDocuments(final long folderId, final Context ctx, final User user, final UserConfiguration userConfig) {
        return 0;
    }

    public boolean exists(final int id, final int version, final Context ctx, final User user, final UserConfiguration userConfig) {
        return false;
    }

    public Delta<DocumentMetadata> getDelta(final long folderId, final long updateSince, final Metadata[] columns, final boolean ignoreDeleted, final Context ctx, final User user, final UserConfiguration userConfig) {
        final SearchIterator<DocumentMetadata> emptyIter = SearchIteratorAdapter.createEmptyIterator();
        return new DeltaImpl<DocumentMetadata>(emptyIter,emptyIter,emptyIter,System.currentTimeMillis());
    }

    public Delta<DocumentMetadata> getDelta(final long folderId, final long updateSince, final Metadata[] columns, final Metadata sort, final int order, final boolean ignoreDeleted, final Context ctx, final User user, final UserConfiguration userConfig) {
        final SearchIterator<DocumentMetadata> emptyIter = SearchIteratorAdapter.createEmptyIterator();
        return new DeltaImpl<DocumentMetadata>(emptyIter,emptyIter,emptyIter,System.currentTimeMillis());
    }

    public InputStream getDocument(final int id, final int version, final Context ctx, final User user,
            final UserConfiguration userConfig) throws OXException {
        virtualFolder(); return null;
    }

    public DocumentMetadata getDocumentMetadata(final int id, final int version,
            final Context ctx, final User user, final UserConfiguration userConfig)
            throws OXException {
        virtualFolder(); return null;
    }

    public TimedResult<DocumentMetadata> getDocuments(final long folderId, final Context ctx, final User user, final UserConfiguration userConfig) {
        return new EmptyTimedResult();
    }

    public TimedResult<DocumentMetadata> getDocuments(final long folderId, final Metadata[] columns, final Context ctx, final User user, final UserConfiguration userConfig) {
        return new EmptyTimedResult();
    }

    public TimedResult<DocumentMetadata> getDocuments(final long folderId, final Metadata[] columns, final Metadata sort, final int order, final Context ctx, final User user, final UserConfiguration userConfig) {
        return new EmptyTimedResult();
    }

    public TimedResult<DocumentMetadata> getDocuments(final int[] ids, final Metadata[] columns, final Context ctx, final User user, final UserConfiguration userConfig) {
        return new EmptyTimedResult();
    }

    public TimedResult<DocumentMetadata> getVersions(final int id, final Context ctx, final User user, final UserConfiguration userConfig) {
        return new EmptyTimedResult();
    }

    public TimedResult<DocumentMetadata> getVersions(final int id, final Metadata[] columns, final Context ctx, final User user, final UserConfiguration userConfig) {
        return new EmptyTimedResult();
    }

    public TimedResult<DocumentMetadata> getVersions(final int id, final Metadata[] columns, final Metadata sort, final int order, final Context ctx, final User user, final UserConfiguration userConfig) {
        return new EmptyTimedResult();
    }

    public boolean hasFolderForeignObjects(final long folderId, final Context ctx, final User user, final UserConfiguration userConfig) {
        return false;
    }

    public boolean isFolderEmpty(final long folderId, final Context ctx) {
        return true;
    }

    public void lock(final int id, final long diff, final ServerSession sessionObj)
            throws OXException {
        virtualFolder();
    }

    public void removeDocument(final long folderId, final long date,
            final ServerSession sessionObj) throws OXException {
        virtualFolder();
    }

    public int[] removeDocument(final int[] id, final long date, final ServerSession sessionObj) {
        return id;
    }

    public void removeUser(final int id, final Context context, final ServerSession session) {
        // Nothing to do.
    }

    public int[] removeVersion(final int id, final int[] versionId, final ServerSession sessionObj) {
        return versionId;
    }

    public void saveDocument(final DocumentMetadata document, final InputStream data, final long sequenceNumber, final ServerSession sessionObj) throws OXException {
        virtualFolder();
    }

    public void saveDocument(final DocumentMetadata document, final InputStream data, final long sequenceNumber, final Metadata[] modifiedColumns, final ServerSession sessionObj) throws OXException {
        virtualFolder();
    }

    public void saveDocumentMetadata(final DocumentMetadata document, final long sequenceNumber, final ServerSession sessionObj) throws OXException {
        virtualFolder();
    }

    public void saveDocumentMetadata(final DocumentMetadata document, final long sequenceNumber, final Metadata[] modifiedColumns, final ServerSession sessionObj) throws OXException {
        virtualFolder();
    }

    public void unlock(final int id, final ServerSession sessionObj) {
        // Nothing to do. 
    }

    public void commit() {
        // Nothing to do.
    }

    public void finish() {
        // Nothing to do.
    }

    public void rollback() {
        // Nothing to to.
    }

    public void setRequestTransactional(final boolean transactional) {
        // Nothing to to.
    }

    public void setCommitsTransaction(final boolean commits) {
        // Nothing to to.
    }

    public void setTransactional(final boolean transactional) {
        // Nothing to to.
    }

    public void startTransaction() {
        // Nothing to to.
    }

    private void virtualFolder() throws OXException{
        throw InfostoreExceptionCodes.NO_DOCUMENTS_IN_VIRTUAL_FOLDER.create();
    }

    private class EmptyTimedResult extends AbstractTimedResult<DocumentMetadata> {

        public EmptyTimedResult() {
            super(new SearchIterator<DocumentMetadata>() {

                public void addWarning(final AbstractOXException warning) {
                    // Nothing to to.
                }

                public void close() {
                    // Nothing to do.
                }

                public AbstractOXException[] getWarnings() {
                    return new AbstractOXException[0];
                }

                public boolean hasNext() {
                    return false;
                }

                public boolean hasSize() {
                    return true;
                }

                public boolean hasWarnings() {
                    return false;
                }

                public DocumentMetadata next() {
                    return null;
                }

                public int size() {
                    return 0;
                }
            });
        }

        @Override
        protected long extractTimestamp(final DocumentMetadata object) {
            return 0;
        }

    }

    public void touch(final int id, final ServerSession session) throws OXException {
        virtualFolder();
    }

    public void setSessionHolder(final SessionHolder sessionHolder) {
        // Nothing to do.
    }
}