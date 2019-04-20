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

package com.openexchange.principleusecount.impl;

import static com.openexchange.java.Autoboxing.I;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.principalusecount.PrincipalUseCountService;
import com.openexchange.principleusecount.impl.osgi.Services;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.session.Session;
import com.openexchange.threadpool.Task;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.tools.sql.DBUtils;

/**
 * {@link PrincipalUseCountServiceImpl}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.2
 */
public class PrincipalUseCountServiceImpl implements PrincipalUseCountService {

    @Override
    public void increment(Session session, int principal) throws OXException {
        try {
            Task<Void> task = new PrincipalUseCountTask(session, principal, PrincipalUseCountTask.TaskType.INCREMENT);
            ThreadPools.submitElseExecute(task);
        } catch (RuntimeException e) {
            throw PrincipalUseCountExceptionCode.UNKNOWN.create(e, e.getMessage());
        } catch (Exception e) {
            throw PrincipalUseCountExceptionCode.UNKNOWN.create(e, e.getMessage());
        }
    }

    @Override
    public void reset(Session session, int principal) throws OXException {
        try {
            Task<Void> task = new PrincipalUseCountTask(session, principal, PrincipalUseCountTask.TaskType.DELETE);
            ThreadPools.submitElseExecute(task);
        } catch (RuntimeException e) {
            throw PrincipalUseCountExceptionCode.UNKNOWN.create(e, e.getMessage());
        } catch (Exception e) {
            throw PrincipalUseCountExceptionCode.UNKNOWN.create(e, e.getMessage());
        }
    }

    @Override
    public void set(Session session, int principal, int value) throws OXException {
        try {
            Task<Void> task = new PrincipalUseCountTask(session, principal, I(value));
            ThreadPools.submitElseExecute(task);
        } catch (RuntimeException e) {
            throw PrincipalUseCountExceptionCode.UNKNOWN.create(e, e.getMessage());
        } catch (Exception e) {
            throw PrincipalUseCountExceptionCode.UNKNOWN.create(e, e.getMessage());
        }

    }

    private static final String SELECT_USECOUNT = "SELECT principal, value FROM principalUseCount WHERE cid=? AND user=? AND principal IN (";

    @Override
    public Map<Integer, Integer> get(Session session, Integer... principals) throws OXException {
        if (principals == null || principals.length == 0) {
            return Collections.emptyMap();
        }
        DatabaseService dbService = Services.getService(DatabaseService.class);
        if (null == dbService) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(DatabaseService.class);
        }
        String sql = DBUtils.getIN(SELECT_USECOUNT, principals.length);

        Connection con = dbService.getReadOnly(session.getContextId());
        ResultSet rs = null;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            int index = 1;
            stmt.setInt(index++, session.getContextId());
            stmt.setInt(index++, session.getUserId());
            for (Integer id : principals) {
                stmt.setInt(index++, id.intValue());
            }
            rs = stmt.executeQuery();
            Map<Integer, Integer> result = new HashMap<>();
            // Initialize result map with 0 values
            for (Integer id : principals) {
                result.put(id, I(0));
            }
            while (rs.next()) {
                result.put(I(rs.getInt("principal")), I(rs.getInt("value")));
            }
            Map<Integer, Integer> sorted = result.entrySet().stream().sorted(Entry.comparingByValue()).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2, LinkedHashMap::new));
            return sorted;
        } catch (SQLException e) {
            throw PrincipalUseCountExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs);
            if (con != null) {
                dbService.backReadOnly(session.getContextId(), con);
            }
        }
    }

}
