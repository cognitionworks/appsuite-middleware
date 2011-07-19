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

package com.openexchange.caldav;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigCascadeException;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.data.conversion.ical.ICalEmitter;
import com.openexchange.data.conversion.ical.ICalParser;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.groupware.calendar.AppointmentSqlFactoryService;
import com.openexchange.groupware.calendar.CalendarCollectionService;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.CommonObject;
import com.openexchange.groupware.container.DataObject;
import com.openexchange.groupware.container.FolderChildObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserException;
import com.openexchange.session.Session;
import com.openexchange.sessiond.impl.SessionHolder;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.user.UserService;
import com.openexchange.webdav.protocol.WebdavCollection;
import com.openexchange.webdav.protocol.WebdavPath;
import com.openexchange.webdav.protocol.WebdavProtocolException;
import com.openexchange.webdav.protocol.WebdavResource;
import com.openexchange.webdav.protocol.helpers.AbstractCollection;
import com.openexchange.webdav.protocol.helpers.AbstractWebdavFactory;

/**
 * The {@link GroupwareCaldavFactory} holds access to all external groupware services and acts as the factory for CaldavResources and CaldavCollections
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class GroupwareCaldavFactory extends AbstractWebdavFactory {
    
    private static final Log LOG = LogFactory.getLog(GroupwareCaldavFactory.class);
    
    private static final CaldavProtocol PROTOCOL = new CaldavProtocol();

    public static final WebdavPath ROOT_URL = new WebdavPath();

    private SessionHolder sessionHolder;

    private AppointmentSqlFactoryService appointments;

    private FolderService folders;

    private ICalEmitter icalEmitter;

    private ICalParser icalParser;

    private ThreadLocal<State> stateHolder = new ThreadLocal<State>();

    private UserService users;

    private CalendarCollectionService calendarUtils;

    private ConfigViewFactory configs;

    public GroupwareCaldavFactory(SessionHolder sessionHolder, AppointmentSqlFactoryService appointments, FolderService folders, ICalEmitter icalEmitter, ICalParser icalParser, UserService users, CalendarCollectionService calendarUtils, ConfigViewFactory configs) {
        this.sessionHolder = sessionHolder;
        this.appointments = appointments;
        this.folders = folders;
        this.icalEmitter = icalEmitter;
        this.icalParser = icalParser;
        this.users = users;
        this.calendarUtils = calendarUtils;
        this.configs = configs;
    }

    @Override
    public void beginRequest() {
        super.beginRequest();
        stateHolder.set(new State(this));
    }

    @Override
    public void endRequest(int status) {
        stateHolder.set(null);
        super.endRequest(status);
    }

    public CaldavProtocol getProtocol() {
        return PROTOCOL;
    }

    public WebdavCollection resolveCollection(WebdavPath url) throws WebdavProtocolException {
        if (url.size() > 1) {
            throw new WebdavProtocolException(url, 404);
        }
        if (isRoot(url)) {
            return mixin(new RootCollection(this));
        }
        return resolveChild(resolveCollection(url.parent()), url);
    }

    public WebdavCollection resolveChild(WebdavCollection collection, WebdavPath url) throws WebdavProtocolException {
        for (WebdavResource resource : collection) {
            if (resource.getUrl().equals(url)) {
                return mixin((AbstractCollection) resource);
            }
        }
        throw new WebdavProtocolException(url, 404);
    }

    // TODO: i18n

    public boolean isRoot(WebdavPath url) {
        return url.size() == 0;
    }

    public WebdavResource resolveResource(WebdavPath url) throws WebdavProtocolException {
        if (url.size() == 2) {
            return mixin(((CaldavCollection) resolveCollection(url.parent())).getChild(url.name()));
        }
        return resolveCollection(url);
    }

    public AppointmentSQLInterface getAppointmentInterface() {
        return appointments.createAppointmentSql(sessionHolder.getSessionObject());
    }

    public FolderService getFolderService() {
        return folders;
    }

    public Session getSession() {
        return sessionHolder.getSessionObject();
    }

    public ICalEmitter getIcalEmitter() {
        return icalEmitter;
    }

    public ICalParser getIcalParser() {
        return icalParser;
    }

    public Context getContext() {
        return sessionHolder.getContext();
    }

    public String getLoginName() {
        return sessionHolder.getSessionObject().getLoginName();
    }

    public SessionHolder getSessionHolder() {
        return sessionHolder;
    }

    public CalendarCollectionService getCalendarUtilities() {
        return calendarUtils;
    }
    
    public String getConfigValue(String key, String defaultValue) throws WebdavProtocolException {
        try {
            ConfigView view = configs.getView(sessionHolder.getUser().getId(), sessionHolder.getContext().getContextId());
            ComposedConfigProperty<String> property = view.property(key, String.class);
            if (!property.isDefined()) {
                return defaultValue;
            }
            return property.get();
        } catch (ConfigCascadeException e) {
            LOG.error(e.getMessage(), e);
            throw new WebdavProtocolException(new WebdavPath(), 500);
        }
    }

    public State getState() {
        return stateHolder.get();
    }
    
    public boolean isInRange(Appointment appointment) throws WebdavProtocolException {
        Date start = start();
        Date end = end();
        for(Date d : Arrays.asList(appointment.getStartDate(), appointment.getEndDate())) {
            if (!d.after(start) || !d.before(end)) {
                return false;
            }
        }
        return true;
    }
    
    public Date end() throws WebdavProtocolException {
        String value = getConfigValue("com.openexchange.caldav.interval.end", "one_year");
        int addYears = 2;
        if (value.equals("two_years")) {
            addYears = 3;
        }
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.YEAR, addYears);
        instance.set(Calendar.DAY_OF_YEAR, 0);
        instance.set(Calendar.HOUR_OF_DAY, 0);
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        instance.set(Calendar.MILLISECOND, 0);

        return instance.getTime();
    }

    public Date start() throws WebdavProtocolException {
        String value = getConfigValue("com.openexchange.caldav.interval.start", "one_month");

        if (value.equals("one_year")) {
            Calendar instance = Calendar.getInstance();
            instance.add(Calendar.YEAR, -1);
            instance.set(Calendar.DAY_OF_YEAR, 0);
            instance.set(Calendar.HOUR_OF_DAY, 0);
            instance.set(Calendar.MINUTE, 0);
            instance.set(Calendar.SECOND, 0);
            instance.set(Calendar.MILLISECOND, 0);

            return instance.getTime();
        }

        Calendar instance = Calendar.getInstance();
        
        if (value.equals("six_months")) {
            instance.add(Calendar.MONTH, -6);
        } else {
            instance.add(Calendar.MONTH, -1);
        }
        instance.set(Calendar.DAY_OF_MONTH, 1);
        instance.set(Calendar.HOUR_OF_DAY, 0);
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        instance.set(Calendar.MILLISECOND, 0);
        return instance.getTime();

    }


    public User resolveUser(int uid) throws WebdavProtocolException {
        try {
            return users.getUser(uid, getContext());
        } catch (UserException e) {
            LOG.error(e.getMessage(), e);
            throw new WebdavProtocolException(new WebdavPath(), 500);
        }
    }

    public static final class State {
        
        private final static int[] FIELDS_FOR_ALL_REQUEST = {
            DataObject.OBJECT_ID
        };
        
        private final static int[] APPOINTMENT_FIELDS = {
            DataObject.OBJECT_ID, DataObject.CREATED_BY, DataObject.CREATION_DATE, DataObject.LAST_MODIFIED, DataObject.MODIFIED_BY,
            FolderChildObject.FOLDER_ID, CommonObject.PRIVATE_FLAG, CommonObject.CATEGORIES, CalendarObject.TITLE, Appointment.LOCATION,
            CalendarObject.START_DATE, CalendarObject.END_DATE, CalendarObject.NOTE, CalendarObject.RECURRENCE_TYPE,
            CalendarObject.RECURRENCE_CALCULATOR, CalendarObject.RECURRENCE_ID, CalendarObject.PARTICIPANTS, CalendarObject.USERS,
            Appointment.SHOWN_AS, Appointment.FULL_TIME, Appointment.COLOR_LABEL, Appointment.TIMEZONE, Appointment.UID,
            Appointment.SEQUENCE, Appointment.ORGANIZER, Appointment.CONFIRMATIONS };

        private GroupwareCaldavFactory factory;

        public State(GroupwareCaldavFactory factory) {
            this.factory = factory;
        }

        private Map<String, Appointment> appointmentCache = new HashMap<String, Appointment>();

        private Map<String, List<Appointment>> changeExceptionCache = new HashMap<String, List<Appointment>>();

        private Map<Integer, List<Appointment>> folderCache = new HashMap<Integer, List<Appointment>>();
        
        private Set<Integer> patchGuard = new HashSet<Integer>();
        
        public void cacheFolder(int folderId) {
            cacheFolderFast(folderId); // Switch this to the other method, once it loads participants
        }
        
        public void cacheFolderSlow(int folderId) {
            if (folderCache.containsKey(folderId)) {
                return;
            }
            AppointmentSQLInterface calendar = factory.getAppointmentInterface();
            try {
                SearchIterator<Appointment> iterator = calendar.getAppointmentsBetweenInFolder(
                    folderId,
                    FIELDS_FOR_ALL_REQUEST,
                    factory.start(),
                    factory.end(),
                    -1,
                    null);
                List<Appointment> children = new LinkedList<Appointment>();
                while (iterator.hasNext()) {
                    Appointment appointment = iterator.next();
                    if (appointment.getUid() == null) {
                        continue; // skip
                    }
                    appointment = calendar.getObjectById(appointment.getObjectID(), folderId);
                    if (appointment.isException()) {
                        List<Appointment> list = changeExceptionCache.get(appointment.getUid());
                        if (list == null) {
                            list = new LinkedList<Appointment>();
                            changeExceptionCache.put(appointment.getUid(), list);
                        }
                        list.add(appointment);
                    } else {
                        appointmentCache.put(appointment.getUid(), appointment);
                    }
                    children.add(appointment);
                }
                folderCache.put(folderId, children);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        
        public void cacheFolderFast(int folderId) {
            if (folderCache.containsKey(folderId)) {
                return;
            }
            AppointmentSQLInterface calendar = factory.getAppointmentInterface();
            try {
                SearchIterator<Appointment> iterator = calendar.getAppointmentsBetweenInFolder(
                    folderId,
                    APPOINTMENT_FIELDS,
                    factory.start(),
                    factory.end(),
                    -1,
                    null);
                List<Appointment> children = new LinkedList<Appointment>();
                while (iterator.hasNext()) {
                    Appointment appointment = iterator.next();
                    if (appointment.getUid() == null) {
                        continue; // skip
                    }
                    if (appointment.isException()) {
                        List<Appointment> list = changeExceptionCache.get(appointment.getUid());
                        if (list == null) {
                            list = new LinkedList<Appointment>();
                            changeExceptionCache.put(appointment.getUid(), list);
                        }
                        list.add(appointment);
                    } else {
                        appointmentCache.put(appointment.getUid(), appointment);
                    }
                    children.add(appointment);
                }
                folderCache.put(folderId, children);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                
            }
        }

        public Appointment get(String uid, int folderId) {
            cacheFolder(folderId);
            Appointment appointment = appointmentCache.get(uid);
            if (appointment == null) {
                appointment = appointmentCache.get(URLDecoder.decode(uid));
            }
            if (appointment != null && appointment.getParentFolderID() != folderId) {
                return null; // anyway, because this uid is in another folder.
            }
            return appointment;
        }

        public List<Appointment> getChangeExceptions(String uid, int folderId) {
            cacheFolder(folderId);
            List<Appointment> exceptions = changeExceptionCache.get(uid);
            if (exceptions == null) {
                return Collections.emptyList();
            }
            return exceptions;
        }

        

        public List<Appointment> getFolder(int id) {
            cacheFolder(id);
            List<Appointment> appointments = folderCache.get(id);
            if (appointments == null) {
                return Collections.emptyList();
            }
            return appointments;
        }

        public boolean hasBeenPatched(Appointment appointment) {
            return patchGuard.contains(appointment.getObjectID());
        }

        public void markAsPatched(Appointment appointment) {
            patchGuard.add(appointment.getObjectID());
        }

    }


    

}
