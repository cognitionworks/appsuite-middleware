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

package com.openexchange.event.impl;

import static com.openexchange.java.Autoboxing.I;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import com.openexchange.api2.OXException;
import com.openexchange.context.ContextService;
import com.openexchange.event.CommonEvent;
import com.openexchange.event.EventException;
import com.openexchange.folder.FolderException;
import com.openexchange.folder.FolderService;
import com.openexchange.group.Group;
import com.openexchange.group.GroupException;
import com.openexchange.group.GroupService;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.server.ServiceException;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.oxfolder.OXFolderPermissionException;

/**
 * EventClient
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 */
public class EventClient {

    private static final Log LOG = LogFactory.getLog(EventClient.class);

    public static final int CREATED = 5;
    public static final int CHANGED = 6;
    public static final int DELETED = 7;
    public static final int MOVED = 8;
    public static final int CONFIRM_ACCEPTED = 9;
    public static final int CONFIRM_DECLINED = 10;
    public static final int CONFIRM_TENTATIVE = 11;

    private final Session session;

    private final int userId;

    private final int contextId;

    public EventClient(final Session session) {
        this.session = session;
        userId = session.getUserId();
        contextId = session.getContextId();
    }

    public void create(final Appointment appointment) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);

        final int folderId = appointment.getParentFolderID();
        if (folderId > 0) {
            final FolderObject folderObj = getFolder(folderId, ctx);
            create(appointment, folderObj);
        }
    }

    public void create(final Appointment appointment, final FolderObject folder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new CalendarObject[] { appointment }, new FolderObject[] { folder });
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.INSERT, Types.APPOINTMENT, appointment, null, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/appointment/insert", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(appointment, CREATED, session);
        EventQueue.add(eventObject);
    }

    public void modify(final Appointment appointment) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);

        final int folderId = appointment.getParentFolderID();
        if (folderId > 0) {
            final FolderObject folderObj = getFolder(folderId, ctx);
            modify(null, appointment, folderObj);
        }
    }

    public void modify(final Appointment oldAppointment, final Appointment newAppointment, final FolderObject folderObj) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new CalendarObject[] { oldAppointment, newAppointment }, new FolderObject[] { folderObj });
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.UPDATE, Types.APPOINTMENT, newAppointment, oldAppointment, folderObj, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/appointment/update", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(newAppointment, CHANGED, session);
        EventQueue.add(eventObject);
    }

    public void accepted(final Appointment appointment) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);

        final int folderId = appointment.getParentFolderID();
        if (folderId > 0) {
            final FolderObject folderObj = getFolder(folderId, ctx);
            accepted(null, appointment, folderObj);
        }
    }

    public void accepted(final Appointment oldAppointment, final Appointment newAppointment, final FolderObject folder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new CalendarObject[] { oldAppointment, newAppointment }, new FolderObject[] { folder });
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.CONFIRM_ACCEPTED, Types.APPOINTMENT, newAppointment, oldAppointment, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/appointment/accepted", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(newAppointment, CONFIRM_ACCEPTED, session);
        EventQueue.add(eventObject);
    }

    public void declined(final Appointment appointment) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);

        final int folderId = appointment.getParentFolderID();
        if (folderId > 0) {
            final FolderObject folderObj = getFolder(folderId, ctx);
            declined(null, appointment, folderObj);
        }
    }

    public void declined(final Appointment oldAppointment, final Appointment newAppointment, final FolderObject folder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new CalendarObject[] { oldAppointment, newAppointment }, new FolderObject[] { folder });
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.CONFIRM_DECLINED, Types.APPOINTMENT, newAppointment, oldAppointment, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/appointment/declined", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(newAppointment, CONFIRM_DECLINED, session);
        EventQueue.add(eventObject);
    }

    public void tentative(final Appointment appointment) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);

        final int folderId = appointment.getParentFolderID();
        if (folderId > 0) {
            final FolderObject folderObj = getFolder(folderId, ctx);
            tentative(null, appointment, folderObj);
        }
    }

    public void tentative(final Appointment oldAppointment, final Appointment newAppointment, final FolderObject folder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new CalendarObject[] { oldAppointment, newAppointment }, new FolderObject[] { folder });
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.CONFIRM_TENTATIVE, Types.APPOINTMENT, newAppointment, oldAppointment, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/appointment/tentative", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(newAppointment, CONFIRM_TENTATIVE, session);
        EventQueue.add(eventObject);
    }

    public void delete(final Appointment appointment) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);

        final int folderId = appointment.getParentFolderID();
        if (folderId > 0) {
            final FolderObject folderObj = getFolder(folderId, ctx);
            delete(appointment, folderObj);
        }
    }

    public void delete(final Appointment appointment, final FolderObject folder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new CalendarObject[] { appointment }, new FolderObject[] { folder });
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.DELETE, Types.APPOINTMENT, appointment, null, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/appointment/delete", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(appointment, DELETED, session);
        EventQueue.add(eventObject);
    }

    public void move(final Appointment appointment, final FolderObject sourceFolder, final FolderObject destinationFolder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new CalendarObject[] { appointment }, new FolderObject[] { sourceFolder, destinationFolder });
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.MOVE, Types.APPOINTMENT, appointment, null, sourceFolder, destinationFolder, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/appointment/move", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(appointment, DELETED, session);
        EventQueue.add(eventObject);
    }

    public void create(final Task task, final FolderObject folder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new CalendarObject[] { task }, new FolderObject[] { folder });
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.INSERT, Types.TASK, task, null, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/task/insert", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(task, CREATED, session);
        EventQueue.add(eventObject);
    }

    public void modify(final Task oldTask, final Task newTask, final FolderObject folder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new CalendarObject[] { oldTask, newTask }, new FolderObject[] { folder });
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.UPDATE, Types.TASK, newTask, oldTask, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/task/update", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(oldTask, CHANGED, session);
        EventQueue.add(eventObject);
    }

    public void accept(Task oldTask, Task newTask) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);

        final int folderId = newTask.getParentFolderID();
        if (folderId > 0) {
            final FolderObject folder = getFolder(folderId, ctx);
            accept(oldTask, newTask, folder);
        }
    }

    public void accept(final Task oldTask, final Task newTask, final FolderObject folder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new CalendarObject[] { oldTask, newTask }, new FolderObject[] { folder });
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.CONFIRM_ACCEPTED, Types.TASK, newTask, oldTask, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/task/accepted", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(oldTask, CONFIRM_ACCEPTED, session);
        EventQueue.add(eventObject);
    }

    public void declined(Task oldTask, Task newTask) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);

        final int folderId = newTask.getParentFolderID();
        if (folderId > 0) {
            final FolderObject folder = getFolder(folderId, ctx);
            declined(oldTask, newTask, folder);
        }
    }

    public void declined(final Task oldTask, final Task newTask, final FolderObject folder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new CalendarObject[] { oldTask, newTask }, new FolderObject[] { folder });
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.CONFIRM_DECLINED, Types.TASK, newTask, oldTask, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/task/declined", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(oldTask, CONFIRM_DECLINED, session);
        EventQueue.add(eventObject);
    }

    public void tentative(Task oldTask, Task newTask) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);

        final int folderId = newTask.getParentFolderID();
        if (folderId > 0) {
            final FolderObject folder = getFolder(folderId, ctx);
            declined(oldTask, newTask, folder);
        }
    }

    public void tentative(final Task oldTask, final Task newTask, final FolderObject folder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new CalendarObject[] { oldTask, newTask }, new FolderObject[] { folder });
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.CONFIRM_TENTATIVE, Types.TASK, newTask, oldTask, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/task/tentative", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(oldTask, CONFIRM_TENTATIVE, session);
        EventQueue.add(eventObject);
    }

    public void delete(final Task task) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);


        final int folderId = task.getParentFolderID();
        if (folderId > 0) {
            final FolderObject folder = getFolder(folderId, ctx);
            delete(task, folder);
        }
    }

    public void delete(final Task task, final FolderObject folder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new CalendarObject[] { task }, new FolderObject[] { folder });
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.DELETE, Types.TASK, task, null, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/task/delete", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(task, DELETED, session);
        EventQueue.add(eventObject);
    }

    public void move(final Task task, final FolderObject sourceFolder, final FolderObject destinationFolder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new CalendarObject[] { task }, new FolderObject[] { sourceFolder, destinationFolder });
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.MOVE, Types.TASK, task, null, sourceFolder, destinationFolder, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/task/move", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(task, DELETED, session);
        EventQueue.add(eventObject);
    }

    public void create(final Contact contact) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);

        final int folderId = contact.getParentFolderID();
        if (folderId > 0) {
            final FolderObject folder = getFolder(folderId, ctx);
            create(contact, folder);
        }
    }

    public void create(final Contact contact, final FolderObject folder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new FolderObject[] { folder }, contact.getParentFolderID());
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.INSERT, Types.CONTACT, contact, null, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/contact/insert", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(contact, CREATED, session);
        EventQueue.add(eventObject);
    }

    public void modify(final Contact oldContact, final Contact newContact, final FolderObject folder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new FolderObject[] { folder }, oldContact.getParentFolderID(), newContact.getParentFolderID());
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.UPDATE, Types.CONTACT, newContact, oldContact, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/contact/update", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(newContact, CHANGED, session);
        EventQueue.add(eventObject);
    }

    public void delete(final Contact contact) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);

        final int folderId = contact.getParentFolderID();
        if (folderId > 0) {
            final FolderObject folder = getFolder(folderId, ctx);
            delete(contact, folder);
        }
    }

    public void delete(final Contact contact, final FolderObject folder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new FolderObject[] { folder }, contact.getParentFolderID());
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.DELETE, Types.CONTACT, contact, null, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/contact/delete", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(contact, DELETED, session);
        EventQueue.add(eventObject);
    }

    public void move(final Contact contact, final FolderObject sourceFolder, final FolderObject destinationFolder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new FolderObject[] { sourceFolder, destinationFolder }, contact.getParentFolderID());
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.MOVE, Types.CONTACT, contact, null, sourceFolder, destinationFolder, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/contact/move", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(contact, MOVED, session);
        EventQueue.add(eventObject);
    }

    public void create(final FolderObject folder) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);

        final int folderId = folder.getParentFolderID();
        if (folderId > 0) {
            final FolderObject parentFolderObj = getFolder(folderId, ctx);
            create(folder, parentFolderObj);
        }
    }

    public void create(final FolderObject folder, final FolderObject parentFolder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new FolderObject[] { folder, parentFolder });
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.INSERT, Types.FOLDER, folder, null, parentFolder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/folder/insert", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(folder, CREATED, session);
        EventQueue.add(eventObject);
    }

    public void modify(final FolderObject oldFolder, final FolderObject newFolder, final FolderObject parentFolder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new FolderObject[] { oldFolder, newFolder, parentFolder }, oldFolder.getParentFolderID(), newFolder.getParentFolderID());
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.UPDATE, Types.FOLDER, newFolder, oldFolder, parentFolder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/folder/update", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(newFolder, CHANGED, session);
        EventQueue.add(eventObject);
    }

    public void delete(final FolderObject folder) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);

        final int folderId = folder.getParentFolderID();
        if (folderId > 0) {
            FolderObject parentFolderObj = null;
            try {
                parentFolderObj = getFolder(folderId, ctx);
            } catch (final OXFolderPermissionException exc) {
                LOG.error("cannot load folder", exc);
            }
            delete(folder, parentFolderObj);
        }
    }

    public void delete(final FolderObject folder, final FolderObject parentFolder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new FolderObject[] { folder, parentFolder });
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.DELETE, Types.FOLDER, folder, null, parentFolder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/folder/delete", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(folder, DELETED, session);
        EventQueue.add(eventObject);
    }

    public void create(final DocumentMetadata document) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);

        final long folderId = document.getFolderId();
        if (folderId > 0) {
            final FolderObject folder = getFolder((int)folderId, ctx);
            create(document, folder);
        }
    }

    public void create(final DocumentMetadata document, final FolderObject folder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new FolderObject[] { folder }, (int) document.getFolderId());
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.INSERT, Types.INFOSTORE, document, null, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/infostore/insert", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(document, CREATED, session);
        EventQueue.add(eventObject);
    }

    public void modify(final DocumentMetadata document) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);

        final long folderId = document.getFolderId();
        if (folderId > 0) {
            final FolderObject folder = getFolder((int)folderId, ctx);
            modify(null, document, folder);
        }
    }

    public void modify(final DocumentMetadata oldDocument, final DocumentMetadata newDocument, final FolderObject folder) throws EventException {
        final Map<Integer, Set<Integer>> affectedUsers;
        if (null == oldDocument) {
            affectedUsers = getAffectedUsers(new FolderObject[] { folder }, (int) newDocument.getFolderId());
        } else {
            affectedUsers = getAffectedUsers(new FolderObject[] { folder }, (int) oldDocument.getFolderId(), (int) newDocument.getFolderId());
        }
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.UPDATE, Types.INFOSTORE, newDocument, oldDocument, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/infostore/update", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(newDocument, CHANGED, session);
        EventQueue.add(eventObject);
    }

    public void delete(final DocumentMetadata document) throws EventException, OXException, ContextException {
        final Context ctx = ContextStorage.getInstance().getContext(contextId);
        //FolderSQLInterface folderSql = new RdbFolderSQLInterface(session, ctx);

        final long folderId = document.getFolderId();
        if (folderId > 0) {
            final FolderObject folder = getFolder((int)folderId, ctx);
            delete(document, folder);
        }
    }

    public void delete(final DocumentMetadata document, final FolderObject folder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new FolderObject[] { folder }, (int) document.getFolderId());
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.DELETE, Types.INFOSTORE, document, null, folder, null, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/infostore/delete", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(document, DELETED, session);
        EventQueue.add(eventObject);
    }

    public void move(final DocumentMetadata document, final FolderObject sourceFolder, final FolderObject destinationFolder) throws EventException {
        Map<Integer, Set<Integer>> affectedUsers = getAffectedUsers(new FolderObject[] { sourceFolder, destinationFolder }, (int) document.getFolderId());
        final CommonEvent genericEvent = new CommonEventImpl(contextId, userId, unmodifyable(affectedUsers), CommonEvent.MOVE, Types.INFOSTORE, document, null, sourceFolder, destinationFolder, session);

        final Dictionary<String, CommonEvent> ht = new Hashtable<String, CommonEvent>();
        ht.put(CommonEvent.EVENT_KEY, genericEvent);

        final Event event = new Event("com/openexchange/groupware/infostore/move", ht);
        triggerEvent(event);

        final EventObject eventObject = new EventObject(document, MOVED, session);
        EventQueue.add(eventObject);
    }

    protected void triggerEvent(final Event event) throws EventException {
        final EventAdmin eventAdmin = ServerServiceRegistry.getInstance().getService(EventAdmin.class);
        if (eventAdmin == null) {
            throw new EventException("event service not available");
        }
        eventAdmin.postEvent(event);
    }

    private FolderObject getFolder(final int folderId, final Context ctx) throws OXException {
        return new OXFolderAccess(ctx).getFolderObject(folderId);
    }

    private Map<Integer, Set<Integer>> getAffectedUsers(FolderObject[] folders, int... folderIds) throws EventException {
        Map<Integer, Set<Integer>> retval = getAffectedUsers(folders);
        for (int folderId : folderIds) {
            getFolderSet(retval, userId).add(I(folderId));
        }
        return retval;
    }

    private Map<Integer, Set<Integer>> getAffectedUsers(FolderObject[] folders) throws EventException {
        Map<Integer, Set<Integer>> retval = new HashMap<Integer, Set<Integer>>();
        retval.put(I(userId), new HashSet<Integer>());
        for (FolderObject folder : folders) {
            try {
                addFolderToAffectedMap(retval, folder);
            } catch (ServiceException e) {
                throw new EventException(e);
            } catch (GroupException e) {
                throw new EventException(e);
            } catch (ContextException e) {
                throw new EventException(e);
            }
        }
        return retval;
    }

    private Map<Integer, Set<Integer>> getAffectedUsers(CalendarObject[] objects, FolderObject[] folders) throws EventException {
        Map<Integer, Set<Integer>> retval = getAffectedUsers(folders);
        for (CalendarObject object : objects) {
            if (null == object) {
                continue;
            }
            getFolderSet(retval, userId).add(I(object.getParentFolderID()));
            UserParticipant[] participants = object.getUsers();
            if (null == participants) {
                continue;
            }
            for (UserParticipant participant : object.getUsers()) {
                final int participantId = participant.getIdentifier();
                if (UserParticipant.NO_ID == participantId) {
                    continue;
                }
                getFolderSet(retval, participantId);
                int folderId = participant.getPersonalFolderId();
                if (UserParticipant.NO_PFID == folderId || 0 == folderId) {
                    continue;
                }
                try {
                    FolderService folderService = ServerServiceRegistry.getInstance().getService(FolderService.class, true);
                    FolderObject folder = folderService.getFolderObject(folderId, contextId);
                    addFolderToAffectedMap(retval, folder);
                } catch (ServiceException e) {
                    throw new EventException(e);
                } catch (FolderException e) {
                    throw new EventException(e);
                } catch (GroupException e) {
                    throw new EventException(e);
                } catch (ContextException e) {
                    throw new EventException(e);
                }
            }
        }
        return retval;
    }

    private void addFolderToAffectedMap(Map<Integer, Set<Integer>> retval, FolderObject folder) throws ServiceException, GroupException, ContextException {
        for (OCLPermission permission : folder.getPermissions()) {
            if (permission.isFolderVisible()) {
                if (permission.isGroupPermission()) {
                    GroupService groupService = ServerServiceRegistry.getInstance().getService(GroupService.class, true);
                    Group group = groupService.getGroup(getContext(contextId), permission.getEntity());
                    for (int groupMember : group.getMember()) {
                        getFolderSet(retval, groupMember).add(I(folder.getObjectID()));
                    }
                } else {
                    getFolderSet(retval, permission.getEntity()).add(I(folder.getObjectID()));
                }
            }
        }
    }

    private static Set<Integer> getFolderSet(Map<Integer, Set<Integer>> map, int userId) {
        Set<Integer> retval = map.get(I(userId));
        if (null == retval) {
            retval = new HashSet<Integer>();
            map.put(I(userId), retval);
        }
        return retval;
    }

    private static Context getContext(int contextId) throws ServiceException, ContextException {
        ContextService contextService = ServerServiceRegistry.getInstance().getService(ContextService.class, true);
        return contextService.getContext(contextId);
    }

    private static Map<Integer, Set<Integer>> unmodifyable(Map<Integer, Set<Integer>> map) {
        for (Entry<Integer, Set<Integer>> entry : map.entrySet()) {
            entry.setValue(Collections.unmodifiableSet(entry.getValue()));
        }
        return Collections.unmodifiableMap(map);
    }
}
