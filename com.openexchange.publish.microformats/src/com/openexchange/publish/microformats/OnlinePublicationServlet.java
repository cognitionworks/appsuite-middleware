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

package com.openexchange.publish.microformats;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.publish.Publication;
import com.openexchange.userconf.UserConfigurationService;

/**
 * {@link OnlinePublicationServlet}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class OnlinePublicationServlet extends HttpServlet {

    private static final Log LOG = LogFactory.getLog(OnlinePublicationServlet.class);
    
    private static final long serialVersionUID = 6966967169899449051L;

    protected static final String SECRET = "secret";

    protected static final String PROTECTED = "protected";

    /**
     * The pattern to split by <code>"/"</code> character.
     */
    protected static final Pattern SPLIT = Pattern.compile("/");

    protected static ContextService contexts = null;

    public static void setContextService(final ContextService service) {
        contexts = service;
    }

    protected static UserConfigurationService userConfigs = null;

    public static void setUserConfigurationService(UserConfigurationService userConfigurationService) {
        userConfigs = userConfigurationService;
    }

    protected boolean checkProtected(final Publication publication, final Map<String, String> args, final HttpServletResponse resp) throws IOException {
        final Map<String, Object> configuration = publication.getConfiguration();
        if (configuration.containsKey(PROTECTED) && ((Boolean) configuration.get("protected")).booleanValue()) {
            final String secret = (String) configuration.get(SECRET);
            if (!secret.equals(args.get(SECRET))) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().println("Cannot find the publication site.");
                return false;
            }
        }
        return true;
    }

    protected boolean checkPublicationPermission(final Publication publication, final HttpServletResponse resp) throws IOException {

        Context ctx = publication.getContext();
        int userId = publication.getUserId();

        try {
            UserConfiguration userConfiguration = userConfigs.getUserConfiguration(userId, ctx);
            if (userConfiguration.isPublication()) {
                return true;
            }
        } catch (OXException e) {
            LOG.error(e.getMessage(), e);
        }
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        resp.getWriter().println("Cannot find the publication site.");

        return false;
    }

    // FIXME: Get Default Encoding from config service
    protected Collection<String> decode(final List<String> subList, final HttpServletRequest req) throws UnsupportedEncodingException {
        final String encoding = req.getCharacterEncoding() == null ? "UTF-8" : req.getCharacterEncoding();
        final List<String> decoded = new ArrayList<String>();
        for (final String component : subList) {
            final String decodedComponent = decode(component, encoding);
            decoded.add(decodedComponent);
        }
        return decoded;
    }

    // FIXME use server service for this
    private String decode(final String string, final String encoding) throws UnsupportedEncodingException {
        final String[] chunks = string.split("\\+");
        final StringBuilder decoded = new StringBuilder(string.length());
        final boolean endsWithPlus = string.endsWith("+");
        for (int i = 0; i < chunks.length; i++) {
            final String chunk = chunks[i];
            decoded.append(URLDecoder.decode(chunk, encoding));
            if (i != chunks.length - 1 || endsWithPlus) {
                decoded.append('+');
            }
        }

        return decoded.toString();
    }
}
