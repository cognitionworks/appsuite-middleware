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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.carddav.resources;

import static com.openexchange.dav.DAVProtocol.protocolException;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.jdom2.Element;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.carddav.CardDAVProperty;
import com.openexchange.carddav.GroupwareCarddavFactory;
import com.openexchange.carddav.Tools;
import com.openexchange.carddav.photos.PhotoUtils;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.contact.ContactFieldOperand;
import com.openexchange.contact.ContactService;
import com.openexchange.contact.storage.ContactTombstoneStorage;
import com.openexchange.contact.vcard.DistributionListMode;
import com.openexchange.contact.vcard.VCardExport;
import com.openexchange.contact.vcard.VCardImport;
import com.openexchange.contact.vcard.VCardParameters;
import com.openexchange.contact.vcard.VCardService;
import com.openexchange.contact.vcard.storage.VCardStorageService;
import com.openexchange.dav.DAVProtocol;
import com.openexchange.dav.DAVUserAgent;
import com.openexchange.dav.PreconditionException;
import com.openexchange.dav.resources.CommonResource;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;
import com.openexchange.groupware.tools.mappings.MappedIncorrectString;
import com.openexchange.groupware.tools.mappings.MappedTruncation;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ConstantOperand;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.webdav.WebDAVRequestContext;
import com.openexchange.webdav.protocol.WebdavPath;
import com.openexchange.webdav.protocol.WebdavProperty;
import com.openexchange.webdav.protocol.WebdavProtocolException;

