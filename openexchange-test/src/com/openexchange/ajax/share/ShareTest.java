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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.ajax.share;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.json.JSONException;
import org.junit.Assert;
import com.openexchange.ajax.folder.Create;
import com.openexchange.ajax.folder.actions.DeleteRequest;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.GetRequest;
import com.openexchange.ajax.folder.actions.GetResponse;
import com.openexchange.ajax.folder.actions.InsertRequest;
import com.openexchange.ajax.folder.actions.InsertResponse;
import com.openexchange.ajax.folder.actions.OCLGuestPermission;
import com.openexchange.ajax.folder.actions.UpdateRequest;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.infostore.actions.DeleteInfostoreRequest;
import com.openexchange.ajax.infostore.actions.GetInfostoreRequest;
import com.openexchange.ajax.infostore.actions.GetInfostoreResponse;
import com.openexchange.ajax.infostore.actions.NewInfostoreRequest;
import com.openexchange.ajax.infostore.actions.NewInfostoreResponse;
import com.openexchange.ajax.infostore.actions.UpdateInfostoreRequest;
import com.openexchange.ajax.infostore.actions.UpdateInfostoreResponse;
import com.openexchange.ajax.share.GuestClient.ClientConfig;
import com.openexchange.ajax.share.actions.AllRequest;
import com.openexchange.ajax.share.actions.ExtendedPermissionEntity;
import com.openexchange.ajax.share.actions.FileShare;
import com.openexchange.ajax.share.actions.FileSharesRequest;
import com.openexchange.ajax.share.actions.FolderShare;
import com.openexchange.ajax.share.actions.FolderSharesRequest;
import com.openexchange.ajax.share.actions.ParsedShare;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.DefaultFile;
import com.openexchange.file.storage.DefaultFileStorageGuestObjectPermission;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.FileStorageGuestObjectPermission;
import com.openexchange.file.storage.FileStorageObjectPermission;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.modules.Module;
import com.openexchange.java.Autoboxing;
import com.openexchange.java.util.TimeZones;
import com.openexchange.java.util.UUIDs;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.share.AuthenticationMode;
import com.openexchange.share.RequestContext;
import com.openexchange.share.recipient.AnonymousRecipient;
import com.openexchange.share.recipient.GuestRecipient;
import com.openexchange.share.recipient.RecipientType;
import com.openexchange.share.recipient.ShareRecipient;

