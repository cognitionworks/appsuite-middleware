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

package com.openexchange.folderstorage.internal.performers;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.Folder;
import com.openexchange.folderstorage.FolderException;
import com.openexchange.folderstorage.FolderServiceDecorator;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.FolderStorageDiscoverer;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.SortableId;
import com.openexchange.folderstorage.StorageParameters;
import com.openexchange.folderstorage.Type;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.internal.CalculatePermission;
import com.openexchange.folderstorage.internal.UserizedFolderImpl;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.folderstorage.type.SharedType;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.tools.TimeZoneUtils;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link AbstractUserizedFolderPerformer} - Abstract super class for actions which return one or multiple instances of
 * {@link UserizedFolder}.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class AbstractUserizedFolderPerformer extends AbstractPerformer {

    private final FolderServiceDecorator decorator;

    private volatile TimeZone timeZone;

    private volatile Locale locale;

    private volatile java.util.List<ContentType> allowedContentTypes;

    /**
     * Initializes a new {@link AbstractUserizedFolderPerformer}.
     * 
     * @param session The session
     * @param decorator The optional folder service decorator
     */
    public AbstractUserizedFolderPerformer(final ServerSession session, final FolderServiceDecorator decorator) {
        super(session);
        this.decorator = decorator;
        storageParameters.setDecorator(decorator);
    }

    /**
     * Initializes a new {@link AbstractUserizedFolderPerformer}.
     * 
     * @param user The user
     * @param context The context
     * @param decorator The optional folder service decorator
     */
    public AbstractUserizedFolderPerformer(final User user, final Context context, final FolderServiceDecorator decorator) {
        super(user, context);
        this.decorator = decorator;
        storageParameters.setDecorator(decorator);
    }

    /**
     * Initializes a new {@link AbstractUserizedFolderPerformer}.
     * 
     * @param session The session
     * @param decorator The optional folder service decorator
     * @param folderStorageDiscoverer The folder storage discoverer
     */
    public AbstractUserizedFolderPerformer(final ServerSession session, final FolderServiceDecorator decorator, final FolderStorageDiscoverer folderStorageDiscoverer) {
        super(session, folderStorageDiscoverer);
        this.decorator = decorator;
        storageParameters.setDecorator(decorator);
    }

    /**
     * Initializes a new {@link AbstractUserizedFolderPerformer}.
     * 
     * @param user The user
     * @param context The context
     * @param decorator The optional folder service decorator
     * @param folderStorageDiscoverer The folder storage discoverer
     */
    public AbstractUserizedFolderPerformer(final User user, final Context context, final FolderServiceDecorator decorator, final FolderStorageDiscoverer folderStorageDiscoverer) {
        super(user, context, folderStorageDiscoverer);
        this.decorator = decorator;
        storageParameters.setDecorator(decorator);
    }

    /**
     * Gets the optional folder service decorator.
     * 
     * @return The folder service decorator or <code>null</code>
     */
    protected FolderServiceDecorator getDecorator() {
        return decorator;
    }

    /**
     * Gets the time zone.
     * <p>
     * If a {@link FolderServiceDecorator decorator} was set and its {@link FolderServiceDecorator#getTimeZone() getTimeZone()} method
     * returns a non-<code>null</code> value, then decorator's time zone is returned; otherwise user's time zone is returned.
     * 
     * @return The time zone
     */
    protected TimeZone getTimeZone() {
        TimeZone tmp = timeZone;
        if (null == tmp) {
            synchronized (this) {
                tmp = timeZone;
                if (null == tmp) {
                    final TimeZone tz = null == decorator ? null : decorator.getTimeZone();
                    timeZone = tmp = (tz == null ? TimeZoneUtils.getTimeZone(getUser().getTimeZone()) : tz);
                }
            }
        }
        return tmp;
    }

    /**
     * Gets the locale.
     * <p>
     * If a {@link FolderServiceDecorator decorator} was set and its {@link FolderServiceDecorator#getLocale() getLocale()} method returns a
     * non-<code>null</code> value, then decorator's locale is returned; otherwise user's locale is returned.
     * 
     * @return The locale
     */
    protected Locale getLocale() {
        Locale tmp = locale;
        if (null == tmp) {
            synchronized (this) {
                tmp = locale;
                if (null == tmp) {
                    final Locale l = null == decorator ? null : decorator.getLocale();
                    locale = tmp = l == null ? getUser().getLocale() : l;
                }
            }
        }
        return tmp;
    }

    /**
     * Gets the allowed content types.
     * 
     * @return The allowed content types
     */
    protected java.util.List<ContentType> getAllowedContentTypes() {
        java.util.List<ContentType> tmp = allowedContentTypes;
        if (null == tmp) {
            synchronized (this) {
                tmp = allowedContentTypes;
                if (null == tmp) {
                    allowedContentTypes = tmp = null == decorator ? ALL_ALLOWED : decorator.getAllowedContentTypes();
                }
            }
        }
        return tmp;
    }

    /**
     * Gets the user-sensitive folder for given folder.
     * 
     * @param folder The folder
     * @param ownPermission The user's permission on given folder
     * @param treeId The tree identifier
     * @param all <code>true</code> to add all subfolders; otherwise <code>false</code> to only add subscribed ones
     * @param nullIsPublicAccess <code>true</code> if a <code>null</code> value obtained from {@link Folder#getSubfolderIDs()} hints to
     *            publicly accessible folder; otherwise <code>false</code>
     * @param storageParameters The storage parameters to use
     * @param openedStorages The list of opened storages
     * @return The user-sensitive folder for given folder
     * @throws FolderException If a folder error occurs
     */
    protected UserizedFolder getUserizedFolder(final Folder folder, final Permission ownPermission, final String treeId, final boolean all, final boolean nullIsPublicAccess, final StorageParameters storageParameters, final java.util.Collection<FolderStorage> openedStorages) throws FolderException {
        final UserizedFolder userizedFolder = new UserizedFolderImpl(folder);
        /*-
         * 
        if (folder.isGlobalID()) {
            // Set default folder flag
            final String id = folder.getID();
            final FolderStorage folderStorage = getOpenedStorage(id, treeId, storageParameters, openedStorages);
            final ContentType contentType = folder.getContentType();
            if (id.equals(folderStorage.getDefaultFolderID(getUser(), treeId, contentType, storageParameters))) {
                // Default folder of current user
                userizedFolder.setDefault(true);
                userizedFolder.setDefaultType(contentType.getModule());
            } else {
                // Not a default folder
                userizedFolder.setDefault(false);
                userizedFolder.setDefaultType(0);
            }
        }
         */
        userizedFolder.setLocale(getLocale());
        /*
         * Permissions
         */
        userizedFolder.setOwnPermission(ownPermission);
        CalculatePermission.calculateUserPermissions(userizedFolder, getContext());
        /*
         * Type
         */
        final boolean isShared;
        {
            final Type type = userizedFolder.getType();
            if (SharedType.getInstance().equals(type) || userizedFolder.getCreatedBy() != getUserId() && PrivateType.getInstance().equals(
                type)) {
                userizedFolder.setType(SharedType.getInstance());
                userizedFolder.setSubfolderIDs(new String[0]);
                isShared = true;
            } else {
                isShared = false;
            }
        }
        // Modify parent
        if (isShared) {
            userizedFolder.setParentID(FolderObject.SHARED_PREFIX + userizedFolder.getCreatedBy());
            userizedFolder.setDefault(false);
            
            // Remain tree if parent is viewable, too.
            //final FolderStorage parentStorage = getOpenedStorage(treeId, folder.getParentID(), storageParameters, openedStorages);
            //final Folder parent = parentStorage.getFolder(treeId, folder.getParentID(), storageParameters);
            //final Permission permission = CalculatePermission.calculate(parent, session, getAllowedContentTypes());
            //if (!permission.isVisible()) {
            //   userizedFolder.setParentID(FolderObject.SHARED_PREFIX + userizedFolder.getCreatedBy());
            //}
            //userizedFolder.setDefault(false);
        }
        if (userizedFolder.getID().startsWith(FolderObject.SHARED_PREFIX)) {
            userizedFolder.setParentID(FolderStorage.SHARED_ID);
        }
        /*
         * Time zone offset and last-modified in UTC
         */
        {
            final Date cd = folder.getCreationDate();
            if (null != cd) {
                userizedFolder.setCreationDate(new Date(addTimeZoneOffset(cd.getTime(), getTimeZone())));
            }
        }
        {
            final Date lm = folder.getLastModified();
            if (null != lm) {
                final long time = lm.getTime();
                userizedFolder.setLastModified(new Date(addTimeZoneOffset(time, getTimeZone())));
                userizedFolder.setLastModifiedUTC(new Date(time));
            }
        }
        if (!isShared) {
            /*
             * Compute user-sensitive subfolders
             */
            hasVisibleSubfolderIDs(folder, treeId, all, userizedFolder, nullIsPublicAccess, storageParameters, openedStorages);
        }
        return userizedFolder;
    }

    private void hasVisibleSubfolderIDs(final Folder folder, final String treeId, final boolean all, final UserizedFolder userizedFolder, final boolean nullIsPublicAccess, final StorageParameters storageParameters, final java.util.Collection<FolderStorage> openedStorages) throws FolderException {
        /*
         * Subfolders
         */
        final String[] subfolders = folder.getSubfolderIDs();
        final java.util.List<String> visibleSubfolderIds = new ArrayList<String>(1);
        if (null == subfolders) {
            if (nullIsPublicAccess) {
                /*
                 * A null value hints to a special folder; e.g. a system folder which contains subfolder for all users
                 */
                visibleSubfolderIds.add("dummyId");
            } else {
                /*
                 * Get appropriate storages and start transaction
                 */
                final String folderId = folder.getID();
                final FolderStorage[] ss = folderStorageDiscoverer.getFolderStoragesForParent(treeId, folderId);
                for (int i = 0; visibleSubfolderIds.isEmpty() && i < ss.length; i++) {
                    final FolderStorage curStorage = ss[i];
                    boolean alreadyOpened = false;
                    final Iterator<FolderStorage> it = openedStorages.iterator();
                    for (int j = 0; !alreadyOpened && j < openedStorages.size(); j++) {
                        if (it.next().equals(curStorage)) {
                            alreadyOpened = true;
                        }
                    }
                    if (!alreadyOpened && curStorage.startTransaction(storageParameters, false)) {
                        openedStorages.add(curStorage);
                    }
                    final SortableId[] visibleIds = curStorage.getSubfolders(treeId, folderId, storageParameters);
                    if (visibleIds.length > 0) {
                        /*
                         * Found a storage which offers visible subfolder(s)
                         */
                        for (int j = 0; visibleSubfolderIds.isEmpty() && j < visibleIds.length; j++) {
                            final String id = visibleIds[0].getId();
                            final Folder subfolder = curStorage.getFolder(treeId, id, storageParameters);
                            if (all || (subfolder.isSubscribed() || subfolder.hasSubscribedSubfolders())) {
                                final Permission p = CalculatePermission.calculate(subfolder, session, getAllowedContentTypes());
                                if (p.isVisible()) {
                                    visibleSubfolderIds.add(id);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            final int length = subfolders.length;
            if (length > 0) {
                /*
                 * Check until a visible subfolder is found in case of a global folder
                 */
                if (folder.isGlobalID()) {
                    for (int i = 0; visibleSubfolderIds.isEmpty() && i < length; i++) {
                        final String id = subfolders[i];
                        final FolderStorage tmp = getOpenedStorage(id, treeId, storageParameters, openedStorages);
                        /*
                         * Get subfolder from appropriate storage
                         */
                        final Folder subfolder = tmp.getFolder(treeId, id, storageParameters);
                        /*
                         * Check for access rights and subscribed status dependent on parameter "all"
                         */
                        if (all || (subfolder.isSubscribed() || subfolder.hasSubscribedSubfolders())) {
                            final Permission subfolderPermission;
                            if (null == getSession()) {
                                subfolderPermission = CalculatePermission.calculate(subfolder, getUser(), getContext(), getAllowedContentTypes());
                            } else {
                                subfolderPermission = CalculatePermission.calculate(subfolder, getSession(), getAllowedContentTypes());
                            }
                            if (subfolderPermission.isVisible()) {
                                visibleSubfolderIds.add(id);
                            }
                        }
                    }
                } else if (all || folder.hasSubscribedSubfolders()) { // User-only folder
                    visibleSubfolderIds.add("dummyId");
                }
            }
        }
        userizedFolder.setSubfolderIDs(visibleSubfolderIds.isEmpty() ? new String[0] : new String[] { visibleSubfolderIds.get(0) });
    }

    private static long addTimeZoneOffset(final long date, final TimeZone timeZone) {
        return (date + timeZone.getOffset(date));
    }

}
