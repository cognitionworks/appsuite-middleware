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

package com.openexchange.chronos.itip.json.actions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.calendar.json.AppointmentAJAXRequest;
import com.openexchange.calendar.json.actions.AppointmentAction;
import com.openexchange.chronos.itip.ITipAnalysis;
import com.openexchange.chronos.itip.ITipAnalyzerService;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.conversion.ConversionService;
import com.openexchange.conversion.Data;
import com.openexchange.conversion.DataArguments;
import com.openexchange.conversion.DataProperties;
import com.openexchange.conversion.DataSource;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.osgi.RankingAwareNearRegistryServiceTracker;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 *
 * {@link AbstractITipAction}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.0
 */
public abstract class AbstractITipAction extends AppointmentAction {

    private static final String OWNER = "com.openexchange.conversion.owner";

    protected static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractITipAction.class);

    protected ServiceLookup services;

    protected RankingAwareNearRegistryServiceTracker<ITipAnalyzerService> analyzerListing;

    public AbstractITipAction(final ServiceLookup services, RankingAwareNearRegistryServiceTracker<ITipAnalyzerService> analyzerListing) {
        super(services);
        this.services = services;
        this.analyzerListing = analyzerListing;
    }

    @Override
    public AJAXRequestResult perform(CalendarSession session, AppointmentAJAXRequest request) throws OXException {
        ITipAnalyzerService analyzer = getAnalyzerService();

        final TimeZone tz = TimeZone.getTimeZone(new ServerSessionAdapter(session.getSession()).getUser().getTimeZone());
        final String timezoneParameter = request.getParameter("timezone");
        TimeZone outputTimeZone = timezoneParameter == null ? tz : TimeZone.getTimeZone(timezoneParameter);

        InputStream stream = null;
        try {
            Map<String, String> mailHeader = new HashMap<String, String>();
            stream = getInputStreamAndFillMailHeader(request.getRequest(), session, mailHeader);
            List<ITipAnalysis> analysis = analyzer.analyze(stream, request.getParameter("descriptionFormat"), session, mailHeader);
            return new AJAXRequestResult(process(analysis, request.getRequest(), session, outputTimeZone), new Date());
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e);
        } finally {
            Streams.close(stream);
        }
    }

    private ITipAnalyzerService getAnalyzerService() throws OXException {
        List<ITipAnalyzerService> serviceList = analyzerListing.getServiceList();
        if (serviceList == null || serviceList.isEmpty()) {
            throw ServiceExceptionCode.serviceUnavailable(ITipAnalyzerService.class);
        }
        ITipAnalyzerService analyzer = serviceList.get(0);
        if (analyzer == null) {
            throw ServiceExceptionCode.serviceUnavailable(ITipAnalyzerService.class);
        }
        return analyzer;
    }

    protected abstract Object process(List<ITipAnalysis> analysis, AJAXRequestData request, CalendarSession session, TimeZone tz) throws JSONException, OXException;

    private InputStream getInputStreamAndFillMailHeader(final AJAXRequestData request, final CalendarSession session, final Map<String, String> mailHeader) throws OXException {
        final String ds = request.getParameter("dataSource");
        if (ds != null) {
            final DataArguments dataArguments = new DataArguments();

            final Object data = request.getData();
            if (data != null) {
                final JSONObject body = (JSONObject) data;
                for (final Map.Entry<String, Object> entry : body.entrySet()) {
                    dataArguments.put(entry.getKey(), entry.getValue().toString());
                }
            } else {
                for (final Map.Entry<String, String> entry : request.getParameters().entrySet()) {
                    dataArguments.put(entry.getKey(), entry.getValue());
                }
            }

            final ConversionService conversionEngine = services.getService(ConversionService.class);
            if (null == conversionEngine) {
                throw ServiceExceptionCode.serviceUnavailable(ConversionService.class);
            }

            final DataSource dataSource = conversionEngine.getDataSource(ds);

            final Data<InputStream> dsData = dataSource.getData(InputStream.class, dataArguments, session.getSession());
            fillMailHeader(dsData, mailHeader);
            return dsData.getData();
        }
        final Object data = request.getData();
        if (data != null) {
            final JSONObject body = (JSONObject) data;
            try {
                return new ByteArrayInputStream(body.getString("ical").getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                LOG.error("", e);
                return null;
            } catch (JSONException x) {
                throw AjaxExceptionCodes.JSON_ERROR.create(x);
            }
        }
        return null;
    }

    private void fillMailHeader(final Data<InputStream> dsData, final Map<String, String> mailHeader) {
        final Map<String, String> properties = dsData.getDataProperties().toMap();

        for (final Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(DataProperties.PROPERTY_EMAIL_HEADER_PREFIX)) {
                mailHeader.put(key.substring(DataProperties.PROPERTY_EMAIL_HEADER_PREFIX.length() + 1, key.length()), entry.getValue());
            }

            if (key.equals(OWNER)) {
                mailHeader.put(OWNER, properties.get(OWNER));
            }
        }
    }
}
