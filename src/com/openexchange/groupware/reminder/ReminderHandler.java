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



package com.openexchange.groupware.reminder;

import com.openexchange.groupware.IDGenerator;
import com.openexchange.tools.iterator.SearchIteratorException;
import java.util.Date;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.ResultSet;

import com.openexchange.api.OXConflictException;
import com.openexchange.api.OXMandatoryFieldException;
import com.openexchange.api.OXObjectNotFoundException;
import com.openexchange.api2.OXException;
import com.openexchange.api2.ReminderSQLInterface;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.server.DBPool;
import com.openexchange.server.DBPoolingException;
import com.openexchange.sessiond.SessionObject;
import com.openexchange.tools.iterator.SearchIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ReminderHandler
 *
 * @author <a href="mailto:sebastian.kauss@open-xchange.org">Sebastian Kauss</a>
 */

public class ReminderHandler implements Types, ReminderSQLInterface {
	
	private Context context = null;
	
	private static final String sqlInsert = "INSERT INTO reminder (object_id, cid, target_id, module, userid, alarm, recurrence, last_modified, folder) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String sqlUpdate = "UPDATE reminder SET alarm = ?, recurrence = ?, description = ?, last_modified = ?, folder = ? WHERE cid = ? AND target_id = ? AND userid = ?";
	
	private static final String sqlDelete = "DELETE FROM reminder WHERE cid = ? AND target_id = ? AND module = ? AND userid = ?";
	
	private static final String sqlDeleteWithId = "DELETE FROM reminder WHERE cid = ? AND object_id = ?";
	
	private static final String sqlDeleteReminderOfObject = "DELETE FROM reminder WHERE cid = ? AND target_id = ? AND module = ?";
	
	private static final String sqlLoad = "SELECT object_id, target_id, module, userid, alarm, recurrence, description, folder, last_modified FROM reminder WHERE cid = ? AND target_id = ? AND module = ? AND userid = ?";
	
	private static final String sqlListByTargetId = "SELECT object_id, target_id, module, userid, alarm, recurrence, description, folder, last_modified FROM reminder WHERE cid = ? AND target_id = ?";
	
	private static final String sqlRange = "SELECT object_id, target_id, module, userid, alarm, recurrence, description, folder, last_modified FROM reminder WHERE cid = ? AND userid = ? AND alarm >= ?";
	
	private static final String sqlModified = "SELECT object_id, target_id, module, userid, alarm, recurrence, description, folder, last_modified FROM reminder WHERE cid = ? AND userid = ? AND last_modified >= ?";
	
	private static final String sqlLoadById = "SELECT object_id, target_id, module, userid, alarm, recurrence, description, folder, last_modified FROM reminder WHERE cid = ? AND object_id = ?";
	
	private static final String ERROR_MISSING_USER = "missing user";
	
	private static final String ERROR_MISSING_TARGET_ID = "missing target id";
	
	private static final String ERROR_MISSING_DATE = "missing date";
	
	private static final Log LOG = LogFactory.getLog(ReminderHandler.class);
	
	public ReminderHandler(SessionObject sessionObj) {
		context = sessionObj.getContext();
	}
	
	public ReminderHandler(Context context) {
		this.context = context;
	}
	
	public int insertReminder( final ReminderObject reminderObj) throws OXMandatoryFieldException, OXConflictException, OXException {
		Connection writeCon = null;
		
		try {
			writeCon = DBPool.pickupWriteable(context);
			
			return insertReminder(reminderObj, writeCon);
		} catch (DBPoolingException exc) {
			throw new OXException(exc);
		} finally {
			DBPool.closeWriterSilent(context,writeCon);
		}
	}
	
	public int insertReminder( final ReminderObject reminderObj, final Connection writeCon) throws OXMandatoryFieldException, OXConflictException, OXException {
		if (reminderObj.getUser() == 0) {
			throw new OXMandatoryFieldException(ERROR_MISSING_USER);
		}
		
		if (reminderObj.getTargetId() == null) {
			throw new OXMandatoryFieldException(ERROR_MISSING_TARGET_ID);
		}
		
		if (reminderObj.getDate() == null) {
			throw new OXMandatoryFieldException(ERROR_MISSING_DATE);
		}
		
		PreparedStatement ps = null;
		try {
			int a  = 0;
			
			final int objectId = IDGenerator.getId(context, Types.REMINDER);
			reminderObj.setObjectId(objectId);
			
			ps = writeCon.prepareStatement(sqlInsert);
			ps.setInt(++a, reminderObj.getObjectId());
			ps.setLong(++a, context.getContextId());
			ps.setString(++a, reminderObj.getTargetId());
			ps.setInt(++a, reminderObj.getModule());
			ps.setInt(++a, reminderObj.getUser());
			ps.setTimestamp(++a, new Timestamp(reminderObj.getDate().getTime()));
			ps.setBoolean(++a, reminderObj.isRecurrenceAppointment());
			ps.setLong(++a, System.currentTimeMillis());
			ps.setString(++a, reminderObj.getFolder());
			
			ps.executeUpdate();
			ps.close();
			
			return objectId;
		} catch (SQLException exc) {
			throw new OXException(exc);
		}
	}
	
