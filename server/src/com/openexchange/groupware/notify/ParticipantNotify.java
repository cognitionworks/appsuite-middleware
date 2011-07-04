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

package com.openexchange.groupware.notify;

import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.data.conversion.ical.ConversionError;
import com.openexchange.data.conversion.ical.ConversionWarning;
import com.openexchange.data.conversion.ical.ICalEmitter;
import com.openexchange.data.conversion.ical.ICalSession;
import com.openexchange.data.conversion.ical.ITipContainer;
import com.openexchange.data.conversion.ical.ITipMethod;
import com.openexchange.database.DBPoolingException;
import com.openexchange.event.impl.AppointmentEventInterface2;
import com.openexchange.event.impl.TaskEventInterface2;
import com.openexchange.group.Group;
import com.openexchange.group.GroupStorage;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.calendar.CalendarCollectionService;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.calendar.Constants;
import com.openexchange.groupware.calendar.OXCalendarException;
import com.openexchange.groupware.calendar.RecurringResultInterface;
import com.openexchange.groupware.calendar.RecurringResultsInterface;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.ExternalUserParticipant;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.container.mail.MailObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.i18n.Notifications;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserException;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.notify.NotificationConfig.NotificationProperty;
import com.openexchange.groupware.notify.State.Type;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.groupware.userconfiguration.RdbUserConfigurationStorage;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.i18n.tools.RenderMap;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.i18n.tools.StringTemplate;
import com.openexchange.i18n.tools.Template;
import com.openexchange.i18n.tools.TemplateReplacement;
import com.openexchange.i18n.tools.TemplateToken;
import com.openexchange.i18n.tools.replacement.AppointmentActionReplacement;
import com.openexchange.i18n.tools.replacement.ChangeExceptionsReplacement;
import com.openexchange.i18n.tools.replacement.CommentsReplacement;
import com.openexchange.i18n.tools.replacement.ConfirmationActionReplacement;
import com.openexchange.i18n.tools.replacement.CreationDateReplacement;
import com.openexchange.i18n.tools.replacement.DeleteExceptionsReplacement;
import com.openexchange.i18n.tools.replacement.EndDateReplacement;
import com.openexchange.i18n.tools.replacement.FolderReplacement;
import com.openexchange.i18n.tools.replacement.FormatLocalizedStringReplacement;
import com.openexchange.i18n.tools.replacement.ParticipantsReplacement;
import com.openexchange.i18n.tools.replacement.ResourcesReplacement;
import com.openexchange.i18n.tools.replacement.SeriesReplacement;
import com.openexchange.i18n.tools.replacement.StartDateReplacement;
import com.openexchange.i18n.tools.replacement.StringReplacement;
import com.openexchange.i18n.tools.replacement.TaskActionReplacement;
import com.openexchange.i18n.tools.replacement.TaskPriorityReplacement;
import com.openexchange.i18n.tools.replacement.TaskStatusReplacement;
import com.openexchange.mail.MailException;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.datasource.MessageDataSource;
import com.openexchange.mail.mime.utils.MIMEMessageUtility;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.resource.Resource;
import com.openexchange.resource.storage.ResourceStorage;
import com.openexchange.server.ServiceException;
import com.openexchange.server.impl.EffectivePermission;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.TimeZoneUtils;
import com.openexchange.tools.exceptions.LoggingLogic;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;

public class ParticipantNotify implements AppointmentEventInterface2, TaskEventInterface2 {

    // TODO: Signature?

    private static final String STR_UNKNOWN = "UNKNOWN";

    private final static Log LOG = com.openexchange.exception.Log.valueOf(LogFactory.getLog(ParticipantNotify.class));

    private static final boolean DEBUG = LOG.isDebugEnabled();

    private final static LoggingLogic LL = LoggingLogic.getLoggingLogic(ParticipantNotify.class);

    public static ParticipantNotify messageSender = new ParticipantNotify();

    /**
     * Initializes a new {@link ParticipantNotify}
     */
    public ParticipantNotify() {
        super();
    }

    /**
     * Sends specified message
     * 
     * @param mmsg The message
     * @param session The session
     * @param obj The calendar object
     * @param state The state
     */
    protected static void sendMessage(final MailMessage mmsg, final ServerSession session, final CalendarObject obj, final State state) {
        messageSender.sendMessage(mmsg, session, obj, state, false);
    }

    protected void sendMessage(final MailMessage msg, final ServerSession session, final CalendarObject obj, final State state, final boolean suppressOXReminderHeader) {
        if (DEBUG) {
            String message;
            if (Multipart.class.isInstance(msg.message)) {
                try {
                    message = ((Multipart) msg.message).getBodyPart(0).getContent().toString() + "\n\n(With ICal attached)";
                } catch (final Exception e) {
                    message = "";
                }
            } else {
                message = msg.message.toString();
            }
            LOG.debug(new StringBuilder().append("Sending message to: ").append(msg.addresses).append("\n=====[").append(msg.title).append(
                "]====\n\n").append(message).append("\n\n"));
        }

        int fuid = msg.folderId;
        if (fuid == -1) {
            fuid = obj.getParentFolderID();
        }

        if (suppressOXReminderHeader) {
            fuid = MailObject.DONT_SET;
        }
        final String type = (msg.overrideType != null) ? msg.overrideType.toString() : state.getType().toString();
        final MailObject mail = new MailObject(session, obj.getObjectID(), fuid, state.getModule(), type);

        String fromAddr;
        final String senderSource = NotificationConfig.getProperty(NotificationProperty.FROM_SOURCE, "primaryMail");
        if (senderSource.equals("defaultSenderAddress")) {
            try {
                fromAddr = getUserSettingMail(session.getUserId(), session.getContext()).getSendAddr();
            } catch (final OXException e) {
                LOG.error(e.getMessage(), e);
                fromAddr = UserStorage.getStorageUser(session.getUserId(), session.getContext()).getMail();
            }
        } else {
            fromAddr = UserStorage.getStorageUser(session.getUserId(), session.getContext()).getMail();
        }

        final User sender = UserStorage.getStorageUser(session.getUserId(), session.getContext());

        if (sender != null) {
            mail.setFromAddr("\"" + sender.getDisplayName() + "\"" + " <" + fromAddr + ">");
        } else {
            mail.setFromAddr(fromAddr);
        }

        mail.setToAddrs(msg.addresses.toArray(new String[msg.addresses.size()]));
        mail.setText(msg.message);
        mail.setSubject(msg.title);

        if (Multipart.class.isInstance(msg.message)) {
            mail.setContentType("multipart/alternative");
        } else {
            mail.setContentType("text/plain; charset=UTF-8");
        }

        if (state.getModule() == Types.TASK) {
            if (msg.internal) {
                state.modifyInternal(mail, obj, session);
            } else {
                state.modifyExternal(mail, obj, session);
            }
        }

        try {
            mail.send();
        } catch (final MailException e) {
            LL.log(e);
        }
    }

    // Override for testing

    protected Set<Integer> loadAllUsersSet(final Context ctx) throws UserException {
        final int[] uids = UserStorage.getInstance().listAllUser(ctx);
        final Set<Integer> allIds = new HashSet<Integer>(uids.length);
        for (final int id : uids) {
            allIds.add(Integer.valueOf(id));
        }
        return allIds;
    }

    protected User[] resolveUsers(final Context ctx, final int... ids) throws LdapException {
        final User[] r = new User[ids.length];
        for (int i = 0; i < ids.length; i++) {
            r[i] = UserStorage.getInstance().getUser(ids[i], ctx);
        }
        return r;
    }

    protected Group[] resolveGroups(final Context ctx, final int... ids) throws LdapException {
        final GroupStorage groups = GroupStorage.getInstance();
        final Group[] r = new Group[ids.length];
        int i = 0;
        for (final int id : ids) {
            r[i++] = groups.getGroup(id, ctx);
        }
        return r;
    }

    protected Resource[] resolveResources(final Context ctx, final int... ids) throws LdapException {
        final ResourceStorage resources = ResourceStorage.getInstance();
        final Resource[] r = new Resource[ids.length];
        int i = 0;
        for (final int id : ids) {
            r[i++] = resources.getResource(id, ctx);
        }
        return r;
    }

    protected UserConfiguration getUserConfiguration(final int id, final int[] groups, final Context context) throws SQLException, LdapException, DBPoolingException, OXException {
        return RdbUserConfigurationStorage.loadUserConfiguration(id, groups, context);
    }

    protected UserSettingMail getUserSettingMail(final int id, final Context context) throws OXException {
        return UserSettingMailStorage.getInstance().loadUserSettingMail(id, context);
    }

    public void appointmentCreated(final Appointment appointmentObj, final Session session) {
        int folderOwner = session.getUserId();
        try {
            folderOwner = getFolderOwner(appointmentObj, new ServerSessionAdapter(session));
        } catch (final ContextException e) {
            LL.log(e);
        }
        sendNotification(null, appointmentObj, session, new AppointmentState(new AppointmentActionReplacement(
            AppointmentActionReplacement.ACTION_NEW), folderOwner == session.getUserId() ? Notifications.APPOINTMENT_CREATE_MAIL : Notifications.APPOINTMENT_CREATE_MAIL_ON_BEHALF, State.Type.NEW), false, false, false);
    }

    public void appointmentModified(final Appointment appointmentObj, final Session session) {
        sendNotification(null, appointmentObj, session, new AppointmentState(new AppointmentActionReplacement(
            AppointmentActionReplacement.ACTION_CHANGED), Notifications.APPOINTMENT_UPDATE_MAIL, State.Type.MODIFIED), false, false, true);
    }

    public void appointmentModified(final Appointment oldAppointment, final Appointment newAppointment, final Session session) {
        sendNotification(oldAppointment, newAppointment, session, new AppointmentState(new AppointmentActionReplacement(
            AppointmentActionReplacement.ACTION_CHANGED), Notifications.APPOINTMENT_UPDATE_MAIL, State.Type.MODIFIED), false, false, true);
    }

    public void appointmentAccepted(final Appointment appointmentObj, final Session session) {
        sendNotification(null, appointmentObj, session, new AppointmentState(
            new AppointmentActionReplacement(AppointmentActionReplacement.ACTION_ACCEPTED),
            new ConfirmationActionReplacement(ConfirmationActionReplacement.ACTION_ACCEPTED),
            Notifications.APPOINTMENT_CONFIRMATION_MAIL,
            State.Type.ACCEPTED), false, false, false);
    }

