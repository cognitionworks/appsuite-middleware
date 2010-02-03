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

package com.openexchange.messaging.generic;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import javax.mail.internet.MailDateFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import com.openexchange.mail.mime.utils.MIMEMessageUtility;
import com.openexchange.mail.text.HTMLProcessing;
import com.openexchange.mail.text.parser.HTMLParser;
import com.openexchange.mail.text.parser.handler.HTML2TextHandler;
import com.openexchange.messaging.generic.internal.TimeZoneUtils;

/**
 * {@link Utility} - Utility class for <i>com.openexchange.messaging.generic</i> bundle.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.16
 */
public final class Utility {

    /**
     * Initializes a new {@link Utility}.
     */
    private Utility() {
        super();
    }

    private static final ConcurrentMap<String, Future<MailDateFormat>> MDF_MAP = new ConcurrentHashMap<String, Future<MailDateFormat>>();

    private static final MailDateFormat DEFAULT_MAIL_DATE_FORMAT;

    static {
        DEFAULT_MAIL_DATE_FORMAT = new MailDateFormat();
        DEFAULT_MAIL_DATE_FORMAT.setTimeZone(TimeZoneUtils.getTimeZone("GMT"));
    }

    /**
     * Gets the default {@link MailDateFormat} instance configured with GMT time zone.
     * <p>
     * Note that returned instance of {@link MailDateFormat} is shared, therefore use a surrounding synchronized block to preserve thread
     * safety:
     * 
     * <pre>
     * ...
     * final MailDateFormat mdf = Utility.getDefaultMailDateFormat();
     * synchronized(mdf) {
     *  mdf.format(date);
     * }
     * ...
     * </pre>
     * 
     * @return The default {@link MailDateFormat} instance configured with GMT time zone
     */
    public static MailDateFormat getDefaultMailDateFormat() {
        return DEFAULT_MAIL_DATE_FORMAT;
    }

