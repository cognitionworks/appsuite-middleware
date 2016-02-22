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

package com.openexchange.caldav.resources;

import java.util.ArrayList;
import java.util.List;
import com.openexchange.caldav.GroupwareCaldavFactory;
import com.openexchange.caldav.mixins.ScheduleDefaultCalendarURL;
import com.openexchange.caldav.mixins.ScheduleDefaultTasksURL;
import com.openexchange.caldav.mixins.SupportedCalendarComponentSets;
import com.openexchange.caldav.mixins.SupportedReportSet;
import com.openexchange.dav.resources.DAVCollection;
import com.openexchange.dav.resources.DAVRootCollection;
import com.openexchange.dav.resources.PlaceholderCollection;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.FolderResponse;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.Type;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.database.contentType.CalendarContentType;
import com.openexchange.folderstorage.database.contentType.TaskContentType;
import com.openexchange.folderstorage.mail.contentType.TrashContentType;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.folderstorage.type.SharedType;
import com.openexchange.groupware.container.CommonObject;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.webdav.protocol.WebdavPath;
import com.openexchange.webdav.protocol.WebdavProtocolException;
import com.openexchange.webdav.protocol.WebdavResource;

/**
 * {@link CalDAVRootCollection}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class CalDAVRootCollection extends DAVRootCollection {

    /**
     * The reserved tree identifier for MS Outlook folder tree: <code>"1"</code>.
     * (copied from com.openexchange.folderstorage.outlook)
     */
    private static final String OUTLOOK_TREE_ID = "1";

    private final GroupwareCaldavFactory factory;
    private String trashFolderID;
    private List<UserizedFolder> subfolders;

    /**
     * Initializes a new {@link CalDAVRootCollection}.
     *
     * @param factory the factory
     */
    public CalDAVRootCollection(GroupwareCaldavFactory factory) {
        super(factory, "Calendars");
        this.factory = factory;
        super.includeProperties(
            new SupportedCalendarComponentSets(SupportedCalendarComponentSets.VEVENT, SupportedCalendarComponentSets.VTODO),
            new ScheduleDefaultCalendarURL(factory), new ScheduleDefaultTasksURL(factory), new SupportedReportSet()
        );
    }

    protected FolderService getFolderService() {
        return factory.getFolderService();
    }

    protected List<UserizedFolder> getSubfolders() throws OXException {
        if (null == this.subfolders) {
            this.subfolders = getVisibleFolders();
        }
        return subfolders;
    }

    @Override
    public DAVCollection getChild(String name) throws WebdavProtocolException {
        try {
            for (UserizedFolder folder : getSubfolders()) {
                if (name.equals(folder.getID())) {
                    LOG.debug("{}: found child collection by name '{}'", this.getUrl(), name);
                    return createCollection(folder);
                }
                if (null != folder.getMeta() && folder.getMeta().containsKey("resourceName") && name.equals(folder.getMeta().get("resourceName"))) {
                    LOG.debug("{}: found child collection by resource name '{}'", this.getUrl(), name);
                    return createCollection(folder);
                }
            }
            LOG.debug("{}: child collection '{}' not found, creating placeholder collection", this.getUrl(), name);
            return new PlaceholderCollection<CommonObject>(factory, constructPathForChildResource(name), CalendarContentType.getInstance(), factory.getState().getTreeID());
        } catch (OXException e) {
            throw protocolException(e);
        }
    }

    private CalDAVFolderCollection<?> createCollection(UserizedFolder folder) throws OXException {
        if (TaskContentType.getInstance().equals(folder.getContentType())) {
            return new TaskCollection(factory, constructPathForChildResource(folder), folder);
        } else if (CalendarContentType.getInstance().equals(folder.getContentType())) {
            return new AppointmentCollection(factory, constructPathForChildResource(folder), folder);
        } else {
            throw new UnsupportedOperationException("content type " + folder.getContentType() + " not supported");
        }
    }

    private CalDAVFolderCollection<?> createCollection(UserizedFolder folder, int order) throws OXException {
        if (TaskContentType.getInstance().equals(folder.getContentType())) {
            return new TaskCollection(factory, constructPathForChildResource(folder), folder, order);
        } else if (CalendarContentType.getInstance().equals(folder.getContentType())) {
            return new AppointmentCollection(factory, constructPathForChildResource(folder), folder, order);
        } else {
            throw new UnsupportedOperationException("content type " + folder.getContentType() + " not supported");
        }
    }

    @Override
    public List<WebdavResource> getChildren() throws WebdavProtocolException {
        List<WebdavResource> children = new ArrayList<WebdavResource>();
        int calendarOrder = 0;
        try {
            for (UserizedFolder folder : getSubfolders()) {
                children.add(createCollection(folder, ++calendarOrder));
                LOG.debug("{}: adding folder collection for folder '{}' as child resource.", getUrl(), folder.getName());
            }
        } catch (OXException e) {
            throw protocolException(e);
        }

        LOG.debug("{}: got {} child resources.", getUrl(), children.size());
        return children;
    }

    /**
     * Constructs a string representing the WebDAV name for a folder resource.
     *
     * @param folder the folder to construct the name for
     * @return the name
     */
    private String constructNameForChildResource(UserizedFolder folder) {
        return folder.getID();
    }

    private WebdavPath constructPathForChildResource(UserizedFolder folder) {
        return constructPathForChildResource(constructNameForChildResource(folder));
    }

    /**
     * Gets a list of all visible and subscribed task- and calendar-folders in the configured folder tree.
     *
     * @return The visible folders
     * @throws FolderException
     */
    private List<UserizedFolder> getVisibleFolders() throws OXException {
        UserPermissionBits permissionBits = ServerSessionAdapter.valueOf(factory.getSession()).getUserPermissionBits();
        List<UserizedFolder> folders = new ArrayList<UserizedFolder>();
        if (permissionBits.hasCalendar()) {
            folders.addAll(getVisibleFolders(PrivateType.getInstance(), CalendarContentType.getInstance()));
            if (permissionBits.hasFullPublicFolderAccess()) {
                folders.addAll(getVisibleFolders(PublicType.getInstance(), CalendarContentType.getInstance()));
            }
            if (permissionBits.hasFullSharedFolderAccess()) {
                folders.addAll(getVisibleFolders(SharedType.getInstance(), CalendarContentType.getInstance()));
            }
        }
        if (permissionBits.hasTask()) {
            folders.addAll(getVisibleFolders(PrivateType.getInstance(), TaskContentType.getInstance()));
        }
        return folders;
    }

    /**
     * Gets a list containing all visible folders of the given {@link Type}.
     * @param type
     * @return
     * @throws FolderException
     */
    private List<UserizedFolder> getVisibleFolders(Type type, ContentType contentType) throws OXException {
        List<UserizedFolder> folders = new ArrayList<UserizedFolder>();
        FolderResponse<UserizedFolder[]> visibleFoldersResponse = getFolderService().getVisibleFolders(
                factory.getState().getTreeID(), contentType, type, false, factory.getSession(), null);
        UserizedFolder[] response = visibleFoldersResponse.getResponse();
        for (UserizedFolder folder : response) {
            if (Permission.READ_OWN_OBJECTS < folder.getOwnPermission().getReadPermission() && false == isTrashFolder(folder)) {
                folders.add(folder);
            }
        }
        return folders;
    }

    /**
     * Gets the id of the default trash folder
     *
     * @return
     */
    private String getTrashFolderID() {
        if (null == trashFolderID) {
            try {
                trashFolderID = getFolderService().getDefaultFolder(factory.getUser(), OUTLOOK_TREE_ID,
                        TrashContentType.getInstance(), factory.getSession(), null).getID();
            } catch (OXException e) {
                LOG.warn("unable to determine default trash folder", e);
            }
        }
        return this.trashFolderID;
    }

    /**
     * Checks whether the supplied folder is a trash folder, i.e. one of it's parent folders is the default trash folder.
     * <p/>
     * Only applicable when using the {@link #OUTLOOK_TREE_ID}.
     *
     * @param folder The folder to check
     * @return <code>true</code> if the folder is a trash folder, <code>false</code>, otherwise
     * @throws OXException
     */
    private boolean isTrashFolder(UserizedFolder folder) throws OXException {
        if (OUTLOOK_TREE_ID.equals(factory.getState().getTreeID()) && PrivateType.getInstance().equals(folder.getType()) &&
            ServerSessionAdapter.valueOf(factory.getSession()).getUserPermissionBits().hasOLOX20()) {
            String trashFolderId = this.getTrashFolderID();
            if (null != trashFolderId) {
                if (trashFolderId.equals(folder.getParentID())) {
                    return true;
                }
                FolderResponse<UserizedFolder[]> pathResponse = getFolderService().getPath(
                        OUTLOOK_TREE_ID, folder.getID(), this.factory.getSession(), null);
                UserizedFolder[] response = pathResponse.getResponse();
                for (UserizedFolder parentFolder : response) {
                    if (trashFolderId.equals(parentFolder.getID())) {
                        LOG.debug("Detected folder below trash: {}", folder);
                        return true;
                    }
                }
            } else {
                LOG.warn("No config value for trash folder id found");
            }
        }
        return false;
    }

}
