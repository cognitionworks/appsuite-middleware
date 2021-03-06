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

package com.openexchange.folderstorage.internal.performers;

import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.FolderExceptionErrorMessage;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.FolderStorageDiscoverer;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;

/**
 * {@link ConsistencyPerformer} - Serves the <code>CLEAR</code> request.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ConsistencyPerformer extends AbstractPerformer {

    /**
     * Initializes a new {@link ConsistencyPerformer}.
     *
     * @param session The session
     * @throws OXException If passed session is invalid
     */
    public ConsistencyPerformer(final ServerSession session) throws OXException {
        super(session);
    }

    /**
     * Initializes a new {@link ConsistencyPerformer}.
     *
     * @param user The user
     * @param context The context
     */
    public ConsistencyPerformer(final User user, final Context context) {
        super(user, context);
    }

    /**
     * Initializes a new {@link ConsistencyPerformer}.
     *
     * @param session The session
     * @param folderStorageDiscoverer The folder storage discoverer
     * @throws OXException If passed session is invalid
     */
    public ConsistencyPerformer(final ServerSession session, final FolderStorageDiscoverer folderStorageDiscoverer) throws OXException {
        super(session, folderStorageDiscoverer);
    }

    /**
     * Initializes a new {@link ConsistencyPerformer}.
     *
     * @param user The user
     * @param context The context
     * @param folderStorageDiscoverer The folder storage discoverer
     */
    public ConsistencyPerformer(final User user, final Context context, final FolderStorageDiscoverer folderStorageDiscoverer) {
        super(user, context, folderStorageDiscoverer);
    }

    /**
     * Performs the consistency check.
     *
     * @param treeId The tree identifier
     * @throws OXException If an error occurs during deletion
     */
    public void doConsistencyCheck(final String treeId) throws OXException {
        final FolderStorage[] folderStorages = folderStorageDiscoverer.getFolderStoragesForTreeID(treeId);
        for (final FolderStorage folderStorage : folderStorages) {
            final boolean started = folderStorage.startTransaction(storageParameters, true);
            try {
                folderStorage.checkConsistency(treeId, storageParameters);
                if (started) {
                    folderStorage.commitTransaction(storageParameters);
                }
            } catch (OXException e) {
                if (started) {
                    folderStorage.rollback(storageParameters);
                }
                throw e;
            } catch (Exception e) {
                if (started) {
                    folderStorage.rollback(storageParameters);
                }
                throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
        }
    }

}
