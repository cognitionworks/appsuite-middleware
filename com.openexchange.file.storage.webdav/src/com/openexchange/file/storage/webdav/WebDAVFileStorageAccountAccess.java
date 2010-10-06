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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.file.storage.webdav;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageAccountAccess;
import com.openexchange.file.storage.FileStorageException;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.FileStorageFolder;
import com.openexchange.file.storage.FileStorageFolderAccess;
import com.openexchange.file.storage.webdav.session.WebDAVHttpClientRegistry;
import com.openexchange.session.Session;

/**
 * {@link WebDAVFileStorageAccountAccess}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class WebDAVFileStorageAccountAccess implements FileStorageAccountAccess {

    /**
     * The default HTTP time out.
     */
    private static final int TIMEOUT = 60000;

    /**
     * The maximum number of connections to be used for a host configuration.
     */
    private static final int MAX_HOST_CONNECTIONS = 20;

    /**
     * The HTTPS identifier constant.
     */
    private static final String HTTPS = "https";

    /**
     * The HTTP protocol constant.
     */
    private static final Protocol PROTOCOL_HTTP = Protocol.getProtocol("http");

    /*-
     * Member stuff
     */

    private final AtomicReference<HttpClient> httpClientRef;

    private final FileStorageAccount account;

    private final Session session;

    private volatile FileStorageFolderAccess folderAccess;

    // private FileStorageFileAccess fileAccess;

    /**
     * Initializes a new {@link WebDAVFileStorageAccountAccess}.
     */
    public WebDAVFileStorageAccountAccess(final FileStorageAccount account, final Session session) {
        super();
        httpClientRef = new AtomicReference<HttpClient>();
        this.account = account;
        this.session = session;
    }

    public void connect() throws FileStorageException {
        if (null != httpClientRef.get()) {
            return;
        }
        final String url = (String) account.getConfiguration().get(WebDAVConstants.WEBDAV_URL);
        if (null == url) {
            throw FileStorageExceptionCodes.MISSING_PARAMETER.create(WebDAVConstants.WEBDAV_URL);
        }
        HttpClient client = httpClientRef.get();
        if (null == client) {
            final HttpClient daClient = clientFor(url, account, session);
            if (httpClientRef.compareAndSet(null, daClient)) {
                client = daClient;
            } else {
                client = httpClientRef.get();
            }
        }
    }

    public boolean isConnected() {
        return (null != httpClientRef.get());
    }

    public void close() {
        /*
         * Close is performed when last session gone by FacebookEventHandler
         */
        httpClientRef.set(null);
    }

    public boolean ping() throws FileStorageException {
        final String url = (String) account.getConfiguration().get(WebDAVConstants.WEBDAV_URL);
        if (null == url) {
            throw FileStorageExceptionCodes.MISSING_PARAMETER.create(WebDAVConstants.WEBDAV_URL);
        }
        final HttpClient client = createNewHttpClient(url, account.getConfiguration());
        checkHttpClient(url, client);
        ((MultiThreadedHttpConnectionManager) client.getHttpConnectionManager()).shutdown();
        return true;
    }

    public boolean cacheable() {
        return true;
    }

    public String getAccountId() {
        return account.getId();
    }

    public FileStorageFolderAccess getFolderAccess() throws FileStorageException {
        final HttpClient client = httpClientRef.get();
        if (null == client) {
            throw FileStorageExceptionCodes.NOT_CONNECTED.create();
        }
        FileStorageFolderAccess tmp = folderAccess;
        if (null == tmp) {
            synchronized (this) {
                tmp = folderAccess;
                if (null == tmp) {
                    folderAccess = tmp = new WebDAVFileStorageFolderAccess(client, account, session);
                }
            }
        }
        return tmp;
    }

    public FileStorageFolder getRootFolder() throws FileStorageException {
        connect();
        return getFolderAccess().getRootFolder();
    }

    /*-
     * ------------------------------------------------------------------------------------------------------------------------------------
     * ----------------------------------------------------------- Helper methods ---------------------------------------------------------
     * ------------------------------------------------------------------------------------------------------------------------------------
     */

    /**
     * Gets the HttpClient for given WebDAV account.
     * 
     * @param account The facebook messaging account providing credentials and settings
     * @param session The user session
     * @return The HttpClient; either newly created or fetched from underlying registry
     * @throws FileStorageException If a HttpClient could not be created
     */
    private static HttpClient clientFor(final String url, final FileStorageAccount account, final Session session) throws FileStorageException {
        final WebDAVHttpClientRegistry registry = WebDAVHttpClientRegistry.getInstance();
        final String accountId = account.getId();
        HttpClient client = registry.getClient(session.getContextId(), session.getUserId(), accountId);
        if (null == client) {
            final HttpClient newInstance = createNewHttpClient(url, account.getConfiguration());
            checkHttpClient(url, newInstance);
            client = registry.addClient(session.getContextId(), session.getUserId(), accountId, newInstance);
            if (null == client) {
                client = newInstance;
            }
        }
        return client;
    }

    /**
     * Creates a new {@link HttpClient}.
     * 
     * @return The newly created {@link HttpClient}
     * @throws FileStorageException If creation fails
     */
    private static HttpClient createNewHttpClient(final String urlStr, final Map<String, Object> configuration) throws FileStorageException {
        // http://www.jarvana.com/jarvana/view/org/apache/jackrabbit/jackrabbit-webdav/2.0-beta3/jackrabbit-webdav-2.0-beta3-javadoc.jar!/org/apache/jackrabbit/webdav/client/methods/package-summary.html
        /*
         * The URL to WebDAV server
         */
        final URL url;
        try {
            url = new URL(urlStr);
        } catch (final MalformedURLException e) {
            throw FileStorageExceptionCodes.INVALID_URL.create(e, urlStr, e.getMessage());
        }
        /*
         * Create host configuration or URI
         */
        final HostConfiguration hostConfiguration;
        {
            final String host = url.getHost();
            if (HTTPS.equalsIgnoreCase(url.getProtocol())) {
                int port = url.getPort();
                if (port == -1) {
                    port = 443;
                }
                /*
                 * Own HTTPS host configuration and relative URI
                 */
                final Protocol httpsProtocol = new Protocol(HTTPS, ((ProtocolSocketFactory) new TrustAllAdapter()), port);
                hostConfiguration = new HostConfiguration();
                hostConfiguration.setHost(host, port, httpsProtocol);
            } else {
                int port = url.getPort();
                if (port == -1) {
                    port = 80;
                }
                /*
                 * HTTP host configuration and relative URI
                 */
                hostConfiguration = new HostConfiguration();
                hostConfiguration.setHost(host, port, PROTOCOL_HTTP);
            }
        }
        /*
         * Define a HttpConnectionManager, which is also responsible for possible multi-threading support
         */
        final HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        final HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setMaxConnectionsPerHost(hostConfiguration, MAX_HOST_CONNECTIONS);
        connectionManager.setParams(params);
        /*
         * Create the HttpClient object and eventually pass the Credentials:
         */
        final HttpClient newClient = new HttpClient(connectionManager);
        int timeout = TIMEOUT;
        {
            final String sTimeout = (String) configuration.get(WebDAVConstants.WEBDAV_TIMEOUT);
            if (null != sTimeout) {
                try {
                    timeout = Integer.parseInt(sTimeout);
                } catch (final NumberFormatException e) {
                    org.apache.commons.logging.LogFactory.getLog(WebDAVFileStorageAccountAccess.class).warn(
                        "Configuration property \"" + WebDAVConstants.WEBDAV_TIMEOUT + "\" is not a number: " + sTimeout);
                    timeout = TIMEOUT;
                }
            }
        }
        newClient.getParams().setSoTimeout(timeout);
        newClient.getParams().setIntParameter("http.connection.timeout", timeout);
        newClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));
        newClient.setHostConfiguration(hostConfiguration);
        /*
         * Apply credentials
         */
        final String login = (String) configuration.get(WebDAVConstants.WEBDAV_LOGIN);
        final String password = (String) configuration.get(WebDAVConstants.WEBDAV_PASSWORD);
        if (null != login && null != password) {
            final Credentials creds = new UsernamePasswordCredentials(login, password);
            newClient.getParams().setAuthenticationPreemptive(true);
            newClient.getState().setCredentials(AuthScope.ANY, creds);
        }
        /*
         * Return
         */
        return newClient;
    }

    /**
     * Performs a dummy request with given {@link HttpClient}.
     * 
     * @param url The URL to WebDAV server
     * @param client The HttpClient to check
     * @throws FileStorageException If check fails
     */
    private static void checkHttpClient(final String url, final HttpClient client) throws FileStorageException {
        try {
            /*
             * Check
             */
            final DavMethod method = new PropFindMethod(url, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
            try {
                client.executeMethod(method);
                /*
                 * Check if request was successfully executed
                 */
                method.checkSuccess();
            } finally {
                method.releaseConnection();
            }
        } catch (final HttpException e) {
            throw WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final DavException e) {
            if (HttpServletResponse.SC_UNAUTHORIZED == e.getErrorCode()) {
                throw WebDAVFileStorageExceptionCodes.INVALID_CREDS.create(e, url);
            }
            throw WebDAVFileStorageExceptionCodes.DAV_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

}
