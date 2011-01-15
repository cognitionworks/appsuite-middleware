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

package com.openexchange.html.internal.parser.handler;

import static com.openexchange.html.internal.HTMLServiceImpl.PATTERN_URL_SOLE;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import com.openexchange.html.HTMLService;
import com.openexchange.html.internal.HTMLServiceImpl;
import com.openexchange.html.internal.parser.HTMLHandler;

/**
 * {@link HTMLURLReplacerHandler} - Replaces any URL containing non-ASCII characters to ASCII using the procedure in RFC3490 section 4.1.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class HTMLURLReplacerHandler implements HTMLHandler {

    private static final String COMMENT_END = "-->";

    private static final String COMMENT_START = "<!--";

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(HTMLURLReplacerHandler.class);

    private static final String CRLF = "\r\n";

    /*-
     * ----------------- Member stuff -----------------
     */

    private final HTMLService htmlService;

    private final StringBuilder htmlBuilder;

    private final StringBuilder attrBuilder;

    private final StringBuilder urlBuilder;

    /**
     * Initializes a new {@link HTMLURLReplacerHandler}.
     * 
     * @param capacity The initial capacity
     */
    public HTMLURLReplacerHandler(final HTMLService htmlService, final int capacity) {
        super();
        this.htmlService = htmlService;
        htmlBuilder = new StringBuilder(capacity);
        attrBuilder = new StringBuilder(128);
        urlBuilder = new StringBuilder(128);
    }

    public void handleXMLDeclaration(final String version, final Boolean standalone, final String encoding) {
        if (null != version) {
            htmlBuilder.append("<?xml version=\"").append(version).append('"');
            if (null != standalone) {
                htmlBuilder.append(" standalone=\"").append(Boolean.TRUE.equals(standalone) ? "yes" : "no").append('"');
            }
            if (null != encoding) {
                htmlBuilder.append(" encoding=\"").append(encoding).append('"');
            }
            htmlBuilder.append("?>").append(CRLF);
        }
    }

    public void handleComment(final String comment) {
        htmlBuilder.append(COMMENT_START).append(comment).append(COMMENT_END);
    }

    public void handleDocDeclaration(final String docDecl) {
        htmlBuilder.append("<!DOCTYPE").append(docDecl).append('>');
    }

    public void handleEndTag(final String tag) {
        htmlBuilder.append("</").append(tag).append('>');
    }

    public void handleError(final String errorMsg) {
        LOG.error(errorMsg);
    }

    public void handleSimpleTag(final String tag, final Map<String, String> attributes) {
        addStartTag(tag, attributes, true);
    }

    public void handleStartTag(final String tag, final Map<String, String> attributes) {
        addStartTag(tag, attributes, false);
    }

    public void handleCDATA(final String text) {
        htmlBuilder.append("<![CDATA[");
        htmlBuilder.append(text);
        htmlBuilder.append("]]>");
    }

    public void handleText(final String text, final boolean ignorable) {
        htmlBuilder.append(text);
    }

    private static final String VAL_START = "=\"";

    /**
     * Adds tag occurring in white list to HTML result.
     * 
     * @param tag The tag to add
     * @param a The tag's attribute set
     * @param simple <code>true</code> to write a simple tag; otherwise <code>false</code>
     */
    private void addStartTag(final String tag, final Map<String, String> a, final boolean simple) {
        attrBuilder.setLength(0);
        for (final Entry<String, String> e : a.entrySet()) {
            final String val = e.getValue();
            final Matcher m = PATTERN_URL_SOLE.matcher(val);
            if (m.matches()) {
                urlBuilder.setLength(0);
                urlBuilder.append(val.substring(0, m.start()));
                replaceURL(urlDecode(m.group()), urlBuilder);
                urlBuilder.append(val.substring(m.end()));
                attrBuilder.append(' ').append(e.getKey()).append(VAL_START).append(urlBuilder.toString()).append('"');
            } else {
                attrBuilder.append(' ').append(e.getKey()).append(VAL_START).append(htmlService.htmlFormat(val, false)).append('"');
            }
        }
        htmlBuilder.append('<').append(tag).append(attrBuilder.toString());
        if (simple) {
            htmlBuilder.append('/');
        }
        htmlBuilder.append('>');
    }

    private static void replaceURL(final String url, final StringBuilder builder) {
        /*
         * Contains any non-ascii character in host part?
         */
        final int restoreLen = builder.length();
        try {
            builder.append(HTMLServiceImpl.checkURL(url));
        } catch (final MalformedURLException e) {
            /*
             * Not a valid URL
             */
            builder.setLength(restoreLen);
            builder.append(url);
        } catch (final Exception e) {
            LOG.warn("URL replacement failed.", e);
            builder.setLength(restoreLen);
            builder.append(url);
        }
    }

    private static String urlDecode(final String s) {
        try {
            return URLDecoder.decode(s, "ISO-8859-1");
        } catch (final UnsupportedEncodingException e) {
            return s;
        }
    }

    /**
     * Gets the filtered HTML content.
     * 
     * @return The filtered HTML content
     */
    public String getHTML() {
        return htmlBuilder.toString();
    }

}
