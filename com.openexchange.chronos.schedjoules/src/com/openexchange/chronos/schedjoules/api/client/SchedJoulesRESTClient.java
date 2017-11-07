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
 *     Copyright (C) 2017-2020 OX Software GmbH
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

package com.openexchange.chronos.schedjoules.api.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.schedjoules.api.auxiliary.SchedJoulesResponseParser;
import com.openexchange.chronos.schedjoules.exception.SchedJoulesAPIExceptionCodes;
import com.openexchange.chronos.schedjoules.impl.SchedJoulesProperty;
import com.openexchange.chronos.schedjoules.osgi.Services;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.rest.client.httpclient.HttpClients;
import com.openexchange.rest.client.httpclient.HttpClients.ClientConfig;

/**
 * {@link SchedJoulesRESTClient}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class SchedJoulesRESTClient implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedJoulesRESTClient.class);

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    private static final String SCHEME = "https";
    private static final String BASE_URL = "api.schedjoules.com";
    private static final String USER_AGENT = "Open-Xchange SchedJoules Client";
    private static final int API_VERSION = 1;

    private static final String ACCEPT_HEADER = "application/vnd.schedjoules; version={{version}}";
    private static final String AUTHORIZATION_HEADER = "Token token=\"{{token}}\"";

    private final CloseableHttpClient httpClient;
    private final String authorizationHeader;
    private final String acceptHeader;

    private final Map<String, BiConsumer<SchedJoulesResponse, HttpResponse>> headerParsers;

    /**
     * Initialises a new {@link SchedJoulesRESTClient}.
     * 
     * @throws OXException if the apiKey is not configured
     */
    public SchedJoulesRESTClient() throws OXException {
        super();
        authorizationHeader = prepareAuthorizationHeader();
        acceptHeader = prepareAcceptHeader();
        httpClient = initializeHttpClient();

        headerParsers = new HashMap<>();

        // Last-Modified header parser
        headerParsers.put(HttpHeaders.LAST_MODIFIED, (schedjoulesResponse, httpResponse) -> {
            String value = getHeaderValue(httpResponse, HttpHeaders.LAST_MODIFIED);
            if (Strings.isEmpty(value)) {
                return;
            }
            try {
                schedjoulesResponse.setLastModified(DATE_FORMATTER.parse(value).getTime());
            } catch (ParseException e) {
                LOGGER.debug("Could not parse the value of the 'Last-Modified' header '{}'", value, e);
            }
        });

        // ETag header parser
        headerParsers.put(HttpHeaders.ETAG, (schedjoulesResponse, httpResponse) -> {
            String value = getHeaderValue(httpResponse, HttpHeaders.ETAG);
            schedjoulesResponse.setETag(value);
        });

        // Content-Type header parser
        headerParsers.put(HttpHeaders.CONTENT_TYPE, (schedjoulesResponse, httpResponse) -> {
            String value = getHeaderValue(httpResponse, HttpHeaders.CONTENT_TYPE);
            if (Strings.isEmpty(value)) {
                return;
            }
            int indexOf = value.indexOf(';');
            schedjoulesResponse.setContentType(indexOf < 0 ? value : value.substring(0, indexOf));
        });
    }

    /**
     * Shuts down the REST client.
     */
    @Override
    public void close() {
        if (httpClient == null) {
            return;
        }
        try {
            httpClient.close();
        } catch (IOException e) {
            LOGGER.debug("Error closing the http client: {}", e.getMessage(), e);
        }
    }

    /**
     * Retrieves the value of the specified header, or <code>null</code> if no such header exists
     * or the value of the header is <code>null</code>
     * 
     * @param httpResponse The {@link HttpResponse}
     * @param headerName The header's name
     * @return the value of the specified header, or <code>null</code> if no such header exists or
     *         the value of the header is <code>null</code>.
     */
    private String getHeaderValue(HttpResponse httpResponse, String headerName) {
        Header ctHeader = httpResponse.getFirstHeader(headerName);
        if (ctHeader == null) {
            return null;
        }
        String value = ctHeader.getValue();
        if (Strings.isEmpty(value)) {
            return null;
        }
        return value;
    }

    /**
     * Prepares the 'Authorization' header
     * 
     * @return The authorisation header
     * @throws OXException if the API key is not configured
     */
    private String prepareAuthorizationHeader() throws OXException {
        LeanConfigurationService service = Services.getService(LeanConfigurationService.class);
        String apiKey = service.getProperty(SchedJoulesProperty.apiKey);
        if (Strings.isEmpty(apiKey)) {
            throw SchedJoulesAPIExceptionCodes.NO_API_KEY_CONFIGURED.create(SchedJoulesProperty.apiKey.getFQPropertyName());
        }
        return AUTHORIZATION_HEADER.replaceFirst("\\{\\{token\\}\\}", apiKey);
    }

    /**
     * Prepares the 'Accept' header
     * 
     * @return the 'Accept' header
     */
    private String prepareAcceptHeader() {
        return ACCEPT_HEADER.replaceFirst("\\{\\{version\\}\\}", Integer.toString(API_VERSION));
    }

    /**
     * Initialises the HTTP client
     * 
     * @return The initialised {@link CloseableHttpClient}
     */
    private CloseableHttpClient initializeHttpClient() {
        ClientConfig clientConfig = ClientConfig.newInstance();
        clientConfig.setUserAgent(USER_AGENT);

        return HttpClients.getHttpClient(clientConfig);
    }

    /**
     * Prepares the specified HTTP request and adds the Authorization header
     * 
     * @param request The {@link HttpRequestBase} to prepare
     * @param scheme The mandatory scheme/protocol
     * @param baseUrl The mandatory base URL
     * @param path The mandatory path
     * @param query The optional query string
     * @throws OXException if the path is not valid
     */
    private void prepareAuthorizedRequest(HttpRequestBase request, String scheme, String baseUrl, String path, String query, String eTag) throws OXException {
        prepareRequest(request, scheme, baseUrl, path, query, eTag);
        request.addHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
    }

    /**
     * Prepares the specified HTTP request
     * 
     * @param request The {@link HttpRequestBase} to prepare
     * @param scheme The mandatory scheme/protocol
     * @param baseUrl The mandatory base URL
     * @param path The mandatory path
     * @param query The optional query string
     * @throws OXException if the path is not valid
     */
    private void prepareRequest(HttpRequestBase request, String scheme, String baseUrl, String path, String query, String eTag) throws OXException {
        try {
            request.setURI(new URI(scheme, baseUrl, path, query, null));
            request.addHeader(HttpHeaders.ACCEPT, acceptHeader);
            if (!Strings.isEmpty(eTag)) {
                request.addHeader(HttpHeaders.IF_NONE_MATCH, eTag);
            }
        } catch (URISyntaxException e) {
            throw SchedJoulesAPIExceptionCodes.INVALID_URI_PATH.create(path, e);
        }
    }

    /**
     * Prepares the specified {@link SchedJoulesRequest}
     * 
     * @param request The {@link SchedJoulesRequest} to prepare
     * @return an {@link HttpUriRequest}
     * @throws OXException if an unknown HTTP method is defined in the request
     */
    private HttpRequestBase prepareRequest(SchedJoulesRequest request) throws OXException {
        // Prepare the query
        String query = prepareQuery(request.getQueryParameters());

        // Prepare the request
        HttpRequestBase httpRequest = createRequest(request.getMethod());
        prepareAuthorizedRequest(httpRequest, SCHEME, BASE_URL, request.getPath(), query, request.getETag());
        return httpRequest;
    }

    /**
     * Creates an {@link HttpRequestBase} with the specified {@link HttpMethod}
     * 
     * @param httpMethod The {@link HttpMethod}
     * @return The new {@link HttpRequestBase}
     * @throws OXException if an unknown HTTP method is provided
     */
    private HttpRequestBase createRequest(HttpMethod httpMethod) throws OXException {
        HttpRequestBase httpRequest;
        switch (httpMethod) {
            case GET:
                httpRequest = new HttpGet();
                break;
            case HEAD:
                httpRequest = new HttpHead();
                break;
            default:
                throw SchedJoulesAPIExceptionCodes.UNKNOWN_HTTP_METHOD.create(httpMethod);
        }
        return httpRequest;
    }

    /**
     * Creates an {@link HttpUriRequest} from the specified {@link URL} and the specified {@link HttpMethod}
     * 
     * @param url The {@link URL} to use
     * @param httpMethod The {@link HttpMethod}
     * @return The new {@link HttpUriRequest}
     * @throws OXException if an unknown HTTP method is provided
     * @throws URISyntaxException If an invalid URL is provided
     */
    private HttpRequestBase prepareRequest(URL url, HttpMethod httpMethod, String eTag) throws OXException {
        HttpRequestBase httpRequest = createRequest(httpMethod);
        prepareRequest(httpRequest, url.getProtocol(), url.getHost(), url.getPath(), url.getQuery(), eTag);
        return httpRequest;
    }

    /**
     * Prepares the query string from the specified query parameters
     * 
     * @param queryParameters The {@link Map} containing the query parameters
     * @return The query string
     */
    private String prepareQuery(Map<String, String> queryParameters) {
        if (queryParameters.isEmpty()) {
            return new String();
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String name : queryParameters.keySet()) {
            stringBuilder.append(name).append("=").append(queryParameters.get(name)).append("&");
        }
        stringBuilder.setLength(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    /**
     * Executes the specified {@link SchedJoulesRequest}
     * 
     * @param request The {@link SchedJoulesRequest} to execute
     * @return The {@link SchedJoulesResponse}
     * @throws OXException if an error is occurred
     */
    public SchedJoulesResponse executeRequest(SchedJoulesRequest request) throws OXException {
        return executeRequest(prepareRequest(request));
    }

    /**
     * Executes a GET request to the specified {@link URL}
     * 
     * @param url The {@link URL}
     * @return The {@link SchedJoulesResponse}
     * @throws OXException if an error is occurred
     */
    public SchedJoulesResponse executeRequest(URL url) throws OXException {
        return executeRequest(url, HttpMethod.GET, null);
    }

    /**
     * Executes a request with the specified method to the specified {@link URL}
     * 
     * @param url The {@link URL}
     * @param httpMethod the {@link HttpMethod} to use
     * @return The {@link SchedJoulesResponse}
     * @throws OXException if an error is occurred
     */
    public SchedJoulesResponse executeRequest(URL url, HttpMethod httpMethod, String eTag) throws OXException {
        return executeRequest(prepareRequest(url, httpMethod, eTag));
    }

    /**
     * Executes the specified {@link HttpUriRequest} and returns a {@link SchedJoulesResponse}
     * 
     * @param httpRequest the {@link HttpUriRequest} to execute
     * @return The {@link SchedJoulesResponse}
     * @throws OXException if an error is occurred
     */
    private SchedJoulesResponse executeRequest(HttpRequestBase httpRequest) throws OXException {
        CloseableHttpResponse httpResponse = null;
        try {
            // Execute the request
            httpResponse = httpClient.execute(httpRequest);

            // Get the response code and assert
            int statusCode = assertStatusCode(httpResponse);
            if (statusCode == 304) {
                // OK, nothing was modified, no response body, return as is
                return new SchedJoulesResponse(statusCode);
            }

            // Prepare the response
            return prepareResponse(httpResponse);
        } catch (ClientProtocolException e) {
            throw SchedJoulesAPIExceptionCodes.CLIENT_PROTOCOL_ERROR.create(e.getMessage(), e);
        } catch (IOException e) {
            throw SchedJoulesAPIExceptionCodes.IO_ERROR.create(e.getMessage(), e);
        } finally {
            consume(httpResponse);
            reset(httpRequest);

        }
    }

    /**
     * Consumes the specified {@link HttpResponse}
     * 
     * @param response the {@link HttpResponse} to consume
     */
    private void consume(HttpResponse response) {
        if (null == response) {
            return;
        }
        HttpEntity entity = response.getEntity();
        if (null == entity) {
            return;
        }
        try {
            EntityUtils.consume(entity);
        } catch (Throwable e) {
            LOGGER.debug("Error while consuming the entity of the HTTP response {}", e.getMessage(), e);
        }
    }

    /**
     * Resets the specified {@link HttpRequestBase}
     * 
     * @param httpRequest The {@link HttpRequestBase} to reset
     */
    private void reset(HttpRequestBase httpRequest) {
        if (httpRequest == null) {
            return;
        }
        try {
            httpRequest.reset();
        } catch (final Throwable e) {
            LOGGER.debug("Error while resetting the HTTP request {}", e.getMessage(), e);
        }
    }

    /**
     * Prepares the {@link SchedJoulesResponse} from the specified {@link HttpResponse}
     * 
     * @param httpResponse The {@link HttpResponse} to extract the content from
     * @return the {@link SchedJoulesResponse}
     * @throws IOException if an I/O error is occurred
     * @throws OXException if any other error is occurred
     */
    private SchedJoulesResponse prepareResponse(HttpResponse httpResponse) throws IOException, OXException {
        SchedJoulesResponse response = new SchedJoulesResponse(httpResponse.getStatusLine().getStatusCode());
        parseHeaders(httpResponse, response);

        HttpEntity entity = httpResponse.getEntity();
        if (entity == null) {
            return response;
        }
        response.setStream(entity.getContent());
        response.setResponseBody(SchedJoulesResponseParser.parse(response));
        return response;
    }

    /**
     * Parses from the specified {@link HttpResponse} the headers that are defined in the {@link #headerParsers}
     * and sets them to the specified {@link SchedJoulesResponse}
     * 
     * @param httpResponse The {@link HttpResponse}
     * @param schedjoulesResponse The {@link SchedJoulesResponse}
     */
    private void parseHeaders(HttpResponse httpResponse, SchedJoulesResponse schedjoulesResponse) {
        for (String key : headerParsers.keySet()) {
            headerParsers.get(key).accept(schedjoulesResponse, httpResponse);
        }
    }

    /**
     * Asserts the status code for any errors
     * 
     * @param httpResponse The {@link HttpResponse}'s status code to assert
     * @return The status code
     * @throws OXException if an HTTP error is occurred (4xx or 5xx)
     */
    private int assertStatusCode(HttpResponse httpResponse) throws OXException {
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        // Assert the 4xx codes
        switch (statusCode) {
            case 401:
                throw SchedJoulesAPIExceptionCodes.NOT_AUTHORIZED.create(httpResponse.getStatusLine().getReasonPhrase(), SchedJoulesProperty.apiKey.getFQPropertyName());
            case 404:
                throw SchedJoulesAPIExceptionCodes.PAGE_NOT_FOUND.create();
        }
        if (statusCode >= 400 && statusCode <= 499) {
            throw SchedJoulesAPIExceptionCodes.UNEXPECTED_ERROR.create(httpResponse.getStatusLine());
        }

        // Assert the 5xx codes
        switch (statusCode) {
            case 500:
                throw SchedJoulesAPIExceptionCodes.REMOTE_INTERNAL_SERVER_ERROR.create(httpResponse.getStatusLine().getReasonPhrase());
            case 503:
                throw SchedJoulesAPIExceptionCodes.REMOTE_SERVICE_UNAVAILABLE.create(httpResponse.getStatusLine().getReasonPhrase());
        }
        if (statusCode >= 500 && statusCode <= 599) {
            throw SchedJoulesAPIExceptionCodes.REMOTE_SERVER_ERROR.create(httpResponse.getStatusLine());
        }
        return statusCode;
    }
}
