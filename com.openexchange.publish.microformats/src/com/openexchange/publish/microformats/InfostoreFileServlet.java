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

package com.openexchange.publish.microformats;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import com.openexchange.ajax.container.FileHolder;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestDataTools;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.responseRenderers.FileResponseRenderer;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.infostore.InfostoreExceptionCodes;
import com.openexchange.groupware.infostore.InfostoreFacade;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.html.HtmlService;
import com.openexchange.java.Strings;
import com.openexchange.log.LogFactory;
import com.openexchange.publish.Publication;
import com.openexchange.publish.PublicationErrorMessage;
import com.openexchange.publish.tools.PublicationSession;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.user.UserService;
import com.openexchange.userconf.UserPermissionService;


/**
 * {@link InfostoreFileServlet}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 *
 */
public class InfostoreFileServlet extends OnlinePublicationServlet {

    private static final long serialVersionUID = -7725853828283398968L;

    private static final String CONTEXTID = "contextId";
    private static final String SITE = "site";
    private static final String INFOSTORE_ID = "infoId";
    private static final String INFOSTORE_VERSION = "infoVersion";

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(InfostoreFileServlet.class));

    private static OXMFPublicationService infostorePublisher = null;


    public static void setInfostorePublisher(final OXMFPublicationService service) {
        infostorePublisher = service;
    }

    private static volatile InfostoreFacade infostore;

    public static void setInfostore(final InfostoreFacade service) {
        infostore = service;
    }

    private static volatile UserService users;

    public static void setUsers(final UserService service) {
        users = service;
    }

    private static volatile UserPermissionService userPermissions;

    public static void setUserPermissions(final UserPermissionService service) {
        userPermissions = service;
    }

    private static volatile FileResponseRenderer fileResponseRenderer;

    public static void setFileResponseRenderer(final FileResponseRenderer renderer) {
    	fileResponseRenderer = renderer;
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        final Map<String, String> args = getPublicationArguments(req);
        boolean startedWriting = false;
        try {
            final Context ctx = contexts.getContext(Integer.parseInt(args.get(CONTEXTID)));
            final Publication publication = infostorePublisher.getPublication(ctx, args.get(SITE));
            if (publication == null || !publication.isEnabled()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                final PrintWriter writer = resp.getWriter();
                final HtmlService htmlService = MicroformatServlet.htmlService;
                writer.println("Unknown site " + (null == htmlService ? "" : htmlService.htmlFormat(args.get(SITE))));
                writer.flush();
                return;
            }
            if (!checkProtected(publication, args, resp)) {
                return;
            }

            if (!checkPublicationPermission(publication, resp)) {
                return;
            }

            final int infoId = Integer.parseInt(args.get(INFOSTORE_ID));

            final User user = getUser(publication);
            final UserPermissionBits userPerms = getUserPermissionBits(publication);

            final DocumentMetadata metadata = loadMetadata(publication, infoId, user, userPerms);

            final InputStream fileData = loadFile(publication, infoId, user, userPerms);

            startedWriting = true;
            writeFile(new PublicationSession(publication), metadata, fileData, req, resp);

        } catch (final Throwable t) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if(!startedWriting) {
                t.printStackTrace(resp.getWriter());
            }
            LOG.error(t.getMessage(), t);
        }

    }

    private User getUser(final Publication publication) throws OXException {
        return users.getUser(publication.getUserId(), publication.getContext());
    }

    private UserPermissionBits getUserPermissionBits(final Publication publication) throws OXException {
        return userPermissions.getUserPermissionBits(publication.getUserId(), publication.getContext());
    }


    private DocumentMetadata loadMetadata(final Publication publication, final int infoId, final User user, final UserPermissionBits userPermissions) throws OXException {
        try {
            return infostore.getDocumentMetadata(infoId, InfostoreFacade.CURRENT_VERSION, publication.getContext(), user, userPermissions);
        } catch (final OXException e) {
            if (InfostoreExceptionCodes.NOT_EXIST.equals(e)) {
                throw PublicationErrorMessage.NotExist.create(e, new Object[0]);
            }
            throw e;
        }
    }

    private void writeFile(final Session session, final DocumentMetadata metadata, final InputStream fileData, final HttpServletRequest req, final HttpServletResponse resp) throws IOException, OXException {
    	final AJAXRequestData request = AJAXRequestDataTools.getInstance().parseRequest(req, false, false, ServerSessionAdapter.valueOf(session), "/publications/infostore", resp);
    	final AJAXRequestResult result = new AJAXRequestResult(new FileHolder(fileData, metadata.getFileSize(), metadata.getFileMIMEType(), metadata.getFileName()), "file");

    	fileResponseRenderer.write(request, result, req, resp);
    }

    private static final boolean isIE(final HttpServletRequest req) {
        final String userAgent = req.getHeader("User-Agent");
        return null != userAgent && userAgent.contains("MSIE");
    }

    private InputStream loadFile(final Publication publication, final int infoId, final User user, final UserPermissionBits userPermissions) throws OXException {
        return infostore.getDocument(infoId, InfostoreFacade.CURRENT_VERSION, publication.getContext(), user, userPermissions);
    }

    private Map<String, String> getPublicationArguments(final HttpServletRequest req) throws UnsupportedEncodingException {
        // URL format is: /publications/files/[cid]/[siteName]/[infostoreID]/[version]?secret=[secret]

        final String[] path = SPLIT.split(req.getPathInfo(), 0);
        final List<String> normalized = new ArrayList<String>(path.length);
        for (int i = 0; i < path.length; i++) {
            if (!path[i].equals("")) {
                normalized.add(path[i]);
            }
        }

        final String site = Strings.join(decode(normalized.subList(1, normalized.size()-2), req), "/");
        final Map<String, String> args = new HashMap<String, String>();
        args.put(CONTEXTID, normalized.get(0));
        args.put(SITE, site);
        args.put(SECRET, req.getParameter(SECRET));
        args.put(INFOSTORE_ID, normalized.get(normalized.size()-2));
        return args;
    }

}