    /**
     * Gets the {@link MailDateFormat} for specified time zone identifier.
     * <p>
     * Note that returned instance of {@link MailDateFormat} is shared, therefore use a surrounding synchronized block to preserve thread
     * safety:
     * 
     * <pre>
     * ...
     * final MailDateFormat mdf = Utility.getMailDateFormat(timeZoneId);
     * synchronized(mdf) {
     *  mdf.format(date);
     * }
     * ...
     * </pre>
     * 
     * @param timeZoneId The time zone identifier
     * @return The {@link MailDateFormat} for specified time zone identifier
     */
    public static MailDateFormat getMailDateFormat(final String timeZoneId) {
        Future<MailDateFormat> future = MDF_MAP.get(timeZoneId);
        if (null == future) {
            final FutureTask<MailDateFormat> ft = new FutureTask<MailDateFormat>(new Callable<MailDateFormat>() {

                public MailDateFormat call() throws Exception {
                    final MailDateFormat mdf = new MailDateFormat();
                    mdf.setTimeZone(TimeZoneUtils.getTimeZone(timeZoneId));
                    return mdf;
                }
            });
            future = MDF_MAP.putIfAbsent(timeZoneId, ft);
            if (null == future) {
                future = ft;
                ft.run();
            }
        }
        try {
            return future.get();
        } catch (final InterruptedException e) {
            org.apache.commons.logging.LogFactory.getLog(Utility.class).error(e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            org.apache.commons.logging.LogFactory.getLog(Utility.class).error(cause.getMessage(), cause);
            return DEFAULT_MAIL_DATE_FORMAT;
        }
    }

    /**
     * Decodes a string header obtained from ENVELOPE fetch item.
     * 
     * @param headerValue The header value
     * @return The decoded header value
     */
    public static String decodeEnvelopeHeader(final String headerValue) {
        return MIMEMessageUtility.decodeEnvelopeHeader(headerValue);
    }

    /**
     * Decodes a multi-mime-encoded header value using the algorithm specified in RFC 2047, Section 6.1.
     * <p>
     * If the charset-conversion fails for any sequence, an {@link UnsupportedEncodingException} is thrown.
     * <p>
     * If the String is not a RFC 2047 style encoded header, it is returned as-is
     * 
     * @param headerValue The possibly encoded header value
     * @return The possibly decoded header value
     */
    public static String decodeMultiEncodedHeader(final String headerValue) {
        return MIMEMessageUtility.decodeMultiEncodedHeader(headerValue);
    }

    /**
     * Folds a string at linear whitespace so that each line is no longer than 76 characters, if possible. If there are more than 76
     * non-whitespace characters consecutively, the string is folded at the first whitespace after that sequence. The parameter
     * <tt>used</tt> indicates how many characters have been used in the current line; it is usually the length of the header name.
     * <p>
     * Note that line breaks in the string aren't escaped; they probably should be.
     * 
     * @param used The characters used in line so far
     * @param foldMe The string to fold
     * @return The folded string
     */
    public static String fold(final int used, final String foldMe) {
        return MIMEMessageUtility.fold(used, foldMe);
    }

    /**
     * Unfolds a folded header. Any line breaks that aren't escaped and are followed by whitespace are removed.
     * 
     * @param headerLine The header line to unfold
     * @return The unfolded string
     */
    public static String unfold(final String headerLine) {
        return MIMEMessageUtility.unfold(headerLine);
    }

    /**
     * Formats plain text to HTML by escaping HTML special characters e.g. <code>&quot;&lt;&quot;</code> is converted to
     * <code>&quot;&amp;lt;&quot;</code>.
     * <p>
     * This is just a convenience method which invokes <code>{@link #htmlFormat(String, boolean)}</code> with latter parameter set to
     * <code>true</code>.
     * 
     * @param plainText The plain text
     * @return properly escaped HTML content
     * @see #htmlFormat(String, boolean)
     */
    public static String htmlFormat(final String plainText) {
        return HTMLProcessing.htmlFormat(plainText);
    }

    /**
     * Formats plain text to HTML by escaping HTML special characters e.g. <code>&quot;&lt;&quot;</code> is converted to
     * <code>&quot;&amp;lt;&quot;</code>.
     * 
     * @param plainText The plain text
     * @param withQuote Whether to escape quotes (<code>&quot;</code>) or not
     * @return properly escaped HTML content
     */
    public static String htmlFormat(final String plainText, final boolean withQuote) {
        return HTMLProcessing.htmlFormat(plainText, withQuote);
    }

    /**
     * Formats HTML to plain text.
     * 
     * @param htmlContent The HTML content
     * @return The converted plain text
     */
    public static String textFormat(final String htmlContent) {
        if (htmlContent == null || htmlContent.length() == 0) {
            return "";
        }
        final HTML2TextHandler html2textHandler = new HTML2TextHandler(4096, true);
        HTMLParser.parse(getConformHTML(htmlContent, "UTF-8"), html2textHandler.reset());
        return html2textHandler.getText();
    }

    /**
     * Searches for non-HTML links and convert them to valid HTML links.
     * <p>
     * Example: <code>http://www.somewhere.com</code> is converted to
     * <code>&lt;a&nbsp;href=&quot;http://www.somewhere.com&quot;&gt;http://www.somewhere.com&lt;/a&gt;</code>.
     * 
     * @param content The content to search in
     * @return The given content with all non-HTML links converted to valid HTML links
     */
    public static String formatHrefLinks(final String content) {
        return HTMLProcessing.formatHrefLinks(content);
    }

    /**
     * Creates valid HTML from specified HTML content conform to W3C standards.
     * 
     * @param htmlContent The HTML content
     * @param charset The charset parameter
     * @return The HTML content conform to W3C standards
     */
    public static String getConformHTML(final String htmlContent, final String charset) {
        return HTMLProcessing.getConformHTML(htmlContent, charset);
    }

    /**
     * Creates a {@link Document DOM document} from specified XML/HTML string.
     * 
     * @param string The XML/HTML string
     * @return A newly created DOM document or <code>null</code> if given string cannot be transformed to a DOM document
     */
    public static Document createDOMDocument(final String string) {
        return HTMLProcessing.createDOMDocument(string);
    }

    /**
     * Pretty-prints specified XML/HTML string.
     * 
     * @param string The XML/HTML string to pretty-print
     * @return The pretty-printed XML/HTML string
     */
    public static String prettyPrintXML(final String string) {
        return HTMLProcessing.prettyPrintXML(string);
    }

    /**
     * Pretty-prints specified XML/HTML node.
     * 
     * @param node The XML/HTML node pretty-print
     * @return The pretty-printed XML/HTML node
     */
    public static String prettyPrintXML(final Node node) {
        return HTMLProcessing.prettyPrintXML(node);
    }

}
