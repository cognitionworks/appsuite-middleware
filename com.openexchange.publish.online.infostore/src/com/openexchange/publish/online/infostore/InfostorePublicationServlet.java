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

package com.openexchange.publish.online.infostore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.composition.IDBasedFileAccess;
import com.openexchange.file.storage.composition.IDBasedFileAccessFactory;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.java.Streams;
import com.openexchange.publish.Publication;
import com.openexchange.publish.PublicationDataLoaderService;
import com.openexchange.publish.online.infostore.util.InfostorePublicationUtils;
import com.openexchange.publish.tools.PublicationSession;
import com.openexchange.tools.encoding.Helper;
import com.openexchange.tools.servlet.CountingHttpServletRequest;
import com.openexchange.tools.servlet.ratelimit.RateLimitedException;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;


/**
 * {@link InfostorePublicationServlet}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 *
 */
public class InfostorePublicationServlet extends HttpServlet {

    private static final long serialVersionUID = 8929899129435791832L;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(InfostorePublicationServlet.class);

    private final String SELF_DESTRUCT = "selfDestruct";

    private final String DESTROY_DOCUMENT = "destroyDocument";

    private volatile PublicationDataLoaderService dataLoader;

    private volatile InfostoreDocumentPublicationService publisher;

    private volatile ContextService context;

    private volatile IDBasedFileAccessFactory fileAccessFactory;

    public InfostorePublicationServlet(ContextService context, PublicationDataLoaderService dataLoader, IDBasedFileAccessFactory fileAccessFactory, InfostoreDocumentPublicationService publicationService) {
        super();

        this.context = context;
        this.dataLoader = dataLoader;
        this.fileAccessFactory = fileAccessFactory;
        this.publisher = publicationService;
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            // create a new HttpSession if it's missing
            req.getSession(true);
            super.service(new CountingHttpServletRequest(req), resp);
        } catch (final RateLimitedException e) {
            resp.setContentType("text/plain; charset=UTF-8");
            if(e.getRetryAfter() > 0) {
                resp.setHeader("Retry-After", String.valueOf(e.getRetryAfter()));
            }
            resp.sendError(429, "Too Many Requests - Your request is being rate limited.");
        }
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        handle(req, resp);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        handle(req, resp);
    }

    @Override
    protected void doPut(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        handle(req, resp);
    }

    private final Pattern SPLIT = Pattern.compile("/");

    private void handle(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            final String[] path = SPLIT.split(req.getRequestURI(), 0);
            final Context ctx = getContext(path);
            final String secret = getSecret(path);
            final Publication publication = getPublication(secret, ctx);
            if (publication == null || !publication.isEnabled()) {
                resp.getWriter().println("Not Found");
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            final DocumentMetadata document = InfostorePublicationUtils.loadDocumentMetadata(publication, this.fileAccessFactory);
            final InputStream is = loadContent(publication);
            configureHeaders(document, req, resp);
            write(is, resp);
            if (mustSelfDestruct(publication)) {
                destroy(publication);
                if (mustDestroyDocument(publication) && !hasMorePublications(publication.getContext(), document)) {
                    final ServerSession session = ServerSessionAdapter.valueOf(new PublicationSession(publication), publication.getContext());
                    destroy(session, document);
                }
            }

        } catch (final Exception x) {
            LOG.error("", x);
            try {
                resp.getWriter().print(x.toString());
            } catch (Exception e) {
                LOG.error("", e);
            }
        }
    }

    private void destroy(final ServerSession session, final DocumentMetadata document) throws OXException {
        IDBasedFileAccess fileAccess = fileAccessFactory.createAccess(session);
        fileAccess.removeDocument(String.valueOf(document.getId()), Long.MAX_VALUE);
    }

    private boolean hasMorePublications(final Context ctx, final DocumentMetadata document) throws OXException {
        return !publisher.getAllPublications(ctx, String.valueOf(document.getId())).isEmpty();
    }

    private boolean mustDestroyDocument(final Publication publication) {
        return publication.getConfiguration().get(DESTROY_DOCUMENT) == Boolean.TRUE;
    }

    private void destroy(final Publication publication) throws OXException {
        publisher.delete(publication);
    }

    private boolean mustSelfDestruct(final Publication publication) {
        return publication.getConfiguration().get(SELF_DESTRUCT) == Boolean.TRUE;
    }

    private final boolean isIE(final HttpServletRequest req) {
        final String userAgent = req.getHeader("User-Agent");
        return null != userAgent && userAgent.contains("MSIE");
    }


    private void configureHeaders(final DocumentMetadata document, final HttpServletRequest req, final HttpServletResponse resp) throws UnsupportedEncodingException {
        final String fileName = document.getFileName();
        if(fileName != null) {
            resp.setHeader("Content-Disposition", "attachment; filename=\""
                + Helper.encodeFilename(fileName, "UTF-8", isIE(req)) + "\"");
        }
    }

    private void write(final InputStream is, final HttpServletResponse resp) throws IOException {
        try {
            final ServletOutputStream out = resp.getOutputStream();
            final int buflen = 65536;
            final byte[] buf = new byte[buflen];
            for (int read; (read = is.read(buf, 0, buflen)) > 0;) {
                out.write(buf, 0, read);
            }
            out.flush();
        } finally {
            Streams.close(is);
        }
    }

    private InputStream loadContent(final Publication publication) throws OXException {
        Collection<? extends Object> load = dataLoader.load(publication, null);
        if(load == null || load.isEmpty()) {
            return new ByteArrayInputStream(new byte[0]);
        }
        return (InputStream) load.iterator().next();
    }

    private Publication getPublication(final String secret, final Context ctx) throws OXException {
        return publisher.getPublication(ctx, secret);
    }

    private String getSecret(final String[] path) {
        return path[path.length-1];
    }

    private Context getContext(final String[] path) throws OXException {
        int cid = -1;
        for(int i = 0; i < path.length; i++) {
            if(path[i].equals("documents") && path.length > i+1) {
                try {
                    cid = Integer.parseInt(path[i+1]);
                    break;
                } catch (final NumberFormatException x) {
                    //
                }
            }
        }
        if(cid == -1) {
            throw new IllegalArgumentException("URL did not contain context id");
        }
        return context.getContext(cid);
    }

}

