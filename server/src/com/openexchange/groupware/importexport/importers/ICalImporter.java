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

package com.openexchange.groupware.importexport.importers;

import static com.openexchange.java.Autoboxing.I;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectProcedure;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.api.OXPermissionException;
import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.api2.OXException;
import com.openexchange.api2.TasksSQLInterface;
import com.openexchange.data.conversion.ical.ConversionError;
import com.openexchange.data.conversion.ical.ConversionWarning;
import com.openexchange.data.conversion.ical.ICalParser;
import com.openexchange.database.DBPoolingException;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.groupware.calendar.AppointmentSqlFactoryService;
import com.openexchange.groupware.calendar.CalendarCollectionService;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.calendar.CalendarField;
import com.openexchange.groupware.calendar.Constants;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.importexport.AbstractImporter;
import com.openexchange.groupware.importexport.Format;
import com.openexchange.groupware.importexport.ImportExportExceptionCodes;
import com.openexchange.groupware.importexport.ImportResult;
import com.openexchange.groupware.importexport.exceptions.ImportExportException;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.groupware.tasks.TaskField;
import com.openexchange.groupware.tasks.TasksSQLImpl;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.server.ServiceException;
import com.openexchange.server.impl.EffectivePermission;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.TimeZoneUtils;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.versit.converter.OXContainerConverter;

/**
 * Imports ICal files. ICal files can be translated to either tasks or
 * appointments within the OX, so the importer works with both SQL interfaces.
 * 
 * @see OXContainerConverter OXContainerConverter - if you have a problem with
 *      the contend of the parsed ICAL file
 * @see AppointmentSQLInterface AppointmentSQLInterface - if you have a problem
 *      entering the parsed entry as Appointment
 * @see TasksSQLInterface TasksSQLInterface - if you have trouble entering the
 *      parsed entry as Task
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias 'Tierlieb'
 *         Prinz</a> (changes to new interface, bugfixes, maintenance)
 */
public class ICalImporter extends AbstractImporter {
	private static final int APP = 0;
	private static final int TASK = 1;

