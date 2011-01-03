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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.database.internal;

import static com.openexchange.tools.sql.DBUtils.closeSQLStuff;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.database.ConfigDatabaseService;
import com.openexchange.database.DBPoolingException;
import com.openexchange.database.DBPoolingExceptionCodes;

/**
 * This class contains methods for handling the server name and identifier.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class Server {

    private static final Log LOG = LogFactory.getLog(Server.class);

    private static final String PROPERTY_NAME = "SERVER_NAME";

    private static final String SELECT = "SELECT server_id FROM server WHERE name=?";

    private static String serverName;

    private static ConfigDatabaseService configDatabaseService;

    private static int serverId = -1;

    /**
     * Prevent instantiation
     */
    private Server() {
        super();
    }

    static void setConfigDatabaseService(ConfigDatabaseService configDatabaseService) {
        Server.configDatabaseService = configDatabaseService;
    }

    public static final int getServerId() throws DBPoolingException {
        synchronized (Server.class) {
            if (-1 == serverId) {
                serverId = Server.loadServerId(getServerName());
                if (-1 == serverId) {
                    throw DBPoolingExceptionCodes.NOT_RESOLVED_SERVER.create(getServerName());
                }
                LOG.trace("Got server id: " + serverId);
            }
        }
        return serverId;
    }

    public static final void start(ConfigurationService service) throws DBPoolingException {
        serverName = service.getProperty(PROPERTY_NAME);
        if (null == serverName || serverName.length() == 0) {
            throw DBPoolingExceptionCodes.NO_SERVER_NAME.create();
        }
    }

    public static String getServerName() throws DBPoolingException {
        if (null == serverName) {
            throw DBPoolingExceptionCodes.NOT_INITIALIZED.create(Server.class.getName());
        }
        return serverName;
    }

    private static int loadServerId(final String name) throws DBPoolingException {
        int retval = -1;
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            con = configDatabaseService.getReadOnly();
            stmt = con.prepareStatement(SELECT);
            stmt.setString(1, name);
            result = stmt.executeQuery();
            if (result.next()) {
                retval = result.getInt(1);
            }
        } catch (final SQLException e) {
            throw DBPoolingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, stmt);
            if (null != con) {
                configDatabaseService.backReadOnly(con);
            }
        }
        return retval;
    }
}