    public void appointmentDeclined(final Appointment appointmentObj, final Session session) {
        sendNotification(null, appointmentObj, session, new AppointmentState(
            new AppointmentActionReplacement(AppointmentActionReplacement.ACTION_DECLINED),
            new ConfirmationActionReplacement(ConfirmationActionReplacement.ACTION_DECLINED),
            Notifications.APPOINTMENT_CONFIRMATION_MAIL,
            State.Type.DECLINED), false, false, false);
    }

    public void appointmentTentativelyAccepted(final Appointment appointmentObj, final Session session) {
        sendNotification(null, appointmentObj, session, new AppointmentState(
            new AppointmentActionReplacement(AppointmentActionReplacement.ACTION_TENTATIVE),
            new ConfirmationActionReplacement(ConfirmationActionReplacement.ACTION_TENTATIVELY_ACCEPTED),
            Notifications.APPOINTMENT_CONFIRMATION_MAIL,
            State.Type.TENTATIVELY_ACCEPTED), false, false, false);
    }

    public void appointmentDeleted(final Appointment appointmentObj, final Session session) {
        /*
         * Clear calendar object from notification pool
         */
        NotificationPool.getInstance().removeByObject(appointmentObj.getObjectID(), session.getContextId());
        /*
         * Send delete notification
         */
        sendNotification(
            null,
            appointmentObj,
            session,
            new AppointmentState(
                new AppointmentActionReplacement(AppointmentActionReplacement.ACTION_DELETED),
                Notifications.APPOINTMENT_DELETE_MAIL,
                State.Type.DELETED),
            NotificationConfig.getPropertyAsBoolean(NotificationProperty.NOTIFY_ON_DELETE, false),
            true,
            false);
    }

    public void taskCreated(final Task taskObj, final Session session) {
        sendNotification(null, taskObj, session, new TaskState(
            new TaskActionReplacement(TaskActionReplacement.ACTION_NEW),
            Notifications.TASK_CREATE_MAIL,
            State.Type.NEW), false, false, false);
    }

    public void taskModified(final Task taskObj, final Session session) {
        sendNotification(null, taskObj, session, new TaskState(
            new TaskActionReplacement(TaskActionReplacement.ACTION_CHANGED),
            Notifications.TASK_UPDATE_MAIL,
            State.Type.MODIFIED), false, false, true);

    }

    public void taskModified(final Task oldTask, final Task newTask, final Session session) {
        sendNotification(oldTask, newTask, session, new TaskState(
            new TaskActionReplacement(TaskActionReplacement.ACTION_CHANGED),
            Notifications.TASK_UPDATE_MAIL,
            State.Type.MODIFIED), false, false, true);
    }

    public void taskAccepted(final Task taskObj, final Session session) {
        sendNotification(null, taskObj, session, new TaskState(
            new TaskActionReplacement(TaskActionReplacement.ACTION_ACCEPTED),
            new ConfirmationActionReplacement(ConfirmationActionReplacement.ACTION_ACCEPTED),
            Notifications.TASK_CONFIRMATION_MAIL,
            State.Type.ACCEPTED), false, false, false);
    }

    public void taskDeclined(final Task taskObj, final Session session) {
        sendNotification(null, taskObj, session, new TaskState(
            new TaskActionReplacement(TaskActionReplacement.ACTION_DECLINED),
            new ConfirmationActionReplacement(ConfirmationActionReplacement.ACTION_DECLINED),
            Notifications.TASK_CONFIRMATION_MAIL,
            State.Type.DECLINED), false, false, false);
    }

    public void taskTentativelyAccepted(final Task taskObj, final Session session) {
        sendNotification(null, taskObj, session, new TaskState(
            new TaskActionReplacement(TaskActionReplacement.ACTION_TENTATIVE),
            new ConfirmationActionReplacement(ConfirmationActionReplacement.ACTION_TENTATIVELY_ACCEPTED),
            Notifications.TASK_CONFIRMATION_MAIL,
            State.Type.TENTATIVELY_ACCEPTED), false, false, false);
    }

    public void taskDeleted(final Task taskObj, final Session session) {
        /*
         * Clear calendar object from notification pool
         */
        NotificationPool.getInstance().removeByObject(taskObj.getObjectID(), session.getContextId());
        /*
         * Send delete notification
         */
        sendNotification(null, taskObj, session, new TaskState(
            new TaskActionReplacement(TaskActionReplacement.ACTION_DELETED),
            Notifications.TASK_DELETE_MAIL,
            State.Type.DELETED), NotificationConfig.getPropertyAsBoolean(NotificationProperty.NOTIFY_ON_DELETE, false), true, false);
    }

    /**
     * TODO new object should have all necessary data when coming through the event system.
     */
    private void sendNotification(final CalendarObject oldObj, final CalendarObject newObj, final Session session, final State state, final boolean forceNotifyOthers, final boolean suppressOXReminderHeader, final boolean isUpdate) {

        if (onlyIrrelevantFieldsChanged(session, oldObj, newObj, state)) {
            return;
        }

        final ServerSession serverSession;
        try {
            serverSession = new ServerSessionAdapter(session);
        } catch (final ContextException e) {
            LOG.error(e.getMessage(), e);
            return;
        }
        /*
         * Remember object's title
         */
        final String title = newObj.getTitle() == null ? (oldObj == null ? "" : (oldObj.getTitle() == null ? "" : oldObj.getTitle())) : newObj.getTitle();
        /*
         * Check if notification shall be dropped
         */
        if (newObj.containsNotification() && !newObj.getNotification() && newObj.getCreatedBy() == serverSession.getUserId() && !forceNotifyOthers) {
            if (DEBUG) {
                LOG.debug(new StringBuilder(256).append("Dropping notification for ").append(
                    (state.getModule() == Types.APPOINTMENT ? "appointment " : "task ")).append(title).append(" (").append(
                    newObj.getObjectID()).append(") since it indicates to discard its notification").toString());
            }
            return;
        }
        if (newObj.getParticipants() == null) {
            if (oldObj == null || oldObj.getParticipants() == null) {
                if (DEBUG) {
                    final StringBuilder builder = new StringBuilder(256).append("Dropping notification for ").append(
                        (state.getModule() == Types.APPOINTMENT ? "appointment " : "task ")).append(title).append(" (").append(
                        newObj.getObjectID()).append(") since it contains NO participants");
                    LOG.debug(builder.toString());
                }
                return;
            }
            /*
             * Grab participants/users from old object
             */
            newObj.setParticipants(oldObj.getParticipants());
            newObj.setUsers(oldObj.getUsers());
        }
        /*
         * Ensure start/end is set
         */
        if (newObj.getStartDate() == null && oldObj != null && oldObj.getStartDate() != null) {
            newObj.setStartDate(oldObj.getStartDate());
        }
        if (newObj.getEndDate() == null && oldObj != null && oldObj.getEndDate() != null) {
            newObj.setEndDate(oldObj.getEndDate());
        }
        if (CalendarObject.NO_RECURRENCE == newObj.getRecurrenceType() && oldObj != null && CalendarObject.NO_RECURRENCE != oldObj.getRecurrenceType()) {
            newObj.setRecurrenceType(oldObj.getRecurrenceType());
            if (oldObj.containsOccurrence()) {
                newObj.setOccurrence(oldObj.getOccurrence());
            }
            if (oldObj.containsInterval()) {
                newObj.setInterval(oldObj.getInterval());
            }
            if (oldObj.containsDays()) {
                newObj.setDays(oldObj.getDays());
            }
            if (oldObj.containsDayInMonth()) {
                newObj.setDayInMonth(oldObj.getDayInMonth());
            }
            if (oldObj.containsUntil()) {
                newObj.setUntil(oldObj.getUntil());
            }
        }
        if (!checkStartAndEndDate(newObj, state.getModule())) {
            return;
        }
        /*
         * Ensure that important fields are set
         */
        if (!newObj.containsCreatedBy() && oldObj != null && oldObj.containsCreatedBy()) {
            newObj.setCreatedBy(oldObj.getCreatedBy());
        }
        if (!newObj.containsCreationDate() && oldObj != null && oldObj.containsCreationDate()) {
            newObj.setCreationDate(oldObj.getCreationDate());
        }
        if (Types.APPOINTMENT == state.getModule()) {
            final Appointment newApp = (Appointment) newObj;
            final Appointment oldApp = oldObj == null ? null : ((Appointment) oldObj);

            if (!newApp.containsFullTime() && oldApp != null && oldApp.containsFullTime()) {
                newApp.setFullTime(oldApp.getFullTime());
            }

            // Set correct recurrence information if CalendarObject is an Appointment
            if (newApp.getRecurrenceType() != Appointment.NO_RECURRENCE) {
                try {
                    ServerServiceRegistry.getInstance().getService(CalendarCollectionService.class, true).fillDAO(
                        (CalendarDataObject) newApp);
                    if (oldObj != null) {
                        ServerServiceRegistry.getInstance().getService(CalendarCollectionService.class, true).fillDAO(
                            (CalendarDataObject) oldApp);
                    }
                } catch (final Exception e) {
                    if (e instanceof ServiceException || e instanceof OXException) {
                        final StringBuilder builder = new StringBuilder(256).append(
                            "Could not set correct recurrence information in notification for appointment").append(title).append(" (").append(
                            newObj.getObjectID()).append("). Cause:\n");
                        LOG.error(builder.toString() + e.getMessage(), e);
                    }
                }
            }
        }

        /*
         * A map to remember receivers
         */
        final Map<Locale, List<EmailableParticipant>> receivers = new HashMap<Locale, List<EmailableParticipant>>();
        /*
         * Generate a render map filled with object-specific information
         */
        final RenderMap renderMap = createRenderMap(newObj, oldObj, isUpdate, title, state, receivers, serverSession);

        /*
         * Add confirmation action replacement to render map if non-null
         */
        {
            final TemplateReplacement confirmActionRepl = state.getConfirmationAction();
            if (confirmActionRepl != null) {
                renderMap.put(confirmActionRepl);
            }
        }

        /*
         * Create message list
         */
        final List<MailMessage> messages = createMessageList(
            oldObj,
            newObj,
            state,
            forceNotifyOthers,
            isUpdate,
            serverSession,
            receivers,
            title,
            renderMap);

        /*
         * Send messages
         */
        for (final MailMessage mmsg : messages) {
            sendMessage(mmsg, serverSession, newObj, state, suppressOXReminderHeader);
        }
    }

