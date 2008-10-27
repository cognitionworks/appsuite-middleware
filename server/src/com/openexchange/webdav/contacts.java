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

package com.openexchange.webdav;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.output.XMLOutputter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.openexchange.api.OXConflictException;
import com.openexchange.api.OXMandatoryFieldException;
import com.openexchange.api.OXObjectNotFoundException;
import com.openexchange.api.OXPermissionException;
import com.openexchange.api2.ContactSQLInterface;
import com.openexchange.api2.OXConcurrentModificationException;
import com.openexchange.api2.OXException;
import com.openexchange.api2.RdbContactSQLInterface;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.container.ContactObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.monitoring.MonitoringInfo;
import com.openexchange.session.Session;
import com.openexchange.webdav.xml.ContactParser;
import com.openexchange.webdav.xml.ContactWriter;
import com.openexchange.webdav.xml.DataParser;
import com.openexchange.webdav.xml.XmlServlet;

/**
 * contacts
 * 
 * @author <a href="mailto:sebastian.kauss@netline-is.de">Sebastian Kauss</a>
 */
public final class contacts extends XmlServlet {

	private static final long serialVersionUID = -3731372041610025543L;

	private static final transient Log LOG = LogFactory.getLog(contacts.class);

	private final Queue<QueuedContact> pendingInvocations;

	public contacts() {
		super();
		pendingInvocations = new LinkedList<QueuedContact>();
	}

	@Override
	protected void parsePropChilds(final HttpServletRequest req, final HttpServletResponse resp,
			final XmlPullParser parser) throws XmlPullParserException, IOException, AbstractOXException {
		final Session session = getSession(req);

		if (isTag(parser, "prop", "DAV:")) {
			/*
			 * Adjust parser
			 */
			parser.nextTag();

			final ContactObject contactobject = new ContactObject();

			final ContactParser contactparser = new ContactParser(session);
			contactparser.parse(parser, contactobject);

			final int method = contactparser.getMethod();

			final Date lastModified = contactobject.getLastModified();
			contactobject.removeLastModified();

			final int inFolder = contactparser.getFolder();

			/*
			 * Prepare contact for being queued
			 */
			switch (method) {
			case DataParser.SAVE:
				if (contactobject.containsObjectID()) {
					pendingInvocations.add(new QueuedContact(contactobject, contactparser, method, lastModified,
							inFolder));
				} else {
					contactobject.setParentFolderID(inFolder);

					if (contactobject.containsImage1() && contactobject.getImage1() == null) {
						contactobject.removeImage1();
					}

					pendingInvocations.add(new QueuedContact(contactobject, contactparser, method, lastModified,
							inFolder));
				}
				break;
			case DataParser.DELETE:
				pendingInvocations.add(new QueuedContact(contactobject, contactparser, method, lastModified, inFolder));
				break;
			default:
				if (LOG.isDebugEnabled()) {
					LOG.debug("invalid method: " + method);
				}
			}
		} else {
			parser.next();
		}
	}

	@Override
	protected void performActions(final OutputStream os, final Session session) throws IOException, AbstractOXException {
		final ContactSQLInterface contactsql = new RdbContactSQLInterface(session);
		while (!pendingInvocations.isEmpty()) {
			final QueuedContact qcon = pendingInvocations.poll();
			if (null != qcon) {
				qcon.actionPerformed(contactsql, os, session.getUserId());
			}
		}
	}

	@Override
	protected void startWriter(final Session sessionObj, final Context ctx, final int objectId, final int folderId,
			final OutputStream os) throws Exception {
		final User userObj = UserStorage.getStorageUser(sessionObj.getUserId(), ctx);
		final ContactWriter contactwriter = new ContactWriter(userObj, ctx, sessionObj);
		contactwriter.startWriter(objectId, folderId, os);
	}

	@Override
	protected void startWriter(final Session sessionObj, final Context ctx, final int folderId,
			final boolean bModified, final boolean bDelete, final Date lastsync, final OutputStream os)
			throws Exception {
		startWriter(sessionObj, ctx, folderId, bModified, bDelete, false, lastsync, os);
	}

	@Override
	protected void startWriter(final Session sessionObj, final Context ctx, final int folderId,
			final boolean bModified, final boolean bDelete, final boolean bList, final Date lastsync,
			final OutputStream os) throws Exception {
		final User userObj = UserStorage.getStorageUser(sessionObj.getUserId(), ctx);
		final ContactWriter contactwriter = new ContactWriter(userObj, ctx, sessionObj);
		contactwriter.startWriter(bModified, bDelete, bList, folderId, lastsync, os);
	}

