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
package com.openexchange.oidc.osgi;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hazelcast.core.HazelcastInstance;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.LoginServlet;
import com.openexchange.ajax.login.LoginConfiguration;
import com.openexchange.ajax.login.LoginRequestHandler;
import com.openexchange.dispatcher.DispatcherPrefixService;
import com.openexchange.exception.OXException;
import com.openexchange.java.ConcurrentList;
import com.openexchange.java.Strings;
import com.openexchange.oidc.OIDCConfig;
import com.openexchange.oidc.OIDCExceptionCode;
import com.openexchange.oidc.OIDCWebSSOProvider;
import com.openexchange.oidc.http.AuthenticationService;
import com.openexchange.oidc.http.InitService;
import com.openexchange.oidc.http.LogoutService;
import com.openexchange.oidc.impl.OIDCLoginRequestHandler;
import com.openexchange.oidc.impl.OIDCLogoutRequestHandler;
import com.openexchange.oidc.impl.OIDCWebSSOProviderImpl;
import com.openexchange.oidc.spi.OIDCBackend;
import com.openexchange.oidc.spi.OIDCExceptionHandler;
import com.openexchange.oidc.state.CoreStateManagement;
import com.openexchange.oidc.tools.OIDCTools;
import com.openexchange.server.ServiceLookup;

/**
 * Registers and stores all OpenID backends and their servlets to handle future requests.
 *
 * @author <a href="mailto:vitali.sjablow@open-xchange.com">Vitali Sjablow</a>
 * @since v7.10.0
 */
public class OIDCBackendRegistry extends ServiceTracker<OIDCBackend, OIDCBackend>{

    private static final Logger LOG = LoggerFactory.getLogger(OIDCBackendRegistry.class);
    private ServiceLookup services;
    private final ConcurrentList<OIDCBackend> backends;
    private ConcurrentHashMap<OIDCBackend, Stack<String>> backendServlets;
    private ConcurrentHashMap<OIDCBackend, Stack<ServiceRegistration<?>>> backendServiceRegistrations;
    private LoginConfiguration loginConfiguration;

    public OIDCBackendRegistry(BundleContext context, ServiceLookup services) {
        super(context, OIDCBackend.class, null);
        this.services = services;
        this.backends = new ConcurrentList<>();
        this.backendServlets = new ConcurrentHashMap<>();
        this.backendServiceRegistrations = new ConcurrentHashMap<>();
        this.loginConfiguration = LoginServlet.getLoginConfiguration();
    }

    @Override
    public OIDCBackend addingService(ServiceReference<OIDCBackend> reference) {
        final OIDCBackend oidcBackend = this.context.getService(reference);

        final Stack<String> servlets = new Stack<>();
        HttpService httpService = this.services.getService(HttpService.class);
        final Stack<ServiceRegistration<?>> serviceRegistrations = new Stack<ServiceRegistration<?>>();

        if (backends.addIfAbsent(oidcBackend)) {
            LOG.info("Adding OIDCBackend: {}", oidcBackend.getPath());
            try {
                OIDCConfig config = oidcBackend.getOIDCConfig();
                String path = oidcBackend.getPath();
                if (config == null) {
                    throw OIDCExceptionCode.MISSING_BACKEND_CONFIGURATION.create(path.isEmpty() ? "No path available" : path);
                }
                if (!Strings.isEmpty(path)) {
                    validatePath(path);
                }
                oidcBackend.setLoginConfiguration(this.loginConfiguration);
                OIDCWebSSOProvider ssoProvider = new OIDCWebSSOProviderImpl(oidcBackend, new CoreStateManagement(this.services.getService(HazelcastInstance.class)), this.services, this.loginConfiguration);
                OIDCExceptionHandler exceptionHandler = oidcBackend.getExceptionHandler();

                this.registerServlet(servlets, httpService, this.getPrefix(oidcBackend), new InitService(ssoProvider, exceptionHandler), "init");
                this.registerServlet(servlets, httpService, this.getPrefix(oidcBackend), new AuthenticationService(ssoProvider, exceptionHandler), "auth");
                this.registerServlet(servlets, httpService, this.getPrefix(oidcBackend), new LogoutService(ssoProvider, exceptionHandler), "logout");
                this.registerRequestHandler(oidcBackend, serviceRegistrations, OIDCTools.OIDC_LOGIN, new OIDCLoginRequestHandler(this.loginConfiguration, oidcBackend, this.services));
                this.registerRequestHandler(oidcBackend, serviceRegistrations, OIDCTools.OIDC_LOGOUT, new OIDCLogoutRequestHandler(this.loginConfiguration, oidcBackend));

                return oidcBackend;
            } catch (OXException | ServletException | NamespaceException e) {
                LOG.error(e.getLocalizedMessage(), e);
                while (!servlets.isEmpty()) {
                    httpService.unregister(servlets.pop());
                }
                while (!serviceRegistrations.isEmpty()) {
                    ServiceRegistration<?> pop = serviceRegistrations.pop();
                    if (null != pop) {
                        pop.unregister();
                    }
                }
                this.backends.remove(oidcBackend);
                this.context.ungetService(reference);
            } finally {
                if (!servlets.isEmpty()) {
                    this.backendServlets.putIfAbsent(oidcBackend, servlets);
                }
                if (!serviceRegistrations.isEmpty()) {
                    this.backendServiceRegistrations.putIfAbsent(oidcBackend, serviceRegistrations);
                }

            }
        }
        return null;
    }

