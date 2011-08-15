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

package com.openexchange.contacts.json.converters;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.fields.ContactFields;
import com.openexchange.ajax.fields.DistributionListFields;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.Converter;
import com.openexchange.ajax.requesthandler.ResultConverter;
import com.openexchange.contacts.json.RequestTools;
import com.openexchange.conversion.DataArguments;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.datasource.ContactImageDataSource;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.contact.helpers.ContactGetter;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;
import com.openexchange.groupware.container.LinkEntryObject;
import com.openexchange.image.ImageService;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link ContactJSONResultConverter}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class ContactJSONResultConverter implements ResultConverter {
    private static final Map<Integer, String> specialColumns = new HashMap<Integer, String>();

    private static final Log LOG = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(ContactJSONResultConverter.class));

    private final ImageService imageService;

    private ServerSession session;

    static {
        specialColumns.put(Contact.LAST_MODIFIED_OF_NEWEST_ATTACHMENT, "date");
        specialColumns.put(Contact.CREATION_DATE, "date");
        specialColumns.put(Contact.LAST_MODIFIED, "date");
        specialColumns.put(Contact.BIRTHDAY, "date");
        specialColumns.put(Contact.ANNIVERSARY, "date");
        specialColumns.put(Contact.IMAGE_LAST_MODIFIED, "date");
        specialColumns.put(Contact.IMAGE1_URL, "image");
        specialColumns.put(Contact.LAST_MODIFIED_UTC, "date_utc");
        specialColumns.put(Contact.DISTRIBUTIONLIST, "distributionlist");
        specialColumns.put(Contact.LINKS, "links");
        specialColumns.put(Contact.DEFAULT_ADDRESS, "remove_if_zero");
    }

    /**
     * Initializes a new {@link JSONResultConverter}.
     *
     * @param imageService
     */
    public ContactJSONResultConverter(final ImageService imageService) {
        super();
        this.imageService = imageService;
    }
    
    @Override
    public String getInputFormat() {
        return "contact";
    }

    @Override
    public String getOutputFormat() {
        return "json";
    }

    @Override
    public Quality getQuality() {
        return Quality.GOOD;
    }

    @Override
    public void convert(final AJAXRequestData request, final AJAXRequestResult result, final ServerSession session, final Converter converter) throws OXException {
        this.session = session;
        final Object resultObject = result.getResultObject();
        final Object newResultObject;
        if(resultObject instanceof Contact) {
            
            // Only one contact to convert
            final Contact contact = (Contact) resultObject;
            newResultObject = convertSingleContact(contact);
        } else {            
            final int[] columns = RequestTools.getColumnsAsIntArray(request, "columns"); 
            
            if (request.getAction().equals("updates")) {
                
                // result contains a Map<String, List<Contact>> to decide between deleted and modified contacts
                @SuppressWarnings("unchecked")
                final Map<String, List<Contact>> contactMap = (Map<String, List<Contact>>) resultObject;
                final List<Contact> modified = contactMap.get("modified");
                final List<Contact> deleted = contactMap.get("deleted");
                
                newResultObject = convertListOfContacts(modified, columns);
                if (!deleted.isEmpty()) {
                    addObjectIdsToResultArray(newResultObject, deleted);
                }                
            } else {
                
                // A list of contacts to convert
                @SuppressWarnings("unchecked")
                final List<Contact> contacts = (List<Contact>) resultObject;
                newResultObject = convertListOfContacts(contacts, columns);
            }
        }
        
        result.setResultObject(newResultObject, "json");
    }

    private Object convertSingleContact(final Contact contact) throws OXException {
        final JSONObject json = new JSONObject();
        final ContactGetter cg = new ContactGetter();
        for (final int column : Contact.JSON_COLUMNS) {
            final ContactField field = ContactField.getByValue(column);
            if (field != null && !field.getAjaxName().isEmpty()) {
                try {
                    final Object value = field.doSwitch(cg, contact);
                    
                    if (isSpecial(column)) {
                        final Object special = convertSpecial(field, contact, cg);
                        if (special != null && !String.valueOf(special).isEmpty()) {
                            final String jsonKey = field.getAjaxName();
                            json.put(jsonKey, special);
                        }                            
                    } else {
                        if (value != null && !String.valueOf(value).isEmpty()) {
                            final String jsonKey = field.getAjaxName();
                            json.put(jsonKey, value);
                        }
                    }
                } catch (final JSONException e) {
                    OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
                }
            }
        }
        
        return json;
    }
    
    private Object convertListOfContacts(final List<Contact> contacts, int[] columns) throws OXException {       
        final JSONArray resultArray = new JSONArray();
        for (final Contact contact : contacts) {
            final JSONArray contactArray = new JSONArray();
            
            final ContactGetter cg = new ContactGetter();
            for (final int column : columns) {
                final ContactField field = ContactField.getByValue(column);
                if (field != null && !field.getAjaxName().isEmpty()) {                
                    final Object value = field.doSwitch(cg, contact);
                    if (isSpecial(column)) {
                        final Object special = convertSpecial(field, contact, cg);
                        if (special == null) {
                            contactArray.put(JSONObject.NULL);                            
                        } else {
                            contactArray.put(special);
                        }                        
                    } else if (value == null) {
                        contactArray.put(JSONObject.NULL);
                    } else {
                        contactArray.put(value);
                    }
                } else {
                    LOG.warn("Did not find field or json name for column: " + column);
                    contactArray.put(JSONObject.NULL);
                }
            }
            
            resultArray.put(contactArray);
        }
        
        return resultArray;
    }
    
    private void addObjectIdsToResultArray(final Object object, final List<Contact> contacts) {
        final JSONArray resultArray = (JSONArray) object;
        for (final Contact contact : contacts) {
            resultArray.put(contact.getObjectID());
        }
    }

    protected boolean isSpecial(final int column) {
        return specialColumns.containsKey(column);
    }

    protected Object convertSpecial(final ContactField field, final Contact contact, final ContactGetter cg) throws OXException {
        final String type = specialColumns.get(field.getNumber());
        if (type.equals("date")) {
            final Object value = field.doSwitch(cg, contact);
            if (value != null) {
                final Date date = (Date) value;
                return date.getTime();
            }

            return null;
        } else if (type.equals("date_utc")) {
            // Set last_modified_utc
            final Date lastModified = contact.getLastModified();
            final Calendar calendar = new GregorianCalendar();
            calendar.setTime(lastModified);
            final int offset = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
            calendar.add(Calendar.MILLISECOND, -offset);

            return calendar.getTime().getTime();
        } else if (type.equals("image")) {
            String imageUrl = null;
            final ImageService imageService = getImageService();
            if (null == imageService) {
                LOG.warn("Contact image URL cannot be written. Missing service: " + ImageService.class.getName());
            } else {
                final byte[] imageData = contact.getImage1();
                if (imageData != null) {
                    final ContactImageDataSource imgSource = new ContactImageDataSource();
                    final DataArguments args = new DataArguments();
                    final String[] argsNames = imgSource.getRequiredArguments();
                    args.put(argsNames[0], String.valueOf(contact.getParentFolderID()));
                    args.put(argsNames[1], String.valueOf(contact.getObjectID()));
                    imageUrl = imageService.addImageData(session, imgSource, args).getImageURL();
                }
            }

            return imageUrl;
        } else if (type.equals("distributionlist")) {
            JSONArray distributionList = null;
            try {
                distributionList = getDistributionListAsJSONArray(contact);
            } catch (final JSONException e) {
                throw OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
            }

            return distributionList;
        } else if (type.equals("links")) {
            JSONArray links = null;
            try {
                links = getLinksAsJSONArray(contact);
            } catch (final JSONException e) {
                throw OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
            }

            return links;
        } else if (type.equals("remove_if_zero")) {
            Integer value = (Integer) field.doSwitch(cg, contact);
            if (value != null) {
                int intValue = value.intValue();
                if (intValue != 0) {
                    return intValue;
                }
            }
            
            return null;
        } else {
            return null;
        }
    }

    private JSONArray getDistributionListAsJSONArray(final Contact contact) throws JSONException {
        final DistributionListEntryObject[] distributionList = contact.getDistributionList();
        if (distributionList == null) {
            return null;
        }

        final JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < distributionList.length; i++) {
            final JSONObject entry = new JSONObject();
            final int emailField = distributionList[i].getEmailfield();

            if (!(emailField == DistributionListEntryObject.INDEPENDENT)) {
                entry.put(DistributionListFields.ID, distributionList[i].getEntryID());
            }

            entry.put(DistributionListFields.MAIL, distributionList[i].getEmailaddress());
            entry.put(DistributionListFields.DISPLAY_NAME, distributionList[i].getDisplayname());
            entry.put(DistributionListFields.MAIL_FIELD, emailField);

            jsonArray.put(entry);
        }

        return jsonArray;
    }

    private JSONArray getLinksAsJSONArray(final Contact contact) throws JSONException {
        final LinkEntryObject[] links = contact.getLinks();

        if (links != null) {
            final JSONArray jsonLinks = new JSONArray();
            for (int i = 0; i < links.length; i++) {
                final LinkEntryObject link = links[i];
                final JSONObject jsonLink = new JSONObject();

                if (link.containsLinkID()) {
                    jsonLink.put(ContactFields.ID, link.getLinkID());
                }

                jsonLink.put(ContactFields.DISPLAY_NAME, link.getLinkDisplayname());
                jsonLinks.put(jsonLink);
            }

            return jsonLinks;
        }
        return null;
    }

    protected ImageService getImageService() {
        return imageService;
    }
}
