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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.database;

import com.openexchange.exceptions.LocalizableStrings;

/**
 * Exception messages to translate for {@link DBPoolingException}.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class DBPoolingExceptionStrings implements LocalizableStrings {

    // The server is unable to connect to the configuration database.
    public static final String NO_CONFIG_DB_MSG = "Cannot get connection to config DB.";

    // It was not possible to determine the database server for a specific OX server and a context.
    // %1$d is replaced with some context identifier.
    // %1$d is replaced with some server identifier.
    public static final String RESOLVE_FAILED_MSG = "Resolving database for context %1$d and server %2$d not possible!";

    // The connection to the database could not be established.
    // %1$d is replaced with the database pool identifier.
    public static final String NO_CONNECTION_MSG = "Cannot get connection to database %1$d.";

    public static final String SCHEMA_FAILED_MSG = "Cannot set schema on database connection.";

    public static final String NULL_CONNECTION_MSG = "Null is returned to connection pool.";

    // %1$s is replaced with some SQL command.
    public static final String SQL_ERROR_MSG = "Problem with executing SQL: %1$s";

    // %1$d is replaced with the database pool identifier.
    public static final String NO_DBPOOL_MSG = "Cannot get information for pool %1$d.";

    // %1$s is replace with some java class name representing the JDBC driver.
    public static final String NO_DRIVER_MSG = "Driver class %1$s missing.";

    // The pool gives this exception if it believes the connection does not belong to it.
    // %1$d is replace with some database connection pool identifier.
    public static final String RETURN_FAILED_MSG = "Cannot return connection to pool %1$d.";

    // A property defining the server name was not found in the configuration properties files.
    public static final String NO_SERVER_NAME_MSG = "Server name is not defined.";

    // %1$s is replaced with some class name where that class has not been initialized before calling its methods.
    public static final String NOT_INITIALIZED_MSG = "%1$s is not initialized.";

    // %1$d is replaced with the time in milliseconds the connection has been used.
    public static final String TOO_LONG_MSG = "Connection used for %1$d milliseconds.";

    // %1$d is replaced with the number of statements that have not been closed before returning the connection.
    public static final String ACTIVE_STATEMENTS_MSG = "%1$d statements aren't closed.";

    // %1$s is replaced with the URL parameter string that is not parseable.
    public static final String PARAMETER_PROBLEM_MSG = "Parsing problem in URL parameter \"%1$s\".";

    public static final String MISSING_CONFIGURATION_MSG = "Configuration file for database configuration is missing.";

    // %1$s is replaced with the name of the required property that is not found. 
    public static final String PROPERTY_MISSING_MSG = "Property \"%1$s\" is not defined.";

    // %1$s is replaced with the name of the java class that should be initialized a second time.
    public static final String ALREADY_INITIALIZED_MSG = "%1$s is already initialized.";

    // %1$s is replaced with the defined server name.
    public static final String NOT_RESOLVED_SERVER_MSG = "Cannot resolve server id for server %1$s.";

    // This error message is only for administrators to show that some coding problem exists.
    // %1$d is replaced with a unique identifier of a database connection pool.
    public static final String UNKNOWN_POOL_MSG = "Nothing known about pool %1$d.";

    /**
     * Prevent instantiation.
     */
    private DBPoolingExceptionStrings() {
        super();
    }
}