/**
 * {@link ShareTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public abstract class ShareTest extends AbstractAJAXSession {

    protected static final OCLGuestPermission[] TESTED_PERMISSIONS = new OCLGuestPermission[] {
        createNamedAuthorPermission("otto@example.com", "Otto Example", "secret"),
        createNamedGuestPermission("horst@example.com", "Horst Example", "secret"),
//        createAnonymousAuthorPermission("secret"),
        createAnonymousGuestPermission("secret"),
        createAnonymousGuestPermission()
    };

    protected static final FileStorageGuestObjectPermission[] TESTED_OBJECT_PERMISSIONS = new FileStorageGuestObjectPermission[] {
        asObjectPermission(TESTED_PERMISSIONS[0]),
        asObjectPermission(TESTED_PERMISSIONS[1]),
        asObjectPermission(TESTED_PERMISSIONS[2]),
        asObjectPermission(TESTED_PERMISSIONS[3])
    };

    protected static final EnumAPI[] TESTED_FOLDER_APIS = new EnumAPI[] { EnumAPI.OX_OLD, EnumAPI.OX_NEW, EnumAPI.OUTLOOK };

    protected static final int[] TESTED_MODULES = new int[] {
        FolderObject.CONTACT, FolderObject.INFOSTORE, FolderObject.TASK, FolderObject.CALENDAR
    };

    protected static final String[] TESTED_MODULES_NAMES = new String[] {
        Module.CONTACTS.getName(), Module.INFOSTORE.getName(), Module.TASK.getName(), Module.CALENDAR.getName()
    };

    protected static final Random random = new Random();
    protected static final int CLEANUP_DELAY = 30000;

    private Map<Integer, FolderObject> foldersToDelete;
    private Map<String, File> filesToDelete;

    /**
     * Initializes a new {@link ShareTest}.
     *
     * @param name The test name
     */
    protected ShareTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        foldersToDelete = new HashMap<Integer, FolderObject>();
        filesToDelete = new HashMap<String, File>();
    }

    /**
     * Gets a request context based on the AJAX clients values.
     */
    protected RequestContext getRequestContext() {
        return new RequestContext() {
            @Override
            public String getProtocol() {
                return client.getProtocol();
            }

            @Override
            public String getHostname() {
                return client.getHostname();
            }

            @Override
            public String getServletPrefix() {
                return "/ajax/";
            }
        };
    }

    /**
     * Inserts and remembers a new shared folder containing the supplied guest permissions.
     *
     * @param api The folder tree to use
     * @param module The module identifier
     * @param parent The ID of the parent folder
     * @param guestPermission The guest permission to add
     * @return The inserted folder
     * @throws Exception
     */
    protected FolderObject insertSharedFolder(EnumAPI api, int module, int parent, OCLGuestPermission guestPermission) throws Exception {
        return insertSharedFolder(api, module, parent, randomUID(), guestPermission);
    }

    /**
     * Inserts and remembers a new shared folder containing the supplied guest permissions.
     *
     * @param api The folder tree to use
     * @param module The module identifier
     * @param parent The ID of the parent folder
     * @param name The folders name
     * @param guestPermission The guest permission to add
     * @return The inserted folder
     * @throws Exception
     */
    protected FolderObject insertSharedFolder(EnumAPI api, int module, int parent, String name, OCLGuestPermission guestPermission) throws Exception {
        FolderObject sharedFolder = Create.createPrivateFolder(name, module, client.getValues().getUserId(), guestPermission);
        sharedFolder.setParentFolderID(parent);
        return insertFolder(api, sharedFolder);
    }

    /**
     * Inserts and remembers a new private folder.
     *
     * @param api The folder tree to use
     * @param module The module identifier
     * @param parent The ID of the parent folder
     * @return The inserted folder
     * @throws Exception
     */
    protected FolderObject insertPrivateFolder(EnumAPI api, int module, int parent) throws Exception {
        FolderObject privateFolder = Create.createPrivateFolder(randomUID(), module, client.getValues().getUserId());
        privateFolder.setParentFolderID(parent);
        return insertFolder(api, privateFolder);
    }

    /**
     * Inserts a public folder below folder 2 or folder 15 if its a drive folder.
     *
     * @param api The folder tree to use
     * @param module The module identifier
     * @return The inserted folder
     * @throws Exception
     */
    protected FolderObject insertPublicFolder(EnumAPI api, int module) throws Exception {
        FolderObject folder = new FolderObject();
        folder.setFolderName(randomUID());
        folder.setModule(module);
        folder.setType(FolderObject.PUBLIC);
        OCLPermission perm1 = new OCLPermission();
        perm1.setEntity(client.getValues().getUserId());
        perm1.setGroupPermission(false);
        perm1.setFolderAdmin(true);
        perm1.setAllPermission(
            OCLPermission.ADMIN_PERMISSION,
            OCLPermission.ADMIN_PERMISSION,
            OCLPermission.ADMIN_PERMISSION,
            OCLPermission.ADMIN_PERMISSION);
        folder.setPermissionsAsArray(new OCLPermission[] { perm1 });
        if (module == FolderObject.INFOSTORE) {
            folder.setParentFolderID(FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID);
        } else {
            folder.setParentFolderID(FolderObject.SYSTEM_PUBLIC_FOLDER_ID);
        }

        InsertRequest request = new InsertRequest(EnumAPI.OX_OLD, folder, true);
        InsertResponse response = client.execute(request);
        response.fillObject(folder);
        return folder;
    }

    /**
     * Inserts and remembers a new file with random content and a random name.
     *
     * @param folderID The parent folder identifier
     * @return The inserted file
     * @throws Exception
     */
    protected File insertFile(int folderID) throws Exception {
        return insertFile(folderID, randomUID());
    }

    /**
     * Inserts and remembers a new file with random content and a random name.
     *
     * @param folderID The parent folder identifier
     * @param guestPermission The guest permission to assign
     * @return The inserted file
     * @throws Exception
     */
    protected File insertSharedFile(int folderID, FileStorageGuestObjectPermission guestPermission) throws Exception {
        return insertSharedFile(folderID, randomUID(), guestPermission);
    }

    /**
     * Inserts and remembers a new file with random content.
     *
     * @param folderID The parent folder identifier
     * @param filename The filename to use
     * @return The inserted file
     * @throws Exception
     */
    protected File insertFile(int folderID, String filename) throws Exception {
        return insertSharedFile(folderID, filename, null);
    }

    /**
     * Inserts and remembers a new shared file with random content.
     *
     * @param folderID The parent folder identifier
     * @param filename The filename to use
     * @param guestPermission The guest permission to assign
     * @return The inserted file
     * @throws Exception
     */
    protected File insertSharedFile(int folderID, String filename, FileStorageGuestObjectPermission guestPermission) throws Exception {
        byte[] contents = new byte[64 + random.nextInt(256)];
        random.nextBytes(contents);
        return insertSharedFile(folderID, filename, guestPermission, contents);
    }

    /**
     * Inserts and remembers a new shared file with random content.
     *
     * @param folderID The parent folder identifier
     * @param filename The filename to use
     * @param guestPermission The guest permission to assign
     * @param data The file contents
     * @return The inserted file
     * @throws Exception
     */
    protected File insertSharedFile(int folderID, String filename, FileStorageGuestObjectPermission guestPermission, byte[] data) throws Exception {
        DefaultFile metadata = new DefaultFile();
        metadata.setFolderId(String.valueOf(folderID));
        metadata.setFileName(filename);
        if (null != guestPermission) {
            metadata.setObjectPermissions(Collections.<FileStorageObjectPermission>singletonList(guestPermission));
        }
        NewInfostoreRequest newRequest = new NewInfostoreRequest(metadata, new ByteArrayInputStream(data));
        NewInfostoreResponse newResponse = getClient().execute(newRequest);
        String id = newResponse.getID();
        metadata.setId(id);
        GetInfostoreRequest getRequest = new GetInfostoreRequest(id);
        GetInfostoreResponse getResponse = client.execute(getRequest);
        File createdFile = getResponse.getDocumentMetadata();
        assertNotNull(createdFile);
        remember(createdFile);
        return createdFile;
    }

    /**
     * Updates and remembers a folder.
     *
     * @param api The folder tree to use
     * @param folder The folder to udpate
     * @return The udpated folder
     * @throws Exception
     */
    protected FolderObject updateFolder(EnumAPI api, FolderObject folder) throws Exception {
        InsertResponse insertResponse = client.execute(new UpdateRequest(api, folder));
        insertResponse.fillObject(folder);
        remember(folder);
        FolderObject updatedFolder = getFolder(api, folder.getObjectID());
        assertNotNull(updatedFolder);
        assertEquals("Folder name wrong", folder.getFolderName(), updatedFolder.getFolderName());
        return updatedFolder;
    }

    /**
     * Updates and remembers a file.
     *
     * @param file The file to udpate
     * @return The updated file, re-fetched from the server
     * @throws Exception
     */
    protected File updateFile(File file, Field[] modifiedColumns) throws Exception {
        UpdateInfostoreRequest updateInfostoreRequest = new UpdateInfostoreRequest(file, modifiedColumns, file.getLastModified());
        updateInfostoreRequest.setFailOnError(true);
        UpdateInfostoreResponse updateInfostoreResponse = getClient().execute(updateInfostoreRequest);
        assertFalse(updateInfostoreResponse.hasError());
        GetInfostoreRequest getInfostoreRequest = new GetInfostoreRequest(file.getId());
        getInfostoreRequest.setFailOnError(true);
        GetInfostoreResponse getInfostoreResponse = getClient().execute(getInfostoreRequest);
        return getInfostoreResponse.getDocumentMetadata();
    }

    /**
     * Gets a file by ID.
     *
     * @param id The identifier of the file to get
     * @return The file
     * @throws Exception
     */
    protected File getFile(String id) throws Exception {
        GetInfostoreRequest getInfostoreRequest = new GetInfostoreRequest(id);
        getInfostoreRequest.setFailOnError(true);
        GetInfostoreResponse getInfostoreResponse = getClient().execute(getInfostoreRequest);
        return getInfostoreResponse.getDocumentMetadata();
    }

    /**
     * Gets a folder by ID.
     *
     * @param api The folder API to use
     * @param objectID The ID of the folder to get
     * @return The folder
     * @throws Exception
     */
    protected FolderObject getFolder(EnumAPI api, int objectID) throws Exception {
        return getFolder(api, objectID, client);
    }

    /**
     * Gets a folder by ID with the given client.
     *
     * @param api The folder API to use
     * @param objectID The ID of the folder to get
     * @return The folder
     * @throws Exception
     */
    protected FolderObject getFolder(EnumAPI api, int objectID, AJAXClient client) throws Exception {
        GetResponse getResponse = client.execute(new GetRequest(api, objectID));
        FolderObject folder = getResponse.getFolder();
        folder.setLastModified(getResponse.getTimestamp());
        return getResponse.getFolder();
    }

    protected FolderObject insertFolder(EnumAPI api, FolderObject folder) throws Exception {
        InsertResponse insertResponse = client.execute(new InsertRequest(api, folder));
        insertResponse.fillObject(folder);
        remember(folder);
        FolderObject createdFolder = getFolder(api, folder.getObjectID());
        assertNotNull(createdFolder);
        assertEquals("Folder name wrong", folder.getFolderName(), createdFolder.getFolderName());
        return createdFolder;
    }

    /**
     * Remembers a folder for cleanup.
     *
     * @param folder The folder to remember
     */
    protected void remember(FolderObject folder) {
        if (null != folder) {
            foldersToDelete.put(Integer.valueOf(folder.getObjectID()), folder);
        }
    }

    /**
     * Remembers a file for cleanup.
     *
     * @param file The file to remember
     */
    protected void remember(File file) {
        if (null != file) {
            filesToDelete.put(file.getId(), file);
        }
    }

    /**
     * Gets all folder shares of a specific module.
     *
     * @param api The folder tree to use
     * @param module The module identifier
     * @return The folder shares
     */
    protected List<FolderShare> getFolderShares(EnumAPI api, int module) throws OXException, IOException, JSONException {
        return getFolderShares(client, api, module);
    }

    /**
     * Gets all folder shares of a specific module.
     *
     * @param client The ajax client to use
     * @param api The folder tree to use
     * @param module The module identifier
     * @return The folder shares
     */
    protected static List<FolderShare> getFolderShares(AJAXClient client, EnumAPI api, int module) throws OXException, IOException, JSONException {
        return client.execute(new FolderSharesRequest(api, Module.getModuleString(module, -1))).getShares();
    }

    /**
     * Discovers a specific guest permission entity amongst all available shares of the current user, based on the folder- and guest identifiers.
     *
     * @param api The folder tree to use
     * @param module The module identifier
     * @param folderID The folder ID to discover the share for
     * @param guest The ID of the guest associated to the share
     * @return The guest permission entity, or <code>null</code> if not found
     */
    protected ExtendedPermissionEntity discoverGuestEntity(EnumAPI api, int module, int folderID, int guest) throws OXException, IOException, JSONException {
        return discoverGuestEntity(client, api, module, folderID, guest);
    }

    /**
     * Discovers a specific guest permission entity amongst all available shares of the current user, based on the folder- and guest identifiers.
     *
     * @param client The ajax client to use
     * @param folderID The folder ID to discover the share for
     * @param guest The ID of the guest associated to the share
     * @return The share, or <code>null</code> if not found
     */
    protected static ExtendedPermissionEntity discoverGuestEntity(AJAXClient client, EnumAPI api, int module, int folderID, int guest) throws OXException, IOException, JSONException {
        List<FolderShare> shares = getFolderShares(client, api, module);
        for (FolderShare share : shares) {
            if (share.getObjectID() == folderID) {
                return discoverGuestEntity(share.getExtendedPermissions(), guest);
            }
        }
        return null;
    }

    /**
     * Discovers a specific guest permission entity amongst all available shares of the current user, based on the file- and guest identifiers.
     *
     * @param folder The folder ID to discover the share for
     * @param item The item ID to discover the share for
     * @param guest The ID of the guest associated to the share
     * @return The guest permission entity, or <code>null</code> if not found
     */
    protected ExtendedPermissionEntity discoverGuestEntity(String folder, String item, int guest) throws OXException, IOException, JSONException {
        return discoverGuestEntity(client, folder, item, guest);
    }

    /**
     * Discovers a specific guest permission entity amongst all available shares of the current user, based on the file- and guest identifiers.
     *
     * @param client The ajax client to use
     * @param folder The folder ID to discover the share for
     * @param item The item ID to discover the share for
     * @param guest The ID of the guest associated to the share
     * @return The share, or <code>null</code> if not found
     */
    protected static ExtendedPermissionEntity discoverGuestEntity(AJAXClient client, String folder, String item, int guest) throws OXException, IOException, JSONException {
        List<FileShare> shares = client.execute(new FileSharesRequest()).getShares();
        for (FileShare share : shares) {
            if (share.getId().equals(item)) {
                return discoverGuestEntity(share.getExtendedPermissions(), guest);
            }
        }
        return null;
    }

    /**
     * Discovers a specific share amongst all available shares of the current user, based on the folder- and guest identifiers.
     *
     * @param client The ajax client to use
     * @param folderID The folder ID to discover the share for
     * @param guest The ID of the guest associated to the share
     * @return The share, or <code>null</code> if not found
     */
    protected static ExtendedPermissionEntity discoverGuestEntity(List<ExtendedPermissionEntity> entities, int guest) throws OXException, IOException, JSONException {
        if (null != entities) {
            for (ExtendedPermissionEntity entity : entities) {
                if (entity.getEntity() == guest) {
                    return entity;
                }
            }
        }
        return null;
    }

    /**
     * Discovers a specific share amongst all available shares of the current user, based on the folder- and guest identifiers.
     *
     * @param client The ajax client to use
     * @param folderID The folder ID to discover the share for
     * @param guest The ID of the guest associated to the share
     * @return The share, or <code>null</code> if not found
     */
    protected static ParsedShare discoverShare(AJAXClient client, int folderID, int guest) throws OXException, IOException, JSONException {
        return discoverShare(client.execute(new AllRequest()).getParsedShares(), folderID, null, guest);
    }

    /**
     * Discovers a specific share amongst all available shares of the current user, based on the folder- and guest identifiers.
     *
     * @param client The ajax client to use
     * @param folderID The folder ID to discover the share for
     * @param item The item ID to discover the share for
     * @param guest The ID of the guest associated to the share
     * @return The share, or <code>null</code> if not found
     */
    protected static ParsedShare discoverShare(AJAXClient client, int folderID, String item, int guest) throws OXException, IOException, JSONException {
        return discoverShare(client.execute(new AllRequest()).getParsedShares(), folderID, item, guest);
    }

    /**
     * Discovers a specific share amongst the supplied shares, based on the folder- and guest identifiers.
     *
     * @param shares The shares to search
     * @param folderID The folder ID to discover the share for
     * @param guest The ID of the guest associated to the share
     * @return The share, or <code>null</code> if not found
     */
    protected static ParsedShare discoverShare(List<ParsedShare> shares, int folderID, int guest) throws OXException, IOException, JSONException {
        return discoverShare(shares, folderID, null, guest);
    }

    /**
     * Discovers a specific share amongst the supplied shares, based on the folder- and guest identifiers.
     *
     * @param shares The shares to search
     * @param folderID The folder ID to discover the share for
     * @param item The item ID to discover the share for
     * @param guest The ID of the guest associated to the share
     * @return The share, or <code>null</code> if not found
     */
    protected static ParsedShare discoverShare(List<ParsedShare> shares, int folderID, String item, int guest) throws OXException, IOException, JSONException {
        String folder = String.valueOf(folderID);
        for (ParsedShare share : shares) {
            if (folder.equals(share.getTarget().getFolder()) && guest == share.getGuest()) {
                if (item == null) {
                    if (share.getTarget().getItem() == null) {
                        return share;
                    }
                } else if (item.equals(share.getTarget().getItem())) {
                    return share;
                }
            }
        }
        return null;
    }

    /**
     * Discovers a specific share amongst all available shares of the current user, based on the guest identifier.
     *
     * @param guest The ID of the guest associated to the share
     * @param folderID The folder ID to discover the share for
     * @return The share, or <code>null</code> if not found
     */
    protected ParsedShare discoverShare(int guest, int folderID) throws OXException, IOException, JSONException {
        return discoverShare(client, folderID, null, guest);
    }

    /**
     * Discovers a specific share amongst all available shares of the current user, based on the guest identifier.
     *
     * @param guest The ID of the guest associated to the share
     * @param folderID The folder ID to discover the share for
     * @param item The item ID to discover the share for
     * @return The share, or <code>null</code> if not found
     */
    protected ParsedShare discoverShare(int guest, int folderID, String item) throws OXException, IOException, JSONException {
        return discoverShare(client, folderID, item, guest);
    }

    @Override
    protected void tearDown() throws Exception {
        if (null != client) {
            deleteFoldersSilently(client, foldersToDelete);
            deleteFilesSilently(client, filesToDelete.values());
        }
        super.tearDown();
    }

    protected static void deleteFoldersSilently(AJAXClient client, Map<Integer, FolderObject> foldersToDelete) throws Exception {
        deleteFoldersSilently(client, foldersToDelete.keySet());
    }

    protected static void deleteFoldersSilently(AJAXClient client, Collection<Integer> foldersIDs) throws Exception {
        if (null != client && null != foldersIDs && 0 < foldersIDs.size()) {
            Date futureTimestamp = new Date(System.currentTimeMillis() + 1000000);
            DeleteRequest deleteRequest = new DeleteRequest(EnumAPI.OX_NEW, Autoboxing.I2i(foldersIDs), futureTimestamp);
            deleteRequest.setHardDelete(Boolean.TRUE);
            client.execute(deleteRequest);
        }
    }

    protected static void deleteFilesSilently(AJAXClient client, Collection<File> files) throws Exception {
        if (null != client && null != files && 0 < files.size()) {
            Date futureTimestamp = new Date(System.currentTimeMillis() + 1000000);
            List<String> folderIDs = new ArrayList<String>();
            List<String> fileIDs = new ArrayList<String>();
            for (File file : files) {
                folderIDs.add(file.getFolderId());
                fileIDs.add(file.getId());
            }
            DeleteInfostoreRequest deleteInfostoreRequest = new DeleteInfostoreRequest(fileIDs, folderIDs, futureTimestamp);
            deleteInfostoreRequest.setHardDelete(Boolean.TRUE);
            client.execute(deleteInfostoreRequest);
        }
    }

    /**
     * Resolves the share behind a guest permission, i.e. accesses the share link and authenticates using the guest's credentials.
     *
     * @param guestPermission The guest permission entity
     * @param recipient The recipient
     * @return An authenticated guest client being able to access the share
     */
    protected GuestClient resolveShare(ExtendedPermissionEntity guestPermission, ShareRecipient recipient) throws Exception {
        return new GuestClient(guestPermission.getShareURL(), recipient);
    }

    /**
     * Resolves the share, i.e. accesses the share link and authenticates using the guest's credentials.
     *
     * @param shareURL The share URL
     * @param recipient The recipient
     * @return An authenticated guest client being able to access the share
     */
    protected GuestClient resolveShare(String shareURL, ShareRecipient recipient) throws Exception {
        return new GuestClient(shareURL, recipient);
    }

    /**
     * Resolves the share behind a guest permission, i.e. accesses the share link and authenticates using the supplied credentials.
     *
     * @param guestPermission The guest permission entity
     * @param username The username, or <code>null</code> if not needed
     * @param password The password, or <code>null</code> if not needed
     * @return An authenticated guest client being able to access the share
     */
    protected GuestClient resolveShare(ExtendedPermissionEntity guestPermission, String username, String password) throws Exception {
        return resolveShare(guestPermission.getShareURL(), username, password);
    }

    /**
     * Resolves the supplied share url, i.e. accesses the share link and authenticates using the supplied credentials.
     *
     * @param url The share URL
     * @param username The username, or <code>null</code> if not needed
     * @param password The password, or <code>null</code> if not needed
     * @return An authenticated guest client being able to access the share
     */
    protected GuestClient resolveShare(String url, String username, String password) throws Exception {
        return new GuestClient(url, username, password);
    }

    /**
     * Resolves the supplied share, i.e. accesses the share link and authenticates using the share's credentials.
     *
     * @param share The share
     * @param recipient The recipient
     * @return An authenticated guest client being able to access the share
     */
    protected GuestClient resolveShare(ParsedShare share, ShareRecipient recipient) throws Exception {
        return new GuestClient(share.getShareURL(), recipient);
    }

    /**
     * Resolves the supplied share url, i.e. accesses the share link and authenticates using the given user name but
     * sets no password and simulates the "skip password" behavior.
     *
     * @param url The share URL
     * @param username The username, or <code>null</code> if not needed
     * @return An authenticated guest client being able to access the share
     */
    protected GuestClient resolveShare(String url, String username) throws Exception {
        ClientConfig clientConfig = new GuestClient.ClientConfig(url)
            .setUsername(username)
            .setSkipPassword(true);
        return new GuestClient(clientConfig);
    }

    protected boolean awaitGuestCleanup(int guestID, long timeout) throws Exception {
        long until = System.currentTimeMillis() + timeout;
        do {
            com.openexchange.ajax.user.actions.GetResponse response = getClient().execute(
                new com.openexchange.ajax.user.actions.GetRequest(guestID, TimeZones.UTC, false));
            if (response.hasError() && null != response.getException()) {
                OXException e = response.getException();
                if ("USR-0010".equals(e.getErrorCode())) {
                    return true;
                } else if ("CON-0125".equals(e.getErrorCode())) {
                    // partly okay
                } else {
                    throw e;
                }
            }
            Thread.sleep(500);
        } while (System.currentTimeMillis() < until);
        return false;
    }

    protected void checkGuestUserDeleted(int guestID) throws Exception {
        assertTrue("Guest user " + guestID + " not deleted after " + CLEANUP_DELAY + "ms", awaitGuestCleanup(guestID, CLEANUP_DELAY));
    }

    /**
     * Checks the supplied OCL permissions against the expected guest permissions.
     *
     * @param expected The expected permissions
     * @param actual The actual permissions
     */
    protected static void checkPermissions(OCLGuestPermission expected, OCLPermission actual) {
        assertEquals("Permission wrong", expected.getDeletePermission(), actual.getDeletePermission());
        assertEquals("Permission wrong", expected.getFolderPermission(), actual.getFolderPermission());
        assertEquals("Permission wrong", expected.getReadPermission(), actual.getReadPermission());
        assertEquals("Permission wrong", expected.getWritePermission(), actual.getWritePermission());
    }

    /**
     * Checks the supplied object permissions against the expected guest permissions.
     *
     * @param expected The expected permissions
     * @param actual The actual permissions
     */
    protected static void checkPermissions(FileStorageGuestObjectPermission expected, FileStorageObjectPermission actual) {
        assertEquals("Permission wrong", expected.canDelete(), actual.canDelete());
        assertEquals("Permission wrong", expected.canWrite(), actual.canWrite());
        assertEquals("Permission wrong", expected.canRead(), actual.canRead());
    }

    /**
     * Checks the supplied share against the expected guest permissions.
     *
     * @param expectedPermission The expected permissions
     * @param expectedFile The expected file
     * @param actual The actual share
     */
    protected static void checkShare(FileStorageGuestObjectPermission expectedPermission, File expectedFile, ParsedShare actual) {
        assertNotNull("No share", actual);
//        assertEquals("Expiry date wrong", expected.getExpiryDate(), actual.getTarget().getExpiryDate());
        checkAuthentication(expectedPermission.getRecipient(), actual);
        checkRecipient(expectedPermission.getRecipient(), actual.getRecipient());
        assertNotNull("No share target", actual.getTarget());
        assertEquals("Target module wrong", FolderObject.INFOSTORE, actual.getTarget().getModule());
        assertEquals("Target folder wrong", expectedFile.getFolderId(), actual.getTarget().getFolder());
        assertEquals("Target item wrong", expectedFile.getId(), actual.getTarget().getItem());
    }

    /**
     * Checks the supplied share against the expected guest permissions.
     *
     * @param expectedPermission The expected permissions
     * @param expectedFolder The expected folder
     * @param actual The actual share
     */
    protected static void checkShare(OCLGuestPermission expectedPermission, FolderObject expectedFolder, ParsedShare actual) {
        assertNotNull("No share", actual);
        checkAuthentication(expectedPermission.getRecipient(), actual);
        checkRecipient(expectedPermission.getRecipient(), actual.getRecipient());
        assertNotNull("No share target", actual.getTarget());
        assertEquals("Target module wrong", expectedFolder.getModule(), actual.getTarget().getModule());
        assertEquals("Target folder wrong", String.valueOf(expectedFolder.getObjectID()), actual.getTarget().getFolder());
    }

    private static void checkAuthentication(ShareRecipient expected, ParsedShare actual) {
        if (RecipientType.ANONYMOUS.equals(expected.getType())) {
            if (null == ((AnonymousRecipient) expected).getPassword()) {
                assertEquals("Wrong authentication", AuthenticationMode.ANONYMOUS, actual.getAuthentication());
            } else {
                assertEquals("Wrong authentication", AuthenticationMode.ANONYMOUS_PASSWORD, actual.getAuthentication());
            }
        } else if (RecipientType.GUEST.equals(expected.getType())) {
            if (null == ((GuestRecipient) expected).getPassword()) {
                assertEquals("Wrong authentication", AuthenticationMode.GUEST, actual.getAuthentication());
            } else {
                assertEquals("Wrong authentication", AuthenticationMode.GUEST_PASSWORD, actual.getAuthentication());
            }
        }
    }

    /**
     * Checks the supplied extended guest permission against the expected guest permissions.
     *
     * @param expectedPermission The expected permissions
     * @param actual The actual extended permission
     */
    protected static void checkGuestPermission(FileStorageGuestObjectPermission expectedPermission, ExtendedPermissionEntity actual) {
        assertNotNull("No guest permission entitiy", actual);
//        assertEquals("Expiry date wrong", expected.getExpiryDate(), actual.getTarget().getExpiryDate());
//        checkAuthentication(expectedPermission.getRecipient(), actual);
//        checkRecipient(expectedPermission.getRecipient(), actual.getRecipient());
//        assertNotNull("No share target", actual.getTarget());
//        assertEquals("Target module wrong", FolderObject.INFOSTORE, actual.getTarget().getModule());
//        assertEquals("Target folder wrong", expectedFile.getFolderId(), actual.getTarget().getFolder());
//        assertEquals("Target item wrong", expectedFile.getId(), actual.getTarget().getItem());
    }

    /**
     * Checks the supplied extended guest permission against the expected guest permissions.
     *
     * @param expectedPermission The expected permissions
     * @param actual The actual extended permission
     */
    protected static void checkGuestPermission(OCLGuestPermission expectedPermission, ExtendedPermissionEntity actual) {
        assertNotNull("No guest permission entitiy", actual);
        assertEquals(expectedPermission.getPermissionBits(), actual.getBits());



//        checkAuthentication(expectedPermission.getRecipient(), actual);
//        checkRecipient(expectedPermission.getRecipient(), actual.getRecipient());
//        assertNotNull("No share target", actual.getTarget());
//        assertEquals("Target module wrong", expectedFolder.getModule(), actual.getTarget().getModule());
//        assertEquals("Target folder wrong", String.valueOf(expectedFolder.getObjectID()), actual.getTarget().getFolder());
    }

