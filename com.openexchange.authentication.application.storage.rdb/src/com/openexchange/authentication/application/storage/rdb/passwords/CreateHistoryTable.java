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

package com.openexchange.authentication.application.storage.rdb.passwords;

import com.openexchange.database.AbstractCreateTableImpl;

/**
 * {@link CreateHistoryTable}
 *
 * @author <a href="mailto:greg.hill@open-xchange.com">Greg Hill</a>
 * @since v7.10.4
 */
public class CreateHistoryTable extends AbstractCreateTableImpl {

    private static final String TABLE_APP_HISTORY = "app_passwords_history";

    //@formatter:off
    private static final String CREATE_APP_HISTORY_TABLE =
        "CREATE TABLE " + TABLE_APP_HISTORY + " ( "
            + "`uuid` binary(16) NOT NULL, "
            + "`cid` int(10) unsigned NOT NULL, "
            + "`user` int(10) unsigned NOT NULL, "
            + "`timestamp` bigint(20) DEFAULT NULL, "
            + "`client` varchar(128) DEFAULT NULL, "
            + "`userAgent` varchar(256) DEFAULT NULL, "
            + "`ip` varchar(64) DEFAULT NULL, "
//            + "`lastLogin` bigint(20) DEFAULT NULL, "
//            + "`lastDevice` varchar(128) DEFAULT NULL, "
//            + "`lastIp` varchar(64) DEFAULT NULL, "
            + "PRIMARY KEY (`uuid`), "
            + "KEY `id` (`cid`,`user`,`uuid`)) "
            + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    //@formatter:on

    @Override
    public String[] requiredTables() {
        return NO_TABLES;
    }

    @Override
    public String[] tablesToCreate() {
        return new String[] { TABLE_APP_HISTORY };
    }

    @Override
    protected String[] getCreateStatements() {
        return new String[] { CREATE_APP_HISTORY_TABLE };
    }
}
