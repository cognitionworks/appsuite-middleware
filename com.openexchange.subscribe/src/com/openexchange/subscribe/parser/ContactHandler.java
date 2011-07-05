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

package com.openexchange.subscribe.parser;

import java.util.Collection;
import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.api2.RdbContactSQLImpl;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.contexts.impl.OXException;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorException;


/**
 * {@link ContactHandler}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 *
 */
public class ContactHandler {
 
    private static final Log LOG = LogFactory.getLog(ContactHandler.class);

    /**
     * Update or insert contacts from a subscription
     * @param subscription
     * @throws OXException
     * @throws OXException
     */
    protected void storeContacts(Session session, int folderId, Collection<Contact> updatedContacts) throws AbstractOXException{
       
        
        RdbContactSQLImpl storage = new RdbContactSQLImpl(session);
        
        for(Contact updatedContact: updatedContacts){
            SearchIterator<Contact> existingContacts = storage.getContactsInFolder(folderId, 0, 0, 0, null, null, Contact.ALL_COLUMNS);
            boolean foundMatch = false;
            while( existingContacts.hasNext() && ! foundMatch ){
                Contact existingContact = null;
                try {
                    existingContact = existingContacts.next();
                } catch (SearchIteratorException e) {
                    e.printStackTrace();
                }
                if( existingContact == null)
                    continue;
                if( isSame(existingContact, updatedContact)){
                    foundMatch = true;
                    updatedContact.setObjectID( existingContact.getObjectID() );
                    storage.updateContactObject(updatedContact,folderId, new Date() );
                }
            }
            if(foundMatch)
                continue;
            updatedContact.setParentFolderID( folderId );
            try {
                storage.insertContactObject(updatedContact);
            } catch (AbstractOXException x) {
                LOG.error(x.getMessage(), x);
            }
        }
    }
    

    protected boolean isSame(Contact first, Contact second){
        if(first.containsGivenName()) {
            if(!first.getGivenName().equals(second.getGivenName())) {
                return false;
            }
        }
        
        if(first.containsSurName()) {
            if(!first.getSurName().equals(second.getSurName())) {
                return false;
            }
        }
        
        if(first.containsDisplayName()) {
            if(!first.getDisplayName().equals(second.getDisplayName())) {
                return false;
            }
        }
        return true;
    }
    
}