	public void updateReminder( final ReminderObject reminderObj) throws OXMandatoryFieldException, OXConflictException, OXException {
		Connection writeCon = null;
		
		try {
			writeCon = DBPool.pickupWriteable(context);
			
			updateReminder(reminderObj, writeCon);
		} catch (DBPoolingException exc) {
			throw new OXException(exc);
		} finally {
			DBPool.closeWriterSilent(context,writeCon);
		}
	}
	
	public void updateReminder( final ReminderObject reminderObj, final Connection writeCon) throws OXMandatoryFieldException, OXConflictException, OXException {
		if (reminderObj.getUser() == 0) {
			throw new OXMandatoryFieldException(ERROR_MISSING_USER);
		}
		
		if (reminderObj.getTargetId() == null) {
			throw new OXMandatoryFieldException(ERROR_MISSING_TARGET_ID);
		}
		
		if (reminderObj.getDate() == null) {
			throw new OXMandatoryFieldException(ERROR_MISSING_DATE);
		}
		
		PreparedStatement ps = null;
		try {
			int a = 0;
			
			ps = writeCon.prepareStatement(sqlUpdate);
			
			ps.setTimestamp(++a, new Timestamp(reminderObj.getDate().getTime()));
			ps.setBoolean(++a, reminderObj.isRecurrenceAppointment());
			
			final String description = reminderObj.getDescription();
			
			if (description == null) {
				ps.setNull(++a, java.sql.Types.VARCHAR);
			} else {
				ps.setString(++a, description);
			}
			
			ps.setLong(++a, System.currentTimeMillis());
			ps.setString(++a, reminderObj.getFolder());
			ps.setInt(++a, context.getContextId());
			ps.setString(++a, reminderObj.getTargetId());
			ps.setInt(++a, reminderObj.getUser());
			
			ps.executeUpdate();
			ps.close();
		} catch (SQLException exc) {
			throw new OXException(exc);
		}
	}
	
	public void deleteReminder(int objectId) throws OXException {
		Connection writeCon = null;
		PreparedStatement ps = null;
		try {
			writeCon = DBPool.pickupWriteable(context);
			int a = 0;
			
			ps = writeCon.prepareStatement(sqlDeleteWithId);
			ps.setInt(++a, context.getContextId());
			ps.setInt(++a, objectId);
			
			final int deleted = ps.executeUpdate();
			ps.close();
			
			if (deleted == 0) {
				throw new OXObjectNotFoundException("no reminder to delete!");
			}
		} catch (SQLException exc) {
			throw new OXException(exc);
		} catch (DBPoolingException exc) {
			throw new OXException(exc);
		} finally {
			DBPool.closeWriterSilent(context,writeCon);
		}
	}
	
	public void deleteReminder( final int targetId, final int userId, final int module) throws OXMandatoryFieldException, OXConflictException, OXException {
		deleteReminder(String.valueOf(targetId), userId, module);
	}
	
	public void deleteReminder( final String targetId, final int userId, final int module) throws OXMandatoryFieldException, OXConflictException, OXException {
		Connection writeCon = null;
		
		try {
			writeCon = DBPool.pickupWriteable(context);
			
			deleteReminder(targetId, userId, module, writeCon);
		} catch (DBPoolingException exc) {
			throw new OXException(exc);
		} finally {
			DBPool.closeWriterSilent(context,writeCon);
		}
	}
	
	public void deleteReminder( final int targetId, final int userId, final int module, final Connection writeCon) throws OXMandatoryFieldException, OXConflictException, OXException {
		deleteReminder(String.valueOf(targetId), userId, module, writeCon);
	}
	
	public void deleteReminder( final String targetId, final int userId, final int module, final Connection writeCon) throws OXMandatoryFieldException, OXConflictException, OXException {
		if (userId == 0) {
			throw new OXMandatoryFieldException(ERROR_MISSING_USER);
		}
		
		if (targetId == null) {
			throw new OXMandatoryFieldException(ERROR_MISSING_TARGET_ID);
		}
		
		PreparedStatement ps = null;
		try {
			int a = 0;
			
			ps = writeCon.prepareStatement(sqlDelete);
			ps.setInt(++a, context.getContextId());
			ps.setString(++a, targetId);
			ps.setInt(++a, module);
			ps.setInt(++a, userId);
			
			final int deleted = ps.executeUpdate();
			ps.close();
			
			if (deleted == 0) {
				throw new OXObjectNotFoundException("no reminder to delete!");
			}
		} catch (SQLException exc) {
			throw new OXException(exc);
		}
	}
	
