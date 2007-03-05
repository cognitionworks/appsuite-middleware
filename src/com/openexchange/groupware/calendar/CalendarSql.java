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

package com.openexchange.groupware.calendar;

import com.openexchange.api.OXObjectNotFoundException;
import com.openexchange.api.OXPermissionException;
import com.openexchange.api2.OXConcurrentModificationException;
import com.openexchange.configuration.ConfigurationException;
import com.openexchange.groupware.*;
import com.openexchange.groupware.container.AppointmentObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.search.AppointmentSearchObject;
import com.openexchange.server.DBPool;
import com.openexchange.server.DBPoolingException;
import com.openexchange.server.EffectivePermission;
import com.openexchange.sessiond.SessionObject;
import com.openexchange.tools.StringCollection;
import com.openexchange.tools.iterator.SearchIteratorException;
import com.openexchange.tools.oxfolder.OXFolderTools;
import com.openexchange.tools.sql.DBUtils;
import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.api2.OXException;
import com.openexchange.tools.iterator.SearchIterator;


/**
   CalendarSql
   @author <a href="mailto:martin.kauss@open-xchange.org">Martin Kauss</a>
*/

public class CalendarSql implements AppointmentSQLInterface {
    
    public static final String default_class = "com.openexchange.groupware.calendar.CalendarMySQL";
    
    public static final String ERROR_PUSHING_DATABASE = "error pushing readable connection";
    public static final String ERROR_PUSHING_WRITEABLE_CONNECTION = "error pushing writeable connection";
    
    public static final String DATES_TABLE_NAME = "prg_dates";
    public static final String VIEW_TABLE_NAME = "prg_date_rights";
    public static final String PARTICIPANT_TABLE_NAME = "prg_dates_members";
    
    private static CalendarSqlImp cimp;
    private SessionObject sessionobject;
    private static final Log LOG = LogFactory.getLog(CalendarSql.class);
    
    public CalendarSql(SessionObject sessionobject) {
        this.sessionobject = sessionobject;
    }                
    
