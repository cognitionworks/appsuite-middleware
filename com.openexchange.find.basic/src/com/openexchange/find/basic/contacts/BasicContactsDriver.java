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

package com.openexchange.find.basic.contacts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.contact.ContactFieldOperand;
import com.openexchange.contact.SortOptions;
import com.openexchange.exception.OXException;
import com.openexchange.find.AutocompleteRequest;
import com.openexchange.find.AutocompleteResult;
import com.openexchange.find.Document;
import com.openexchange.find.FindExceptionCode;
import com.openexchange.find.Module;
import com.openexchange.find.ModuleConfig;
import com.openexchange.find.SearchRequest;
import com.openexchange.find.SearchResult;
import com.openexchange.find.basic.AbstractContactFacetingModuleSearchDriver;
import com.openexchange.find.basic.Services;
import com.openexchange.find.common.ContactDisplayItem;
import com.openexchange.find.contacts.ContactsDocument;
import com.openexchange.find.contacts.ContactsFacetType;
import com.openexchange.find.facet.Facet;
import com.openexchange.find.facet.FacetValue;
import com.openexchange.find.facet.Filter;
import com.openexchange.find.facet.MandatoryFilter;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ConstantOperand;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link BasicContactsDriver}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class BasicContactsDriver extends AbstractContactFacetingModuleSearchDriver {

    /**
     * Defines the contact fields that are available in a {@link ContactsDocument}. Matches the fields typically fetched from the
     * storage when serving the "list" request.
     */
    private static final ContactField[] CONTACT_FIELDS = {
        ContactField.OBJECT_ID, ContactField.FOLDER_ID, ContactField.PRIVATE_FLAG, ContactField.DISPLAY_NAME, ContactField.GIVEN_NAME,
        ContactField.SUR_NAME, ContactField.TITLE, ContactField.POSITION, ContactField.INTERNAL_USERID, ContactField.EMAIL1,
        ContactField.EMAIL2, ContactField.EMAIL3, ContactField.COMPANY, ContactField.DISTRIBUTIONLIST,
        ContactField.MARK_AS_DISTRIBUTIONLIST, ContactField.NUMBER_OF_IMAGES, ContactField.LAST_MODIFIED, ContactField.YOMI_LAST_NAME,
        ContactField.SUR_NAME, ContactField.YOMI_FIRST_NAME, ContactField.GIVEN_NAME, ContactField.DISPLAY_NAME,
        ContactField.YOMI_COMPANY, ContactField.COMPANY, ContactField.EMAIL1, ContactField.EMAIL2, ContactField.USE_COUNT
    };


    private final Map<String, ContactSearchFacet> staticFacets;
    private final AddressbookFacet addressbookFacet;

    /**
     * Initializes a new {@link BasicContactsDriver}.
     */
    public BasicContactsDriver() {
        super();
        addressbookFacet = new AddressbookFacet();
        staticFacets = new HashMap<String, ContactSearchFacet>();
        AddressbookFacet addressbookFacet = new AddressbookFacet();
        staticFacets.put(addressbookFacet.getID(), addressbookFacet);
        AddressFacet addressFacet = new AddressFacet();
        staticFacets.put(addressFacet.getID(), addressFacet);
        ContactTypeFacet contactTypeFacet = new ContactTypeFacet();
        staticFacets.put(contactTypeFacet.getID(), contactTypeFacet);
        EmailFacet emailFacet = new EmailFacet();
        staticFacets.put(emailFacet.getID(), emailFacet);
        FolderTypeFacet folderTypeFacet = new FolderTypeFacet();
        staticFacets.put(folderTypeFacet.getID(), folderTypeFacet);
        NameFacet nameFacet = new NameFacet();
        staticFacets.put(nameFacet.getID(), nameFacet);
        PhoneFacet phoneFacet = new PhoneFacet();
        staticFacets.put(phoneFacet.getID(), phoneFacet);
    }

    @Override
    public Module getModule() {
        return Module.CONTACTS;
    }

    @Override
    public boolean isValidFor(ServerSession session) {
        return session.getUserConfiguration().hasContact();
    }

    @Override
    public ModuleConfig getConfiguration(ServerSession session) throws OXException {
        return new ModuleConfig(getModule(), new ArrayList<Facet>(staticFacets.values()), Collections.<MandatoryFilter>emptyList());
    }

    @Override
    public SearchResult search(SearchRequest searchRequest, ServerSession session) throws OXException {
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND);
        /*
         * build filters
         */
        for (Filter filter : searchRequest.getFilters()) {
            SearchTerm<?> term = getSearchTerm(session, filter);
            if (null != term) {
                searchTerm.addSearchTerm(term);
            } else {
                /*
                 * no search results if any filter indicates a FALSE condition
                 */
                return SearchResult.EMPTY;
            }
        }
        /*
         * combine with addressbook queries
         */
        for (String query : searchRequest.getQueries()) {
            SearchTerm<?> term = addressbookFacet.getSearchTerm(session, query);
            if (null != term) {
                searchTerm.addSearchTerm(term);
            }
        }
        /*
         * check for valid search term
         */
        if (0 == searchTerm.getOperands().length) {
            return SearchResult.EMPTY;
        }
        /*
         * search
         */
        List<Document> contactDocuments = new ArrayList<Document>();
        SortOptions sortOptions = new SortOptions(searchRequest.getStart(), searchRequest.getSize());
        SearchIterator<Contact> searchIterator = null;
        try {
            searchIterator = Services.getContactService().searchContacts(session, searchTerm, CONTACT_FIELDS, sortOptions);
            while (searchIterator.hasNext()) {
                contactDocuments.add(new ContactsDocument(searchIterator.next()));
            }
        } finally {
            SearchIterators.close(searchIterator);
        }
        return new SearchResult(-1, searchRequest.getStart(), contactDocuments);
    }

    @Override
    protected AutocompleteResult doAutocomplete(AutocompleteRequest autocompleteRequest, ServerSession session) throws OXException {
        /*
         * only offer ContactsFacetType.CONTACTS facet dynamically
         */
        List<Contact> contacts = autocompleteContacts(session, autocompleteRequest);
        List<FacetValue> contactValues = new ArrayList<FacetValue>(contacts.size());
        for (Contact contact : contacts) {
            String id = "contact";
            Filter filter = new Filter(Collections.singleton(id), String.valueOf(contact.getObjectID()));
            contactValues.add(new FacetValue(prepareFacetValueId(id, session.getContextId(),
                Integer.toString(contact.getObjectID())), new ContactDisplayItem(contact), 1, filter));
        }
        return new AutocompleteResult(Collections.singletonList(new Facet(ContactsFacetType.CONTACTS, contactValues)));
    }

    /**
     * Creates a search term for the query using a facet matching the supplied field.
     *
     * @param session The server session
     * @param field The filter field to select the matching facet
     * @param query The query
     * @return The search term, or <code>null</code> to indicate a <code>FALSE</code> condition with empty results.
     * @throws OXException
     */
    private SearchTerm<?> createSearchTerm(ServerSession session, String field, String query) throws OXException {
        /*
         * check static facets first
         */
        ContactSearchFacet contactFacet = staticFacets.get(field);
        if (null != contactFacet) {
            return contactFacet.getSearchTerm(session, query);
        }
        /*
         * check facets from autocomplete
         */
        if ("contact".equals(field)) {
            SingleSearchTerm searchTerm = new SingleSearchTerm(SingleOperation.EQUALS);
            searchTerm.addOperand(new ContactFieldOperand(ContactField.OBJECT_ID));
            searchTerm.addOperand(new ConstantOperand<Integer>(Integer.valueOf(query)));
            return searchTerm;
        }
        /*
         * unknown filter field
         */
        throw FindExceptionCode.UNSUPPORTED_FILTER_FIELD.create(field);
    }

    /**
     * Gets the search term for a filter definition.
     *
     * @param session The server session
     * @param filter The filter
     * @return The search term, or <code>null</code> to indicate a <code>FALSE</code> condition with empty results.
     * @throws OXException
     */
    private SearchTerm<?> getSearchTerm(ServerSession session, Filter filter) throws OXException {
        Set<String> fields = filter.getFields();
        Set<String> queries = filter.getQueries();
        if (1 == fields.size() && 1 == queries.size()) {
            return createSearchTerm(session, fields.iterator().next(), queries.iterator().next());
        }
        CompositeSearchTerm compositeTerm = new CompositeSearchTerm(CompositeOperation.OR);
        for (String field : fields) {
            for (String query : queries) {
                SearchTerm<?> searchTerm = createSearchTerm(session, field, query);
                if (null != searchTerm) {
                    compositeTerm.addSearchTerm(searchTerm);
                }
            }
        }
        return 0 == compositeTerm.getOperands().length ? null : compositeTerm;
    }

}