	public void deleteReminder( final int targetId, final int module) throws OXMandatoryFieldException, OXConflictException, OXException {
		deleteReminder(String.valueOf(targetId), module);
	}
	
	public void deleteReminder( final String targetId, final int module) throws OXMandatoryFieldException, OXConflictException, OXException {
		Connection writeCon = null;
		
		try {
			writeCon = DBPool.pickupWriteable(context);
			
			deleteReminder(targetId, module, writeCon);
		} catch (DBPoolingException exc) {
			throw new OXException(exc);
		} finally {
			DBPool.closeWriterSilent(context,writeCon);
		}
	}
	
	public void deleteReminder( final int targetId, final int module, final Connection writeCon) throws OXMandatoryFieldException, OXConflictException, OXException {
		deleteReminder(String.valueOf(targetId), module, writeCon);
	}
	
	public void deleteReminder( final String targetId, final int module, final Connection writeCon) throws OXMandatoryFieldException, OXConflictException, OXException {
		if (targetId == null) {
			throw new OXMandatoryFieldException(ERROR_MISSING_TARGET_ID);
		}
		
		PreparedStatement ps = null;
		try {
			int a = 0;
			
			ps = writeCon.prepareStatement(sqlDeleteReminderOfObject);
			ps.setInt(++a, context.getContextId());
			ps.setString(++a, targetId);
			ps.setInt(++a, module);
			
			final int deleted = ps.executeUpdate();
			ps.close();
			
			if (deleted == 0) {
				throw new OXObjectNotFoundException("no reminder to delete!");
			}
		} catch (SQLException exc) {
			throw new OXException(exc);
		}
	}
	
	public boolean existsReminder(final int targetId, final int userId, final int module) throws OXMandatoryFieldException, OXConflictException, OXException {
		try {
			loadReminder(targetId, userId, module);
			return true;
		} catch (OXObjectNotFoundException exc) {
			return false;
		}
	}
	
	public ReminderObject loadReminder( final int targetId, final int userId, final int module) throws OXMandatoryFieldException, OXConflictException, OXException {
		return loadReminder(String.valueOf(targetId), userId, module);
	}
	
	public ReminderObject loadReminder( final String targetId, final int userId, final int module) throws OXMandatoryFieldException, OXConflictException, OXException {
		Connection readCon = null;
		
		try {
			readCon = DBPool.pickup(context);
			
			return loadReminder(targetId, userId, module, readCon);
		} catch (DBPoolingException exc) {
			throw new OXException(exc);
		} finally {
			DBPool.closeReaderSilent(context,readCon);
		}
	}
	
	public ReminderObject loadReminder( final int targetId, final int userId, final int module, final Connection readCon) throws OXMandatoryFieldException, OXConflictException, OXException, OXObjectNotFoundException {
		return loadReminder(String.valueOf(targetId), userId, module, readCon);
	}
	
	public ReminderObject loadReminder( final String targetId, final int userId, final int module, final Connection readCon) throws OXMandatoryFieldException, OXConflictException, OXException, OXObjectNotFoundException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			int a = 0;
			
			ps = readCon.prepareStatement(sqlLoad);
			ps.setInt(++a, context.getContextId());
			ps.setString(++a, targetId);
			ps.setInt(++a, module);
			ps.setInt(++a, userId);
			
