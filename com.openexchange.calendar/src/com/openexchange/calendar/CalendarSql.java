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

package com.openexchange.calendar;

import static com.openexchange.java.Autoboxing.I;
import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.api.OXObjectNotFoundException;
import com.openexchange.api.OXPermissionException;
import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.api2.OXConcurrentModificationException;
import com.openexchange.api2.OXException;
import com.openexchange.calendar.api.CalendarCollection;
import com.openexchange.configuration.ConfigurationException;
import com.openexchange.database.DBPoolingException;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.calendar.CalendarConfig;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.calendar.CalendarFolderObject;
import com.openexchange.groupware.calendar.Constants;
import com.openexchange.groupware.calendar.OXCalendarException;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.data.Check;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.search.AppointmentSearchObject;
import com.openexchange.groupware.search.Order;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.server.impl.DBPool;
import com.openexchange.server.impl.EffectivePermission;
import com.openexchange.session.Session;
import com.openexchange.tools.StringCollection;
import com.openexchange.tools.encoding.Charsets;
import com.openexchange.tools.exceptions.SimpleTruncatedAttribute;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorAdapter;
import com.openexchange.tools.iterator.SearchIteratorException;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.sql.DBUtils;

/**
 * {@link CalendarSql} - The implementation of {@link AppointmentSQLInterface}.
 * 
 * @author <a href="mailto:martin.kauss@open-xchange.org">Martin Kauss</a>
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a> (some refactoring)
 */
public class CalendarSql implements AppointmentSQLInterface {

    public static final String default_class = "com.openexchange.calendar.CalendarMySQL";

    public static final String ERROR_PUSHING_DATABASE = "error pushing readable connection";

    public static final String ERROR_PUSHING_WRITEABLE_CONNECTION = "error pushing writeable connection";

    public static final String DATES_TABLE_NAME = "prg_dates";

    public static final String VIEW_TABLE_NAME = "prg_date_rights";

    public static final String PARTICIPANT_TABLE_NAME = "prg_dates_members";

    private static CalendarSqlImp cimp;

    private final Session session;

    private CalendarCollection recColl;

    private boolean includePrivateAppointments;

    private static final Log LOG = LogFactory.getLog(CalendarSql.class);

    /**
     * Initializes a new {@link CalendarSql}.
     * 
     * @param session The session providing needed user data
     */
    public CalendarSql(final Session session) {
        this.session = session;
        this.recColl = new CalendarCollection();
    }

