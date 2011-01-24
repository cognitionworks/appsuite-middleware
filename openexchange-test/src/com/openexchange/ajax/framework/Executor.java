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

package com.openexchange.ajax.framework;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie2;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.cookies.CookieJar;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.framework.AJAXRequest.FieldParameter;
import com.openexchange.ajax.framework.AJAXRequest.FileParameter;
import com.openexchange.ajax.framework.AJAXRequest.Parameter;
import com.openexchange.configuration.AJAXConfig;
import com.openexchange.configuration.AJAXConfig.Property;
import com.openexchange.tools.URLParameter;
import com.openexchange.tools.servlet.AjaxException;

public class Executor extends Assert {
    /**
     * To use character encoding.
     */
    private static final String ENCODING = "UTF-8";

    /**
     * Prevent instantiation
     */
    private Executor() {
        super();
    }

    public static <T extends AbstractAJAXResponse> T execute(final AJAXClient client,
        final AJAXRequest<T> request) throws AjaxException, IOException,
        SAXException, JSONException {
        return execute(client.getSession(), request);
    }

    public static <T extends AbstractAJAXResponse> T execute(final AJAXSession session,
        final AJAXRequest<T> request) throws AjaxException, IOException,
        SAXException, JSONException {
        return execute(session, request,
            AJAXConfig.getProperty(Property.PROTOCOL),
            AJAXConfig.getProperty(Property.HOSTNAME));
    }

    public static <T extends AbstractAJAXResponse> T execute(final AJAXSession session,
        final AJAXRequest<T> request, final String hostname) throws AjaxException,
        IOException, SAXException, JSONException {
        return execute(session, request, AJAXConfig
            .getProperty(Property.PROTOCOL), hostname);
    }

    public static <T extends AbstractAJAXResponse> T execute(final AJAXSession session, final AJAXRequest<T> request,
            final String protocol, final String hostname) throws AjaxException, IOException, SAXException,
            JSONException {

        final String urlString = protocol + "://" + hostname + request.getServletPath();
        final HttpUriRequest httpRequest;
        switch (request.getMethod()) {
        case GET:
        	httpRequest = new HttpGet(
        			addQueryParamsToUri(urlString, getGETParameter(session, request)));
            break;
        case POST:
        	HttpPost httpPost = new HttpPost(urlString + getURLParameter(session, request, true));
        	httpPost.setEntity( getBodyParameters(request));
        	httpPost.getParams().setParameter("Content-Type", "application/x-www-form-urlencoded");
        	//Kommt so nicht an, ist immer noch text/plain
        	httpRequest = httpPost;
            break;
        case UPLOAD:
        	HttpPost httpUpload = new HttpPost(urlString + getURLParameter(session, request, false)); //TODO old request used to set "mimeEncoded" = true here
        	addUPLOADParameter(httpUpload, request);
        	httpRequest = httpUpload;
            break;
        case PUT:
        	HttpPut httpPut = new HttpPut(urlString + getURLParameter(session, request, false));
        	httpPut.setEntity( new InputStreamEntity( createBody( request.getBody()), -1));
        	httpRequest = httpPut;
            break;
        default:
            throw new AjaxException(AjaxException.Code.InvalidParameter, request.getMethod().name());
        }
        
        DefaultHttpClient newClient = new OxHttpClient();

    	CookieStore cookieStore = session.getCookieStore();
    	newClient.setCookieStore(cookieStore); 
    	final WebConversation conv = session.getConversation();
    	syncCookies(newClient, conv, hostname);
    	
    	
        long startRequest = System.currentTimeMillis();
    	HttpResponse response = newClient.execute(httpRequest);
        long requestDuration = System.currentTimeMillis() - startRequest;

        AbstractAJAXParser<? extends T> parser = request.getParser();
        parser.checkResponse(response);
        String responseBody = EntityUtils.toString(response.getEntity());
        
        long startParse = System.currentTimeMillis();
        T retval = parser.parse(responseBody);
        long parseDuration = System.currentTimeMillis() - startParse;

        retval.setRequestDuration(requestDuration);
        retval.setParseDuration(parseDuration);
        
        return retval;
    }

    private static void syncCookies(DefaultHttpClient newClient, WebConversation conv, String hostname) {
		CookieStore cookieStore = newClient.getCookieStore();
		CookieJar cookieJar = conv.getCookieJar();
		syncCookies(cookieStore, cookieJar, hostname);
	}

	public static void syncCookies(CookieStore cookieStore, CookieJar cookieJar, String hostname) {
		List<Cookie> cookies1 = cookieStore.getCookies();
		Set<String> storedNames = new HashSet<String>();
		for(Cookie c : cookies1){
			cookieJar.putCookie(c.getName(), c.getValue());
			storedNames.add(c.getName());
		}
		
		String[] cookies2 = cookieJar.getCookieNames();
		BasicClientCookie2 myCookie;
		for(String name: cookies2)
			if(! storedNames.contains(name)){
				myCookie = new BasicClientCookie2(name, cookieJar.getCookieValue(name));
				myCookie.setDomain(hostname);
				cookieStore.addCookie(myCookie);
			}
	}

