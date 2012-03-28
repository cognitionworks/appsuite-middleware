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

package com.openexchange.ajax.requesthandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.notify.hostname.HostnameService;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.groupware.upload.impl.UploadEvent;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.servlet.http.Tools;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link AJAXRequestDataTools} - Tools for parsing AJAX requests.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class AJAXRequestDataTools {

    private static final AJAXRequestDataTools INSTANCE = new AJAXRequestDataTools();

    /**
     * Gets the default instance
     * 
     * @return The default instance
     */
    public static AJAXRequestDataTools getInstance() {
        return INSTANCE;
    }

    /*-
     * ----------------------- Member stuff -----------------------
     */
    
    /**
     * Initializes a new {@link AJAXRequestDataTools}.
     */
    protected AJAXRequestDataTools() {
        super();
    }

    /**
     * Parses an appropriate {@link AJAXRequestData} instance from specified arguments.
     * 
     * @param req The HTTP Servlet request
     * @param preferStream Whether to prefer request's stream instead of parsing its body data to an appropriate (JSON) object
     * @param isFileUpload Whether passed request is considered as a file upload
     * @param session The associated session
     * @param prefix The part of request's URI considered as prefix; &lt;prefix&gt; + <code>'/'</code> + &lt;module&gt;
     * @return An appropriate {@link AJAXRequestData} instance
     * @throws IOException If an I/O error occurs
     * @throws OXException If an OX error occurs
     */
    public AJAXRequestData parseRequest(final HttpServletRequest req, final boolean preferStream, final boolean isFileUpload, final ServerSession session, final String prefix) throws IOException, OXException {
        final AJAXRequestData retval = new AJAXRequestData();
        parseHostName(retval, req, session);
        /*
         * Set the module
         */
        retval.setModule(getModule(prefix, req));
        
        /*
         * Set request URI
         */
        retval.setServletRequestURI(AJAXServlet.getServletSpecificURI(req));
        retval.setAction(getAction(req));
        /*
         * Set the format
         */
        retval.setFormat(req.getParameter("format"));
        /*
         * Pass all parameters to AJAX request object
         */
        {
            @SuppressWarnings("unchecked") final Set<Entry<String, String[]>> entrySet = req.getParameterMap().entrySet();
            for (final Entry<String, String[]> entry : entrySet) {
                retval.putParameter(entry.getKey(), entry.getValue()[0]);
            }
        }
        /*
         * Check for ETag header to support client caching
         */
        {
            final String eTag = req.getHeader("If-None-Match");
            if (null != eTag) {
                retval.setETag(eTag);
            }
        }
        /*
         * Set request body
         */
        if (isFileUpload) {
            final UploadEvent upload = AJAXServlet.processUploadStatic(req);
            final Iterator<UploadFile> iterator = upload.getUploadFilesIterator();
            while (iterator.hasNext()) {
                retval.addFile(iterator.next());
            }
            final Iterator<String> names = upload.getFormFieldNames();
            while (names.hasNext()) {
                final String name = names.next();
                retval.putParameter(name, upload.getFormField(name));
            }
            retval.setUploadEvent(upload);
        } else if (preferStream || parseBoolParameter("binary", req)) {
            /*
             * Pass request's stream
             */
            retval.setUploadStreamProvider(new HTTPRequestInputStreamProvider(req));
        } else {
            /*
             * Guess an appropriate body object
             */
            final String body = AJAXServlet.getBody(req);
            if (startsWith('{', body)) {
                /*
                 * Expect the body to be a JSON object
                 */
                try {
                    retval.setData(new JSONObject(body));
                } catch (final JSONException e) {
                    retval.setData(body);
                }
            } else if (startsWith('[', body)) {
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

    private static final Set<String> BOOL_VALS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("true", "1", "yes", "on")));

    private static boolean parseBoolParameter(final String name, final HttpServletRequest req) {
        final String parameter = req.getParameter(name);
        if (null == parameter) {
            return false;
        }
        return BOOL_VALS.contains(parameter.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Parses host name, secure and AJP route.
     * 
     * @param request The AJAX request data
     * @param req The HTTP Servlet request
     * @param session The associated session
     */
    public static void parseHostName(final AJAXRequestData request, final HttpServletRequest req, final ServerSession session) {
        request.setSecure(Tools.considerSecure(req));
        {
            final HostnameService hostnameService = ServerServiceRegistry.getInstance().getService(HostnameService.class);
            if (null == hostnameService) {
                request.setHostname(req.getServerName());
            } else {
                final String hn = hostnameService.getHostname(session.getUserId(), session.getContextId());
                request.setHostname(null == hn ? req.getServerName() : hn);
            }
        }
        request.setRemoteAddress(req.getRemoteAddr());
        request.setRoute(Tools.getRoute(req.getSession(true).getId()));
    }

    private static boolean startsWith(final char startingChar, final String toCheck) {
        if (null == toCheck) {
            return false;
        }
        final int len = toCheck.length();
        if (len <= 0) {
            return false;
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

	/**
	 * Gets the module from specified HTTP request.
	 * 
	 * @param prefix The dispatcher's default prefix to strip from request's {@link HttpServletRequest#getPathInfo() path info}.
	 * @param req The HTTP request
	 * @return The determined module
	 */
	public String getModule(final String prefix, final HttpServletRequest req) {
		 String pathInfo = req.getRequestURI();
         final int lastIndex = pathInfo.lastIndexOf(';');
         if (lastIndex > 0) {
        	 pathInfo = pathInfo.substring(0, lastIndex);
         }
         return pathInfo.substring(prefix.length());
	}

	/**
	 * Gets the action from specified HTTP request.
	 * 
	 * @param req The HTTP request
	 * @return The determined action
	 */
	public String getAction(final HttpServletRequest req) {
		final String action = req.getParameter("action");
		if (null == action) {
			return req.getMethod().toUpperCase(Locale.US);
		}
        return action;
        
	}
}