//    private static void checkAuthentication(ShareRecipient expected, ExtendedPermissionEntity actual) {
//        if (RecipientType.ANONYMOUS.equals(expected.getType())) {
//            if (null == ((AnonymousRecipient) expected).getPassword()) {
//                assertEquals("Wrong authentication", AuthenticationMode.ANONYMOUS, actual.getAuthentication());
//            } else {
//                assertEquals("Wrong authentication", AuthenticationMode.ANONYMOUS_PASSWORD, actual.getAuthentication());
//            }
//        } else if (RecipientType.GUEST.equals(expected.getType())) {
//            if (null == ((GuestRecipient) expected).getPassword()) {
//                assertEquals("Wrong authentication", AuthenticationMode.GUEST, actual.getAuthentication());
//            } else {
//                assertEquals("Wrong authentication", AuthenticationMode.GUEST_PASSWORD, actual.getAuthentication());
//            }
//        }
//    }

    private static void checkRecipient(ShareRecipient expected, ShareRecipient actual) {
        assertNotNull("No recipient", actual);
        assertEquals("Wrong recipient type", expected.getType(), actual.getType());
        if (RecipientType.ANONYMOUS.equals(expected.getType())) {
            assertEquals("Wrong password", ((AnonymousRecipient) expected).getPassword(), ((AnonymousRecipient) actual).getPassword());
        } else if (RecipientType.GUEST.equals(expected.getType())) {
            GuestRecipient expectedRecipient = (GuestRecipient) expected;;
            GuestRecipient actualRecipient = (GuestRecipient) actual;
            assertEquals("Wrong e-mail address", expectedRecipient.getEmailAddress(), actualRecipient.getEmailAddress());
//            assertEquals("Wrong display name address", expectedRecipient.getDisplayName(), actualRecipient.getDisplayName());
        }
    }

    protected static OCLGuestPermission createNamedGuestPermission(String emailAddress, String displayName, String password) {
        OCLGuestPermission guestPermission = createNamedPermission(emailAddress, displayName, password);
        guestPermission.setAllPermission(
            OCLPermission.READ_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS);
        guestPermission.getRecipient().setBits(guestPermission.getPermissionBits());
        return guestPermission;
    }

    protected static OCLGuestPermission createNamedGuestPermission(String emailAddress, String displayName) {
        OCLGuestPermission guestPermission = createNamedPermission(emailAddress, displayName);
        guestPermission.setAllPermission(
            OCLPermission.READ_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS);
        guestPermission.getRecipient().setBits(guestPermission.getPermissionBits());
        return guestPermission;
    }

    protected static OCLGuestPermission createNamedAuthorPermission(String emailAddress, String displayName, String password) {
        OCLGuestPermission guestPermission = createNamedPermission(emailAddress, displayName, password);
        guestPermission.setAllPermission(
            OCLPermission.CREATE_OBJECTS_IN_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.WRITE_ALL_OBJECTS, OCLPermission.DELETE_ALL_OBJECTS);
        guestPermission.getRecipient().setBits(guestPermission.getPermissionBits());
        return guestPermission;
    }

    protected static OCLGuestPermission createNamedAuthorPermission(String emailAddress, String displayName) {
        OCLGuestPermission guestPermission = createNamedPermission(emailAddress, displayName);
        guestPermission.setAllPermission(
            OCLPermission.CREATE_OBJECTS_IN_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.WRITE_ALL_OBJECTS, OCLPermission.DELETE_ALL_OBJECTS);
        guestPermission.getRecipient().setBits(guestPermission.getPermissionBits());
        return guestPermission;
    }

    protected static OCLGuestPermission createNamedPermission(String emailAddress, String displayName, String password) {
        OCLGuestPermission guestPermission = new OCLGuestPermission();
        GuestRecipient guestRecipient = new GuestRecipient();
        guestRecipient.setEmailAddress(emailAddress);
        guestRecipient.setDisplayName(displayName);
        guestRecipient.setPassword(password);
        guestPermission.setRecipient(guestRecipient);
        guestPermission.setGroupPermission(false);
        guestPermission.setFolderAdmin(false);
        guestPermission.getRecipient().setBits(guestPermission.getPermissionBits());
        return guestPermission;
    }

    protected static OCLGuestPermission createNamedPermission(String emailAddress, String displayName) {
        OCLGuestPermission guestPermission = new OCLGuestPermission();
        GuestRecipient guestRecipient = new GuestRecipient();
        guestRecipient.setEmailAddress(emailAddress);
        guestRecipient.setDisplayName(displayName);
        guestPermission.setRecipient(guestRecipient);
        guestPermission.setGroupPermission(false);
        guestPermission.setFolderAdmin(false);
        guestPermission.getRecipient().setBits(guestPermission.getPermissionBits());
        return guestPermission;
    }

    protected static OCLGuestPermission createAnonymousGuestPermission(String password) {
        OCLGuestPermission guestPermission = createAnonymousPermission(password);
        guestPermission.setAllPermission(
            OCLPermission.READ_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS);
        guestPermission.getRecipient().setBits(guestPermission.getPermissionBits());
        return guestPermission;
    }

