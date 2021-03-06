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

package com.openexchange.chronos.storage;

import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.chronos.Conference;
import com.openexchange.database.provider.DBTransactionPolicy;
import com.openexchange.exception.OXException;

/**
 * {@link ConferenceStorage}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.4
 */
public interface ConferenceStorage {

    /**
     * Generates the next unique identifier for inserting new conference data.
     * <p/>
     * <b>Note:</b> This method should only be called within an active transaction, i.e. if the storage has been initialized using
     * {@link DBTransactionPolicy#NO_TRANSACTIONS} in favor of an externally controlled transaction.
     *
     * @return The next unique conference identifier
     */
    int nextId() throws OXException;

    /**
     * Loads all conferences for a specific event.
     *
     * @param eventId The identifier of the event to load the conferences for
     * @return The conferences
     */
    List<Conference> loadConferences(String eventId) throws OXException;

    /**
     * Loads the conferences for specific events.
     *
     * @param eventIds The identifiers of the events to load the conferences for
     * @return The conferences, mapped to the identifiers of the corresponding events
     */
    Map<String, List<Conference>> loadConferences(String[] eventIds) throws OXException;

    /**
     * Loads information about which events have at least one conference in the storage.
     *
     * @param eventIds The identifiers of the event to get the conference information for
     * @return A set holding the identifiers of those events where at least one conference stored
     */
    Set<String> hasConferences(String[] eventIds) throws OXException;

    /**
     * Deletes all conferences for a specific event.
     *
     * @param eventId The identifier of the event to delete the conferences for
     */
    void deleteConferences(String eventId) throws OXException;

    /**
     * Deletes all conferences for multiple events.
     *
     * @param eventIds The identifiers of the events to delete the conferences for
     */
    void deleteConferences(List<String> eventIds) throws OXException;

    /**
     * Deletes multiple conferences for a specific event.
     *
     * @param eventId The identifier of the event to delete the conferences for
     * @param conferenceIds The identifiers of the conferences to delete
     */
    void deleteConferences(String eventId, int[] conferencesIds) throws OXException;

    /**
     * Deletes all existing conferences for an account.
     *
     * @return <code>true</code> if something was actually deleted, <code>false</code>, otherwise
     */
    boolean deleteAllConferences() throws OXException;

    /**
     * Inserts conferences for a specific event.
     *
     * @param eventId The identifier of the event to insert the conferences for
     * @param conferences The conferences to insert
     */
    void insertConferences(String eventId, List<Conference> conferences) throws OXException;

    /**
     * Updates conferences for a specific event.
     *
     * @param eventId The identifier of the event to update the conferences for
     * @param conferences The conferences to update
     */
    void updateConferences(String eventId, List<Conference> conferences) throws OXException;

}
