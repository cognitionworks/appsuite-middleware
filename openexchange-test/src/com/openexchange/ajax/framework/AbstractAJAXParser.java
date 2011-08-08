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

import com.openexchange.exception.OXException;
import java.io.IOException;
import junit.framework.Assert;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.parser.ResponseParser;

/**
 * Abstract implementation of an AJAX response parser. This parser also does
 * some standard check of the response that can be overwritten, if the server
 * does not provide a standard response.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public abstract class AbstractAJAXParser<T extends AbstractAJAXResponse> extends Assert {

    /**
     * Should this parser fail if the response contains an error.
     */
    private final boolean failOnError;
    
    /**
     * Default constructor.
     * @param failOnError <code>true</code> and this parser checks the server
     * response for containing error messages and lets the test fail.
     */
    protected AbstractAJAXParser(boolean failOnError) {
        super();
        this.failOnError = failOnError;
    }

    protected Response getResponse(String body) throws JSONException {
        final Response response = ResponseParser.parse(body);
        assertFalse(response.getErrorMessage(), failOnError && response.hasError());
        return response;
    }

    public String checkResponse(HttpResponse resp) throws ParseException, IOException {
        assertEquals("Response code is not okay.", HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());
        return EntityUtils.toString(resp.getEntity());
    }

    public T parse(String body) throws JSONException {
        final Response response = getResponse(body);
        return createResponse(response);
    }

    /**
     * This method must either return the detailed response object or fail with a JSONException.
     * @param response simple response data object providing some method for handling the general JSON response object.
     * @return the detailed response object corresponding to the request and NEVER <code>null</code>.
     * @throws JSONException if creating the detailed response object fails.
     */
    protected abstract T createResponse(final Response response) throws JSONException;

    /**
     * @return the failOnError
     */
    protected boolean isFailOnError() {
        return failOnError;
    }
}
