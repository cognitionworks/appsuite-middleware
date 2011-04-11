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

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import com.openexchange.api2.OXException;
import com.openexchange.groupware.contact.helpers.DefaultContactComparator;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.search.ContactSearchObject;
import com.openexchange.groupware.search.Order;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link ContactSearchMultiplexer}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 *
 */
public class ContactSearchMultiplexer {

    private ContactInterfaceDiscoveryService discoveryService;

    public ContactSearchMultiplexer(ContactInterfaceDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    public SearchIterator<Contact> extendedSearch(ServerSession session, ContactSearchObject search, int orderBy, Order order, int[] cols) throws OXException {
        
        int[] folders = search.getFolders();
        int contextId = session.getContextId();
        List<SearchIterator<Contact>> searchIterators = new LinkedList<SearchIterator<Contact>>();
        if(null != folders && folders.length > 0) {
            List<Integer> foldersForDefaultSearch = new ArrayList<Integer>(folders.length);
            for (int folderId : folders) {
                if(discoveryService.hasSpecificContactInterface(folderId, contextId)) {
                    ContactInterface contactInterface = discoveryService.newContactInterface(folderId, session);
                    search.setFolders(folderId);
                    SearchIterator<Contact> iterator = contactInterface.getContactsByExtendedSearch(search, orderBy, order, cols);
                    searchIterators.add(iterator);
                } else {
                    foldersForDefaultSearch.add(I(folderId));
                }
            }
            if(!foldersForDefaultSearch.isEmpty()) {
                search.setFolders(foldersForDefaultSearch);
                ContactInterface defaultContactInterface = discoveryService.newDefaultContactInterface(session);
                SearchIterator<Contact> contactsByExtendedSearch = defaultContactInterface.getContactsByExtendedSearch(search, orderBy, order, cols);
                searchIterators.add(contactsByExtendedSearch);
            }
            
        } else {
            List<ContactInterfaceProviderRegistration> registrations = discoveryService.getRegistrations(contextId);
            for (ContactInterfaceProviderRegistration registration : registrations) {
                ContactInterface contactInterface = registration.newContactInterface(session);
                search.setFolders(registration.getFolderId());
                SearchIterator<Contact> searchIterator = contactInterface.getContactsByExtendedSearch(search, orderBy, order, cols);
                searchIterators.add(searchIterator);
            }
            search.clearFolders();
            ContactInterface defaultContactInterface = discoveryService.newDefaultContactInterface(session);
            SearchIterator<Contact> contactsByExtendedSearch = defaultContactInterface.getContactsByExtendedSearch(search, orderBy, order, cols);
            searchIterators.add(contactsByExtendedSearch);
        
        }
        if(searchIterators.size() == 1) {
            return searchIterators.get(0);
        }
        return new ContactMergerator(new DefaultContactComparator(orderBy, order), searchIterators);
    }
}
