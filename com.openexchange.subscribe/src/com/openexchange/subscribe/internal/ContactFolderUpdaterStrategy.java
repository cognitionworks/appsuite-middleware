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

package com.openexchange.subscribe.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.openexchange.api2.RdbContactSQLImpl;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.subscribe.Subscription;
import com.openexchange.subscribe.SubscriptionSession;
import com.openexchange.tools.iterator.SearchIterator;

/**
 * {@link ContactFolderUpdaterStrategy}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="karsten.will@open-xchange.com">Karsten Will</a>
 */
public class ContactFolderUpdaterStrategy implements FolderUpdaterStrategy<Contact> {

    private static final int SQL_INTERFACE = 1;

    private static final int SUBSCRIPTION = 2;

    private static final int[] COMPARISON_COLUMNS = {
        Contact.OBJECT_ID, Contact.FOLDER_ID, Contact.GIVEN_NAME, Contact.SUR_NAME, Contact.BIRTHDAY, Contact.DISPLAY_NAME, Contact.EMAIL1,
        Contact.EMAIL2, Contact.EMAIL3, Contact.USERFIELD20 };

    public int calculateSimilarityScore(Contact original, Contact candidate, Object session) throws AbstractOXException {
        int score = 0;
        int threshhold = getThreshhold(session);
        
        // For the sake of simplicity we assume that equal names mean equal contacts
        // TODO: This needs to be diversified in the form of "unique-in-context" later (if there is only one "Max Mustermann" in a folder it
        // is unique and qualifies as identifier. If there are two "Max Mustermann" it does not.)
        if ((isset(original.getGivenName()) || isset(candidate.getGivenName())) && eq(original.getGivenName(), candidate.getGivenName())) {
            score += 5;
        }
        if ((isset(original.getSurName()) || isset(candidate.getSurName())) && eq(original.getSurName(), candidate.getSurName())) {
            score += 5;
        }
        if ((isset(original.getDisplayName()) || isset(candidate.getDisplayName())) && eq(original.getDisplayName(), candidate.getDisplayName())) {
            score += 10;
        }
        // an email-address is unique so if this is identical the contact should be the same
        if (eq(original.getEmail1(), candidate.getEmail1())) {
            score += 10;
        }
        if (eq(original.getEmail2(), candidate.getEmail2())) {
            score += 10;
        }
        if (eq(original.getEmail3(), candidate.getEmail3())) {
            score += 10;
        }
        if (original.containsBirthday() && candidate.containsBirthday() && eq(original.getBirthday(), candidate.getBirthday())) {
            score += 5;
        }
        
        if( score < threshhold && original.equalsContentwise(candidate)) { //the score check is only to speed the process up
            score = threshhold + 1;
        }
        return score;
    }

    /**
     * @param original
     * @param candidate
     * @return
     */
    private boolean hasEqualContent(Contact original, Contact candidate) {
        for(int fieldNumber: Contact.ALL_COLUMNS){
            if(original.get(fieldNumber) == null){
                if(candidate.get(fieldNumber) != null){
                    return false;
                }
            } else {
                if(candidate.get(fieldNumber) != null){
                    if(! original.get(fieldNumber).equals(candidate.get(fieldNumber)))
                        return false;
                }
            }
        }
        return false;
    }

    private boolean isset(String s) {
        return s == null || s.length() > 0;
    }

    protected boolean eq(Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            return false;
        } else {
            return o1.equals(o2);
        }
    }

    public void closeSession(Object session) throws AbstractOXException {

    }

    public Collection<Contact> getData(Subscription subscription, Object session) throws AbstractOXException {
        RdbContactSQLImpl contacts = (RdbContactSQLImpl) getFromSession(SQL_INTERFACE, session);

        int folderId = subscription.getFolderIdAsInt();
        int numberOfContacts = contacts.getNumberOfContacts(folderId);
        SearchIterator<Contact> contactsInFolder = contacts.getContactsInFolder(
            folderId,
            0,
            numberOfContacts,
            Contact.OBJECT_ID,
            "ASC",
            COMPARISON_COLUMNS);
        List<Contact> retval = new ArrayList<Contact>();
        while (contactsInFolder.hasNext()) {
            retval.add(contactsInFolder.next());
        }
        return retval;
    }

    public int getThreshhold(Object session) throws AbstractOXException {
        return 9;
    }

    public boolean handles(FolderObject folder) {
        return folder.getModule() == FolderObject.CONTACT;
    }

    public void save(Contact newElement, Object session) throws AbstractOXException {
        RdbContactSQLImpl contacts = (RdbContactSQLImpl) getFromSession(SQL_INTERFACE, session);
        Subscription subscription = (Subscription) getFromSession(SUBSCRIPTION, session);
        newElement.setParentFolderID(subscription.getFolderIdAsInt());

        // as this is a new contact it needs a UUID to make later aggregation possible. This has to be a new one.
        newElement.setUserField20(UUID.randomUUID().toString());
        
        contacts.insertContactObject(newElement);
    }

    private Object getFromSession(int key, Object session) {
        return ((Map<Integer, Object>) session).get(key);
    }

    public Object startSession(Subscription subscription) throws AbstractOXException {
        Map<Integer, Object> userInfo = new HashMap<Integer, Object>();
        userInfo.put(SQL_INTERFACE, new RdbContactSQLImpl(new SubscriptionSession(subscription)));
        userInfo.put(SUBSCRIPTION, subscription);
        return userInfo;
    }

    public void update(Contact original, Contact update, Object session) throws AbstractOXException {
        RdbContactSQLImpl contacts = (RdbContactSQLImpl) getFromSession(SQL_INTERFACE, session);

        update.setParentFolderID(original.getParentFolderID());
        update.setObjectID(original.getObjectID());
        update.setLastModified(original.getLastModified());
        // We need to carry over the UUID to keep existing relations
        update.setUserField20(original.getUserField20());

        contacts.updateContactObject(update, update.getParentFolderID(), update.getLastModified());
    }

}
