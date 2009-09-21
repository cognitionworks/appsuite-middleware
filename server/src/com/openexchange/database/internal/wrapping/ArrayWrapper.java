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

package com.openexchange.database.internal.wrapping;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * The method {@link #getResultSet()} must be overwritten to return the {@link ResultSetWrapper}.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class ArrayWrapper implements Array {

    private final Array delegate;

    private final ResultSetWrapper result;

    public ArrayWrapper(Array delegate, ResultSetWrapper result) {
        super();
        this.delegate = delegate;
        this.result = result;
    }

    public Object getArray() throws SQLException {
        return delegate.getArray();
    }

    public Object getArray(Map<String, Class<?>> map) throws SQLException {
        return delegate.getArray(map);
    }

    public Object getArray(long index, int count) throws SQLException {
        return delegate.getArray(index, count);
    }

    public Object getArray(long index, int count, Map<String, Class<?>> map) throws SQLException {
        return delegate.getArray(index, count, map);
    }

    public int getBaseType() throws SQLException {
        return delegate.getBaseType();
    }

    public String getBaseTypeName() throws SQLException {
        return delegate.getBaseTypeName();
    }

    public ResultSet getResultSet() throws SQLException {
        return new ResultSetWrapper(delegate.getResultSet(), result.getStatement());
    }

    public ResultSet getResultSet(Map<String, Class<?>> map) throws SQLException {
        return new ResultSetWrapper(delegate.getResultSet(map), result.getStatement());
    }

    public ResultSet getResultSet(long index, int count) throws SQLException {
        return new ResultSetWrapper(delegate.getResultSet(index, count), result.getStatement());
    }

    public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map) throws SQLException {
        return new ResultSetWrapper(delegate.getResultSet(index, count, map), result.getStatement());
    }
}