    private boolean onlyIrrelevantFieldsChanged(final Session session, final CalendarObject oldObj, final CalendarObject newObj, final State state) {
        if (oldObj == null || newObj == null) {
            return false;
        }
        return state.onlyIrrelevantFieldsChanged(oldObj, newObj);
    }

    private List<MailMessage> createMessageList(final CalendarObject oldObj, final CalendarObject newObj, final State state, final boolean forceNotifyOthers, final boolean isUpdate, final ServerSession session, final Map<Locale, List<EmailableParticipant>> receivers, final String title, final RenderMap renderMap) {
        final OXFolderAccess access = new OXFolderAccess(session.getContext());
        final StringBuilder b = new StringBuilder(2048);
        Set<Integer> allUserIds = null;
        try {
            allUserIds = loadAllUsersSet(session.getContext());
        } catch (final UserException ue) {
            LL.log(ue);
            return Collections.emptyList();
        }

        final List<MailMessage> messages = new ArrayList<MailMessage>();
        for (final Map.Entry<Locale, List<EmailableParticipant>> entry : receivers.entrySet()) {
            final Locale locale = entry.getKey();
            /*
             * Apply new locale to replacements
             */
            final TemplateReplacement actionRepl = state.getAction();
            actionRepl.setLocale(locale);
            renderMap.applyLocale(locale);

            /*
             * Iterate over locale's participants
             */
            final List<EmailableParticipant> participants = entry.getValue();
            for (final EmailableParticipant p : participants) {
                TimeZone tz = TimeZone.getDefault();
                boolean sendMail = true;

                if (p.type != Participant.EXTERNAL_USER && allUserIds.contains(Integer.valueOf(p.id))) {
                    try {
                        sendMail = !p.ignoreNotification && state.sendMail(
                            getUserSettingMail(p.id, session.getContext()),
                            newObj.getCreatedBy(),
                            p.id,
                            session.getUserId()) && ((!newObj.containsNotification() || newObj.getNotification()) || (forceNotifyOthers && p.id != session.getUserId()));
                        tz = p.timeZone;
                    } catch (final AbstractOXException e) {
                        LL.log(e);
                    }
                } else {
                    sendMail = !p.ignoreNotification && (!newObj.containsNotification() || newObj.getNotification()) || (newObj.getModifiedBy() != p.id && forceNotifyOthers);
                    sendMail = sendMail && (!EnumSet.of(State.Type.ACCEPTED, State.Type.DECLINED, State.Type.TENTATIVELY_ACCEPTED).contains(
                        state.getType()) || p.email.equals(newObj.getOrganizer()));
                    if (p.timeZone != null) {
                        tz = p.timeZone;
                    }
                }

                if (sendMail) {
                    /*
                     * Apply time zone
                     */
                    renderMap.applyTimeZone(tz);

                    /*
                     * Folder
                     */
                    final int folderId = p.folderId > 0 ? p.folderId : newObj.getParentFolderID();
                    if (folderId > 0) {
                        final String folderName = getFolderName(folderId, locale, access);
                        final FolderReplacement folderRepl = new FolderReplacement(folderName);
                        folderRepl.setLocale(locale);
                        if (oldObj != null) {
                            if (p.folderId > 0) {
                                checkChangedFolder(oldObj, p.email, folderId, folderRepl, session);
                            } else {
                                folderRepl.setChanged(newObj.getParentFolderID() != oldObj.getParentFolderID());
                            }
                        }
                        renderMap.put(folderRepl);
                    }

                    /*
                     * Special information(s)
                     */
                    state.addSpecial(newObj, oldObj, renderMap, p);

                    if (isUpdate && EmailableParticipant.STATE_NONE == p.state) {
                        /*
                         * Add to pool
                         */
                        NotificationPool.getInstance().put(
                            new PooledNotification(p, title, state, locale, (RenderMap) renderMap.clone(), session, newObj));
                        if (DEBUG) {
                            LOG.debug(new StringBuilder(128).append((Types.APPOINTMENT == state.getModule() ? "Appointment" : "Task")).append(
                                " update (id = ").append(newObj.getObjectID()).append(") notification added to pool for receiver ").append(
                                p.email).toString());
                        }
                    } else {
                        /*
                         * Compose message
                         */
                        MailMessage message = null;
                        if (Participant.USER == p.type) {
                            message = createUserMessage(
                                session,
                                newObj,
                                p,
                                (userCanReadObject(p, newObj, session)),
                                title,
                                actionRepl,
                                state,
                                locale,
                                renderMap,
                                isUpdate,
                                b);
                        } else {
                            message = createParticipantMessage(session, newObj, p, title, actionRepl, state, locale, renderMap, isUpdate, b);
                        }
                        messages.add(message);
                        if (DEBUG) {
                            LOG.debug(new StringBuilder(128).append((Types.APPOINTMENT == state.getModule() ? "Appointment" : "Task")).append(
                                " (id = ").append(newObj.getObjectID()).append(") \"").append(
                                EmailableParticipant.STATE_NEW == p.state ? "New" : (EmailableParticipant.STATE_REMOVED == p.state ? "Deleted" : state.getType().toString())).append(
                                "\" notification message generated for receiver ").append(p.email).toString());
                        }
                    }
                }
            }
        }
        return messages;
    }

    /**
     * Checks if specified user participant has read permission on given calendar object.
     * 
     * @param participant The user participant
     * @param obj The calendar object
     * @param session The session providing needed user data
     * @return <code>true</code> if specified user participant has read permission on given calendar object; otherwise <code>false</code>
     */
    public static boolean userCanReadObject(final EmailableParticipant participant, final CalendarObject obj, final ServerSession session) {
        UserConfiguration userConfig;
        try {
            userConfig = UserConfigurationStorage.getInstance().getUserConfiguration(participant.id, session.getContext());
            final OXFolderAccess oxfa = new OXFolderAccess(session.getContext());

            if (oxfa.getFolderType(obj.getParentFolderID()) == FolderObject.PRIVATE) {
                return true;
            }

            final EffectivePermission permission = oxfa.getFolderPermission(obj.getParentFolderID(), participant.id, userConfig);

            if (permission.canReadAllObjects()) {
                return true;
            }

            if (permission.canReadOwnObjects() && obj.getCreatedBy() == participant.id) {
                return true;
            }
        } catch (final OXException e) {
            LL.log(e);
        }

        return false;
    }

    /**
     * Gets the folder name.
     * 
     * @param folderId The folder ID
     * @param locale The locale
     * @param access The folder access instance
     * @return The folder name
     */
    protected String getFolderName(final int folderId, final Locale locale, final OXFolderAccess access) {
        String folderName = FolderObject.getFolderString(folderId, locale);
        if (folderName == null) {
            try {
                folderName = access.getFolderName(folderId);
            } catch (final OXException e) {
                LOG.error(e.getMessage(), e);
                folderName = "";
            }
        }
        return folderName;
    }

    /**
     * Creates a message for specified user.
     * 
     * @param session
     * @param p The participant
     * @param canRead <code>true</code> if provided participant has read permission; otherwise <code>false</code>
     * @param title The object's title
     * @param actionRepl The action replacement to compose the message's title
     * @param state The object's state
     * @param locale The locale
     * @param renderMap The render map
     * @param isUpdate <code>true</code> if an update event triggered the notification; otherwise <code>false</code>
     * @param b A string builder
     * @return The created message
     */
    protected static MailMessage createUserMessage(final ServerSession session, final CalendarObject cal, final EmailableParticipant p, final boolean canRead, final String title, final TemplateReplacement actionRepl, final State state, final Locale locale, final RenderMap renderMap, final boolean isUpdate, final StringBuilder b) {
        return createParticipantMessage0(session, cal, p, canRead, title, actionRepl, state, locale, renderMap, isUpdate, b);
    }

    /**
     * Creates a message for specified participant.
     * 
     * @param session
     * @param p The participant
     * @param title The object's title
     * @param actionRepl The action replacement to compose the message's title
     * @param state The object's state
     * @param locale The locale
     * @param renderMap The render map
     * @param isUpdate <code>true</code> if an update event triggered the notification; otherwise <code>false</code>
     * @param b A string builder
     * @return The created message
     */
    protected static MailMessage createParticipantMessage(final ServerSession session, final CalendarObject cal, final EmailableParticipant p, final String title, final TemplateReplacement actionRepl, final State state, final Locale locale, final RenderMap renderMap, final boolean isUpdate, final StringBuilder b) {
        return createParticipantMessage0(session, cal, p, true, title, actionRepl, state, locale, renderMap, isUpdate, b);
    }

