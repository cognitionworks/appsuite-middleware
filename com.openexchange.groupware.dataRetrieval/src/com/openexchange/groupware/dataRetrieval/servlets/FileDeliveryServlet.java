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

package com.openexchange.groupware.dataRetrieval.servlets;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.groupware.dataRetrieval.Constants;
import com.openexchange.groupware.dataRetrieval.DataProvider;
import com.openexchange.groupware.dataRetrieval.FileMetadata;
import com.openexchange.groupware.dataRetrieval.config.Configuration;
import com.openexchange.groupware.dataRetrieval.registry.DataProviderRegistry;
import com.openexchange.java.Streams;
import com.openexchange.session.RandomTokenContainer;
import com.openexchange.tools.io.IOTools;
import com.openexchange.tools.servlet.CountingHttpServletRequest;
import com.openexchange.tools.servlet.ratelimit.RateLimitedException;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link FileDeliveryServlet}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class FileDeliveryServlet extends HttpServlet {

    private static final long serialVersionUID = -1246179982601539367L;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(FileDeliveryServlet.class);

    private transient final RandomTokenContainer<Map<String, Object>> paramMap;
    private transient final DataProviderRegistry dataProviders;

    private final Configuration configuration;

    /**
     * Initializes a new {@link FileDeliveryServlet}.
     * 
     * @param paramMap {@link RandomTokenContainer}
     * @param dataProviders {@link DataProviderRegistry}
     * @param configuration The {@link Configuration}
     */
    public FileDeliveryServlet(RandomTokenContainer<Map<String, Object>> paramMap, DataProviderRegistry dataProviders, Configuration configuration) {
        super();
        this.paramMap = paramMap;
        this.dataProviders = dataProviders;
        this.configuration = configuration;
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            // create a new HttpSession if it's missing
            req.getSession(true);
            super.service(new CountingHttpServletRequest(req), resp);
        } catch (RateLimitedException e) {
            e.send(resp);
        }
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        if (configuration.isEnabled() == false) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Data retrieval is disabled.");
            return;
        }
        final String token = req.getParameter("token");
        if (token == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter 'token");
            return;
        }
        final Map<String, Object> parameters = paramMap.get(token);
        if (parameters == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        final String id = (String) parameters.get(Constants.DATA_PROVDER_KEY);
        final ServerSession session = (ServerSession) parameters.get(Constants.SESSION_KEY);

        if (configuration.hasExpired(((Long) parameters.get(Constants.CREATED)).longValue())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        DataProvider<Object> provider = null;
        Object state = null;
        try {
            provider = dataProviders.getProvider(id);

            state = provider.start();

            final FileMetadata metadata = provider.retrieveMetadata(state, parameters, session);

            InputStream stream = provider.retrieve(state, parameters, session);
            stream = setHeaders(stream, metadata, resp);

            IOTools.copy(stream, resp.getOutputStream());
            if (configuration.expiresAfterAccess()) {
                paramMap.remove(token);
            }
        } catch (final Exception e) {
            LOG.error("", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (state != null && provider != null) {
                provider.close(state);
            }
        }
    }

    private InputStream setHeaders(final InputStream stream, final FileMetadata metadata, final HttpServletResponse resp) throws IOException {
        final InputStream in = new BufferedInputStream(stream, 65536); // FIXME: How come backends don't supply correct size? This has memory implications that are not so nice.
        final ByteArrayOutputStream out = new ByteArrayOutputStream((int) metadata.getSize());
        int count = 0;
        int value = 0;
        try {
            while ((value = in.read()) != -1) {
                out.write(value);
                count++;
            }
        } finally {
            Streams.close(in, out);
        }

        resp.setContentLength(count);

        if (metadata.getType() != null) {
            resp.setContentType(metadata.getType());
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

}