    public boolean[] hasAppointmentsBetween(Date d1, Date d2) throws OXException {
        if (sessionobject != null) {
            Connection readcon = null;
            try {
                readcon = DBPool.pickup(sessionobject.getContext());
                return cimp.getUserActiveAppointmentsRangeSQL(sessionobject.getContext(), sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), sessionobject.getUserConfiguration(), d1, d2, readcon); 
            } catch(Exception e) {
                throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, e);
            } finally {
                try {
                    if (readcon != null) {
                        DBPool.push(sessionobject.getContext(), readcon);
                    }
                } catch (DBPoolingException dbpe) {
                    LOG.error(ERROR_PUSHING_DATABASE, dbpe);
                }
            }
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }  
    }

    public SearchIterator getAppointmentsBetweenInFolder(int fid, int[] cols, Date start, Date end, int orderBy, String orderDir) throws OXException, SQLException {        
        return getAppointmentsBetweenInFolder(fid, cols, start, end, 0, 0, orderBy, orderDir);
    }

    public SearchIterator getAppointmentsBetweenInFolder(int fid, int[] cols, Date start, Date end, int from, int to, int orderBy, String orderDir) throws OXException, SQLException {    
        if (sessionobject != null) {
            Connection readcon = null;
            PreparedStatement prep = null;
            ResultSet rs = null;
            boolean close_connection = true;
            try {
                readcon = DBPool.pickup(sessionobject.getContext());
                cols = CalendarCommonCollection.checkAndAlterCols(cols); 
                if (OXFolderTools.getFolderType(fid, sessionobject.getUserObject().getId(), sessionobject.getContext(), readcon) == FolderObject.PRIVATE) {
                    CalendarOperation co = new CalendarOperation();
                    EffectivePermission oclp = OXFolderTools.getEffectiveFolderOCL(fid, sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), sessionobject.getContext(), sessionobject.getUserConfiguration());
                    if (oclp.canReadAllObjects()) {
                        prep = cimp.getPrivateFolderRangeSQL(sessionobject.getContext(), sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), fid, start, end, StringCollection.getSelect(cols, DATES_TABLE_NAME), true, readcon, orderBy, orderDir);
                        rs = cimp.getResultSet(prep);
                        co.setRequestedFolder(fid);
                        co.setResultSet(rs, prep, cols, cimp, readcon, from, to, sessionobject);
                        close_connection = false;
                        return new CachedCalendarIterator(co);
                    } else if (oclp.canReadOwnObjects()) {
                        prep = cimp.getPrivateFolderRangeSQL(sessionobject.getContext(), sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), fid, start, end, StringCollection.getSelect(cols, DATES_TABLE_NAME), false, readcon, orderBy, orderDir);
                        rs = cimp.getResultSet(prep);
                        co.setRequestedFolder(fid);
                        co.setResultSet(rs, prep, cols, cimp, readcon, from, to, sessionobject);
                        close_connection = false;
                        return new CachedCalendarIterator(co);
                    } else {
                        throw new OXCalendarException(OXCalendarException.Code.NO_PERMISSION);
                    }
                } else if (OXFolderTools.getFolderType(fid, sessionobject.getUserObject().getId(), sessionobject.getContext(), readcon) == FolderObject.PUBLIC) {
                    CalendarOperation co = new CalendarOperation();
                    EffectivePermission oclp = OXFolderTools.getEffectiveFolderOCL(fid, sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), sessionobject.getContext(), sessionobject.getUserConfiguration());
                    if (oclp.canReadAllObjects()) {  
                        prep = cimp.getPublicFolderRangeSQL(sessionobject.getContext(), sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), fid, start, end, StringCollection.getSelect(cols, DATES_TABLE_NAME), true, readcon, orderBy, orderDir);
                        rs = cimp.getResultSet(prep);
                        co.setRequestedFolder(fid);
                        co.setResultSet(rs, prep, cols, cimp, readcon, from, to, sessionobject);
                        close_connection = false;
                        return new CachedCalendarIterator(co);
                    } else if (oclp.canReadOwnObjects()) {
                        prep = cimp.getPublicFolderRangeSQL(sessionobject.getContext(), sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), fid, start, end, StringCollection.getSelect(cols, DATES_TABLE_NAME), false, readcon, orderBy, orderDir);
                        rs = cimp.getResultSet(prep);
                        co.setRequestedFolder(fid);
                        co.setResultSet(rs, prep, cols, cimp, readcon, from, to, sessionobject);
                        close_connection = false;
                        return new CachedCalendarIterator(co);
                    } else {
                        throw new OXCalendarException(OXCalendarException.Code.NO_PERMISSION);
                    }                    
                } else {
                    CalendarOperation co = new CalendarOperation();
                    EffectivePermission oclp = OXFolderTools.getEffectiveFolderOCL(fid, sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), sessionobject.getContext(), sessionobject.getUserConfiguration());
                    int shared_folder_owner = OXFolderTools.getFolderOwner(fid, sessionobject.getContext(), readcon);
                    if (oclp.canReadAllObjects()) {
                        prep = cimp.getSharedFolderRangeSQL(sessionobject.getContext(), sessionobject.getUserObject().getId(), shared_folder_owner, sessionobject.getUserObject().getGroups(), fid, start, end, StringCollection.getSelect(cols, DATES_TABLE_NAME), true, readcon, orderBy, orderDir);
                        rs = cimp.getResultSet(prep);
                        co.setRequestedFolder(fid);
                        co.setResultSet(rs, prep,cols, cimp, readcon, from, to, sessionobject);
                        close_connection = false;
                        return new CachedCalendarIterator(co);
                    } else if (oclp.canReadOwnObjects()) {
                        prep = cimp.getSharedFolderRangeSQL(sessionobject.getContext(), sessionobject.getUserObject().getId(), shared_folder_owner, sessionobject.getUserObject().getGroups(), fid, start, end, StringCollection.getSelect(cols, DATES_TABLE_NAME), false, readcon, orderBy, orderDir);
                        rs = cimp.getResultSet(prep);
                        co.setRequestedFolder(fid);
                        co.setResultSet(rs, prep, cols, cimp, readcon, from, to, sessionobject);
                        close_connection = false;
                        return new CachedCalendarIterator(co);
                    } else {
                        throw new OXCalendarException(OXCalendarException.Code.NO_PERMISSION);
                    }                    
                }
            } catch (IndexOutOfBoundsException ioobe) {
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, ioobe, 19);
            } catch (OXPermissionException oxpe) {
                throw oxpe;
            } catch(OXCalendarException oxc) {
                throw oxc;
            } catch(OXException oxe) {
                throw oxe;
            } catch(SQLException sqle) {
                throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
            } catch (Exception e) {
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, 20);
            } finally  {
                if (close_connection) {
                    CalendarCommonCollection.closeResultSet(rs);
                    CalendarCommonCollection.closePreparedStatement(prep);
                }
                if (readcon != null && close_connection) {
                    try {
                        DBPool.push(sessionobject.getContext(), readcon);
                    } catch (DBPoolingException dbpe) {
                        LOG.error(ERROR_PUSHING_DATABASE, dbpe);
                    }
                }
            }    
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }       
    }    
    
    public SearchIterator getModifiedAppointmentsInFolder(int fid, Date start, Date end, int[] cols, Date since) throws OXException, SQLException {    
        if (sessionobject != null) {
            Connection readcon = null;
            PreparedStatement prep = null;
            ResultSet rs = null;
            boolean close_connection = true;
            try {
                readcon = DBPool.pickup(sessionobject.getContext());
                cols = CalendarCommonCollection.checkAndAlterCols(cols); 
                if (OXFolderTools.getFolderType(fid, sessionobject.getUserObject().getId(), sessionobject.getContext(), readcon) == FolderObject.PRIVATE) {
                    CalendarOperation co = new CalendarOperation();
                    EffectivePermission oclp = OXFolderTools.getEffectiveFolderOCL(fid, sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), sessionobject.getContext(), sessionobject.getUserConfiguration());
                    prep = cimp.getPrivateFolderModifiedSinceSQL(sessionobject.getContext(), sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), fid, since, StringCollection.getSelect(cols, DATES_TABLE_NAME), oclp.canReadAllObjects(), readcon, start, end);
                    rs = cimp.getResultSet(prep);
                    co.setRequestedFolder(fid);
                    co.setResultSet(rs, prep, cols, cimp, readcon, 0, 0, sessionobject);
                    close_connection = false;
                    return new CachedCalendarIterator(co);
                } else if (OXFolderTools.getFolderType(fid, sessionobject.getUserObject().getId(), sessionobject.getContext(), readcon) == FolderObject.PUBLIC) {
                    CalendarOperation co = new CalendarOperation();
                    EffectivePermission oclp = OXFolderTools.getEffectiveFolderOCL(fid, sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), sessionobject.getContext(), sessionobject.getUserConfiguration());
                    prep = cimp.getPublicFolderModifiedSinceSQL(sessionobject.getContext(), sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), fid, since, StringCollection.getSelect(cols, DATES_TABLE_NAME), oclp.canReadAllObjects(), readcon, start, end);
                    rs = cimp.getResultSet(prep);
                    co.setRequestedFolder(fid);
                    co.setResultSet(rs, prep, cols, cimp, readcon, 0, 0, sessionobject);
                    close_connection = false;
                    return new CachedCalendarIterator(co);
                } else {
                    CalendarOperation co = new CalendarOperation();
                    EffectivePermission oclp = OXFolderTools.getEffectiveFolderOCL(fid, sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), sessionobject.getContext(), sessionobject.getUserConfiguration());
                    int shared_folder_owner = OXFolderTools.getFolderOwner(fid, sessionobject.getContext(), readcon);                    
                    prep = cimp.getSharedFolderModifiedSinceSQL(sessionobject.getContext(), sessionobject.getUserObject().getId(), shared_folder_owner, sessionobject.getUserObject().getGroups(), fid, since, StringCollection.getSelect(cols, DATES_TABLE_NAME), oclp.canReadAllObjects(), readcon, start, end);
                    rs = cimp.getResultSet(prep);
                    co.setRequestedFolder(fid);
                    co.setResultSet(rs, prep, cols, cimp, readcon, 0, 0, sessionobject);
                    close_connection = false;
                    return new CachedCalendarIterator(co);
                }
            } catch (IndexOutOfBoundsException ioobe) {
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, ioobe, 21);
            } catch (OXPermissionException oxpe) {
                throw oxpe;
            } catch(OXCalendarException oxc) {
                throw oxc;
            } catch(OXException oxe) {
                throw oxe;
            } catch(SQLException sqle) {
                throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
            } catch (Exception e) {
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, 22);
            } finally {
                if (close_connection) {
                    CalendarCommonCollection.closeResultSet(rs);
                    CalendarCommonCollection.closePreparedStatement(prep);
                }
                if (readcon != null && close_connection) {
                    try {
                        DBPool.push(sessionobject.getContext(), readcon);
                    } catch (DBPoolingException dbpe) {
                        LOG.error(ERROR_PUSHING_DATABASE, dbpe);
                    }
                }
            }
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }        
    }
        
    public SearchIterator getModifiedAppointmentsInFolder(int fid, int cols[], Date since) throws OXException, SQLException {        
        return getModifiedAppointmentsInFolder(fid, null, null, cols, since);
    }

    public SearchIterator getDeletedAppointmentsInFolder(int fid, int cols[], Date since) throws OXException, SQLException {    
        if (sessionobject != null) {
            Connection readcon = null;
            PreparedStatement prep = null;
            ResultSet rs = null;
            boolean close_connection = true;
           try {
               readcon = DBPool.pickup(sessionobject.getContext());
               cols = CalendarCommonCollection.checkAndAlterCols(cols); 
               
                if (OXFolderTools.getFolderType(fid, sessionobject.getUserObject().getId(), sessionobject.getContext(), readcon) == FolderObject.PRIVATE) {
                    CalendarOperation co = new CalendarOperation();
                    prep = cimp.getPrivateFolderDeletedSinceSQL(sessionobject.getContext(), sessionobject.getUserObject().getId(), fid, since, StringCollection.getSelect(cols, "del_dates"), readcon);
                    rs = cimp.getResultSet(prep);
                    co.setRequestedFolder(fid);
                    co.setResultSet(rs, prep, cols, cimp, readcon, 0, 0, sessionobject);
                    close_connection = false;
                    return new CachedCalendarIterator(co);
                } else if (OXFolderTools.getFolderType(fid, sessionobject.getUserObject().getId(), sessionobject.getContext(), readcon) == FolderObject.PUBLIC) {
                    CalendarOperation co = new CalendarOperation();
                    prep = cimp.getPublicFolderDeletedSinceSQL(sessionobject.getContext(), sessionobject.getUserObject().getId(), fid, since, StringCollection.getSelect(cols, "del_dates"), readcon);
                    rs = cimp.getResultSet(prep);
                    co.setRequestedFolder(fid);
                    co.setResultSet(rs, prep,cols, cimp, readcon, 0, 0, sessionobject);
                    close_connection = false;
                    return new CachedCalendarIterator(co);
                } else {
                    CalendarOperation co = new CalendarOperation();
                    int shared_folder_owner = OXFolderTools.getFolderOwner(fid, sessionobject.getContext(), readcon);
                    prep = cimp.getSharedFolderDeletedSinceSQL(sessionobject.getContext(), sessionobject.getUserObject().getId(), shared_folder_owner, fid, since, StringCollection.getSelect(cols, "del_dates"), readcon);
                    rs = cimp.getResultSet(prep);
                    co.setRequestedFolder(fid);
                    co.setResultSet(rs, prep, cols, cimp, readcon, 0, 0, sessionobject);
                    close_connection = false;
                    return new CachedCalendarIterator(co);
                }
            } catch (IndexOutOfBoundsException ioobe) {
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, ioobe, 23);
            } catch (OXPermissionException oxpe) {
                throw oxpe;          
            } catch(OXCalendarException oxc) {
                throw oxc;
            } catch(OXException oxe) {
                throw oxe;
            } catch(SQLException sqle) {
                throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
            } catch (Exception e) {
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, 24);
            } finally {
                if (close_connection) {
                    CalendarCommonCollection.closeResultSet(rs);
                    CalendarCommonCollection.closePreparedStatement(prep);
                }
                if (readcon != null && close_connection) {
                    try {
                        DBPool.push(sessionobject.getContext(), readcon);
                    } catch (DBPoolingException dbpe) {
                        LOG.error(ERROR_PUSHING_DATABASE, dbpe);
                    }
                }
            }
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }           
    }
    
    public CalendarDataObject getObjectById(int oid, int inFolder) throws OXException, SQLException, OXObjectNotFoundException, OXPermissionException {
        if (sessionobject != null) {
            Connection readcon = null;
            PreparedStatement prep = null;
            ResultSet rs = null;
            try {
                readcon = DBPool.pickup(sessionobject.getContext());
                CalendarOperation co = new CalendarOperation();
                prep = cimp.getPreparedStatement(readcon, cimp.loadAppointment(oid, sessionobject.getContext()));
                rs = cimp.getResultSet(prep);  
                CalendarDataObject cdao = co.loadAppointment(rs, oid, inFolder, cimp, readcon, sessionobject, CalendarOperation.READ, inFolder);
                if (cdao.getRecurrenceType() != AppointmentObject.NO_RECURRENCE) {
                    RecurringResults rrs = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 1);
                    RecurringResult rr = rrs.getRecurringResultByPosition(1);
                    if (rr != null) {
                        cdao.setStartDate(new Date(rr.getStart()));
                        cdao.setEndDate(new Date(rr.getEnd()));
                    }
                }
                return cdao;
            } catch (OXPermissionException oxpe) {
                throw oxpe;                
            } catch(OXObjectNotFoundException oxonfe) {
                throw oxonfe;
            } catch(OXCalendarException oxc) {
                throw oxc;
            } catch(OXException oxe) {
                throw oxe;
            } catch(SQLException sqle) {
                throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
            } catch(DBPoolingException dbpe) {
                throw new OXException(dbpe);
            } finally {
                    CalendarCommonCollection.closeResultSet(rs);
                    CalendarCommonCollection.closePreparedStatement(prep);                    
                if (readcon != null) {
                    try {
                        DBPool.push(sessionobject.getContext(), readcon);
                    } catch (DBPoolingException dbpe) {
                        LOG.error(ERROR_PUSHING_DATABASE, dbpe);
                    }
                }
            }
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
    }
    
    public CalendarDataObject[] insertAppointmentObject(CalendarDataObject cdao) throws OXException, OXPermissionException {
        if (sessionobject != null) {
            Connection writecon = null;
            try {
                CalendarOperation co = new CalendarOperation();
                if (co.prepareUpdateAction(cdao, sessionobject.getUserObject().getId(), cdao.getParentFolderID(), sessionobject.getUserObject().getTimeZone())) {
                    try {
                        EffectivePermission oclp = OXFolderTools.getEffectiveFolderOCL(cdao.getEffectiveFolderId(), sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), sessionobject.getContext(), sessionobject.getUserConfiguration());
                        if (oclp.canCreateObjects()) {
                            cdao.setActionFolder(cdao.getParentFolderID());
                            ConflictHandler ch = new ConflictHandler(cdao, sessionobject, true);
                            CalendarDataObject conflicts[] = ch.getConflicts();
                            if (conflicts.length == 0) {
                                writecon = DBPool.pickupWriteable(sessionobject.getContext());            
                                writecon.setAutoCommit(false);
                                return cimp.insertAppointment(cdao, writecon, sessionobject);
                            } else {
                                return conflicts;
                            }
                        } else {
                            throw new OXPermissionException(new OXCalendarException(OXCalendarException.Code.LOAD_PERMISSION_EXCEPTION_6));
                        }
                    } catch(DataTruncation dt) {
                        String fields[] = DBUtils.parseTruncatedFields(dt);
                        int fid[] = new int[fields.length];
                        OXException oxe = new OXException(new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR));
                        oxe.setCategory(AbstractOXException.Category.TRUNCATED);
                        int id = -1;
                        for (int a = 0; a < fid.length; a++) {
                            id = CalendarCommonCollection.getFieldId(fields[a]);
                            oxe.addTruncatedId(id);
                        }
                        throw oxe;                        
                    } catch(SQLException sqle) {
                        try {
                            if (!writecon.getAutoCommit()) {
                                writecon.rollback();
                            }
                        } catch(SQLException rb) {
                            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, rb);
                        }
                        throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
                    } finally {
                        if (writecon != null) {
                            writecon.setAutoCommit(true);
                        }
                    }                
                } else {
                    throw new OXCalendarException(OXCalendarException.Code.INSERT_WITH_OBJECT_ID);
                }
            } catch(DataTruncation dt) {
                String fields[] = DBUtils.parseTruncatedFields(dt);
                int fid[] = new int[fields.length];
                OXException oxe = new OXException(new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR));
                oxe.setCategory(AbstractOXException.Category.TRUNCATED);
                int id = -1;
                for (int a = 0; a < fid.length; a++) {
                    id = CalendarCommonCollection.getFieldId(fields[a]);
                    oxe.addTruncatedId(id);
                }
                throw oxe;
            } catch (SQLException sqle) {
                throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
            } catch(DBPoolingException dbpe) {
                throw new OXException(dbpe);             
            } catch (OXPermissionException oxpe) {
                throw oxpe;
            } catch(OXCalendarException oxc) {
                throw oxc;
            } catch(OXException oxe) {
                throw oxe;
            } catch(Exception e) {
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, 25);
            } finally {
                if (writecon != null) {
                    try {
                        DBPool.pushWrite(sessionobject.getContext(), writecon);
                    } catch (DBPoolingException dbpe) {
                        LOG.error(ERROR_PUSHING_WRITEABLE_CONNECTION, dbpe);
                    }
                }
            }
            
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }    
    }
    
    public CalendarDataObject[] updateAppointmentObject(CalendarDataObject cdao, int inFolder, Date clientLastModified) throws OXException, OXPermissionException, OXConcurrentModificationException, OXObjectNotFoundException {    
        if (sessionobject != null) { 
            Connection writecon = null;
            try {
                CalendarOperation co = new CalendarOperation();
                if (!co.prepareUpdateAction(cdao, sessionobject.getUserObject().getId(), inFolder, sessionobject.getUserObject().getTimeZone())) {
                    CalendarDataObject edao = cimp.loadObjectForUpdate(cdao, sessionobject, inFolder);
                    CalendarDataObject conflict_dao = CalendarCommonCollection.fillFieldsForConflictQuery(cdao, edao);
                    ConflictHandler ch = new ConflictHandler(conflict_dao, sessionobject, false);
                    CalendarDataObject conflicts[] = ch.getConflicts();
                    if (conflicts.length == 0) {                    
                        writecon = DBPool.pickupWriteable(sessionobject.getContext());                    
                        try {
                            writecon.setAutoCommit(false);
                            if (cdao.containsParentFolderID()) {
                                cdao.setActionFolder(cdao.getParentFolderID());
                            } else {
                                cdao.setActionFolder(inFolder);
                            }
                            return cimp.updateAppointment(cdao, edao, writecon, sessionobject, inFolder, clientLastModified);
                        } catch(DataTruncation dt) {
                            String fields[] = DBUtils.parseTruncatedFields(dt);
                            int fid[] = new int[fields.length];
                            OXException oxe = new OXException(new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR));
                            oxe.setCategory(AbstractOXException.Category.TRUNCATED);
                            int id = -1;
                            for (int a = 0; a < fid.length; a++) {
                                id = CalendarCommonCollection.getFieldId(fields[a]);
                                oxe.addTruncatedId(id);
                            }
                            throw oxe;
                        } catch(SQLException sqle) {
                            try {
                                if (writecon != null) {
                                    writecon.rollback();
                                }
                            } catch(SQLException rb) {
                                throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, rb);
                            }
                            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
                        } finally {
                            if (writecon != null) {
                                writecon.setAutoCommit(true);
                            }
                        }                
                    } else {
                        return conflicts;
                    }
                } else {
                    throw new OXCalendarException(OXCalendarException.Code.UPDATE_WITHOUT_OBJECT_ID);
                }
            } catch(DataTruncation dt) {
                String fields[] = DBUtils.parseTruncatedFields(dt);
                int fid[] = new int[fields.length];
                OXException oxe = new OXException(new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR));
                oxe.setCategory(AbstractOXException.Category.TRUNCATED);
                int id = -1;
                for (int a = 0; a < fid.length; a++) {
                    id = CalendarCommonCollection.getFieldId(fields[a]);
                    oxe.addTruncatedId(id);
                }
                throw oxe;                
            } catch(SQLException sqle) {
                throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
            } catch(DBPoolingException dbpe) {
                throw new OXException(dbpe);
            } catch(OXObjectNotFoundException oxonfe) {
                throw oxonfe;
            } catch(OXCalendarException oxce) {
                throw oxce;
            } catch(OXException oxe) {
                throw oxe;
            } catch (Exception e) {
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, 26);
            } finally {
                if (writecon != null) {
                    try {
                        DBPool.pushWrite(sessionobject.getContext(), writecon);
                    } catch (DBPoolingException dbpe) {
                        LOG.error(ERROR_PUSHING_WRITEABLE_CONNECTION, dbpe);
                    }
                }
            }
            
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }           
    }

    public void deleteAppointmentObject(CalendarDataObject cdao, int inFolder, Date clientLastModified) throws OXException, SQLException, OXPermissionException, OXConcurrentModificationException {
        if (sessionobject != null) {
            Connection writecon = null;
            try  {
                writecon = DBPool.pickupWriteable(sessionobject.getContext());
                try {
                    writecon.setAutoCommit(false);
                    cimp.deleteAppointment(sessionobject.getUserObject().getId(), cdao, writecon, sessionobject, inFolder, clientLastModified);
                } catch(SQLException sqle) {
                    try {
                        writecon.rollback();
                    } catch(SQLException rb) {
                        throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, rb);
                    }
                    throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
                } finally {
                    try {
                        writecon.setAutoCommit(true);
                    } catch(SQLException ac) {
                        throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, ac);
                    }
                }
            } catch(OXConcurrentModificationException oxcme) {
                throw oxcme;
            } catch(OXPermissionException oxpe) {
                throw oxpe;    
            } catch(OXObjectNotFoundException oxonfe) {
                throw oxonfe;
            } catch(OXCalendarException oxc) {
                throw oxc;
            } catch(OXException oxe) {
                throw oxe;                
            } catch(Exception e) {
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, 28);
            } finally {
                if (writecon != null) {
                    try {
                        DBPool.pushWrite(sessionobject.getContext(), writecon);
                    } catch (DBPoolingException dbpe) {
                        LOG.error(ERROR_PUSHING_WRITEABLE_CONNECTION,  dbpe);
                    }
                }
            }
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
    }
    
    public void deleteAppointmentsInFolder(int fid) throws OXException, SQLException, OXPermissionException {
        if (sessionobject != null) {
            Connection readcon = null, writecon = null;
            PreparedStatement prep = null;
            ResultSet rs = null;
            try  {
                readcon = DBPool.pickup(sessionobject.getContext());
                try {
                    if (OXFolderTools.isFolderPrivate(fid, sessionobject.getContext(), readcon)) {
                        prep = cimp.getPrivateFolderObjects(fid, sessionobject.getContext(), readcon);
                        rs = cimp.getResultSet(prep);
                        writecon = DBPool.pickupWriteable(sessionobject.getContext());
                        writecon.setAutoCommit(false);
                        cimp.deleteAppointmentsInFolder(sessionobject, rs, readcon, writecon, FolderObject.PRIVATE, fid);
                    } else if (OXFolderTools.isFolderPublic(fid, sessionobject.getContext(), readcon)) {
                        prep = cimp.getPublicFolderObjects(fid, sessionobject.getContext(), readcon);
                        rs = cimp.getResultSet(prep);
                        writecon = DBPool.pickupWriteable(sessionobject.getContext());
                        writecon.setAutoCommit(false);
                        cimp.deleteAppointmentsInFolder(sessionobject, rs, readcon, writecon, FolderObject.PUBLIC, fid);
                    } else {
                        throw new OXCalendarException(OXCalendarException.Code.FOLDER_DELETE_INVALID_REQUEST);
                    }
                } catch(SQLException sqle) {
                    try {
                        writecon.rollback();
                    } catch(SQLException rb) {
                        throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, rb);
                    }
                    throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
                } finally {
                    try {
                        writecon.setAutoCommit(true);
                    } catch(SQLException ac) {
                        throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, ac);
                    }
                }
            } catch(OXCalendarException oxc) {
                throw oxc;
            } catch(OXPermissionException oxpe) {
                throw oxpe;
            } catch(OXException oxe) {
                throw oxe;
            } catch(Exception e) {
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, 29);
            } finally {
                     CalendarCommonCollection.closeResultSet(rs);
                    CalendarCommonCollection.closePreparedStatement(prep);
                if (readcon != null) {
                    try {
                        DBPool.push(sessionobject.getContext(), readcon);
                    } catch (DBPoolingException dbpe) {
                        LOG.error(ERROR_PUSHING_DATABASE, dbpe);
                    }
                }
                if (writecon != null) {
                    try {
                        DBPool.pushWrite(sessionobject.getContext(), writecon);
                    } catch (DBPoolingException dbpe) {
                        LOG.error(ERROR_PUSHING_WRITEABLE_CONNECTION,  dbpe);
                    }
                }
            }
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }        
    }      
    
    public boolean checkIfFolderContainsForeignObjects(int uid, int fid) throws OXException, SQLException {
        if (sessionobject != null) {
            Connection readcon = null;
            try {
                readcon = DBPool.pickup(sessionobject.getContext());
                if (OXFolderTools.isFolderPrivate(fid, sessionobject.getContext(), readcon)) {
                    return cimp.checkIfFolderContainsForeignObjects(uid, fid, sessionobject.getContext(), readcon, FolderObject.PRIVATE);
                } else if (OXFolderTools.isFolderPublic(fid, sessionobject.getContext(), readcon)) {
                    return cimp.checkIfFolderContainsForeignObjects(uid, fid, sessionobject.getContext(), readcon, FolderObject.PUBLIC);
                } else {
                    throw new OXCalendarException(OXCalendarException.Code.FOLDER_FOREIGN_INVALID_REQUEST);
                }
            } catch(OXCalendarException oxc) {
                throw oxc;
            } catch(SQLException sqle) {
                throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);                
            } catch(DBPoolingException dbpe) {
                throw new OXException(dbpe);
            } catch(OXException oxe) {
                throw oxe;                
            } catch(Exception e) {          
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, 30);
            } finally {
                if (readcon != null) {
                    try {
                        DBPool.push(sessionobject.getContext(), readcon);
                    } catch (DBPoolingException dbpe) {
                        LOG.error(ERROR_PUSHING_DATABASE, dbpe);
                    }
                }
            }
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }        
    }
    
    public boolean isFolderEmpty(int uid, int fid) throws OXException, SQLException {
        if (sessionobject != null) {
            Connection readcon = null;
            try {
                readcon = DBPool.pickup(sessionobject.getContext());
                if (OXFolderTools.isFolderPrivate(fid, sessionobject.getContext(), readcon)) {
                    return cimp.checkIfFolderIsEmpty(uid, fid, sessionobject.getContext(), readcon, FolderObject.PRIVATE);
                } else if (OXFolderTools.isFolderPublic(fid, sessionobject.getContext(), readcon)) {
                    return cimp.checkIfFolderIsEmpty(uid, fid, sessionobject.getContext(), readcon, FolderObject.PUBLIC);
                } else {
                    throw new OXCalendarException(OXCalendarException.Code.FOLDER_IS_EMPTY_INVALID_REQUEST);
                }
            } catch(OXCalendarException oxc) {
                throw oxc;
            } catch(SQLException sqle) {
                throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
            } catch(DBPoolingException dbpe) {
                throw new OXException(dbpe);
            } catch(OXException oxe) {
                throw oxe;
            } catch(Exception e) {          
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, 31);
            } finally {
                if (readcon != null) {
                    try {
                        DBPool.push(sessionobject.getContext(), readcon);
                    } catch (DBPoolingException dbpe) {
                        LOG.error(ERROR_PUSHING_DATABASE, dbpe);
                    }
                }
            }
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }        
    }    
    
    
    public void setUserConfirmation(int oid, int uid, int confirm, String confirm_message) throws OXException {
        if (sessionobject != null) {
            cimp.setUserConfirmation(oid, uid, confirm, confirm_message, sessionobject);
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
    }

    public SearchIterator getObjectsById(int[][] oids, int[] cols) throws OXException {
        if (sessionobject != null) {
            if (oids.length > 0) {
                Connection readcon = null;
                PreparedStatement prep = null;
                ResultSet rs = null;
                boolean close_connection = true;
                try {
                    readcon = DBPool.pickup(sessionobject.getContext());
                    cols = CalendarCommonCollection.checkAndAlterCols(cols); 
                    CalendarOperation co = new CalendarOperation();
                    prep = cimp.getPreparedStatement(readcon, cimp.getObjectsByidSQL(oids, sessionobject.getContext().getContextId(), StringCollection.getSelect(cols, DATES_TABLE_NAME)));
                    rs = cimp.getResultSet(prep);
                    co.setOIDS(true, oids);
                    co.setResultSet(rs, prep, cols, cimp, readcon, 0, 0, sessionobject);
                    close_connection = false;
                    return new CachedCalendarIterator(co);
                } catch(SQLException sqle) {
                    throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
                } catch(DBPoolingException dbpe) {
                    throw new OXException(dbpe);
                } catch(OXObjectNotFoundException oxonfe) {
                    throw oxonfe;
                } catch(OXCalendarException oxc) {
                    throw oxc;
                } catch(OXException oxe) {
                    throw oxe;
                } catch(Exception e) {
                    throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, 32);
                } finally {
                    if (readcon != null && close_connection) {
                        CalendarCommonCollection.closeResultSet(rs);
                        CalendarCommonCollection.closePreparedStatement(prep);
                        try {
                            DBPool.push(sessionobject.getContext(), readcon);
                        } catch (DBPoolingException dbpe) {
                            LOG.error(ERROR_PUSHING_DATABASE ,dbpe);
                        }
                    }
                }
            } else {
                return new CalendarOperation();
            }
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
    }    
    
    public SearchIterator getAppointmentsByExtendedSearch(AppointmentSearchObject searchobject, int orderBy, String orderDir, int cols[]) throws OXException, SQLException {
        return getAppointmentsByExtendedSearch(searchobject, orderBy, orderDir, cols, 0, 0);
    }    
    
    public SearchIterator getAppointmentsByExtendedSearch(AppointmentSearchObject searchobject, int orderBy, String orderDir, int cols[], int from, int to) throws OXException, SQLException {
        if (sessionobject != null) {
            Connection readcon = null;
            PreparedStatement prep = null;
            ResultSet rs = null;
            boolean close_connection = true;
            try {
                CalendarOperation co = new CalendarOperation();
                if (searchobject.getFolder() != 0) {
                    co.setRequestedFolder(searchobject.getFolder());
                } else {
                    int ara[] = new int[1];
                    ara[0] = AppointmentObject.USERS;
                    cols = CalendarCommonCollection.enhanceCols(cols, ara, 1);
                }                
                cols = CalendarCommonCollection.checkAndAlterCols(cols); 
                CalendarFolderObject cfo = null;
                try {
                    cfo = CalendarCommonCollection.getVisibleAndReadableFolderObject(sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), sessionobject.getContext(), sessionobject.getUserConfiguration(), readcon);
                } catch (DBPoolingException dbpe) {
                    throw new OXException(dbpe);
                } catch (SearchIteratorException sie) {
                    throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, sie, 1);
                }                
                readcon = DBPool.pickup(sessionobject.getContext());
                prep = cimp.getSearchQuery(StringCollection.getSelect(cols, DATES_TABLE_NAME), sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), sessionobject.getUserConfiguration(), orderBy, orderDir, searchobject, sessionobject.getContext(), readcon, cfo);
                rs = cimp.getResultSet(prep);
                co.setResultSet(rs, prep, cols, cimp, readcon, 0, 0, sessionobject);
                close_connection = false;
                return new CachedCalendarIterator(co);
            } catch(SQLException sqle) {
                throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
            } catch(DBPoolingException dbpe) {
                throw new OXException(dbpe);
            } catch(OXObjectNotFoundException oxonfe) {
                throw oxonfe;
            } catch(OXCalendarException oxc) {
                throw oxc;
            } catch(OXException oxe) {
                throw oxe;                
            } catch(Exception e) {
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, 33);
            } finally {
                if (close_connection) {
                    CalendarCommonCollection.closeResultSet(rs);
                    CalendarCommonCollection.closePreparedStatement(prep);
                }
                if (readcon != null && close_connection) {
                    try {
                        DBPool.push(sessionobject.getContext(), readcon);
                    } catch (DBPoolingException dbpe) {
                        LOG.error(ERROR_PUSHING_DATABASE ,dbpe);
                    }
                }
            }
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }     
    }        
    
    public SearchIterator searchAppointments(String searchpattern, int fid, int orderBy, String orderDir, int[] cols) throws OXException {
        AppointmentSearchObject searchobject = new AppointmentSearchObject();
        searchobject.setPattern(searchpattern);
        searchobject.setFolder(fid);
        try {
            return getAppointmentsByExtendedSearch(searchobject, orderBy, orderDir, cols);
        } catch (SQLException sqle) {
            throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
        }
    }
    
    public final long attachmentAction(int oid, int uid, Context c, boolean action) throws OXException {
        return cimp.attachmentAction(oid, uid, c, action);
    }
    
    public SearchIterator getFreeBusyInformation(int uid, int type, Date start, Date end) throws OXException {
        if (sessionobject != null) {
            Connection readcon = null;
            PreparedStatement prep = null;
            ResultSet rs = null;
            boolean close_connection = true;
            try {
                readcon = DBPool.pickup(sessionobject.getContext());
                switch(type) {
                    case Participant.USER:
                        prep = cimp.getFreeBusy(uid, sessionobject.getContext(), start, end, readcon);
                        break;
                    case Participant.RESOURCE:
                        prep = cimp.getResourceFreeBusy(uid, sessionobject.getContext(), start, end, readcon);
                        break;
                    default:
                        throw new OXCalendarException(OXCalendarException.Code.FREE_BUSY_UNSUPPOTED_TYPE, type);
                }
                rs = cimp.getResultSet(prep);
                SearchIterator si = new FreeBusyResults(rs, prep, sessionobject.getContext(), readcon, start.getTime(), end.getTime());
                close_connection = false;
                return si;
            } catch(SQLException sqle) {
                throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
            } catch(DBPoolingException dbpe) {
                throw new OXException(dbpe);
            } catch(OXObjectNotFoundException oxonfe) {
                throw oxonfe;
            } catch(OXCalendarException oxc) {
                throw oxc;
            } catch(OXException oxe) {
                throw oxe;       
            } catch(Exception e) {
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, 34);
            } finally {
                if (close_connection) {
                     CalendarCommonCollection.closeResultSet(rs);
                    CalendarCommonCollection.closePreparedStatement(prep);
                }   
                if (readcon != null && close_connection) {
                    try {
                        DBPool.push(sessionobject.getContext(), readcon);
                    } catch (DBPoolingException dbpe) {
                        LOG.error(ERROR_PUSHING_DATABASE ,dbpe);
                    }
                }
            }
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }             
    }

    public SearchIterator getActiveAppointments(int user_uid, Date start, Date end, int cols[]) throws OXException {
        if (sessionobject != null) {
            Connection readcon = null;
            PreparedStatement prep = null;
            boolean close_connection = true;
            try {
                readcon = DBPool.pickup(sessionobject.getContext());
                cols = CalendarCommonCollection.checkAndAlterCols(cols); 
                CalendarOperation co = new CalendarOperation();
                prep = cimp.getActiveAppointments(sessionobject.getContext(), sessionobject.getUserObject().getId(), start, end, StringCollection.getSelect(cols, DATES_TABLE_NAME), readcon);
                ResultSet rs = cimp.getResultSet(prep);
                co.setResultSet(rs, prep, cols, cimp, readcon, 0, 0, sessionobject); 
                close_connection = false;
                return new CachedCalendarIterator(co);
            } catch(SQLException sqle) {
                throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
            } catch(DBPoolingException dbpe) {
                throw new OXException(dbpe);
            } catch(OXObjectNotFoundException oxonfe) {
                throw oxonfe;
            } catch(OXCalendarException oxc) {
                throw oxc;
            } catch(OXException oxe) {
                throw oxe;                
            } catch(Exception e) {
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, 35);
            } finally {
                if (readcon != null && close_connection) {
                    try {
                        DBPool.push(sessionobject.getContext(), readcon);
                    } catch (DBPoolingException dbpe) {
                        LOG.error(ERROR_PUSHING_DATABASE ,dbpe);
                    }
                }
            }
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }
    }
    
    public SearchIterator getModifiedAppointmentsBetween(int userId, Date start, Date end, int[] cols, Date since, int orderBy, String orderDir) throws OXException, SQLException {
       if (sessionobject != null) {
            Connection readcon = null;
            PreparedStatement prep = null;
            ResultSet rs = null;
            boolean close_connection = true;
            try {
                readcon = DBPool.pickup(sessionobject.getContext());
                cols = CalendarCommonCollection.checkAndAlterCols(cols); 
                CalendarOperation co = new CalendarOperation();
                prep = cimp.getAllAppointmentsForUser(sessionobject.getContext(), sessionobject.getUserObject().getId(), sessionobject.getUserObject().getGroups(), sessionobject.getUserConfiguration(), start, end, StringCollection.getSelect(cols, DATES_TABLE_NAME), readcon, since, orderBy, orderDir);
                rs = cimp.getResultSet(prep);
                co.setResultSet(rs, prep, cols, cimp, readcon, 0, 0, sessionobject); 
                close_connection = false;
                return new CachedCalendarIterator(co);
            } catch(OXPermissionException oxpe) {
                throw oxpe;
            } catch(SQLException sqle) {
                throw new OXCalendarException(OXCalendarException.Code.CALENDAR_SQL_ERROR, sqle);
            } catch(DBPoolingException dbpe) {
                throw new OXException(dbpe);
            } catch(OXCalendarException oxc) {
                throw oxc;
            } catch(OXException oxe) {
                throw oxe;
            } catch(Exception e) {
                throw new OXCalendarException(OXCalendarException.Code.UNEXPECTED_EXCEPTION, e, 36);
            } finally {
                if (close_connection) {
                    CalendarCommonCollection.closeResultSet(rs);
                    CalendarCommonCollection.closePreparedStatement(prep);
                }
                if (readcon != null && close_connection) {
                    try {
                        DBPool.push(sessionobject.getContext(), readcon);
                    } catch (DBPoolingException dbpe) {
                        LOG.error(ERROR_PUSHING_DATABASE ,dbpe);
                    }
                }
            }
        } else {
            throw new OXCalendarException(OXCalendarException.Code.ERROR_SESSIONOBJECT_IS_NULL);
        }          
    }
    
    public SearchIterator getAppointmentsBetween(int user_uid, Date start, Date end, int cols[], int orderBy, String orderDir) throws OXException, SQLException {
        return getModifiedAppointmentsBetween(user_uid, start, end, cols, null, orderBy, orderDir);
    }

    public static final CalendarSqlImp getCalendarSqlImplementation() {
        if (cimp != null){
            return cimp;
        } else {
            LOG.error("No CalendarSqlImp Class found !");
            try {
                cimp = (CalendarSqlImp) Class.forName(default_class).newInstance();
                return cimp;
            } catch(ClassNotFoundException cnfe) {
                LOG.error(cnfe.getMessage(), cnfe);
            } catch (IllegalAccessException iae) {
                LOG.error(iae.getMessage(), iae);
            } catch (InstantiationException ie) {
    		LOG.error(ie.getMessage(), ie);
            }    
            return null;
        }
    }
            
    static {
        try {
            if (cimp == null) {
                CalendarConfig.init();
                String classname = CalendarConfig.getProperty("CalendarSQL");
                if (classname == null) {
                    classname = default_class;
                }
                LOG.debug("Using "+classname+" in CalendarSql");
                cimp = (CalendarSqlImp) Class.forName(classname).newInstance();
            }
        } catch(ConfigurationException ce) {
            LOG.error(ce.getMessage(), ce);
        } catch(ClassNotFoundException cnfe) {
            LOG.error(cnfe.getMessage(), cnfe);
	} catch (IllegalAccessException iae) {
            LOG.error(iae.getMessage(), iae);
	} catch (InstantiationException ie) {
		LOG.error(ie.getMessage(), ie);
	}            
    }
    
    
}