    public boolean[] hasAppointmentsBetween(final Date d1, final Date d2) throws OXException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        final Context ctx = Tools.getContext(session);
        final User user = Tools.getUser(session, ctx);
        final UserConfiguration userConfiguration = Tools.getUserConfiguration(ctx, session.getUserId());
        Connection readcon = null;
        try {
            readcon = DBPool.pickup(ctx);
            return cimp.getUserActiveAppointmentsRangeSQL(ctx, session.getUserId(), user.getGroups(), userConfiguration, d1, d2, readcon);
        } catch (final OXException e) {
            // Don't mask OX exceptions in a SQL exception.
            throw e;
        } catch(final Exception e) {
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, e);
        } finally {
            if (readcon != null) {
                DBPool.push(ctx, readcon);
            }
        }
    }

    public SearchIterator<Appointment> getAppointmentsBetweenInFolder(final int fid, final int[] cols, final Date start, final Date end, final int orderBy, final String orderDir) throws OXException, SQLException {
        return getAppointmentsBetweenInFolder(fid, cols, start, end, 0, 0, orderBy, orderDir);
    }

    
    public SearchIterator<Appointment> getAppointmentsBetweenInFolder(final int fid, int[] cols, final Date start, final Date end, final int from, final int to, final int orderBy, final String orderDir) throws OXException, SQLException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        Connection readcon = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        boolean close_connection = true;
        final Context ctx = Tools.getContext(session);
        final User user = Tools.getUser(session, ctx);
        final UserConfiguration userConfig = Tools.getUserConfiguration(ctx, session.getUserId());
        try {
            readcon = DBPool.pickup(ctx);
            cols = recColl.checkAndAlterCols(cols);
            final OXFolderAccess ofa = new OXFolderAccess(readcon, ctx);
            final int folderType = ofa.getFolderType(fid, session.getUserId());
            final CalendarOperation co = new CalendarOperation();
            final EffectivePermission oclp = ofa.getFolderPermission(fid, session.getUserId(), userConfig);
            
            mayRead(oclp);
            
            if (folderType == FolderObject.PRIVATE)
                prep = cimp.getPrivateFolderRangeSQL(ctx, session.getUserId(), user.getGroups(), fid, start, end, StringCollection.getSelect(cols, DATES_TABLE_NAME), oclp.canReadAllObjects(), readcon, orderBy, orderDir);
            else if (folderType == FolderObject.PUBLIC)
                prep = cimp.getPublicFolderRangeSQL(ctx, session.getUserId(), user.getGroups(), fid, start, end, StringCollection.getSelect(cols, DATES_TABLE_NAME), oclp.canReadAllObjects(), readcon, orderBy, orderDir);
            else {
                final int shared_folder_owner = ofa.getFolderOwner(fid);
                prep = cimp.getSharedFolderRangeSQL(ctx, session.getUserId(), shared_folder_owner, user.getGroups(), fid, start, end, StringCollection.getSelect(cols, DATES_TABLE_NAME), oclp.canReadAllObjects(), readcon, orderBy, orderDir, doesIncludePrivateAppointments());
            }
                
            rs = cimp.getResultSet(prep);
            co.setRequestedFolder(fid);
            co.setResultSet(rs, prep, cols, cimp, readcon, from, to, session, ctx);
            close_connection = false;
            return new AppointmentIteratorAdapter(new AnonymizingIterator(co, ctx, session.getUserId()));
            
        } catch (final IndexOutOfBoundsException ioobe) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, ioobe, Integer.valueOf(19));
        } catch (final OXPermissionException oxpe) {
            throw oxpe;
        } catch(final OXCalendarException oxc) {
            throw oxc;
        } catch(final OXException oxe) {
            throw oxe;
        } catch(final SQLException sqle) {
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
        } catch (final Exception e) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, Integer.valueOf(20));
        } finally  {
            if (close_connection) {
                recColl.closeResultSet(rs);
                recColl.closePreparedStatement(prep);
            }
            if (readcon != null && close_connection) {
                DBPool.push(ctx, readcon);
            }
        }
    }

    /**
     * @return
     */
    private boolean doesIncludePrivateAppointments() {
        return includePrivateAppointments;
    }

    public SearchIterator<Appointment> getModifiedAppointmentsInFolder(final int fid, final Date start, final Date end, int[] cols, final Date since) throws OXException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        Connection readcon = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        boolean close_connection = true;
        final Context ctx = Tools.getContext(session);
        final User user = Tools.getUser(session, ctx);
        final UserConfiguration userConfig = Tools.getUserConfiguration(ctx, session.getUserId());
        try {
            readcon = DBPool.pickup(ctx);
            cols = recColl.checkAndAlterCols(cols);
            final OXFolderAccess ofa = new OXFolderAccess(readcon, ctx);
            int folderType = ofa.getFolderType(fid, session.getUserId());
            final CalendarOperation co = new CalendarOperation();
            final EffectivePermission oclp = ofa.getFolderPermission(fid, session.getUserId(), userConfig);
            final int shared_folder_owner = ofa.getFolderOwner(fid);
            mayRead(oclp);
            
            if (folderType == FolderObject.PRIVATE)
                prep = cimp.getPrivateFolderModifiedSinceSQL(ctx, session.getUserId(), user.getGroups(), fid, since, StringCollection.getSelect(cols, DATES_TABLE_NAME), oclp.canReadAllObjects(), readcon, start, end);
            else if (folderType == FolderObject.PUBLIC) 
                prep = cimp.getPublicFolderModifiedSinceSQL(ctx, session.getUserId(), user.getGroups(), fid, since, StringCollection.getSelect(cols, DATES_TABLE_NAME), oclp.canReadAllObjects(), readcon, start, end);
            else
                prep = cimp.getSharedFolderModifiedSinceSQL(ctx, session.getUserId(), shared_folder_owner, user.getGroups(), fid, since, StringCollection.getSelect(cols, DATES_TABLE_NAME), oclp.canReadAllObjects(), readcon, start, end, !this.includePrivateAppointments);
            rs = cimp.getResultSet(prep);
            co.setRequestedFolder(fid);
            co.setResultSet(rs, prep, cols, cimp, readcon, 0, 0, session, ctx);
            close_connection = false;
            if(includePrivateAppointments)
                return new AppointmentIteratorAdapter(new AnonymizingIterator(co, ctx, session.getUserId()));
            return new AppointmentIteratorAdapter(new CachedCalendarIterator(co, ctx, session.getUserId()));
        } catch (final IndexOutOfBoundsException ioobe) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, ioobe, I(21));
        } catch (final OXPermissionException oxpe) {
            throw oxpe;
        } catch(final OXCalendarException oxc) {
            throw oxc;
        } catch(final OXException oxe) {
            throw oxe;
        } catch(final SQLException sqle) {
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
        } catch (final Exception e) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, Integer.valueOf(22));
        } finally {
            if (close_connection) {
                recColl.closeResultSet(rs);
                recColl.closePreparedStatement(prep);
            }
            if (readcon != null && close_connection) {
                DBPool.push(ctx, readcon);
            }
        }
    }

    private void mayRead(final EffectivePermission oclp) throws OXCalendarException {
        if (!oclp.canReadAllObjects() && !oclp.canReadOwnObjects()) {
            throw new OXCalendarException(OXCalendarException.Code.NO_PERMISSION, I(oclp.getFuid()));   
        }
    }

    public SearchIterator<Appointment> getModifiedAppointmentsInFolder(final int fid, final int cols[], final Date since) throws OXException {
        return getModifiedAppointmentsInFolder(fid, null, null, cols, since);
    }

    public SearchIterator<Appointment> getDeletedAppointmentsInFolder(final int fid, int cols[], final Date since) throws OXException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        Connection readcon = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        boolean close_connection = true;
        final Context ctx = Tools.getContext(session);
        final UserConfiguration userConfig = Tools.getUserConfiguration(ctx, session.getUserId());
        try {
            readcon = DBPool.pickup(ctx);
            cols = recColl.checkAndAlterCols(cols);
            final OXFolderAccess ofa = new OXFolderAccess(readcon, ctx);
            final EffectivePermission oclp = ofa.getFolderPermission(fid, session.getUserId(), userConfig);
            mayRead(oclp);
            if (ofa.getFolderType(fid, session.getUserId()) == FolderObject.PRIVATE) {
                final CalendarOperation co = new CalendarOperation();
                prep = cimp.getPrivateFolderDeletedSinceSQL(ctx, session.getUserId(), fid, since, StringCollection.getSelect(cols, "del_dates"), readcon);
                rs = cimp.getResultSet(prep);
                co.setRequestedFolder(fid);
                co.setResultSet(rs, prep, cols, cimp, readcon, 0, 0, session, ctx);
                close_connection = false;
                return new AppointmentIteratorAdapter(new CachedCalendarIterator(co, ctx, session.getUserId()));
            } else if (ofa.getFolderType(fid, session.getUserId()) == FolderObject.PUBLIC) {
                final CalendarOperation co = new CalendarOperation();
                prep = cimp.getPublicFolderDeletedSinceSQL(ctx, session.getUserId(), fid, since, StringCollection.getSelect(cols, "del_dates"), readcon);
                rs = cimp.getResultSet(prep);
                co.setRequestedFolder(fid);
                co.setResultSet(rs, prep,cols, cimp, readcon, 0, 0, session, ctx);
                close_connection = false;
                return new AppointmentIteratorAdapter(new CachedCalendarIterator(co, ctx, session.getUserId()));
            } else {
                final CalendarOperation co = new CalendarOperation();
                final int shared_folder_owner = ofa.getFolderOwner(fid);
                prep = cimp.getSharedFolderDeletedSinceSQL(ctx, session.getUserId(), shared_folder_owner, fid, since, StringCollection.getSelect(cols, "del_dates"), readcon);
                rs = cimp.getResultSet(prep);
                co.setRequestedFolder(fid);
                co.setResultSet(rs, prep, cols, cimp, readcon, 0, 0, session, ctx);
                close_connection = false;
                return new AppointmentIteratorAdapter(new CachedCalendarIterator(co, ctx, session.getUserId()));
            }
        } catch (final IndexOutOfBoundsException ioobe) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, ioobe, Integer.valueOf(23));
        } catch (final OXPermissionException oxpe) {
            throw oxpe;
        } catch(final OXCalendarException oxc) {
            throw oxc;
        } catch(final OXException oxe) {
            throw oxe;
        } catch(final SQLException sqle) {
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
        } catch (final Exception e) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, Integer.valueOf(24));
        } finally {
            if (close_connection) {
                recColl.closeResultSet(rs);
                recColl.closePreparedStatement(prep);
            }
            if (readcon != null && close_connection) {
                DBPool.push(ctx, readcon);
            }
        }
    }

    public CalendarDataObject getObjectById(final int oid, final int inFolder) throws OXException, SQLException, OXObjectNotFoundException, OXPermissionException {
        return getObjectById(oid, inFolder, null);
    }

    /**
     * Gets the appointment denoted by specified object ID in given folder
     * 
     * @param oid The object ID
     * @param inFolder The folder ID
     * @param readcon A connection with read capability (leave to <code>null</code> to fetch from pool)
     * @return The appointment object
     * @throws OXException
     * @throws OXObjectNotFoundException
     * @throws OXPermissionException
     */
    public CalendarDataObject getObjectById(final int oid, final int inFolder, final Connection readcon) throws OXException, OXObjectNotFoundException, OXPermissionException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        Connection rcon = readcon;
        boolean closeRead = false;
        PreparedStatement prep = null;
        ResultSet rs = null;
        final Context ctx = Tools.getContext(session);
        try {
            if (rcon == null) {
                rcon = DBPool.pickup(ctx);
                closeRead = true;
            }
            final CalendarOperation co = new CalendarOperation();
            prep = cimp.getPreparedStatement(rcon, cimp.loadAppointment(oid, ctx));
            rs = cimp.getResultSet(prep);
            final CalendarDataObject cdao = co.loadAppointment(rs, oid, inFolder, cimp, rcon, session, ctx, CalendarOperation.READ, inFolder);
            recColl.safelySetStartAndEndDateForRecurringAppointment(cdao);
            return cdao;
        } catch (final OXPermissionException oxpe) {
            throw oxpe;
        } catch(final OXObjectNotFoundException oxonfe) {
            throw oxonfe;
        } catch(final OXCalendarException oxc) {
            throw oxc;
        } catch(final OXException oxe) {
            throw oxe;
        } catch(final SQLException sqle) {
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
        } catch(final DBPoolingException dbpe) {
            throw new OXException(dbpe);
        } finally {
            recColl.closeResultSet(rs);
            recColl.closePreparedStatement(prep);
            if (closeRead && rcon != null) {
                DBPool.push(ctx, rcon);
            }
        }
    }

    public CalendarDataObject[] insertAppointmentObject(final CalendarDataObject cdao) throws OXException, OXPermissionException {
        RecurrenceChecker.check(cdao);
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        Connection writecon = null;
        final Context ctx = Tools.getContext(session);
        final User user = Tools.getUser(session, ctx);
        final UserConfiguration userConfig = Tools.getUserConfiguration(ctx, session.getUserId());
        try {
            final CalendarOperation co = new CalendarOperation();
            if (cdao.containsRecurrenceType())
                recColl.checkRecurring(cdao);
            if (co.prepareUpdateAction(cdao, null, session.getUserId(), cdao.getParentFolderID(), user.getTimeZone())) {
                try {
                    final OXFolderAccess ofa = new OXFolderAccess(ctx);
                    final EffectivePermission oclp = ofa.getFolderPermission(cdao.getEffectiveFolderId(), session.getUserId(), userConfig);
                    if (oclp.canCreateObjects()) {
                        recColl.checkForInvalidCharacters(cdao);
                        cdao.setActionFolder(cdao.getParentFolderID());
                        writecon = DBPool.pickupWriteable(ctx);
                        writecon.setAutoCommit(false);
                        final ConflictHandler ch = new ConflictHandler(cdao, null, session, true);
                        final CalendarDataObject conflicts[] = ch.getConflicts();
                        if (conflicts.length == 0) {                            
                            return cimp.insertAppointment(cdao, writecon, session);
                        }
                        return conflicts;
                    }
                    throw new OXPermissionException(new OXCalendarException(OXCalendarException.Code.LOAD_PERMISSION_EXCEPTION_6));
                } catch(final DataTruncation dt) {
                    final String fields[] = DBUtils.parseTruncatedFields(dt);
                    final int fid[] = new int[fields.length];
                    final OXCalendarException oxe = new OXCalendarException(OXCalendarException.Code.TRUNCATED_SQL_ERROR);
                    int id = -1;
                    for (int a = 0; a < fid.length; a++) {
                        id = recColl.getFieldId(fields[a]);
                        final String value = recColl.getString(cdao, id);
                        if(value == null) {
                            oxe.addTruncatedId(id);
                        } else {
                            final int valueLength = Charsets.getBytes(value, Charsets.UTF_8).length;
                            final int maxLength = DBUtils.getColumnSize(writecon, "prg_dates", fields[a]);
                            oxe.addProblematic(new SimpleTruncatedAttribute(id, maxLength, valueLength));
                        }
                    }
                    throw oxe;
                } catch(final SQLException sqle) {
                    try {
                        if (!writecon.getAutoCommit()) {
                            writecon.rollback();
                        }
                    } catch(final SQLException rb) {
                        LOG.error("Rollback failed: " + rb.getMessage(), rb);
                    }
                    throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
                } finally {
                    if (writecon != null) {
                        writecon.setAutoCommit(true);
                    }
                }
            }
            throw new OXCalendarException(OXCalendarException.Code.INSERT_WITH_OBJECT_ID);
        } catch(final DataTruncation dt) {
            final String fields[] = DBUtils.parseTruncatedFields(dt);
            final int fid[] = new int[fields.length];
            final OXException oxe = new OXCalendarException(OXCalendarException.Code.TRUNCATED_SQL_ERROR, dt, new Object[0]);
            int id = -1;
            for (int a = 0; a < fid.length; a++) {
                id = recColl.getFieldId(fields[a]);
                final String value = recColl.getString(cdao, id);
                if(value == null) {
                    oxe.addTruncatedId(id);
                } else {
                    final int valueLength = Charsets.getBytes(value, Charsets.UTF_8).length;
                    int maxLength = 0;
                    try {
                        maxLength = DBUtils.getColumnSize(writecon, "prg_dates", fields[a]);
                        oxe.addProblematic(new SimpleTruncatedAttribute(id, maxLength, valueLength));
                    } catch (final SQLException e) {
                        LOG.error(e.getMessage(), e);
                        oxe.addTruncatedId(id);
                    }
                }
            }
            throw oxe;
        } catch (final SQLException sqle) {
            final OXCalendarException exception = new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
            LOG.error("Additioal info for: "+exception.getExceptionID()+": "+sqle.getMessage(), sqle);
            throw exception;
        } catch(final DBPoolingException dbpe) {
            throw new OXException(dbpe);
        } catch (final OXPermissionException oxpe) {
            throw oxpe;
        } catch(final OXCalendarException oxc) {
            throw oxc;
        } catch(final OXException oxe) {
            throw oxe;
        } catch(final Exception e) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, Integer.valueOf(25));
        } finally {
            if (writecon != null) {
                DBPool.pushWrite(ctx, writecon);
            }
        }
    }

    public CalendarDataObject[] updateAppointmentObject(final CalendarDataObject cdao, final int inFolder, final Date clientLastModified) throws OXException {
        RecurrenceChecker.check(cdao);
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        Connection writecon = null;
        final Context ctx = Tools.getContext(session);
        final User user = Tools.getUser(session, ctx);
        try {
            writecon = DBPool.pickupWriteable(ctx);
            
            final CalendarOperation co = new CalendarOperation();
            final CalendarDataObject edao = cimp.loadObjectForUpdate(cdao, session, ctx, inFolder, writecon);
            if (co.prepareUpdateAction(cdao, edao, session.getUserId(), inFolder, user.getTimeZone())) {
                // Insert-through-update detected
                throw new OXCalendarException(OXCalendarException.Code.UPDATE_WITHOUT_OBJECT_ID);
            }
            recColl.checkForInvalidCharacters(cdao);
            final CalendarDataObject[] conflicts;
            {
                final CalendarDataObject conflict_dao = recColl.fillFieldsForConflictQuery(cdao, edao, false);
                final ConflictHandler ch = new ConflictHandler(conflict_dao, edao, session, false);
                conflicts = ch.getConflicts();
            }
            if (conflicts.length == 0) {
                // Check user participants completeness
                if (cdao.containsUserParticipants()) {
                    final UserParticipant[] edaoUsers = edao.getUsers();
                    final List<UserParticipant> origUsers = Arrays.asList(edaoUsers);

                    for (final UserParticipant cur : cdao.getUsers()) {
                        if (cur.containsAlarm() && cur.containsConfirm()) {
                            continue;
                        }
                        
                        // Get corresponding user from edao
                        final int index = origUsers.indexOf(cur);
                        if (index != -1) {
                            final UserParticipant origUser = origUsers.get(index);
                            if (!cur.containsConfirm()) {
                                cur.setConfirm(origUser.getConfirm());
                            }
                            if (!cur.containsAlarm()) {
                                cur.setAlarmMinutes(origUser.getAlarmMinutes());
                            }
                        }
                    }
                }
                
                try {
                    writecon.setAutoCommit(false);
                    if (cdao.containsParentFolderID()) {
                        cdao.setActionFolder(cdao.getParentFolderID());
                    } else {
                        cdao.setActionFolder(inFolder);
                    }
                    return cimp.updateAppointment(cdao, edao, writecon, session, ctx, inFolder, clientLastModified);
                } catch(final DataTruncation dt) {
                    final String fields[] = DBUtils.parseTruncatedFields(dt);
                    final int fid[] = new int[fields.length];
                    final OXException oxe = new OXCalendarException(OXCalendarException.Code.TRUNCATED_SQL_ERROR, dt, new Object[0]);
                    int id = -1;
                    for (int a = 0; a < fid.length; a++) {
                        id = recColl.getFieldId(fields[a]);
                        final String value = recColl.getString(cdao, id);
                        if(value == null) {
                            oxe.addTruncatedId(id);
                        } else {
                            final int valueLength = Charsets.getBytes(value, Charsets.UTF_8).length;
                            final int maxLength = DBUtils.getColumnSize(writecon, "prg_dates", fields[a]);
                            oxe.addProblematic(new SimpleTruncatedAttribute(id, maxLength, valueLength));
                        }
                    }
                    throw oxe;
                } catch(final SQLException sqle) {
                    try {
                        if (writecon != null) {
                            writecon.rollback();
                        }
                    } catch(final SQLException rb) {
                        LOG.error("Rollback failed: " + rb.getMessage(), rb);
                    }
                    throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
                } finally {
                    if (writecon != null) {
                        writecon.setAutoCommit(true);
                    }
                }
            }
            return conflicts;
        } catch(final DataTruncation dt) {
            final String fields[] = DBUtils.parseTruncatedFields(dt);
            final int fid[] = new int[fields.length];
            final OXException oxe = new OXCalendarException(OXCalendarException.Code.TRUNCATED_SQL_ERROR, dt, new Object[0]);
            int id = -1;
            for (int a = 0; a < fid.length; a++) {
                id = recColl.getFieldId(fields[a]);
                final String value = recColl.getString(cdao, id);
                if(value == null) {
                    oxe.addTruncatedId(id);
                } else {
                    final int valueLength = Charsets.getBytes(value, Charsets.UTF_8).length;
                    int maxLength = 0;
                    try {
                        maxLength = DBUtils.getColumnSize(writecon, "prg_dates", fields[a]);
                        oxe.addProblematic(new SimpleTruncatedAttribute(id, maxLength, valueLength));
                    } catch (final SQLException e) {
                        LOG.error(e.getMessage(), e);
                        oxe.addTruncatedId(id);
                    }

                }
            }
            throw oxe;
        } catch(final SQLException sqle) {
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
        } catch(final DBPoolingException dbpe) {
            throw new OXException(dbpe);
        } catch(final OXException oxe) {
            throw oxe;
        } catch (final AbstractOXException e) {
            throw new OXException(e);
        } catch (final Exception e) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, Integer.valueOf(26));
        } finally {
            if (writecon != null) {
                DBPool.pushWrite(ctx, writecon);
            }
        }
    }

    public void deleteAppointmentObject(final CalendarDataObject cdao, final int inFolder, final Date clientLastModified) throws OXException, SQLException, OXPermissionException, OXConcurrentModificationException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        final Context ctx = Tools.getContext(session);
        final Connection writecon;
        try {
            writecon = DBPool.pickupWriteable(ctx);
        } catch (final DBPoolingException e) {
            throw new OXCalendarException(e);
        }
        try  {
            writecon.setAutoCommit(false);
            cimp.deleteAppointment(session.getUserId(), cdao, writecon, session, ctx, inFolder, clientLastModified);
            writecon.commit();
        } catch(final OXConcurrentModificationException oxcme) {
            try {
                writecon.rollback();
            } catch(final SQLException rb) {
                LOG.error("Rollback failed: " + rb.getMessage(), rb);
            }
            throw oxcme;
        } catch(final OXPermissionException oxpe) {
            try {
                writecon.rollback();
            } catch(final SQLException rb) {
                LOG.error("Rollback failed: " + rb.getMessage(), rb);
            }
            throw oxpe;
        } catch(final OXObjectNotFoundException oxonfe) {
            try {
                writecon.rollback();
            } catch(final SQLException rb) {
                LOG.error("Rollback failed: " + rb.getMessage(), rb);
            }
            throw oxonfe;
        } catch(final OXCalendarException oxc) {
            try {
                writecon.rollback();
            } catch(final SQLException rb) {
                LOG.error("Rollback failed: " + rb.getMessage(), rb);
            }
            throw oxc;
        } catch(final OXException oxe) {
            try {
                writecon.rollback();
            } catch(final SQLException rb) {
                LOG.error("Rollback failed: " + rb.getMessage(), rb);
            }
            throw oxe;
        } catch(final SQLException e) {
            try {
                writecon.rollback();
            } catch(final SQLException rb) {
                LOG.error("Rollback failed: " + rb.getMessage(), rb);
            }
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, e, Integer.valueOf(28));
        } finally {
            try {
                writecon.setAutoCommit(true);
            } catch(final SQLException ac) {
                LOG.error(ac.getMessage(), ac);
            }
            DBPool.pushWrite(ctx, writecon);
        }
    }
    
    public void deleteAppointmentsInFolder(final int fid) throws OXException, SQLException, OXPermissionException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        final Context ctx = Tools.getContext(session);
        Connection writecon = null;
        try {
            writecon = DBPool.pickupWriteable(ctx);
            deleteAppointmentsInFolder(fid, writecon);
        } catch (DBPoolingException e) {
            LOG.error("Error while getting database connection", e);
            throw new OXException(e);
        } finally {
            if (writecon != null) {
                DBPool.pushWrite(ctx, writecon);
            }
        }
    }

    public void deleteAppointmentsInFolder(final int fid, final Connection writeCon) throws OXException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        PreparedStatement prep = null;
        ResultSet rs = null;
        final Context ctx = Tools.getContext(session);
        try  {
            try {
                final OXFolderAccess ofa = new OXFolderAccess(writeCon, ctx);
                if (ofa.getFolderType(fid, session.getUserId()) == FolderObject.PRIVATE) {
                    prep = cimp.getPrivateFolderObjects(fid, ctx, writeCon);
                    rs = cimp.getResultSet(prep);
                    cimp.deleteAppointmentsInFolder(session, ctx, rs, writeCon, writeCon, FolderObject.PRIVATE, fid);
                } else if (ofa.getFolderType(fid, session.getUserId()) == FolderObject.PUBLIC) {
                    prep = cimp.getPublicFolderObjects(fid, ctx, writeCon);
                    rs = cimp.getResultSet(prep);
                    cimp.deleteAppointmentsInFolder(session, ctx, rs, writeCon, writeCon, FolderObject.PUBLIC, fid);
                } else {
                    throw new OXCalendarException(OXCalendarException.Code.FOLDER_DELETE_INVALID_REQUEST);
                }
            } catch(final SQLException sqle) {
                throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
            }
        } catch(final OXCalendarException oxc) {
            throw oxc;
        } catch(final OXPermissionException oxpe) {
            throw oxpe;
        } catch(final OXException oxe) {
            throw oxe;
        } catch(final Exception e) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, Integer.valueOf(29));
        } finally {
            recColl.closeResultSet(rs);
            recColl.closePreparedStatement(prep);
        }
    }

    public boolean checkIfFolderContainsForeignObjects(final int uid, final int fid) throws OXException, SQLException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        Connection readcon = null;
        final Context ctx = Tools.getContext(session);
        try {
            readcon = DBPool.pickup(ctx);
            final OXFolderAccess ofa = new OXFolderAccess(readcon, ctx);
            if (ofa.getFolderType(fid, session.getUserId()) == FolderObject.PRIVATE) {
                return cimp.checkIfFolderContainsForeignObjects(uid, fid, ctx, readcon, FolderObject.PRIVATE);
            } else if (ofa.getFolderType(fid, session.getUserId()) == FolderObject.PUBLIC) {
                return cimp.checkIfFolderContainsForeignObjects(uid, fid, ctx, readcon, FolderObject.PUBLIC);
            } else {
                throw new OXCalendarException(OXCalendarException.Code.FOLDER_FOREIGN_INVALID_REQUEST);
            }
        } catch(final OXCalendarException oxc) {
            throw oxc;
        } catch(final SQLException sqle) {
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
        } catch(final DBPoolingException dbpe) {
            throw new OXException(dbpe);
        } catch(final OXException oxe) {
            throw oxe;
        } catch(final Exception e) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, Integer.valueOf(30));
        } finally {
            if (readcon != null) {
                DBPool.push(ctx, readcon);
            }
        }
    }

    public boolean checkIfFolderContainsForeignObjects(final int uid, final int fid, final Connection readCon) throws OXException,
            SQLException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        final Context ctx = Tools.getContext(session);
        try {
            final OXFolderAccess ofa = new OXFolderAccess(readCon, ctx);
            if (ofa.getFolderType(fid, session.getUserId()) == FolderObject.PRIVATE) {
                return cimp.checkIfFolderContainsForeignObjects(uid, fid, ctx, readCon, FolderObject.PRIVATE);
            } else if (ofa.getFolderType(fid, session.getUserId()) == FolderObject.PUBLIC) {
                return cimp.checkIfFolderContainsForeignObjects(uid, fid, ctx, readCon, FolderObject.PUBLIC);
            } else {
                throw new OXCalendarException(OXCalendarException.Code.FOLDER_FOREIGN_INVALID_REQUEST);
            }
        } catch (final OXCalendarException oxc) {
            throw oxc;
        } catch (final SQLException sqle) {
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
        } catch (final OXException oxe) {
            throw oxe;
        } catch (final Exception e) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, Integer.valueOf(30));
        }
    }

    public boolean isFolderEmpty(final int uid, final int fid) throws OXException, SQLException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        Connection readcon = null;
        final Context ctx = Tools.getContext(session);
        try {
            readcon = DBPool.pickup(ctx);
            final OXFolderAccess ofa = new OXFolderAccess(readcon, ctx);
            if (ofa.getFolderType(fid, session.getUserId()) == FolderObject.PRIVATE) {
                return cimp.checkIfFolderIsEmpty(uid, fid, ctx, readcon, FolderObject.PRIVATE);
            } else if (ofa.getFolderType(fid, session.getUserId()) == FolderObject.PUBLIC) { 
                return cimp.checkIfFolderIsEmpty(uid, fid, ctx, readcon, FolderObject.PUBLIC);
            } else {
                throw new OXCalendarException(OXCalendarException.Code.FOLDER_IS_EMPTY_INVALID_REQUEST);
            }
        } catch(final OXCalendarException oxc) {
            throw oxc;
        } catch(final SQLException sqle) {
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
        } catch(final DBPoolingException dbpe) {
            throw new OXException(dbpe);
        } catch(final OXException oxe) {
            throw oxe;
        } catch(final Exception e) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, Integer.valueOf(31));
        } finally {
            if (readcon != null) {
                DBPool.push(ctx, readcon);
            }
        }
    }

    public boolean isFolderEmpty(final int uid, final int fid, final Connection readCon) throws OXException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        final Context ctx = Tools.getContext(session);
        try {
            final OXFolderAccess ofa = new OXFolderAccess(readCon, ctx);
            if (ofa.getFolderType(fid, session.getUserId()) == FolderObject.PRIVATE) {
                return cimp.checkIfFolderIsEmpty(uid, fid, ctx, readCon, FolderObject.PRIVATE);
            } else if (ofa.getFolderType(fid, session.getUserId()) == FolderObject.PUBLIC) {
                return cimp.checkIfFolderIsEmpty(uid, fid, ctx, readCon, FolderObject.PUBLIC);
            } else {
                throw new OXCalendarException(OXCalendarException.Code.FOLDER_IS_EMPTY_INVALID_REQUEST);
            }
        } catch (final OXCalendarException oxc) {
            throw oxc;
        } catch (final SQLException sqle) {
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
        } catch (final OXException oxe) {
            throw oxe;
        } catch (final Exception e) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, Integer.valueOf(31));
        }
    }

    /* (non-Javadoc)
     * @see com.openexchange.api2.AppointmentSQLInterface#setUserConfirmation(int, int, int, java.lang.String)
     */
    public Date setUserConfirmation(final int oid, int folderId, final int uid, final int confirm, final String confirm_message) throws OXException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        final Context ctx = Tools.getContext(session);
        if (confirm_message != null) {
            String error = null;
            error = Check.containsInvalidChars(confirm_message);
            if (error != null) {
                throw new OXCalendarException(OXCalendarException.Code.INVALID_CHARACTER, "Confirm Message", error);
            }
        }
        return cimp.setUserConfirmation(oid, folderId, uid, confirm, confirm_message, session, ctx);
    }
    
    public Date setExternalConfirmation(int oid, int folderId, String mail, int confirm, String message) throws OXException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        final Context ctx = Tools.getContext(session);
        if (message != null) {
            String error = null;
            error = Check.containsInvalidChars(message);
            if (error != null) {
                throw new OXCalendarException(OXCalendarException.Code.INVALID_CHARACTER, "Confirm Message", error);
            }
        }
        return cimp.setExternalConfirmation(oid, folderId, mail, confirm, message, session, ctx);
    }

    public SearchIterator<Appointment> getObjectsById(final int[][] oids, int[] cols) throws OXException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        if (oids.length > 0) {
            Connection readcon = null;
            PreparedStatement prep = null;
            ResultSet rs = null;
            boolean close_connection = true;
            final Context ctx = Tools.getContext(session);
            try {
                readcon = DBPool.pickup(ctx);
                cols = recColl.checkAndAlterCols(cols);
                final CalendarOperation co = new CalendarOperation();
                prep = cimp.getPreparedStatement(readcon, cimp.getObjectsByidSQL(oids, session.getContextId(), StringCollection.getSelect(cols, DATES_TABLE_NAME)));
                rs = cimp.getResultSet(prep);
                co.setOIDS(true, oids);
                co.setResultSet(rs, prep, cols, cimp, readcon, 0, 0, session, ctx);
                close_connection = false;
                return new AppointmentIteratorAdapter(new AnonymizingIterator(co, ctx, session.getUserId(), oids));
            } catch(final SQLException sqle) {
                throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
            } catch(final DBPoolingException dbpe) {
                throw new OXException(dbpe);
            } catch(final OXObjectNotFoundException oxonfe) {
                throw oxonfe;
            } catch(final OXCalendarException oxc) {
                throw oxc;
            } catch(final OXException oxe) {
                throw oxe;
            } catch(final Exception e) {
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, Integer.valueOf(32));
            } finally {
                if (readcon != null && close_connection) {
                    recColl.closeResultSet(rs);
                    recColl.closePreparedStatement(prep);
                    DBPool.push(ctx, readcon);
                }
            }
        }
        return SearchIteratorAdapter.createEmptyIterator();
    }

    public SearchIterator<Appointment> getAppointmentsByExtendedSearch(final AppointmentSearchObject searchobject, final int orderBy, final Order orderDir, final int cols[]) throws OXException, SQLException {
        return getAppointmentsByExtendedSearch(searchobject, orderBy, orderDir, cols, 0, 0);
    }
    
    public SearchIterator<Appointment> searchAppointments(final AppointmentSearchObject searchObj, final int orderBy, final Order orderDir, int[] cols) throws OXException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        
        final Context ctx = Tools.getContext(session);
        final User user = Tools.getUser(session, ctx);
        final UserConfiguration userConfig = Tools.getUserConfiguration(ctx, session.getUserId());
        cols = recColl.checkAndAlterCols(cols);
        
        Connection readcon = null;
        try {
            readcon = DBPool.pickup(ctx);
        } catch (DBPoolingException e) {
            throw new OXException(e);
        }
        
        boolean closeCon = true;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            final OXFolderAccess folderAccess = new OXFolderAccess(readcon, ctx);   
            final CalendarOperation co = new CalendarOperation();
            
            CalendarFolderObject cfo = null;
            if (searchObj.hasFolders()) {
                final int folderId = searchObj.getFolders()[0];
                final EffectivePermission folderPermission = folderAccess.getFolderPermission(folderId, user.getId(), userConfig);

                if (folderPermission.isFolderVisible() && (folderPermission.canReadAllObjects() || folderPermission.canReadOwnObjects())) {
                    co.setRequestedFolder(folderId);
                } else {
                    throw new OXPermissionException(new OXCalendarException(OXCalendarException.Code.NO_PERMISSIONS_TO_READ));
                }
            } else {
                // Missing folder attribute indicates a search over all calendar folders the user can see,
                // so create a list with all folders in which the user is allowed to see appointments                
                try {
                    cfo = recColl.getAllVisibleAndReadableFolderObject(user.getId(), user.getGroups(), ctx, userConfig, readcon);
                } catch (SearchIteratorException e) {
                    throw new OXException(e);
                } catch (DBPoolingException e) {
                    throw new OXException(e);
                } catch (SQLException e) {
                    throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, e);
                }
                