/**
 * {@link ContactResource}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ContactResource extends CommonResource<Contact> {

    /**
     * The file extension used for vCard resources.
     */
    public static final String EXTENSION_VCF = ".vcf";

    /**
     * The content type used for vCard resources.
     */
    public static final String CONTENT_TYPE = "text/vcard; charset=utf-8";

    private static final int MAX_RETRIES = 3;

    /** The contact fields that are considered when resolving distribution list references */
    private static final ContactField CONTACT_FIELDS_TO_LOAD[] = { ContactField.OBJECT_ID, ContactField.FOLDER_ID, ContactField.UID, ContactField.DISPLAY_NAME, ContactField.EMAIL1, ContactField.EMAIL2, ContactField.EMAIL3, ContactField.MARK_AS_DISTRIBUTIONLIST, ContactField.DISTRIBUTIONLIST };

    private final GroupwareCarddavFactory factory;
    private final CardDAVCollection parent;
    private VCardImport vCardImport;
    private VCardExport vCardExport;

    /**
     * Initializes a new {@link ContactResource}.
     *
     * @param factory The CardDAV factory
     * @param parent The parent folder collection
     * @param object An existing groupware object represented by this resource, or <code>null</code> if a placeholder resource should be created
     * @param url The resource url
     */
    public ContactResource(GroupwareCarddavFactory factory, CardDAVCollection parent, Contact object, WebdavPath url) {
        super(parent, object, url);
        this.factory = factory;
        this.parent = parent;
    }

    /**
     * Creates a new contact resource from a vCard import.
     *
     * @param factory The CardDAV factory
     * @param parent The parent folder collection
     * @param url The target resource URL
     * @param vCardImport The vCard import to apply
     * @return The new contact resource
     */
    static ContactResource fromImport(GroupwareCarddavFactory factory, CardDAVCollection parent, WebdavPath url, VCardImport vCardImport) {
        ContactResource contactResource = new ContactResource(factory, parent, null, url);
        contactResource.vCardImport = vCardImport;
        return contactResource;
    }

    @Override
    protected String getFileExtension() {
        return EXTENSION_VCF;
    }

    @Override
    public String getContentType() throws WebdavProtocolException {
        return CONTENT_TYPE;
    }

    @Override
    public Long getLength() throws WebdavProtocolException {
        if (exists()) {
            VCardExport vCardResource = getVCardResource(false);
            if (null != vCardResource && null != vCardResource.getVCard()) {
                return Long.valueOf(vCardResource.getVCard().getLength());
            }
        }
        return L(0L);
    }

    @Override
    public InputStream getBody() throws WebdavProtocolException {
        if (exists()) {
            VCardExport vCardResource = getVCardResource(true);
            if (null != vCardResource) {
                try {
                    return vCardResource.getClosingStream();
                } catch (OXException e) {
                    throw protocolException(getUrl(), e);
                }
            }
        }
        throw protocolException(getUrl(), HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    public void save() throws WebdavProtocolException {
        IFileHolder vCardFileHolder = null;
        String vCardID = null;
        String previousVCardID = null;
        boolean saved = false;
        Session session = factory.getSession();
        try {
            if (!exists()) {
                throw protocolException(getUrl(), HttpServletResponse.SC_CONFLICT);
            } else if (null == vCardImport || null == vCardImport.getContact()) {
                throw protocolException(getUrl(), HttpServletResponse.SC_NOT_FOUND);
            }
            /*
             * store original vCard if possible
             */
            previousVCardID = object.getVCardId();
            Contact contact = vCardImport.getContact();
            vCardFileHolder = vCardImport.getVCard();
            vCardID = storeVCard(session.getContextId(), vCardFileHolder);
            contact.setVCardId(vCardID);
            /*
             * update contact, trying again in case of recoverable errors
             */
            ContactService contactService = factory.requireService(ContactService.class);
            restoreDistributionListReferences(session, contactService, contact, Integer.toString(contact.getParentFolderID()));
            for (int i = 0; i < MAX_RETRIES && false == saved; i++) {
                try {
                    contactService.updateContact(session, Integer.toString(contact.getParentFolderID()), Integer.toString(contact.getObjectID()), contact, contact.getLastModified());
                    LOG.debug("{}: saved.", getUrl());
                    saved = true;
                } catch (OXException e) {
                    if (false == handle(e)) {
                        break;
                    }
                }
            }
            /*
             * process attachments
             */
            handleAttachments(object, contact);
        } catch (OXException e) {
            throw protocolException(getUrl(), e);
        } finally {
            Streams.close(vCardFileHolder);
            closeVCardImport();
            if (saved) {
                deleteVCard(session.getContextId(), previousVCardID);
            } else if (null != vCardID) {
                deleteVCard(session.getContextId(), vCardID);
            }
        }
    }

    @Override
    public void delete() throws WebdavProtocolException {
        boolean deleted = false;
        Contact object = this.object;
        String vCardID = null == object ? null : object.getVCardId();
        Session session = factory.getSession();
        try {
            if (false == exists()) {
                throw protocolException(getUrl(), HttpServletResponse.SC_NOT_FOUND);
            }
            /*
             * delete contact, trying again in case of recoverable errors
             */
            if (null != object) {
                for (int i = 0; i < MAX_RETRIES && false == deleted; i++) {
                    try {
                        ContactService contactService = factory.requireService(ContactService.class);
                        contactService.deleteContact(session, Integer.toString(object.getParentFolderID()), Integer.toString(object.getObjectID()), object.getLastModified());
                        LOG.debug("{}: deleted.", getUrl());
                        deleted = true;
                        this.object = null;
                    } catch (OXException e) {
                        if (false == handle(e)) {
                            break;
                        }
                    }
                }
            }
        } finally {
            if (null != vCardID && deleted) {
                deleteVCard(session.getContextId(), vCardID);
            }
        }
    }

    @Override
    public void create() throws WebdavProtocolException {
        String vCardID = null;
        IFileHolder vCardFileHolder = null;
        boolean created = false;
        Session session = factory.getSession();
        try {
            if (exists()) {
                throw protocolException(getUrl(), HttpServletResponse.SC_CONFLICT);
            } else if (null == vCardImport || null == vCardImport.getContact()) {
                throw protocolException(getUrl(), HttpServletResponse.SC_NOT_FOUND);
            }
            /*
             * import vCard as new contact
             */
            ContactService contactService = factory.requireService(ContactService.class);
            Contact contact = vCardImport.getContact();
            if (null != url) {
                String extractedUID = Tools.extractUID(url);
                if (null != extractedUID && false == extractedUID.equals(contact.getUid())) {
                    /*
                     * Always extract the UID from the URL; the Addressbook client in MacOS 10.6 uses different UIDs in
                     * the WebDAV path and the UID field in the vCard, so we need to store this UID in the contact
                     * resource, too, to recognize later updates on the resource.
                     */
                    LOG.debug("{}: Storing WebDAV resource name in filename.", getUrl());
                    contact.setFilename(extractedUID);
                }
            }
            contact.setContextId(session.getContextId());
            String parentFolderID = parent.getFolder().getID();
            if (DAVUserAgent.IOS.equals(getUserAgent()) && false == parentFolderID.equals(factory.getState().getDefaultFolder().getID())) {
                /*
                 * for iOS, set initial parent to the default contacts folder & insert tombstone record for automatic cleanup during next sync
                 */
                ContactTombstoneStorage tombstoneStorage = factory.getOptionalService(ContactTombstoneStorage.class);
                if (null != tombstoneStorage && tombstoneStorage.supports(session, parentFolderID)) {
                    LOG.debug("{}: Re-routing contact creation to default folder for iOS client, inserting tombstone in targeted folder for client recovery.", getUrl());
                    tombstoneStorage.insertTombstone(session, parentFolderID, contact);
                }
                parentFolderID = factory.getState().getDefaultFolder().getID();
            }
            if (contact.getMarkAsDistribtuionlist() && false == parent.isSyncDistributionLists()) {
                /*
                 * insert & delete not supported contact group (next sync cleans up the client)
                 */
                try {
                    LOG.warn("{}: contact groups not supported, performing immediate deletion of this resource.", this.getUrl());
                    contact.removeDistributionLists();
                    contact.removeNumberOfDistributionLists();
                    contactService.createContact(session, parentFolderID, contact);
                    contactService.deleteContact(session, parentFolderID, Integer.toString(contact.getObjectID()), contact.getLastModified());
                } catch (OXException e) {
                    throw protocolException(getUrl(), e);
                }
                return;
            }

            restoreDistributionListReferences(session, contactService, contact, parentFolderID);
            /*
             * store original vCard if possible
             */
            vCardFileHolder = vCardImport.getVCard();
            vCardID = storeVCard(session.getContextId(), vCardFileHolder);
            contact.setVCardId(vCardID);
            /*
             * save contact, trying again in case of recoverable errors
             */
            object = contact;
            for (int i = 0; i <= MAX_RETRIES && false == created; i++) {
                try {
                    contactService.createContact(session, parentFolderID, object);
                    LOG.debug("{}: created.", getUrl());
                    created = true;
                } catch (OXException e) {
                    if (false == handle(e)) {
                        break;
                    }
                }
            }
            /*
             * process indicated attachments
             */
            handleAttachments(null, object);
        } catch (OXException e) {
            throw protocolException(getUrl(), e);
        } finally {
            Streams.close(vCardFileHolder);
            closeVCardImport();
            if (null != vCardID && false == created) {
                deleteVCard(session.getContextId(), vCardID);
            }
        }
    }

    private void restoreDistributionListReferences(Session session, ContactService contactService, Contact contact, String parentFolderID) throws OXException {
        if (contact == null || !contact.getMarkAsDistribtuionlist()) {
            return;
        }
        // try to search for known contacts
        SearchTerm<?> searchTerm = createSearchTerm(parentFolderID, Arrays.asList(contact.getDistributionList()), contact.getUid());
        SearchIterator<Contact> contacts = null;
        try {
            contacts = contactService.searchContacts(session, searchTerm, CONTACT_FIELDS_TO_LOAD);
            List<Contact> dbContacts = SearchIterators.asList(contacts);
            Optional<Contact> originalContact = dbContacts.stream().filter(x -> x.getUid().equals(contact.getUid())).findFirst();
            List<DistributionListEntryObject> prepared = restoreDistributionListReferences0(originalContact, Arrays.asList(contact.getDistributionList()), dbContacts);
            contact.setDistributionList(prepared.stream().toArray(DistributionListEntryObject[]::new));
        } finally {
            SearchIterators.close(contacts);
        }
    }

    private SearchTerm<?> createSearchTerm(String parentFolderID, List<DistributionListEntryObject> newDistList, String originalContactUID) {
        SingleSearchTerm folderTerm = new SingleSearchTerm(SingleOperation.EQUALS);
        folderTerm.addOperand(new ContactFieldOperand(ContactField.FOLDER_ID));
        folderTerm.addOperand(new ConstantOperand<String>(parentFolderID));

        CompositeSearchTerm uidOrTerm = null;
        for (DistributionListEntryObject entry : newDistList) {
            String uid = entry.getContactUid();
            if (Strings.isNotEmpty(uid)) {
                if (uidOrTerm == null) {
                    uidOrTerm = new CompositeSearchTerm(CompositeOperation.OR);
                }
                SingleSearchTerm uidTerm = new SingleSearchTerm(SingleOperation.EQUALS);
                uidTerm.addOperand(new ContactFieldOperand(ContactField.UID));
                uidTerm.addOperand(new ConstantOperand<String>(uid));
                uidOrTerm.addSearchTerm(uidTerm);
            }
        }
        SearchTerm<?> searchTerm = null;
        if (uidOrTerm != null) {
            if (Strings.isNotEmpty(originalContactUID)) {
                SingleSearchTerm uidTerm = new SingleSearchTerm(SingleOperation.EQUALS);
                uidTerm.addOperand(new ContactFieldOperand(ContactField.UID));
                uidTerm.addOperand(new ConstantOperand<String>(originalContactUID));
                uidOrTerm.addSearchTerm(uidTerm);
            }
            CompositeSearchTerm compSearchTerm = new CompositeSearchTerm(CompositeOperation.AND);
            compSearchTerm.addSearchTerm(folderTerm);
            compSearchTerm.addSearchTerm(uidOrTerm);
            searchTerm = compSearchTerm;
        } else {
            searchTerm = folderTerm;
        }
        return searchTerm;
    }

    private List<DistributionListEntryObject> restoreDistributionListReferences0(Optional<Contact> originalContact, List<DistributionListEntryObject> provided, List<Contact> dbContacts) {
        for (DistributionListEntryObject entry : provided) {
            if (entry.containsEntryID() && entry.containsFolderld() && entry.containsEmailfield()) { //skip the appropriate set ones
                continue;
            }
            Optional<Contact> findFirst = dbContacts.stream().filter(x -> x.getUid().equals(entry.getContactUid())).findFirst();
            if (findFirst.isPresent()) {
                Contact dbContact = findFirst.get();
                if (!entry.containsDisplayname()) {
                    entry.setDisplayname(dbContact.getDisplayName());
                }
                if (entry.containsEmailaddress() && Strings.isNotEmpty(entry.getEmailaddress())) {
                    String providedMailAddress = entry.getEmailaddress();
                    if (providedMailAddress.equals(dbContact.getEmail1())) {
                        entry.setEmailfield(DistributionListEntryObject.EMAILFIELD1);
                    } else if (providedMailAddress.equals(dbContact.getEmail2())) {
                        entry.setEmailfield(DistributionListEntryObject.EMAILFIELD2);
                    } else if (providedMailAddress.equals(dbContact.getEmail3())) {
                        entry.setEmailfield(DistributionListEntryObject.EMAILFIELD3);
                    }
                } else {
                    /*
                     * insert reference to first non-empty email address of contact as fallback
                     */
                    try {
                        if (Strings.isNotEmpty(dbContact.getEmail1())) {
                            entry.setEmailfield(DistributionListEntryObject.EMAILFIELD1);
                            entry.setEmailaddress(dbContact.getEmail1(), false);
                        } else if (Strings.isNotEmpty(dbContact.getEmail2())) {
                            entry.setEmailfield(DistributionListEntryObject.EMAILFIELD2);
                            entry.setEmailaddress(dbContact.getEmail2(), false);
                        } else if (Strings.isNotEmpty(dbContact.getEmail3())) {
                            entry.setEmailfield(DistributionListEntryObject.EMAILFIELD3);
                            entry.setEmailaddress(dbContact.getEmail3(), false);
                        }
                    } catch (OXException e) {
                        LOG.warn("Unexpected error assigning default mail address from reference contact", e);
                    }
                }
                if (!entry.containsEntryID() || entry.getEntryID() < 1) {
                    entry.setEntryID(dbContact.getObjectID());
                }
                if (!entry.containsFolderld() || entry.getFolderID() < 1) {
                    entry.setFolderID(dbContact.getParentFolderID());
                }
            } else if (originalContact.isPresent()) {
                // try to match dleo from original list
                DistributionListEntryObject[] origDistList = originalContact.get().getDistributionList();
                if (origDistList == null) {
                    continue;
                }
                // try matching uid...
                Optional<DistributionListEntryObject> findOriginalDistListEntry = Arrays.asList(origDistList).stream().filter(x -> x.getContactUid() != null && x.getContactUid().equals(entry.getContactUid())).findFirst();
                if (false == findOriginalDistListEntry.isPresent()) {
                    // try match by mail als last resort
                    findOriginalDistListEntry = Arrays.asList(origDistList).stream().filter(x -> x.getEmailaddress() != null && x.getEmailaddress().equals(entry.getEmailaddress())).findFirst();
                }
                if (findOriginalDistListEntry.isPresent()) { // internal user or one-off => set appropriate to not overwrite NULL values
                    DistributionListEntryObject relatedDbEntry = findOriginalDistListEntry.get();
                    if (relatedDbEntry.containsFolderld()) {
                        entry.setFolderID(relatedDbEntry.getFolderID());
                    }
                    if (relatedDbEntry.containsEntryID()) {
                        entry.setEntryID(relatedDbEntry.getEntryID());
                    }
                    if (relatedDbEntry.containsSortName()) {
                        entry.setSortName(relatedDbEntry.getSortName());
                    }
                    if (relatedDbEntry.containsEmailfield()) {
                        entry.setEmailfield(relatedDbEntry.getEmailfield());
                    }
                    if (relatedDbEntry.containsContactUid()) {
                        entry.setContactUid(relatedDbEntry.getContactUid());
                    }
                    continue;
                }
                continue;
            }
        }
        return provided;
    }

    @Override
    protected void deserialize(InputStream inputStream) throws OXException, IOException {
        VCardService vCardService = factory.requireService(VCardService.class);
        VCardParameters parameters = vCardService.createParameters(factory.getSession()).setKeepOriginalVCard(parent.isStoreOriginalVCard()).setImportAttachments(true).setRemoveAttachmentsFromKeptVCard(true).setDistributionListMode(getDistributionListMode());
        if (!exists()) {
            /*
             * import vCard as new contact
             */
            vCardImport = vCardService.importVCard(inputStream, null, parameters);
            if (null == vCardImport || null == vCardImport.getContact()) {
                throw new PreconditionException(DAVProtocol.CARD_NS.getURI(), "valid-address-data", getUrl(), HttpServletResponse.SC_FORBIDDEN);
            }
        } else {
            /*
             * import vCard and merge with existing contact, ensuring that some important properties don't change
             */
            ContactService contactService = factory.getContactService();
            if (null == contactService) {
                throw ServiceExceptionCode.absentService(ContactService.class);
            }
            Contact contact = contactService.getContact(factory.getSession(), String.valueOf(object.getParentFolderID()), String.valueOf(object.getObjectID()));
            if (null != contact) {
                String uid = contact.getUid();
                int parentFolderID = contact.getParentFolderID();
                int contextID = contact.getContextId();
                Date lastModified = contact.getLastModified();
                int objectID = contact.getObjectID();
                String vCardID = contact.getVCardId();
                contact.setProperty("com.openexchange.contact.vcard.photo.uri", PhotoUtils.buildURI(factory.getServiceSafe(ConfigViewFactory.class), getHostData(), contact));
                contact.setProperty("com.openexchange.contact.vcard.photo.contentType", contact.getImageContentType());
                vCardImport = factory.requireService(VCardService.class).importVCard(inputStream, contact, parameters);
                if (null == vCardImport || null == vCardImport.getContact()) {
                    throw new PreconditionException(DAVProtocol.CARD_NS.getURI(), "valid-address-data", getUrl(), HttpServletResponse.SC_FORBIDDEN);
                }
                vCardImport.getContact().setUid(uid);
                vCardImport.getContact().setParentFolderID(parentFolderID);
                vCardImport.getContact().setContextId(contextID);
                vCardImport.getContact().setLastModified(lastModified);
                vCardImport.getContact().setObjectID(objectID);
                vCardImport.getContact().setVCardId(vCardID);
            }
        }
    }

    @Override
    protected WebdavProperty internalGetProperty(WebdavProperty property) throws WebdavProtocolException {
        if (exists() && DAVProtocol.CARD_NS.getURI().equals(property.getNamespace()) && "address-data".equals(property.getName())) {
            String value;
            Set<String> propertyNames = extractRequestedProperties(property);
            if (null != propertyNames && 0 < propertyNames.size()) {
                try (VCardExport vCardExport = generateVCardResource(propertyNames); InputStream inputStream = vCardExport.getClosingStream()) {
                    value = Streams.stream2string(inputStream, Charsets.UTF_8_NAME);
                } catch (IOException | OXException e) {
                    throw protocolException(getUrl(), e);
                }
            } else {
                try (InputStream inputStream = getBody()) {
                    value = Streams.stream2string(inputStream, Charsets.UTF_8_NAME);
                } catch (IOException e) {
                    throw protocolException(getUrl(), e);
                }
            }
            WebdavProperty result = new WebdavProperty(property.getNamespace(), property.getName());
            result.setXML(true);
            result.setValue("<![CDATA[" + value + "]]>");
            return result;
        }
        return null;
    }

    /**
     * Silently closes the body file holder if set.
     */
    private void closeVCardImport() {
        if (null != vCardImport) {
            Streams.close(vCardImport);
            vCardImport = null;
        }
    }

    /**
     * Tries to handle an exception.
     *
     * @param e the exception to handle
     * @return <code>true</code>, if the operation should be retried, <code>false</code>, otherwise.
     */
    private boolean handle(OXException e) throws WebdavProtocolException {
        LOG.debug("Trying to handle exception: {}", e.getMessage(), e);
        if (Tools.isImageProblem(e)) {
            /*
             * image problem, handle by create without image
             */
            if (object != null) {
                LOG.warn("{}: {} - removing image and trying again.", getUrl(), e.getMessage());
                object.removeImage1();
            }
            return true;
        } else if (Tools.isDataTruncation(e)) {
            /*
             * handle by trimming truncated fields
             */
            if (trimTruncatedAttributes(e)) {
                LOG.warn("{}: {} - trimming fields and trying again.", getUrl(), e.getMessage());
                return true;
            }
        } else if (Tools.isIncorrectString(e)) {
            /*
             * handle by removing incorrect characters
             */
            if (replaceIncorrectStrings(e, "")) {
                LOG.warn("{}: {} - removing incorrect characters and trying again.", getUrl(), e.getMessage());
                return true;
            }
        } else if (Category.CATEGORY_PERMISSION_DENIED.equals(e.getCategory())) {
            /*
             * handle by overriding sync-token
             */
            LOG.debug("{}: {}", this.getUrl(), e.getMessage());
            LOG.debug("{}: overriding next sync token for client recovery.", this.getUrl());
            factory.setOverrideNextSyncToken(parent.getFolder().getID(), "0");
        } else if (Category.CATEGORY_CONFLICT.equals(e.getCategory())) {
            throw protocolException(getUrl(), e, HttpServletResponse.SC_CONFLICT);
        } else if (Category.CATEGORY_SERVICE_DOWN.equals(e.getCategory())) {
            /*
             * throw appropriate protocol exception
             */
            throw protocolException(getUrl(), e, HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } else {
            throw protocolException(getUrl(), e);
        }
        return false;
    }

    private boolean trimTruncatedAttributes(OXException e) {
        try {
            return MappedTruncation.truncate(e.getProblematics(), object);
        } catch (OXException x) {
            LOG.warn("{}: error trying to handle truncated attributes", getUrl(), x);
            return false;
        }
    }

    private boolean replaceIncorrectStrings(OXException e, String replacement) {
        try {
            return MappedIncorrectString.replace(e.getProblematics(), object, replacement);
        } catch (OXException x) {
            LOG.warn("{}: error trying to handle truncated attributes", getUrl(), x);
            return false;
        }
    }

    private VCardExport getVCardResource(boolean reset) throws WebdavProtocolException {
        VCardExport vCardResource = this.vCardExport;
        if (null == vCardResource) {
            try {
                vCardResource = generateVCardResource(null);
            } catch (OXException e) {
                throw protocolException(getUrl(), e);
            }
        }
        this.vCardExport = reset ? null : vCardResource;
        return vCardResource;
    }

    private VCardExport generateVCardResource(Set<String> propertyNames) throws OXException {
        /*
         * determine required contact fields for the export
         */
        VCardService vCardService = factory.requireService(VCardService.class);
        VCardParameters parameters = vCardService.createParameters(factory.getSession()).setDistributionListMode(getDistributionListMode());
        ContactField[] contactFields;
        if (null != propertyNames && 0 < propertyNames.size()) {
            parameters.setPropertyNames(propertyNames);
            contactFields = vCardService.getContactFields(propertyNames);
        } else {
            contactFields = null;
        }
        /*
         * load required contact data from storage
         */
        Contact contact = factory.getContactService().getContact(factory.getSession(), String.valueOf(object.getParentFolderID()), String.valueOf(object.getObjectID()), contactFields);
        applyAttachments(contact);
        if (isExportPhotoAsURI() && 0 < contact.getNumberOfImages()) {
            contact.setProperty("com.openexchange.contact.vcard.photo.uri", PhotoUtils.buildURI(factory.getServiceSafe(ConfigViewFactory.class), getHostData(), contact));
            contact.setProperty("com.openexchange.contact.vcard.photo.contentType", contact.getImageContentType());
        }
        /*
         * export contact data & return resulting vCard stream
         */
        InputStream originalVCard = null;
        try {
            Session session = factory.getSession();
            originalVCard = optVCard(session.getContextId(), object.getVCardId());
            return vCardService.exportContact(contact, originalVCard, parameters);
        } finally {
            Streams.close(originalVCard);
        }
    }

    /**
     * Stores a vCard.
     *
     * @param contextID The context identifier
     * @param fileHolder The file holder carrying the vCard data, or <code>null</code> to do nothing
     * @return The identifier of the stored vCard
     */
    private String storeVCard(int contextID, IFileHolder fileHolder) {
        if (null != fileHolder) {
            VCardStorageService vCardStorageService = factory.getVCardStorageService(contextID);
            if (null != vCardStorageService) {
                try (InputStream inputStream = fileHolder.getStream()) {
                    String vCardID = vCardStorageService.saveVCard(inputStream, contextID);
                    LOG.debug("{}: saved vCard in '{}'.", getUrl(), vCardID);
                    return vCardID;
                } catch (OXException | IOException e) {
                    LOG.warn("Error storing vCard in context {}.", I(contextID), e);
                }
            }
        }
        return null;
    }

    /**
     * Optionally gets the input stream for a stored vCard.
     *
     * @param contextID The context identifier
     * @param vCardID The identifier of the vCard to get, or <code>null</code> to do nothing
     * @return The vCard, or <code>null</code> if not available
     */
    private InputStream optVCard(int contextID, String vCardID) {
        if (null != vCardID) {
            VCardStorageService vCardStorage = factory.getVCardStorageService(contextID);
            if (null != vCardStorage) {
                try {
                    return vCardStorage.getVCard(vCardID, contextID);
                } catch (OXException e) {
                    LOG.warn("Error retrieving vCard with id {} in context {} from storage.", vCardID, I(contextID), e);
                }
            }
        }
        return null;
    }

    /**
     * Deletes a vCard silently.
     *
     * @param contextID The context identifier
     * @param vCardID The identifier of the vCard to delete, or <code>null</code> to do nothing
     * @return <code>true</code> if a vCard file actually has been deleted, <code>false</code>, otherwise
     */
    private boolean deleteVCard(int contextID, String vCardID) {
        if (null != vCardID) {
            VCardStorageService vCardStorage = factory.getVCardStorageService(contextID);
            if (null != vCardStorage) {
                try {
                    return vCardStorage.deleteVCard(vCardID, contextID);
                } catch (OXException e) {
                    if ("FLS-0017".equals(e.getErrorCode())) {
                        LOG.debug("vCard file with id {} in context {} no longer found in storage.", vCardID, I(contextID), e);
                    } else {
                        LOG.warn("Error while deleting vCard with id {} in context {} from storage.", vCardID, I(contextID), e);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Extracts a set of vCard property names as specified in the child nodes of the supplied <code>CARDDAV:address-data</code> property.
     *
     * @param addressDataProperty The <code>CARDDAV:address-data</code> property to get the requested vCard properties from
     * @return The requested vCard properties, or <code>null</code> if no specific properties are requested
     */
    private static Set<String> extractRequestedProperties(WebdavProperty addressDataProperty) {
        List<Element> childElements = addressDataProperty.getChildren();
        if (null != childElements && 0 < childElements.size()) {
            Set<String> propertyNames = new HashSet<String>(childElements.size());
            for (Element childElement : childElements) {
                if (DAVProtocol.CARD_NS.equals(childElement.getNamespace()) && "prop".equals(childElement.getName())) {
                    String name = childElement.getAttributeValue("name");
                    if (null != name) {
                        propertyNames.add(name);
                    }
                }
            }
            return propertyNames;
        }
        return null;
    }

    /**
     * Gets the distribution list mode to use for serialization to vCards, based on the client's user agent.
     *
     * @return The distribution list mode
     */
    private DistributionListMode getDistributionListMode() {
        return DistributionListMode.ADDRESSBOOKSERVER; // only mode as of now
    }

    /**
     * Gets a value indicating whether the value of the <code>PHOTO</code>-property in vCards should be exported as URI or not, based on
     * the <code>Prefer</code>-header sent by the client.
     *
     * @return <code>true</code> if the photo should be exported as URI, <code>false</code>, otherwise
     */
    private boolean isExportPhotoAsURI() {
        /*
         * evaluate "Prefer" header first
         */
        WebDAVRequestContext requestContext = DAVProtocol.getRequestContext();
        if (null != requestContext) {
            Enumeration<?> preferHeaders = requestContext.getHeaders("Prefer");
            if (null != preferHeaders && preferHeaders.hasMoreElements()) {
                do {
                    String value = String.valueOf(preferHeaders.nextElement());
                    if ("photo=uri".equalsIgnoreCase(value)) {
                        return true;
                    }
                    if ("photo=binary".equalsIgnoreCase(value)) {
                        return false;
                    }
                } while (preferHeaders.hasMoreElements());
            }
        }
        /*
         * default to configuration
         */
        try {
            return "uri".equals(factory.getServiceSafe(LeanConfigurationService.class).getProperty(CardDAVProperty.PREFERRED_PHOTO_ENCODING));
        } catch (OXException e) {
            LOG.warn("Error getting \"{}\", falling back 'binary'.", CardDAVProperty.PREFERRED_PHOTO_ENCODING, e);
        }
        return false;
    }

}
