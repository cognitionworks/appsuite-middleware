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

package com.openexchange.ajax;

import static com.openexchange.tools.oxfolder.OXFolderUtility.getUserName;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONWriter;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.helper.ParamContainer;
import com.openexchange.ajax.writer.ResponseWriter;
import com.openexchange.api2.OXException;
import com.openexchange.api2.sync.FolderSyncInterface;
import com.openexchange.api2.sync.RdbFolderSyncInterface;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.monitoring.MonitoringInfo;
import com.openexchange.session.Session;
import com.openexchange.tools.UnsynchronizedStringWriter;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.oxfolder.OXFolderException;
import com.openexchange.tools.oxfolder.OXFolderException.FolderCode;
import com.openexchange.tools.servlet.http.HttpServletResponseWrapper;
import com.openexchange.tools.servlet.http.Tools;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link SyncServlet} - The AJAX servlet to serve SyncML requests
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public class SyncServlet extends PermissionServlet {

	private static final long serialVersionUID = 8749478304854849616L;

	private static final transient org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory
			.getLog(SyncServlet.class);

	public static final String ACTION_REFRESH_SERVER = "refresh_server";

	/**
	 * Default constructor
	 */
	public SyncServlet() {
		super();
	}

	@Override
	protected void incrementRequests() {
		MonitoringInfo.incrementNumberOfConnections(MonitoringInfo.SYNCML);
	}

	@Override
	protected void decrementRequests() {
		MonitoringInfo.decrementNumberOfConnections(MonitoringInfo.SYNCML);
	}

	@Override
	protected boolean hasModulePermission(final ServerSession session) {
		return session.getUserConfiguration().hasSyncML();
	}

	@Override
	protected void doPut(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		resp.setContentType(CONTENTTYPE_JAVASCRIPT);
		Tools.disableCaching(resp);
		try {
			actionPut(req, resp);
		} catch (final AbstractOXException e) {
			LOG.error("SyncServlet.doPut()", e);
			final Writer writer;
			if (((HttpServletResponseWrapper) resp).getOutputSelection() == HttpServletResponseWrapper.OUTPUT_STREAM) {
				writer = resp.getWriter();
			} else {
				writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(resp.getOutputStream(), resp
						.getCharacterEncoding())), true);
			}
			final Response response = new Response();
			response.setException(e);
			try {
				ResponseWriter.write(response, writer);
			} catch (final JSONException e1) {
				LOG.error(e1.getMessage(), e1);
			}
		} catch (final Exception e) {
			final AbstractOXException wrapper = getWrappingOXException(e);
			LOG.error(wrapper.getMessage(), wrapper);
			final Writer writer;
			if (((HttpServletResponseWrapper) resp).getOutputSelection() == HttpServletResponseWrapper.OUTPUT_STREAM) {
				writer = resp.getWriter();
			} else {
				writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(resp.getOutputStream(), resp
						.getCharacterEncoding())), true);
			}
			final Response response = new Response();
			response.setException(wrapper);
			try {
				ResponseWriter.write(response, writer);
			} catch (final JSONException e1) {
				LOG.error(e1.getMessage(), e1);
			}
		}
	}

	/**
	 * Assigns incoming PUT request to corresponding method
	 */
	private final void actionPut(final HttpServletRequest req, final HttpServletResponse resp) throws IOException,
			AbstractOXException {
		final String actionStr = checkStringParam(req, PARAMETER_ACTION);
		if (actionStr.equalsIgnoreCase(ACTION_REFRESH_SERVER)) {
			actionPutClearFolderContent(req, resp);
		} else {
			throw getWrappingOXException(new Exception("Action \"" + actionStr
					+ "\" NOT supported via PUT on /ajax/sync"));
		}
	}

	private final void actionPutClearFolderContent(final HttpServletRequest req, final HttpServletResponse resp)
			throws IOException {
		try {
			actionPutClearFolderContent(getSessionObject(req), resp.getWriter(), getBody(req), ParamContainer
					.getInstance(req, EnumComponent.SYNCML, resp));
		} catch (final JSONException e) {
			writeErrorResponse((HttpServletResponseWrapper) resp, e);
		}
	}

	private final void actionPutClearFolderContent(final Session sessionObj, final Writer writer, final String body,
			final ParamContainer paramContainer) throws JSONException, IOException {
		/*
		 * Some variables
		 */
		final Response response = new Response();
		final UnsynchronizedStringWriter strWriter = new UnsynchronizedStringWriter();
		final JSONWriter jsonWriter = new JSONWriter(strWriter);
		Date lastModifiedDate = null;
		/*
		 * Start response
		 */
		jsonWriter.array();
		try {
			final Context ctx = ContextStorage.getStorageContext(sessionObj.getContextId());
			Date timestamp = null;
			final JSONArray jsonArr = new JSONArray(body);
			final int length = jsonArr.length();
			FolderSyncInterface folderSyncInterface = null;
			MailServletInterface mailInterface = null;
			try {
				long lastModified = 0;
				final OXFolderAccess access = new OXFolderAccess(ctx);
				NextId: for (int i = 0; i < length; i++) {
					final String deleteIdentifier = jsonArr.getString(i);
					int delFolderId = -1;
					if ((delFolderId = getUnsignedInteger(deleteIdentifier)) >= 0) {
						if (timestamp == null) {
							timestamp = paramContainer.checkDateParam(PARAMETER_TIMESTAMP);
						}
						if (folderSyncInterface == null) {
							folderSyncInterface = new RdbFolderSyncInterface(sessionObj, ctx, access);
						}
						FolderObject delFolderObj;
						try {
							delFolderObj = access.getFolderObject(delFolderId);
						} catch (final OXException exc) {
							/*
							 * Folder could not be found and therefore need not
							 * to be deleted
							 */
							if (LOG.isWarnEnabled()) {
								LOG.warn(exc.getMessage(), exc);
							}
							continue NextId;
						}
						if (delFolderObj.getLastModified().getTime() > timestamp.getTime()) {
							jsonWriter.value(delFolderObj.getObjectID());
							continue NextId;
						}
						folderSyncInterface.clearFolder(delFolderObj, timestamp);
						lastModified = Math.max(lastModified, delFolderObj.getLastModified().getTime());
					} else if (deleteIdentifier.startsWith(FolderObject.SHARED_PREFIX)) {
					    throw new OXFolderException(
                            OXFolderException.FolderCode.NO_ADMIN_ACCESS,
                            getUserName(sessionObj.getUserId(), ctx),
                            deleteIdentifier,
                            Integer.valueOf(ctx.getContextId()));
					} else {
						if (UserConfigurationStorage.getInstance()
								.getUserConfigurationSafe(sessionObj.getUserId(), ctx).hasWebMail()) {
							if (mailInterface == null) {
								mailInterface = MailServletInterface.getInstance(sessionObj);
							}
							mailInterface.clearFolder(deleteIdentifier);
						} else {
							jsonWriter.value(deleteIdentifier);
						}
					}
				}
				if (lastModified != 0) {
					lastModifiedDate = new Date(lastModified);
				}
			} finally {
				if (mailInterface != null) {
					mailInterface.close(true);
					mailInterface = null;
				}
			}
		} catch (final OXFolderException e) {
			LOG.error(e.getMessage(), e);
			if (!e.getCategory().equals(Category.USER_CONFIGURATION)) {
				response.setException(e);
			}
		} catch (final AbstractOXException e) {
			LOG.error(e.getMessage(), e);
			response.setException(e);
		} catch (final Exception e) {
			final AbstractOXException wrapper = getWrappingOXException(e);
			LOG.error(wrapper.getMessage(), wrapper);
			response.setException(wrapper);
		}
		/*
		 * Close response and flush print writer
		 */
		jsonWriter.endArray();
		response.setData(new JSONArray(strWriter.toString()));
		response.setTimestamp(lastModifiedDate);
		ResponseWriter.write(response, writer);
	}

	/*-
	 * ++++++++++++++++++++++ Helper methods +++++++++++++++++++++++
	 */

	private static final void writeErrorResponse(final HttpServletResponseWrapper resp, final Throwable e)
			throws IOException {
		final AbstractOXException wrapper = getWrappingOXException(e);
		LOG.error(wrapper.getMessage(), wrapper);
		writeErrorResponse(resp, wrapper);
	}

	private static final void writeErrorResponse(final HttpServletResponseWrapper resp, final AbstractOXException e)
			throws IOException {
		final Writer writer;
		if (resp.getOutputSelection() == HttpServletResponseWrapper.OUTPUT_STREAM) {
			writer = resp.getWriter();
		} else {
			writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(resp.getOutputStream(), resp
					.getCharacterEncoding())), true);
		}
		final Response response = new Response();
		response.setException(e);
		try {
			ResponseWriter.write(response, writer);
		} catch (final JSONException e1) {
			LOG.error(e1.getMessage(), e1);
		}
	}

	private static final AbstractOXException getWrappingOXException(final Throwable cause) {
		return new AbstractOXException(EnumComponent.SYNCML, Category.INTERNAL_ERROR, 9999, cause.getMessage(), cause);
	}

	private static final String checkStringParam(final HttpServletRequest req, final String paramName)
			throws OXException {
		final String paramVal = req.getParameter(paramName);
		if (paramVal == null) {
			throw new OXFolderException(FolderCode.MISSING_PARAMETER, paramName);
		}
		return paramVal;
	}

	/**
     * The radix for base <code>10</code>.
     */
    private static final int RADIX = 10;

    /**
     * Parses a positive <code>int</code> value from passed {@link String} instance.
     * 
     * @param s The string to parse
     * @return The parsed positive <code>int</code> value or <code>-1</code> if parsing failed
     */
    private static final int getUnsignedInteger(final String s) {
        if (s == null) {
            return -1;
        }

        final int max = s.length();

        if (max <= 0) {
            return -1;
        }
        if (s.charAt(0) == '-') {
            return -1;
        }

        int result = 0;
        int i = 0;

        final int limit = -Integer.MAX_VALUE;
        final int multmin = limit / RADIX;
        int digit;

        if (i < max) {
            digit = Character.digit(s.charAt(i++), RADIX);
            if (digit < 0) {
                return -1;
            }
            result = -digit;
        }
        while (i < max) {
            /*
             * Accumulating negatively avoids surprises near MAX_VALUE
             */
            digit = Character.digit(s.charAt(i++), RADIX);
            if (digit < 0) {
                return -1;
            }
            if (result < multmin) {
                return -1;
            }
            result *= RADIX;
            if (result < limit + digit) {
                return -1;
            }
            result -= digit;
        }
        return -result;
    }

}
