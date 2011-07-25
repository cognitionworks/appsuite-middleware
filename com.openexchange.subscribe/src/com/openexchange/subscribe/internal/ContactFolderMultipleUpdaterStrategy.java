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

package com.openexchange.subscribe.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.api2.ContactSQLInterface;
import com.openexchange.api2.FinalContactInterface;
import com.openexchange.api2.RdbContactSQLImpl;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.ContactInterface;
import com.openexchange.groupware.contact.ContactUnificationState;
import com.openexchange.groupware.contact.OverridingContactInterface;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.generic.TargetFolderDefinition;
import com.openexchange.groupware.search.Order;
import com.openexchange.subscribe.TargetFolderSession;
import com.openexchange.tools.iterator.SearchIterator;


/**
 * {@link ContactFolderMultipleUpdaterStrategy}
 * This differs from ContactFolderUpdaterStrategy in 2 ways
 * - individual fields are only written if present in the update and not filled yet. So no fields will be deleted and none will be overwritten.
 * - aggregating relations between contacts (see {@link ContactInterface}) are respected as well as generated if appropriate 
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 */
public class ContactFolderMultipleUpdaterStrategy implements FolderUpdaterStrategy<Contact> {
    private static final int SQL_INTERFACE = 1;

    private static final int TARGET = 2;
    
    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(ContactFolderMultipleUpdaterStrategy.class));

    // All columns need to be loaded here as we keep the original, not the update and no data may be lost
    private static final int[] COMPARISON_COLUMNS = Contact.CONTENT_COLUMNS;

    public int calculateSimilarityScore(final Contact original, final Contact candidate, final Object session) throws OXException {
        int score = 0;
        final int threshhold = getThreshold(session);
        boolean contactsAreAbleToBeAssociated = false;
        final FinalContactInterface contactStore = (FinalContactInterface) getFromSession(SQL_INTERFACE, session);
        
        if (false && candidate.getUserField20() != null && !candidate.getUserField20().equals("")){
            
            final UUID uuid = UUID.fromString(candidate.getUserField20());
                                    
            try{
                final Contact contactfromUUID = contactStore.getContactByUUID(uuid);
                if (contactfromUUID != null){
                    contactsAreAbleToBeAssociated = true;
                    final ContactUnificationState state = contactStore.getAssociationBetween(original, candidate);
                    // if the two contacts are associated RED already a big negative score needs to be given
                    if (state.equals(ContactUnificationState.RED)){
                        score = -1000;
                    }
                    // if the contacts are associated GREEN already a big positive score needs to be given
                    if (state.equals(ContactUnificationState.GREEN)){
                        score = 1000;
                    }
                }
            } catch (final OXException e) {                
                LOG.error(e);                            
            }
        }        
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
            score += threshhold + 1;
        }
        //before returning the score the contacts need to be associated GREEN here if the score is high enough, they are not associated already, and they are both on the system
        try {
            if (score >= threshhold && contactsAreAbleToBeAssociated){
                final List<UUID> idsOfAlreadyAssociatedContacts = contactStore.getAssociatedContacts(original);
                //List<Contact> associatedContacts = 
                boolean alreadyAssociated = false;
                for (final UUID uuid : idsOfAlreadyAssociatedContacts){
                    final Contact contact = contactStore.getContactByUUID(uuid);
                    if (contact.equals(candidate)){
                        alreadyAssociated = true;
                    }
                }
                if (!alreadyAssociated){
                    contactStore.associateTwoContacts(original, candidate);                
                }
            }
        } catch (final OXException e) {
            LOG.error(e);
        } 
        return score;
    }

    /**
     * @param original
     * @param candidate
     * @return
     */
    private boolean hasEqualContent(final Contact original, final Contact candidate) {
        for(final int fieldNumber: Contact.ALL_COLUMNS){
            if(original.get(fieldNumber) == null){
                if(candidate.get(fieldNumber) != null){
                    return false;
                }
            } else {
                if(candidate.get(fieldNumber) != null){
                    if(! original.get(fieldNumber).equals(candidate.get(fieldNumber))) {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private boolean isset(final String s) {
        return s == null || s.length() > 0;
    }

    protected boolean eq(final Object o1, final Object o2) {
        if (o1 == null || o2 == null) {
            return false;
        } else {
            return o1.equals(o2);
        }
    }

    public void closeSession(final Object session) throws OXException {

    }

    public Collection<Contact> getData(final TargetFolderDefinition target, final Object session) throws OXException {
        final RdbContactSQLImpl contacts = (RdbContactSQLImpl) getFromSession(SQL_INTERFACE, session);

        final int folderId = target.getFolderIdAsInt();
        final int numberOfContacts = contacts.getNumberOfContacts(folderId);
        final SearchIterator<Contact> contactsInFolder = contacts.getContactsInFolder(
            folderId,
            0,
            numberOfContacts,
            Contact.OBJECT_ID,
            Order.ASCENDING,
            null,
            COMPARISON_COLUMNS);
        final List<Contact> retval = new ArrayList<Contact>();
        while (contactsInFolder.hasNext()) {
            retval.add(contactsInFolder.next());
        }
        return retval;
    }

    public int getThreshold(final Object session) throws OXException {
        return 9;
    }

    public boolean handles(final FolderObject folder) {
        return folder.getModule() == FolderObject.CONTACT;
    }

    public void save(final Contact newElement, final Object session) throws OXException {
        final OverridingContactInterface contacts = (OverridingContactInterface) getFromSession(SQL_INTERFACE, session);
        final TargetFolderDefinition target = (TargetFolderDefinition) getFromSession(TARGET, session);
        newElement.setParentFolderID(target.getFolderIdAsInt());
        
        // as this is a new contact it needs a UUID to make later aggregation possible. This has to be a new one.
        newElement.setUserField20(UUID.randomUUID().toString());

        contacts.forceInsertContactObject(newElement);
    }

    private Object getFromSession(final int key, final Object session) {
        return ((Map<Integer, Object>) session).get(key);
    }

    public Object startSession(final TargetFolderDefinition target) throws OXException {
        final Map<Integer, Object> userInfo = new HashMap<Integer, Object>();
        userInfo.put(SQL_INTERFACE, new RdbContactSQLImpl(new TargetFolderSession(target)));
        userInfo.put(TARGET, target);
        return userInfo;
    }

    public void update(final Contact original, final Contact update, final Object session) throws OXException {
        //This may only fill up fields NEVER overwrite them. Original should be used as base and filled up as needed
        //ALL Content Columns need to be considered here
        final ContactSQLInterface contactStore = (ContactSQLInterface) getFromSession(SQL_INTERFACE, session);
        final int[] columns = Contact.CONTENT_COLUMNS;
        for (final int field : columns){
            if (original.get(field) == null){
                if (update.get(field) != null){
                    original.set(field, update.get(field));
                }
            }
        }
        contactStore.updateContactObject(original, original.getParentFolderID(), original.getLastModified());
    }
}