	public static WebResponse execute4Download(final AJAXSession session, final AJAXRequest<?> request,
            final String protocol, final String hostname) throws AjaxException, IOException, JSONException {
        final String urlString = protocol + "://" + hostname + request.getServletPath();
        final WebRequest req;
        switch (request.getMethod()) {
        case GET:
            GetMethodWebRequest get = new GetMethodWebRequest(urlString);
            req = get;
            addURLParameter(get, session, request);
            break;
        default:
            throw new AjaxException(AjaxException.Code.InvalidParameter, request.getMethod().name());
        }
        final WebConversation conv = session.getConversation();
        final WebResponse resp;
        // The upload returns a web page that should not be interpreted.
        // final long startRequest = System.currentTimeMillis();
        resp = conv.getResource(req);
        //final long requestDuration = System.currentTimeMillis() - startRequest;
        return resp;
    }

    private static void addURLParameter(GetMethodWebRequest req, AJAXSession session, AJAXRequest<?> request) throws IOException, JSONException {
        if (null != session.getId()) {
            req.setParameter(AJAXServlet.PARAMETER_SESSION, session.getId());
        }
        for (final Parameter param : request.getParameters()) {
            if (!(param instanceof FileParameter)) {
                req.setParameter(param.getName(), param.getValue());
            }
        }
    }

    /*************************************
     *** Rewrite for HttpClient: Start ***
     *************************************/
    
    private static String addQueryParamsToUri(String uri, List<NameValuePair> queryParams){
    	
    	java.util.Collections.sort(queryParams, new Comparator<NameValuePair>(){
			public int compare(NameValuePair o1, NameValuePair o2) {
				return (o1.getName().compareTo(o2.getName()));
			}}); //sorting the query params alphabetically
    	
    	if(uri.contains("?"))
    		uri += "&";
    	else
    		uri += "?";		
    	return uri + URLEncodedUtils.format(queryParams, "UTF-8");
    }
    
    private static List<NameValuePair> getGETParameter(AJAXSession session, AJAXRequest<?> ajaxRequest) throws IOException, JSONException{ //new
    	List<NameValuePair> pairs = new LinkedList<NameValuePair>();
    	
        if (session.getId() != null)
        	pairs.add( new BasicNameValuePair(AJAXServlet.PARAMETER_SESSION, session.getId()));

        for (final Parameter param : ajaxRequest.getParameters()) {
            if (!(param instanceof FileParameter)) {
            	pairs.add( new BasicNameValuePair(param.getName(), param.getValue()));
            }
        }

        return pairs;    	    	
    }
        
    private static void addUPLOADParameter(HttpPost postMethod, AJAXRequest<?> request) throws IOException, JSONException {
    	List<NameValuePair> pairs = new LinkedList<NameValuePair>();
    	MultipartEntity parts = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
    	
        for (final Parameter param : request.getParameters()) {
            if (param instanceof FieldParameter) {
                final FieldParameter fparam = (FieldParameter) param;
                pairs.add( new BasicNameValuePair(fparam.getFieldName(), fparam.getFieldContent()));
            }
            if (param instanceof FileParameter) {
            	FileParameter fparam = (FileParameter) param;
            	InputStream is = fparam.getInputStream();
            	InputStreamBody body = new InputStreamBody(is, fparam.getMimeType());
            	
            	parts.addPart(fparam.getFileName(), body);
            }
        }
        postMethod.setEntity(parts);
        
    }
    
    private static HttpEntity getBodyParameters(AJAXRequest<?> request) throws IOException, JSONException {
    	List<NameValuePair> pairs = new LinkedList<NameValuePair>();

        for (final Parameter param : request.getParameters()) {
            if (param instanceof FieldParameter) {
                final FieldParameter fparam = (FieldParameter) param;
                pairs.add( new BasicNameValuePair(fparam.getFieldName(), fparam.getFieldContent()));
            }
        }
        
        return new UrlEncodedFormEntity(pairs);
    }
    /*************************************
     *** Rewrite for HttpClient: End   ***
     *************************************/

    /**
     * @param strict <code>true</code> to only add URLParameters to the URL. This is needed for the POST request of the login method.
     * Unfortunately breaks this a lot of other tests.
     */
    private static String getURLParameter(AJAXSession session, AJAXRequest<?> request, boolean strict) throws IOException, JSONException {
        final URLParameter parameter = new URLParameter();
        if (null != session.getId()) {
            parameter.setParameter(AJAXServlet.PARAMETER_SESSION, session.getId());
        }
        for (final Parameter param : request.getParameters()) {
            if (!strict && !(param instanceof FileParameter) && !(param instanceof FieldParameter)) {
                parameter.setParameter(param.getName(), param.getValue());
            }
            if (strict && param instanceof com.openexchange.ajax.framework.AJAXRequest.URLParameter) {
                parameter.setParameter(param.getName(), param.getValue());
            }
            // Don't throw error here because field and file parameters are added on POST with method addBodyParameter().
        }
        return parameter.getURLParameters();
    }

    private static InputStream createBody(final Object body) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(body.toString().getBytes(ENCODING));
    }
}
