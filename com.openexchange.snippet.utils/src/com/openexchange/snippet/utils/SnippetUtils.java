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

package com.openexchange.snippet.utils;

import static com.openexchange.java.Strings.isEmpty;
import java.util.Optional;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.exception.OXException;
import com.openexchange.html.HtmlService;
import com.openexchange.snippet.utils.internal.Services;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;

/**
 * {@link SnippetUtils} - Some utility methods for Snippet module.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SnippetUtils {

    /**
     * Initializes a new {@link SnippetUtils}.
     */
    private SnippetUtils() {
        super();
    }

    /**
     * Sanitizes given Snippet content.
     *
     * @param content The content
     * @return The sanitized content
     */
    public static String sanitizeContent(String content) {
        if (isEmpty(content)) {
            return content;
        }

        int s1 = content.indexOf('<');
        if (s1 < 0) {
            return content;
        }

        int s2 = content.indexOf('>');
        if (s2 < 0 || s2 < s1) {
            return content;
        }

        return sanitizeHtmlContent(content);
    }

    /**
     * Sanitizes given Snippet HTML content.
     *
     * @param content The HTML content
     * @return The sanitized HTML content
     */
    public static String sanitizeHtmlContent(String content) {
        HtmlService service = Services.getService(HtmlService.class);
        if (null == service) {
            return content;
        }

        return sanitizeHtmlContent(content, service);
    }

    /**
     * Sanitizes given Snippet HTML content.
     *
     * @param content The HTML content
     * @param service The HTML service to use
     * @return The sanitized HTML content
     */
    public static String sanitizeHtmlContent(String content, HtmlService service) {
        try {
            String retval = service.sanitize(content, null, false, null, null);

            int start = retval.indexOf("<body>");
            if (start >= 0) {
                start += 6;
                int end = retval.indexOf("</body>", start);
                if (end > 0) {
                    retval = retval.substring(start, end).trim();
                }
            }

            return retval;
        } catch (Exception e) {
            // Ignore
            return content;
        }
    }

    /**
     * Parses the content type from miscellaneous JSON data.
     *
     * @param misc The miscellaneous JSON object
     * @return The extracted content type information or <code>"text/plain"</code> as fall-back
     * @throws OXException If content type cannot be extracted
     */
    public static String parseContentTypeFromMisc(final Object misc) throws OXException {
        if (misc instanceof JSONObject) {
            return parseContentTypeFromMisc((JSONObject) misc);
        }

        try {
            return parseContentTypeFromMisc(new JSONObject(misc.toString()));
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_BUILD_ERROR.create(e);
        }
    }

    /**
     * Parses the content type from miscellaneous JSON data.
     *
     * @param misc The miscellaneous JSON object
     * @return The extracted content type information or <code>"text/plain"</code> as fall-back
     * @throws OXException If content type cannot be extracted
     */
    public static String parseContentTypeFromMisc(final String misc) throws OXException {
        try {
            return parseContentTypeFromMisc(new JSONObject(misc));
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_BUILD_ERROR.create(e);
        }
    }

    /**
     * Parses the content type from miscellaneous JSON data.
     *
     * @param misc The miscellaneous JSON object
     * @return The extracted content type information or <code>"text/plain"</code> as fall-back
     */
    public static String parseContentTypeFromMisc(final JSONObject misc) {
        String cts = misc.optString("content-type", null);
        return null == cts ? "text/plain" : cts;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Parses optional content type from miscellaneous JSON data.
     *
     * @param misc The miscellaneous JSON object
     * @return The optional content type information
     * @throws OXException If content type cannot be extracted
     */
    public static Optional<String> parseOptionalContentTypeFromMisc(final Object misc) throws OXException {
        if (misc instanceof JSONObject) {
            return parseOptionalContentTypeFromMisc((JSONObject) misc);
        }

        try {
            return parseOptionalContentTypeFromMisc(new JSONObject(misc.toString()));
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_BUILD_ERROR.create(e);
        }
    }

    /**
     * Parses optional content type from miscellaneous JSON data.
     *
     * @param misc The miscellaneous JSON object
     * @return The optional content type information
     * @throws OXException If content type cannot be extracted
     */
    public static Optional<String> parseOptionalContentTypeFromMisc(final String misc) throws OXException {
        try {
            return parseOptionalContentTypeFromMisc(new JSONObject(misc));
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_BUILD_ERROR.create(e);
        }
    }

    /**
     * Parses optional content type from miscellaneous JSON data.
     *
     * @param misc The miscellaneous JSON object
     * @return The optional content type information
     */
    public static Optional<String> parseOptionalContentTypeFromMisc(final JSONObject misc) {
        String cts = misc.optString("content-type", null);
        return Optional.ofNullable(cts);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the image identifier stored in the <code>"misc"</code> JSON object of the snippet.
     *
     * @param misc The <code>"misc"</code> JSON object
     * @return The image identifier or empty
     * @throws OXException
     */
    public static Optional<String> getImageId(final Object misc) throws OXException {
        if (misc == null) {
            return Optional.empty();
        }

        try {
            JSONObject m = new JSONObject(misc.toString());
            return Optional.ofNullable(m.optString("imageId"));
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_BUILD_ERROR.create(e);
        }
    }

}
