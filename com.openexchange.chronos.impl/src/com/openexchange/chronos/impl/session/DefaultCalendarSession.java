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

package com.openexchange.chronos.impl.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import com.openexchange.chronos.common.DefaultCalendarParameters;
import com.openexchange.chronos.impl.Check;
import com.openexchange.chronos.impl.osgi.Services;
import com.openexchange.chronos.service.CalendarConfig;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarService;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.CalendarUtilities;
import com.openexchange.chronos.service.EntityResolver;
import com.openexchange.chronos.service.FreeBusyService;
import com.openexchange.chronos.service.RecurrenceService;
import com.openexchange.exception.OXException;
import com.openexchange.framework.request.RequestContext;
import com.openexchange.framework.request.RequestContextHolder;
import com.openexchange.groupware.notify.hostname.HostData;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link DefaultCalendarSession}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class DefaultCalendarSession implements CalendarSession {

    private final CalendarService calendarService;
    private final CalendarParameters parameters;
    private final ServerSession session;
    private final EntityResolver entityResolver;
    private final HostData hostData;
    private final CalendarConfig config;
    private final List<OXException> warnings;

    /**
     * Initializes a new {@link DefaultCalendarSession}.
     *
     * @param session The underlying server session
     * @param calendarService A reference to the calendar service
     * @throws OXException In case the user has no calendar access
     */
    public DefaultCalendarSession(Session session, CalendarService calendarService) throws OXException {
        this(session, calendarService, null);
    }

    /**
     * Initializes a new {@link DefaultCalendarSession}.
     *
     * @param session The underlying server session
     * @param calendarService A reference to the calendar service
     * @param parameters calendar parameters to use
     * @throws OXException In case the user has no calendar access
     */
    public DefaultCalendarSession(Session session, CalendarService calendarService, CalendarParameters parameters) throws OXException {
        super();
        this.calendarService = calendarService;
        this.parameters = null != parameters ? parameters : new DefaultCalendarParameters();
        this.session = Check.hasCalendar(ServerSessionAdapter.valueOf(session));
        this.entityResolver = new DefaultEntityResolver(this.session, Services.getServiceLookup());
        RequestContext requestContext = RequestContextHolder.get();
        this.hostData = null != requestContext ? requestContext.getHostData() : null;
        this.warnings = new ArrayList<OXException>();
        this.config = new CalendarConfigImpl(this, Services.getServiceLookup());
    }

    @Override
    public CalendarService getCalendarService() {
        return calendarService;
    }

    @Override
    public FreeBusyService getFreeBusyService() {
        return Services.getService(FreeBusyService.class);
    }

    @Override
    public RecurrenceService getRecurrenceService() {
        return Services.getService(RecurrenceService.class);
    }

    @Override
    public CalendarUtilities getUtilities() {
        return new DefaultCalendarUtilities(Services.getServiceLookup());
    }

    @Override
    public CalendarConfig getConfig() {
        return config;
    }

    @Override
    public ServerSession getSession() {
        return session;
    }

    @Override
    public HostData getHostData() {
        return hostData;
    }

    @Override
    public int getUserId() {
        return session.getUserId();
    }

    @Override
    public int getContextId() {
        return session.getContextId();
    }

    @Override
    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    @Override
    public void addWarning(OXException warning) {
        warnings.add(warning);
    }

    @Override
    public List<OXException> getWarnings() {
        return warnings;
    }

    @Override
    public <T> CalendarParameters set(String parameter, T value) {
        return parameters.set(parameter, value);
    }

    @Override
    public <T> T get(String parameter, Class<T> clazz) {
        return parameters.get(parameter, clazz);
    }

    @Override
    public <T> T get(String parameter, Class<T> clazz, T defaultValue) {
        return parameters.get(parameter, clazz, defaultValue);
    }

    @Override
    public boolean contains(String parameter) {
        return parameters.contains(parameter);
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return parameters.entrySet();
    }

    @Override
    public String toString() {
        return "CalendarSession [context=" + session.getContextId() + ", user=" + session.getUserId() + ", sessionId=" + session.getSessionID() + "]";
    }

}
