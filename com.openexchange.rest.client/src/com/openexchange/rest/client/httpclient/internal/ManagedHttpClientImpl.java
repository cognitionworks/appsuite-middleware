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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.rest.client.httpclient.internal;

import static com.openexchange.java.Autoboxing.b;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import com.openexchange.annotation.NonNull;
import com.openexchange.rest.client.httpclient.ManagedHttpClient;


/**
 * {@link ManagedHttpClientImpl}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.4
 */
@SuppressWarnings("deprecation")
public class ManagedHttpClientImpl implements ManagedHttpClient {

    private final String clientId;
    private final AtomicReference<CloseableHttpClient> httpClientReference;
    private final AtomicReference<ClientConnectionManager> ccm;
    private final Supplier<Boolean> reloadCallback;

    private volatile int configHashCode;

    /**
     * 
     * Initializes a new {@link ManagedHttpClientImpl}.
     * 
     * @param clientId The client identifier
     * @param configHashCode The hash code of the client configuration
     * @param httpClient The actual HTTP client
     * @param ccm The connection manager of the HTTP client
     * @param reloadCallback A callback that initializes a new HTTP client and replaces it in this managed instance.
     *            <p>
     *            Apache framework silently closes the connection pool, when execution of a request fails. This
     *            callback ensures that a working HTTP client is returned to the caller or at least an error can be
     *            handled.
     *            <p>
     *            If the creation of the new HTTP client fails the managed instance will be removed from the cache and
     *            the underlying HTTP client closed. This will persist the error in cases where the managed client is a
     *            member within the calling class.
     *            <p>
     *            When called, will return <code>true</code> in case the client has been successfully reloaded,
     *            <code>false</code> in case the client is unusable.
     */
    public ManagedHttpClientImpl(String clientId, int configHashCode, CloseableHttpClient httpClient, ClientConnectionManager ccm, Supplier<Boolean> reloadCallback) {
        super();
        this.clientId = clientId;
        this.configHashCode = configHashCode;
        this.httpClientReference = new AtomicReference<>(httpClient);
        this.ccm = new AtomicReference<>(ccm);
        this.reloadCallback = reloadCallback;
    }

    private @NonNull HttpClient getHttpClient() throws IllegalStateException {
        CloseableHttpClient httpClient = httpClientReference.get();
        if (null == httpClient) {
            throw new IllegalStateException("HttpClient is null.");
        }
        if (ccm.get().isShutdown()) {
            /*
             * Connection manager is unusable. Client has to be re-created.
             */
            if (b(reloadCallback.get())) {
                httpClient = httpClientReference.get();
                if (null == httpClient) {
                    throw new IllegalStateException("HttpClient is null.");
                }
                return httpClient;
            }
            throw new IllegalStateException("HttpClient is not useable.");
        }
        return httpClient;
    }

    /**
     * Removes the HTTP client reference from this managed instance
     *
     * @return The HTTP client
     */
    public CloseableHttpClient unset() {
        ccm.set(null);
        return httpClientReference.getAndSet(null);
    }

    /**
     * Replaces the HTTP client in this managed instance
     *
     * @param newHttpClient The new HTTP client
     * @param ccm The ClientConnectionManager the HTTP client uses
     * @param configHashCode The hash code of the configuration used to create the HTTP client
     * @return The (old) HTTP client used by this managed instance
     */
    public CloseableHttpClient reload(CloseableHttpClient newHttpClient, ClientConnectionManager ccm, int configHashCode) {
        this.configHashCode = configHashCode;
        this.ccm.set(ccm);
        return httpClientReference.getAndSet(newHttpClient);
    }

    /**
     * Gets the clientId
     *
     * @return The clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets the configHash
     *
     * @return The configHash
     */
    public int getConfigHash() {
        return configHashCode;
    }

    @Override
    public String toString() {
        return "managedHttpClient[clientId=" + clientId + ", configHashCode=" + String.valueOf(configHashCode) + "]";
    }

    // ------------------------------------------------ HTTP client methods --------------------------------------------------------------

    @Override
    public HttpResponse execute(HttpUriRequest request) throws IOException, ClientProtocolException {
        return getHttpClient().execute(request);
    }

    @Override
    public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException, ClientProtocolException {
        return getHttpClient().execute(request, context);
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException, ClientProtocolException {
        return getHttpClient().execute(target, request);
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
        return getHttpClient().execute(target, request, context);
    }

    @Override
    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return getHttpClient().execute(request, responseHandler);
    }

    @Override
    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
        return getHttpClient().execute(request, responseHandler, context);
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return getHttpClient().execute(target, request, responseHandler);
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
        return getHttpClient().execute(target, request, responseHandler, context);
    }

    @Override
    public HttpParams getParams() {
        return getHttpClient().getParams();
    }

    @Override
    public org.apache.http.conn.ClientConnectionManager getConnectionManager() {
        return getHttpClient().getConnectionManager();
    }

}
