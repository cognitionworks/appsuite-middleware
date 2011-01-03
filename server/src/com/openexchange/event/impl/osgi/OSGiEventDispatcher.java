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

package com.openexchange.event.impl.osgi;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import com.openexchange.event.CommonEvent;
import com.openexchange.event.impl.AppointmentEventInterface;
import com.openexchange.event.impl.AppointmentEventInterface2;
import com.openexchange.event.impl.EventDispatcher;
import com.openexchange.event.impl.TaskEventInterface;
import com.openexchange.event.impl.TaskEventInterface2;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.session.Session;

/**
 * Grabs events from the OSGi Event Admin and disseminates them to server listeners. Only handles appointments, and has to be extended once
 * needed.
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class OSGiEventDispatcher implements EventHandlerRegistration, EventDispatcher {

    private static final Log LOG = LogFactory.getLog(OSGiEventDispatcher.class);

    private final List<AppointmentEventInterface> appointmentListeners;

    private final List<TaskEventInterface> taskListeners;

    private ServiceRegistration serviceRegistration;

    /**
     * Initializes a new {@link OSGiEventDispatcher}.
     */
    public OSGiEventDispatcher() {
        super();
        appointmentListeners = new ArrayList<AppointmentEventInterface>();
        taskListeners = new ArrayList<TaskEventInterface>();
    }

    public void addListener(final AppointmentEventInterface listener) {
        this.appointmentListeners.add(listener);
    }

    public void created(final Appointment appointment, final Session session) {
        for (final AppointmentEventInterface listener : appointmentListeners) {
            listener.appointmentCreated(appointment, session);
        }
    }

    public void modified(final Appointment oldAppointment, final Appointment newAppointment, final Session session) {
        for (final AppointmentEventInterface listener : appointmentListeners) {
            if (oldAppointment != null && AppointmentEventInterface2.class.isAssignableFrom(listener.getClass())) {
                ((AppointmentEventInterface2) listener).appointmentModified(oldAppointment, newAppointment, session);
            } else {
                listener.appointmentModified(newAppointment, session);
            }
        }
    }

    public void accepted(final Appointment appointment, final Session session) {
        for (final AppointmentEventInterface listener : appointmentListeners) {
            listener.appointmentAccepted(appointment, session);
        }
    }

    public void declined(final Appointment appointment, final Session session) {
        for (final AppointmentEventInterface listener : appointmentListeners) {
            listener.appointmentDeclined(appointment, session);
        }
    }

    public void tentativelyAccepted(final Appointment appointment, final Session session) {
        for (final AppointmentEventInterface listener : appointmentListeners) {
            listener.appointmentTentativelyAccepted(appointment, session);
        }
    }

    public void deleted(final Appointment appointment, final Session session) {
        for (final AppointmentEventInterface listener : appointmentListeners) {
            listener.appointmentDeleted(appointment, session);
        }
    }

    public void addListener(final TaskEventInterface listener) {
        this.taskListeners.add(listener);
    }

    public void created(final Task task, final Session session) {
        for (final TaskEventInterface listener : taskListeners) {
            listener.taskCreated(task, session);
        }
    }

    public void modified(final Task oldTask, final Task newTask, final Session session) {
        for (final TaskEventInterface listener : taskListeners) {
            if (oldTask != null && TaskEventInterface2.class.isAssignableFrom(listener.getClass())) {
                ((TaskEventInterface2) listener).taskModified(oldTask, newTask, session);
            } else {
                listener.taskModified(newTask, session);
            }
        }
    }

    public void accepted(final Task task, final Session session) {
        for (final TaskEventInterface listener : taskListeners) {
            listener.taskAccepted(task, session);
        }
    }

    public void declined(final Task task, final Session session) {
        for (final TaskEventInterface listener : taskListeners) {
            listener.taskDeclined(task, session);
        }
    }

    public void tentativelyAccepted(final Task task, final Session session) {
        for (final TaskEventInterface listener : taskListeners) {
            listener.taskTentativelyAccepted(task, session);
        }
    }

    public void modified(final Task task, final Session session) {
        modified(null, task, session);
    }

    public void deleted(final Task task, final Session session) {
        for (final TaskEventInterface listener : taskListeners) {
            listener.taskDeleted(task, session);
        }
    }

    public void handleEvent(final Event event) {
        try {
            final CommonEvent commonEvent = (CommonEvent) event.getProperty(CommonEvent.EVENT_KEY);

            final Object actionObj = commonEvent.getActionObj();
            final Object oldObj = commonEvent.getOldObj();
            final Session session = commonEvent.getSession();

            final int module = commonEvent.getModule();
            final int action = commonEvent.getAction();
            if (Types.APPOINTMENT == module) {
                if (CommonEvent.INSERT == action) {
                    created((Appointment) actionObj, session);
                } else if (CommonEvent.UPDATE == action || CommonEvent.MOVE == action) {
                    modified((Appointment) oldObj, (Appointment) actionObj, session);
                } else if (CommonEvent.DELETE == action) {
                    deleted((Appointment) actionObj, session);
                } else if (CommonEvent.CONFIRM_ACCEPTED == action) {
                    accepted((Appointment) actionObj, session);
                } else if (CommonEvent.CONFIRM_DECLINED == action) {
                    declined((Appointment) actionObj, session);
                } else if (CommonEvent.CONFIRM_TENTATIVE == action) {
                    tentativelyAccepted((Appointment) actionObj, session);
                }
            } else if (Types.TASK == module) {
                if (CommonEvent.INSERT == action) {
                    created((Task) actionObj, session);
                } else if (CommonEvent.UPDATE == action || CommonEvent.MOVE == action) {
                    modified((Task) oldObj, (Task) actionObj, session);
                } else if (CommonEvent.DELETE == action) {
                    deleted((Task) actionObj, session);
                } else if (CommonEvent.CONFIRM_ACCEPTED == action) {
                    accepted((Task) actionObj, session);
                } else if (CommonEvent.CONFIRM_DECLINED == action) {
                    declined((Task) actionObj, session);
                } else if (CommonEvent.CONFIRM_TENTATIVE == action) {
                    tentativelyAccepted((Task) actionObj, session);
                }
            }
        } catch (final Exception e) {
            // Catch all exceptions to get them into the normal logging
            // mechanism.
            LOG.error(e.getMessage(), e);
        }
    }

    public void registerService(final BundleContext context) {
        final Dictionary<Object, Object> serviceProperties = new Hashtable<Object, Object>();
        serviceProperties.put(EventConstants.EVENT_TOPIC, new String[] { "com/openexchange/groupware/*" });
        serviceRegistration = context.registerService(EventHandler.class.getName(), this, serviceProperties);
    }

    public void unregisterService() {
        if (null != serviceRegistration) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }
}