    private static MailMessage createParticipantMessage0(final ServerSession session, final CalendarObject cal, final EmailableParticipant p, final boolean canRead, final String title, final TemplateReplacement actionRepl, final State state, final Locale locale, final RenderMap renderMap, final boolean isUpdate, final StringBuilder b) {
        final MailMessage msg = new MailMessage();
        final Template createTemplate = state.getTemplate();
        final StringHelper strings = new StringHelper(locale);

        b.setLength(0);
        actionRepl.setLocale(locale);
        msg.title = b.append(actionRepl.getReplacement()).append(": ").append(title).toString();
        b.setLength(0);
        if (isUpdate) {
            if (EmailableParticipant.STATE_REMOVED == p.state) {
                /*
                 * Current participant is removed by caught update event
                 */
                msg.overrideType = State.Type.DELETED;
                /*
                 * Get cloned version of render map to apply changed status
                 */
                final RenderMap clone = clonedRenderMap(renderMap);
                if (Types.APPOINTMENT == state.getModule()) {
                    msg.title = b.append(
                        new AppointmentActionReplacement(AppointmentActionReplacement.ACTION_DELETED, locale).getReplacement()).append(": ").append(
                        title).toString();
                    b.setLength(0);
                    /*
                     * Render proper message for removed participant
                     */
                    msg.message = new StringTemplate(Notifications.APPOINTMENT_DELETE_MAIL).render(p.getLocale(), clone);
                } else {
                    msg.title = b.append(new TaskActionReplacement(TaskActionReplacement.ACTION_DELETED, locale).getReplacement()).append(
                        ": ").append(title).toString();
                    b.setLength(0);
                    /*
                     * Render proper message for removed participant
                     */
                    msg.message = new StringTemplate(Notifications.TASK_DELETE_MAIL).render(p.getLocale(), clone);

                }
            } else if (EmailableParticipant.STATE_NEW == p.state) {
                /*
                 * Current participant is added by caught update event
                 */
                msg.overrideType = State.Type.NEW;
                /*
                 * Get cloned version of render map to apply changed status
                 */
                final RenderMap clone = clonedRenderMap(renderMap);
                if (Types.APPOINTMENT == state.getModule()) {
                    msg.title = b.append(new AppointmentActionReplacement(AppointmentActionReplacement.ACTION_NEW, locale).getReplacement()).append(
                        ": ").append(title).toString();
                    b.setLength(0);
                    /*
                     * Render proper message for removed participant
                     */
                    final String message = getAppointmentCreateTemplate(p, canRead, cal, session);
                    final String textMessage = new StringTemplate(message).render(p.getLocale(), clone);
                    if (p.type == Participant.USER && !NotificationConfig.getPropertyAsBoolean(NotificationProperty.INTERNAL_IMIP, false)) {
                        msg.message = textMessage;
                    } else {
                        msg.message = generateMessageMultipart(session, cal, textMessage, state.getModule(), state.getType(), ITipMethod.REQUEST, p, strings, b);
                    }
                } else {
                    msg.title = b.append(new TaskActionReplacement(TaskActionReplacement.ACTION_NEW, locale).getReplacement()).append(": ").append(
                        title).toString();
                    b.setLength(0);
                    /*
                     * Render proper message for removed participant
                     */
                    final String message = getTaskCreateMessage(p, canRead);
                    msg.message = new StringTemplate(message).render(p.getLocale(), clone);
                }
            } else {
                String textMessage = "";
                if (p.type == Participant.EXTERNAL_USER || p.type == Participant.RESOURCE) {
                    final String template = Types.APPOINTMENT == state.getModule() ? Notifications.APPOINTMENT_UPDATE_MAIL_EXT : Notifications.TASK_UPDATE_MAIL_EXT;
                    textMessage = new StringTemplate(template).render(p.getLocale(), renderMap);
                } else if (!canRead) {
                    final String template = state.getModule() == Types.APPOINTMENT ? Notifications.APPOINTMENT_UPDATE_MAIL_NO_ACCESS : Notifications.TASK_UPDATE_MAIL_NO_ACCESS;
                    textMessage = new StringTemplate(template).render(p.getLocale(), renderMap);
                } else {
                    textMessage = createTemplate.render(p.getLocale(), renderMap);
                }
                
                if (cal.getRecurrenceType() == Appointment.NO_RECURRENCE && p.type == Participant.EXTERNAL_USER) {
                    msg.message = generateMessageMultipart(session, cal, textMessage, state.getModule(), state.getType(), ITipMethod.REQUEST, p, strings, b);
                } else {
                    msg.message = textMessage;
                }
            }
        } else {
            if (State.Type.NEW.equals(state.getType())) {
                final int owner = getFolderOwner(cal, session);
                final boolean isOnBehalf = owner != session.getUserId();
                String textMessage = "";
                if ((p.type == Participant.EXTERNAL_USER || p.type == Participant.RESOURCE)) {
                    final String template = strings.getString(Types.APPOINTMENT == state.getModule() ? (isOnBehalf ? Notifications.APPOINTMENT_CREATE_MAIL_ON_BEHALF_EXT : Notifications.APPOINTMENT_CREATE_MAIL_EXT) : Notifications.TASK_CREATE_MAIL_EXT);
                    textMessage = new StringTemplate(template).render(p.getLocale(), renderMap);
                } else if (!canRead) {
                    final String template = strings.getString(state.getModule() == Types.APPOINTMENT ? (isOnBehalf ? Notifications.APPOINTMENT_CREATE_MAIL_ON_BEHALF_NO_ACCESS : Notifications.APPOINTMENT_CREATE_MAIL_NO_ACCESS) : Notifications.TASK_CREATE_MAIL_NO_ACCESS);
                    textMessage = new StringTemplate(template).render(p.getLocale(), renderMap);
                } else {
                    textMessage = createTemplate.render(p.getLocale(), renderMap);
                }
                if (p.type == Participant.USER && !NotificationConfig.getPropertyAsBoolean(NotificationProperty.INTERNAL_IMIP, false)) {
                    msg.message = textMessage;
                } else {
                    msg.message = generateMessageMultipart(
                        session,
                        cal,
                        textMessage,
                        state.getModule(),
                        state.getType(),
                        ITipMethod.REQUEST,
                        p,
                        strings,
                        b);
                }
            } else if (EnumSet.of(State.Type.ACCEPTED, State.Type.DECLINED, State.Type.TENTATIVELY_ACCEPTED).contains(state.getType())) {
                String textMessage = "";
                if ((p.type == Participant.EXTERNAL_USER || p.type == Participant.RESOURCE)) {
                    final String template = strings.getString(Types.APPOINTMENT == state.getModule() ? Notifications.APPOINTMENT_CONFIRMATION_MAIL_EXT : Notifications.TASK_CONFIRMATION_MAIL_EXT);
                    textMessage = new StringTemplate(template).render(p.getLocale(), renderMap);
                } else {
                    textMessage = createTemplate.render(p.getLocale(), renderMap);
                }
                // Attach IMIP Magic only for external users on secondary events, to tell them the state of the appointment, but don't
                // bother with internal users.
                if (p.type == Participant.EXTERNAL_USER) {
                    msg.message = generateMessageMultipart(
                        session,
                        cal,
                        textMessage,
                        state.getModule(),
                        state.getType(),
                        ITipMethod.REPLY,
                        p,
                        strings,
                        b);
                } else {
                    msg.message = textMessage;
                }
            } else if (state.getType() == State.Type.DELETED) {
                if (p.type == Participant.USER && !NotificationConfig.getPropertyAsBoolean(NotificationProperty.INTERNAL_IMIP, false)) {
                    msg.message = createTemplate.render(p.getLocale(), renderMap);
                } else {
                    msg.message = generateMessageMultipart(
                        session,
                        cal,
                        createTemplate.render(p.getLocale(), renderMap),
                        state.getModule(),
                        state.getType(),
                        ITipMethod.CANCEL,
                        p,
                        strings,
                        b);
                }
            } else {
                msg.message = createTemplate.render(p.getLocale(), renderMap);
            }
        }
        if (Participant.RESOURCE == p.type) {
            /*-
             * Special prefixes for resource participant receivers.
             * 
             * Prefix already applied to multipart/* content, therefore only check for text/plain content
             */
            final Object content = msg.message;
            if (content instanceof String) {
                /*
                 * Prepend prefix to text content
                 */
                msg.message = b.append(String.format(strings.getString(Notifications.RESOURCE_PREFIX), p.displayName)).append(": ").append(
                    content).toString();
                b.setLength(0);
            }
            /*
             * Prefix title
             */
            msg.title = b.append('[').append(strings.getString(Notifications.RESOURCE_TITLE_PREFIX)).append("] ").append(msg.title).toString();
            b.setLength(0);
        }
        msg.addresses.add(p.email);
        msg.folderId = p.folderId;
        msg.internal = p.type != Participant.EXTERNAL_USER;
        return msg;
    }

