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

package com.openexchange.folderstorage.messaging;

import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.FolderType;
import com.openexchange.folderstorage.messaging.osgi.Services;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.messaging.registry.MessagingServiceRegistry;

/**
 * {@link MessagingFolderType} - The folder type for messaging.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MessagingFolderType implements FolderType {

    private static final MessagingFolderType instance = new MessagingFolderType();

    /**
     * Gets the {@link MessagingFolderType} instance.
     *
     * @return The {@link MessagingFolderType} instance
     */
    public static MessagingFolderType getInstance() {
        return instance;
    }

    /**
     * Initializes a new {@link MessagingFolderType}.
     */
    private MessagingFolderType() {
        super();
    }

    @Override
    public boolean servesTreeId(final String treeId) {
        return FolderStorage.REAL_TREE_ID.equals(treeId);
    }

    /**
     * The private folder identifier.
     */
    private static final String PRIVATE_FOLDER_ID = String.valueOf(FolderObject.SYSTEM_PRIVATE_FOLDER_ID);

    @Override
    public boolean servesFolderId(final String folderId) {
        if (null == folderId) {
            return false;
        }
        /*
         * <service-id>://<account-id>/<fullname>
         */
        final MessagingFolderIdentifier pfi;
        try {
            pfi = new MessagingFolderIdentifier(folderId);
        } catch (OXException e) {
            // org.slf4j.LoggerFactory.getLogger(MessagingFolderType.class).warn("", e);
            return false;
        }
        /*
         * Check if service exists
         */
        final MessagingServiceRegistry registry = Services.getService(MessagingServiceRegistry.class);
        if (null == registry) {
            return false;
        }
        if (!registry.containsMessagingService(pfi.getServiceId(), -1, -1)) { // FIXME: Please pass user and context
            return false;
        }
        return true;
    }

    @Override
    public boolean servesParentId(final String folderId) {
        if (null == folderId) {
            return false;
        }
        if (PRIVATE_FOLDER_ID.equals(folderId)) {
            return true;
        }
        return servesFolderId(folderId);
    }

}