    private String getPrefix(final OIDCBackend oidcBackend) {
        StringBuilder prefixBuilder = new StringBuilder();
        prefixBuilder.append(this.services.getService(DispatcherPrefixService.class).getPrefix());
        //TODO QS-VS: was soll das?? Hard coded ist vermutlich nicht die beste Idee...
        prefixBuilder.append("oidc/");
        String path = oidcBackend.getPath();
        if (!Strings.isEmpty(path)) {
            prefixBuilder.append(path).append("/");
        }
        return prefixBuilder.toString();
    }

    /**
     * Helper method that validates the path to only contain allowed characters
     * @param path The path to be checked.
     * @return
     */
    private void validatePath(String path) throws OXException{
        if (path.matches(".*[^a-zA-Z0-9].*")) {
            throw OIDCExceptionCode.INVALID_BACKEND_PATH.create(path);
        }
    }

    /**
     * Helper method to register a servlet
     * @param servlets the servlets stack of a OIDCBackend
     * @param httpService the HttpService where to register the servlet
     * @param prefix prefix of this OIDCBackend
     * @param servlet the servlet to be registered
     * @param part additional servlet path information
     * @throws ServletException if the servlet's init method throws an exception, or the given servlet object has already been registered at a different alias.
     * @throws NamespaceException if the registration fails because the alias is already in use.
     */
    private void registerServlet(final Stack<String> servlets, HttpService httpService, String prefix, Servlet servlet, String part) throws ServletException, NamespaceException {
        String servletName = prefix + part;
        httpService.registerServlet(servletName, servlet, null, null);
        servlets.push(servletName);
    }

    private void registerRequestHandler(final OIDCBackend backend, final Stack<ServiceRegistration<?>> serviceRegistrations, String oidcAction, LoginRequestHandler requestHandler) {
        Dictionary<String, Object> requestHandlerProps = new Hashtable<String, Object>();
        requestHandlerProps.put(AJAXServlet.PARAMETER_ACTION, oidcAction + OIDCTools.getPathString(backend.getPath()));
        serviceRegistrations.push(context.registerService(LoginRequestHandler.class, requestHandler, requestHandlerProps));
    }

    /**
     * Removes all registered OIDC backends from backend container. Unregisters all
     * servlets for each backend and also all Handlers.
     */
    public void stop() {
        for (OIDCBackend oidcBackend : backends) {
            Stack<String> servlets = backendServlets.remove(oidcBackend);
            try {
                if (null != servlets) {
                    HttpService httpService = services.getService(HttpService.class);
                    while (!servlets.isEmpty()) {
                        String pop = servlets.pop();
                        httpService.unregister(pop);
                    }
                }
            } catch (Exception e) {
                LOG.error("Error while removing path for OIDC Backend", e);
            }
            Stack<ServiceRegistration<?>> registrations = backendServiceRegistrations.remove(oidcBackend);
            try {
                if (null != registrations) {
                    while (!registrations.isEmpty()) {
                        registrations.pop().unregister();
                    }
                }
            } catch (Exception e) {
                LOG.error("Error while removing path for OIDC Backend", e);
            }
        }
    }

    @Override
    public void removedService(ServiceReference<OIDCBackend> reference, OIDCBackend samlBackend) {
        backends.remove(samlBackend);
        Stack<String> servlets = backendServlets.remove(samlBackend);
        try {
            if (null != servlets) {
                HttpService httpService = services.getService(HttpService.class);
                while (!servlets.isEmpty()) {
                    httpService.unregister(servlets.pop());
                }
            }
        } catch (Exception e) {
            LOG.error("Error while removing path for SAML Backend");
        }
        Stack<ServiceRegistration<?>> registrations = backendServiceRegistrations.remove(samlBackend);
        try {
            if (null != registrations) {
                while (!registrations.isEmpty()) {
                    registrations.pop().unregister();
                }
            }
        } catch (Exception e) {
            LOG.error("Error while removing path for SAML Backend");
        }
        context.ungetService(reference);
    }
}