    /**
     * Builds a multipart object containing the text/plain and iCal part
     * 
     * @return The multipart object or given text if building failed
     */
    private static Object generateMessageMultipart(final ServerSession session, final CalendarObject cal, final String text, final int module, final State.Type type, final ITipMethod method, final EmailableParticipant p, final StringHelper strings, final StringBuilder b) {
        if (module == Types.TASK) {
            return text;
        }
        /*
         * Generate iCal for appointment
         */
        try {
            final Multipart mp = new MimeMultipart("alternative");
            /*
             * Compose text part
             */
            final BodyPart textPart = new MimeBodyPart();
            if (Participant.RESOURCE == p.type) {
                /*
                 * Prepend resource prefix to first text/plain body part
                 */
                textPart.setContent(
                    b.append(String.format(strings.getString(Notifications.RESOURCE_PREFIX), p.displayName)).append(": ").append(text).toString(),
                    "text/plain; charset=UTF-8");
                b.setLength(0);
            } else {
                /*
                 * Apply text as given
                 */
                textPart.setContent(text, "text/plain; charset=UTF-8");
            }
            /*
             * Compose iCal part
             */
            final Appointment app = (Appointment) cal;
            final ICalEmitter emitter = ServerServiceRegistry.getInstance().getService(ICalEmitter.class);
            final ICalSession icalSession = emitter.createSession();
            Date until = null;
            if (Appointment.NO_RECURRENCE != app.getRecurrenceType()) {
                until = app.getEndDate();
                app.setEndDate(computeFirstOccurrenceEnd(app));
            }
            final boolean hasAlarm = app.containsAlarm();
            final int alarm = app.getAlarm();
            app.removeAlarm();
            final ITipContainer iTip = new ITipContainer(method, type, session.getUserId());
            emitter.writeAppointment(
                icalSession,
                app,
                session.getContext(),
                iTip,
                new ArrayList<ConversionError>(),
                new ArrayList<ConversionWarning>());
            if (null != until) {
                app.setEndDate(until);
            }
            if (hasAlarm) {
                app.setAlarm(alarm);
            }
            /*
             * Copy stream
             */
            final byte[] icalFile;
            final boolean isAscii;
            {
                final ByteArrayOutputStream byteArrayOutputStream = new UnsynchronizedByteArrayOutputStream();
                emitter.writeSession(icalSession, byteArrayOutputStream);
                icalFile = byteArrayOutputStream.toByteArray();
                isAscii = isAscii(icalFile);
            }

            final BodyPart iCalPart = new MimeBodyPart();

            final String contentType = b.append("text/calendar; ").append(method.getMethod()).append("; charset=\"utf-8\"").toString();
            b.setLength(0);
            iCalPart.setDataHandler(new DataHandler(new MessageDataSource(icalFile, contentType)));
            iCalPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.foldContentType(contentType));
            iCalPart.setHeader(MessageHeaders.HDR_CONTENT_TRANSFER_ENC, isAscii ? "7bit" : "quoted-printable");
            /*
             * Add the parts to parental multipart & return
             */
            mp.addBodyPart(textPart);
            mp.addBodyPart(iCalPart);
            return mp;
        } catch (final MessagingException e) {
            LOG.error("Unable to compose message", e);
        } catch (final ConversionError e) {
            LOG.error("Unable to compose message", e);
        } catch (final OXException e) {
            LOG.error("Unable to compose message", e);
        }
        /*
         * Failed to create multipart
         */
        return text;
    }

    private static boolean isAscii(final byte[] bytes) {
        boolean isAscci = true;
        for (int i = 0; isAscci && (i < bytes.length); i++) {
            final byte b = bytes[i];
            isAscci = (b < 128 && b >= 0);
        }
        return isAscci;
    }

    private static String getTaskCreateMessage(final EmailableParticipant p, final boolean canRead) {
        if (p.type == Participant.EXTERNAL_USER || p.type == Participant.RESOURCE) {
            return Notifications.TASK_CREATE_MAIL_EXT;
        } else if (!canRead) {
            return Notifications.TASK_CREATE_MAIL_NO_ACCESS;
        } else {
            return Notifications.TASK_CREATE_MAIL;
        }
    }

    private static String getAppointmentCreateTemplate(final EmailableParticipant p, final boolean canRead, final CalendarObject cal, final ServerSession session) {
        final int folderOwner = getFolderOwner(cal, session);
        if (p.type == Participant.EXTERNAL_USER || p.type == Participant.RESOURCE) {
            if (folderOwner == session.getUserId()) {
                return Notifications.APPOINTMENT_CREATE_MAIL_EXT;
            }
            return Notifications.APPOINTMENT_CREATE_MAIL_ON_BEHALF_EXT;
        } else if (!canRead) {
            if (folderOwner == session.getUserId()) {
                return Notifications.APPOINTMENT_CREATE_MAIL_NO_ACCESS;
            }
            return Notifications.APPOINTMENT_CREATE_MAIL_ON_BEHALF_NO_ACCESS;
        } else {
            if (folderOwner == session.getUserId()) {
                return Notifications.APPOINTMENT_CREATE_MAIL;
            }
            return Notifications.APPOINTMENT_CREATE_MAIL_ON_BEHALF;
        }
    }
    
    private static int getFolderOwner(final CalendarObject cal, final ServerSession session) {
        final OXFolderAccess oxfa = new OXFolderAccess(session.getContext());
        try {
            return oxfa.getFolderOwner(cal.getParentFolderID());
        } catch (final OXException e) {
            LL.log(e);
            return session.getUserId();
        }
    }

    private RenderMap createRenderMap(final CalendarObject newObj, final CalendarObject oldObj, final boolean isUpdate, final String title, final State state, final Map<Locale, List<EmailableParticipant>> receivers, final ServerSession session) {
        final int module = state.getModule();
        /*
         * Containers for traversed participants
         */
        final SortedSet<EmailableParticipant> participantSet = new TreeSet<EmailableParticipant>();
        final SortedSet<EmailableParticipant> resourceSet = new TreeSet<EmailableParticipant>();
        final Map<String, EmailableParticipant> all = new HashMap<String, EmailableParticipant>();
        /*
         * Traverse participants and fill containers
         */
        final UserParticipant[] users = newObj.getUsers();
        if (null == users) {
            Participant[] oldParticipants = new Participant[0];
            if (oldObj != null) {
                oldParticipants = oldObj.getParticipants();
            }
            sortParticipants(oldParticipants, newObj.getParticipants(), participantSet, resourceSet, receivers, session, all);
        } else {
            UserParticipant[] oldUsers = new UserParticipant[0];
            if (oldObj != null) {
                oldUsers = oldObj.getUsers();
            }
            Participant[] oldParticipants = new Participant[0];
            if (oldObj != null) {
                oldParticipants = oldObj.getParticipants();
            }

            sortUserParticipants(oldUsers, newObj.getUsers(), participantSet, isUpdate, receivers, session, all);
            sortExternalParticipantsAndResources(
                oldParticipants,
                newObj.getParticipants(),
                participantSet,
                resourceSet,
                receivers,
                session,
                all,
                newObj.getOrganizer());
        }
        // Add task owner to receivers list to make him receive mails about changed participants states.
        if (newObj instanceof Task) {
            addTaskOwner(newObj, receivers, all, session);
        }
        /*
         * Generate a render map
         */
        final RenderMap renderMap = new RenderMap();
        renderMap.put(new FormatLocalizedStringReplacement(TemplateToken.TITLE, Notifications.FORMAT_DESCRIPTION, title).setChanged(isUpdate ? (oldObj == null ? false : !compareObjects(
            title,
            oldObj.getTitle())) : false));
        renderMap.put(new ParticipantsReplacement(participantSet).setChanged(isUpdate));
        renderMap.put(new ResourcesReplacement(resourceSet).setChanged(isUpdate));
        {
            String createdByDisplayName = STR_UNKNOWN;
            final Context ctx = session.getContext();
            if (0 != newObj.getCreatedBy()) {
                try {
                    createdByDisplayName = resolveUsers(ctx, newObj.getCreatedBy())[0].getDisplayName();
                } catch (final LdapException e) {
                    createdByDisplayName = STR_UNKNOWN;
                    LL.log(e);
                }
            }
            String modifiedByDisplayName = STR_UNKNOWN;
            try {
                modifiedByDisplayName = resolveUsers(ctx, session.getUserId())[0].getDisplayName();
            } catch (final LdapException e) {
                modifiedByDisplayName = STR_UNKNOWN;
                LL.log(e);
            }

            String onBehalfDisplayName = STR_UNKNOWN;
            final OXFolderAccess oxfa = new OXFolderAccess(session.getContext());
            try {
                onBehalfDisplayName = resolveUsers(ctx, oxfa.getFolderOwner(newObj.getParentFolderID()))[0].getDisplayName();
            } catch (final LdapException e) {
                LL.log(e);
            } catch (final OXException e) {
                LL.log(e);
            }
            renderMap.put(new StringReplacement(TemplateToken.CREATED_BY, createdByDisplayName));
            renderMap.put(new StringReplacement(TemplateToken.CHANGED_BY, modifiedByDisplayName));
            renderMap.put(new StringReplacement(TemplateToken.BEHALF_OF, onBehalfDisplayName));
        }
        {
            final String note = null == newObj.getNote() ? "" : newObj.getNote();
            renderMap.put(new CommentsReplacement(note).setChanged(isUpdate ? (oldObj == null ? false : !compareStrings(
                note,
                oldObj.getNote())) : false));
        }
        /*
         * Add task-specific replacements
         */
        if (Types.TASK == module) {
            final Task task = (Task) newObj;
            final Task oldTask = (Task) oldObj;
            {
                final int priority = task.getPriority();
                try {
                    renderMap.put(new TaskPriorityReplacement(priority).setChanged(oldTask == null ? false : priority != oldTask.getPriority()));
                } catch (final IllegalArgumentException e) {
                    renderMap.put(TaskPriorityReplacement.emptyTaskPriorityReplacement());
                }
            }
            {
                final int status = task.getStatus();
                final int percentComplete = task.getPercentComplete();
                boolean changed = false;
                if (status != 0 && oldTask != null) {
                    changed |= (status != oldTask.getStatus());
                    changed |= (percentComplete != oldTask.getPercentComplete());
                }
                try {
                    renderMap.put(new TaskStatusReplacement(status, percentComplete).setChanged(changed));
                } catch (final IllegalArgumentException e) {
                    renderMap.put(TaskStatusReplacement.emptyTaskStatusReplacement());
                }
            }
        }
        /*
         * Generate replacements which got modified by participant data
         */
        {
            final boolean isTask = (Types.TASK == module);
            final boolean isFulltime;
            if (isTask) {
                isFulltime = true;
            } else {
                isFulltime = ((Appointment) newObj).getFullTime();
            }
            final Date start = newObj.getStartDate();
            renderMap.put(new StartDateReplacement(start, isFulltime).setChanged(isUpdate ? (oldObj == null ? false : !compareObjects(
                start,
                oldObj.getStartDate())) : false));
            Date end = newObj.getEndDate();
            /*
             * Determine changed status with original end time
             */
            final boolean endChanged = isUpdate ? (oldObj == null ? false : !compareObjects(end, oldObj.getEndDate())) : false;
            /*
             * Set end time to first occurrence's end time if necessary
             */
            
            if (newObj.getRecurrenceType() != CalendarObject.NO_RECURRENCE) {
                if (start != null && end != null) {
                    end = computeFirstOccurrenceEnd(start.getTime(), end.getTime());
                }
            }

            renderMap.put(new EndDateReplacement(end, isFulltime, isTask).setChanged(endChanged));
        }
        renderMap.put(new CreationDateReplacement(
            newObj.containsCreationDate() ? newObj.getCreationDate() : (oldObj == null ? null : oldObj.getCreationDate()),
            null));
        {
            final SeriesReplacement seriesRepl;
            if (newObj.containsRecurrenceType() || newObj.getRecurrenceType() != CalendarObject.NO_RECURRENCE) {
                seriesRepl = new SeriesReplacement(newObj, (Types.TASK == module));
                seriesRepl.setChanged(isUpdate ? (oldObj == null ? false : !compareRecurrenceInformation(newObj, oldObj)) : false);
            } else if (oldObj != null && oldObj.containsRecurrenceType()) {
                seriesRepl = new SeriesReplacement(oldObj, (Types.TASK == module));
                seriesRepl.setChanged(false);
            } else {
                seriesRepl = new SeriesReplacement(newObj, (Types.TASK == module));
                seriesRepl.setChanged(false);
            }
            renderMap.put(seriesRepl);
        }
        {
            final DeleteExceptionsReplacement deleteExceptionsReplacement;
            final Date[] deleteExcs = newObj.getDeleteException();
            if (newObj.containsDeleteExceptions() || deleteExcs != null) {
                deleteExceptionsReplacement = new DeleteExceptionsReplacement(deleteExcs);
                deleteExceptionsReplacement.setChanged(isUpdate ? (oldObj == null ? false : !compareDates(
                    deleteExcs,
                    oldObj.getDeleteException())) : false);
            } else if (oldObj != null && oldObj.containsDeleteExceptions()) {
                deleteExceptionsReplacement = new DeleteExceptionsReplacement(oldObj.getDeleteException());
                deleteExceptionsReplacement.setChanged(false);
            } else {
                deleteExceptionsReplacement = new DeleteExceptionsReplacement(deleteExcs);
                deleteExceptionsReplacement.setChanged(false);
            }
            renderMap.put(deleteExceptionsReplacement);
        }
        {
            final ChangeExceptionsReplacement changeExceptionsReplacement;
            final Date[] changeExcs = newObj.getChangeException();
            if (newObj.containsChangeExceptions() || changeExcs != null) {
                changeExceptionsReplacement = new ChangeExceptionsReplacement(changeExcs);
                final String recurrenceTitle;
                if (isChangeException(newObj) && null != (recurrenceTitle = getRecurrenceTitle(newObj, session.getContext()))) {
                    changeExceptionsReplacement.setChangeException(true);
                    changeExceptionsReplacement.setRecurrenceTitle(recurrenceTitle);
                } else {
                    changeExceptionsReplacement.setChanged(isUpdate ? (oldObj == null ? false : !compareDates(
                        changeExcs,
                        oldObj.getChangeException())) : false);
                }
            } else if (oldObj != null && oldObj.containsChangeExceptions()) {
                final Date[] oldChangeExcs = oldObj.getChangeException();
                changeExceptionsReplacement = new ChangeExceptionsReplacement(oldChangeExcs);
                final String recurrenceTitle;
                if (oldChangeExcs != null && isChangeException(oldObj) && null != (recurrenceTitle = getRecurrenceTitle(
                    oldObj,
                    session.getContext()))) {
                    changeExceptionsReplacement.setChangeException(true);
                    changeExceptionsReplacement.setRecurrenceTitle(recurrenceTitle);
                } else {
                    changeExceptionsReplacement.setChanged(false);
                }
            } else {
                changeExceptionsReplacement = new ChangeExceptionsReplacement(changeExcs);
                changeExceptionsReplacement.setChanged(false);
            }
            renderMap.put(changeExceptionsReplacement);
        }
        return renderMap;
    }

    private static RenderMap clonedRenderMap(final RenderMap renderMap) {
        return ((RenderMap) renderMap.clone()).applyChangedStatus(false);
    }

    private void checkChangedFolder(final CalendarObject oldObj, final String email, final int folderId, final FolderReplacement folderRepl, final ServerSession session) {
        final Participant[] oldParticipants = oldObj.getParticipants();
        final Context ctx = session.getContext();
        if (oldParticipants != null) {
            for (int i = 0; i < oldParticipants.length; i++) {
                final Participant participant = oldParticipants[i];
                switch (participant.getType()) {
                case Participant.USER:
                    EmailableParticipant p = getUserParticipant(participant, ctx);
                    if (p.type == Participant.USER && p.folderId > 0 && p.email.equalsIgnoreCase(email)) {
                        folderRepl.setChanged(p.folderId != folderId);
                        return;
                    }
                    break;
                case Participant.EXTERNAL_USER:
                    p = getExternalParticipant(participant, session);
                    if (p.type == Participant.USER && p.folderId > 0 && p.email.equalsIgnoreCase(email)) {
                        folderRepl.setChanged(p.folderId != folderId);
                        return;
                    }
                    break;
                case Participant.RESOURCE:
                    p = getResourceParticipant(participant, session);
                    if (p.type == Participant.USER && p.folderId > 0 && p.email.equalsIgnoreCase(email)) {
                        folderRepl.setChanged(p.folderId != folderId);
                        return;
                    }
                    break;
                case Participant.GROUP:
                    try {
                        // FIXME 101 SELECT problem
                        final Group group = resolveGroups(ctx, participant.getIdentifier())[0];
                        final int[] members = group.getMember();
                        final User[] memberObjects = resolveUsers(ctx, members);
                        for (final User user : memberObjects) {

                            final int[] groups = user.getGroups();
                            final TimeZone tz = TimeZoneUtils.getTimeZone(user.getTimeZone());

                            if (user.getMail() != null) {
                                p = new EmailableParticipant(
                                    ctx.getContextId(),
                                    Participant.USER,
                                    user.getId(),
                                    groups,
                                    user.getMail(),
                                    user.getDisplayName(),
                                    user.getLocale(),
                                    tz,
                                    10,
                                    -1,
                                    CalendarObject.NONE,
                                    null,
                                    participant.isIgnoreNotification());
                                if (p.type == Participant.USER && p.folderId > 0 && p.email.equalsIgnoreCase(email)) {
                                    folderRepl.setChanged(p.folderId != folderId);
                                    return;
                                }
                            }
                        }
                    } catch (final LdapException e) {
                        LL.log(e);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown Participant Type: " + participant.getType());
                }
            }
        }
    }

    private void sortExternalParticipantsAndResources(final Participant[] oldParticipants, final Participant[] newParticipants, final Set<EmailableParticipant> participantSet, final Set<EmailableParticipant> resourceSet, final Map<Locale, List<EmailableParticipant>> receivers, final ServerSession session, final Map<String, EmailableParticipant> all, final String organizer) {
        sortNewExternalParticipantsAndResources(newParticipants, participantSet, resourceSet, receivers, session, all, oldParticipants);
        sortOldExternalParticipantsAndResources(
            oldParticipants,
            participantSet,
            resourceSet,
            receivers,
            all,
            session,
            newParticipants,
            organizer);
    }

    private void sortOldExternalParticipantsAndResources(final Participant[] oldParticipants, final Set<EmailableParticipant> participantSet, final Set<EmailableParticipant> resourceSet, final Map<Locale, List<EmailableParticipant>> receivers, final Map<String, EmailableParticipant> all, final ServerSession session, final Participant[] newParticipants, final String organizer) {
        if (oldParticipants == null) {
            return;
        }
        final Context ctx = session.getContext();
        // List<Participant> mergedWithOrganizer = new ArrayList<Participant>(Arrays.asList(oldParticipants));
        // if (organizer != null && !organizer.trim().equals(""))
        // mergedWithOrganizer.add(new ExternalUserParticipant(organizer));

        for (final Participant participant : oldParticipants) {
            switch (participant.getType()) {
            case Participant.USER:
                break;
            case Participant.EXTERNAL_USER:
                EmailableParticipant p = getExternalParticipant(participant, session);
                if (p != null) {
                    p.state = contains(participant, newParticipants) ? EmailableParticipant.STATE_NONE : EmailableParticipant.STATE_REMOVED;
                    addSingleParticipant(p, participantSet, resourceSet, receivers, all, false);
                }
                break;
            case Participant.RESOURCE:
                p = getResourceParticipant(participant, session);
                if (p == null) {
                    // Might be user added as resource (!)
                    p = getUserParticipant(participant, ctx);
                }
                if (p != null) {
                    p.state = contains(participant, newParticipants) ? EmailableParticipant.STATE_NONE : EmailableParticipant.STATE_REMOVED;
                    addSingleParticipant(p, participantSet, resourceSet, receivers, all, true);
                }
                break;
            case Participant.GROUP:
                break;
            default:
                throw new IllegalArgumentException("Unknown Participant Type: " + participant.getType());
            }
        }

        if (organizer != null) {
            addSingleParticipant(
                getExternalParticipant(new ExternalUserParticipant(organizer), session),
                participantSet,
                resourceSet,
                receivers,
                all,
                false);
        }
    }

    private void sortNewExternalParticipantsAndResources(final Participant[] newParticipants, final Set<EmailableParticipant> participantSet, final Set<EmailableParticipant> resourceSet, final Map<Locale, List<EmailableParticipant>> receivers, final ServerSession session, final Map<String, EmailableParticipant> all, final Participant[] oldParticipants) {
        if (newParticipants == null) {
            return;
        }
        final Context ctx = session.getContext();
        for (final Participant participant : newParticipants) {
            switch (participant.getType()) {
            case Participant.USER:
                break;
            case Participant.EXTERNAL_USER:
                EmailableParticipant p = getExternalParticipant(participant, session);
                if (p != null) {
                    p.state = contains(participant, oldParticipants) ? EmailableParticipant.STATE_NONE : EmailableParticipant.STATE_NEW;
                    addSingleParticipant(p, participantSet, resourceSet, receivers, all, false);
                }

                break;
            case Participant.RESOURCE:
                p = getResourceParticipant(participant, session);
                if (p == null) {
                    // Might be user added as resource (!)
                    p = getUserParticipant(participant, ctx);
                }
                if (p != null) {
                    p.state = contains(participant, oldParticipants) ? EmailableParticipant.STATE_NONE : EmailableParticipant.STATE_NEW;
                    addSingleParticipant(p, participantSet, resourceSet, receivers, all, true);
                }
                break;
            case Participant.GROUP:
                break;
            default:
                throw new IllegalArgumentException("Unknown Participant Type: " + participant.getType());
            }
        }
    }

    private void sortParticipants(final Participant[] oldParticipants, final Participant[] newParticipants, final Set<EmailableParticipant> participantSet, final Set<EmailableParticipant> resourceSet, final Map<Locale, List<EmailableParticipant>> receivers, final ServerSession session, final Map<String, EmailableParticipant> all) {
        sortNewParticipants(newParticipants, participantSet, resourceSet, receivers, session, all, oldParticipants);
        sortOldParticipants(oldParticipants, participantSet, resourceSet, receivers, all, session, newParticipants);
    }

    private void sortOldParticipants(final Participant[] oldParticipants, final Set<EmailableParticipant> participantSet, final Set<EmailableParticipant> resourceSet, final Map<Locale, List<EmailableParticipant>> receivers, final Map<String, EmailableParticipant> all, final ServerSession session, final Participant[] newParticipants) {
        if (oldParticipants == null) {
            return;
        }
        final Context ctx = session.getContext();
        for (final Participant participant : oldParticipants) {
            switch (participant.getType()) {
            case Participant.USER:
                EmailableParticipant p = getUserParticipant(participant, ctx);
                if (p != null) {
                    p.state = contains(participant, newParticipants) ? EmailableParticipant.STATE_NONE : EmailableParticipant.STATE_REMOVED;
                    addSingleParticipant(p, participantSet, resourceSet, receivers, all, false);
                }
                break;
            case Participant.EXTERNAL_USER:
                p = getExternalParticipant(participant, session);
                if (p != null) {
                    p.state = contains(participant, newParticipants) ? EmailableParticipant.STATE_NONE : EmailableParticipant.STATE_REMOVED;
                    addSingleParticipant(p, participantSet, resourceSet, receivers, all, false);
                }
                break;
            case Participant.RESOURCE:
                p = getResourceParticipant(participant, session);
                if (p != null) {
                    p.state = contains(participant, newParticipants) ? EmailableParticipant.STATE_NONE : EmailableParticipant.STATE_REMOVED;
                    addSingleParticipant(p, participantSet, resourceSet, receivers, all, true);
                }
                break;
            case Participant.GROUP:
                try {
                    // FIXME 101 SELECT problem
                    final int state = contains(participant, newParticipants) ? EmailableParticipant.STATE_NONE : EmailableParticipant.STATE_REMOVED;
                    final Group group = resolveGroups(ctx, participant.getIdentifier())[0];
                    final int[] members = group.getMember();
                    final User[] memberObjects = resolveUsers(ctx, members);
                    for (final User user : memberObjects) {
                        // final String lang = user.getPreferredLanguage();
                        final int[] groups = user.getGroups();
                        final TimeZone tz = TimeZoneUtils.getTimeZone(user.getTimeZone());

                        if (user.getMail() != null) {
                            p = new EmailableParticipant(
                                ctx.getContextId(),
                                Participant.USER,
                                user.getId(),
                                groups,
                                user.getMail(),
                                user.getDisplayName(),
                                user.getLocale(),
                                tz,
                                10,
                                -1,
                                CalendarObject.NONE,
                                null,
                                participant.isIgnoreNotification());
                            p.state = state;
                            addSingleParticipant(p, participantSet, resourceSet, receivers, all, false);
                        }
                    }
                } catch (final LdapException e) {
                    LL.log(e);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown Participant Type: " + participant.getType());
            }

        }
    }

    private void sortNewParticipants(final Participant[] newParticipants, final Set<EmailableParticipant> participantSet, final Set<EmailableParticipant> resourceSet, final Map<Locale, List<EmailableParticipant>> receivers, final ServerSession session, final Map<String, EmailableParticipant> all, final Participant[] oldParticipants) {
        if (newParticipants == null) {
            return;
        }
        final Context ctx = session.getContext();
        for (final Participant participant : newParticipants) {
            switch (participant.getType()) {
            case Participant.USER:
                EmailableParticipant p = getUserParticipant(participant, ctx);
                if (p != null) {
                    p.state = contains(participant, oldParticipants) ? EmailableParticipant.STATE_NONE : EmailableParticipant.STATE_NEW;
                    addSingleParticipant(p, participantSet, resourceSet, receivers, all, false);
                }
                break;
            case Participant.EXTERNAL_USER:
                p = getExternalParticipant(participant, session);
                if (p != null) {
                    p.state = contains(participant, oldParticipants) ? EmailableParticipant.STATE_NONE : EmailableParticipant.STATE_NEW;
                    addSingleParticipant(p, participantSet, resourceSet, receivers, all, false);
                }

                break;
            case Participant.RESOURCE:
                p = getResourceParticipant(participant, session);
                if (p != null) {
                    p.state = contains(participant, oldParticipants) ? EmailableParticipant.STATE_NONE : EmailableParticipant.STATE_NEW;
                    addSingleParticipant(p, participantSet, resourceSet, receivers, all, true);
                }
                break;
            case Participant.GROUP:
                try {
                    // FIXME 101 SELECT problem
                    final int state = contains(participant, oldParticipants) ? EmailableParticipant.STATE_NONE : EmailableParticipant.STATE_NEW;
                    final Group group = resolveGroups(ctx, participant.getIdentifier())[0];
                    final int[] members = group.getMember();
                    final User[] memberObjects = resolveUsers(ctx, members);
                    for (final User user : memberObjects) {
                        // final String lang = user.getPreferredLanguage();
                        final int[] groups = user.getGroups();
                        final TimeZone tz = TimeZoneUtils.getTimeZone(user.getTimeZone());

                        if (user.getMail() != null) {
                            p = new EmailableParticipant(
                                ctx.getContextId(),
                                Participant.USER,
                                user.getId(),
                                groups,
                                user.getMail(),
                                user.getDisplayName(),
                                user.getLocale(),
                                tz,
                                10,
                                -1,
                                CalendarObject.NONE,
                                null,
                                participant.isIgnoreNotification());
                            p.state = state;
                            addSingleParticipant(p, participantSet, resourceSet, receivers, all, false);
                        }
                    }
                } catch (final LdapException e) {
                    LL.log(e);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown Participant Type: " + participant.getType());
            }

        }
    }

    private EmailableParticipant getExternalParticipant(final Participant participant, final ServerSession session) {
        if (null == participant.getEmailAddress()) {
            return null;
        }
        /*
         * Store session user's locale and time zone which are used for external participants
         */
        final User user;
        Locale l;
        TimeZone tz;
        try {
            user = resolveUsers(session.getContext(), session.getUserId())[0];
            l = user.getLocale();
            tz = getCalendarTools().getTimeZone(user.getTimeZone());
        } catch (final LdapException e) {
            // Should not happen
            LOG.warn("Could not resolve user from session: UserId: " + session.getUserId() + " in Context: " + session.getContextId());
            l = Locale.getDefault();
            tz = TimeZone.getDefault();
        }
        return new EmailableParticipant(
            session.getContextId(),
            participant.getType(),
            -1,
            new int[0],
            participant.getEmailAddress(),
            participant.getDisplayName(),
            l,
            tz,
            0,
            -1,
            CalendarObject.NONE,
            null,
            participant.isIgnoreNotification());
    }

    private EmailableParticipant getUserParticipant(final Participant participant, final Context ctx) {

        int[] groups = null;
        TimeZone tz = null;
        String mail = null;
        String displayName = null;
        int folderId = -1;
        Locale locale = null;

        try {
            final User user = resolveUsers(ctx, participant.getIdentifier())[0];
            locale = user.getLocale();
            mail = user.getMail();
            if (mail == null) {
                mail = participant.getEmailAddress();
            }
            displayName = user.getDisplayName();
            if (displayName == null) {
                displayName = participant.getDisplayName();
            }
            groups = user.getGroups();
            tz = getCalendarTools().getTimeZone(user.getTimeZone());
            if (participant instanceof UserParticipant) {
                final UserParticipant userParticipant = (UserParticipant) participant;
                folderId = userParticipant.getPersonalFolderId();
                // System.out.println("PERSONAL FOLDER ID FOR PARTICIPANT "+
                // userParticipant.getIdentifier()+": "+folderId);
            }
        } catch (final LdapException e) {
            LL.log(e);
        }

        if (mail != null) {
            if (participant instanceof UserParticipant) {
                final UserParticipant up = (UserParticipant) participant;
                return new EmailableParticipant(
                    ctx.getContextId(),
                    up.getType(),
                    up.getIdentifier(),
                    groups,
                    mail,
                    displayName,
                    locale,
                    tz,
                    10,
                    folderId,
                    up.getConfirm(),
                    up.getConfirmMessage(),
                    participant.isIgnoreNotification());
            }
            return new EmailableParticipant(
                ctx.getContextId(),
                participant.getType(),
                participant.getIdentifier(),
                groups,
                mail,
                displayName,
                locale,
                tz,
                10,
                folderId,
                CalendarObject.NONE,
                null,
                participant.isIgnoreNotification());
        }
        return null;
    }

    private EmailableParticipant getResourceParticipant(final Participant participant, final ServerSession session) {
        final int[] groups = new int[0];
        String mail = null;
        String displayName = null;
        final Context ctx = session.getContext();
        try {
            final Resource resource = resolveResources(ctx, participant.getIdentifier())[0];
            mail = resource.getMail();
            if (mail == null) {
                mail = participant.getEmailAddress();
            }
            displayName = resource.getDisplayName();
            if (displayName == null) {
                displayName = participant.getDisplayName();
            }
        } catch (final LdapException e) {
            LL.log(e);
        }

        Locale l;
        try {
            final User user = resolveUsers(session.getContext(), session.getUserId())[0];
            l = user.getLocale();
        } catch (final LdapException e) {
            // Should not happen
            LOG.warn("Could not resolve user from session: UserId: " + session.getUserId() + " in Context: " + session.getContextId());
            l = Locale.getDefault();
        }

        EmailableParticipant p;
        if (mail != null) {
            p = new EmailableParticipant(
                ctx.getContextId(),
                participant.getType(),
                participant.getIdentifier(),
                groups,
                mail,
                displayName,
                l,
                TimeZone.getDefault(),
                -1,
                MailObject.DONT_SET,
                CalendarObject.NONE,
                null,
                participant.isIgnoreNotification());
            return p;
        }
        return null;
    }

    private void sortUserParticipants(final UserParticipant[] oldParticipants, final UserParticipant[] newParticipants, final Set<EmailableParticipant> participantSet, final boolean forUpdate, final Map<Locale, List<EmailableParticipant>> receivers, final ServerSession session, final Map<String, EmailableParticipant> all) {
        if (newParticipants == null) {
            return;
        }
        final Context ctx = session.getContext();
        for (final UserParticipant participant : newParticipants) {
            final EmailableParticipant p = getUserParticipant(participant, ctx);
            if (p != null) {
                p.state = containsUser(participant, oldParticipants) ? EmailableParticipant.STATE_NONE : EmailableParticipant.STATE_NEW;
                addSingleParticipant(p, participantSet, null, receivers, all, false);
            }
        }

        for (final UserParticipant participant : oldParticipants) {
            final EmailableParticipant p = getUserParticipant(participant, ctx);
            if (p != null) {
                p.state = containsUser(participant, newParticipants) ? EmailableParticipant.STATE_NONE : EmailableParticipant.STATE_REMOVED;
                if (forUpdate) {
                    addSingleParticipant(p, participantSet, null, receivers, all, false);
                } else {
                    addReceiver(p, receivers, all);
                }
            }
        }

    }

    private void addTaskOwner(final CalendarObject task, final Map<Locale, List<EmailableParticipant>> receivers, final Map<String, EmailableParticipant> all, final ServerSession session) {
        final Context ctx = session.getContext();
        final int creatorId = task.getCreatedBy();
        try {
            final User user = resolveUsers(ctx, creatorId)[0];
            final EmailableParticipant emailable =  new EmailableParticipant(
                ctx.getContextId(),
                Participant.USER,
                creatorId,
                user.getGroups(),
                user.getMail(),
                user.getDisplayName(),
                user.getLocale(),
                getCalendarTools().getTimeZone(user.getTimeZone()),
                10,
                -1,
                CalendarObject.NONE,
                null,
                false);
            addReceiver(emailable, receivers, all);
        } catch (final LdapException e) {
            LL.log(e);
        }
    }

    private void addReceiver(final EmailableParticipant participant, final Map<Locale, List<EmailableParticipant>> receivers, final Map<String, EmailableParticipant> all) {

        if (all.containsKey(participant.email)) {
            final EmailableParticipant other = all.get(participant.email);
            if (other.reliability < participant.reliability) {
                if (other.getLocale().equals(participant.getLocale())) {
                    other.copy(participant);
                    return;
                }
                final List<EmailableParticipant> p = receivers.get(other.getLocale());
                p.remove(p.indexOf(other));
            }
            return;
        }
        final Locale l = participant.getLocale();

        List<EmailableParticipant> p = receivers.get(l);
        if (p == null) {
            p = new ArrayList<EmailableParticipant>();
            receivers.put(l, p);
        }

        all.put(participant.email, participant);
        p.add(participant);

    }

    private void addSingleParticipant(final EmailableParticipant participant, final Set<EmailableParticipant> participantSet, final Set<EmailableParticipant> resourceSet, final Map<Locale, List<EmailableParticipant>> receivers, final Map<String, EmailableParticipant> all, final boolean /* HACK */resource) {
        addReceiver(participant, receivers, all);
        if (resource) {
            resourceSet.add(participant);
        } else {
            participantSet.add(participant);
        }
    }

    static final class MailMessage {

        public Type overrideType;

        /**
         * Initializes a new MailMessage
         */
        public MailMessage() {
            super();
        }

        public Object message;

        public String title;

        public List<String> addresses = new ArrayList<String>();

        public int folderId;

        public boolean internal;
    }

    private static final boolean containsUser(final UserParticipant toSearch, final UserParticipant[] userParticipants) {
        if (null == userParticipants) {
            return true;
        }
        for (final UserParticipant userParticipant : userParticipants) {
            if (userParticipant != null && userParticipant.equals(toSearch)) {
                return true;
            }
        }
        return false;
    }

    private static final boolean contains(final Participant toSearch, final Participant[] participants) {
        if (null == participants) {
            return true;
        }
        for (final Participant participant : participants) {
            if (participant != null && participant.equals(toSearch)) {
                return true;
            }
        }
        return false;
    }

    private static final boolean compareRecurrenceInformation(final CalendarObject o1, final CalendarObject o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1.getRecurrenceType() != o2.getRecurrenceType()) {
            return false;
        }
        if (CalendarObject.DAILY == o1.getRecurrenceType()) {
            return o1.getInterval() == o2.getInterval();
        }
        if (CalendarObject.WEEKLY == o1.getRecurrenceType()) {
            if (o1.getInterval() != o2.getInterval()) {
                return false;
            }
            return o1.getDays() == o2.getDays();
        }
        if (CalendarObject.MONTHLY == o1.getRecurrenceType()) {
            if (o1.getInterval() != o2.getInterval()) {
                return false;
            }
            if (o1.getDays() != o2.getDays()) {
                return false;
            }
            return o1.getDayInMonth() == o2.getDayInMonth();
        }
        if (CalendarObject.YEARLY == o1.getRecurrenceType()) {
            if (o1.getMonth() != o2.getMonth()) {
                return false;
            }
            if (o1.getDays() != o2.getDays()) {
                return false;
            }
            return o1.getDayInMonth() == o2.getDayInMonth();
        }
        return true;
    }

    /**
     * Compares given {@link Object} references.
     * 
     * @param o1 The first object
     * @param o2 The second object
     * @return <code>true</code> if both {@link Object} references are considered to be equal; otherwise <code>false</code>
     */
    static final boolean compareObjects(final Object o1, final Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null) {
            if (o2 == null) {
                return true;
            }
            return false;
        }
        return o1.equals(o2);
    }

    static final boolean compareDates(final Date[] dates1, final Date[] dates2) {
        if (dates1 == dates2) {
            return true;
        }
        if (dates1 == null) {
            if (dates2 == null) {
                return true;
            }
            return dates2.length == 0 ? true : false;
        }
        if (dates2 == null && dates1.length == 0) {
            return true;
        }
        return Arrays.equals(dates1, dates2);
    }

    /**
     * Compares given {@link String} references.
     * <p>
     * Note: A <code>null</code> reference and an empty string are considered to be equal. Otherwise use
     * {@link #compareObjects(Object, Object)}
     * 
     * @param s1 The first string
     * @param s2 The second string
     * @return <code>true</code> if both {@link String} references are considered to be equal; otherwise <code>false</code>
     */
    static final boolean compareStrings(final String s1, final String s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null) {
            if (s2 == null) {
                return true;
            }
            return s2.length() == 0 ? true : false;
        }
        if (s2 == null && s1.length() == 0) {
            return true;
        }
        return s1.equals(s2);
    }

    /**
     * Checks the dates of specified calendar object.
     * <ul>
     * <li>For a single event: if it is not more than 30 minutes in the past and its end date is not in the past compared to
     * {@link System#currentTimeMillis()}.</li>
     * <li>For a recurring event: if its until date is in the past</li>
     * </ul>
     * 
     * @param calendarObj The calendar object whose start and end date is ought to be checked
     * @param module The module
     * @return <code>true</code> if notifications shall be dropped; otherwise <code>false</code>.
     */
    static final boolean checkStartAndEndDate(final CalendarObject calendarObj, final int module) {
        final long now = System.currentTimeMillis();
        if (CalendarObject.NO_RECURRENCE == calendarObj.getRecurrenceType()) {
            {
                // Do not send notification mails for tasks and appointments in the
                // past. Bug #12063
                final Date endDate = calendarObj.getEndDate();
                if (endDate != null) {
                    if (Types.APPOINTMENT == module && endDate.getTime() < now) {
                        if (DEBUG) {
                            LOG.debug(new StringBuilder().append("Ignoring notification(s) for single appointment object ").append(
                                calendarObj.getObjectID()).append(" since its end date is in the past").toString());
                        }
                        return false;
                    }
                    if (Types.TASK == module && !compare2Date(endDate.getTime(), now)) {
                        if (DEBUG) {
                            LOG.debug(new StringBuilder().append("Ignoring notification(s) for single task object ").append(
                                calendarObj.getObjectID()).append(" since its end date is in the past").toString());
                        }
                        return false;
                    }
                }
            }
        } else {
            final Date untilDate = calendarObj.getUntil();
            if (null != untilDate) {
                if (Types.APPOINTMENT == module && untilDate.getTime() < now) {
                    if (DEBUG) {
                        LOG.debug(new StringBuilder().append("Ignoring notification(s) for recurring appointment object ").append(
                            calendarObj.getObjectID()).append(" since its until date is in the past").toString());
                    }
                    return false;
                }
                if (Types.TASK == module && !compare2Date(untilDate.getTime(), now)) {
                    if (DEBUG) {
                        LOG.debug(new StringBuilder().append("Ignoring notification(s) for recurring task object ").append(
                            calendarObj.getObjectID()).append(" since its until date is in the past").toString());
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Compares if given time millis fit into the date denoted by specified date millis.
     * 
     * @param date The date millis
     * @param millis The time millis
     * @return <code>true</code> if given time millis fit into the date denoted by specified date millis; otherwise <code>false</code>
     */
    private static boolean compare2Date(final long date, final long millis) {
        return date >= (millis - (millis % Constants.MILLI_DAY));
    }

    /**
     * Computes the first occurence's end time.
     * 
     * @param startMillis The start time in UTC milliseconds
     * @param endMillis The end time in UTC milliseconds
     * @return The first occurence's end time.
     */
    private static Date computeFirstOccurrenceEnd(final long startMillis, final long endMillis) {
        final Calendar cal = GregorianCalendar.getInstance(getCalendarTools().getTimeZone("UTC"), Locale.ENGLISH);
        cal.setTimeInMillis(endMillis);
        final int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
        final int minutes = cal.get(Calendar.MINUTE);
        cal.setTimeInMillis(startMillis);
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minutes);
        return cal.getTime();
    }

    private static Date computeFirstOccurrenceEnd(final CalendarObject app) throws OXException {
        final CalendarCollectionService service = ServerServiceRegistry.getInstance().getService(CalendarCollectionService.class);
        final RecurringResultsInterface recurrences = service.calculateFirstRecurring(app);
        final RecurringResultInterface recurringResult = recurrences.getRecurringResult(0);
        return new Date(recurringResult.getEnd());
    }

    /**
     * Checks if specified appointment is a change exception.
     * 
     * @param event The event to examine
     * @return <code>true</code> if specified appointment is a change exception; otherwise <code>false</code>
     */
    private static boolean isChangeException(final CalendarObject appointment) {
        return appointment.containsObjectID() && appointment.containsRecurrenceID() && appointment.getRecurrenceID() > 0 && appointment.getObjectID() != appointment.getRecurrenceID();
    }

    /**
     * Gets the recurrence master's title of specified event.
     * 
     * @param appointment The change exception
     * @param ctx The context
     * @return The recurrence master's title or <code>null</code>.
     */
    private static String getRecurrenceTitle(final CalendarObject appointment, final Context ctx) {
        final int recurrenceId = appointment.getRecurrenceID();
        if (recurrenceId <= 0) {
            return null;
        }
        try {
            return getCalendarTools().getAppointmentTitle(recurrenceId, ctx);
        } catch (final OXCalendarException e) {
            return null;
        }
    }

    private static CalendarCollectionService getCalendarTools() {
        return ServerServiceRegistry.getInstance().getService(CalendarCollectionService.class);
    }
}
