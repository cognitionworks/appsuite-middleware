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

package com.openexchange.folderstorage.database;

import gnu.trove.TIntObjectHashMap;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.openexchange.database.DBPoolingException;
import com.openexchange.folderstorage.FolderException;
import com.openexchange.folderstorage.FolderExceptionErrorMessage;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.database.getfolder.SystemInfostoreFolder;
import com.openexchange.folderstorage.database.getfolder.SystemPrivateFolder;
import com.openexchange.folderstorage.database.getfolder.SystemPublicFolder;
import com.openexchange.folderstorage.database.getfolder.SystemSharedFolder;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.folderstorage.type.SharedType;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.i18n.FolderStrings;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.userconfiguration.UserConfiguration;

/**
 * {@link DatabaseFolderConverter}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class DatabaseFolderConverter {

    private static interface FolderConverter {

        DatabaseFolder convert(FolderObject fo) throws FolderException;
    }

    private static final TIntObjectHashMap<FolderConverter> SYSTEM_CONVERTERS;

    private static final TIntObjectHashMap<FolderConverter> CONVERTERS;

    static {
        TIntObjectHashMap<FolderConverter> m = new TIntObjectHashMap<FolderConverter>(4);
        m.put(FolderObject.SYSTEM_PUBLIC_FOLDER_ID, new FolderConverter() {

            public DatabaseFolder convert(final FolderObject fo) throws FolderException {
                return SystemPublicFolder.getSystemPublicFolder(fo);
            }
        });
        m.put(FolderObject.SYSTEM_INFOSTORE_FOLDER_ID, new FolderConverter() {

            public DatabaseFolder convert(final FolderObject fo) throws FolderException {
                return SystemInfostoreFolder.getSystemInfostoreFolder(fo);
            }
        });
        m.put(FolderObject.SYSTEM_PRIVATE_FOLDER_ID, new FolderConverter() {

            public DatabaseFolder convert(final FolderObject fo) throws FolderException {
                return SystemPrivateFolder.getSystemPrivateFolder(fo);
            }
        });
        SYSTEM_CONVERTERS = m;

        m = new TIntObjectHashMap<FolderConverter>(4);
        m.put(FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID, new FolderConverter() {

            public DatabaseFolder convert(final FolderObject fo) throws FolderException {
                final DatabaseFolder retval = new LocalizedDatabaseFolder(fo);
                retval.setName(FolderStrings.SYSTEM_PUBLIC_INFOSTORE_FOLDER_NAME);
                return retval;
            }
        });
        m.put(FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID, new FolderConverter() {

            public DatabaseFolder convert(final FolderObject fo) throws FolderException {
                final DatabaseFolder retval = new LocalizedDatabaseFolder(fo);
                retval.setName(FolderStrings.SYSTEM_USER_INFOSTORE_FOLDER_NAME);
                return retval;
            }
        });
        m.put(FolderObject.SYSTEM_LDAP_FOLDER_ID, new FolderConverter() {

            public DatabaseFolder convert(final FolderObject fo) throws FolderException {
                final DatabaseFolder retval = new LocalizedDatabaseFolder(fo);
                retval.setName(FolderStrings.SYSTEM_LDAP_FOLDER_NAME);
                retval.setParentID(FolderStorage.PUBLIC_ID);
                return retval;
            }
        });
        m.put(FolderObject.SYSTEM_GLOBAL_FOLDER_ID, new FolderConverter() {

            public DatabaseFolder convert(final FolderObject fo) throws FolderException {
                final DatabaseFolder retval = new LocalizedDatabaseFolder(fo);
                retval.setName(FolderStrings.SYSTEM_GLOBAL_FOLDER_NAME);
                retval.setParentID(FolderStorage.PUBLIC_ID);
                return retval;
            }
        });
        CONVERTERS = m;
    }

    /**
     * Initializes a new {@link DatabaseFolderConverter}.
     */
    private DatabaseFolderConverter() {
        super();
    }

    /**
     * Converts specified {@link FolderObject} instance to a {@link DatabaseFolder} instance.
     * 
     * @param fo The {@link FolderObject} instance
     * @param user The user
     * @param userConfiguration The user configuration
     * @param ctx The context
     * @param con The connection
     * @return The converted {@link DatabaseFolder} instance
     * @throws FolderException If conversion fails
     */
    public static DatabaseFolder convert(final FolderObject fo, final User user, final UserConfiguration userConfiguration, final Context ctx, final Connection con) throws FolderException {
        try {
            final int folderId = fo.getObjectID();
            if (FolderObject.SYSTEM_SHARED_FOLDER_ID == folderId) {
                /*
                 * The system shared folder
                 */
                return SystemSharedFolder.getSystemSharedFolder(fo, user, userConfiguration, ctx, con);
            }
            /*
             * Look-up a system converter
             */
            FolderConverter folderConverter = SYSTEM_CONVERTERS.get(folderId);
            if (null != folderConverter) {
                /*
                 * Return immediately
                 */
                return folderConverter.convert(fo);
            }
            /*
             * Look-up a converter
             */
            final DatabaseFolder retval;
            folderConverter = CONVERTERS.get(folderId);
            if (null != folderConverter) {
                retval = folderConverter.convert(fo);
            } else if (fo.isDefaultFolder()) {
                /*
                 * A default folder: set locale-sensitive name
                 */
                final int module = fo.getModule();
                if (module == FolderObject.TASK) {
                    retval = new LocalizedDatabaseFolder(fo);
                    retval.setName(FolderStrings.DEFAULT_TASK_FOLDER_NAME);
                } else if (module == FolderObject.CONTACT) {
                    retval = new LocalizedDatabaseFolder(fo);
                    retval.setName(FolderStrings.DEFAULT_CONTACT_FOLDER_NAME);
                } else if (module == FolderObject.CALENDAR) {
                    retval = new LocalizedDatabaseFolder(fo);
                    retval.setName(FolderStrings.DEFAULT_CALENDAR_FOLDER_NAME);
                } else {
                    retval = new DatabaseFolder(fo);
                }
            } else {
                retval = new DatabaseFolder(fo);
            }
            if (PrivateType.getInstance().equals(retval.getType()) && user.getId() != retval.getCreatedBy()) {
                retval.setType(SharedType.getInstance());
                /*
                 * A shared folder has no subfolders in real tree
                 */
                retval.setSubfolderIDs(new String[0]);
                retval.setSubscribedSubfolders(false);
                retval.setCacheable(false);
                retval.setParentID(new StringBuilder(16).append(FolderObject.SHARED_PREFIX).append(retval.getCreatedBy()).toString());
            } else {
                /*
                 * Set subfolders for non-private folder. For private folder FolderStorage.getSubfolders() is supposed to be used.
                 */
                final List<Integer> subfolderIds = FolderObject.getSubfolderIds(folderId, ctx, con);
                if (subfolderIds.isEmpty()) {
                    retval.setSubfolderIDs(new String[0]);
                    retval.setSubscribedSubfolders(false);
                } else {
                    final List<String> tmp = new ArrayList<String>(subfolderIds.size());
                    for (final Integer id : subfolderIds) {
                        tmp.add(id.toString());
                    }
                    retval.setSubfolderIDs(tmp.toArray(new String[tmp.size()]));
                    retval.setSubscribedSubfolders(true);
                }
            }
            return retval;
        } catch (final DBPoolingException e) {
            throw new FolderException(e);
        } catch (final SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        }
    }

}
