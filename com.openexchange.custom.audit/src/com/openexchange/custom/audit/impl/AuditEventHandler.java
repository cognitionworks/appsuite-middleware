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

package com.openexchange.custom.audit.impl;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.openexchange.api2.FolderSQLInterface;
import com.openexchange.api2.OXException;
import com.openexchange.api2.RdbFolderSQLInterface;
import com.openexchange.custom.audit.configuration.AuditConfiguration;
import com.openexchange.custom.audit.logging.AuditFileHandler;
import com.openexchange.custom.audit.logging.AuditFilter;
import com.openexchange.event.CommonEvent;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.server.ServiceException;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.FolderObjectIterator;
import com.openexchange.tools.iterator.SearchIteratorException;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * @author <a href="mailto:benjamin.otterbach@open-xchange.com">Benjamin Otterbach</a>
 */
public class AuditEventHandler implements EventHandler {

	private static final Logger LOG = Logger.getLogger(AuditEventHandler.class.getName());

	private static final AuditEventHandler instance = new AuditEventHandler();
	
	private static final SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
    public static AuditEventHandler getInstance() {
        return instance;
    }
	
    /**
     * Initializes a new {@link AuditEventHandler}.
     */
	public AuditEventHandler() {
		super();
		
		try {
			/*
			 * Find out if the custom FileHandler should be used to log into
			 * a seperate logfile. If so, add a filter to the root logger to
			 * avoid that the messages will also be written to the master
			 * logfile.
			 */
			{
				if (AuditConfiguration.getEnabled() == true) {
					try {
						Logger rootLogger = Logger.getLogger("");
						Handler[] handlers = rootLogger.getHandlers();		
						for (int position = 0; position < handlers.length; position ++) {
							handlers[position].setFilter(new AuditFilter());
						}
						LOG.addHandler(new AuditFileHandler());
					} catch (SecurityException e) {
						LOG.log(Level.SEVERE, e.getMessage(), e);
					} catch (IOException e) {
						LOG.log(Level.SEVERE, e.getMessage(), e);
					}
					LOG.info("Using own Logging instance.");
				} else {
					LOG.info("Using global Logging instance.");
				}
			}
		} catch (ServiceException e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	public void handleEvent(final Event event) {
		try {
			StringBuffer log = new StringBuffer();
			
			final CommonEvent commonEvent = (CommonEvent) event.getProperty(CommonEvent.EVENT_KEY);
			final Context context = ContextStorage.getInstance().getContext(commonEvent.getContextId());
			
	        ModuleSwitch: switch (commonEvent.getModule()) {
	        default: break ModuleSwitch;
	        case Types.APPOINTMENT:	        	
	        	Appointment appointment = (Appointment)commonEvent.getActionObj();
	        	
				if (commonEvent.getAction() == CommonEvent.INSERT) {
					log.append("EVENT TYPE: INSERT; ");
				} else if (commonEvent.getAction() == CommonEvent.UPDATE) {
					log.append("EVENT TYPE: UPDATE; ");
				} else if (commonEvent.getAction() == CommonEvent.DELETE) {
					log.append("EVENT TYPE: DELETE; ");
				}
				
				log.append("EVENT TIME: " + logDateFormat.format(new Date()) + "; ");
				log.append("OBJECT TYPE: APPOINTMENT; ");
				log.append("CONTEXT ID: " + commonEvent.getContextId() + "; ");
				log.append("OBJECT ID: " + appointment.getObjectID() + "; ");
				log.append("CREATED BY: " + UserStorage.getInstance().getUser(appointment.getCreatedBy(), context).getDisplayName() + "; ");
				log.append("MODIFIED BY: " + UserStorage.getInstance().getUser(appointment.getModifiedBy(), context).getDisplayName() + "; ");
				log.append("TITLE: " + appointment.getTitle() + "; ");
				log.append("START DATE: " + appointment.getStartDate() + "; ");
				log.append("END DATE: " + appointment.getEndDate() + "; ");
				log.append("FOLDER: " + getPathToRoot(appointment.getParentFolderID(), commonEvent.getContextId(), commonEvent.getSession()) + ";");

	        	break ModuleSwitch;
	        case Types.CONTACT:
	        	Contact contact = (Contact)commonEvent.getActionObj();
	        	
				if (commonEvent.getAction() == CommonEvent.INSERT) {
					log.append("EVENT TYPE: INSERT; ");
				} else if (commonEvent.getAction() == CommonEvent.UPDATE) {
					log.append("EVENT TYPE: UPDATE; ");
				} else if (commonEvent.getAction() == CommonEvent.DELETE) {
					log.append("EVENT TYPE: DELETE; ");
				}
				
				log.append("EVENT TIME: " + logDateFormat.format(new Date()) + "; ");
				log.append("OBJECT TYPE: CONTACT; ");
				log.append("CONTEXT ID: " + commonEvent.getContextId() + "; ");
				log.append("OBJECT ID: " + contact.getObjectID() + "; ");
				log.append("CREATED BY: " + UserStorage.getInstance().getUser(contact.getCreatedBy(), context).getDisplayName() + "; ");
				log.append("MODIFIED BY: " + UserStorage.getInstance().getUser(contact.getModifiedBy(), context).getDisplayName() + "; ");
				log.append("CONTACT FULLNAME: " + contact.getDisplayName() + ";");
				log.append("FOLDER: " + getPathToRoot(contact.getParentFolderID(), commonEvent.getContextId(), commonEvent.getSession()) + ";");
				
	        	break ModuleSwitch;
	        case Types.TASK:
	        	Task task = (Task)commonEvent.getActionObj();
	        	
				if (commonEvent.getAction() == CommonEvent.INSERT) {
					log.append("EVENT TYPE: INSERT; ");
				} else if (commonEvent.getAction() == CommonEvent.UPDATE) {
					log.append("EVENT TYPE: UPDATE; ");
				} else if (commonEvent.getAction() == CommonEvent.DELETE) {
					log.append("EVENT TYPE: DELETE; ");
				}
				
				log.append("EVENT TIME: " + logDateFormat.format(new Date()) + "; ");
				log.append("OBJECT TYPE: TASK; ");
				log.append("CONTEXT ID: " + commonEvent.getContextId() + "; ");
				log.append("OBJECT ID: " + task.getObjectID() + "; ");
				log.append("CREATED BY: " + UserStorage.getInstance().getUser(task.getCreatedBy(), context).getDisplayName() + "; ");
				log.append("MODIFIED BY: " + UserStorage.getInstance().getUser(task.getModifiedBy(), context).getDisplayName() + "; ");
				log.append("TITLE: " + task.getTitle() + "; ");
				log.append("FOLDER: " + getPathToRoot(task.getParentFolderID(), commonEvent.getContextId(), commonEvent.getSession()) + ";");
				
	        	break ModuleSwitch;
	        case Types.INFOSTORE:
	        	DocumentMetadata document = (DocumentMetadata)commonEvent.getActionObj();
	        	
				if (commonEvent.getAction() == CommonEvent.INSERT) {
					log.append("EVENT TYPE: INSERT; ");
				} else if (commonEvent.getAction() == CommonEvent.UPDATE) {
					log.append("EVENT TYPE: UPDATE; ");
				} else if (commonEvent.getAction() == CommonEvent.DELETE) {
					log.append("EVENT TYPE: DELETE; ");
				}
				
				log.append("EVENT TIME: " + logDateFormat.format(new Date()) + "; ");
				log.append("OBJECT TYPE: INFOSTORE; ");
				log.append("CONTEXT ID: " + commonEvent.getContextId() + "; ");
				log.append("OBJECT ID: " + document.getId() + "; ");
				log.append("CREATED BY: " + UserStorage.getInstance().getUser(document.getCreatedBy(), context).getDisplayName() + "; ");
				log.append("MODIFIED BY: " + UserStorage.getInstance().getUser(document.getModifiedBy(), context).getDisplayName() + "; ");
				log.append("TITLE: " + document.getTitle() + "; ");
				log.append("TITLE: " + document.getFileName() + "; ");
				log.append("FOLDER: " + getPathToRoot((int)document.getFolderId(), commonEvent.getContextId(), commonEvent.getSession()) + ";");
				
	        	break ModuleSwitch;
	        }
			
			if (LOG.isLoggable(Level.INFO)) {
				LOG.log(Level.INFO, log.toString());
			}
			
		} catch (final Exception e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}	
	}
	
	/**
	 * This method will return the full folder path as String.
	 * @param folderId
	 * @param contextId
	 * @param sessionObj
	 * @return String fullFolderPath
	 */
	private String getPathToRoot(int folderId, int contextId, Session sessionObj) {
		String retval = "";
		
		try {
			final FolderSQLInterface foldersqlinterface = new RdbFolderSQLInterface(new ServerSessionAdapter(sessionObj));
			final Queue<FolderObject> q = ((FolderObjectIterator) foldersqlinterface.getPathToRoot(folderId)).asQueue();
			final int size = q.size();
			final Iterator<FolderObject> iter = q.iterator();
			for (int i = 0; i < size; i++) {
			    retval = iter.next().getFolderName() + "/" + retval;
			}
		} catch (ContextException e) {
			e.printStackTrace();
		} catch (SearchIteratorException e) {
			e.printStackTrace();
		} catch (OXException e) {
			e.printStackTrace();
		}
		
		return retval;
	}
	
}
