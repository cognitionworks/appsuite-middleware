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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.groupware.reminder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.server.impl.DBPool;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorException;
import com.openexchange.tools.iterator.SearchIteratorException.Code;

class ReminderSearchIterator implements SearchIterator<ReminderObject> {

    private ReminderObject next;

    private final ResultSet rs;

    private final PreparedStatement preparedStatement;

    private final Connection readCon;

    private final List<AbstractOXException> warnings;

    private final Context ctx;

    ReminderSearchIterator(Context ctx, final PreparedStatement preparedStatement, final ResultSet rs, final Connection readCon) throws SearchIteratorException {
        super();
        this.ctx = ctx;
        this.warnings =  new ArrayList<AbstractOXException>(2);
        this.rs = rs;
        this.readCon = readCon;
        this.preparedStatement = preparedStatement;
        try {
            next = ReminderHandler.result2Object(ctx, rs, preparedStatement, false);
        } catch (final ReminderException exc) {
            next = null;
        } catch (final SQLException exc) {
            throw new SearchIteratorException(Code.SQL_ERROR, exc, EnumComponent.REMINDER);
        }
    }

    public boolean hasNext() {
        return next != null;
    }

    public ReminderObject next() throws SearchIteratorException {
        final ReminderObject reminderObj = next;
        try {
            next = ReminderHandler.result2Object(ctx, rs, preparedStatement, false);
        } catch (final ReminderException exc) {
            next = null;
        } catch (final SQLException exc) {
            throw new SearchIteratorException(Code.SQL_ERROR, exc, EnumComponent.REMINDER);
        }
        return reminderObj;
    }

    public void close() throws SearchIteratorException {
        try {
            if (rs != null) {
                rs.close();
            }

            if (preparedStatement != null) {
                preparedStatement.close();
            }

            DBPool.closeReaderSilent(ctx,readCon);
        } catch (final SQLException exc) {
            throw new SearchIteratorException(Code.SQL_ERROR, exc, EnumComponent.REMINDER);
        }
    }

    public int size() {
        return -1;
    }

    public boolean hasSize() {
        return false;
    }

    public void addWarning(final AbstractOXException warning) {
        warnings.add(warning);
    }

    public AbstractOXException[] getWarnings() {
        return warnings.isEmpty() ? null : warnings.toArray(new AbstractOXException[warnings.size()]);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}