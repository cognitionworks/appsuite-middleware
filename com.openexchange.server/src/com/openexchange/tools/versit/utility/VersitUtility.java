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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.tools.versit.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.api2.TasksSQLInterface;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.calendar.AppointmentSqlFactoryService;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.container.CommonObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.groupware.tasks.TasksSQLImpl;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.versit.Versit;
import com.openexchange.tools.versit.VersitDefinition;
import com.openexchange.tools.versit.VersitObject;
import com.openexchange.tools.versit.converter.ConverterException;
import com.openexchange.tools.versit.converter.OXContainerConverter;

/**
 * {@link VersitUtility}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class VersitUtility {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(VersitUtility.class);

    /**
     * No instantiation
     */
    private VersitUtility() {
        super();
    }

    private static final String VERSIT_VTODO = "VTODO";

    private static final String VERSIT_VEVENT = "VEVENT";

    /**
     * Saves specified <code>ICalendar</code> mail part into corresponding default folders. The resulting instances of {@link CommonObject}
     * are added to given list.
     *
     * @param icalInputStream The ICal input stream
     * @param baseContentType The ICal's base content type (e.g. <i>text/calendar</i>)
     * @param charset The charset encoding of provided input stream's data
     * @param retvalList The list to which the resulting instance of {@link CommonObject} is added
     * @param session The session providing needed user data
     * @param ctx The context
     * @throws IOException If an I/O error occurs
     * @throws ConverterException If input stream's data cannot be converted to ICal object
     * @throws OXException If ICal object cannot be put into session user's default contact folder
     */
    public static void saveICal(final InputStream icalInputStream, final String baseContentType, final String charset, final List<CommonObject> retvalList, final Session session, final Context ctx) throws IOException, ConverterException, OXException {
        /*
         * Define versit reader
         */
        final VersitDefinition def = Versit.getDefinition(baseContentType);
        final VersitDefinition.Reader r = def.getReader(icalInputStream, charset);
        /*
         * Ok, convert versit object to corresponding data object and save this object via its interface
         */
        OXContainerConverter oxc = null;
        AppointmentSQLInterface appointmentInterface = null;
        TasksSQLInterface taskInterface = null;
        try {
            oxc = new OXContainerConverter(session);
            final VersitObject rootVersitObj = def.parseBegin(r);
            VersitObject vo = null;
            int defaultCalendarFolder = -1;
            int defaultTaskFolder = -1;
            final OXFolderAccess access = new OXFolderAccess(ctx);
            while ((vo = def.parseChild(r, rootVersitObj)) != null) {
                if (VERSIT_VEVENT.equals(vo.name)) {
                    /*
                     * An appointment
                     */
                    final CalendarDataObject appointmentObj = oxc.convertAppointment(vo);
                    appointmentObj.setContext(ctx);
                    if (defaultCalendarFolder == -1) {
                        defaultCalendarFolder = access.getDefaultFolderID(session.getUserId(), FolderObject.CALENDAR);
                    }
                    appointmentObj.setParentFolderID(defaultCalendarFolder);
                    /*
                     * Create interface if not done, yet
                     */
                    if (appointmentInterface == null) {
                        appointmentInterface = ServerServiceRegistry.getInstance().getService(AppointmentSqlFactoryService.class).createAppointmentSql(session);
                    }
                    appointmentInterface.insertAppointmentObject(appointmentObj);
                    /*
                     * Add to list
                     */
                    retvalList.add(appointmentObj);
                } else if (VERSIT_VTODO.equals(vo.name)) {
                    /*
                     * A task
                     */
                    final Task taskObj = oxc.convertTask(vo);
                    if (defaultTaskFolder == -1) {
                        defaultTaskFolder = access.getDefaultFolderID(session.getUserId(), FolderObject.TASK);
                    }
                    taskObj.setParentFolderID(defaultTaskFolder);
                    /*
                     * Create interface if not done, yet
                     */
                    if (taskInterface == null) {
                        taskInterface = new TasksSQLImpl(session);
                    }
                    taskInterface.insertTaskObject(taskObj);
                    /*
                     * Add to list
                     */
                    retvalList.add(taskObj);
                } else {
                    LOG.warn("invalid versit object: {}", vo.name);
                }
            }
        } finally {
            if (oxc != null) {
                oxc.close();
                oxc = null;
            }
        }
    }
}