	@Override
	protected boolean hasModulePermission(final Session sessionObj, final Context ctx) {
		final UserConfiguration uc = UserConfigurationStorage.getInstance().getUserConfigurationSafe(
				sessionObj.getUserId(), ctx);
		return (uc.hasWebDAVXML() && uc.hasContact());
	}

	private final class QueuedContact {

		private final ContactObject contactObject;

		private final ContactParser contactParser;

		private final int action;

		private final Date lastModified;

		private final int inFolder;

		/**
		 * Initializes a new {@link QueuedTask}
		 * 
		 * @param contactObject
		 *            The contact object
		 * @param contactParser
		 *            The contact's parser
		 * @param action
		 *            The desired action
		 * @param lastModified
		 *            The last-modified date
		 * @param inFolder
		 *            The contact's folder
		 */
		public QueuedContact(final ContactObject contactObject, final ContactParser contactParser, final int action,
				final Date lastModified, final int inFolder) {
			super();
			this.contactObject = contactObject;
			this.contactParser = contactParser;
			this.action = action;
			this.lastModified = lastModified;
			this.inFolder = inFolder;
		}

		public void actionPerformed(final ContactSQLInterface contactsSQL, final OutputStream os, final int user)
				throws IOException {

			final XMLOutputter xo = new XMLOutputter();
			final String client_id = contactParser.getClientID();

			try {
				switch (action) {
				case DataParser.SAVE:
					if (contactObject.containsObjectID()) {
						if (lastModified == null) {
							throw new OXMandatoryFieldException("missing field last_modified");
						}

						contactsSQL.updateContactObject(contactObject, inFolder, lastModified);
					} else {
						contactsSQL.insertContactObject(contactObject);
					}
					break;
				case DataParser.DELETE:
					if (lastModified == null) {
						throw new OXMandatoryFieldException("missing field last_modified");
					}

					contactsSQL.deleteContactObject(contactObject.getObjectID(), inFolder, lastModified);
					break;
				default:
					throw new OXConflictException("invalid method: " + action);
				}
				writeResponse(contactObject, HttpServletResponse.SC_OK, OK, client_id, os, xo);
			} catch (final OXMandatoryFieldException exc) {
				LOG.debug(_parsePropChilds, exc);
				writeResponse(contactObject, HttpServletResponse.SC_CONFLICT, getErrorMessage(exc,
						MANDATORY_FIELD_EXCEPTION), client_id, os, xo);
			} catch (final OXPermissionException exc) {
				LOG.debug(_parsePropChilds, exc);
				writeResponse(contactObject, HttpServletResponse.SC_FORBIDDEN, getErrorMessage(exc,
						PERMISSION_EXCEPTION), client_id, os, xo);
			} catch (final OXConflictException exc) {
				LOG.debug(_parsePropChilds, exc);
				writeResponse(contactObject, HttpServletResponse.SC_CONFLICT, getErrorMessage(exc, CONFLICT_EXCEPTION),
						client_id, os, xo);
			} catch (final OXObjectNotFoundException exc) {
				LOG.debug(_parsePropChilds, exc);
				writeResponse(contactObject, HttpServletResponse.SC_NOT_FOUND, OBJECT_NOT_FOUND_EXCEPTION, client_id,
						os, xo);
			} catch (final OXConcurrentModificationException exc) {
				LOG.debug(_parsePropChilds, exc);
				writeResponse(contactObject, HttpServletResponse.SC_CONFLICT, MODIFICATION_EXCEPTION, client_id, os, xo);
			} catch (final OXException exc) {
				if (exc.getCategory() == Category.USER_INPUT) {
					LOG.debug(_parsePropChilds, exc);
					writeResponse(contactObject, HttpServletResponse.SC_CONFLICT, getErrorMessage(exc,
							USER_INPUT_EXCEPTION), client_id, os, xo);
				} else {
					LOG.error(_parsePropChilds, exc);
					writeResponse(contactObject, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, getErrorMessage(exc,
							SERVER_ERROR_EXCEPTION)
							+ exc.toString(), client_id, os, xo);
				}
			} catch (final Exception exc) {
				LOG.error(_parsePropChilds, exc);
				writeResponse(contactObject, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, getErrorMessage(
						SERVER_ERROR_EXCEPTION, "undefinied error")
						+ exc.toString(), client_id, os, xo);
			}

		}
	}

	@Override
	protected void decrementRequests() {
		MonitoringInfo.decrementNumberOfConnections(MonitoringInfo.OUTLOOK);
	}

	@Override
	protected void incrementRequests() {
		MonitoringInfo.incrementNumberOfConnections(MonitoringInfo.OUTLOOK);
	}
}
