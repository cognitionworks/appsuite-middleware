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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.contact.internal;

import static com.openexchange.contact.internal.Tools.parse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import com.openexchange.contact.ContactFieldOperand;
import com.openexchange.contact.ContactService;
import com.openexchange.contact.SortOptions;
import com.openexchange.contact.internal.mapping.ContactMapper;
import com.openexchange.contact.storage.ContactStorage;
import com.openexchange.event.impl.EventClient;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.ContactExceptionCodes;
import com.openexchange.groupware.contact.ContactMergerator;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.search.ContactSearchObject;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ConstantOperand;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.impl.EffectivePermission;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.SearchIterator;

/**
 * {@link ContactServiceImpl} - {@link ContactService} implementation.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ContactServiceImpl extends DefaultContactService {
	
    public ContactServiceImpl() {
    	super();
    }
	
	@Override
    protected void doCreateContact(Session session, String folderID, Contact contact) throws OXException {
		int userID = session.getUserId();
		int contextID = session.getContextId();
		final ContactStorage storage = Tools.getStorage(session, folderID);
		/*
		 * check supplied contact
		 */
		Check.validateProperties(contact);		
		/*
		 * check general permissions
		 */
		final EffectivePermission permission = Tools.getPermission(contextID, folderID, userID);
		Check.canCreateObjects(permission, session, folderID);
		/*
		 * check folder
		 */
		final FolderObject folder = Tools.getFolder(contextID, folderID);
		Check.isContactFolder(folder, session);
		Check.noPrivateInPublic(folder, contact, session);
		Check.canWriteInGAB(storage, session, folderID, contact);
		/*
		 * prepare create
		 */
        final Date now = new Date();
		contact.setParentFolderID(parse(folderID));
        contact.setContextId(contextID);
        contact.setLastModified(now);
        contact.setCreationDate(now);
        contact.setCreatedBy(userID);
        contact.setModifiedBy(userID);
        contact.removeObjectID(); // set by storage during create
        contact.setNumberOfAttachments(0);
        contact.setUseCount(0);
        if (contact.containsImage1()) {
        	contact.setImageLastModified(now);
        	if (null != contact.getImage1()) {
            	contact.setNumberOfImages(1);
        	} else {
            	contact.setNumberOfImages(0);
            	contact.setImageContentType(null);        		
        	}
        }        
		if (false == contact.containsUid() || Tools.isEmpty(contact.getUid())) {
			contact.setUid(UUID.randomUUID().toString());
		}
        if (false == contact.containsFileAs() && contact.containsDisplayName()) {
            contact.setFileAs(contact.getDisplayName());
        }
		/*
		 * pass through to storage
		 */
		storage.create(session, folderID, contact);
		/*
		 * broadcast event
		 */
		new EventClient(session).create(contact, folder);
	}
	
	@Override
    protected void doUpdateAndMoveContact(Session session, String sourceFolderId, String targetFolderId, String objectID, 
			Contact contact, Date lastRead) throws OXException {
		int userID = session.getUserId();
		int contextID = session.getContextId();
		/*
		 * check supplied contact
		 */
		ContactMapper.getInstance().validateAll(contact);
		if (contact.containsObjectID() && false == Integer.toString(contact.getObjectID()).equals(objectID)) {
			throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(parse(objectID), contextID);
		}
		/*
		 * check source folder
		 */
		final FolderObject sourceFolder = Tools.getFolder(contextID, sourceFolderId);
		Check.isContactFolder(sourceFolder, session);
		final EffectivePermission sourceFolderPermission = Tools.getPermission(contextID, sourceFolderId, userID);
		Check.canDeleteOwn(sourceFolderPermission, session, sourceFolderId);
		/*
		 * check destination folder
		 */
		final FolderObject targetFolder = Tools.getFolder(contextID, targetFolderId);
		Check.isContactFolder(targetFolder, session);
		Check.noPrivateInPublic(targetFolder, contact, session);
		final EffectivePermission targetFolderPermission = Tools.getPermission(contextID, targetFolderId, userID);
		Check.canCreateObjects(targetFolderPermission, session, targetFolderId);
		/*
		 * check currently stored contact
		 */
		final ContactStorage sourceStorage = Tools.getStorage(session, sourceFolderId);
		final Contact storedContact = sourceStorage.get(session, sourceFolderId, objectID, ContactField.values());
        Check.contactNotNull(storedContact, contextID, Tools.parse(objectID));
		if (storedContact.getCreatedBy() != userID) {
			Check.canDeleteAll(sourceFolderPermission, session, sourceFolderId);
		}
		Check.lastModifiedBefore(storedContact, lastRead);
		Check.folderEquals(storedContact, sourceFolderId, contextID);
		/*
		 * check for not allowed changes
		 */
		final Contact delta = ContactMapper.getInstance().getDifferences(storedContact, contact);
		if (delta.containsContextId()) {
			throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(parse(objectID), contextID);
		} else if (delta.containsObjectID()) {
			throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(parse(objectID), contextID);
		} else if (delta.containsUid() && false == Tools.isEmpty(storedContact.getUid())) {
			throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(parse(objectID), contextID);
		} else if (delta.containsCreatedBy()) {
			throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(parse(objectID), contextID);
		} else if (delta.containsCreationDate()) {
			throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(parse(objectID), contextID);
		} else if (delta.containsPrivateFlag() && delta.getPrivateFlag() && storedContact.getModifiedBy() != userID) {
			throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(parse(objectID), contextID);
		}
		/*
		 * prepare update
		 */
        final Date now = new Date();
        delta.setLastModified(now);
        delta.setModifiedBy(userID);
        delta.setParentFolderID(parse(targetFolderId));
		if ((false == storedContact.containsUid() || Tools.isEmpty(storedContact.getUid())) && false == delta.containsUid()) {
			delta.setUid(UUID.randomUUID().toString());
		}
        if (delta.containsImage1()) {
        	delta.setImageLastModified(now);
        	if (null != delta.getImage1()) {
        		delta.setNumberOfImages(1);
        	} else {
        		delta.setNumberOfImages(0);
        		delta.setImageContentType(null);        		
        	}
        }
        Tools.invalidateAddressesIfNeeded(delta);
		/*
		 * pass through to storage
		 */
		final ContactStorage targetStorage = Tools.getStorage(session, targetFolderId);
		if (sourceStorage.equals(targetStorage)) {
			/*
			 * same storage, send update as delta
			 */
			sourceStorage.update(session, sourceFolderId, objectID, delta, lastRead);			
		} else {
			/*
			 * different storage, perform delete & create of complete contact information
			 */
			//TODO: move attachments
			ContactMapper.getInstance().mergeDifferences(storedContact, delta); 
			targetStorage.create(session, targetFolderId, storedContact);
			try {
				sourceStorage.delete(session, userID, sourceFolderId, objectID, lastRead);
			} catch (final OXException e) {
				LOG.warn("error deleting contact from source folder, rolling back move operation", e);
				// TODO: simple rollback for now
				targetStorage.delete(session, userID, targetFolderId, Integer.toString(storedContact.getObjectID()), 
						storedContact.getLastModified());
				throw e;
			}
		}
		/*
		 * broadcast event
		 */
		ContactMapper.getInstance().mergeDifferences(contact, delta);
		contact.setObjectID(storedContact.getObjectID());
		contact.setParentFolderID(storedContact.getParentFolderID());
		for (ContactStorage contactStorage : Tools.getStorages(session)) {
			contactStorage.updateReferences(session, contact);
		}
		new EventClient(session).modify(storedContact, contact, targetFolder);
	}
	
	@Override
    protected void doUpdateContact(Session session, String folderID, String objectID, Contact contact, Date lastRead) throws OXException {
		int userID = session.getUserId();
		int contextID = session.getContextId();
		ContactStorage storage = Tools.getStorage(session, folderID);
		/*
		 * check supplied contact
		 */
		Check.validateProperties(contact);
		if (contact.containsObjectID() && false == Integer.toString(contact.getObjectID()).equals(objectID)) {
			throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(parse(objectID), contextID);
		}
		/*
		 * check general permissions
		 */
		EffectivePermission permission = Tools.getPermission(contextID, folderID, userID);
		Check.canWriteOwn(permission, session, folderID);
		/*
		 * check currently stored contact
		 */
		Contact storedContact = storage.get(session, folderID, objectID, ContactField.values());
        Check.contactNotNull(storedContact, contextID, Tools.parse(objectID));
		if (storedContact.getCreatedBy() != userID) {
			Check.canWriteAll(permission, session, folderID);
		}
		Check.lastModifiedBefore(storedContact, lastRead);
		Check.folderEquals(storedContact, folderID, contextID);
		/*
		 * check folder
		 */
		FolderObject folder = Tools.getFolder(contextID, folderID);
		Check.isContactFolder(folder, session);
		Check.noPrivateInPublic(folder, contact, session);
		Check.canWriteInGAB(storage, session, folderID, contact);
		/*
		 * check for not allowed changes
		 */
		Contact delta = ContactMapper.getInstance().getDifferences(storedContact, contact);
		if (delta.containsContextId()) {
			throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(parse(objectID), contextID);
		} else if (delta.containsObjectID()) {
			throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(parse(objectID), contextID);
		} else if (delta.containsUid() && false == Tools.isEmpty(storedContact.getUid())) {
			throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(parse(objectID), contextID);
		} else if (delta.containsCreatedBy()) {
			throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(parse(objectID), contextID);
		} else if (delta.containsCreationDate()) {
			throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(parse(objectID), contextID);
		} else if (delta.containsParentFolderID()) {
			throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(parse(objectID), contextID);
		} else if (delta.containsPrivateFlag() && delta.getPrivateFlag() && storedContact.getModifiedBy() != userID) {
			throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(parse(objectID), contextID);
		}
		/*
		 * prepare update
		 */
        Date now = new Date();
        delta.setLastModified(now);
        delta.setModifiedBy(userID);
		if ((false == storedContact.containsUid() || Tools.isEmpty(storedContact.getUid())) && false == delta.containsUid()) {
			delta.setUid(UUID.randomUUID().toString());
		}
        if (delta.containsImage1()) {
        	delta.setImageLastModified(now);
        	if (null != delta.getImage1()) {
        		delta.setNumberOfImages(1);
        	} else {
        		delta.setNumberOfImages(0);
        		delta.setImageContentType(null);        		
        	}
        }
        Tools.invalidateAddressesIfNeeded(delta);
		/*
		 * pass through to storage
		 */
		storage.update(session, folderID, objectID, delta, lastRead);
		/*
		 * broadcast event
		 */
		ContactMapper.getInstance().mergeDifferences(contact, delta);
		contact.setObjectID(storedContact.getObjectID());
		contact.setParentFolderID(storedContact.getParentFolderID());
		for (ContactStorage contactStorage : Tools.getStorages(session)) {
			contactStorage.updateReferences(session, contact);
		}
		new EventClient(session).modify(storedContact, contact, folder);
	}
	
	@Override
    protected void doDeleteContact(Session session, String folderID, String objectID, Date lastRead) throws OXException {
		int userID = session.getUserId();
		int contextID = session.getContextId();
		final ContactStorage storage = Tools.getStorage(session, folderID);
		/*
		 * check folder
		 */
		final FolderObject folder = Tools.getFolder(contextID, folderID);
		Check.isContactFolder(folder, session);
		/*
		 * check general permissions
		 */
		final EffectivePermission permission = Tools.getPermission(contextID, folderID, userID);
		Check.canDeleteOwn(permission, session, folderID);
		/*
		 * check currently stored contact
		 */
		final Contact storedContact = storage.get(session, folderID, objectID, new ContactField[] { ContactField.CREATED_BY, 
				ContactField.LAST_MODIFIED });
		Check.contactNotNull(storedContact, contextID, Tools.parse(objectID));
		if (storedContact.getCreatedBy() != userID) {
			Check.canDeleteAll(permission, session, folderID);
		}
		Check.lastModifiedBefore(storedContact, lastRead);
		/*
		 * delete contact from storage
		 */
		storage.delete(session, userID, folderID, objectID, lastRead);
		/*
		 * broadcast event
		 */
		storedContact.setContextId(contextID);
		storedContact.setParentFolderID(parse(folderID));
		storedContact.setObjectID(parse(objectID));
		new EventClient(session).delete(storedContact, folder);
	}
	
	@Override
    protected <O> SearchIterator<Contact> doGetContacts(boolean deleted, Session session, final String folderID, 
			final String[] ids, final ContactField[] fields, final SortOptions sortOptions, 
			final Date since) throws OXException {
		int userID = session.getUserId();
		int contextID = session.getContextId();
		/*
		 * check folder
		 */
		final FolderObject folder = Tools.getFolder(contextID, folderID);
		Check.isContactFolder(folder, session);
		/*
		 * check general permissions
		 */
		final EffectivePermission permission = Tools.getPermission(contextID, folderID, userID);
		Check.canReadOwn(permission, session, folderID);
		/*
		 * prepare fields
		 */
		final QueryFields queryFields = new QueryFields(fields);		
		/*
		 * get contacts from storage
		 */		
		final ContactStorage storage = Tools.getStorage(session, folderID);
		SearchIterator<Contact> contacts = null;
		if (null != since) {
			contacts = deleted ? storage.deleted(session, folderID, since, queryFields.getFields(), sortOptions) : 
				storage.modified(session, folderID, since, queryFields.getFields(), sortOptions);
		} else if (null != ids) {
			contacts = storage.list(session, folderID, ids, queryFields.getFields(), sortOptions);
		} else {
			contacts = storage.all(session, folderID, queryFields.getFields(), sortOptions);
		} 
		if (null == contacts) {
			throw ContactExceptionCodes.UNEXPECTED_ERROR.create("got no results from storage");
		}
		/*
		 * filter results respecting object permission restrictions, adding attachment info as needed
		 */
		return new ResultIterator(contacts, queryFields.needsAttachmentInfo(), session, permission.canReadAllObjects());	
	}
	
	@Override
    protected <O> SearchIterator<Contact> doSearchContacts(Session session, SearchTerm<O> term, ContactField[] fields, 
			SortOptions sortOptions) throws OXException {
		int userID = session.getUserId();
		int contextID = session.getContextId();
		/*
		 * analyze term
		 */
		final SearchTermAnalyzer termAnanlyzer = new SearchTermAnalyzer(term);
		/*
		 * determine queried storages according to searched folders
		 */
		final Map<ContactStorage, List<String>> queriedStorages = Tools.getStorages(session, 
				termAnanlyzer.hasFolderIDs() ? termAnanlyzer.getFolderIDs() : Tools.getSearchFolders(contextID, userID));
		if (null == queriedStorages || 0 == queriedStorages.size()) {
			throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create("No contact storage found for queried folder IDs");
		}
		/*
		 * prepare fields
		 */
		final QueryFields queryFields = new QueryFields(fields);
		/*
		 * perform searches
		 */
		final List<SearchIterator<Contact>> searchIterators = new ArrayList<SearchIterator<Contact>>();		
		for (final Entry<ContactStorage, List<String>> queriedStorage : queriedStorages.entrySet()) {
			final SearchTerm<?> searchTerm;			
			if (termAnanlyzer.hasFolderIDs()) {
				/*
				 * leave term as is
				 */
				searchTerm = term;
			} else {
				/*
				 * combine term with extracted folder information for that storage
				 */
				final CompositeSearchTerm combinedTerm = new CompositeSearchTerm(CompositeOperation.AND);
				combinedTerm.addSearchTerm(Tools.getFoldersTerm(queriedStorage.getValue()));
				combinedTerm.addSearchTerm(term);
				searchTerm = combinedTerm;
			}			
			/*
			 * get results, filtered respecting object permission restrictions, adding attachment info as needed
			 */
			final SearchIterator<Contact> searchIterator = queriedStorage.getKey().search(
			    session, searchTerm, queryFields.getFields(), sortOptions);
			searchIterators.add(new ResultIterator(searchIterator, queryFields.needsAttachmentInfo(), session));
		}
		return 2 > searchIterators.size() ? searchIterators.get(0) : 
			new ContactMergerator(Tools.getComparator(sortOptions), searchIterators);				
	}
	
	@Override
    protected SearchIterator<Contact> doSearchContacts(Session session, ContactSearchObject contactSearch, ContactField[] fields, 
			SortOptions sortOptions) throws OXException {
		int userID = session.getUserId();
		int contextID = session.getContextId();
		/*
		 * check supplied search
		 */
		Check.validateSearch(contactSearch);
		/*
		 * determine queried storages according to searched folders
		 */
		Map<ContactStorage, List<String>> queriedStorages = Tools.getStorages(session,
				contactSearch.hasFolders() ? Tools.toStringList(contactSearch.getFolders()) : Tools.getSearchFolders(contextID, userID));
		if (null == queriedStorages || 0 == queriedStorages.size()) {
			throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create("No contact storage found for queried folder IDs");
		}
		/*
		 * prepare fields
		 */
		QueryFields queryFields = new QueryFields(fields);
		/*
		 * perform searches
		 */
		List<SearchIterator<Contact>> searchIterators = new ArrayList<SearchIterator<Contact>>();		
		for (Entry<ContactStorage, List<String>> queriedStorage : queriedStorages.entrySet()) {
			/*
			 * apply folders specific to this storage to contact search
			 */
			contactSearch.setFolders(Tools.parse(queriedStorage.getValue()));
			/*
			 * get results, filtered respecting object permission restrictions, adding attachment info as needed
			 */
			SearchIterator<Contact> searchIterator = queriedStorage.getKey().search(
			    session, contactSearch, queryFields.getFields(), sortOptions);
			searchIterators.add(new ResultIterator(searchIterator, queryFields.needsAttachmentInfo(), session));
		}
		/*
		 * deliver sorted results
		 */
		return 2 > searchIterators.size() ? searchIterators.get(0) : 
			new ContactMergerator(Tools.getComparator(sortOptions), searchIterators);				
	}
	
	@Override
    protected String doGetOrganization(Session session) throws OXException {
		final String folderID = Integer.toString(FolderObject.SYSTEM_LDAP_FOLDER_ID);
		final int userID = Tools.getContext(session).getMailadmin();
		final ContactStorage storage = Tools.getStorage(session, folderID);
		final Contact contact = storage.get(session, folderID, Integer.toString(userID), new ContactField[] { ContactField.COMPANY } );
		Check.contactNotNull(contact, session.getContextId(), userID);
		return contact.getCompany();
	}
	
	@Override
    protected <O> SearchIterator<Contact> doGetUsers(Session session, int[] userIDs, SearchTerm<O> term,
			ContactField[] fields, SortOptions sortOptions) throws OXException {
		int currentUserID = session.getUserId();
		int contextID = session.getContextId();
		final String folderID = Integer.toString(FolderObject.SYSTEM_LDAP_FOLDER_ID);
		final ContactStorage storage = Tools.getStorage(session, folderID);
		/*
		 * limit queried fields when necessary due to permissions
		 */
		final EffectivePermission permission = Tools.getPermission(contextID, folderID, currentUserID);
		final QueryFields queryFields;		
		if (permission.canReadAllObjects() || permission.canReadOwnObjects() && 1 == userIDs.length && currentUserID == userIDs[0]) {
			// no limitation
			queryFields = new QueryFields(fields);		
		} else {
			// restrict queried fields
			queryFields = new QueryFields(fields, LIMITED_USER_FIELDS);
		}
		/*
		 * prepare search term for users
		 */
		final SearchTerm<?> searchTerm;
		final SingleSearchTerm folderIDTerm = new SingleSearchTerm(SingleOperation.EQUALS);
		folderIDTerm.addOperand(new ContactFieldOperand(ContactField.FOLDER_ID));
		folderIDTerm.addOperand(new ConstantOperand<String>(folderID));
		if (null != userIDs && 0 < userIDs.length) {
			final SearchTerm<?> userIDsTerm;
			if (1 == userIDs.length) {
				final SingleSearchTerm userIDTerm = new SingleSearchTerm(SingleOperation.EQUALS);
				userIDTerm.addOperand(new ContactFieldOperand(ContactField.INTERNAL_USERID));
				userIDTerm.addOperand(new ConstantOperand<Integer>(userIDs[0]));
				userIDsTerm = userIDTerm;
			} else {
				final CompositeSearchTerm orTerm = new CompositeSearchTerm(CompositeOperation.OR);
				for (final int userID : userIDs) {
					final SingleSearchTerm userIDTerm = new SingleSearchTerm(SingleOperation.EQUALS);
					userIDTerm.addOperand(new ContactFieldOperand(ContactField.INTERNAL_USERID));
					userIDTerm.addOperand(new ConstantOperand<Integer>(userID));
					orTerm.addSearchTerm(userIDTerm);
				}
				userIDsTerm = orTerm;
			}
			final CompositeSearchTerm compositeTerm = new CompositeSearchTerm(CompositeOperation.AND);
			compositeTerm.addSearchTerm(folderIDTerm);
			compositeTerm.addSearchTerm(userIDsTerm);
			searchTerm = compositeTerm;
		} else if (null != term) {
			final CompositeSearchTerm compositeTerm = new CompositeSearchTerm(CompositeOperation.AND);
			compositeTerm.addSearchTerm(folderIDTerm);
			compositeTerm.addSearchTerm(term);
			searchTerm = compositeTerm;
		} else {
			searchTerm = folderIDTerm;
		}
		/*
		 * get user contacts from storage
		 */
		return new ResultIterator(storage.search(session, searchTerm, queryFields.getFields(), sortOptions), 
				queryFields.needsAttachmentInfo(), session, true);
	}

	@Override
    protected SearchIterator<Contact> doGetUsers(Session session, int[] userIDs, ContactSearchObject contactSearch,
			ContactField[] fields, SortOptions sortOptions) throws OXException {
		int currentUserID = session.getUserId();
		int contextID = session.getContextId();
		String folderID = Integer.toString(FolderObject.SYSTEM_LDAP_FOLDER_ID);
		ContactStorage storage = Tools.getStorage(session, folderID);
		/*
		 * limit queried fields when necessary due to permissions
		 */
		EffectivePermission permission = Tools.getPermission(contextID, folderID, currentUserID);
		QueryFields queryFields;		
		if (permission.canReadAllObjects() || permission.canReadOwnObjects() && 1 == userIDs.length && currentUserID == userIDs[0]) {
			// no limitation
			queryFields = new QueryFields(fields);		
		} else {
			// restrict queried fields
			queryFields = new QueryFields(fields, LIMITED_USER_FIELDS);
		}
		/*
		 * get user contacts from storage
		 */
		return new ResultIterator(storage.search(session, contactSearch, queryFields.getFields(), sortOptions), 
				queryFields.needsAttachmentInfo(), session, true);
	}

}
