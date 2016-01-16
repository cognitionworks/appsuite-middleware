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

package com.openexchange.carddav;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.carddav.resources.RootCollection;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.contact.ContactFieldOperand;
import com.openexchange.contact.ContactService;
import com.openexchange.contact.SortOptions;
import com.openexchange.contact.vcard.VCardService;
import com.openexchange.contact.vcard.storage.VCardStorageFactory;
import com.openexchange.contact.vcard.storage.VCardStorageService;
import com.openexchange.dav.DAVFactory;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.FolderResponse;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.Type;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.database.contentType.ContactContentType;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.folderstorage.type.SharedType;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.search.Order;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.java.Strings;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ConstantOperand;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.tools.session.SessionHolder;
import com.openexchange.user.UserService;
import com.openexchange.webdav.protocol.Protocol;
import com.openexchange.webdav.protocol.WebdavCollection;
import com.openexchange.webdav.protocol.WebdavPath;
import com.openexchange.webdav.protocol.WebdavProtocolException;
import com.openexchange.webdav.protocol.WebdavResource;

/**
 * {@link GroupwareCarddavFactory}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class GroupwareCarddavFactory extends DAVFactory {

    private static final String OVERRIDE_NEXT_SYNC_TOKEN_PROPERTY = "com.openexchange.carddav.overridenextsynctoken";
    private static final String FOLDER_BLACKLIST_PROPERTY = "com.openexchange.carddav.ignoreFolders";
    private static final String FOLDER_TRRE_ID_PROPERTY = "com.openexchange.carddav.tree";
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GroupwareCarddavFactory.class);

    private final ThreadLocal<State> stateHolder;

    /**
     * Initializes a new {@link GroupwareCarddavFactory}.
     *
     * @param protocol The protocol
     * @param services A service lookup reference
     * @param sessionHolder The session holder to use
     */
    public GroupwareCarddavFactory(Protocol protocol, ServiceLookup services, SessionHolder sessionHolder) {
        super(protocol, services, sessionHolder);
        this.stateHolder = new ThreadLocal<State>();
    }

    @Override
    protected String getURLPrefix() {
        return "/carddav/";
    }

    @Override
    public void beginRequest() {
        stateHolder.set(new State(this));
        super.beginRequest();
    }

    @Override
    public void endRequest(int status) {
        stateHolder.set(null);
        super.endRequest(status);
    }

    @Override
    public WebdavCollection resolveCollection(WebdavPath url) throws WebdavProtocolException {
        if (0 == url.size()) {
            /*
             * this is the root collection
             */
            return mixin(new RootCollection(this));
        } else if (1 == url.size()) {
            /*
             * get child collection from root by name
             */
            return mixin(new RootCollection(this).getChild(url.name()));
        } else {
            throw WebdavProtocolException.Code.GENERAL_ERROR.create(url, HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    public WebdavResource resolveResource(WebdavPath url) throws WebdavProtocolException {
        if (2 == url.size()) {
            /*
             * get child resource from parent collection by name
             */
            return mixin(new RootCollection(this).getChild(url.parent().name()).getChild(url.name()));
        } else {
            return resolveCollection(url);
        }
    }

    public User resolveUser(int userID) throws OXException {
        return getService(UserService.class).getUser(userID, getContext());
    }

    public FolderService getFolderService() {
        return getService(FolderService.class);
    }

    public ContactService getContactService() throws OXException {
        return getService(ContactService.class);
    }

    public State getState() {
        return stateHolder.get();
    }

    public VCardService getVCardService() {
        return getService(VCardService.class);
    }

    public VCardStorageService getVCardStorageService(int contextId) {
        VCardStorageFactory vCardStorageFactory = getOptionalService(VCardStorageFactory.class);
        if (vCardStorageFactory != null) {
            return vCardStorageFactory.getVCardStorageService(getService(ConfigViewFactory.class), contextId);
        }
        return null;
    }

    public String getConfigValue(String key, String defaultValue) throws OXException {
        ConfigView view = getService(ConfigViewFactory.class).getView(getUser().getId(), getContext().getContextId());
        ComposedConfigProperty<String> property = view.property(key, String.class);
        return property.isDefined() ? property.get() : defaultValue;
    }

    /**
     * (Optionally) Gets coerced property value from configuration.
     *
     * @param property The property name
     * @param coerceTo The type to coerce to
     * @param defaultValue The default value
     * @return The coerced value or <code>defaultValue</code> if absent
     */
    public <T> T optConfigValue(String property, Class<T> coerceTo, T defaultValue) throws OXException {
        ConfigView view = getService(ConfigViewFactory.class).getView(getUser().getId(), getContext().getContextId());
        return view.opt(property, coerceTo, defaultValue);
    }

    /**
     * Sets the next sync token for the current user to <code>"0"</code>,
     * enforcing the next sync status report to contain all changes
     * independently of the sync token supplied by the client, thus emulating
     * some kind of slow-sync this way.
     */
    public void overrideNextSyncToken() {
        this.setOverrideNextSyncToken("0");
    }

    /**
     * Sets the next sync token for the current user to the supplied value.
     *
     * @param value The overridden value
     */
    public void setOverrideNextSyncToken(String value) {
        try {
            getService(UserService.class).setUserAttribute(getOverrideNextSyncTokenAttributeName(), value, getUser().getId(), getContext());
        } catch (OXException e) {
            LOG.error("", e);
        }
    }

    /**
     * Gets a value indicating the overridden sync token for the current user if defined
     *
     * @return The value of the overridden sync-token, or <code>null</code> if not set
     */
    public String getOverrideNextSyncToken() {
        try {
            return getService(UserService.class).getUserAttribute(getOverrideNextSyncTokenAttributeName(), getUser().getId(), getContext());
        } catch (OXException e) {
            LOG.error("", e);
        }
        return null;
    }

    private String getOverrideNextSyncTokenAttributeName() {
        String userAgent = (String) getSession().getParameter("user-agent");
        return null != userAgent ? OVERRIDE_NEXT_SYNC_TOKEN_PROPERTY + userAgent.hashCode() : OVERRIDE_NEXT_SYNC_TOKEN_PROPERTY;
    }

    public static final class State {

        private static final ContactField[] BASIC_FIELDS = { ContactField.OBJECT_ID, ContactField.LAST_MODIFIED, ContactField.CREATION_DATE, ContactField.UID, ContactField.FILENAME, ContactField.FOLDER_ID, ContactField.VCARD_ID
        };

        private final GroupwareCarddavFactory factory;
        private Map<String, Contact> uidCache = null;
        private Map<String, Contact> filenameCache = null;
        private List<UserizedFolder> allFolders = null;
        private List<UserizedFolder> reducedFolders = null;
        private HashSet<String> folderBlacklist = null;
        private UserizedFolder defaultFolder = null;
        private String treeID = null;
        private Date overallLastModified = null;
        private Long maxVCardSize = null;
        private Long maxUploadSize = null;

        /**
         * Initializes a new {@link State}.
         *
         * @param factory the CardDAV factory
         */
        public State(final GroupwareCarddavFactory factory) {
            super();
            this.factory = factory;
        }

        /**
         * Loads a {@link Contact} containing all data identified by the supplied uid
         *
         * @param uid
         * @return
         * @throws AbstractOXException
         */
        public Contact load(String uid) throws OXException {
            Contact contact = this.get(uid);
            if (null == contact) {
                LOG.debug("Contact '{}' not found, unable to load.", uid);
                return null;
            } else {
                return this.load(contact);
            }
        }

        public Contact load(Contact contact) throws OXException {
            return load(contact, null);
        }

        /**
         * Loads a {@link Contact} containing all data identified by object-
         * and parent folder id found in the supplied contact.
         *
         * @param contact
         * @return
         * @throws OXException
         * @throws ContextException
         */
        public Contact load(Contact contact, ContactField[] fields) throws OXException {
            if (null == contact) {
                throw new IllegalArgumentException("contact is null");
            } else if (false == contact.containsObjectID() || false == contact.containsParentFolderID()) {
                throw new IllegalArgumentException("need at least object- and parent folder id");
            }
            return this.load(contact.getObjectID(), Integer.toString(contact.getParentFolderID()), fields);
        }

        /**
         * Gets all contacts, each containing the basic information as defined by
         * the <code>FIELDS_FOR_ALL_REQUEST</code> array.
         *
         * @return
         * @throws AbstractOXException
         */
        public Collection<Contact> getContacts() throws OXException {
            return this.getUidCache().values();
        }

        public Collection<Contact> getContacts(String folderID) throws OXException {
            List<Contact> contacts = new ArrayList<Contact>();
            int parsedFolderID = Tools.parse(folderID);
            for (Contact contact : getContacts()) {
                if (contact.getParentFolderID() == parsedFolderID) {
                    contacts.add(contact);
                }
            }
            return contacts;
        }

        /**
         * Gets the default contact folder.
         *
         * @return the default folder.
         * @throws OXException
         */
        public UserizedFolder getDefaultFolder() throws OXException {
            if (null == this.defaultFolder) {
                this.defaultFolder = factory.getFolderService().getDefaultFolder(factory.getUser(), getTreeID(), ContactContentType.getInstance(), factory.getSession(), null);
            }
            return this.defaultFolder;
        }

        /**
         * Gets the folder identified by the supplied id.
         *
         * @param id
         * @return
         * @throws OXException
         */
        public UserizedFolder getFolder(String id) throws OXException {
            final List<UserizedFolder> folders = this.getFolders();
            for (final UserizedFolder folder : folders) {
                if (id.equals(folder.getID())) {
                    return folder;
                }
            }
            LOG.warn("Folder '{}' not found.", id);
            return null;
        }

        /**
         * Gets a list of all folders.
         *
         * @return
         * @throws FolderException
         * @throws ConfigCascadeException
         * @throws OXException
         */
        public List<UserizedFolder> getFolders() throws OXException {
            if (null == this.allFolders) {
                this.allFolders = getVisibleFolders();
            }
            return this.allFolders;
        }

        public List<UserizedFolder> getReducedFolders() throws OXException {
            if (null == this.reducedFolders) {
                reducedFolders = new ArrayList<UserizedFolder>();
                UserizedFolder defaultContactsFolder = getDefaultFolder();
                if (false == isBlacklisted(defaultContactsFolder)) {
                    reducedFolders.add(defaultContactsFolder);
                }
                try {
                    UserizedFolder globalAddressBookFolder = factory.getFolderService().getFolder(FolderStorage.REAL_TREE_ID, FolderStorage.GLOBAL_ADDRESS_BOOK_ID, factory.getSession(), null);
                    if (false == isBlacklisted(globalAddressBookFolder)) {
                        reducedFolders.add(globalAddressBookFolder);
                    }
                } catch (OXException e) {
                    if (Category.CATEGORY_PERMISSION_DENIED.equals(e.getCategory())) {
                        LOG.debug("No permission for global addressbook, skipping.", e);
                    } else {
                        throw e;
                    }
                }
            }
            return this.reducedFolders;
        }

        /**
         * Gets a list of all visible folders.
         *
         * @return
         * @throws FolderException
         */
        private List<UserizedFolder> getVisibleFolders() throws OXException {
            UserPermissionBits permissionBits = ServerSessionAdapter.valueOf(factory.getSession()).getUserPermissionBits();
            List<UserizedFolder> folders = new ArrayList<UserizedFolder>();
            folders.addAll(this.getVisibleFolders(PrivateType.getInstance()));
            if (permissionBits.hasFullPublicFolderAccess()) {
                folders.addAll(this.getVisibleFolders(PublicType.getInstance()));
            }
            if (permissionBits.hasFullSharedFolderAccess()) {
                folders.addAll(this.getVisibleFolders(SharedType.getInstance()));
            }
            return folders;
        }

        /**
         * Gets a list containing all visible folders of the given {@link Type}.
         *
         * @param type The folder type
         * @return The visible folders
         * @throws FolderException
         */
        private List<UserizedFolder> getVisibleFolders(Type type) throws OXException {
            List<UserizedFolder> folders = new ArrayList<UserizedFolder>();
            FolderService folderService = this.factory.getFolderService();
            FolderResponse<UserizedFolder[]> visibleFoldersResponse = folderService.getVisibleFolders(FolderStorage.REAL_TREE_ID, ContactContentType.getInstance(), type, true, this.factory.getSession(), null);
            UserizedFolder[] response = visibleFoldersResponse.getResponse();
            for (UserizedFolder folder : response) {
                if (Permission.READ_OWN_OBJECTS < folder.getOwnPermission().getReadPermission() && false == this.isBlacklisted(folder)) {
                    folders.add(folder);
                }
            }
            return folders;
        }

        public List<UserizedFolder> getDeletedFolders(Date since) throws OXException {
            List<UserizedFolder> folders = new ArrayList<UserizedFolder>();
            FolderService folderService = this.factory.getFolderService();
            FolderResponse<UserizedFolder[][]> updatedFoldersResponse = folderService.getUpdates(FolderStorage.REAL_TREE_ID, since, false, new ContentType[] { ContactContentType.getInstance() }, this.factory.getSession(), null);
            UserizedFolder[][] response = updatedFoldersResponse.getResponse();
            if (2 <= response.length && null != response[1] && 0 < response[1].length) {
                for (UserizedFolder folder : response[1]) {
                    if (Permission.READ_OWN_OBJECTS < folder.getOwnPermission().getReadPermission() && false == this.isBlacklisted(folder) && ContactContentType.getInstance().equals(folder.getContentType())) {
                        folders.add(folder);
                    }
                }
            }
            return folders;
        }

        /**
         * Gets an aggregated {@link Date} representing the last modification time of all resources.
         *
         * @return
         * @throws AbstractOXException
         */
        public Date getLastModified() throws OXException {
            if (null == this.overallLastModified) {
                overallLastModified = new Date(0);
                for (UserizedFolder folder : this.getFolders()) {
                    overallLastModified = Tools.getLatestModified(overallLastModified, getLastModified(folder, overallLastModified));
                }
            }
            return overallLastModified;
        }

        /**
         * Gets the last modification time of the supplied folder, including its contents.
         * This covers the folder's last modification time itself, and both updated and
         * deleted items inside the folder.
         *
         * @param folder the folder to get the last modification time for
         * @param since the minimum last modification time to consider
         * @return
         * @throws AbstractOXException
         */
        public Date getLastModified(UserizedFolder folder, Date since) throws OXException {
            ContactField[] fields = new ContactField[] { ContactField.LAST_MODIFIED };
            Date lastModified = Tools.getLatestModified(since, folder);
            SearchIterator<Contact> iterator = null;
            SortOptions sortOptions = new SortOptions(ContactField.LAST_MODIFIED, Order.DESCENDING);
            sortOptions.setLimit(1);
            try {
                iterator = factory.getContactService().getModifiedContacts(factory.getSession(), folder.getID(), lastModified, fields, sortOptions);
                if (iterator.hasNext()) {
                    Contact contact = iterator.next();
                    if (false == contact.getMarkAsDistribtuionlist()) {
                        lastModified = Tools.getLatestModified(lastModified, contact);
                    }
                }
            } finally {
                close(iterator);
            }
            try {
                iterator = factory.getContactService().getDeletedContacts(factory.getSession(), folder.getID(), lastModified, fields, sortOptions);
                if (iterator.hasNext()) {
                    Contact contact = iterator.next();
                    if (false == contact.getMarkAsDistribtuionlist()) {
                        lastModified = Tools.getLatestModified(lastModified, contact);
                    }
                }
            } finally {
                close(iterator);
            }
            return lastModified;
        }

        public Date getLastModified(UserizedFolder folder) throws OXException {
            return getLastModified(folder, new Date(0));
        }

        /**
         * Gets an aggregated list of all modified contacts in all folders since the supplied {@link Date}.
         *
         * @param since
         * @return
         * @throws AbstractOXException
         */
        public List<Contact> getModifiedContacts(Date since) throws OXException {
            List<Contact> contacts = new ArrayList<Contact>();
            List<UserizedFolder> folders = this.getFolders();
            for (UserizedFolder folder : folders) {
                contacts.addAll(this.getModifiedContacts(since, folder.getID()));
            }
            return contacts;
        }

        /**
         * Gets a list of all modified contacts in a folder since the supplied {@link Date}.
         *
         * @param since
         * @param folderID
         * @return
         * @throws AbstractOXException
         */
        public List<Contact> getModifiedContacts(Date since, String folderID) throws OXException {
            List<Contact> contacts = new ArrayList<Contact>();
            SearchIterator<Contact> iterator = null;
            try {
                iterator = factory.getContactService().getModifiedContacts(factory.getSession(), folderID, since, BASIC_FIELDS);
                while (iterator.hasNext()) {
                    Contact contact = iterator.next();
                    if (false == contact.getMarkAsDistribtuionlist()) {
                        contacts.add(contact);
                    }
                }
            } finally {
                close(iterator);
            }
            return contacts;
        }

        /**
         * Gets an aggregated list of all deleted contacts in all folders since the supplied {@link Date}.
         *
         * @param since
         * @return
         * @throws AbstractOXException
         */
        public List<Contact> getDeletedContacts(Date since) throws OXException {
            List<Contact> contacts = new ArrayList<Contact>();
            List<UserizedFolder> folders = this.getFolders();
            for (UserizedFolder folder : folders) {
                contacts.addAll(this.getDeletedContacts(since, folder.getID()));
            }
            List<UserizedFolder> deletedFolders = this.getDeletedFolders(since);
            for (UserizedFolder deletedFolder : deletedFolders) {
                List<Contact> deletedContacts = this.getDeletedContacts(since, deletedFolder.getID());
                LOG.debug("Detected deleted folder: {}, containing {} contacts.", deletedFolder, deletedContacts.size());
                contacts.addAll(deletedContacts);
                //                contacts.addAll(this.getDeletedContacts(since, deletedFolder.getID()));
            }
            return contacts;
        }

        /**
         * Gets an aggregated list of all deleted contacts in a folder since the supplied {@link Date}.
         *
         * @param since
         * @param folderID
         * @return
         * @throws AbstractOXException
         */
        public List<Contact> getDeletedContacts(Date since, String folderID) throws OXException {
            List<Contact> contacts = new ArrayList<Contact>();
            SearchIterator<Contact> iterator = null;
            try {
                iterator = factory.getContactService().getDeletedContacts(factory.getSession(), folderID, since, BASIC_FIELDS);
                while (iterator.hasNext()) {
                    Contact contact = iterator.next();
                    if (false == contact.getMarkAsDistribtuionlist()) {
                        contacts.add(contact);
                    }
                }
            } finally {
                close(iterator);
            }
            return contacts;
        }

        /**
         * Determines whether the supplied folder is blacklisted and should be ignored or not.
         *
         * @param userizedFolder
         * @return
         */
        private boolean isBlacklisted(UserizedFolder userizedFolder) {
            if (null == this.folderBlacklist) {
                String ignoreFolders = null;
                try {
                    ignoreFolders = factory.getConfigValue(FOLDER_BLACKLIST_PROPERTY, null);
                } catch (OXException e) {
                    LOG.error("", e);
                }
                if (null == ignoreFolders || 0 >= ignoreFolders.length()) {
                    this.folderBlacklist = new HashSet<String>(0);
                } else {
                    this.folderBlacklist = new HashSet<String>(Arrays.asList(Strings.splitByComma(ignoreFolders)));
                }
            }
            return this.folderBlacklist.contains(userizedFolder.getID());
        }

        /**
         * Gets the used folder tree identifier for folder operations.
         */
        private String getTreeID() {
            if (null == this.treeID) {
                try {
                    treeID = factory.getConfigValue(FOLDER_TRRE_ID_PROPERTY, FolderStorage.REAL_TREE_ID);
                } catch (OXException e) {
                    LOG.warn("falling back to tree id ''{}''.", FolderStorage.REAL_TREE_ID, e);
                    treeID = FolderStorage.REAL_TREE_ID;
                }
            }
            return this.treeID;
        }

        /**
         * Gets the maximum size allowed for contact vCards.
         *
         * @return The maximum size, or <code>0</code> if not restricted
         */
        public long getMaxVCardSize() {
            if (null == maxVCardSize) {
                Long defaultValue = Long.valueOf(4194304);
                try {
                    maxVCardSize = this.factory.optConfigValue("com.openexchange.contact.maxVCardSize", Long.class, defaultValue);
                } catch (OXException e) {
                    LOG.warn("error reading value for \"com.openexchange.contact.maxVCardSize\", falling back to {}.", defaultValue, e);
                    maxVCardSize = defaultValue;
                }
            }
            return maxVCardSize.longValue();
        }

        /**
         * Gets the maximum (overall) upload size per request.
         *
         * @return The maximum upload size, or <code>0</code> if not restricted
         */
        public long getMaxUploadSize() {
            if (null == maxUploadSize) {
                Long defaultValue = Long.valueOf(104857600);
                try {
                    maxUploadSize = factory.optConfigValue("MAX_UPLOAD_SIZE", Long.class, defaultValue);
                } catch (OXException e) {
                    LOG.warn("error reading value for \"MAX_UPLOAD_SIZE\", falling back to {}.", defaultValue, e);
                    maxUploadSize = defaultValue;
                }
            }
            return maxUploadSize.longValue();
        }

        /**
         * Gets the maximum number of contacts to fetch from the storage, based on the configuration value for
         * <code>com.openexchange.webdav.recursiveMarshallingLimit</code>.
         *
         * @return The contact limit, or <code>0</code> for no limitations
         */
        private int getContactLimit() {
            int limit = 25000;
            try {
                limit = Integer.valueOf(factory.getConfigValue("com.openexchange.webdav.recursiveMarshallingLimit", String.valueOf(limit)));
            } catch (OXException e) {
                LOG.warn("error getting \"com.openexchange.webdav.recursiveMarshallingLimit\", falling back to \"{}\".", limit, e);
            }
            return 0 >= limit ? 0 : 1 + limit;
        }

        /**
         * Gets a contact object containing the basic information.
         *
         * @param uid
         * @return
         * @throws AbstractOXException
         */
        public Contact get(String uid) throws OXException {
            Contact contact = this.getUidCache().get(uid);
            if (null == contact) {
                LOG.debug("Contact with UID '{}' not found, trying to get contact by filename...", uid);
                contact = getFilenameCache().get(uid);
            }
            if (null == contact) {
                LOG.debug("Contact with UID '{}' not found.", uid);
            }
            return contact;
        }

        /**
         * Loads a contact with the supplied fields.
         *
         * @param objectId the contact's object ID
         * @param inFolder the contact' parent folder ID
         * @return the contact
         * @throws OXException
         */
        private Contact load(int objectId, String inFolder, ContactField[] fields) throws OXException {
            Contact contact = null;
            if (null != fields) {
                contact = factory.getContactService().getContact(factory.getSession(), inFolder, Integer.toString(objectId), fields);
            } else {
                contact = factory.getContactService().getContact(factory.getSession(), inFolder, Integer.toString(objectId));
            }
            if (null == contact) {
                LOG.warn("Contact '{}' in folder '{}' not found.", objectId, inFolder);
            }
            return contact;
        }

        private Map<String, Contact> getUidCache() throws OXException {
            if (null == this.uidCache) {
                this.uidCache = generateUidCache();
            }
            return this.uidCache;
        }

        private Map<String, Contact> getFilenameCache() throws OXException {
            if (null == this.filenameCache) {
                this.filenameCache = generateFilenameCache();
            }
            return this.filenameCache;
        }

        private Map<String, Contact> generateUidCache() throws OXException {
            /*
             * prepare search term
             */
            List<UserizedFolder> folders = getFolders();
            if (0 == folders.size()) {
                return Collections.emptyMap();
            }
            CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND);
            if (1 == folders.size()) {
                SingleSearchTerm term = new SingleSearchTerm(SingleOperation.EQUALS);
                term.addOperand(new ContactFieldOperand(ContactField.FOLDER_ID));
                term.addOperand(new ConstantOperand<String>(folders.get(0).getID()));
                searchTerm.addSearchTerm(term);
            } else {
                CompositeSearchTerm orTerm = new CompositeSearchTerm(CompositeOperation.OR);
                for (UserizedFolder folder : getFolders()) {
                    SingleSearchTerm term = new SingleSearchTerm(SingleOperation.EQUALS);
                    term.addOperand(new ContactFieldOperand(ContactField.FOLDER_ID));
                    term.addOperand(new ConstantOperand<String>(folder.getID()));
                    orTerm.addSearchTerm(term);
                }
                searchTerm.addSearchTerm(orTerm);
            }
            CompositeSearchTerm noDistListTerm = new CompositeSearchTerm(CompositeOperation.OR);
            SingleSearchTerm term1 = new SingleSearchTerm(SingleOperation.EQUALS);
            term1.addOperand(new ContactFieldOperand(ContactField.NUMBER_OF_DISTRIBUTIONLIST));
            term1.addOperand(new ConstantOperand<Integer>(Integer.valueOf(0)));
            noDistListTerm.addSearchTerm(term1);
            SingleSearchTerm term2 = new SingleSearchTerm(SingleOperation.ISNULL);
            term2.addOperand(new ContactFieldOperand(ContactField.NUMBER_OF_DISTRIBUTIONLIST));
            noDistListTerm.addSearchTerm(term2);
            searchTerm.addSearchTerm(noDistListTerm);
            /*
             * get contacts
             */
            HashMap<String, Contact> cache = new HashMap<String, Contact>();
            SortOptions sortOptions = new SortOptions(ContactField.OBJECT_ID, Order.ASCENDING);
            sortOptions.setLimit(getContactLimit());
            SearchIterator<Contact> iterator = null;
            try {
                iterator = factory.getContactService().searchContacts(factory.getSession(), searchTerm, BASIC_FIELDS, sortOptions);
                while (iterator.hasNext()) {
                    Contact contact = iterator.next();
                    if (false == contact.containsUid()) {
                        LOG.warn("No UID found in contact '{}', skipping.", contact);
                        continue;
                    }
                    cache.put(contact.getUid(), contact);
                }
            } finally {
                SearchIterators.close(iterator);
            }
            return cache;
        }

        private Map<String, Contact> generateFilenameCache() throws OXException {
            HashMap<String, Contact> cache = new HashMap<String, Contact>();
            for (Contact contact : getUidCache().values()) {
                if (null != contact.getFilename()) {
                    cache.put(contact.getFilename(), contact);
                }
            }
            return cache;
        }

        private static void close(final SearchIterator<Contact> iterator) {
            SearchIterators.close(iterator);
        }
    }

}