//    protected static OCLGuestPermission createAnonymousAuthorPermission(String password) {
//        OCLGuestPermission guestPermission = createAnonymousPermission(password);
//        guestPermission.setAllPermission(
//            OCLPermission.CREATE_OBJECTS_IN_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.WRITE_ALL_OBJECTS, OCLPermission.DELETE_ALL_OBJECTS);
//        guestPermission.getRecipient().setBits(guestPermission.getPermissionBits());
//        return guestPermission;
//    }

    protected static OCLGuestPermission createAnonymousGuestPermission() {
        return createAnonymousGuestPermission(null);
    }

//    protected static OCLGuestPermission createAnonymousAuthorPermission() {
//        return createAnonymousAuthorPermission(null);
//    }

    protected static OCLGuestPermission createAnonymousPermission(String password) {
        AnonymousRecipient recipient = new AnonymousRecipient();
        recipient.setPassword(password);
        OCLGuestPermission guestPermission = new OCLGuestPermission(recipient);
        AnonymousRecipient anonymousRecipient = new AnonymousRecipient();
        anonymousRecipient.setPassword(password);
        guestPermission.setRecipient(anonymousRecipient);
        guestPermission.setGroupPermission(false);
        guestPermission.setFolderAdmin(false);
        guestPermission.getRecipient().setBits(guestPermission.getPermissionBits());
        return guestPermission;
    }

    protected static FileStorageGuestObjectPermission asObjectPermission(OCLGuestPermission guestPermission) {
        DefaultFileStorageGuestObjectPermission objectPermission = new DefaultFileStorageGuestObjectPermission();
        objectPermission.setEntity(guestPermission.getEntity());
        objectPermission.setExpiryDate(guestPermission.getExpiryDate());
        objectPermission.setGroup(guestPermission.isGroupPermission());
        objectPermission.setRecipient(guestPermission.getRecipient());
        if (guestPermission.canDeleteAllObjects()) {
            objectPermission.setPermissions(FileStorageObjectPermission.DELETE);
        } else if (guestPermission.canWriteAllObjects()) {
            objectPermission.setPermissions(FileStorageObjectPermission.WRITE);
        } else if (guestPermission.canReadAllObjects()) {
            objectPermission.setPermissions(FileStorageObjectPermission.READ);
        }
        objectPermission.getRecipient().setBits(objectPermission.getPermissions());
        return objectPermission;
    }

    protected int getDefaultFolder(int module) throws Exception {
        return getDefaultFolder(client, module);
    }

    protected static int getDefaultFolder(AJAXClient client, int module) throws Exception {
        switch (module) {
        case FolderObject.CONTACT:
            return client.getValues().getPrivateContactFolder();
        case FolderObject.CALENDAR:
            return client.getValues().getPrivateAppointmentFolder();
        case FolderObject.INFOSTORE:
            return client.getValues().getPrivateInfostoreFolder();
        case FolderObject.TASK:
            return client.getValues().getPrivateTaskFolder();
        default:
            Assert.fail("No default folder for moduel: " + module);
            return 0;
        }
    }

    protected static String randomUID() {
        return UUIDs.getUnformattedString(UUID.randomUUID());
    }

    protected static int randomModule() {
        return TESTED_MODULES[random.nextInt(TESTED_MODULES.length)];
    }

    protected static EnumAPI randomFolderAPI() {
        return TESTED_FOLDER_APIS[random.nextInt(TESTED_FOLDER_APIS.length)];
    }

    protected static OCLGuestPermission randomGuestPermission() {
        return TESTED_PERMISSIONS[random.nextInt(TESTED_PERMISSIONS.length)];
    }

    protected static FileStorageGuestObjectPermission randomGuestObjectPermission() {
        return TESTED_OBJECT_PERMISSIONS[random.nextInt(TESTED_OBJECT_PERMISSIONS.length)];
    }

    /**
     * Gets the username to use for login from the supplied share recipient.
     *
     * @param recipient The recipient
     * @return The username
     */
    public static String getUsername(ShareRecipient recipient) {
        switch (recipient.getType()) {
        case ANONYMOUS:
            return recipient.getType().toString().toLowerCase();
        case GUEST:
            return ((GuestRecipient) recipient).getEmailAddress();
        default:
            Assert.fail("Unknown recipient: " + recipient.getType());
            return null;
        }
    }

    /**
     * Gets the password to use for login from the supplied share recipient.
     *
     * @param recipient The recipient
     * @return The password, or <code>null</code> if not needed
     */
    public static String getPassword(ShareRecipient recipient) {
        switch (recipient.getType()) {
        case ANONYMOUS:
            return ((AnonymousRecipient) recipient).getPassword();
        case GUEST:
            return ((GuestRecipient) recipient).getPassword();
        default:
            Assert.fail("Unknown recipient: " + recipient.getType());
            return null;
        }
    }

}
