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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXActionServiceFactory;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.writer.ResponseWriter;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.notify.hostname.HostnameService;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.groupware.upload.impl.UploadEvent;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.systemname.SystemNameService;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;
import com.openexchange.tools.servlet.http.Tools;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link MultipleAdapterServletNew} is a rewrite of the really good {@link MultipleAdapterServlet} with smarter handling of the request
 * parameters.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public abstract class MultipleAdapterServletNew extends PermissionServlet {

    private static final class HTTPRequestInputStreamProvider implements AJAXRequestData.InputStreamProvider {

        private final HttpServletRequest req;

        HTTPRequestInputStreamProvider(final HttpServletRequest req) {
            this.req = req;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return req.getInputStream();
        }
    }

    private static final long serialVersionUID = -8060034833311074781L;

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(MultipleAdapterServletNew.class));

    private final AJAXActionServiceFactory factory;

    /**
     * Initializes a new {@link MultipleAdapterServletNew}.
     *
     * @param factory The factory to map incoming request to an appropriate {@link AJAXActionService}
     * @throws NullPointerException If factory is <code>null</code>
     */
    protected MultipleAdapterServletNew(final AJAXActionServiceFactory factory) {
        super();
        if (null == factory) {
            throw new NullPointerException("Factory is null.");
        }
        this.factory = factory;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp, false);
    }

    @Override
    protected void doPut(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp, false);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp, true);
    }

    /**
     * Handles given HTTP request and generates an appropriate result using referred {@link AJAXActionService}.
     *
     * @param req The HTTP request to handle
     * @param resp The HTTP response to write to
     * @param preferStream <code>true</code> to prefer passing request's body as binary data using an {@link InputStream} (typically for
     *            HTTP POST method); otherwise <code>false</code> to generate an appropriate {@link Object} from request's body
     * @throws IOException If an I/O error occurs
     */
    protected final void handle(final HttpServletRequest req, final HttpServletResponse resp, final boolean preferStream) throws IOException, ServletException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(AJAXServlet.CONTENTTYPE_JAVASCRIPT);
        Tools.disableCaching(resp);

        final String action = req.getParameter(PARAMETER_ACTION);
        final boolean isFileUpload = FileUploadBase.isMultipartContent(new ServletRequestContext(req));

        final ServerSession session = getSessionObject(req);

        final Response response = new Response(session);
        try {
            if (action == null) {
                throw AjaxExceptionCodes.MISSING_PARAMETER.create( PARAMETER_ACTION);
            }
            if (handleIndividually(action, req, resp)) {
                return;
            }

            final AJAXRequestData data = parseRequest(req, preferStream, isFileUpload, session);
            if (handleIndividually(action, data, req, resp)) {
                return;
            }

            final AJAXRequestResult result = factory.createActionService(action).perform(data, session);
            response.setData(result.getResultObject());
            response.setTimestamp(result.getTimestamp());
            final Collection<OXException> warnings = result.getWarnings();
            if (null != warnings && !warnings.isEmpty()) {
                response.addWarnings(warnings);
            }
        } catch (final OXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        }  catch (final IllegalStateException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof OXException) {
                final OXException oxe = (OXException) cause;
                LOG.error(oxe.getMessage(), oxe);
                response.setException(oxe);
            } else {
                LOG.error(e.getMessage(), e);
                response.setException(AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage()));
            }
        }
        try {
            if (isFileUpload || (req.getParameter("respondWithHTML") != null && req.getParameter("respondWithHTML").equalsIgnoreCase("true")) ) {
                resp.setContentType(AJAXServlet.CONTENTTYPE_HTML);
                String callback = req.getParameter("callback");
                if(callback == null) {
                    callback = action;
                }
                final StringWriter w = new StringWriter();
                ResponseWriter.write(response, w);

                resp.getWriter().print(substituteJS(w.toString(), callback));
            } else {
                ResponseWriter.write(response, resp.getWriter());
            }
        } catch (final JSONException e) {
            final OXException e1 = OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
            LOG.error(e1.getMessage(), e1);
            sendError(resp);
        }
    }

    /**
     * Override this to handle an action differently from the usual JSON handling. This is primarily useful for handling up- / downloads.
     *
     * @param action The action parameter given
     * @param req The HTTP request object
     * @param resp The HTTP response object
     * @return <code>true</code> if operation completed successfully and therefore usual JSON handling must be omitted; otherwise <code>false</code> to fall-back to usual JSON handling
     * @throws OXException
     */
    protected boolean handleIndividually(final String action, final HttpServletRequest req, final HttpServletResponse resp) throws IOException, ServletException, OXException {
        return false;
    }

    /**
     * Override this to handle an action differently from the usual JSON handling. This is primarily useful for handling up- / downloads.
     *
     * @param action The action parameter given
     * @param data The parsed request
     * @param req The HTTP request object
     * @param resp The HTTP response object
     * @return <code>true</code> if operation completed successfully and therefore usual JSON handling must be omitted; otherwise <code>false</code> to fall-back to usual JSON handling
     * @throws IOException
     * @throws ServletException
     * @throws OXException
     */
    protected boolean handleIndividually(final String action, final AJAXRequestData data, final HttpServletRequest req, final HttpServletResponse resp) throws IOException, ServletException, OXException {
        return false;
    }

    protected AJAXRequestData parseRequest(final HttpServletRequest req, final boolean preferStream, final boolean isFileUpload, final ServerSession session) throws IOException, OXException {
        final AJAXRequestData retval = new AJAXRequestData();
        retval.setSecure(Tools.considerSecure(req));
        {
            final HostnameService hostnameService = ServerServiceRegistry.getInstance().getService(HostnameService.class);
            if (null == hostnameService) {
                retval.setHostname(req.getServerName());
            } else {
                final String hn = hostnameService.getHostname(session.getUserId(), session.getContextId());
                retval.setHostname(null == hn ? req.getServerName() : hn);
            }
        }
        retval.setRoute(ServerServiceRegistry.getInstance().getService(SystemNameService.class).getSystemName()); // Maybe use system name service
        /*
         * Pass all parameters to AJAX request object
         */
        {
            @SuppressWarnings("unchecked") final Set<Entry<String, String[]>> entrySet = req.getParameterMap().entrySet();
            for (final Entry<String, String[]> entry : entrySet) {
                retval.putParameter(entry.getKey(), entry.getValue()[0]);
            }
        }
        if (isFileUpload) {
            final UploadEvent upload = processUpload(req);
            final Iterator<UploadFile> iterator = upload.getUploadFilesIterator();
            while(iterator.hasNext()) {
                retval.addFile(iterator.next());
            }
            final Iterator<String> names = upload.getFormFieldNames();
            while(names.hasNext()) {
                final String name = names.next();
                retval.putParameter(name, upload.getFormField(name));
            }
            retval.setUploadEvent(upload);
        } else if (preferStream) {
            /*
             * Pass request's stream
             */
            retval.setUploadStreamProvider(new HTTPRequestInputStreamProvider(req));
        } else {
            /*
             * Guess an appropriate body object
             */
            final String body = AJAXServlet.getBody(req);
            if (startsWith('{', body, true)) {
                /*
                 * Expect the body to be a JSON object
                 */
                try {
                    retval.setData(new JSONObject(body));
                } catch (final JSONException e) {
                    retval.setData(body);
                }
            } else if (startsWith('[', body, true)) {
                /*
                 * Expect the body to be a JSON array
                 */
                try {
                    retval.setData(new JSONArray(body));
                } catch (final JSONException e) {
                    retval.setData(body);
                }
            } else {
                retval.setData(0 == body.length() ? null : body);
            }
        }
        return retval;
    }

    private static boolean startsWith(final char startingChar, final String toCheck, final boolean ignoreHeadingWhitespaces) {
        if (null == toCheck) {
            return false;
        }
        final int len = toCheck.length();
        if (len <= 0) {
            return false;
        }
        if (!ignoreHeadingWhitespaces) {
            return startingChar == toCheck.charAt(0);
        }
        int i = 0;
        if (Character.isWhitespace(toCheck.charAt(i))) {
            do {
                i++;
            } while (i < len && Character.isWhitespace(toCheck.charAt(i)));
        }
        if (i >= len) {
            return false;
        }
        return startingChar == toCheck.charAt(i);
    }

}
