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

package com.openexchange.http.grizzly;

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import com.openexchange.config.ConfigTools;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.http.grizzly.osgi.GrizzlyServiceRegistry;
import com.openexchange.server.Initialization;

/**
 * {@link GrizzlyConfig} Collects and exposes configuration parameters needed by GrizzlOX
 *
 * @author <a href="mailto:marc	.arens@open-xchange.com">Marc Arens</a>
 */
public class GrizzlyConfig implements Initialization {

    private static final Log LOG = com.openexchange.log.Log.loggerFor(GrizzlyConfig.class);

    private static final GrizzlyConfig instance = new GrizzlyConfig();

    public static GrizzlyConfig getInstance() {
        return instance;
    }

    private final AtomicBoolean started = new AtomicBoolean();

    // grizzly properties

    /** The host for the http network listener. Default value: 0.0.0.0, bind to every nic of your host. */
    private String httpHost = "0.0.0.0";

    /** The default port for the http network listener. */
    private int httpPort = 8080;

    /** Enable grizzly monitoring via JMX? */
    private boolean isJMXEnabled = false;

    /** Enable Bi-directional, full-duplex communications channels over a single TCP connection. */
    private boolean isWebsocketsEnabled = false;

    /** Enable Technologies for pseudo realtime communication with the server */
    private boolean isCometEnabled = false;

    /** The max. number of allowed request parameters */
    private int maxRequestParameters = 30;

    /** Unique backend route for every single backend behind the load balancer */
    private String backendRoute = "OX0";

    // server properties

    /** Maximal age of a cookie in seconds. A negative value destroys the cookie when the browser exits. A value of 0 deletes the cookie. */
    private int cookieMaxAge = 604800;

    /** Interval between two client requests in seconds until the JSession is declared invalid */
    private int cookieMaxInactivityInterval = 1800;

    /** Marks cookies as secure although the request is insecure e.g. when the backend is behind a ssl terminating proxy */
    private boolean isCookieForceHttps = false;

    /** Make the cookie accessible only via http methods. This prevents Javascript access to the cookie / cross site scripting */
    private boolean isCookieHttpOnly = true;

    /** Default encoding for incoming Http Requests, this value must be equal to the web server's default encoding */
    private String defaultEncoding = "UTF-8";

    /** The name of the protocolHeader used to decide if we are dealing with a in-/secure Request */
    private String protocolHeader = "X-Forwarded-Proto";

    /** The value indicating secure http communication */
    private String httpsProtoValue = "https";

    /** The port used for http communication */
    private int httpProtoPort = 80;

    /** The port used for https communication */
    private int httpsProtoPort = 443;

    // sessiond properties

    /** Is autologin enabled in the session.d properties? */
    private boolean isSessionAutologin = false;

    @Override
    public void start() throws OXException {
        if (!started.compareAndSet(false, true)) {
            LOG.error(this.getClass().getName() + " already started");
            return;
        }
        init();
    }

