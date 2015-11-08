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

package com.openexchange.caldav.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.http.HttpServletResponse;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import com.openexchange.caldav.CaldavProtocol;
import com.openexchange.caldav.GroupwareCaldavFactory;
import com.openexchange.caldav.mixins.ScheduleOutboxURL;
import com.openexchange.data.conversion.ical.ConversionError;
import com.openexchange.data.conversion.ical.ConversionWarning;
import com.openexchange.data.conversion.ical.FreeBusyInformation;
import com.openexchange.dav.resources.DAVCollection;
import com.openexchange.exception.OXException;
import com.openexchange.freebusy.FreeBusyData;
import com.openexchange.webdav.protocol.Protocol;
import com.openexchange.webdav.protocol.Protocol.Property;
import com.openexchange.webdav.protocol.WebdavPath;
import com.openexchange.webdav.protocol.WebdavProtocolException;
import com.openexchange.webdav.protocol.WebdavResource;
import com.openexchange.webdav.protocol.helpers.AbstractResource;

/**
 * {@link ScheduleOutboxCollection} - A resource at which busy time
 * information requests are targeted.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ScheduleOutboxCollection extends DAVCollection {

    private static final String CALDAV_NS = CaldavProtocol.CAL_NS.getURI();
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ScheduleOutboxCollection.class);

    private final GroupwareCaldavFactory factory;

    private List<FreeBusyInformation> freeBusyRequest = null;

    /**
     * Initializes a new {@link ScheduleOutboxCollection}.
     *
     * @param factory the factory
     */
    public ScheduleOutboxCollection(GroupwareCaldavFactory factory) {
        super(factory, new WebdavPath(ScheduleOutboxURL.SCHEDULE_OUTBOX));
        this.factory = factory;
    }

    @Override
    public String getResourceType() throws WebdavProtocolException {
        return super.getResourceType() + "<CAL:schedule-outbox />";
    }

    @Override
    public void putBody(InputStream body, boolean guessSize) throws WebdavProtocolException {
        this.freeBusyRequest = this.parseFreeBusyRequest(body);
    }

    @Override
    public boolean hasBody() {
        return true;
    }

    @Override
    public InputStream getBody() throws WebdavProtocolException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(this.getScheduleResponse());
        XMLOutputter outputter = new XMLOutputter();
        try {
            outputter.output(document, outputStream);
        } catch (IOException e) {
            throw WebdavProtocolException.Code.GENERAL_ERROR.create(getUrl(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private List<FreeBusyInformation> parseFreeBusyRequest(final InputStream inputStream) throws WebdavProtocolException {
        try {
            return factory.getIcalParser().parseFreeBusy(inputStream, TimeZone.getTimeZone("UTC"), factory.getContext(),
                    new ArrayList<ConversionError>(), new ArrayList<ConversionWarning>());
        } catch (final ConversionError e) {
            throw WebdavProtocolException.Code.GENERAL_ERROR.create(getUrl(), HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    public Element getScheduleResponse() {
        Element scheduleResponse = new Element("schedule-response", CALDAV_NS);
        if (null != this.freeBusyRequest) {
            for (FreeBusyInformation freeBusyInformation : freeBusyRequest) {
                Map<String, FreeBusyData> freeBusy = null;
                try {
                    freeBusy = factory.getFreeBusyService().getMergedFreeBusy(factory.getSession(),
                        Arrays.asList(freeBusyInformation.getAttendees()),
                        freeBusyInformation.getStartDate(), freeBusyInformation.getEndDate());
                } catch (OXException e) {
                    LOG.error("error getting free/busy information", e);
                }
                if (null != freeBusy) {
                    for (String attendee : freeBusyInformation.getAttendees()) {
                        scheduleResponse.addContent(getResponse(attendee, freeBusyInformation.getUid(), freeBusy.get(attendee)));
                    }
                }
            }
        }
        return scheduleResponse;
    }

    private Element getResponse(String attendee, String uid, FreeBusyData freeBusyData) {
        Element response = new Element("response", CALDAV_NS);
        /*
         * prepare recipient
         */
        Element recipient = new Element("recipient", CALDAV_NS);
        response.addContent(recipient);
        Element href = new Element("href", Protocol.DAV_NS);
        href.addContent("mailto:" + attendee);
        recipient.addContent(href);
        /*
         * prepare request status
         */
        Element requestStatus = new Element("request-status", CALDAV_NS);
        response.addContent(requestStatus);
        /*
         * try to resolve user
         */
        if (null != freeBusyData) {
            /*
             * add freebusy info for this user
             */
            final Element calendarData = new Element("calendar-data", CALDAV_NS);
            response.addContent(calendarData);
            try {
                if (false == freeBusyData.hasData() && freeBusyData.hasWarnings()) {
                    requestStatus.addContent("3.7;Invalid calendar user");
                } else {
                    calendarData.addContent(this.getVFreeBusy(uid, freeBusyData));
                    requestStatus.addContent("2.0;Success");
                }
            } catch (OXException e) {
                LOG.warn("error getting freebusy", e);
                requestStatus.addContent("5.1;Service unavailable");
            }
        } else {
            /*
             * no info for this user
             */
            requestStatus.addContent("3.7;Invalid calendar user");
        }
        /*
         * add response description
         */
        Element responseDescription = new Element("responsedescription", Protocol.DAV_NS);
        responseDescription.addContent("OK");
        response.addContent(responseDescription);
        return response;
    }

    private String getVFreeBusy(String uid, FreeBusyData freeBusyData) throws OXException {
        /*
         * generate free busy information
         */
        FreeBusyInformation fbInfo = new FreeBusyInformation();
        fbInfo.setAttendee(freeBusyData.getParticipant());
        fbInfo.setUid(uid);
        fbInfo.setFreeBusyIntervals(freeBusyData.getIntervals());
        fbInfo.setUid(uid);
        fbInfo.setStartDate(freeBusyData.getFrom());
        fbInfo.setEndDate(freeBusyData.getUntil());
        /*
         * serialize as free/busy reply
         */
        return factory.getIcalEmitter().writeFreeBusyReply(
                fbInfo, factory.getContext(), new LinkedList<ConversionError>(), new LinkedList<ConversionWarning>());
    }

    @Override
    public String getContentType() throws WebdavProtocolException {
        return "text/xml; charset=UTF-8";
    }

    @Override
    public String getDisplayName() throws WebdavProtocolException {
        return "Schedule Outbox";
    }

    @Override
    protected boolean isset(final Property p) {
        return true;
    }

    @Override
    public void delete() throws WebdavProtocolException {
    }

    @Override
    public void setLanguage(final String language) throws WebdavProtocolException {
    }

    @Override
    public void setLength(final Long length) throws WebdavProtocolException {
    }

    @Override
    public void setContentType(final String type) throws WebdavProtocolException {
    }

    @Override
    public String getSource() throws WebdavProtocolException {
        return null;
    }

    @Override
    public void setSource(final String source) throws WebdavProtocolException {
    }

    @Override
    public List<WebdavResource> getChildren() throws WebdavProtocolException {
        return Collections.emptyList();
    }

    @Override
    public Date getCreationDate() throws WebdavProtocolException {
        return null;
    }

    @Override
    public Date getLastModified() throws WebdavProtocolException {
        return null;
    }

    @Override
    public AbstractResource getChild(final String name) throws WebdavProtocolException {
        return null;
    }

}
