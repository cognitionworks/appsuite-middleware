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

package com.openexchange.contacts.json.actions;

import org.json.JSONException;
import org.json.JSONObject;

import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.contacts.json.ContactRequest;
import com.openexchange.contacts.json.RequestTools;
import com.openexchange.contacts.json.converters.ContactParser;
import com.openexchange.contacts.json.mapping.ContactMapper;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.ContactInterface;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.groupware.upload.impl.UploadEvent;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link NewAction}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
@Action(method = RequestMethod.PUT, name = "new", description = "Create a contact.", parameters = {
    @Parameter(name = "session", description = "A session ID previously obtained from the login module.")
}, requestBody = "Contact object as described in Common object data and Detailed contact data. The field id is not included. To add some contact image the PUT command must be replaced with a POST command and all data must be provided within a multipart/form-data body. The normal request body must be placed into a form field named json while the image file must be placed in a file field named file. The response is then an HTML page as described in section File uploads.",
responseDescription = "A json objekt with attribute id of the newly created contact.")
public class NewAction extends ContactAction {

    /**
     * Initializes a new {@link NewAction}.
     * @param serviceLookup
     */
    public NewAction(final ServiceLookup serviceLookup) {
        super(serviceLookup);
    }

    @Override
    protected AJAXRequestResult perform(final ContactRequest req) throws OXException {
        final ServerSession session = req.getSession();
        final boolean containsImage = req.containsImage();
        final JSONObject json = req.getContactJSON(containsImage);
        if (!json.has("folder_id")) {
            throw OXException.mandatoryField("missing folder");
        }

        try {
            final int folder = json.getInt("folder_id");
            final ContactInterface contactInterface = getContactInterfaceDiscoveryService().newContactInterface(folder, session);
            final ContactParser parser = new ContactParser();
            final Contact contact = parser.parse(json);
            if (containsImage) {
                UploadEvent uploadEvent = null;
                try {
                    uploadEvent = req.getUploadEvent();
                    final UploadFile file = uploadEvent.getUploadFileByFieldName("file");
                    if (file == null) {
                        throw AjaxExceptionCodes.NO_UPLOAD_IMAGE.create();
                    }

                    RequestTools.setImageData(contact, file);
                } finally {
                    if (uploadEvent != null) {
                        uploadEvent.cleanUp();
                    }
                }

            }

            contactInterface.insertContactObject(contact);
            final JSONObject object = new JSONObject("{\"id\":" + contact.getObjectID() + "}");
            return new AJAXRequestResult(object, contact.getLastModified(), "json");
        } catch (final JSONException e) {
            throw OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
        }
    }

    @Override
    protected AJAXRequestResult perform2(final ContactRequest request) throws OXException, JSONException {
        final boolean containsImage = request.containsImage();
        final JSONObject json = request.getContactJSON(containsImage);
        if (false == json.has("folder_id")) {
            throw OXException.mandatoryField("missing folder");
        }
        final Contact contact = ContactMapper.getInstance().deserialize(json, ContactMapper.getInstance().getAllFields());
        if (containsImage) {
        	RequestTools.setImageData(request, contact);
        }
        getContactService().createContact(request.getSession(), json.getString("folder_id"), contact);
        try {
            final JSONObject object = new JSONObject("{\"id\":" + contact.getObjectID() + "}");
            return new AJAXRequestResult(object, contact.getLastModified(), "json");
        } catch (final JSONException e) {
            throw OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
        }
    }

}