    @Override
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            LOG.error(this.getClass().getName() + " cannot be stopped since it has no been started before");
            return;
        }
    }

    private void init() throws OXException {
        ConfigurationService configService = GrizzlyServiceRegistry.getInstance().getService(ConfigurationService.class);
        if (configService == null) {
            throw GrizzlyExceptionCode.NEEDED_SERVICE_MISSING.create(ConfigurationService.class.getSimpleName());
        }

        // grizzly properties
        this.isJMXEnabled = configService.getBoolProperty("com.openexchange.http.grizzly.hasJMXEnabled", false);
        this.isWebsocketsEnabled = configService.getBoolProperty("com.openexchange.http.grizzly.hasWebSocketsEnabled", false);
        this.isCometEnabled = configService.getBoolProperty("com.openexchange.http.grizzly.hasCometEnabled", false);

        // server properties
        this.cookieMaxAge = Integer.valueOf(ConfigTools.parseTimespanSecs(configService.getProperty("com.openexchange.cookie.ttl", "1W")));
        this.cookieMaxInactivityInterval = configService.getIntProperty("com.openexchange.servlet.maxInactiveInterval", 1800);
        this.isCookieForceHttps = configService.getBoolProperty("com.openexchange.forceHTTPS", false);
        this.isCookieHttpOnly = configService.getBoolProperty("com.openexchange.cookie.httpOnly", true);
        this.defaultEncoding = configService.getProperty("DefaultEncoding", "UTF-8");
        this.protocolHeader = configService.getProperty("com.openexchange.server.protocolHeader", "X-Forwarded-Proto");
        this.httpsProtoValue = configService.getProperty("com.openexchange.server.httpsProtoValue", "https");
        this.httpProtoPort = configService.getIntProperty("com.openexchange.server.httpProtoPort", 80);
        this.httpsProtoPort = configService.getIntProperty("com.openexchange.server.httpsProtoPort", 443);

        this.httpHost = configService.getProperty("com.openexchange.connector.networkListenerHost", "127.0.0.1");
        // keep backwards compatibility with ajp config
        if(httpHost.equals("*")) {
            this.httpHost="0.0.0.0";
        }
        this.httpPort = configService.getIntProperty("com.openexchange.connector.networkListenerPort", 8009);
        this.maxRequestParameters = configService.getIntProperty("com.openexchange.connector.maxRequestParameters", 30);
        this.backendRoute = configService.getProperty("com.openexchange.server.backendRoute", "OX0");

        // sessiond properties
        this.isSessionAutologin = configService.getBoolProperty("com.openexchange.sessiond.autologin", false);

    }

    /**
     * Gets the started
     *
     * @return The started
     */
    public AtomicBoolean getStarted() {
        return started;
    }

    /**
     * Gets the defaultEncoding used for incoming http requests
     *
     * @return The defaultEncoding
     */
    public String getDefaultEncoding() {
        return defaultEncoding;
    }

    /**
     * Gets the httpHost
     *
     * @return The httpHost
     */
    public String getHttpHost() {
        return instance.httpHost;
    }

    /**
     * Gets the httpPort
     *
     * @return The httpPort
     */
    public int getHttpPort() {
        return instance.httpPort;
    }

    /**
     * Gets the hasJMXEnabled
     *
     * @return The hasJMXEnabled
     */
    public boolean isJMXEnabled() {
        return instance.isJMXEnabled;
    }

    /**
     * Gets the hasWebsocketsEnabled
     *
     * @return The hasWebsocketsEnabled
     */
    public boolean isWebsocketsEnabled() {
        return instance.isWebsocketsEnabled;
    }

    /**
     * Gets the hasCometEnabled
     *
     * @return The hasCometEnabled
     */
    public boolean isCometEnabled() {
        return instance.isCometEnabled;
    }

    /**
     * Gets the maxRequestParameters
     *
     * @return The maxRequestParameters
     */
    public int getMaxRequestParameters() {
        return instance.maxRequestParameters;
    }

    /**
     * Gets the backendRoute
     *
     * @return The backendRoute
     */
    public String getBackendRoute() {
        return instance.backendRoute;
    }

    /**
     * Gets the cookieMaxAge
     *
     * @return The cookieMaxAge
     */
    public int getCookieMaxAge() {
        return instance.cookieMaxAge;
    }

    /**
     * Gets the cookieMaxInactivityInterval
     *
     * @return The cookieMaxInactivityInterval
     */
    public int getCookieMaxInactivityInterval() {
        return instance.cookieMaxInactivityInterval;
    }

    /**
     * Gets the isCookieForceHttps
     *
     * @return The isCookieForceHttps
     */
    public boolean isCookieForceHttps() {
        return instance.isCookieForceHttps;
    }

    /**
     * Gets the isCookieHttpOnly
     *
     * @return The isCookieHttpOnly
     */
    public boolean isCookieHttpOnly() {
        return instance.isCookieHttpOnly;
    }

    /**
     * Gets the isSessionAutologin
     *
     * @return The isSessionAutologin
     */
    public boolean isSessionAutologin() {
        return instance.isSessionAutologin;
    }


    /**
     * Gets the log
     *
     * @return The log
     */
    public static Log getLog() {
        return LOG;
    }


    /**
     * Gets the protocolHeader
     *
     * @return The protocolHeader
     */
    public String getProtocolHeader() {
        return protocolHeader;
    }


    /**
     * Gets the httpsProtoValue
     *
     * @return The httpsProtoValue
     */
    public String getHttpsProtoValue() {
        return httpsProtoValue;
    }


    /**
     * Gets the httpProtoPort
     *
     * @return The httpProtoPort
     */
    public int getHttpProtoPort() {
        return httpProtoPort;
    }


    /**
     * Gets the httpsProtoPort
     *
     * @return The httpsProtoPort
     */
    public int getHttpsProtoPort() {
        return httpsProtoPort;
    }

}
