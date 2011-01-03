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

package com.openexchange.tools.oxfolder;

import static com.openexchange.tools.oxfolder.OXFolderUtility.folderModule2String;
import static com.openexchange.tools.oxfolder.OXFolderUtility.getUserName;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.api2.OXException;
import com.openexchange.cache.impl.FolderCacheManager;
import com.openexchange.database.DBPoolingException;
import com.openexchange.database.provider.DBPoolProvider;
import com.openexchange.database.provider.StaticDBPoolProvider;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.groupware.calendar.AppointmentSqlFactoryService;
import com.openexchange.groupware.contact.Contacts;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.infostore.InfostoreFacade;
import com.openexchange.groupware.infostore.facade.impl.InfostoreFacadeImpl;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.tasks.Tasks;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.server.impl.DBPool;
import com.openexchange.server.impl.EffectivePermission;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.oxfolder.OXFolderException.FolderCode;

/**
 * {@link OXFolderAccess} - Provides access to OX folders.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class OXFolderAccess {

    /**
     * A connection with "read-only" capability.
     */
    private final Connection readCon;

    /**
     * The associated context
     */
    private final Context ctx;

    /**
     * Initializes a new {@link OXFolderAccess}.
     * <p>
     * Since the access is created with a connection with "read-only" capability, an appropriate connection is going to be fetched from DB
     * pool every time when needed.
     * 
     * @param ctx The context
     */
    public OXFolderAccess(final Context ctx) {
        this(null, ctx);
    }

    /**
     * Initializes a new {@link OXFolderAccess}.
     * 
     * @param readCon A connection with "read-only" capability or <code>null</code> to let the access fetch an appropriate connection from
     *            DB pool every time when needed
     * @param ctx The context
     */
    public OXFolderAccess(final Connection readCon, final Context ctx) {
        super();
        this.readCon = readCon;
        this.ctx = ctx;
    }

    /**
     * Tests if the folder associated with specified folder ID exists.
     * 
     * @param folderId The folder ID
     * @return <code>true</code> if the folder associated with specified folder ID exists; otherwise <code>false</code>
     * @throws OXException If an error occurs while checking existence
     */
    public boolean exists(final int folderId) throws OXException {
        try {
            getFolderObject(folderId);
            return true;
        } catch (final OXFolderNotFoundException e) {
            return false;
        } catch (final OXFolderException e) {
            if (OXFolderException.FolderCode.NOT_EXISTS.getNumber() == e.getDetailNumber()) {
                return false;
            }
            throw e;
        } catch (final OXException e) {
            if (EnumComponent.FOLDER.equals(e.getComponent()) && OXFolderException.FolderCode.NOT_EXISTS.getNumber() == e.getDetailNumber()) {
                return false;
            }
            throw e;
        }
    }

    /**
     * Loads matching <code>com.openexchange.groupware.container.FolderObject</code> instance either from cache (if enabled) or from
     * underlying storage.
     * 
     * @param folderId - the folder ID
     * @return matching <code>com.openexchange.groupware.container.FolderObject</code> instance
     * @throws OXException
     */
    public final FolderObject getFolderObject(final int folderId) throws OXException {
        final FolderObject fo;
        if (FolderCacheManager.isEnabled()) {
            fo = FolderCacheManager.getInstance().getFolderObject(folderId, true, ctx, readCon);
        } else {
            fo = FolderObject.loadFolderObjectFromDB(folderId, ctx, readCon);
        }
        return fo;
    }

    /**
     * Creates a <code>java.util.List</code> of <code>FolderObject</code> instances which match given folder IDs.
     * 
     * @param folderIDs - the folder IDs as an <code>int</code> array
     * @return a <code>java.util.List</code> of <code>FolderObject</code> instances
     * @throws OXException
     */
    public final List<FolderObject> getFolderObjects(final int[] folderIDs) throws OXException {
        final List<FolderObject> retval = new ArrayList<FolderObject>(folderIDs.length);
        for (final int fuid : folderIDs) {
            try {
                retval.add(getFolderObject(fuid));
            } catch (final OXFolderNotFoundException e) {
                continue;
            }
        }
        return retval;
    }

    /**
     * Creates a <code>java.util.List</code> of <code>FolderObject</code> instances fills which match given folder IDs.
     * 
     * @param folderIDs - the folder IDs backed by a <code>java.util.Collection</code>
     * @return a <code>java.util.List</code> of <code>FolderObject</code> instances
     * @throws OXException
     */
    public final List<FolderObject> getFolderObjects(final Collection<Integer> folderIDs) throws OXException {
        final int size = folderIDs.size();
        final List<FolderObject> retval = new ArrayList<FolderObject>(size);
        final Iterator<Integer> iter = folderIDs.iterator();
        for (int i = 0; i < size; i++) {
            try {
                retval.add(getFolderObject(iter.next().intValue()));
            } catch (final OXFolderNotFoundException e) {
                continue;
            }
        }
        return retval;
    }

    /**
     * Determines folder type. The returned value is either <code>FolderObject.PRIVATE</code>, <code>FolderObject.PUBLIC</code> or
     * <code>FolderObject.SHARED</code>. <b>NOTE:</b> This method assumes that given user has read access!
     * 
     * @param folderId - the folder ID
     * @param userId - the user ID
     * @return the folder type
     * @throws OXException
     */
    public final int getFolderType(final int folderId, final int userId) throws OXException {
        return getFolderObject(folderId).getType(userId);
    }

    /**
     * Determines the <b>plain</b> folder type meaning the returned value is either <code>FolderObject.PRIVATE</code> or
     * <code>FolderObject.PUBLIC</code>. <b>NOTE:</b> Do not use this method to check if folder is shared (<code>FolderObject.SHARED</code>
     * ), use {@link #getFolderType(int, int)} instead.
     * 
     * @param folderId - the folder ID
     * @return the folder type
     * @throws OXException
     * @see <code>getFolderType(int, int)</code>
     */
    public final int getFolderType(final int folderId) throws OXException {
        return getFolderObject(folderId).getType();
    }

    /**
     * Determines folder module.
     * 
     * @param folderId - the folder ID
     * @return folder module
     * @throws OXException
     */
    public final int getFolderModule(final int folderId) throws OXException {
        return getFolderObject(folderId).getModule();
    }

    /**
     * Determines folder owner.
     * 
     * @param folderId - the folder ID
     * @return folder owner
     * @throws OXException
     */
    public final int getFolderOwner(final int folderId) throws OXException {
        return getFolderObject(folderId).getCreatedBy();
    }

    /**
     * Determines if folder is shared. <b>NOTE:</b> This method assumes that given user has read access!
     * 
     * @param folderId - the folder ID
     * @param userId - the user ID
     * @return <code>true</code> if folder is shared, otherwise <code>false</code>
     * @throws OXException
     */
    public final boolean isFolderShared(final int folderId, final int userId) throws OXException {
        return (getFolderType(folderId, userId) == FolderObject.SHARED);
    }

    /**
     * Determines if folder is an user's default folder.
     * 
     * @param folderId - the folder ID
     * @return <code>true</code> if folder is marked as a default folder, otherwise <code>false</code>
     * @throws OXException
     */
    public final boolean isDefaultFolder(final int folderId) throws OXException {
        return getFolderObject(folderId).isDefaultFolder();
    }

    /**
     * Determines given folder's name.
     * 
     * @param folderId - the folder ID
     * @return folder name
     * @throws OXException
     */
    public String getFolderName(final int folderId) throws OXException {
        return getFolderObject(folderId).getFolderName();
    }

    /**
     * Determines given folder's parent ID.
     * 
     * @param folderId - the folder ID
     * @return folder parent ID
     * @throws OXException
     */
    public int getParentFolderID(final int folderId) throws OXException {
        return getFolderObject(folderId).getParentFolderID();
    }

    /**
     * Determines given folder's last modifies date.
     * 
     * @param folderId
     * @return folder's last modifies date
     * @throws OXException
     */
    public Date getFolderLastModified(final int folderId) throws OXException {
        return getFolderObject(folderId).getLastModified();
    }

    /**
     * Determines user's effective permission on the folder matching given folder ID.
     * 
     * @param folderId - the folder ID
     * @param userId - the user ID
     * @param userConfig - the user configuration
     * @return user's effective permission
     * @throws OXException
     */
    public final EffectivePermission getFolderPermission(final int folderId, final int userId, final UserConfiguration userConfig) throws OXException {
        try {
            final FolderObject fo = getFolderObject(folderId);
            return fo.getEffectiveUserPermission(userId, userConfig, readCon);
        } catch (final SQLException e) {
            throw new OXFolderException(FolderCode.SQL_ERROR, e, e.getMessage());
        } catch (final DBPoolingException e) {
            throw new OXFolderException(FolderCode.DBPOOLING_ERROR, e, Integer.valueOf(ctx.getContextId()));
        }
    }

    /**
     * Determines user's default folder of given module.
     * 
     * @param userId - the user ID
     * @param module - the module
     * @return user's default folder of given module
     * @throws OXException
     */
    public final FolderObject getDefaultFolder(final int userId, final int module) throws OXException {
        try {
            final int folderId = OXFolderSQL.getUserDefaultFolder(userId, module, readCon, ctx);
            if (folderId == -1) {
                throw new OXFolderException(
                    FolderCode.NO_DEFAULT_FOLDER_FOUND,
                    folderModule2String(module),
                    getUserName(userId, ctx),
                    Integer.valueOf(ctx.getContextId()));
            }
            return getFolderObject(folderId);
        } catch (final SQLException e) {
            throw new OXFolderException(FolderCode.SQL_ERROR, e, e.getMessage());
        } catch (final DBPoolingException e) {
            throw new OXFolderException(FolderCode.DBPOOLING_ERROR, e, Integer.valueOf(ctx.getContextId()));
        }
    }

    /**
     * Determines if session's user is allowed to delete all objects located in given folder.
     * <p>
     * <b>Note</b>: This method checks only by contained items and does <small><b>NOT</b></small> check by the user's effective folder
     * permission itself. Thus the user is supposed to hold sufficient folder permissions on specified folder.
     * 
     * @param fo - the folder object
     * @param session - current user session
     * @param ctx - the context
     * @return
     * @throws OXException
     */
    public final boolean canDeleteAllObjectsInFolder(final FolderObject fo, final Session session, final Context ctx) throws OXException {
        final int userId = session.getUserId();
        final UserConfiguration userConfig = UserConfigurationStorage.getInstance().getUserConfigurationSafe(userId, ctx);
        try {
            /*
             * Check user permission on folder
             */
            final OCLPermission oclPerm = fo.getEffectiveUserPermission(userId, userConfig, readCon);
            if (!oclPerm.isFolderVisible()) {
                /*
                 * Folder is not visible to user
                 */
                return false;
            }
            if (oclPerm.canDeleteAllObjects()) {
                /*
                 * Can delete all objects
                 */
                return true;
            }
            if (oclPerm.canDeleteOwnObjects()) {
                /*
                 * User may only delete own objects. Check if folder contains foreign objects which must not be deleted.
                 */
                return !containsForeignObjects(fo, session, ctx);
            }
            /*
             * No delete permission: Return true if folder is empty
             */
            return isEmpty(fo, session, ctx);
        } catch (final SQLException e) {
            throw new OXFolderException(FolderCode.SQL_ERROR, e, e.getMessage());
        } catch (final DBPoolingException e) {
            throw new OXFolderException(FolderCode.DBPOOLING_ERROR, e, Integer.valueOf(ctx.getContextId()));
        } catch (final Throwable t) {
            throw new OXFolderException(FolderCode.RUNTIME_ERROR, t, Integer.valueOf(ctx.getContextId()));
        }
    }

    /**
     * Checks if given folder contains session-user-foreign objects.
     * 
     * @param fo The folder to check
     * @param session The session
     * @param ctx The context
     * @return <code>true</code> if given folder contains session-user-foreign objects; otherwise <code>false</code>
     * @throws OXException If check fails
     */
    public final boolean containsForeignObjects(final FolderObject fo, final Session session, final Context ctx) throws OXException {
        try {
            final int userId = session.getUserId();
            final int module = fo.getModule();
            if (module == FolderObject.TASK) {
                final Tasks tasks = Tasks.getInstance();
                if (null == readCon) {
                    Connection rc = null;
                    try {
                        rc = DBPool.pickup(ctx);
                        return tasks.containsNotSelfCreatedTasks(session, rc, fo.getObjectID());
                    } catch (final DBPoolingException e) {
                        throw new OXException(e);
                    } finally {
                        if (null != rc) {
                            DBPool.closeReaderSilent(ctx, rc);
                        }
                    }
                }
                return tasks.containsNotSelfCreatedTasks(session, readCon, fo.getObjectID());
            } else if (module == FolderObject.CALENDAR) {
                final AppointmentSQLInterface calSql =
                    ServerServiceRegistry.getInstance().getService(AppointmentSqlFactoryService.class).createAppointmentSql(session);
                if (readCon == null) {
                    return calSql.checkIfFolderContainsForeignObjects(userId, fo.getObjectID());
                }
                return calSql.checkIfFolderContainsForeignObjects(userId, fo.getObjectID(), readCon);
            } else if (module == FolderObject.CONTACT) {
                if (readCon == null) {
                    return Contacts.containsForeignObjectInFolder(fo.getObjectID(), userId, session);
                }
                return Contacts.containsForeignObjectInFolder(fo.getObjectID(), userId, session, readCon);
            } else if (module == FolderObject.PROJECT) {
                return false;
            } else if (module == FolderObject.INFOSTORE) {
                final InfostoreFacade db =
                    new InfostoreFacadeImpl(readCon == null ? new DBPoolProvider() : new StaticDBPoolProvider(readCon));
                final UserConfiguration userConfig = UserConfigurationStorage.getInstance().getUserConfigurationSafe(userId, ctx);
                return db.hasFolderForeignObjects(fo.getObjectID(), ctx, UserStorage.getStorageUser(session.getUserId(), ctx), userConfig);
            } else {
                throw new OXFolderException(FolderCode.UNKNOWN_MODULE, folderModule2String(module), Integer.valueOf(ctx.getContextId()));
            }
        } catch (final DBPoolingException e) {
            throw new OXFolderException(FolderCode.DBPOOLING_ERROR, e, Integer.valueOf(ctx.getContextId()));
        } catch (final SQLException e) {
            throw new OXFolderException(FolderCode.SQL_ERROR, e, e.getMessage());
        } catch (final Throwable t) {
            throw new OXFolderException(FolderCode.RUNTIME_ERROR, t, Integer.valueOf(ctx.getContextId()));
        }
    }

    /**
     * Checks if given folder is empty.
     * 
     * @param fo The folder to check
     * @param session The session
     * @param ctx The context
     * @return <code>true</code> if given folder is empty; otherwise <code>false</code>
     * @throws OXException If checking emptiness fails
     */
    public final boolean isEmpty(final FolderObject fo, final Session session, final Context ctx) throws OXException {
        try {
            final int userId = session.getUserId();
            final int module = fo.getModule();
            if (FolderObject.TASK == module) {
                final Tasks tasks = Tasks.getInstance();
                return readCon == null ? tasks.isFolderEmpty(ctx, fo.getObjectID()) : tasks.isFolderEmpty(ctx, readCon, fo.getObjectID());
            } else if (FolderObject.CALENDAR == module) {
                final AppointmentSQLInterface calSql =
                    ServerServiceRegistry.getInstance().getService(AppointmentSqlFactoryService.class).createAppointmentSql(session);
                return readCon == null ? calSql.isFolderEmpty(userId, fo.getObjectID()) : calSql.isFolderEmpty(
                    userId,
                    fo.getObjectID(),
                    readCon);
            } else if (FolderObject.CONTACT == module) {
                return readCon == null ? !Contacts.containsAnyObjectInFolder(fo.getObjectID(), ctx) : !Contacts.containsAnyObjectInFolder(
                    fo.getObjectID(),
                    readCon,
                    ctx);
            } else if (FolderObject.PROJECT == module) {
                return true;
            } else if (FolderObject.INFOSTORE == module) {
                final InfostoreFacade db =
                    new InfostoreFacadeImpl(readCon == null ? new DBPoolProvider() : new StaticDBPoolProvider(readCon));
                return db.isFolderEmpty(fo.getObjectID(), ctx);
            } else {
                throw new OXFolderException(FolderCode.UNKNOWN_MODULE, folderModule2String(module), Integer.valueOf(ctx.getContextId()));
            }
        } catch (final SQLException e) {
            throw new OXFolderException(FolderCode.SQL_ERROR, e, e.getMessage());
        } catch (final Throwable t) {
            throw new OXFolderException(FolderCode.RUNTIME_ERROR, t, Integer.valueOf(ctx.getContextId()));
        }
    }

}
