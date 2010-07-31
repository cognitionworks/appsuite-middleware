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

package com.openexchange.groupware.contact;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import com.openexchange.api.OXConflictException;
import com.openexchange.api.OXObjectNotFoundException;
import com.openexchange.api2.OXConcurrentModificationException;
import com.openexchange.api2.OXException;
import com.openexchange.contact.LdapServer;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.search.ContactSearchObject;
import com.openexchange.tools.iterator.SearchIterator;

/**
 * {@link ContactInterface} - The contact interface.
 * 
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 */
public interface ContactInterface {

    /**
     * The property for the folder ID which is overridden.
     */
    public static final String OVERRIDE_FOLDER_ATTRIBUTE = "override_folder";

    /**
     * The property for the context ID which is overridden.
     */
    public static final String OVERRIDE_CONTEXT_ATTRIBUTE = "override_context";

    public void insertContactObject(final Contact co) throws OXException;

    public void updateContactObject(final Contact co, final int fid, final java.util.Date d) throws OXException, OXConcurrentModificationException, ContactException;

    void updateUserContact(Contact contact, Date lastmodified) throws OXException;

    /**
     * Determines the number of contacts a certain private or public folder.
     * 
     * @param folderId - The Folder ID
     * @param readCon - The Readable Connection To DB
     * @return Amount of contacts as an <code>int</code>
     * @throws OXException, OXPermissionException, OXFolderObjectNotFoundException
     */
    public int getNumberOfContacts(int folderId) throws OXException;

    /**
     * List contacts in a folder
     * 
     * @param folderId The Folder ID
     * @param from Start position in list
     * @param to End position in list
     * @param orderBy Column id to sort. 0 if no order by is used
     * @param orderDir Order direction (asc or desc) may be null if no special ordering is requested
     * @param cols The columns filled to the dataobject
     * @param readcon The readable Database Connection
     * @return A SearchIterator contains Task objects
     * @throws OXException, OXPermissionException, OXFolderObjectNotFoundException
     */
    public SearchIterator<Contact> getContactsInFolder(int folderId, int from, int to, int orderBy, String orderDir, int[] cols) throws OXException;

    /**
     * Lists all contacts that match the given search
     * 
     * @param searchObject The SearchObject
     * @param cols fields that will be added to the data object
     * @return A SearchIterator contains ContactObject
     * @throws OXException, OXPermissionException, OXFolderObjectNotFoundException
     */
    public SearchIterator<Contact> getContactsByExtendedSearch(ContactSearchObject searchobject, int orderBy, String orderDir, int[] cols) throws OXException;

    /**
     * Lists all contacts where the firstname, lastname or the displayname match the given searchpattern
     * 
     * @param searchpattern The searchpattern
     * @param folderId folder id where to search
     * @param cols fields that will be added to the data object
     * @return A SearchIterator contains ContactObject
     * @throws OXException, OXPermissionException, OXFolderObjectNotFoundException
     */
    SearchIterator<Contact> searchContacts(String searchpattern, int folderId, int orderBy, String orderDir, int[] cols) throws OXException;

    /**
     * Loads one contact by the given ID
     * 
     * @param objectId The Object ID
     * @return return the ContactObject
     * @throws OXException, OXPermissionException
     */
    public Contact getObjectById(int objectId, int inFolder) throws OXException;

    /**
     * Loads the contact of the given user id
     * 
     * @param userId The User ID
     * @return User's contact
     * @throws OXException If loading the user fails
     */
    public Contact getUserById(int userId) throws OXException;

    /**
     * Loads the contact of the given user id
     * 
     * @param userId The User ID
     * @param performReadCheck <code>true</code> to perform read check; otherwise <code>false</code>
     * @return User's contact
     * @throws OXException If loading the user fails
     */
    public Contact getUserById(int userId, boolean performReadCheck) throws OXException;

    /**
     * Lists all modified objects in a folder
     * 
     * @param folderID The Folder ID
     * @param since all modification >= since
     * @return A SearchIterator containing ContactObjects
     * @throws OXException, OXPermissionException, OXFolderObjectNotFoundException
     */
    public SearchIterator<Contact> getModifiedContactsInFolder(int folderId, int[] cols, Date since) throws OXException;

    /**
     * Lists all deleted objects in a folder
     * 
     * @param folderID The Folder ID
     * @param since all modification >= since
     * @return A SearchIterator containing ContactObjects
     * @throws OXException, OXPermissionException, OXFolderObjectNotFoundException
     */
    public SearchIterator<Contact> getDeletedContactsInFolder(int folderId, int[] cols, Date since) throws OXException;

    public void deleteContactObject(final int oid, final int fuid, final Date client_date) throws OXObjectNotFoundException, OXConflictException, OXException;

    /**
     * Loads a range of contacts by the given IDs
     * 
     * @param objectIdAndInFolder[] array with two dimensions. First dimension contains a seond array with two values. 1. value is object_id
     *            2. value if folder_id
     * @param cols The columns filled to the dataobject
     * @return A SearchIterator containing ContactObjects
     * @throws OXException
     */
    public SearchIterator<Contact> getObjectsById(int[][] objectIdAndInFolder, int cols[]) throws OXException;

    public Contact getContactByUUID(String uuid) throws OXException;
    
    public List<Contact> getAssociatedContacts(Contact contact) throws OXException;
    
    public void associateTwoContacts(Contact master, Contact slave) throws OXException;
    
    public void separateTwoContacts(Contact master, Contact slave) throws OXException;
    
    public ContactUnificationState getAssociationBetween(Contact c1, Contact c2) throws OXException;
    
    /**
     * Gets the folder ID.
     * 
     * @return The folder ID
     */
    public int getFolderId();

    /**
     * Gets the LDAP server.
     * 
     * @return The LDAP server
     */
    public LdapServer getLdapServer();

    /**
     * Sets the session instance.
     * 
     * @param s The session instance to set
     * @throws OXException If applying given session instance fails
     */
    //public void setSession(Session s) throws OXException;

}
