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

package com.openexchange.filestore.impl;

import static com.openexchange.filestore.FileStorageCodes.NO_SUCH_FILE_STORAGE;
import java.net.URI;
import java.net.URISyntaxException;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.filestore.FileStorageService;
import com.openexchange.filestore.FileStorages;
import com.openexchange.filestore.Info;
import com.openexchange.filestore.OwnerInfo;
import com.openexchange.filestore.QuotaFileStorage;
import com.openexchange.filestore.QuotaFileStorageExceptionCodes;
import com.openexchange.filestore.QuotaFileStorageListener;
import com.openexchange.filestore.QuotaFileStorageService;
import com.openexchange.filestore.StorageInfo;
import com.openexchange.filestore.event.FileStorageListener;
import com.openexchange.filestore.impl.osgi.Services;
import com.openexchange.filestore.unified.UnifiedQuotaService;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.FileStorageInfo;
import com.openexchange.groupware.filestore.Filestore;
import com.openexchange.groupware.filestore.FilestoreExceptionCodes;
import com.openexchange.groupware.filestore.FilestoreStorage;
import com.openexchange.osgi.ServiceListing;
import com.openexchange.user.User;
import com.openexchange.user.UserService;

/**
 * {@link DBQuotaFileStorageService} - The database-backed {@link QuotaFileStorageService}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.0
 */
public class DBQuotaFileStorageService implements QuotaFileStorageService {

    private final FileStorageService fileStorageService;
    private final ServiceListing<FileStorageListener> storageListeners;
    private final ServiceListing<QuotaFileStorageListener> quotaListeners;
    private final ServiceListing<UnifiedQuotaService> unifiedQuotaServices;