//                final int ara[] = new int[1];
//                ara[0] = Appointment.PARTICIPANTS;
//                cols = recColl.enhanceCols(cols, ara, 1);
            }
            
            final StringBuilder columnBuilder = new StringBuilder();
            boolean first = true;
            for (int i = 0; i < cols.length; i++) {
                final String temp = recColl.getFieldName(cols[i]);
                
                if (temp != null) {
                    if (first) {
                        columnBuilder.append(temp);
                        first = false;
                    } else {
                        columnBuilder.append(",");
                        columnBuilder.append(temp);
                    }
                }
            }
            
            stmt = cimp.getSearchStatement(user.getId(), searchObj, cfo, folderAccess, columnBuilder.toString(), orderBy, orderDir, ctx, readcon);
            rs = cimp.getResultSet(stmt);
            co.setResultSet(rs, stmt, cols, cimp, readcon, 0, 0, session, ctx);       
            
            // Don't close connection, it's used within the SearchIterator
            closeCon = false;
            
            return new AppointmentIteratorAdapter(new CachedCalendarIterator(cfo, co, ctx, session.getUserId()));
        } catch (SQLException e) {
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, e);
        } catch (AbstractOXException e) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, Integer.valueOf(1));
        } finally {
            if (stmt != null) {
                recColl.closeResultSet(rs);
                recColl.closePreparedStatement(stmt);
            }
            
            if (closeCon && readcon != null) {
                DBPool.push(ctx, readcon);
            }
        }
    }

    private SearchIterator<Appointment> getAppointmentsByExtendedSearch(final AppointmentSearchObject searchobject, final int orderBy, final Order orderDir, int cols[], final int from, final int to) throws OXException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        Search.checkPatternLength(searchobject);
        Connection readcon = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        boolean close_connection = true;
        final Context ctx = Tools.getContext(session);
        final User user = Tools.getUser(session, ctx);
        final UserConfiguration userConfig = Tools.getUserConfiguration(ctx, session.getUserId());
        try {
            final CalendarOperation co = new CalendarOperation();
            if (searchobject.getFolder() > 0) {
                co.setRequestedFolder(searchobject.getFolder());
            } else {
                final int ara[] = new int[1];
                ara[0] = Appointment.PARTICIPANTS;
                cols = recColl.enhanceCols(cols, ara, 1);
            }
            cols = recColl.checkAndAlterCols(cols);
            CalendarFolderObject cfo = null;
            try {
                cfo = recColl.getAllVisibleAndReadableFolderObject(session.getUserId(), user.getGroups(), ctx, userConfig);
            } catch (final DBPoolingException dbpe) {
                throw new OXException(dbpe);
            } catch (final SearchIteratorException sie) {
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, sie, Integer.valueOf(1));
            }
            readcon = DBPool.pickup(ctx);
            final int userId;
            final int[] groups;
            final UserConfiguration uc;
            final OXFolderAccess folderAccess = new OXFolderAccess(readcon, ctx);
            boolean isShared = false;
            if (isShared = (searchobject.getFolder() > 0 && folderAccess.isFolderShared(searchobject.getFolder(), session.getUserId()))) {
                userId = folderAccess.getFolderOwner(searchobject.getFolder());
                groups = UserStorage.getStorageUser(userId, ctx).getGroups();
                uc = UserConfigurationStorage.getInstance().getUserConfiguration(userId, groups, ctx);
            } else {
                userId = session.getUserId();
                groups = user.getGroups();
                uc = userConfig;
            }
            prep = cimp.getSearchQuery(StringCollection.getSelect(cols, DATES_TABLE_NAME), userId, groups, uc, orderBy, orderDir, searchobject, ctx, readcon, cfo, isShared);
            rs = cimp.getResultSet(prep);
            co.setResultSet(rs, prep, cols, cimp, readcon, 0, 0, session, ctx);
            close_connection = false;
            return new AppointmentIteratorAdapter(new CachedCalendarIterator(co, ctx, session.getUserId()));
        } catch(final SQLException sqle) {
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
        } catch(final DBPoolingException dbpe) {
            throw new OXException(dbpe);
        } catch(final OXObjectNotFoundException oxonfe) {
            throw oxonfe;
        } catch(final OXCalendarException oxc) {
            throw oxc;
        } catch(final OXException oxe) {
            throw oxe;
        } catch(final Exception e) {
            LOG.error(e.getMessage(), e); // Unfortunately the nested exception looses its stack trace.
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, I(33));
        } finally {
            if (close_connection) {
                recColl.closeResultSet(rs);
                recColl.closePreparedStatement(prep);
            }
            if (readcon != null && close_connection) {
                DBPool.push(ctx, readcon);
            }
        }
    }


    public final long attachmentAction(int folderId, final int oid, final int uid, Session session, final Context c, final int numberOfAttachments) throws OXException {
        return cimp.attachmentAction(folderId, oid, uid, session, c, numberOfAttachments);
    }

    public SearchIterator<Appointment> getFreeBusyInformation(final int uid, final int type, final Date start, final Date end) throws OXException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        final Context ctx = Tools.getContext(session);
        final User user = Tools.getUser(session, ctx);
        Connection readcon = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        boolean close_connection = true;
        final UserConfiguration userConfig = Tools.getUserConfiguration(ctx, session.getUserId());
        final CalendarSqlImp calendarsqlimp = CalendarSql.getCalendarSqlImplementation();
        PreparedStatement private_folder_information = null;
        try {
            if (!userConfig.hasFreeBusy()) {
                return SearchIteratorAdapter.createEmptyIterator();
            }
            readcon = DBPool.pickup(ctx);
            switch(type) {
                case Participant.USER:
                    private_folder_information = calendarsqlimp.getAllPrivateAppointmentAndFolderIdsForUser(ctx, user.getId(), readcon);
                    prep = cimp.getFreeBusy(uid, ctx, start, end, readcon);
                    break;
                case Participant.RESOURCE:
                    final long whole_day_start = recColl.getUserTimeUTCDate(start, user.getTimeZone());
                    long whole_day_end = recColl.getUserTimeUTCDate(end, user.getTimeZone());
                    if (whole_day_end <= whole_day_start) {
                        whole_day_end = whole_day_start+Constants.MILLI_DAY;
                    }
                    private_folder_information = calendarsqlimp.getResourceConflictsPrivateFolderInformation(ctx, start, end, new Date(whole_day_start), new Date(whole_day_end), readcon, wrapParenthesis(uid));
                    prep = cimp.getResourceFreeBusy(uid, ctx, start, end, readcon);
                    break;
                default:
                    throw new OXCalendarException(OXCalendarException.Code.FREE_BUSY_UNSUPPOTED_TYPE, Integer.valueOf(type));
            }
            rs = cimp.getResultSet(prep);
            //final SearchIterator si = new FreeBusyResults(rs, prep, ctx, readcon, start.getTime(), end.getTime());
            final SearchIterator si = new FreeBusyResults(rs, prep, ctx, session.getUserId(), user.getGroups(), userConfig, readcon, true, new Participant[0], private_folder_information, calendarsqlimp, start.getTime(), end.getTime());
            close_connection = false;
            return si;
        } catch(final SQLException sqle) {
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
        } catch(final DBPoolingException dbpe) {
            throw new OXException(dbpe);
        } catch(final OXObjectNotFoundException oxonfe) {
            throw oxonfe;
        } catch(final OXCalendarException oxc) {
            throw oxc;
        } catch(final OXException oxe) {
            throw oxe;
        } catch(final Exception e) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, Integer.valueOf(34));
        } finally {
            if (close_connection) {
                recColl.closeResultSet(rs);
                recColl.closePreparedStatement(prep);
            }
            if (readcon != null && close_connection) {
                DBPool.push(ctx, readcon);
            }
        }
    }

    public SearchIterator<Appointment> getActiveAppointments(final int user_uid, final Date start, final Date end, int cols[]) throws OXException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        Connection readcon = null;
        PreparedStatement prep = null;
        boolean close_connection = true;
        final Context ctx = Tools.getContext(session);
        try {
            readcon = DBPool.pickup(ctx);
            cols = recColl.checkAndAlterCols(cols);
            final CalendarOperation co = new CalendarOperation();
            prep = cimp.getActiveAppointments(ctx, session.getUserId(), start, end, StringCollection.getSelect(cols, DATES_TABLE_NAME), readcon);
            final ResultSet rs = cimp.getResultSet(prep);
            co.setResultSet(rs, prep, cols, cimp, readcon, 0, 0, session, ctx);
            close_connection = false;
            return new AppointmentIteratorAdapter(new CachedCalendarIterator(co, ctx, session.getUserId()));
        } catch(final SQLException sqle) {
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
        } catch(final DBPoolingException dbpe) {
            throw new OXException(dbpe);
        } catch(final OXObjectNotFoundException oxonfe) {
            throw oxonfe;
        } catch(final OXCalendarException oxc) {
            throw oxc;
        } catch(final OXException oxe) {
            throw oxe;
        } catch(final Exception e) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, Integer.valueOf(35));
        } finally {
            if (readcon != null && close_connection) {
                DBPool.push(ctx, readcon);
            }
        }
    }

    public SearchIterator<Appointment> getModifiedAppointmentsBetween(final int userId, final Date start, final Date end, int[] cols, final Date since, final int orderBy, final String orderDir) throws OXException, SQLException {
        if (session == null) {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
        Connection readcon = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        boolean close_connection = true;
        final Context ctx = Tools.getContext(session);
        final User user = Tools.getUser(session, ctx);
        final UserConfiguration userConfig = Tools.getUserConfiguration(ctx, session.getUserId());
        try {
            readcon = DBPool.pickup(ctx);
            cols = recColl.checkAndAlterCols(cols);
            final CalendarOperation co = new CalendarOperation();
            prep = cimp.getAllAppointmentsForUser(ctx, session.getUserId(), user.getGroups(), userConfig, start, end, StringCollection.getSelect(cols, DATES_TABLE_NAME), readcon, since, orderBy, orderDir);
            rs = cimp.getResultSet(prep);
            co.setResultSet(rs, prep, cols, cimp, readcon, 0, 0, session, ctx);
            close_connection = false;
            return new AppointmentIteratorAdapter(new CachedCalendarIterator(co, ctx, session.getUserId()));
        } catch(final OXPermissionException oxpe) {
            throw oxpe;
        } catch(final SQLException sqle) {
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
        } catch(final DBPoolingException dbpe) {
            throw new OXException(dbpe);
        } catch(final OXCalendarException oxc) {
            throw oxc;
        } catch(final OXException oxe) {
            throw oxe;
        } catch(final Exception e) {
            throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, Integer.valueOf(36));
        } finally {
            if (close_connection) {
                recColl.closeResultSet(rs);
                recColl.closePreparedStatement(prep);
            }
            if (readcon != null && close_connection) {
                DBPool.push(ctx, readcon);
            }
        }
    }

    public SearchIterator<Appointment> getAppointmentsBetween(final int user_uid, final Date start, final Date end, final int cols[], final int orderBy, final String orderDir) throws OXException, SQLException {
        return getModifiedAppointmentsBetween(user_uid, start, end, cols, null, orderBy, orderDir);
    }

    public static final CalendarSqlImp getCalendarSqlImplementation() {
        if (cimp != null){
            return cimp;
        }
        LOG.error("No CalendarSqlImp Class found !");
        try {
            cimp = (CalendarSqlImp) Class.forName(default_class).newInstance();
            return cimp;
        } catch(final ClassNotFoundException cnfe) {
            LOG.error(cnfe.getMessage(), cnfe);
        } catch (final IllegalAccessException iae) {
            LOG.error(iae.getMessage(), iae);
        } catch (final InstantiationException ie) {
            LOG.error(ie.getMessage(), ie);
        }
        return null;
    }

    static {
        try {
            if (cimp == null) {
                CalendarConfig.init();
                String classname = CalendarConfig.getProperty("CalendarSQL");
                if (classname == null) {
                    classname = default_class;
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Using "+classname+" in CalendarSql");
                }
                cimp = (CalendarSqlImp) Class.forName(classname).newInstance();
            }
        } catch(final ConfigurationException ce) {
            LOG.error(ce.getMessage(), ce);
        } catch(final ClassNotFoundException cnfe) {
            LOG.error(cnfe.getMessage(), cnfe);
        } catch (final IllegalAccessException iae) {
            LOG.error(iae.getMessage(), iae);
        } catch (final InstantiationException ie) {
            LOG.error(ie.getMessage(), ie);
        }
    }

    /**
     * Wraps specified <code>int</code> with parenthesis
     * 
     * @param i
     *            The <code>int</code> value to wrap
     * @return The wrapped <code>int</code> value
     */
    private static final String wrapParenthesis(final int i) {
        final String str = String.valueOf(i);
        return new StringBuilder(str.length() + 2).append('(').append(str).append(')').toString();
    }

    public int resolveUid(String uid) throws OXException {
        return cimp.resolveUid(session, uid);
    }

    public int getFolder(int objectId) throws OXException {
        return cimp.getFolder(session, objectId);
    }

    public void setIncludePrivateAppointments(boolean include) {
        this.includePrivateAppointments = include;
    }

    public boolean getIncludePrivateAppointments() {
        return this.includePrivateAppointments;
    }
}
