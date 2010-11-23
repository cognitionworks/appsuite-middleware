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

package com.openexchange.ajax.requesthandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONObject;
import com.openexchange.ajax.fields.RequestConstants;
import com.openexchange.ajax.parser.DataParser;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.groupware.upload.impl.UploadEvent;
import com.openexchange.tools.servlet.AjaxException;

/**
 * {@link AJAXRequestData} contains the parameters and the payload of the request.
 * 
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class AJAXRequestData {

    public static interface InputStreamProvider {

        /**
         * Gets this provider's input stream.
         * 
         * @return The input stream
         * @throws IOException If an I/O error occurs
         */
        InputStream getInputStream() throws IOException;
    }

    private final Map<String, String> params;

    private boolean secure;

    private Object data;

    private InputStreamProvider uploadStreamProvider;

    private List<UploadFile> files = new ArrayList<UploadFile>(5);

    private String hostname;

    private String route;

    private UploadEvent uploadEvent;

    /**
     * Initializes a new {@link AJAXRequestData}.
     * 
     * @param json The JSON data
     * @throws AjaxException If an AJAX error occurs
     */
    public AJAXRequestData(final JSONObject json) throws AjaxException {
        this();
        data = DataParser.checkJSONObject(json, RequestConstants.DATA);
    }

    /**
     * Initializes a new {@link AJAXRequestData}.
     */
    public AJAXRequestData() {
        super();
        params = new LinkedHashMap<String, String>();
    }

    /**
     * Puts given name-value-pair into this data's parameters.
     * <p>
     * A <code>null</code> value removes the mapping.
     * 
     * @param name The parameter name
     * @param value The parameter value
     * @throws NullPointerException If name is <code>null</code>
     */
    public void putParameter(final String name, final String value) {
        if (null == name) {
            throw new NullPointerException("name is null");
        }
        if (null == value) {
            params.remove(name);
        } else {
            params.put(name, value);
        }
    }

    /**
     * Gets the value mapped to given parameter name.
     * 
     * @param name The parameter name
     * @return The value mapped to given parameter name or <code>null</code> if not present
     * @throws NullPointerException If name is <code>null</code>
     */
    public String getParameter(final String name) {
        if (null == name) {
            throw new NullPointerException("name is null");
        }
        return params.get(name);
    }

    /**
     * Gets all available parameter names wrapped by an {@link Iterator iterator}.
     * 
     * @return The {@link Iterator iterator} for available parameter names
     */
    public Iterator<String> getParameterNames() {
        return params.keySet().iterator();
    }

    /**
     * Gets an {@link Iterator iterator} for those parameters not matching given parameter names.
     * 
     * @param nonMatchingParameterNames The non-matching parameter names
     * @return An {@link Iterator iterator} for non-matching parameters
     */
    public Iterator<Entry<String, String>> getNonMatchingParameters(final Collection<String> nonMatchingParameterNames) {
        final Map<String, String> clone = new HashMap<String, String>(params);
        clone.keySet().removeAll(nonMatchingParameterNames);
        return clone.entrySet().iterator();
    }

    /**
     * Gets an {@link Iterator iterator} for those parameters matching given parameter names.
     * 
     * @param matchingParameterNames The matching parameter names
     * @return An {@link Iterator iterator} for matching parameters
     */
    public Iterator<Entry<String, String>> getMatchingParameters(final Collection<String> matchingParameterNames) {
        final Map<String, String> clone = new HashMap<String, String>(params);
        clone.keySet().retainAll(matchingParameterNames);
        return clone.entrySet().iterator();
    }

    /**
     * Gets the data object.
     * 
     * @return The data object or <code>null</code> if not available
     */
    public Object getData() {
        return data;
    }

    /**
     * Sets the data object.
     * 
     * @param data The data object to set
     */
    public void setData(final Object data) {
        this.data = data;
    }

    /**
     * Whether this request has a secure connection.
     * 
     * @return <code>true</code> if this request has a secure connection; otherwise <code>false</code>
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * Sets whether this request has a secure connection.
     * 
     * @param secure <code>true</code> if this request has a secure connection; otherwise <code>false</code>
     */
    public void setSecure(final boolean secure) {
        this.secure = secure;
    }

    /**
     * Gets the upload stream. Retrieves the body of the request as binary data as an {@link InputStream}.
     * 
     * @return The upload stream or <code>null</code> if not available
     * @throws IOException If an I/O error occurs
     */
    public InputStream getUploadStream() throws IOException {
        return uploadStreamProvider.getInputStream();
    }

    /**
     * Sets the upload stream provider
     * 
     * @param uploadStream The upload stream provider to set
     */
    public void setUploadStreamProvider(final InputStreamProvider uploadStreamProvider) {
        this.uploadStreamProvider = uploadStreamProvider;
    }

    /**
     * Computes a list of missing parameters from a list of mandatory parameters. The typical idiom to use this would be:
     * 
     * <pre>
     * List&lt;String&gt; missingParameters = requestData.getMissingParameters(&quot;param1&quot;, &quot;param2&quot;, &quot;param3&quot;);
     * if (!missingParameters.isEmpty()) {
     *     // Throw AbstractOXException
     * }
     * </pre>
     * 
     * @param mandatoryParameters The mandatory parameters expected.
     * @return A list of missing parameter names
     */
    public List<String> getMissingParameters(final String... mandatoryParameters) {
        final List<String> missing = new ArrayList<String>(mandatoryParameters.length);
        for (final String paramName : mandatoryParameters) {
            if (!params.containsKey(paramName)) {
                missing.add(paramName);
            }
        }
        return missing;
    }

    /**
     * Find out whether this request contains an uploaded file. Note that this is only possible via a servlet interface and
     * not via the multiple module.
     * @return true if one or more files were uploaded, false otherwise.
     */
    public boolean hasUploads() {
        return !files.isEmpty();
    }
    
    /**
     * Retrieve file uploads.
     * @return A list of file uploads.
     */
    public List<UploadFile> getFiles() {
        return Collections.unmodifiableList(files);
    }
    
    /**
     * Retrieve a file with a given form name.
     * @param name The name of the form field that include the file
     * @return The file, or null if no file field of this name was found
     */
    public UploadFile getFile(String name) {
        for (UploadFile file : files) {
            if(file.getFieldName().equals(name)) {
                return file;
            }
        }
        return null;
    }
    
    public void addFile(UploadFile file) {
        files.add(file);
    }
    
    /**
     * Constructs a URL to this server, injecting the hostname and optionally the jvm route.
     * @param protocol The protocol to use (http or https). If <code>null</code>, defaults to the protocol used for this request.
     * @param path The path on the server. If <code>null</code> no path is inserted
     * @param withRoute Whether to include the jvm route in the server URL or not 
     * @param query The query string. If <code>null</code> no query is included
     * @return A string builder with the URL so far, ready for meddling.
     */
    public StringBuilder constructURL(String protocol, String path, boolean withRoute, String query) {
        StringBuilder url = new StringBuilder();
        if(protocol == null) {
            protocol = isSecure() ? "https://" : "http://";
        }
        url.append(protocol);
        if(!protocol.endsWith("://")) {
            url.append("://");
        }
        
        url.append(hostname);
        if(path != null) {
            if(!path.startsWith("/")) {
                url.append('/');
            }
            url.append(path);
        }
        if(withRoute) {
            url.append(";jsessionid=12345.").append(route);
        }
        if(query != null) {
            if(!query.startsWith("?")) {
                url.append('?');
            }
            url.append(query);
        }
        return url;
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    
    public String getRoute() {
        return route;
    }
    
    public void setRoute(String route) {
        this.route = route;
    }

    public void setUploadEvent(UploadEvent upload) {
        this.uploadEvent = upload;
    }
    
    public UploadEvent getUploadEvent() {
        return uploadEvent;
    }

}
