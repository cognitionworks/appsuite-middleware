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

import java.util.UUID;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.chronos.Attachment;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccess;
import com.openexchange.chronos.service.EventID;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.server.ServiceLookup;

/**
 * {@link GetAttachment}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class GetAttachment extends ChronosAction {

    /**
     * Initialises a new {@link GetAttachment}.
     * 
     * @param services
     */
    protected GetAttachment(ServiceLookup services) {
        super(services);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.chronos.json.action.ChronosAction#perform(com.openexchange.chronos.provider.composition.IDBasedCalendarAccess, com.openexchange.ajax.requesthandler.AJAXRequestData)
     */
    @Override
    protected AJAXRequestResult perform(IDBasedCalendarAccess calendarAccess, AJAXRequestData requestData) throws OXException {
        // Gather the parameters
        EventID eventId = parseIdParameter(requestData);
        String managedId = requestData.getParameter("managedId");
        String folderId = requestData.getParameter("folderId");
        int mid = Integer.parseInt(managedId);

        // Get the attachment
        Attachment attachment = calendarAccess.getAttachment(eventId, folderId, mid);

        // Prepare the response
        ThresholdFileHolder fileHolder = new ThresholdFileHolder();
        boolean error = true;
        try {
            // Write to file holder
            fileHolder.write(attachment.getData().getStream());

            // Parameterise file holder
            fileHolder.setName(attachment.getFilename());
            fileHolder.setContentType(attachment.getFormatType());

            // Compose & return result
            AJAXRequestResult result = new AJAXRequestResult(fileHolder, "apiResponse");
            setETag(UUID.randomUUID().toString(), AJAXRequestResult.YEAR_IN_MILLIS * 50, result);
            error = false;
            return result;
        } finally {
            if (error) {
                Streams.close(fileHolder);
            }
        }
    }

    /**
     * Set the etag for the file
     * 
     * @param eTag The eTag identiier
     * @param expires The TTL in milliseconds
     * @param result The result to set the etag on
     */
    private void setETag(final String eTag, final long expires, final AJAXRequestResult result) {
        result.setExpires(expires);
        result.setHeader("ETag", eTag);
    }
}
