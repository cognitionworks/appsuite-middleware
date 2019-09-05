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

package com.openexchange.rest.services.session;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.crypto.CryptographicServiceAuthenticationFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.rest.services.annotation.Role;
import com.openexchange.rest.services.annotation.RoleAllowed;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.ObfuscatorService;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.sessiond.SessiondServiceExtended;
import com.openexchange.sessionstorage.SessionStorageService;
import com.openexchange.sessionstorage.StoredSession;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 *
 * The {@link SessionRESTService} allows clients to retrieve session information.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.8.1
 */
@Path("/preliminary/session/v1/")
@RoleAllowed(Role.BASIC_AUTHENTICATED)
public class SessionRESTService {

    private final ServiceLookup services;

    /**
     * Initializes a new {@link SessionRESTService}.
     */
    public SessionRESTService(ServiceLookup services) {
        super();
        this.services = services;
    }

    private volatile Integer timeout;

    /**
     * Gets the default timeout for session-storage operations.
     *
     * @return The default timeout in milliseconds
     */
    private int timeout() {
        Integer tmp = timeout;
        if (null == tmp) {
            synchronized (this) {
                tmp = timeout;
                if (null == tmp) {
                    int defaultTimeout = 3000;
                    ConfigurationService service = services.getOptionalService(ConfigurationService.class);
                    if (service == null) {
                        return defaultTimeout;
                    }
                    tmp = Integer.valueOf(service.getIntProperty("com.openexchange.sessiond.sessionstorage.timeout", defaultTimeout));
                    timeout = tmp;
                }
            }
        }
        return tmp.intValue();
    }

    /**
     * Checks if the given session is a guest session.
     *
     * @param session The session
     * @return <code>true</code> if the session is a guest session, <code>false</code> otherwise
     * @throws OXExceptionIf checking for a guest session fails
     */
    private boolean isGuest(Session session) throws OXException {
        if (session == null) {
            return false;
        }

        if (Boolean.TRUE.equals(session.getParameter(Session.PARAM_GUEST))) {
            return true;
        }

        ServerSession serverSession = ServerSessionAdapter.valueOf(session);
        if (serverSession != null) {
            User user = serverSession.getUser();
            return user != null && user.isGuest();
        }
        return false;
    }

    /**
     * <pre>
     * GET /preliminary/session/v1/get/{session}
     * </pre>
     */
    @GET
    @Path("/get/{session}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject all(@PathParam("session") String session) throws OXException {
        try {
            SessiondService sessiondService = services.getOptionalService(SessiondService.class);
            if (null == sessiondService) {
                throw ServiceExceptionCode.absentService(SessiondService.class);
            }

            Session ses;
            if (sessiondService instanceof SessiondServiceExtended) {
                ses = ((SessiondServiceExtended) sessiondService).getSession(session, false);
                if (ses == null) {
                    ses = optFromSessionStorage(session);
                }
            } else {
                ses = sessiondService.getSession(session);
            }

            if (ses == null) {
                // No such session...
                return new JSONObject(0);
            }

            // Basic user information
            JSONObject jResponse = new JSONObject(6).put("context", ses.getContextId()).put("user", ses.getUserId());

            // Add "guest" flag
            boolean isGuest = isGuest(ses);
            jResponse.put("guest", isGuest);

            // Add crypto session identifier
            CryptographicServiceAuthenticationFactory cryptoAuthenticationFactory = services.getOptionalService(CryptographicServiceAuthenticationFactory.class);
            if (cryptoAuthenticationFactory != null) {
                String cryptoSessionId = cryptoAuthenticationFactory.getSessionValueFrom(ses);
                jResponse.put("cryptoSessionId", cryptoSessionId);
            }

            return jResponse;
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private Session optFromSessionStorage(String sessionId) throws OXException {
        SessionStorageService sessionStorageService = services.getOptionalService(SessionStorageService.class);
        if (sessionStorageService == null) {
            return null;
        }

        Session session = sessionStorageService.lookupSession(sessionId, timeout());
        if (session instanceof StoredSession) {
            ObfuscatorService obfuscator = services.getOptionalService(ObfuscatorService.class);
            if (obfuscator != null) {
                StoredSession storedSession = (StoredSession) session;
                storedSession.setPassword(obfuscator.unobfuscate(storedSession.getPassword()));
            }
        }
        return session;
    }
}