    /**
     * Initializes a new {@link DBQuotaFileStorageService}.
     *
     * @param storageListeners The file storage listeners
     * @param unifiedQuotaServices The tracked Unified Quota services
     * @param quotaListeners The quota listeners
     * @param fileStorageService The file storage service
     */
    public DBQuotaFileStorageService(ServiceListing<FileStorageListener> storageListeners, ServiceListing<UnifiedQuotaService> unifiedQuotaServices, ServiceListing<QuotaFileStorageListener> quotaListeners, FileStorageService fileStorageService) {
        super();
        this.storageListeners = storageListeners;
        this.unifiedQuotaServices = unifiedQuotaServices;
        this.quotaListeners = quotaListeners;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public QuotaFileStorage getUnlimitedQuotaFileStorage(URI baseUri, int optOwner, int contextId) throws OXException {
        // Generate full URI
        URI uri = optOwner > 0 ? FileStorages.getFullyQualifyingUriForUser(optOwner, contextId, baseUri) : FileStorages.getFullyQualifyingUriForContext(contextId, baseUri);
        if (null == uri) {
            throw FilestoreExceptionCodes.URI_CREATION_FAILED.create(baseUri.toString() + '/' + (optOwner > 0 ? FileStorages.getNameForUser(optOwner, contextId) : FileStorages.getNameForContext(contextId)));
        }

        // Return appropriate unlimited quota file storage
        return new DBQuotaFileStorage(contextId, Info.administrative(), OwnerInfo.builder().setOwnerId(optOwner).build(), Long.MAX_VALUE, fileStorageService.getFileStorage(uri), uri, storageListeners, quotaListeners, unifiedQuotaServices);
    }

    @Override
    public boolean hasIndividualFileStorage(int userId, int contextId) throws OXException {
        UserService userService = Services.requireService(UserService.class);
        User user = userService.getUser(userId, contextId);
        if (user.getFilestoreId() <= 0) {
            // No individual file storage set; uses the context-associated one
            return false;
        }

        int nextOwnerId = user.getFileStorageOwner();
        if (nextOwnerId <= 0) {
            // User is the owner
            return true;
        }

        // Separate owner (chain)
        User owner;
        do {
            owner = nextOwnerId == userId ? user : userService.getUser(nextOwnerId, contextId);
            nextOwnerId = owner.getFileStorageOwner();
        } while (nextOwnerId > 0);

        if (owner.getFilestoreId() <= 0) {
            // Huh... Owner has no file storage set
            throw QuotaFileStorageExceptionCodes.INSTANTIATIONERROR.create();
        }

        return owner.getId() == userId;
    }

    @Override
    public QuotaFileStorage getQuotaFileStorage(int contextId, Info info) throws OXException {
        return getQuotaFileStorage(-1, contextId, info);
    }

    @Override
    public URI getFileStorageUriFor(int userId, int contextId) throws OXException {
        return getQuotaFileStorage(userId, contextId, Info.administrative()).getUri();
    }

    @Override
    public QuotaFileStorage getQuotaFileStorage(int userId, int contextId, Info info) throws OXException {
        // Get the file storage info
        StorageInfo storageInfo = getFileStorageInfoFor(userId, contextId);

        // Determine file storage's base URI
        Filestore filestore;
        try {
            filestore = FilestoreStorage.getInstance().getFilestore(storageInfo.getId());
        } catch (OXException e) {
            if (false == NO_SUCH_FILE_STORAGE.equals(e)) {
                throw e;
            }

            // No such file storage -- apparently wrong file storage information. Retry.
            invalidate(userId, contextId);
            storageInfo = getFileStorageInfoFor(userId, contextId);
            filestore = FilestoreStorage.getInstance().getFilestore(storageInfo.getId());
        }

        // Generate full URI
        URI uri = generateFullUri(filestore, storageInfo.getName());

        // Create appropriate file storage instance
        return new DBQuotaFileStorage(contextId, info, storageInfo.getOwnerInfo(), storageInfo.getQuota(), fileStorageService.getFileStorage(uri), uri, storageListeners, quotaListeners, unifiedQuotaServices);
    }

    private URI generateFullUri(Filestore filestore, String name) throws OXException {
        URI baseUri = filestore.getUri();
        try {
            String scheme = baseUri.getScheme();
            return new URI(null == scheme ? "file" : scheme, baseUri.getAuthority(), FileStorages.ensureEndingSlash(baseUri.getPath()) + name, baseUri.getQuery(), baseUri.getFragment());
        } catch (URISyntaxException e) {
            throw FilestoreExceptionCodes.URI_CREATION_FAILED.create(e, baseUri.toString() + '/' + name);
        }
    }

    private void invalidate(int userId, int contextId) throws OXException {
        ContextService contextService = Services.requireService(ContextService.class);
        contextService.invalidateContext(contextId);

        UserService userService = Services.requireService(UserService.class);
        userService.invalidateUser(contextService.getContext(contextId), userId);
    }

    @Override
    public StorageInfo getFileStorageInfoFor(int userId, int contextId) throws OXException {
        ContextService contextService = Services.requireService(ContextService.class);
        if (userId <= 0) {
            return newStorageInfoFor(OwnerInfo.NO_OWNER, contextService.getContext(contextId));
        }

        UserService userService = Services.requireService(UserService.class);
        Context context = contextService.getContext(contextId);
        User user = userService.getUser(userId, context);
        if (user.getFilestoreId() <= 0) {
            // No user-specific file storage
            return newStorageInfoFor(OwnerInfo.NO_OWNER, context);
        }

        // A user-specific file storage; determine its owner
        int nextOwnerId = user.getFileStorageOwner();
        if (nextOwnerId <= 0 || nextOwnerId == userId) {
            // User is the owner
            return newStorageInfoFor(OwnerInfo.builder().setOwnerId(userId).setMaster(true).build(), user);
        }

        // Separate owner (chain)
        User owner;
        do {
            owner = userService.getUser(nextOwnerId, context);
            nextOwnerId = owner.getFileStorageOwner();
        } while (nextOwnerId > 0);

        if (owner.getFilestoreId() <= 0) {
            // Huh... Owner has no file storage set
            throw QuotaFileStorageExceptionCodes.INSTANTIATIONERROR.create();
        }

        return newStorageInfoFor(OwnerInfo.builder().setOwnerId(owner.getId()).setMaster(false).build(), owner);
    }

    private StorageInfo newStorageInfoFor(OwnerInfo ownerInfo, FileStorageInfo fileStorageInfo) {
        return new StorageInfo(fileStorageInfo.getFilestoreId(), ownerInfo, fileStorageInfo.getFilestoreName(), fileStorageInfo.getFileStorageQuota());
    }

}
