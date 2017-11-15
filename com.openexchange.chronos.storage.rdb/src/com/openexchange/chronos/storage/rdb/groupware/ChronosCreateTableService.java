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

package com.openexchange.chronos.storage.rdb.groupware;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.openexchange.database.AbstractCreateTableImpl;

/**
 * {@link ChronosCreateTableService}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class ChronosCreateTableService extends AbstractCreateTableImpl {

    // Table names
    /**
     * The CALENDAR_ACCOUNT table name
     */
    protected static final String CALENDAR_ACCOUNT = "calendar_account";

    /**
     * the CALENDAR_ALARM table name
     */
    protected static final String CALENDAR_ALARM = "calendar_alarm";

    /**
     * The CALENDAR_ALARM_SEQUENCE table name
     */
    protected static final String CALENDAR_ALARM_SEQUENCE = "calendar_alarm_sequence";

    /**
     * The CALENDAR_ALARM_TRIGGER table name
     */
    protected static final String CALENDAR_ALARM_TRIGGER = "calendar_alarm_trigger";

    /**
     * The CALENDAR_ATTENDEE table name
     */
    protected static final String CALENDAR_ATTENDEE = "calendar_attendee";

    /**
     * The CALENDAR_ATTENDEE_TOMBSTONE table name
     */
    protected static final String CALENDAR_ATTENDEE_TOMBSTONE = "calendar_attendee_tombstone";

    /**
     * The CALENDAR_AVAILABLE table name
     */
    protected static final String CALENDAR_AVAILABLE = "calendar_available";

    /**
     * The CALENDAR_AVAILABLE_SEQUENCE table name
     */
    protected static final String CALENDAR_AVAILABLE_SEQUENCE = "calendar_available_sequence";

    /**
     * The CALENDAR_EVENT table name
     */
    protected static final String CALENDAR_EVENT = "calendar_event";

    /**
     * The CALENDAR_EVENT_SEQUENCE table name
     */
    protected static final String CALENDAR_EVENT_SEQUENCE = "calendar_event_sequence";

    /**
     * The CALENDAR_EVENT_TOMBSTONE table name
     */
    protected static final String CALENDAR_EVENT_TOMBSTONE = "calendar_event_tombstone";

    /**
     * Gets the <code>CREATE TABLE</code> statements for the <i>chronos</i> tables, mapped by their table names.
     *
     * @return The <code>CREATE TABLE</code> statements mapped by their table name
     */
    static Map<String, String> getTablesByName() {
        Map<String, String> tablesByName = new HashMap<String, String>(11); //@formatter:off
        tablesByName.put(CALENDAR_ACCOUNT,
            "CREATE TABLE calendar_account (" +
                "cid INT4 UNSIGNED NOT NULL," +
                "id INT4 UNSIGNED NOT NULL," +
                "user INT4 UNSIGNED NOT NULL," +
                "provider VARCHAR(64) NOT NULL," +
                "enabled BOOLEAN DEFAULT NULL," +
                "modified BIGINT(20) NOT NULL," +
                "internalConfig BLOB," +
                "userConfig BLOB," +
                "PRIMARY KEY (cid,id,user)," +
                "KEY user (cid,user,provider)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;"
        );
        tablesByName.put(CALENDAR_EVENT_SEQUENCE,
            "CREATE TABLE calendar_event_sequence (" +
                "cid INT4 UNSIGNED NOT NULL," +
                "account INT4 UNSIGNED NOT NULL," +
                "id INT4 UNSIGNED NOT NULL," +
                "PRIMARY KEY (cid,account)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;"
        );
        tablesByName.put(CALENDAR_ALARM_SEQUENCE,
            "CREATE TABLE calendar_alarm_sequence (" +
                "cid INT4 UNSIGNED NOT NULL," +
                "account INT4 UNSIGNED NOT NULL," +
                "id INT4 UNSIGNED NOT NULL," +
                "PRIMARY KEY (cid,account)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;"
        );
        tablesByName.put(CALENDAR_EVENT,
            "CREATE TABLE calendar_event (" +
                "cid INT4 UNSIGNED NOT NULL," +
                "account INT4 UNSIGNED NOT NULL," +
                "id INT4 UNSIGNED NOT NULL," +
                "user INT4 UNSIGNED DEFAULT NULL," +
                "folder VARCHAR(255) DEFAULT NULL," +
                "series INT4 UNSIGNED DEFAULT NULL," +
                "uid VARCHAR(1024) DEFAULT NULL," +
                "relatedTo VARCHAR(767) DEFAULT NULL," +
                "timestamp BIGINT(20) NOT NULL," +
                "created BIGINT(20)," +
                "createdBy INT4 UNSIGNED," +
                "modified BIGINT(20)," +
                "modifiedBy INT4 UNSIGNED," +
                "start datetime NOT NULL," +
                "startTimezone VARCHAR(255) DEFAULT NULL," +
                "end datetime DEFAULT NULL," +
                "endTimezone VARCHAR(255) DEFAULT NULL," +
                "allDay BOOLEAN DEFAULT NULL," +
                "rrule VARCHAR(255) DEFAULT NULL," +
                "exDate TEXT DEFAULT NULL," +
                "overriddenDate TEXT DEFAULT NULL," +
                "recurrence VARCHAR(32) DEFAULT NULL," +
                "sequence INT4 UNSIGNED DEFAULT NULL," +
                "transp INT4 UNSIGNED DEFAULT NULL," +
                "class VARCHAR(64) DEFAULT NULL," +
                "status VARCHAR(64) DEFAULT NULL," +
                "organizer VARCHAR(767) DEFAULT NULL," +
                "summary VARCHAR(255) DEFAULT NULL," +
                "location VARCHAR(255) DEFAULT NULL," +
                "description TEXT DEFAULT NULL," +
                "categories VARCHAR(1024) DEFAULT NULL," +
                "color VARCHAR(32) DEFAULT NULL," +
                "url VARCHAR(767) DEFAULT NULL," +
                "geo POINT DEFAULT NULL," +
                "rangeFrom BIGINT(20) NOT NULL," +
                "rangeUntil BIGINT(20) NOT NULL," +
                "filename VARCHAR(1024) DEFAULT NULL," +
                "extendedProperties BLOB DEFAULT NULL," +
                "PRIMARY KEY (cid,account,id)," +
                "KEY rangeFrom (cid,account,rangeFrom)," +
                "KEY rangeUntil (cid,account,rangeUntil)," +
                "KEY timestamp (cid,account,timestamp)," +
                "KEY user (cid,account,user)," +
                "KEY folder (cid,account,folder(191))," +
                "KEY uid (cid,account,uid(191))," +
                "KEY filename (cid,account,filename(191))" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;"
        );
        tablesByName.put(CALENDAR_EVENT_TOMBSTONE,
            "CREATE TABLE calendar_event_tombstone (" +
                "cid INT4 UNSIGNED NOT NULL," +
                "account INT4 UNSIGNED NOT NULL," +
                "id INT4 UNSIGNED NOT NULL," +
                "user INT4 UNSIGNED DEFAULT NULL," +
                "folder VARCHAR(255) DEFAULT NULL," +
                "series INT4 UNSIGNED DEFAULT NULL," +
                "uid VARCHAR(1024) DEFAULT NULL," +
                "relatedTo VARCHAR(767) DEFAULT NULL," +
                "timestamp BIGINT(20) NOT NULL," +
                "created BIGINT(20)," +
                "createdBy INT4 UNSIGNED," +
                "modified BIGINT(20)," +
                "modifiedBy INT4 UNSIGNED," +
                "start datetime DEFAULT NULL," +
                "startTimezone VARCHAR(255) DEFAULT NULL," +
                "end datetime DEFAULT NULL," +
                "endTimezone VARCHAR(255) DEFAULT NULL," +
                "allDay BOOLEAN DEFAULT NULL," +
                "rrule VARCHAR(255) DEFAULT NULL," +
                "exDate TEXT DEFAULT NULL," +
                "overriddenDate TEXT DEFAULT NULL," +
                "recurrence VARCHAR(32) DEFAULT NULL," +
                "sequence INT4 UNSIGNED DEFAULT NULL," +
                "transp INT4 UNSIGNED DEFAULT NULL," +
                "class VARCHAR(64) DEFAULT NULL," +
                "status VARCHAR(64) DEFAULT NULL," +
                "organizer VARCHAR(767) DEFAULT NULL," +
                "summary VARCHAR(255) DEFAULT NULL," +
                "location VARCHAR(255) DEFAULT NULL," +
                "description TEXT DEFAULT NULL," +
                "categories VARCHAR(1024) DEFAULT NULL," +
                "color VARCHAR(32) DEFAULT NULL," +
                "url VARCHAR(767) DEFAULT NULL," +
                "geo POINT DEFAULT NULL," +
                "rangeFrom BIGINT(20) NOT NULL," +
                "rangeUntil BIGINT(20) NOT NULL," +
                "filename VARCHAR(1024) DEFAULT NULL," +
                "extendedProperties BLOB DEFAULT NULL," +
                "PRIMARY KEY (cid,account,id)," +
                "KEY rangeFrom (cid,account,rangeFrom)," +
                "KEY rangeUntil (cid,account,rangeUntil)," +
                "KEY timestamp (cid,account,timestamp)," +
                "KEY user (cid,account,user)," +
                "KEY folder (cid,account,folder(191))," +
                "KEY uid (cid,account,uid(191))," +
                "KEY filename (cid,account,filename(191))" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;"
        );
        tablesByName.put(CALENDAR_ATTENDEE,
            "CREATE TABLE calendar_attendee (" +
                "cid INT4 UNSIGNED NOT NULL," +
                "account INT4 UNSIGNED NOT NULL," +
                "event INT4 UNSIGNED NOT NULL," +
                "entity INT4 NOT NULL," +
                "uri VARCHAR(512) NOT NULL," +
                "cn VARCHAR(512) DEFAULT NULL," +
                "folder VARCHAR(255) DEFAULT NULL," +
                "cuType VARCHAR(255) DEFAULT NULL," +
                "role VARCHAR(255) DEFAULT NULL," +
                "partStat VARCHAR(255) DEFAULT NULL," +
                "rsvp BOOLEAN DEFAULT NULL," +
                "comment TEXT DEFAULT NULL," +
                "member VARCHAR(1024) DEFAULT NULL," +
                "extendedProperties BLOB DEFAULT NULL," +
                "PRIMARY KEY (cid,account,event,entity)," +
                "KEY uri (cid,account,event,uri(191))," +
                "KEY folder (cid,account,entity,folder(191))" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;"
        );
        tablesByName.put(CALENDAR_ATTENDEE_TOMBSTONE,
            "CREATE TABLE calendar_attendee_tombstone (" +
                "cid INT4 UNSIGNED NOT NULL," +
                "account INT4 UNSIGNED NOT NULL," +
                "event INT4 UNSIGNED NOT NULL," +
                "entity INT4 NOT NULL," +
                "uri VARCHAR(512) NOT NULL," +
                "cn VARCHAR(512) DEFAULT NULL," +
                "folder VARCHAR(255) DEFAULT NULL," +
                "cuType VARCHAR(255) DEFAULT NULL," +
                "role VARCHAR(255) DEFAULT NULL," +
                "partStat VARCHAR(255) DEFAULT NULL," +
                "rsvp BOOLEAN DEFAULT NULL," +
                "comment TEXT DEFAULT NULL," +
                "member VARCHAR(1024) DEFAULT NULL," +
                "extendedProperties BLOB DEFAULT NULL," +
                "PRIMARY KEY (cid,account,event,entity)," +
                "KEY uri (cid,account,event,uri(191))," +
                "KEY folder (cid,account,entity,folder(191))" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;"
        );
        tablesByName.put(CALENDAR_ALARM,
            "CREATE TABLE calendar_alarm (" +
                "cid INT4 UNSIGNED NOT NULL," +
                "account INT4 UNSIGNED NOT NULL," +
                "id INT4 UNSIGNED NOT NULL," +
                "event VARCHAR(128) NOT NULL," +
                "user INT4 UNSIGNED NOT NULL," +
                "uid VARCHAR(767) DEFAULT NULL," +
                "relatedTo VARCHAR(767) DEFAULT NULL," +
                "acknowledged BIGINT(20) DEFAULT NULL," +
                "action VARCHAR(32) NOT NULL," +
                "repetition VARCHAR(64) DEFAULT NULL," +
                "triggerRelated VARCHAR(32) DEFAULT NULL," +
                "triggerDuration VARCHAR(32) DEFAULT NULL," +
                "triggerDate BIGINT(20) DEFAULT NULL," +
                "extendedProperties BLOB DEFAULT NULL," +
                "PRIMARY KEY (cid,account,id)," +
                "KEY event_user (cid,account,event,user)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;"
        );
        tablesByName.put(CALENDAR_ALARM_TRIGGER,
            "CREATE TABLE calendar_alarm_trigger (" +
                "cid INT4 UNSIGNED NOT NULL," +
                "account INT4 UNSIGNED NOT NULL," +
                "alarm INT4 UNSIGNED NOT NULL," +
                "user INT4 UNSIGNED NOT NULL," +
                "eventId VARCHAR(128) NOT NULL," +
                "folder VARCHAR(255) NOT NULL,"+
                "triggerDate BIGINT(20) NOT NULL," +
                "action VARCHAR(32) NOT NULL," +
                "recurrence VARCHAR(32) DEFAULT NULL," +
                "floatingTimezone VARCHAR(255) DEFAULT NULL," +
                "relatedTime BIGINT(20) DEFAULT NULL," +
                "pushed BOOL DEFAULT FALSE," +
                "PRIMARY KEY (cid,account,alarm)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;"
        );
        tablesByName.put(CALENDAR_AVAILABLE,
            "CREATE TABLE calendar_available (" +
                "cid INT4 UNSIGNED NOT NULL," +
                "id INT4 UNSIGNED NOT NULL," +
                "user INT4 UNSIGNED NOT NULL," +
                "uid VARCHAR(767) DEFAULT NULL," +
                "start datetime NOT NULL," +
                "end datetime DEFAULT NULL," +
                "startTimezone VARCHAR(255) DEFAULT NULL," +
                "endTimezone VARCHAR(255) DEFAULT NULL," +
                "allDay BOOLEAN DEFAULT NULL," +
                "created BIGINT(20) NOT NULL," +
                "description TEXT DEFAULT NULL," +
                "modified BIGINT(20) NOT NULL," +
                "location VARCHAR(255) DEFAULT NULL," +
                "exDate TEXT DEFAULT NULL," +
                "recurrence BIGINT(20) DEFAULT NULL," +
                "rrule VARCHAR(255) DEFAULT NULL," +
                "summary VARCHAR(255) DEFAULT NULL," +
                "categories VARCHAR(1024) DEFAULT NULL, " +
                "comment VARCHAR(512) DEFAULT NULL," +
                "extendedProperties BLOB DEFAULT NULL," +
                "PRIMARY KEY (cid,user,id)," +
                "KEY uid (cid,user,uid(191))" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;"
        );
        tablesByName.put(CALENDAR_AVAILABLE_SEQUENCE,
            "CREATE TABLE calendar_available_sequence (" +
                "cid INT4 UNSIGNED NOT NULL," +
                "id INT4 UNSIGNED NOT NULL," +
                "PRIMARY KEY (cid)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;"
        );
        return tablesByName; //@formatter:on
    }

    /**
     * Initializes a new {@link ChronosCreateTableService}.
     */
    public ChronosCreateTableService() {
        super();
    }

    @Override
    public String[] getCreateStatements() {
        Collection<String> createStatements = getTablesByName().values();
        return createStatements.toArray(new String[createStatements.size()]);
    }

    @Override
    public String[] requiredTables() {
        return new String[0];
    }

    @Override
    public String[] tablesToCreate() {
        Set<String> tableNames = getTablesByName().keySet();
        return tableNames.toArray(new String[tableNames.size()]);
    }

}