			rs = ps.executeQuery();
			return convertResult2ReminderObject(rs, ps, true);
		} catch (SQLException exc) {
			throw new OXException(exc);
		}
	}
	
	public ReminderObject loadReminder( final int objectId) throws OXMandatoryFieldException, OXConflictException, OXException {
		Connection readCon = null;
		
		try {
			readCon = DBPool.pickup(context);
			
			return loadReminder(objectId, readCon);
		} catch (DBPoolingException exc) {
			throw new OXException(exc);
		} finally {
			DBPool.closeReaderSilent(context,readCon);
		}
	}
	
	public ReminderObject loadReminder( final int objectId, final Connection readCon) throws OXMandatoryFieldException, OXConflictException, OXException, OXObjectNotFoundException {
		try {
			int a = 0;
			
			final PreparedStatement ps = readCon.prepareStatement(sqlLoadById);
			ps.setInt(++a, context.getContextId());
			ps.setInt(++a, objectId);
			
			final ResultSet rs = ps.executeQuery();
			final ReminderObject reminderObj = convertResult2ReminderObject(rs, ps, true);
			
			if (reminderObj != null) {
				return reminderObj;
			} else {
				throw new OXObjectNotFoundException("reminder object not found!");
			}
		} catch (SQLException exc) {
			throw new OXException(exc);
		}
	}
	
	public ReminderObject convertResult2ReminderObject(final ResultSet rs, final PreparedStatement preparedStatement, boolean closeStatements) throws SQLException, OXObjectNotFoundException {
		try {
			if (rs.next()) {
				int a = 0;
				final ReminderObject reminderObj = new ReminderObject();
				reminderObj.setObjectId(rs.getInt(++a));
				reminderObj.setTargetId(rs.getString(++a));
				reminderObj.setModule(rs.getInt(++a));
				reminderObj.setUser(rs.getInt(++a));
				reminderObj.setDate(rs.getTimestamp(++a));
				reminderObj.setRecurrenceAppointment(rs.getBoolean(++a));
				reminderObj.setDescription(rs.getString(++a));
				reminderObj.setFolder(rs.getString(++a));
				reminderObj.setLastModified(new Date(rs.getLong(++a))); // TODO: Fix me
				
				return reminderObj;
			} else {
				throw new OXObjectNotFoundException("reminder object not found");
			}
		} finally {
			if (closeStatements) {
				if (rs != null) {
					rs.close();
				}
				
				if (preparedStatement != null) {
					preparedStatement.close();
				}
			}
		}
	}
	
	public SearchIterator listReminder( final int targetId) throws OXException {
		Connection readCon = null;
		
		try {
			readCon = DBPool.pickup(context);
			
			final PreparedStatement ps = readCon.prepareStatement(sqlListByTargetId);
			ps.setInt(1, context.getContextId());
			ps.setInt(2, targetId);
			
			final ResultSet rs = ps.executeQuery();
			return new ReminderSearchIterator(rs, ps, readCon);
		} catch (Exception exc) {
			throw new OXException(exc);
		}
	}
	
	public SearchIterator listReminder(final int userId, final Date end) throws OXException {
		Connection readCon = null;
		
		try {
			readCon = DBPool.pickup(context);
			
			final PreparedStatement ps = readCon.prepareStatement(sqlRange);
			ps.setInt(1, context.getContextId());
			ps.setInt(2, userId);
			ps.setTimestamp(3, new Timestamp(end.getTime()));
			
			final ResultSet rs = ps.executeQuery();
			return new ReminderSearchIterator(rs, ps, readCon);
		} catch (Exception exc) {
			throw new OXException(exc);
		}
	}
	
	public SearchIterator listModifiedReminder(final int userId, final Date lastModified) throws OXException {
		Connection readCon = null;
		
		try {
			readCon = DBPool.pickup(context);
			
			final PreparedStatement ps = readCon.prepareStatement(sqlModified);
			ps.setInt(1, context.getContextId());
			ps.setInt(2, userId);
			ps.setTimestamp(3, new Timestamp(lastModified.getTime()));
			
			final ResultSet rs = ps.executeQuery();
			return new ReminderSearchIterator(rs, ps, readCon);
		} catch (Exception exc) {
			throw new OXException(exc);
		}
	}
	
	private class ReminderSearchIterator implements SearchIterator {
		
		private ReminderObject next = null;
		
		private ResultSet rs = null;
		
		private PreparedStatement preparedStatement = null;
		
		private Connection readCon;
		
		private ReminderSearchIterator(final ResultSet rs, final PreparedStatement preparedStatement, final Connection readCon) throws SearchIteratorException {
			this.rs = rs;
			this.readCon = readCon;
			this.preparedStatement = preparedStatement;
			try {
				next = convertResult2ReminderObject(rs, preparedStatement, false);
			} catch (OXObjectNotFoundException exc) {
				next = null;
			} catch (SQLException exc) {
				throw new SearchIteratorException(exc);
			}
		}
		
		public boolean hasNext() {
			return next != null;
		}
		
		public Object next() throws SearchIteratorException {
			final ReminderObject reminderObj = next;
			try {
				next = convertResult2ReminderObject(rs, preparedStatement, false);
			} catch (OXObjectNotFoundException exc) {
				next = null;
			} catch (SQLException exc) {
				throw new SearchIteratorException(exc);
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
				
				DBPool.closeReaderSilent(context,readCon);
			} catch (Exception exc) {
				throw new SearchIteratorException(exc);
			}
		}
		
		public int size() {
			throw new UnsupportedOperationException("Method size() not implemented");
		}
		
		public boolean hasSize() {
			return false;
		}
	}
}
