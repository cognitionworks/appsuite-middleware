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

package com.openexchange.logging.filter;

import java.sql.Statement;
import org.slf4j.Marker;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;


/**
 * {@link ParamsCheckingTurboFilter} - Checks if parameters can be safely passed.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.4.2
 */
public class ParamsCheckingTurboFilter extends ExtendedTurboFilter {

    /**
     * Initializes a new {@link ParamsCheckingTurboFilter}.
     */
    public ParamsCheckingTurboFilter() {
        super();
    }

    @Override
    public int getRanking() {
        return -10;
    }

    @Override
    public FilterReply decide(final Marker marker, final Logger logger, final Level level, final String format, final Object[] params, final Throwable t) {
        if (null != params && params.length > 0) {
            if (logger.getEffectiveLevel().levelInt <= level.levelInt) {
                for (int i = params.length; i-- > 0;) {
                    Object param = params[i];
                    if (param instanceof Statement) {
                        // Statements might already be closed due to asynchronous nature of logging framework
                        // Ensure no exception occurs when trying to acquire statement's string representation
                        Statement stmt = (Statement) param;
                        params[i] = getSqlStatement(stmt, "<unknown>");
                    }
                }
            }
        }

        return FilterReply.NEUTRAL;
    }

    /**
     * Gets the SQL statement from given <code>Statement</code> instance.
     *
     * @param stmt The <code>Statement</code> instance
     * @param query The optional query associated with given <code>Statement</code> instance
     * @return The SQL statement
     */
    private String getSqlStatement(Statement stmt, String query) {
        if (stmt == null) {
            return query;
        }
        try {
            String sql = stmt.toString();
            int pos = sql.indexOf(": ");
            return pos < 0 ? sql : sql.substring(pos + 2);
        } catch (Exception x) {
            return query;
        }
    }

}
