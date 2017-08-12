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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.importexport.helpers;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.api2.TasksSQLInterface;
import com.openexchange.contact.ContactService;
import com.openexchange.exception.OXException;
import com.openexchange.folder.FolderService;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.groupware.tasks.TasksSQLImpl;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.importexport.exceptions.ImportExportExceptionMessages;
import com.openexchange.importexport.osgi.ImportExportServices;
import com.openexchange.java.Strings;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link ExportFileNameCreator}
 *
 * @author <a href="mailto:Jan-Oliver.Huhn@open-xchange.com">Jan-Oliver Huhn</a>
 * @since v7.10
 */
public class ExportFileNameCreator {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ExportFileNameCreator.class);

    /**
     * Prevent instantiation.
     */
    private ExportFileNameCreator() {
        super();
    }

    /**
     * Creates a file name based on the folder
     * 
     * @param session The session object
     * @param folder The folder to create the file name with
     * @return String The file name
     */
    public static String createFolderExportFileName(ServerSession session, String folder) {
        FolderService folderService = ImportExportServices.getFolderService();
        final StringBuilder sb = new StringBuilder();
        try {
            FolderObject folderObj = folderService.getFolderObject(Integer.parseInt(folder), session.getContextId());
            sb.append(folderObj.getFolderName());
        } catch (OXException e) {
            LOG.error(ImportExportExceptionMessages.COULD_NOT_CREATE_FILE_NAME, e);
            sb.append(getLocalizedFileName(session, ExportDefaultFileNames.DEFAULT_NAME));
        }
        sb.append(".");
        return sb.toString();
    }

    /**
     * Creates a file name based on the a batch of folders and objects
     * 
     * @param session The session object
     * @param batchIds The batchIds to create the file name with
     * @return String The file name
     */
    public static String createBatchExportFileName(ServerSession session, Map<String, List<String>> batchIds) {
        StringBuilder sb = new StringBuilder();
        Entry<String, List<String>> entry = batchIds.entrySet().iterator().next();
        if (batchIds.size() == 1) {
            //check for contacts of the same folder
            if (entry.getValue().size() > 1) {
                sb.append(createBatchExportFileName(session, entry.getKey(), null));
            } else {
                //exactly one contact to export, file name equals contact name
                sb.append(createBatchExportFileName(session, entry.getKey(), entry.getValue().get(0)));
            }
        } else {
            //batch of contact ids from different folders, file name is set to a default
            FolderService folderService = ImportExportServices.getFolderService();
            try {
                FolderObject folderObj = folderService.getFolderObject(Integer.parseInt(entry.getKey()), session.getContextId());
                if (FolderObject.CONTACT == folderObj.getModule()) {
                    sb.append(getLocalizedFileName(session, ExportDefaultFileNames.CONTACTS_NAME));
                } else if (FolderObject.CALENDAR == folderObj.getModule()) {
                    sb.append(getLocalizedFileName(session, ExportDefaultFileNames.ICAL_APPOINTMENT_NAME));
                } else if (FolderObject.TASK == folderObj.getModule()) {
                    sb.append(getLocalizedFileName(session, ExportDefaultFileNames.ICAL_TASKS_NAME));
                }
                } catch (OXException e) {
                    LOG.error(ImportExportExceptionMessages.COULD_NOT_CREATE_FILE_NAME, e);
                    sb.append(getLocalizedFileName(session, ExportDefaultFileNames.DEFAULT_NAME));
                }
            sb.append(".");
        }
        return sb.toString();
    }

    /**
     * Helper method for creating a file name based on the folder and the object
     * 
     * @param session The session object
     * @param folder The folderId to create the file name with
     * @param batchId The batchId to create the file name with
     * @return String The file name
     */
    private static String createBatchExportFileName(ServerSession session, String folder, String batchId) {
        StringBuilder sb = new StringBuilder();
        FolderService folderService = ImportExportServices.getFolderService();
        try {
            FolderObject folderObj = folderService.getFolderObject(Integer.parseInt(folder), session.getContextId());
            if (null == batchId || batchId.equals("")) {
                sb.append(getLocalizedFileName(session, folderObj.getFolderName()));
            } else {
                    if (FolderObject.CONTACT == folderObj.getModule()) {
                        sb.append(createSingleContactName(session, folder, batchId));
                    } else if (FolderObject.CALENDAR == folderObj.getModule()) {
                        AppointmentSQLInterface appointmentSql = ImportExportServices.getAppointmentFactoryService().createAppointmentSql(session);
                        Appointment appointmentObj = appointmentSql.getObjectById(Integer.parseInt(batchId), Integer.parseInt(folder));
                        String title = appointmentObj.getTitle();
                        if (Strings.isEmpty(title)) {
                            sb.append(getLocalizedFileName(session, ExportDefaultFileNames.ICAL_APPOINTMENT_NAME));
                        } else {
                            sb.append(title);
                        }
                    } else if (FolderObject.TASK == folderObj.getModule()) {
                        TasksSQLInterface tasksSql = new TasksSQLImpl(session);
                        Task taskObj = tasksSql.getTaskById(Integer.parseInt(batchId), Integer.parseInt(folder));
                        String title = taskObj.getTitle();
                        if (Strings.isEmpty(title)) {
                            sb.append(getLocalizedFileName(session, ExportDefaultFileNames.ICAL_TASKS_NAME));
                        } else {
                            sb.append(title);
                        }
                    }
            }
        } catch (OXException e) {
            LOG.error(ImportExportExceptionMessages.COULD_NOT_CREATE_FILE_NAME, e);
            sb.append(getLocalizedFileName(session, ExportDefaultFileNames.DEFAULT_NAME));
        } catch (SQLException e) {
            LOG.error(ImportExportExceptionMessages.COULD_NOT_CREATE_FILE_NAME, e);
            sb.append(getLocalizedFileName(session, ExportDefaultFileNames.DEFAULT_NAME));
        }
        sb.append(".");
        return sb.toString();
    }

    /**
     * Creates a localized string based on the file name
     * 
     * @param session The session object
     * @param fileName The file name of the module
     * @return String The localized file name
     */
    private static String getLocalizedFileName(ServerSession session, String fileName) {
        return StringHelper.valueOf(session.getUser().getLocale()).getString(fileName);
    }

    /**
     * Creates a file name for a single contact
     * 
     * @param session The session object
     * @param folder The folderId
     * @param batchId The objectId
     * @return String a file name for a single contact export
     * @throws OXException if contact is unavailable
     */
    private static String createSingleContactName(ServerSession session, String folder, String batchId) throws OXException {
        StringBuilder sb = new StringBuilder();
        ContactService contactService = ImportExportServices.getContactService();
        Contact contactObj = contactService.getContact(session, folder, batchId, null);
        if (contactObj.getMarkAsDistribtuionlist()) {
            String displayName = contactObj.getDisplayName();
            if (Strings.isEmpty(displayName)) {
                sb.append(getLocalizedFileName(session, ExportDefaultFileNames.CONTACTS_NAME));
            } else {
                sb.append(displayName);
            }
        } else {
            if (Strings.isEmpty(contactObj.getGivenName()) && Strings.isEmpty(contactObj.getSurName())) {
                sb.append(getLocalizedFileName(session, ExportDefaultFileNames.CONTACTS_NAME));
            } else if (!Strings.isEmpty(contactObj.getGivenName()) && !Strings.isEmpty(contactObj.getSurName())) {
                sb.append(contactObj.getGivenName() + " " + contactObj.getSurName());
            } else {
                if (!Strings.isEmpty(contactObj.getGivenName()) && Strings.isEmpty(contactObj.getSurName())) {
                    sb.append(contactObj.getGivenName());
                } else if (Strings.isEmpty(contactObj.getGivenName()) && !Strings.isEmpty(contactObj.getSurName())) {
                    sb.append(contactObj.getSurName());
                }
            }
        }
        return sb.toString();
    }

}
