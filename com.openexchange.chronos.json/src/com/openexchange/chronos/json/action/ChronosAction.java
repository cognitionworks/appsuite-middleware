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

package com.openexchange.chronos.json.action;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.container.FileHolder;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.chronos.Attachment;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.json.converter.EventConflictResultConverter;
import com.openexchange.chronos.json.converter.mapper.EventMapper;
import com.openexchange.chronos.json.exception.CalendarExceptionCodes;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccess;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccessFactory;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.attach.AttachmentConfig;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.groupware.upload.impl.UploadEvent;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.TimeZoneUtils;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link ChronosAction}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.0
 */
public abstract class ChronosAction extends AbstractChronosAction {

    /**
     * Initializes a new {@link ChronosAction}.
     *
     * @param services A service lookup reference
     */
    protected ChronosAction(ServiceLookup services) {
        super(services);
    }

    @Override
    public AJAXRequestResult perform(AJAXRequestData requestData, ServerSession session) throws OXException {
        AJAXRequestResult result;
        IDBasedCalendarAccess calendarAccess = initCalendarAccess(requestData);
        boolean committed = false;
        try {
            calendarAccess.startTransaction();
            result = perform(calendarAccess, requestData);
            calendarAccess.commit();
            committed = true;
        } finally {
            if (false == committed) {
                calendarAccess.rollback();
            }
            calendarAccess.finish();
        }
        List<OXException> warnings = calendarAccess.getWarnings();
        if (null != warnings && 0 < warnings.size()) {
            result.addWarnings(warnings);
        }
        return result;
    }

    /**
     * Gets the timezone used to interpret <i>Time</i> parameters in for the underlying request.
     *
     * @return The timezone
     */
    protected static TimeZone getTimeZone(Session session, AJAXRequestData requestData) throws OXException {
        String timezoneId = requestData.getParameter("timezone");
        if (Strings.isEmpty(timezoneId)) {
            timezoneId = ServerSessionAdapter.valueOf(session).getUser().getTimeZone();
        }
        return TimeZoneUtils.getTimeZone(timezoneId);
    }

    /**
     * Performs a request.
     *
     * @param calendarAccess The initialized calendar access to use
     * @param requestData The underlying request data
     * @return The request result
     */
    protected abstract AJAXRequestResult perform(IDBasedCalendarAccess calendarAccess, AJAXRequestData requestData) throws OXException;

    /**
     * Handles the specified {@link OXException} and if it is of type
     * {@link Category#CATEGORY_CONFLICT}, it returns the correct response.
     * Otherwise the original exception is re-thrown
     *
     * @param e The {@link OXException} to handle
     * @return The proper conflict response
     * @throws OXException The original {@link OXException} if not of type {@link Category#CATEGORY_CONFLICT}
     */
    AJAXRequestResult handleConflictException(OXException e) throws OXException {
        if (isConflict(e)) {
            return new AJAXRequestResult(e.getProblematics(), EventConflictResultConverter.INPUT_FORMAT);
        }
        throw e;
    }

    /**
     * Initializes the calendar access for a request and parses all known parameters supplied by the client, throwing an appropriate
     * exception in case a required parameters is missing.
     *
     * @param requestData The underlying request data
     * @return The initialized calendar access
     */
    protected IDBasedCalendarAccess initCalendarAccess(AJAXRequestData requestData) throws OXException {
        IDBasedCalendarAccess calendarAccess = requireService(IDBasedCalendarAccessFactory.class).createAccess(requestData.getSession());
        Set<String> requiredParameters = getRequiredParameters();
        Set<String> optionalParameters = getOptionalParameters();
        Set<String> parameters = new HashSet<String>();
        parameters.addAll(requiredParameters);
        parameters.addAll(optionalParameters);
        for (String parameter : parameters) {
            Entry<String, ?> entry = parseParameter(requestData, parameter, requiredParameters.contains(parameter));
            if (null != entry) {
                calendarAccess.set(entry.getKey(), entry.getValue());
            }
        }
        return calendarAccess;
    }

