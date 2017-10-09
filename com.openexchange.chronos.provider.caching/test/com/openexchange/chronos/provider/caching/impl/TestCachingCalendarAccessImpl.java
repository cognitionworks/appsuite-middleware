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

package com.openexchange.chronos.provider.caching.impl;

import java.util.List;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.provider.CalendarAccount;
import com.openexchange.chronos.provider.CalendarFolder;
import com.openexchange.chronos.provider.caching.CachingCalendarAccess;
import com.openexchange.chronos.provider.caching.ExternalCalendarResult;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.exception.OXException;
import com.openexchange.session.Session;

/**
 * {@link TestCachingCalendarAccessImpl}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.10.0
 */
public class TestCachingCalendarAccessImpl extends CachingCalendarAccess {

    private boolean configSaved = false;

    public TestCachingCalendarAccessImpl(Session session, CalendarAccount account, CalendarParameters parameters) throws OXException {
        super(session, account, parameters);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.openexchange.chronos.provider.CalendarAccess#close()
     */
    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see com.openexchange.chronos.provider.CalendarAccess#getFolder(java.lang.String)
     */
    @Override
    public CalendarFolder getFolder(String folderId) throws OXException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.openexchange.chronos.provider.CalendarAccess#getVisibleFolders()
     */
    @Override
    public List<CalendarFolder> getVisibleFolders() throws OXException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.openexchange.chronos.provider.CalendarAccess#getChangeExceptions(java.lang.String, java.lang.String)
     */
    @Override
    public List<Event> getChangeExceptions(String folderId, String seriesId) throws OXException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.openexchange.chronos.provider.caching.CachingCalendarAccess#getRefreshInterval()
     */
    @Override
    public long getRefreshInterval() {
        return 60;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.openexchange.chronos.provider.caching.CachingCalendarAccess#getEvents(java.lang.String)
     */
    @Override
    public ExternalCalendarResult getEvents(String folderId) throws OXException {
        return new ExternalCalendarResult();
    }

    @Override
    protected void saveConfig() {
        configSaved = true;
    }

    public boolean isConfigSaved() {
        return configSaved;
    }

    @Override
    public long getExternalRequestTimeout() {
        return 1;
    }

    @Override
    public void handleExceptions(String calendarFolderId, OXException e) {
        // TODO Auto-generated method stub

    }

    @Override
    public String updateFolder(String folderId, CalendarFolder folder, long clientTimestamp) throws OXException {
        // TODO Auto-generated method stub
        return null;
    }

}
