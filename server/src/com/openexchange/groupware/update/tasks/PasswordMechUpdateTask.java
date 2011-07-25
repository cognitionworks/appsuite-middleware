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

package com.openexchange.groupware.update.tasks;

import static com.openexchange.tools.sql.DBUtils.closeSQLStuff;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.databaseold.Database;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.Schema;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTask;

/**
 * PasswordMechUpdateTask - Adds column <tt>passwordMech</tt> to table
 * <tt>user</tt> if it does not exist, yet
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public class PasswordMechUpdateTask implements UpdateTask {

	private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(PasswordMechUpdateTask.class));

	public int addedWithVersion() {
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.openexchange.groupware.update.UpdateTask#getPriority()
	 */
	public int getPriority() {
		/*
		 * Modification on database: highest priority.
		 */
		return UpdateTask.UpdateTaskPriority.HIGHEST.priority;
	}

	private static final String SQL_MODIFY = "ALTER TABLE user ADD COLUMN passwordMech VARCHAR(32) NOT NULL";

	private static final String SQL_UPDATE = "UPDATE user SET passwordMech = ?";

	private static final String SHA = "{SHA}";

	private static final String STR_INFO = "Performing update task 'PasswordMechUpdateTask'";

	public void perform(final Schema schema, final int contextId) throws OXException {
		if (LOG.isInfoEnabled()) {
			LOG.info(STR_INFO);
		}
		if (checkColumn(contextId)) {
			/*
			 * Column already exists
			 */
			return;
		}
		Connection writeCon = null;
		PreparedStatement stmt = null;
		try {
			writeCon = Database.get(contextId, true);
			try {
				stmt = writeCon.prepareStatement(SQL_MODIFY);
				stmt.executeUpdate();
				stmt.close();
				stmt = writeCon.prepareStatement(SQL_UPDATE);
				stmt.setString(1, SHA);
				stmt.executeUpdate();
			} catch (final SQLException e) {
	            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
			}
		} finally {
			closeSQLStuff(null, stmt);
			if (writeCon != null) {
				Database.back(contextId, true, writeCon);
			}
		}
	}

	private static final String SQL_SELECT_ALL = "SELECT * FROM user";

	private static final String COLUMN = "passwordMech";

	/**
	 * Determines if column named 'passwordMech' already exists in table 'user'
	 * 
	 * @param contextId -
	 *            the context ID
	 * @return <code>true</code> if column named 'passwordMech' was found;
	 *         otherwise <code>false</code>
	 * @throws OXException
	 */
	private static final boolean checkColumn(final int contextId) throws OXException {
		Connection readCon = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			readCon = Database.get(contextId, false);
			try {
				stmt = readCon.createStatement();
				rs = stmt.executeQuery(SQL_SELECT_ALL);
				final ResultSetMetaData meta = rs.getMetaData();
				final int length = meta.getColumnCount();
				boolean found = false;
				for (int i = 1; i <= length && !found; i++) {
					found = COLUMN.equals(meta.getColumnName(i));
				}
				return found;
			} catch (final SQLException e) {
	            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
			}
		} finally {
			closeSQLStuff(rs, stmt);
			if (readCon != null) {
				Database.back(contextId, false, readCon);
			}
		}
	}

}