    /**
     * Parses the {@link Event} from the payload object of the specified {@link AJAXRequestData}.
     * Any {@link Attachment} uploads will also be handled and properly attached to the {@link Event}.
     *
     * @param session The groupware {@link Session}
     * @param requestData The {@link AJAXRequestData}
     * @return The parsed {@link Event}
     * @throws OXException if a parsing error occurs
     */
    protected Event parseEvent(Session session, AJAXRequestData requestData) throws OXException {
        Map<String, UploadFile> uploads = new HashMap<>();
        JSONObject jsonEvent;
        long maxUploadSize = AttachmentConfig.getMaxUploadSize();
        if (requestData.hasUploads(-1, maxUploadSize > 0 ? maxUploadSize : -1L)) {
            jsonEvent = handleUploads(requestData, uploads);
        } else {
            jsonEvent = extractJsonBody(requestData);
        }
        try {
            Event event = EventMapper.getInstance().deserialize(jsonEvent, EventMapper.getInstance().getMappedFields(), getTimeZone(session, requestData));
            processAttachments(uploads, event);
            return event;
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_READ_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Processes any attachments the {@link Event} might have. It simply sets
     * the 'data' field of each attachment.
     *
     * @param uploads The uploaded attachments' data
     * @param event The event that contains the metadata of the attachments
     * @throws OXException if there are missing references between attachment metadata and attachment body parts
     */
    private void processAttachments(Map<String, UploadFile> uploads, Event event) throws OXException {
        if (!event.containsAttachments()) {
            return;
        }
        for (Attachment attachment : event.getAttachments()) {
            /*
             * skip all non-URI- and managed attachments
             */
            if (null == attachment.getUri() || 0 < attachment.getManagedId()) {
                continue;
            }
            /*
             * associate uploads to attachments by matching 'cid' URI
             */
            String contentId;
            try {
                URI uri = new URI(attachment.getUri());
                if (false == "cid".equalsIgnoreCase(uri.getScheme())) {
                    continue;
                }
                contentId = uri.getSchemeSpecificPart();
            } catch (URISyntaxException e) {
                throw CalendarExceptionCodes.MISSING_METADATA_ATTACHMENT_REFERENCE.create(e, attachment.getFilename());
            }
            UploadFile uploadFile = uploads.get(contentId);
            if (uploadFile == null) {
                throw CalendarExceptionCodes.MISSING_BODY_PART_ATTACHMENT_REFERENCE.create(contentId);
            }
            File tmpFile = uploadFile.getTmpFile();
            FileHolder fileHolder = new FileHolder(FileHolder.newClosureFor(tmpFile), tmpFile.length(), attachment.getFormatType(), attachment.getFilename());
            attachment.setData(fileHolder);
        }
    }

    /**
     * Handles the file uploads and extracts the {@link JSONObject} payload from the upload request.
     *
     * @param requestData The {@link AJAXRequestData}
     * @param uploads The {@link Map} with the uploads
     * @return The {@link JSONObject} payload of the POST request
     * @throws OXException if an error is occurred
     */
    private JSONObject handleUploads(AJAXRequestData requestData, Map<String, UploadFile> uploads) throws OXException {
        UploadEvent uploadEvent = requestData.getUploadEvent();
        final List<UploadFile> uploadFiles = uploadEvent.getUploadFiles();
        for (UploadFile uploadFile : uploadFiles) {
            String contentId = uploadFile.getContentId();
            if (Strings.isEmpty(contentId)) {
                contentId = uploadFile.getFieldName(); // fallback to 'name'
            }
            if (Strings.isEmpty(contentId)) {
                throw AjaxExceptionCodes.BAD_REQUEST_CUSTOM.create("Unable to extract the Content-ID for the attachment.");
            }
            uploads.put(contentId, uploadFile);
        }

        return extractJsonBody(uploadEvent);
    }

    /**
     * Extracts the {@link JSONObject} payload from the specified {@link AJAXRequestData}
     *
     * @param requestData the {@link AJAXRequestData} to extract the {@link JSONObject} payload from
     * @return The extracted {@link JSONObject} payload
     * @throws OXException if the payload is missing, or a parsing error occurs
     */
    protected JSONObject extractJsonBody(AJAXRequestData requestData) throws OXException {
        Object data = requestData.getData();
        if (data == null || !(data instanceof JSONObject)) {
            throw AjaxExceptionCodes.ILLEGAL_REQUEST_BODY.create();
        }
        return (JSONObject) data;
    }

    /**
     * Extracts the {@link JSONObject} payload from the specified {@link UploadEvent}
     *
     * @param upload the {@link UploadEvent} to extract the {@link JSONObject} payload from
     * @return The extracted {@link JSONObject} payload
     * @throws OXException if the payload is missing, or a parsing error occurs
     */
    private JSONObject extractJsonBody(UploadEvent upload) throws OXException {
        try {
            final String obj = upload.getFormField("json_0");
            if (Strings.isEmpty(obj)) {
                throw AjaxExceptionCodes.ILLEGAL_REQUEST_BODY.create();
            }

            JSONObject json = new JSONObject();
            json.reset();
            json.parseJSONString(obj);
            return json;
        } catch (final JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }
}
