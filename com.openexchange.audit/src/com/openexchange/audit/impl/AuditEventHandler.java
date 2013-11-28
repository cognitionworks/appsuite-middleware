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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.audit.impl;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import org.apache.commons.lang.Validate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import com.openexchange.api2.FolderSQLInterface;
import com.openexchange.api2.RdbFolderSQLInterface;
import com.openexchange.audit.configuration.AuditConfiguration;
import com.openexchange.event.CommonEvent;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.FileStorageEventConstants;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.contact.Contacts;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.groupware.tools.iterator.FolderObjectIterator;
import com.openexchange.java.Strings;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * @author <a href="mailto:benjamin.otterbach@open-xchange.com">Benjamin Otterbach</a>
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a> - refactoring
 */
public class AuditEventHandler implements EventHandler {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AuditEventHandler.class);

    private static final SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Initializes a new {@link AuditEventHandler}.
     */
    public AuditEventHandler() {
        super();
        try {
            if (AuditConfiguration.getEnabled() == true) {
                LOG.info("Using own Logging instance.");
            } else {
                LOG.info("Using global Logging instance.");
            }
        } catch (final OXException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleEvent(final Event event) {
        Validate.notNull(event, "Event mustn't be null.");

        if (!LOG.isInfoEnabled()) {
            // Not allowed to log
            return;
        }
        try {
            final StringBuilder log = new StringBuilder(2048);
            final String topic = event.getTopic();

            if (topic.startsWith("com/openexchange/groupware/infostore/")) {
                handleInfostoreEvent(event, log);
            } else if (topic.startsWith("com/openexchange/groupware/")) {
                handleGroupwareEvent(event, log);
            }

            final String infoMsg = log.toString();
            if (!Strings.isEmpty(infoMsg)) {
                LOG.info(infoMsg);
            }
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * Handles events that belong to the infostore.
     *
     * @param event - the {@link Event} that was received
     * @param log - the log to add information
     * @throws OXException
     */
    protected void handleInfostoreEvent(Event event, StringBuilder log) throws OXException {
        Validate.notNull(event, "Event mustn't be null.");
        Validate.notNull(log, "StringBuilder to write to mustn't be null.");

        String topic = event.getTopic();

        if (topic.equals(FileStorageEventConstants.ACCESS_TOPIC)) {
            if (AuditConfiguration.getFileAccessLogging()) {
                log.append("EVENT TYPE: ACCESS; ");
            } else {
                return;
            }
        }
        if (topic.equals(FileStorageEventConstants.CREATE_TOPIC)) {
            log.append("EVENT TYPE: INSERT; ");
        } else if (topic.equals(FileStorageEventConstants.UPDATE_TOPIC)) {
            log.append("EVENT TYPE: UPDATE; ");
        } else if (topic.equals(FileStorageEventConstants.DELETE_TOPIC)) {
            log.append("EVENT TYPE: DELETE; ");
        }
        synchronized (logDateFormat) {
            log.append("EVENT TIME: ").append(logDateFormat.format(new Date())).append("; ");
        }
        log.append("OBJECT TYPE: FILE; ");

        final Session session = (Session) event.getProperty(FileStorageEventConstants.SESSION);
        if (Boolean.TRUE.equals(session.getParameter(Session.PARAM_PUBLICATION))) {
            String remoteAddress = null;
            try {
                remoteAddress = (String) event.getProperty("remoteAddress");
            } catch (ClassCastException e) {
                remoteAddress = "unknown";
            }

            if (remoteAddress == null) {
                remoteAddress = "unknown";
            }

            log.append("PUBLISH: ");
            log.append(remoteAddress);
            log.append("; ");
        } else {
            appendUserInformation(session.getUserId(), session.getContextId(), log);
        }
        log.append("CONTEXT ID: ").append(session.getContextId()).append("; ");
        log.append("OBJECT ID: ").append(event.getProperty(FileStorageEventConstants.OBJECT_ID)).append("; ");
        {
            final Object fileName = event.getProperty(FileStorageEventConstants.FILE_NAME);
            if (null != fileName) {
                log.append("FILE NAME: ").append(fileName).append("; ");
            }
        }
        log.append("SERVICE ID: ").append(event.getProperty(FileStorageEventConstants.SERVICE)).append("; ");
        log.append("ACCOUNT ID: ").append(event.getProperty(FileStorageEventConstants.ACCOUNT_ID)).append("; ");
        {
            final String folderId = (String) event.getProperty(FileStorageEventConstants.FOLDER_ID);
            if (null != folderId) {
                try {
                    final int iFolderId = Integer.parseInt(folderId);
                    log.append("FOLDER: ").append(getPathToRoot(iFolderId, session)).append(';');
                } catch (NumberFormatException e) {
                    log.append("FOLDER: ").append(folderId).append(';');
                }
            }
        }
    }

    /**
     * Handles events that belong to other server parts
     *
     * @param event - the {@link Event} that was received
     * @param log - the log to add information
     * @throws OXException
     */
    protected void handleGroupwareEvent(Event event, StringBuilder log) throws OXException {
        Validate.notNull(event, "Event mustn't be null.");
        Validate.notNull(log, "StringBuilder to write to mustn't be null.");

        final CommonEvent commonEvent = (CommonEvent) event.getProperty(CommonEvent.EVENT_KEY);

        if (null != commonEvent) {
            final int contextId = commonEvent.getContextId();
            final Context context = ContextStorage.getInstance().getContext(contextId);

            handleMainCommmonEvent(commonEvent, log);

            ModuleSwitch: switch (commonEvent.getModule()) {
            default:
                break ModuleSwitch;

            case Types.APPOINTMENT:
                handleAppointmentCommonEvent(commonEvent, context, log);
                break ModuleSwitch;

            case Types.CONTACT:
                handleContactCommonEvent(commonEvent, context, log);
                break ModuleSwitch;

            case Types.TASK:
                handleTaskCommonEvent(commonEvent, context, log);
                break ModuleSwitch;

            case Types.INFOSTORE:
                handleInfostoreCommonEvent(commonEvent, context, log);
                break ModuleSwitch;
            }
        }
    }

    /**
     * Handles the general information of a CommonEvent that should be logged for all action objects.
     *
     * @param commonEvent
     * @param log
     */
    protected void handleMainCommmonEvent(CommonEvent commonEvent, StringBuilder log) {
        Validate.notNull(commonEvent, "CommonEvent mustn't be null.");
        Validate.notNull(log, "StringBuilder to write to mustn't be null.");

        if (commonEvent.getAction() == CommonEvent.INSERT) {
            log.append("EVENT TYPE: INSERT; ");
        } else if (commonEvent.getAction() == CommonEvent.UPDATE) {
            log.append("EVENT TYPE: UPDATE; ");
        } else if (commonEvent.getAction() == CommonEvent.DELETE) {
            log.append("EVENT TYPE: DELETE; ");
        }

        synchronized (logDateFormat) {
            log.append("EVENT TIME: ").append(logDateFormat.format(new Date())).append("; ");
        }
    }

    /**
     * Handles appointment events.
     *
     * @param event - the {@link CommonEvent} that was received
     * @param context - the {@link Context}
     * @param log - the log to add information
     * @throws OXException
     */
    protected void handleAppointmentCommonEvent(CommonEvent commonEvent, Context context, StringBuilder log) throws OXException {
        Validate.notNull(commonEvent, "CommonEvent mustn't be null.");
        Validate.notNull(log, "StringBuilder to write to mustn't be null.");

        final Appointment appointment = (Appointment) commonEvent.getActionObj();
        Appointment oldAppointment = (Appointment) commonEvent.getOldObj();

        log.append("OBJECT TYPE: APPOINTMENT; ");
        appendUserInformation(commonEvent.getUserId(), commonEvent.getContextId(), log);
        log.append("CONTEXT ID: ").append(commonEvent.getContextId()).append("; ");
        log.append("OBJECT ID: ").append(appointment.getObjectID()).append("; ");
        log.append("CREATED BY: ").append(UserStorage.getInstance().getUser(appointment.getCreatedBy(), context).getDisplayName()).append(
            "; ");
        log.append("MODIFIED BY: ").append(UserStorage.getInstance().getUser(appointment.getModifiedBy(), context).getDisplayName()).append(
            "; ");
        log.append("TITLE: ").append(appointment.getTitle()).append("; ");
        log.append("START DATE: ").append(appointment.getStartDate()).append("; ");
        log.append("END DATE: ").append(appointment.getEndDate()).append("; ");
        log.append("FOLDER: ").append(getPathToRoot(appointment.getParentFolderID(), commonEvent.getSession())).append("; ");
        log.append("PARTICIPANTS: ").append(Arrays.toString(appointment.getParticipants())).append("; ");
        if (oldAppointment != null) {
            log.append("OLD PARTICIPANTS: ").append(Arrays.toString(oldAppointment.getParticipants())).append("; ");
        }
        if (commonEvent.getSession() != null) {
            log.append("CLIENT: ").append(commonEvent.getSession().getClient()).append("; ");
        }
    }

    /**
     * Handles contact events.
     *
     * @param event - the {@link CommonEvent} that was received
     * @param context - the {@link Context}
     * @param log - the log to add information
     * @throws OXException
     */
    protected void handleContactCommonEvent(CommonEvent commonEvent, Context context, StringBuilder log) throws OXException {
        Validate.notNull(commonEvent, "CommonEvent mustn't be null.");
        Validate.notNull(log, "StringBuilder to write to mustn't be null.");

        Contact contact = (Contact) commonEvent.getActionObj();

        if (CommonEvent.DELETE != commonEvent.getAction() && (null == contact || false == contact.containsDisplayName() || false == contact.containsCreatedBy() || false == contact.containsModifiedBy() || false == contact.containsObjectID() || false == contact.containsParentFolderID())) {
            contact = Contacts.getContactById(((Contact) commonEvent.getActionObj()).getObjectID(), commonEvent.getSession());
        }

        log.append("OBJECT TYPE: CONTACT; ");
        appendUserInformation(commonEvent.getUserId(), commonEvent.getContextId(), log);
        log.append("CONTEXT ID: ").append(commonEvent.getContextId()).append("; ");
        if (null != contact) {
            log.append("OBJECT ID: ").append(contact.getObjectID()).append("; ");
            if (contact.containsCreatedBy()) {
                log.append("CREATED BY: ").append(
                    UserStorage.getInstance().getUser(contact.getCreatedBy(), context).getDisplayName()).append("; ");
            }
            if (contact.containsModifiedBy()) {
                log.append("MODIFIED BY: ").append(
                    UserStorage.getInstance().getUser(contact.getModifiedBy(), context).getDisplayName()).append("; ");
            }
            log.append("CONTACT FULLNAME: ").append(contact.getDisplayName()).append(';');
            log.append("FOLDER: ").append(getPathToRoot(contact.getParentFolderID(), commonEvent.getSession())).append(';');
        }
    }

    /**
     * Handles task events.
     *
     * @param event - the {@link CommonEvent} that was received
     * @param context - the {@link Context}
     * @param log - the log to add information
     * @throws OXException
     */
    protected void handleTaskCommonEvent(CommonEvent commonEvent, Context context, StringBuilder log) throws OXException {
        Validate.notNull(commonEvent, "CommonEvent mustn't be null.");
        Validate.notNull(log, "StringBuilder to write to mustn't be null.");

        final Task task = (Task) commonEvent.getActionObj();

        log.append("OBJECT TYPE: TASK; ");
        appendUserInformation(commonEvent.getUserId(), commonEvent.getContextId(), log);
        log.append("CONTEXT ID: ").append(commonEvent.getContextId()).append("; ");
        log.append("OBJECT ID: ").append(task.getObjectID()).append("; ");
        log.append("CREATED BY: ").append(UserStorage.getInstance().getUser(task.getCreatedBy(), context).getDisplayName()).append("; ");
        log.append("MODIFIED BY: ").append(UserStorage.getInstance().getUser(task.getModifiedBy(), context).getDisplayName()).append("; ");
        log.append("TITLE: ").append(task.getTitle()).append("; ");
        log.append("FOLDER: ").append(getPathToRoot(task.getParentFolderID(), commonEvent.getSession())).append(';');
    }

    /**
     * Handles infostore events.
     *
     * @param event - the {@link CommonEvent} that was received
     * @param context - the {@link Context}
     * @param log - the log to add information
     * @throws OXException
     */
    protected void handleInfostoreCommonEvent(CommonEvent commonEvent, Context context, StringBuilder log) throws OXException {
        Validate.notNull(commonEvent, "CommonEvent mustn't be null.");
        Validate.notNull(log, "StringBuilder to write to mustn't be null.");

        final DocumentMetadata document = (DocumentMetadata) commonEvent.getActionObj();

        log.append("OBJECT TYPE: INFOSTORE; ");
        appendUserInformation(commonEvent.getUserId(), commonEvent.getContextId(), log);
        log.append("CONTEXT ID: ").append(commonEvent.getContextId()).append("; ");
        log.append("OBJECT ID: ").append(document.getId()).append("; ");
        log.append("CREATED BY: ").append(UserStorage.getInstance().getUser(document.getCreatedBy(), context).getDisplayName()).append("; ");
        log.append("MODIFIED BY: ").append(UserStorage.getInstance().getUser(document.getModifiedBy(), context).getDisplayName()).append(
            "; ");
        log.append("TITLE: ").append(document.getTitle()).append("; ");
        log.append("TITLE: ").append(document.getFileName()).append("; ");
        log.append("FOLDER: ").append(getPathToRoot((int) document.getFolderId(), commonEvent.getSession())).append(';');
    }

    /**
     * This method will return the full folder path as String.
     *
     * @param folderId
     * @param sessionObj
     * @return String fullFolderPath
     */
    protected String getPathToRoot(final int folderId, final Session sessionObj) {
        String retval = "";

        try {
            final FolderSQLInterface foldersqlinterface = new RdbFolderSQLInterface(ServerSessionAdapter.valueOf(sessionObj));
            final Queue<FolderObject> q = ((FolderObjectIterator) foldersqlinterface.getPathToRoot(folderId)).asQueue();
            final int size = q.size();
            final Iterator<FolderObject> iter = q.iterator();
            for (int i = 0; i < size; i++) {
                retval = iter.next().getFolderName() + "/" + retval;
            }
        } catch (final OXException e) {
            LOG.error(e.getMessage(), e);
        }

        return retval;
    }

    private void appendUserInformation(final int userId, final int contextId, final StringBuilder log) {
        String displayName;
        try {
            displayName = UserStorage.getInstance().getUser(userId, ContextStorage.getInstance().getContext(contextId)).getDisplayName();
        } catch (final Exception e) {
            // Ignore
            displayName = null;
        }
        log.append("USER: ");
        if (null == displayName) {
            log.append(userId);
        } else {
            log.append(displayName);
            log.append(" (").append(userId).append(')');
        }
        log.append("; ");
    }

}