	private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(ICalImporter.class));

	public boolean canImport(final ServerSession session, final Format format,
			final List<String> folders,
			final Map<String, String[]> optionalParams)
			throws ImportExportException {
		if (!format.equals(Format.ICAL)) {
			return false;
		}
		final OXFolderAccess folderAccess = new OXFolderAccess(
				session.getContext());
		final Iterator<String> iterator = folders.iterator();
		while (iterator.hasNext()) {
			final String folder = iterator.next();

			int folderId = 0;
			try {
				folderId = Integer.parseInt(folder);
			} catch (final NumberFormatException e) {
				throw ImportExportExceptionCodes.NUMBER_FAILED
						.create(e, folder);
			}

			FolderObject fo;
			try {
				fo = folderAccess.getFolderObject(folderId);
			} catch (final OXException e) {
				return false;
			}

			// check format of folder
			final int module = fo.getModule();
			if (module == FolderObject.CALENDAR) {
				if (!UserConfigurationStorage
						.getInstance()
						.getUserConfigurationSafe(session.getUserId(),
								session.getContext()).hasCalendar()) {
					return false;
				}
			} else if (module == FolderObject.TASK) {
				if (!UserConfigurationStorage
						.getInstance()
						.getUserConfigurationSafe(session.getUserId(),
								session.getContext()).hasTask()) {
					return false;
				}
			} else {
				return false;
			}

			// check read access to folder
			EffectivePermission perm;
			try {
				perm = fo.getEffectiveUserPermission(
						session.getUserId(),
						UserConfigurationStorage.getInstance()
								.getUserConfigurationSafe(session.getUserId(),
										session.getContext()));
			} catch (final DBPoolingException e) {
				throw ImportExportExceptionCodes.NO_DATABASE_CONNECTION
						.create(e);
			} catch (final SQLException e) {
				throw ImportExportExceptionCodes.SQL_PROBLEM.create(e,
						e.getMessage());
			}

			if (!perm.canCreateObjects()) {
				return false;
			}
		}
		return true;
	}

	private int[] determineFolders(ServerSession session, List<String> folders,
			Format format) throws ImportExportException {
		int[] result = new int[] { -1, -1 };
		final OXFolderAccess folderAccess = new OXFolderAccess(
				session.getContext());
		final Iterator<String> iterator = folders.iterator();
		while (iterator.hasNext()) {
			final int folderId = Integer.parseInt(iterator.next());
			FolderObject fo;
			try {
				fo = folderAccess.getFolderObject(folderId);
			} catch (final OXException e) {
				throw ImportExportExceptionCodes.LOADING_FOLDER_FAILED.create(
						e, I(folderId));
			}
			if (fo.getModule() == FolderObject.CALENDAR) {
				result[APP] = folderId;
			} else if (fo.getModule() == FolderObject.TASK) {
				result[TASK] = folderId;
			} else {
				throw ImportExportExceptionCodes.CANNOT_IMPORT.create(
						fo.getFolderName(), format);
			}
		}
		return result;
	}

	public List<ImportResult> importData(final ServerSession session,
			final Format format, final InputStream is,
			final List<String> folders,
			final Map<String, String[]> optionalParams)
			throws ImportExportException {
		int[] res = determineFolders(session, folders, format);
		int appointmentFolderId = res[APP];
		int taskFolderId = res[TASK];

		final AppointmentSQLInterface appointmentInterface = retrieveAppointmentInterface(
				appointmentFolderId, session);
		final TasksSQLInterface taskInterface = retrieveTaskInterface(
				taskFolderId, session);
		final ICalParser parser = retrieveIcalParser();
		final Context ctx = session.getContext();
		final TimeZone defaultTz = TimeZoneUtils.getTimeZone(UserStorage
				.getStorageUser(session.getUserId(), ctx).getTimeZone());

		final List<ImportResult> list = new ArrayList<ImportResult>();
		final List<ConversionError> errors = new ArrayList<ConversionError>();
		final List<ConversionWarning> warnings = new ArrayList<ConversionWarning>();

		if (appointmentFolderId != -1) {
			importAppointment(session, is, optionalParams, appointmentFolderId,
					appointmentInterface, parser, ctx, defaultTz, list, errors,
					warnings);
		}
		if (taskFolderId != -1) {
			importTask(is, taskFolderId, taskInterface, parser, ctx, defaultTz,
					list, errors, warnings);
		}
		return list;
	}

	private void importTask(final InputStream is, int taskFolderId,
			final TasksSQLInterface taskInterface, final ICalParser parser,
			final Context ctx, final TimeZone defaultTz,
			final List<ImportResult> list, final List<ConversionError> errors,
			final List<ConversionWarning> warnings)
			throws ImportExportException {
		List<Task> tasks;
		try {
			tasks = parser.parseTasks(is, defaultTz, ctx, errors, warnings);
		} catch (final ConversionError conversionError) {
			throw new ImportExportException(conversionError);
		}
		final TIntObjectHashMap<ConversionError> errorMap = new TIntObjectHashMap<ConversionError>();

		for (final ConversionError error : errors) {
			errorMap.put(error.getIndex(), error);
		}

		final TIntObjectHashMap<List<ConversionWarning>> warningMap = new TIntObjectHashMap<List<ConversionWarning>>();

		for (final ConversionWarning warning : warnings) {
			List<ConversionWarning> warningList = warningMap.get(warning
					.getIndex());
			if (warningList == null) {
				warningList = new LinkedList<ConversionWarning>();
				warningMap.put(warning.getIndex(), warningList);
			}
			warningList.add(warning);
		}

		int index = 0;
		final Iterator<Task> iter = tasks.iterator();
		while (iter.hasNext()) {
			final ImportResult importResult = new ImportResult();
			final ConversionError error = errorMap.get(index);
			if (error != null) {
				errorMap.remove(index);
				importResult.setException(new ImportExportException(error));
			} else {
				// IGNORE WARNINGS. Protocol doesn't allow for warnings.
				// TODO: Verify This
				final Task task = iter.next();
				task.setParentFolderID(taskFolderId);
				try {
					taskInterface.insertTaskObject(task);
					importResult.setObjectId(String.valueOf(task
							.getObjectID()));
					importResult.setDate(task.getLastModified());
					importResult.setFolder(String.valueOf(taskFolderId));
				} catch (final OXException e) {
					LOG.error(e.getMessage(), e);
					importResult.setException(e);
				}

				final List<ConversionWarning> warningList = warningMap
						.get(index);
				if (warningList != null) {
					importResult.addWarnings(warningList);
					importResult
							.setException(ImportExportExceptionCodes.WARNINGS
									.create(I(warningList.size())));
				}
			}
			importResult.setEntryNumber(index);
			list.add(importResult);
			index++;
		}
		if (!errorMap.isEmpty()) {
			errorMap.forEachValue(new TObjectProcedure<ConversionError>() {

				public boolean execute(final ConversionError error) {
					final ImportResult importResult = new ImportResult();
					importResult.setEntryNumber(error.getIndex());
					importResult.setException(new ImportExportException(
							error));
					list.add(importResult);
					return true;
				}
			});
		}
	}

	private void importAppointment(final ServerSession session,
			final InputStream is, final Map<String, String[]> optionalParams,
			int appointmentFolderId,
			final AppointmentSQLInterface appointmentInterface,
			final ICalParser parser, final Context ctx,
			final TimeZone defaultTz, final List<ImportResult> list,
			final List<ConversionError> errors,
			final List<ConversionWarning> warnings)
			throws ImportExportException {
		List<CalendarDataObject> appointments;
		try {
			appointments = parser.parseAppointments(is, defaultTz, ctx,
					errors, warnings);
		} catch (final ConversionError conversionError) {
			throw new ImportExportException(conversionError);
		}
		final TIntObjectHashMap<ConversionError> errorMap = new TIntObjectHashMap<ConversionError>();

		for (final ConversionError error : errors) {
			errorMap.put(error.getIndex(), error);
		}

		sortSeriesMastersFirst(appointments);
		Map<Integer, Integer> pos2Master = handleChangeExceptions(appointments);
		Map<Integer, Integer> master2id = new HashMap<Integer,Integer>();
		
		final TIntObjectHashMap<List<ConversionWarning>> warningMap = new TIntObjectHashMap<List<ConversionWarning>>();

		for (final ConversionWarning warning : warnings) {
			List<ConversionWarning> warningList = warningMap.get(warning
					.getIndex());
			if (warningList == null) {
				warningList = new LinkedList<ConversionWarning>();
				warningMap.put(warning.getIndex(), warningList);
			}
			warningList.add(warning);
		}

		int index = 0;
		final Iterator<CalendarDataObject> iter = appointments.iterator();
		
		boolean suppressNotification = (optionalParams != null && optionalParams
				.containsKey("suppressNotification"));
		while (iter.hasNext()) {
			final ImportResult importResult = new ImportResult();
			final ConversionError error = errorMap.get(index);
			if (error != null) {
				errorMap.remove(index);
				importResult.setException(new ImportExportException(error));
			} else {
				final CalendarDataObject appointmentObj = iter.next();
				appointmentObj.setContext(session.getContext());
				appointmentObj.setParentFolderID(appointmentFolderId);
				appointmentObj.setIgnoreConflicts(true);
				if (suppressNotification) {
					appointmentObj.setNotification(false);
				}
				// Check for possible full-time appointment
				check4FullTime(appointmentObj);
				try {
					boolean isMaster = appointmentObj.containsUid() && !pos2Master.containsKey(index);
					boolean isChange = appointmentObj.containsUid() && pos2Master.containsKey(index);
					Date changeDate = new Date(Long.MAX_VALUE);
					Integer masterID = master2id.get(pos2Master.get(index));
					if(isChange){
						appointmentObj.setRecurrenceID(masterID);
						appointmentObj.removeUid();
						appointmentObj.setRecurrenceDatePosition(calculateRecurrenceDatePosition(appointmentObj.getStartDate()));
					}
					final Appointment[] conflicts;
					if(isChange){
						appointmentObj.setObjectID(masterID);
						conflicts = appointmentInterface.updateAppointmentObject(appointmentObj, appointmentFolderId, changeDate);
					}  else {
						conflicts = appointmentInterface.insertAppointmentObject(appointmentObj);
					}
					if(isMaster)
						master2id.put(index, appointmentObj.getObjectID());
					if (conflicts == null || conflicts.length == 0) {
						importResult.setObjectId(String
								.valueOf(appointmentObj.getObjectID()));
						importResult.setDate(appointmentObj
								.getLastModified());
						importResult.setFolder(String
								.valueOf(appointmentFolderId));
					} else {
						importResult
								.setException(ImportExportExceptionCodes.RESOURCE_HARD_CONFLICT
										.create());
					}
				} catch (final OXException e) {
					LOG.error(e.getMessage(), e);
					importResult.setException(e);
				}
				final List<ConversionWarning> warningList = warningMap
						.get(index);
				if (warningList != null) {
					importResult.addWarnings(warningList);
					importResult
							.setException(ImportExportExceptionCodes.WARNINGS
									.create(I(warningList.size())));
				}
			}
			importResult.setEntryNumber(index);
			list.add(importResult);
			index++;
		}
		if (!errorMap.isEmpty()) {
			errorMap.forEachValue(new TObjectProcedure<ConversionError>() {

				public boolean execute(final ConversionError error) {
					final ImportResult importResult = new ImportResult();
					importResult.setEntryNumber(error.getIndex());
					importResult.setException(new ImportExportException(
							error));
					list.add(importResult);
					return true;
				}
			});
		}
	}

	private Date calculateRecurrenceDatePosition(Date startDate) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTime(startDate);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	private void sortSeriesMastersFirst(List<CalendarDataObject> appointments) {
		Collections.sort(appointments, new Comparator<CalendarDataObject>(){
			public int compare(CalendarDataObject o1, CalendarDataObject o2) {
				if( o1.containsRecurrenceType() && !o2.containsRecurrenceType())
					return -1;
				if( !o1.containsRecurrenceType() && o2.containsRecurrenceType())
					return 1;
				return 0;
			}});
	}
	/**
	 * @return mapping for position of a recurrence in the list to the position of the recurrence master
	 */
	private Map<Integer, Integer> handleChangeExceptions(List<CalendarDataObject> appointments) {
		Map<String, Integer> uid2master = new HashMap<String,Integer>();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		CalendarDataObject app;
			
		//find master
		for(int pos = 0, len = appointments.size(); pos < len; pos++){
			app = appointments.get(pos);
			if(! app.containsUid())
				continue;
			
			String uid = app.getUid();
			if(! uid2master.containsKey(uid))
				uid2master.put(uid, pos);
		}
		
		//references to master
		for(int pos = 0, len = appointments.size(); pos < len; pos++){
			app = appointments.get(pos);
			if(! app.containsUid())
				continue;
			
			String uid = app.getUid();
			Integer masterPos = uid2master.get(uid);
				
			if(pos > masterPos) //only work on change exceptions, not on the series master 
				map.put(pos, uid2master.get(uid));
		}
		
		return map;
	}

	private ICalParser retrieveIcalParser() throws ImportExportException {
		try {
			return ServerServiceRegistry.getInstance().getService(
					ICalParser.class, true);
		} catch (ServiceException e) {
			throw ImportExportExceptionCodes.ICAL_PARSER_SERVICE_MISSING
					.create(e);
		}
	}

	private AppointmentSQLInterface retrieveAppointmentInterface(
			int appointmentFolderId, ServerSession session)
			throws ImportExportException {
		if (appointmentFolderId == -1)
			return null;

		if (!UserConfigurationStorage
				.getInstance()
				.getUserConfigurationSafe(session.getUserId(),
						session.getContext()).hasCalendar())
			throw ImportExportExceptionCodes.CALENDAR_DISABLED
					.create(new OXPermissionException(
							OXPermissionException.Code.NoPermissionForModul,
							"Calendar"));

		return ServerServiceRegistry.getInstance()
				.getService(AppointmentSqlFactoryService.class)
				.createAppointmentSql(session);
	}

	private TasksSQLInterface retrieveTaskInterface(int taskFolderId,
			ServerSession session) throws ImportExportException {
		if (taskFolderId == -1)
			return null;
		if (!UserConfigurationStorage
				.getInstance()
				.getUserConfigurationSafe(session.getUserId(),
						session.getContext()).hasTask())
			throw ImportExportExceptionCodes.TASKS_DISABLED
					.create(new OXPermissionException(
							OXPermissionException.Code.NoPermissionForModul,
							"Task"));

		return new TasksSQLImpl(session);
	}

	/**
	 * Checks if specified appointment lasts exactly one day; if so treat it as
	 * a full-time appointment through setting
	 * {@link CalendarDataObject#setFullTime(boolean)} to <code>true</code>.
	 * <p>
	 * Moreover its start/end date is changed to match the date in UTC time
	 * zone.
	 * 
	 * @param appointmentObj
	 *            The appointment to check
	 */
	private void check4FullTime(final Appointment appointmentObj) {
		final long start = appointmentObj.getStartDate().getTime();
		final long end = appointmentObj.getEndDate().getTime();
		if (Constants.MILLI_DAY == (end - start)) {
			// Appointment exactly lasts one day; assume a full-time appointment
			appointmentObj.setFullTime(true);
			// Adjust start/end to UTC date's zero time; e.g.
			// "13. January 2009 00:00:00 UTC"
			final TimeZone tz = ServerServiceRegistry.getInstance()
					.getService(CalendarCollectionService.class)
					.getTimeZone(appointmentObj.getTimezone());
			long offset = tz.getOffset(start);
			appointmentObj.setStartDate(new Date(start + offset));
			offset = tz.getOffset(end);
			appointmentObj.setEndDate(new Date(end + offset));
		}
	}

	@Override
	protected String getNameForFieldInTruncationError(final int id,
			final OXException oxex) {
		if (oxex.getComponent() == EnumComponent.APPOINTMENT) {
			final CalendarField field = CalendarField
					.getByAppointmentObjectId(id);
			if (field != null) {
				return field.getName();
			}
		} else if (oxex.getComponent() == EnumComponent.TASK) {
			final TaskField field = TaskField.getByTaskID(id);
			if (field != null) {
				return field.getName();
			}
		}
		return String.valueOf(id);

	}

}
