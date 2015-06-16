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

package com.openexchange.carddav.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import com.openexchange.carddav.GroupwareCarddavFactory;
import com.openexchange.carddav.Tools;
import com.openexchange.carddav.mapping.CardDAVMapper;
import com.openexchange.contact.vcard.storage.VCardStorageService;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.tools.mappings.MappedIncorrectString;
import com.openexchange.groupware.tools.mappings.MappedTruncation;
import com.openexchange.groupware.tools.mappings.Mapping;
import com.openexchange.tools.versit.Versit;
import com.openexchange.tools.versit.VersitDefinition;
import com.openexchange.tools.versit.VersitObject;
import com.openexchange.tools.versit.converter.ConverterException;
import com.openexchange.webdav.protocol.WebdavPath;
import com.openexchange.webdav.protocol.WebdavProtocolException;

/**
 * {@link ContactResource} - Abstract base class for CardDAV resources.
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ContactResource extends CardDAVResource {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ContactResource.class);
	private static final int MAX_RETRIES = 3;

    private boolean exists = false;
    private Contact contact = null;
    private int retryCount = 0;
    private String parentFolderID = null;
    private String vCardId;

    /**
     * Creates a new {@link ContactResource} representing an existing contact.
     *
     * @param contact the contact
     * @param factory the CardDAV factory
	 * @param url the WebDAV URL
     */
	public ContactResource(Contact contact, GroupwareCarddavFactory factory, WebdavPath url) {
		super(factory, url);
		this.contact = contact;
		this.exists = null != contact;
	}

	/**
	 * Creates a new placeholder {@link ContactResource} at the specified URL.
	 *
     * @param factory the CardDAV factory
	 * @param url the WebDAV URL
	 * @param parentFolderID the ID of the parent folder
	 * @throws WebdavProtocolException
	 */
    public ContactResource(GroupwareCarddavFactory factory, WebdavPath url, String parentFolderID) throws WebdavProtocolException {
    	this(null, factory, url);
    	this.parentFolderID = parentFolderID;
    }

	@Override
	public void create() throws WebdavProtocolException {
		if (this.exists()) {
			throw protocolException(HttpServletResponse.SC_CONFLICT);
		} else if (null == this.contact) {
			throw protocolException(HttpServletResponse.SC_NOT_FOUND);
		}

        try {
		    if (false == contact.getMarkAsDistribtuionlist()) {
		        if (vCardId == null) {
		            storeVCard();
		        }
	            /*
	             * Insert contact
	             */
		        this.factory.getContactService().createContact(factory.getSession(), Integer.toString(contact.getParentFolderID()), contact);
	            LOG.debug("{}: created.", this.getUrl());
		    } else {
	            /*
	             * Insert & delete not supported contact (next sync cleans up the client)
	             */
                LOG.warn("{}: contact groups not supported, performing immediate deletion of this resource.", this.getUrl());
		        contact.removeDistributionLists();
		        contact.removeNumberOfDistributionLists();
                this.factory.getContactService().createContact(factory.getSession(), Integer.toString(contact.getParentFolderID()), contact);
                this.factory.getContactService().deleteContact(factory.getSession(), Integer.toString(contact.getParentFolderID()),
                    Integer.toString(contact.getObjectID()), contact.getLastModified());
		    }
        } catch (OXException e) {
        	if (handle(e)) {
        		this.create();
        	} else {
                if (vCardId != null && contact.getObjectID() == 0) {
                    factory.getVCardStorageService().deleteVCard(vCardId, factory.getSession().getContextId());
                }
        	}
        }
	}

	@Override
	public boolean exists() throws WebdavProtocolException {
		return this.exists;
	}

	@Override
	public void delete() throws WebdavProtocolException {
		if (false == this.exists()) {
			throw protocolException(HttpServletResponse.SC_NOT_FOUND);
		}
		
		vCardId = contact.getVCardId();
    	try {
    		/*
    		 * Delete contact
    		 */
        	this.factory.getContactService().deleteContact(factory.getSession(), Integer.toString(contact.getParentFolderID()),
        			Integer.toString(contact.getObjectID()), contact.getLastModified());

        	if (vCardId != null) {
        	    factory.getVCardStorageService().deleteVCard(vCardId, factory.getSession().getContextId());
        	}
            LOG.debug("{}: deleted.", this.getUrl());
            this.contact = null;
        } catch (OXException e) {
        	if (handle(e)) {
        		delete();
        	}
        }
    	
	}

	@Override
	public void save() throws WebdavProtocolException {
		if (false == this.exists()) {
			throw protocolException(HttpServletResponse.SC_NOT_FOUND);
		}
		
		String originalVCardId = contact.getVCardId();
        try {
            if (vCardId == null) {
                storeVCard();
            }
        	/*
        	 * Update contact
        	 */
        	this.factory.getContactService().updateContact(factory.getSession(), Integer.toString(contact.getParentFolderID()),
        			Integer.toString(contact.getObjectID()), contact, contact.getLastModified());
            LOG.debug("{}: saved.", this.getUrl());
        } catch (OXException e) {
        	if (handle(e)) {
        		save();
        	} else {
        	    if (originalVCardId != null) {
        	        contact.setVCardId(originalVCardId);
        	    }
        	    if (vCardId != null) {
                    factory.getVCardStorageService().deleteVCard(vCardId, factory.getSession().getContextId());
        	    }
        	}
        } finally {
            if (vCardId != null && originalVCardId != null && !vCardId.equals(originalVCardId)) {
                factory.getVCardStorageService().deleteVCard(originalVCardId, factory.getSession().getContextId());
            }
        }
	}

	@Override
	public Date getCreationDate() throws WebdavProtocolException {
		return null != this.contact ? contact.getCreationDate() : new Date(0);
	}

	@Override
	public Date getLastModified() throws WebdavProtocolException {
		return null != this.contact ? contact.getLastModified() : new Date(0);
	}

	@Override
	public String getDisplayName() throws WebdavProtocolException {
		return null != this.contact ? this.contact.getDisplayName() : null;
	}

	@Override
	public void setDisplayName(String displayName) throws WebdavProtocolException {
		if (null != this.contact) {
			this.contact.setDisplayName(displayName);
		}
	}

	@Override
	protected void applyVersitObject(VersitObject versitObject) throws WebdavProtocolException {
		try {
		    if (null == versitObject) {
                return;
            }
			/*
			 * Deserialize contact
			 */
			Contact newContact = isGroup(versitObject) ? deserializeAsTemporaryGroup(versitObject) : deserializeAsContact(versitObject);
		    if (this.exists()) {
		    	/*
		    	 * Update previously set metadata
		    	 */
		        newContact.setParentFolderID(this.contact.getParentFolderID());
		        newContact.setContextId(this.contact.getContextId());
		        newContact.setLastModified(this.contact.getLastModified());
                newContact.setObjectID(this.contact.getObjectID());
                newContact.setVCardId(this.contact.getVCardId());
		        /*
		         * Check for property changes
		         */
				for (final Mapping<? extends Object, Contact> mapping : CardDAVMapper.getInstance().getMappings().values()) {
					if (mapping.isSet(this.contact) && false == mapping.isSet(newContact)) {
						// set this one explicitly so that the property gets removed during update
						mapping.copy(newContact, newContact);
					} else if (mapping.isSet(newContact) && mapping.equals(contact, newContact)) {
						// this is no change, so ignore in update
						mapping.remove(newContact);
					}
				}
		        /*
		         * Never update the UID
		         */
		        newContact.removeUid();
		    } else {
		    	/*
		    	 * Apply default metadata
		    	 */
		        newContact.setContextId(this.factory.getSession().getContextId());
		        newContact.setParentFolderID(Tools.parse(this.parentFolderID));
	    		if (null != this.url) {
	    			String extractedUID = Tools.extractUID(url);
	    			if (null != extractedUID && false == extractedUID.equals(newContact.getUid())) {
	                	/*
	                	 * Always extract the UID from the URL; the Addressbook client in MacOS 10.6 uses different UIDs in
	                	 * the WebDAV path and the UID field in the vCard, so we need to store this UID in the contact
	                	 * resource, too, to recognize later updates on the resource.
	                	 */
	            		LOG.debug("{}: Storing WebDAV resource name in filename.", getUrl());
	            		newContact.setFilename(extractedUID);
	            	}
	    		}
		    }
		    /*
		     * Take over new contact
		     */
		    this.contact = newContact;
		} catch (ConverterException e) {
			throw protocolException(e);
		} catch (OXException e) {
			throw protocolException(e);
		}
	}

	@Override
	protected String generateVCard() throws WebdavProtocolException {
		return serializeAsContact();
	}

	@Override
	protected String getUID() {
		return null != this.contact ? this.contact.getUid() : Tools.extractUID(getUrl());
	}

	/**
	 * Stores the original vCard binary if the underlying contact service is capable of storing the reference.
	 *
	 * @return The vCard identifier of the VCardStorage or null if saving failed or is not possbible.
	 * @throws OXException 
	 */
    private void storeVCard() throws OXException {
        if (!factory.getContactService().supports(factory.getSession(), Integer.toString(contact.getParentFolderID()), ContactField.VCARD_ID)) {
            return;
        }

        try (InputStream stream = new ByteArrayInputStream(getOriginalVCard().getBytes())) {
            vCardId = factory.getVCardStorageService().saveVCard(stream, factory.getSession().getContextId());
        } catch (IOException e) {
            LOG.warn("Unable to store vcard.", e);
        }

        if (vCardId != null) {
            contact.setVCardId(vCardId);
        }
    }

	private Contact deserializeAsContact(VersitObject versitObject) throws OXException, ConverterException {
	    try (InputStream stream = new ByteArrayInputStream(getOriginalVCard().getBytes())) {
	        return factory.getVCardService().importVCard(stream, null, null);	        
	    } catch (IOException e) {
            throw super.protocolException(e);
        }
	}

    private Contact deserializeAsTemporaryGroup(VersitObject versitObject) throws OXException {
        Contact contact = new Contact();
        contact.setMarkAsDistributionlist(true);
        String formattedName = versitObject.getProperty("FN").getValue().toString();
        contact.setDisplayName(formattedName);
        contact.setSurName(formattedName);
        String uid = versitObject.getProperty("UID").getValue().toString();
        if (null != uid && 0 < uid.length()) {
            contact.setUid(uid);
        }
        return contact;
    }

    private String serializeAsContact() throws WebdavProtocolException {
        InputStream optVCard = null;
        if (contact.containsVCardId() && contact.getVCardId() != null) {
            optVCard = factory.getVCardStorageService().getVCard(contact.getVCardId(), contact.getContextId());
        }

        try (InputStream exportContact = factory.getVCardService().exportContact(contact, optVCard, null)) {
            return IOUtils.toString(exportContact, "UTF-8");
        } catch (OXException | IOException e) {
            throw super.protocolException(e);
        }
    }

	/**
	 * Tries to handle an exception.
	 *
	 * @param e the exception to handle
	 * @return <code>true</code>, if the operation should be retried,
	 * <code>false</code>, otherwise.
	 * @throws WebdavProtocolException
	 */
	private boolean handle(OXException e) throws WebdavProtocolException {
		boolean retry = false;
    	if (Tools.isImageProblem(e)) {
    		/*
    		 * image problem, handle by create without image
    		 */
        	LOG.warn("{}: {} - removing image and trying again.", this.getUrl(), e.getMessage());
        	this.contact.removeImage1();
        	retry = true;
        } else if (Tools.isDataTruncation(e)) {
            /*
             * handle by trimming truncated fields
             */
            if (this.trimTruncatedAttributes(e)) {
                LOG.warn("{}: {} - trimming fields and trying again.", this.getUrl(), e.getMessage());
                retry = true;
            }
        } else if (Tools.isIncorrectString(e)) {
            /*
             * handle by removing incorrect characters
             */
            if (this.replaceIncorrectStrings(e, "")) {
                LOG.warn("{}: {} - removing incorrect characters and trying again.", this.getUrl(), e.getMessage());
                retry = true;
            }
    	} else if (Category.CATEGORY_PERMISSION_DENIED.equals(e.getCategory())) {
    		/*
    		 * handle by overriding sync-token
    		 */
    		LOG.debug("{}: {}", this.getUrl(), e.getMessage());
        	LOG.debug("{}: overriding next sync token for client recovery.", this.getUrl());
			this.factory.overrideNextSyncToken();
    	} else if (Category.CATEGORY_CONFLICT.equals(e.getCategory())) {
    		throw super.protocolException(e, HttpServletResponse.SC_CONFLICT);
    	} else {
    		throw super.protocolException(e);
    	}

    	if (retry) {
    		retryCount++;
    		return retryCount <= MAX_RETRIES;
    	} else {
    		return false;
    	}
	}

    private boolean trimTruncatedAttributes(OXException e) {
        try {
            return MappedTruncation.truncate(e.getProblematics(), this.contact);
        } catch (OXException x) {
            LOG.warn("{}: error trying to handle truncated attributes", getUrl(), x);
            return false;
        }
    }

    private boolean replaceIncorrectStrings(OXException e, String replacement) {
        try {
            return MappedIncorrectString.replace(e.getProblematics(), this.contact, replacement);
        } catch (OXException x) {
            LOG.warn("{}: error trying to handle truncated attributes", getUrl(), x);
            return false;
        }
    }

    private static boolean isGroup(VersitObject versitObject) {
        com.openexchange.tools.versit.Property property = versitObject.getProperty("X-ADDRESSBOOKSERVER-KIND");
        return null != property  && "group".equals(property.getValue());
    }

}
