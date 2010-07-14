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

package com.openexchange.groupware.contact.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.openexchange.groupware.contact.Contacts.Mapper;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.userconfiguration.UserConfiguration;

/**
 * {@link PrivateFlag}
 *
 * @author <a href="mailto:ben.pahne@comfire.de">Benjamin Frederic Pahne</a>
 */
public final class PrivateFlag implements Mapper {

    public PrivateFlag() {
        super();
    }

    public String getDBFieldName() {
        return "pflag";
    }

    public void addToContactObject(ResultSet rs, int pos, Contact co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
        final int t = rs.getInt(pos);
        if (t == 1) {
            co.setPrivateFlag(true);
        } else {
            co.setPrivateFlag(false);
        }
    }

    public boolean containsElement(Contact co) {
        return co.containsPrivateFlag();
    }

    public void fillPreparedStatement(PreparedStatement ps, int pos, Contact co) throws SQLException {
        if (co.getPrivateFlag()) {
            ps.setInt(pos, 1);
        } else {
            ps.setNull(pos, java.sql.Types.INTEGER);
        }
    }

    public boolean compare(Contact co, Contact original) {
        return (co.getPrivateFlag() == original.getPrivateFlag());
    }

    public String getValueAsString(final Contact co) {
        return null;
    }

    public String getReadableTitle() {
        return null;
    }
}