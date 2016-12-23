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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.chronos.storage;

import java.util.List;
import java.util.Map;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.Event;
import com.openexchange.exception.OXException;

/**
 * {@link AlarmStorage}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public interface AlarmStorage {

    void insertAlarms(Event event, int userID, List<Alarm> alarms) throws OXException;

    Map<Integer, List<Alarm>> loadAlarms(Event event) throws OXException;

    List<Alarm> loadAlarms(Event event, int userID) throws OXException;

    Map<Integer, List<Alarm>> loadAlarms(List<Event> events, int userID) throws OXException;

    void updateAlarms(Event event, int userID, List<Alarm> alarms) throws OXException;

    //TODO: redundant?
    void updateFolderID(int eventID, int userID, int folderID) throws OXException;

    void updateAlarms(Event event) throws OXException;

    /**
     * Deletes all alarms stored for a specific event.
     *
     * @param objectID The identifier of the event to remove the alarms for
     */
    void deleteAlarms(int objectID) throws OXException;

    /**
     * Deletes all alarms of a user stored for a specific event.
     *
     * @param objectID The identifier of the event to remove the alarms for
     * @param userID The identifier of the user to remove the alarms for
     */
    void deleteAlarms(int objectID, int userID) throws OXException;

    /**
     * Deletes all alarms of multiple users stored for a specific event.
     *
     * @param objectID The identifier of the event to remove the alarms for
     * @param userIDs The identifiers of the users to remove the alarms for
     */
    void deleteAlarms(int objectID, int[] userIDs) throws OXException;

}
