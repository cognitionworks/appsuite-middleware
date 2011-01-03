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

package com.openexchange.ajax.parser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.fields.ResponseFields;
import com.openexchange.ajax.fields.ResponseFields.ParsingFields;
import com.openexchange.ajax.fields.ResponseFields.TruncatedFields;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.Component;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.AbstractOXException.Parsing;
import com.openexchange.groupware.AbstractOXException.ProblematicAttribute;
import com.openexchange.groupware.AbstractOXException.Truncated;

/**
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class ResponseParser {

    /**
     * Prevent instantiation.
     */
    private ResponseParser() {
        super();
    }

    /**
     * Deserializes a response into the Response object.
     * @param body JSON response string.
     * @return the parsed object.
     * @throws JSONException if parsing fails.
     */
    public static Response parse(final String body) throws JSONException {
        return parse(new JSONObject(body));
    }

    /**
     * Deserializes a JSON response into the Response object.
     * @param json JSON response.
     * @return the parsed object.
     * @throws JSONException if parsing fails.
     */
    public static Response parse(final JSONObject json) throws JSONException {
        final Response retval = new Response(json);
        parse(retval, json);
        return retval;
    }
    
    public static void parse(final Response response, final JSONObject json) throws JSONException {
        if (json.has(ResponseFields.DATA)) {
            response.setData(json.get(ResponseFields.DATA));
        }
        if (json.has(ResponseFields.TIMESTAMP)) {
            response.setTimestamp(new Date(json.getLong(ResponseFields.TIMESTAMP)));
        }
        final String message = json.optString(ResponseFields.ERROR, null);
        final String code = json.optString(ResponseFields.ERROR_CODE, null);
        if (message != null || code != null) {
            final Component component = parseComponent(code);
            final int number = parseErrorNumber(code);
            final int categoryCode = json.optInt(ResponseFields.ERROR_CATEGORY, -1);
            final Category category;
            if (-1 == categoryCode) {
                category = Category.CODE_ERROR;
            } else {
                category = Category.byCode(categoryCode);
            }
            final AbstractOXException exception = new AbstractOXException(component, category, number, message, null);
            if (Category.WARNING.equals(category)) {
                response.setWarning(exception);
            } else {
                response.setException(exception);
            }
            if (json.has(ResponseFields.ERROR_ID)) {
                exception.overrideExceptionID(json.getString(ResponseFields.ERROR_ID));
            }
            parseErrorMessageArgs(json.optJSONArray(ResponseFields.ERROR_PARAMS), exception);
            parseProblematics(json.optJSONArray(ResponseFields.PROBLEMATIC), exception);
        }
    }

    /**
     * Parses the component part of the error code.
     * 
     * @param code
     *            error code to parse.
     * @return the parsed component or {@link EnumComponent#NONE}.
     */
    private static Component parseComponent(final String code) {
        if (code == null || code.length() == 0) {
            return EnumComponent.NONE;
        }
        final int pos = code.indexOf('-');
        if (pos != -1) {
            final String abbr = code.substring(0, pos);
            final EnumComponent component = EnumComponent.byAbbreviation(abbr);
            if (component != null) {
                return component;
            }
            return new StringComponent(abbr);
        }
        return EnumComponent.NONE;
    }

    /**
     * Parses the error number out of the error code.
     * 
     * @param code
     *            error code to parse.
     * @return the parsed error number or 0.
     */
    private static int parseErrorNumber(final String code) {
        if (code == null || code.length() == 0) {
            return 0;
        }
        final int pos = code.indexOf('-');
        if (pos != -1) {
            try {
                return Integer.parseInt(code.substring(pos + 1));
            } catch (final NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Parses the error message arguments.
     * 
     * @param jArgs
     *            the json array with the error message arguments or
     *            <code>null</code>.
     * @param exception
     *            the error message arguments will be stored in this exception.
     */
    private static void parseErrorMessageArgs(final JSONArray jArgs,
        final AbstractOXException exception) {
        if (null != jArgs) {
            final Object[] args = new Object[jArgs.length()];
            for (int i = 0; i < jArgs.length(); i++) {
                args[i] = jArgs.opt(i);
            }
            exception.setMessageArgs(args);
        }
    }

    private static void parseProblematics(final JSONArray probs, final AbstractOXException exc) throws JSONException {
        if (null == probs) {
            return;
        }
        final List<ProblematicAttribute> problematics = new ArrayList<ProblematicAttribute>();
        for (int i = 0; i < probs.length(); i++) {
            final JSONObject json = probs.getJSONObject(i);
            if (json.has(TruncatedFields.ID)) {
                problematics.add(parseTruncated(json));
            } else if (json.has(ParsingFields.NAME)) {
                problematics.add(parseParsing(json));
            }
        }
        for (final ProblematicAttribute problematic : problematics) {
            exc.addProblematic(problematic);
        }
    }

    private static Truncated parseTruncated(final JSONObject json) throws JSONException {
        final int id = json.getInt(TruncatedFields.ID);
        return new Truncated() {
            public int getId() {
                return id;
            }
            public int getLength() {
                return 0;
            }
            public int getMaxSize() {
                return 0;
            }
        };
    }
    
    private static Parsing parseParsing(final JSONObject json) throws JSONException {
        final String attribute = json.getString(ParsingFields.NAME);
        return new Parsing() {
            public String getAttribute() {
                return attribute;
            }
        };
    }

    public static final class StringComponent implements Component {

        private static final long serialVersionUID = 1159589477110476030L;
        private final String abbr;

        public StringComponent(final String abbr) {
            super();
            this.abbr = abbr;
        }

        public String getAbbreviation() {
            return abbr;
        }
    }
}
