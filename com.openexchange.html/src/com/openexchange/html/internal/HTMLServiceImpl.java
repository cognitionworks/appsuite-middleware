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

package com.openexchange.html.internal;

import gnu.inet.encoding.IDNAException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.Serializer;
import org.htmlcleaner.TagNode;
import org.w3c.tidy.Tidy;
import com.openexchange.config.ConfigurationService;
import com.openexchange.html.HTMLService;
import com.openexchange.html.Range;
import com.openexchange.html.internal.parser.HTMLParser;
import com.openexchange.html.internal.parser.handler.HTML2TextHandler;
import com.openexchange.html.internal.parser.handler.HTMLFilterHandler;
import com.openexchange.html.internal.parser.handler.HTMLImageFilterHandler;
import com.openexchange.html.internal.parser.handler.HTMLURLReplacerHandler;
import com.openexchange.html.services.ServiceRegistry;
import com.openexchange.proxy.ImageContentTypeRestriction;
import com.openexchange.proxy.ProxyException;
import com.openexchange.proxy.ProxyRegistration;
import com.openexchange.proxy.ProxyRegistry;
import com.openexchange.tools.stream.UnsynchronizedByteArrayInputStream;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;

/**
 * {@link HTMLServiceImpl}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class HTMLServiceImpl implements HTMLService {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(HTMLServiceImpl.class);

    private static final String CHARSET_US_ASCII = "US-ASCII";

    private static final Pattern PAT_META_CT = Pattern.compile("<meta[^>]*?http-equiv=\"?content-type\"?[^>]*?>", Pattern.CASE_INSENSITIVE);

    private static final String RPL_CT = "#CT#";

    private static final String HTML_META_TEMPLATE = "\r\n    <meta content=\"" + RPL_CT + "\" http-equiv=\"Content-Type\" />";

    private static final String RPL_CS = "#CS#";

    private static final String CT_TEXT_HTML = "text/html; charset=" + RPL_CS;

    private static final String TAG_E_HEAD = "</head>";

    private static final String TAG_S_HEAD = "<head>";

    /*-
     * Member stuff
     */

    private final Properties tidyConfiguration;

    private final Map<Character, String> htmlCharMap;

    private final Map<String, Character> htmlEntityMap;

    /**
     * Initializes a new {@link HTMLServiceImpl}.
     * 
     * @param tidyConfiguration The jTidy configuration
     * @param htmlCharMap The HTML entity to string map
     * @param htmlEntityMap The string to HTML entity map
     */
    public HTMLServiceImpl(final Properties tidyConfiguration, final Map<Character, String> htmlCharMap, final Map<String, Character> htmlEntityMap) {
        super();
        this.tidyConfiguration = tidyConfiguration;
        this.htmlCharMap = htmlCharMap;
        this.htmlEntityMap = htmlEntityMap;
    }

    private static final Pattern IMG_PATTERN = Pattern.compile("<img[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public String replaceImages(final String content, final String sessionId) {
        if (null == content) {
            return null;
        }
        try {
            final Matcher imgMatcher = IMG_PATTERN.matcher(content);
            if (imgMatcher.find()) {
                /*
                 * Check presence of ProxyRegistry
                 */
                final ProxyRegistry proxyRegistry = ProxyRegistryProvider.getInstance().getProxyRegistry();
                if (null == proxyRegistry) {
                    LOG.warn("Missing ProxyRegistry service. Replacing image URL skipped.");
                    return content;
                }
                /*
                 * Start replacing
                 */
                final StringBuilder sb = new StringBuilder(content.length());
                int lastMatch = 0;
                do {
                    sb.append(content.substring(lastMatch, imgMatcher.start()));
                    final String imgTag = imgMatcher.group();
                    replaceSrcAttribute(imgTag, sessionId, sb, proxyRegistry);
                    lastMatch = imgMatcher.end();
                } while (imgMatcher.find());
                sb.append(content.substring(lastMatch));
                return sb.toString();
            }

        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return content;
    }

    private static final Pattern SRC_PATTERN = Pattern.compile(
        "(?:src=\"([^\"]*)\")|(?:src='([^']*)')|(?:src=[^\"']([^\\s>]*))",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static String replaceSrcAttribute(final String imgTag, final String sessionId, final StringBuilder sb, final ProxyRegistry proxyRegistry) {
        final Matcher srcMatcher = SRC_PATTERN.matcher(imgTag);
        int lastMatch = 0;
        if (srcMatcher.find()) {
            /*
             * 'src' attribute found
             */
            sb.append(imgTag.substring(lastMatch, srcMatcher.start()));
            try {
                /*
                 * Extract URL
                 */
                int group = 1;
                String urlStr = srcMatcher.group(group);
                if (urlStr == null) {
                    urlStr = srcMatcher.group(++group);
                    if (urlStr == null) {
                        urlStr = srcMatcher.group(++group);
                    }
                }
                /*
                 * Check for an inline image
                 */
                if (urlStr.toLowerCase(Locale.ENGLISH).startsWith("cid", 0)) {
                    sb.append(srcMatcher.group());
                } else {
                    /*
                     * Add proxy registration
                     */
                    final URL imageUrl = new URL(urlStr);
                    final URI uri =
                        proxyRegistry.register(new ProxyRegistration(imageUrl, sessionId, ImageContentTypeRestriction.getInstance()));
                    /*
                     * Compose replacement
                     */
                    sb.append("src=\"").append(uri.toString()).append('"');
                }
            } catch (final MalformedURLException e) {
                LOG.warn("Invalid URL found in \"img\" tag: " + imgTag, e);
                sb.append(srcMatcher.group());
            } catch (final ProxyException e) {
                LOG.warn("Proxy registration failed for \"img\" tag: " + imgTag, e);
                sb.append(srcMatcher.group());
            } catch (final Exception e) {
                LOG.warn("URL replacement failed for \"img\" tag: " + imgTag, e);
                sb.append(srcMatcher.group());
            }
            lastMatch = srcMatcher.end();
        }
        sb.append(imgTag.substring(lastMatch));
        return sb.toString();
    }

    public String formatHrefLinks(final String content) {
        try {
            final Matcher m = PATTERN_LINK_WITH_GROUP.matcher(content);
            final StringBuilder targetBuilder = new StringBuilder(content.length());
            final StringBuilder sb = new StringBuilder(256);
            int lastMatch = 0;
            while (m.find()) {
                targetBuilder.append(content.substring(lastMatch, m.start()));
                final String url = m.group(1);
                sb.setLength(0);
                if ((url == null) || (isSrcAttr(content, m.start(1)))) {
                    targetBuilder.append(checkTarget(m.group(), sb));
                } else {
                    appendLink(url, sb);
                    targetBuilder.append(sb.toString());
                }
                lastMatch = m.end();
            }
            targetBuilder.append(content.substring(lastMatch));
            return targetBuilder.toString();
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        } catch (final StackOverflowError error) {
            LOG.error(StackOverflowError.class.getName(), error);
        }
        return content;
    }

    private static final String STR_IMG_SRC = "src=";

    private static boolean isSrcAttr(final String line, final int urlStart) {
        return (urlStart >= 5) && ((STR_IMG_SRC.equalsIgnoreCase(line.substring(urlStart - 5, urlStart - 1))) || (STR_IMG_SRC.equalsIgnoreCase(line.substring(
            urlStart - 4,
            urlStart))));
    }

    private static final Pattern PATTERN_TARGET = Pattern.compile("(<a[^>]*?target=\"?)([^\\s\">]+)(\"?.*</a>)", Pattern.CASE_INSENSITIVE);

    private static final String STR_BLANK = "_blank";

    private static String checkTarget(final String anchorTag, final StringBuilder sb) {
        final Matcher m = PATTERN_TARGET.matcher(anchorTag);
        if (m.matches()) {
            if (!STR_BLANK.equalsIgnoreCase(m.group(2))) {
                return sb.append(m.group(1)).append(STR_BLANK).append(m.group(3)).toString();
            }
            return anchorTag;
        }
        /*
         * No target specified
         */
        final int pos = anchorTag.indexOf('>');
        if (pos == -1) {
            return anchorTag;
        }
        return sb.append(anchorTag.substring(0, pos)).append(" target=\"").append(STR_BLANK).append('"').append(anchorTag.substring(pos)).toString();
    }

    public String formatURLs(final String content, final List<Range> links) {
        try {
            final Matcher m = PATTERN_URL.matcher(content);
            final StringBuilder targetBuilder = new StringBuilder(content.length());
            final StringBuilder sb = new StringBuilder(256);
            int lastMatch = 0;
            // Adding links shift the positions compared to the original mail text. This must be added.
            int shift = 0;
            while (m.find()) {
                final int startOpeningPos = m.start();
                targetBuilder.append(content.substring(lastMatch, startOpeningPos));
                sb.setLength(0);
                appendLink(m.group(), sb);
                targetBuilder.append(sb.toString());
                lastMatch = m.end();
                final int endOpeningPos = sb.indexOf(">");
                final Range range1 = new Range(startOpeningPos + shift, startOpeningPos + endOpeningPos + 1 + shift);
                links.add(range1);
                final int startClosingPos = sb.indexOf("<", endOpeningPos);
                final Range range2 = new Range(startOpeningPos + startClosingPos + shift, startOpeningPos + sb.length() + shift);
                links.add(range2);
                shift += range1.end - range1.start;
                shift += range2.end - range2.start;
            }
            targetBuilder.append(content.substring(lastMatch));
            return targetBuilder.toString();
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        } catch (final StackOverflowError error) {
            LOG.error(StackOverflowError.class.getName(), error);
        }
        return content;
    }

    private static void appendLink(final String url, final StringBuilder builder) {
        try {
            final int mlen = url.length() - 1;
            if ((mlen > 0) && (')' == url.charAt(mlen))) { // Ends with a parenthesis
                /*
                 * Keep starting parenthesis if present
                 */
                if ('(' == url.charAt(0)) { // Starts with a parenthesis
                    appendAnchor(url.substring(1, mlen), builder);
                    builder.append('(');
                } else {
                    appendAnchor(url.substring(0, mlen), builder);
                }
                /*
                 * Append closing parenthesis
                 */
                builder.append(')');
            } else if ((mlen >= 0) && ('(' == url.charAt(0))) { // Starts with a parenthesis, but does not end with a parenthesis
                /*
                 * Append opening parenthesis
                 */
                builder.append('(');
                appendAnchor(url.substring(1), builder);
            } else {
                appendAnchor(url, builder);
            }
        } catch (final Exception e) {
            /*
             * Append as-is
             */
            LOG.warn(e.getMessage(), e);
            builder.append(url);
        }
    }

    private static void appendAnchor(final String url, final StringBuilder builder) throws MalformedURLException, IDNAException {
        builder.append("<a href=\"");
        if (url.startsWith("www") || url.startsWith("news")) {
            builder.append("http://");
        }
        builder.append(checkURL(url)).append("\" target=\"_blank\">").append(url).append("</a>");
    }

    /**
     * Checks if specified URL needs to be converted to its ASCII form.
     * 
     * @param url The URL to check
     * @return The checked URL
     * @throws MalformedURLException If URL is malformed
     * @throws IDNAException If conversion fails
     */
    public static String checkURL(final String url) throws MalformedURLException, IDNAException {
        String urlStr = url;
        /*
         * Get the host part of URL. Ensure scheme is present before creating a java.net.URL instance
         */
        final String host =
            new URL(
                urlStr.startsWith("www.") || urlStr.startsWith("news.") ? new StringBuilder("http://").append(urlStr).toString() : urlStr).getHost();
        if (null != host && !isAscii(host)) {
            final String encodedHost = gnu.inet.encoding.IDNA.toASCII(host);
            urlStr = Pattern.compile(Pattern.quote(host)).matcher(urlStr).replaceFirst(Matcher.quoteReplacement(encodedHost));
        }
        /*
         * Still contains any non-ascii character?
         */
        final char[] chars = urlStr.toCharArray();
        final int len = chars.length;
        StringBuilder tmp = null;
        int lastpos = 0;
        int i;
        for (i = 0; i < len; i++) {
            final char c = chars[i];
            if (c >= 128) {
                if (null == tmp) {
                    tmp = new StringBuilder(len + 16);
                }
                tmp.append(urlStr.substring(lastpos, i)).append('%').append(Integer.toHexString(c).toUpperCase(Locale.ENGLISH));
                lastpos = i + 1;
            }
        }
        /*
         * Return
         */
        if (null == tmp) {
            return urlStr;
        }
        return (lastpos < len) ? tmp.append(urlStr.substring(lastpos)).toString() : tmp.toString();
    }

    /**
     * Checks whether the specified string's characters are ASCII 7 bit
     * 
     * @param s The string to check
     * @return <code>true</code> if string's characters are ASCII 7 bit; otherwise <code>false</code>
     */
    private static boolean isAscii(final String s) {
        final char[] chars = s.toCharArray();
        boolean isAscci = true;
        for (int i = 0; isAscci && (i < chars.length); i++) {
            isAscci &= (chars[i] < 128);
        }
        return isAscci;
    }

    public String filterWhitelist(final String htmlContent) {
        final HTMLFilterHandler handler = new HTMLFilterHandler(this, htmlContent.length());
        HTMLParser.parse(htmlContent, handler);
        return handler.getHTML();
    }

    public String filterWhitelist(final String htmlContent, final String configName) {
        String confName = configName;
        if (!confName.endsWith(".properties")) {
            confName += ".properties";
        }
        final String definition = getConfiguration().getText(confName);
        if (definition == null) {
            // Apparently, the file was not found, so we'll just fall back to the default whitelist
            return filterWhitelist(htmlContent);
        }
        final HTMLFilterHandler handler = new HTMLFilterHandler(this, htmlContent.length(), definition);
        HTMLParser.parse(htmlContent, handler);
        return handler.getHTML();
    }

    protected ConfigurationService getConfiguration() {
        return ServiceRegistry.getInstance().getService(ConfigurationService.class);
    }

    public String filterExternalImages(final String htmlContent, final boolean[] modified) {
        final HTMLImageFilterHandler handler = new HTMLImageFilterHandler(this, htmlContent.length());
        HTMLParser.parse(htmlContent, handler);
        modified[0] |= handler.isImageURLFound();
        return handler.getHTML();
    }

    public String html2text(final String htmlContent, final boolean appendHref) {
        final HTML2TextHandler handler = new HTML2TextHandler(this, htmlContent.length(), appendHref);
        HTMLParser.parse(htmlContent, handler);
        return handler.getText();
    }

    private static final String HTML_BR = "<br />";

    private static final Pattern PATTERN_CRLF = Pattern.compile("\r?\n");

    public String htmlFormat(final String plainText, final boolean withQuote, final List<Range> ignoreRanges) {
        return PATTERN_CRLF.matcher(escape(plainText, withQuote, ignoreRanges)).replaceAll(HTML_BR);
    }

    public String htmlFormat(final String plainText, final boolean withQuote) {
        return PATTERN_CRLF.matcher(escape(plainText, withQuote, Collections.<Range> emptyList())).replaceAll(HTML_BR);
    }

    private String escape(final String s, final boolean withQuote, final List<Range> ignoreRanges) {
        final int len = s.length();
        final StringBuilder sb = new StringBuilder(len);
        /*
         * Escape
         */
        final Set<Integer> ignorePositions;
        if (null == ignoreRanges || ignoreRanges.isEmpty()) {
            ignorePositions = new HashSet<Integer>(0);
        } else {
            ignorePositions = new HashSet<Integer>(ignoreRanges.size() * 16);
            for (final Range ignoreRange : ignoreRanges) {
                final int end = ignoreRange.end;
                for (int i = ignoreRange.start; i < end; i++) {
                    ignorePositions.add(Integer.valueOf(i));
                }
            }
        }
        final char[] chars = s.toCharArray();
        final Map<Character, String> htmlChar2EntityMap = htmlCharMap;
        if (withQuote) {
            for (int i = 0; i < chars.length; i++) {
                final char c = chars[i];
                if (ignorePositions.contains(Integer.valueOf(i))) {
                    sb.append(c);
                } else {
                    final String entity = htmlChar2EntityMap.get(Character.valueOf(c));
                    if (entity == null) {
                        sb.append(c);
                    } else {
                        sb.append('&').append(entity).append(';');
                    }
                }
            }
        } else {
            for (int i = 0; i < chars.length; i++) {
                final char c = chars[i];
                if (ignorePositions.contains(Integer.valueOf(i)) || ('"' == c)) {
                    sb.append(c);
                } else {
                    final String entity = htmlChar2EntityMap.get(Character.valueOf(c));
                    if (entity == null) {
                        sb.append(c);
                    } else {
                        sb.append('&').append(entity).append(';');
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Formats plain text to HTML by escaping HTML special characters e.g. <code>&quot;&lt;&quot;</code> is converted to
     * <code>&quot;&amp;lt;&quot;</code>.
     * <p>
     * This is just a convenience method which invokes <code>{@link #htmlFormat(String, boolean)}</code> with latter parameter set to
     * <code>true</code>.
     * 
     * @param plainText The plain text
     * @return The properly escaped HTML content
     * @see #htmlFormat(String, boolean)
     */
    public String htmlFormat(final String plainText) {
        return htmlFormat(plainText, true);
    }

    private static final String REGEX_URL_SOLE =
        "\\b(?:https?://|ftp://|mailto:|news\\.|www\\.)[-\\p{L}\\p{Sc}0-9+&@#/%?=~_()|!:,.;]*[-\\p{L}\\p{Sc}0-9+&@#/%=~_()|]";

    /**
     * The regular expression to match URLs inside text:<br>
     * <code>\b(?:https?://|ftp://|mailto:|news\\.|www\.)[-\p{L}\p{Sc}0-9+&@#/%?=~_()|!:,.;]*[-\p{L}\p{Sc}0-9+&@#/%=~_()|]</code>
     * <p>
     * Parentheses, if present, are allowed in the URL -- The leading one is <b>not</b> absorbed.
     */
    public static final Pattern PATTERN_URL_SOLE = Pattern.compile(REGEX_URL_SOLE);

    private static final String REGEX_URL = "\\(?" + REGEX_URL_SOLE;

    /**
     * The regular expression to match URLs inside text:<br>
     * <code>\(?\b(?:https?://|ftp://|mailto:|news\\.|www\.)[-\p{L}\p{Sc}0-9+&@#/%?=~_()|!:,.;]*[-\p{L}\p{Sc}0-9+&@#/%=~_()|]</code>
     * <p>
     * Parentheses, if present, are allowed in the URL -- The leading one is absorbed, too.
     * 
     * <pre>
     * String s = matcher.group();
     * int mlen = s.length() - 1;
     * if (mlen &gt; 0 &amp;&amp; '(' == s.charAt(0) &amp;&amp; ')' == s.charAt(mlen)) {
     *     s = s.substring(1, mlen);
     * }
     * </pre>
     */
    public static final Pattern PATTERN_URL = Pattern.compile(REGEX_URL);

    public Pattern getURLPattern() {
        return PATTERN_URL;
    }

    private static final String REGEX_ANCHOR = "<a\\s+href[^>]+>.*?</a>";

    private static final Pattern PATTERN_LINK = Pattern.compile(REGEX_ANCHOR + '|' + REGEX_URL);

    public Pattern getLinkPattern() {
        return PATTERN_LINK;
    }

    private static final Pattern PATTERN_LINK_WITH_GROUP = Pattern.compile(REGEX_ANCHOR + "|(" + REGEX_URL + ')');

    public Pattern getLinkWithGroupPattern() {
        return PATTERN_LINK_WITH_GROUP;
    }

    /**
     * Maps specified HTML entity - e.g. <code>&amp;uuml;</code> - to corresponding UNICODE character.
     * 
     * @param entity The HTML entity
     * @return The corresponding UNICODE character or <code>null</code>
     */
    public Character getHTMLEntity(final String entity) {
        if (null == entity) {
            return null;
        }
        String key = entity;
        if (key.charAt(0) == '&') {
            key = key.substring(1);
        }
        {
            final int lastPos = key.length() - 1;
            if (key.charAt(lastPos) == ';') {
                key = key.substring(0, lastPos);
            }
        }
        final Character tmp = htmlEntityMap.get(key);
        if (tmp != null) {
            return tmp;
        }
        return null;
    }

    private static final Pattern PAT_HTML_ENTITIES = Pattern.compile("&(?:#([0-9]+)|([a-zA-Z]+));");

    /**
     * Replaces all HTML entities occurring in specified HTML content.
     * 
     * @param content The content
     * @return The content with HTML entities replaced
     */
    public String replaceHTMLEntities(final String content) {
        final Matcher m = PAT_HTML_ENTITIES.matcher(content);
        final MatcherReplacer mr = new MatcherReplacer(m, content);
        final StringBuilder sb = new StringBuilder(content.length());
        while (m.find()) {
            final String numEntity = m.group(1);
            if (null == numEntity) {
                final Character entity = getHTMLEntity(m.group());
                if (null != entity) {
                    mr.appendLiteralReplacement(sb, entity.toString());
                }
            } else {
                mr.appendLiteralReplacement(sb, String.valueOf((char) Integer.parseInt(numEntity)));
            }
        }
        mr.appendTail(sb);
        return sb.toString();
    }

    public String prettyPrint(final String htmlContent) {
        try {
            final Tidy tidy = createNewTidyInstance();
            /*
             * Pretty print document
             */
            final ByteArrayOutputStream out = new UnsynchronizedByteArrayOutputStream(htmlContent.length());
            tidy.parseDOM(new UnsynchronizedByteArrayInputStream(htmlContent.getBytes(CHARSET_US_ASCII)), out);
            return new String(out.toByteArray(), CHARSET_US_ASCII);
        } catch (final UnsupportedEncodingException e) {
            LOG.error(e.getMessage(), e);
            return htmlContent;
        }
    }

    private static final Pattern PATTERN_BODY_START = Pattern.compile(Pattern.quote("<body"), Pattern.CASE_INSENSITIVE);

    public String checkBaseTag(final String htmlContent, final boolean externalImagesAllowed) {
        if (null == htmlContent) {
            return htmlContent;
        }
        /*
         * The <base> tag must be between the document's <head> tags. Also, there must be no more than one base element per document.
         */
        final Matcher m1 = PATTERN_BODY_START.matcher(htmlContent);
        return checkBaseTag(htmlContent,externalImagesAllowed,  m1.find() ? m1.start() : htmlContent.length());
    }

    private static final Pattern PATTERN_BASE_TAG = Pattern.compile("<base[^>]*href=\\s*(?:\"|')(\\S*?)(?:\"|')[^>]*>(.*?</base>)?", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static String checkBaseTag(final String htmlContent, final boolean externalImagesAllowed, final int end) {
        final Matcher m = PATTERN_BASE_TAG.matcher(htmlContent);
        if (!m.find() || m.end() >= end) {
            return htmlContent;
        }
        final StringBuilder sb = new StringBuilder(htmlContent.length());
        final MatcherReplacer mr = new MatcherReplacer(m, htmlContent);
        /*
         * Check first found <base> tag
         */
        if (externalImagesAllowed) {
            final String href = m.group(1).trim().toLowerCase(Locale.ENGLISH);
            if (href.startsWith("http://") || href.startsWith("https://")) {
                /*
                 * Base tag contains an absolute URL
                 */
                mr.appendLiteralReplacement(sb, m.group(0));
            } else {
                mr.appendLiteralReplacement(sb, "");
            }
        } else {
            mr.appendLiteralReplacement(sb, "");
        }
        /*
         * Drop any subsequent <base> tag
         */
        while (m.find() && m.end() < end) {
            mr.appendLiteralReplacement(sb, "");
        };
        mr.appendTail(sb);
        return sb.toString();
    }

    public String dropScriptTagsInHeader(final String htmlContent) {
        if (null == htmlContent || htmlContent.indexOf("<script") < 0) {
            return htmlContent;
        }
        final Matcher m1 = PATTERN_BODY_START.matcher(htmlContent);
        return dropScriptTagsInHeader(htmlContent, m1.find() ? m1.start() : htmlContent.length());
    }

    private static final Pattern PATTERN_SCRIPT_TAG = Pattern.compile("<script[^>]*>" + ".*?" + "</script>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static String dropScriptTagsInHeader(final String htmlContent, final int end) {
        final Matcher m = PATTERN_SCRIPT_TAG.matcher(htmlContent);
        if (!m.find() || m.end() >= end) {
            return htmlContent;
        }
        final StringBuilder sb = new StringBuilder(htmlContent.length());
        final MatcherReplacer mr = new MatcherReplacer(m, htmlContent);
        do {
            mr.appendLiteralReplacement(sb, "");
        } while (m.find() && m.end() < end);
        mr.appendTail(sb);
        return sb.toString();
    }
    
    public String getConformHTML(final String htmlContent, final String charset) {
        return getConformHTML(htmlContent, charset, true);
    }

    public String getConformHTML(final String htmlContent, final String charset, final boolean replaceUrls) {
        if (null == htmlContent || 0 == htmlContent.length()) {
            /*
             * Nothing to do...
             */
            return htmlContent;
        }
        /*
         * Validate with JTidy library
         */
        String html = validate(htmlContent);
        /*
         * Check for meta tag in validated HTML content which indicates documents content type. Add if missing.
         */
        final int headTagLen = TAG_S_HEAD.length();
        final int start = html.indexOf(TAG_S_HEAD) + headTagLen;
        if (start >= headTagLen) {
            final int end = html.indexOf(TAG_E_HEAD);
            // final Matcher m = PAT_META_CT.matcher(html.substring(start, end));
            if (!occursWithin(html, start, end, true, "http-equiv=\"content-type\"", "http-equiv=content-type")) {
                final StringBuilder sb = new StringBuilder(html);
                final String cs;
                if (null == charset) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Missing charset. Using fallback \"US-ASCII\" instead.");
                    }
                    cs = CHARSET_US_ASCII;
                } else {
                    cs = charset;
                }
                sb.insert(start, HTML_META_TEMPLATE.replaceFirst(RPL_CT, CT_TEXT_HTML.replaceFirst(RPL_CS, cs)));
                html = sb.toString();
            }
        }
        html = processDownlevelRevealedConditionalComments(html);
        html = removeXHTMLCData(html);
        /*
         * Check URLs
         */
        if (!replaceUrls) {
            return html;
        }
        final HTMLURLReplacerHandler handler = new HTMLURLReplacerHandler(this, html.length());
        HTMLParser.parse(html, handler);
        return handler.getHTML();
    }

    private static boolean occursWithin(final String str, final int start, final int end, final boolean ignorecase, final String... searchStrings) {
        final String source = ignorecase ? str.toLowerCase(Locale.US) : str;
        int index;
        for (final String searchString : searchStrings) {
            final String searchMe = ignorecase ? searchString.toLowerCase(Locale.US) : searchString;
            if (((index = source.indexOf(searchMe, start)) >= start) && ((index + searchMe.length()) < end)) {
                return true;
            }
        }
        return false;
    }

    private static final Pattern PATTERN_XHTML_CDATA;

    private static final Pattern PATTERN_UNQUOTE1;

    private static final Pattern PATTERN_UNQUOTE2;

    private static final Pattern PATTERN_XHTML_COMMENT;

    static {
        final String group1 = RegexUtility.group("<style[^>]*type=\"text/(?:css|javascript)\"[^>]*>\\s*", true);

        final String ignore1 = RegexUtility.concat(RegexUtility.quote("/*<![CDATA[*/"), "\\s*");

        final String commentStart = RegexUtility.group(RegexUtility.OR(RegexUtility.quote("<!--"), RegexUtility.quote("&lt;!--")), false);

        final String commentEnd =
            RegexUtility.concat(RegexUtility.group(RegexUtility.OR(RegexUtility.quote("-->"), RegexUtility.quote("--&gt;")), false), "\\s*");

        final String group2 = RegexUtility.group(RegexUtility.concat(commentStart, ".*?", commentEnd), true);

        final String ignore2 = RegexUtility.concat(RegexUtility.quote("/*]]>*/"), "\\s*");

        final String group3 = RegexUtility.group(RegexUtility.quote("</style>"), true);

        final String regex = RegexUtility.concat(group1, ignore1, group2, ignore2, group3);

        PATTERN_XHTML_CDATA = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        final String commentEnd2 = RegexUtility.group(RegexUtility.OR(RegexUtility.quote("-->"), RegexUtility.quote("--&gt;")), false);

        PATTERN_XHTML_COMMENT = Pattern.compile(RegexUtility.concat(commentStart, ".*?", commentEnd2), Pattern.DOTALL);

        PATTERN_UNQUOTE1 = Pattern.compile(RegexUtility.quote("&lt;!--"), Pattern.CASE_INSENSITIVE);

        PATTERN_UNQUOTE2 = Pattern.compile(RegexUtility.quote("--&gt;"), Pattern.CASE_INSENSITIVE);
    }

    /**
     * Removes unnecessary CDATA from CSS or JavaScript <code>style</code> elements:
     * 
     * <pre>
     * &lt;style type=&quot;text/css&quot;&gt;
     * /*&lt;![CDATA[&#42;/
     * &lt;!--
     *  /* Some Definitions &#42;/
     * --&gt;
     * /*]]&gt;&#42;/
     * &lt;/style&gt;
     * </pre>
     * 
     * is turned to
     * 
     * <pre>
     * &lt;style type=&quot;text/css&quot;&gt;
     * &lt;!--
     *  /* Some Definitions &#42;/
     * --&gt;
     * &lt;/style&gt;
     * </pre>
     * 
     * @param htmlContent The (X)HTML content possibly containing CDATA in CSS or JavaScript <code>style</code> elements
     * @return The (X)HTML content with CDATA removed
     */
    private static String removeXHTMLCData(final String htmlContent) {
        final Matcher m = PATTERN_XHTML_CDATA.matcher(htmlContent);
        if (m.find()) {
            final MatcherReplacer mr = new MatcherReplacer(m, htmlContent);
            final StringBuilder sb = new StringBuilder(htmlContent.length());
            final String endingComment = "-->";
            StringBuilder tmp = null;
            do {
                // Un-quote
                final String match =
                    Matcher.quoteReplacement(PATTERN_UNQUOTE2.matcher(PATTERN_UNQUOTE1.matcher(m.group(2)).replaceAll("<!--")).replaceAll(
                        "-->"));
                // Check for additional HTML comments
                if (PATTERN_XHTML_COMMENT.matcher(m.group(2)).replaceAll("").indexOf(endingComment) == -1) {
                    // No additional HTML comments
                    if (null == tmp) {
                        tmp = new StringBuilder(match.length() + 16);
                    } else {
                        tmp.setLength(0);
                    }
                    mr.appendReplacement(sb, tmp.append("$1").append(match).append("$3").toString());
                } else {
                    // Additional HTML comments
                    if (null == tmp) {
                        tmp = new StringBuilder(match.length() + 16);
                    } else {
                        tmp.setLength(0);
                    }
                    mr.appendReplacement(sb, tmp.append("$1<!--\n").append(match).append("$3").toString());
                }
            } while (m.find());
            mr.appendTail(sb);
            return sb.toString();
        }
        return htmlContent;
    }

    private static final Pattern PATTERN_CC = Pattern.compile("(<!(?:--)?\\[if)([^\\]]+\\]>)(.*?)(<!\\[endif\\](?:--)?>)", Pattern.DOTALL);

    private static final String CC_START_IF = "<!--[if";

    private static final String CC_END_IF = "-->";

    private static final String CC_ENDIF = "<!--<![endif]-->";

    /**
     * Processes detected downlevel-revealed <a href="http://en.wikipedia.org/wiki/Conditional_comment">conditional comments</a> through
     * adding dashes before and after each <code>if</code> statement tag to complete them as a valid HTML comment and leaves center code
     * open to rendering on non-IE browsers:
     * 
     * <pre>
     * &lt;![if !IE]&gt;
     * &lt;link rel=&quot;stylesheet&quot; type=&quot;text/css&quot; href=&quot;non-ie.css&quot;&gt;
     * &lt;![endif]&gt;
     * </pre>
     * 
     * is turned to
     * 
     * <pre>
     * &lt;!--[if !IE]&gt;--&gt;
     * &lt;link rel=&quot;stylesheet&quot; type=&quot;text/css&quot; href=&quot;non-ie.css&quot;&gt;
     * &lt;!--&lt;![endif]--&gt;
     * </pre>
     * 
     * @param htmlContent The HTML content possibly containing downlevel-revealed conditional comments
     * @return The HTML content whose downlevel-revealed conditional comments contain valid HTML for non-IE browsers
     */
    private static String processDownlevelRevealedConditionalComments(final String htmlContent) {
        final Matcher m = PATTERN_CC.matcher(htmlContent);
        if (!m.find()) {
            /*
             * No conditional comments found
             */
            return htmlContent;
        }
        int lastMatch = 0;
        final StringBuilder sb = new StringBuilder(htmlContent.length() + 128);
        do {
            sb.append(htmlContent.substring(lastMatch, m.start()));
            sb.append(CC_START_IF).append(m.group(2));
            final String wrappedContent = m.group(3);
            if (!wrappedContent.startsWith("-->", 0)) {
                sb.append(CC_END_IF);
            }
            sb.append(wrappedContent);
            if (wrappedContent.endsWith("<!--")) {
                sb.append(m.group(4));
            } else {
                sb.append(CC_ENDIF);
            }
            lastMatch = m.end();
        } while (m.find());
        sb.append(htmlContent.substring(lastMatch));
        return sb.toString();
    }

    /**
     * Validates specified HTML content with <a href="http://tidy.sourceforge.net/">tidy html</a> library and falls back using <a
     * href="http://htmlcleaner.sourceforge.net/">HtmlCleaner</a> if any error occurs.
     * 
     * @param htmlContent The HTML content
     * @return The validated HTML content
     */
    private String validate(final String htmlContent) {
        return validate(htmlContent, true);
    }

    /**
     * Validates specified HTML content with <a href="http://tidy.sourceforge.net/">tidy html</a> library and falls back using <a
     * href="http://htmlcleaner.sourceforge.net/">HtmlCleaner</a> if any error occurs.
     * 
     * @param htmlContent The HTML content
     * @return The validated HTML content
     */
    private String validate(final String htmlContent, final boolean forceHtmlCleaner) {
        if (forceHtmlCleaner) {
            return validateWithHtmlCleaner(htmlContent);
        }
        /*
         * Obtain a new Tidy instance
         */
        final Tidy tidy = createNewTidyInstance();
        /*
         * Run tidy, providing a reader and writer
         */
        String validatedHtml;
        try {
            final Writer writer = new UnsynchronizedStringWriter(htmlContent.length());
            tidy.parse(new UnsynchronizedStringReader(htmlContent), writer);
            validatedHtml = writer.toString();
        } catch (final RuntimeException rte) {
            /*
             * Tidy failed horribly...
             */
            LOG.warn("JTidy library failed to pretty-print HTML content. Using HtmlCleaner library as fall-back.", rte);
            validatedHtml = null;
        }
        /*
         * Check Tidy output
         */
        if (null == validatedHtml || 0 == validatedHtml.length()) {
            validatedHtml = validateWithHtmlCleaner(htmlContent);
        }
        return validatedHtml;
    }

    /**
     * The {@link HtmlCleaner} constant which is safe being used by multiple threads as of <a
     * href="http://htmlcleaner.sourceforge.net/javause.php#example2">this example</a>.
     */
    private static final HtmlCleaner HTML_CLEANER;

    /**
     * The {@link Serializer} constant which is safe being used by multiple threads as of <a
     * href="http://htmlcleaner.sourceforge.net/javause.php#example2">this example</a>.
     */
    private static final Serializer SERIALIZER;

    static {
        final CleanerProperties props = new CleanerProperties();
        props.setOmitDoctypeDeclaration(false);
        props.setOmitXmlDeclaration(true);
        props.setPruneTags("script");
        props.setTransSpecialEntitiesToNCR(true);
        props.setTransResCharsToNCR(true);
        props.setRecognizeUnicodeChars(false);
        props.setUseEmptyElementTags(false);
        HTML_CLEANER = new HtmlCleaner(props);
        SERIALIZER = new PrettyXmlSerializer(props, " ");
    }

    private static final String DOCTYPE_DECL = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\r\n\r\n";

    private String validateWithHtmlCleaner(final String htmlContent) {
        try {
            /*
             * Clean...
             */
            final TagNode htmlNode = HTML_CLEANER.clean(htmlContent);
            /*
             * Check for presence of HTML namespace
             */
            if (!htmlNode.hasAttribute("xmlns")) {
                htmlNode.setAttribute("xmlns", "http://www.w3.org/1999/xhtml");
            }
            /*
             * Serialize
             */
            final UnsynchronizedStringWriter writer = new UnsynchronizedStringWriter(htmlContent.length());
            SERIALIZER.write(htmlNode, writer, "UTF-8");
            final StringBuilder builder = writer.getBuffer();
            /*
             * Insert DOCTYPE if absent
             */
            if (builder.indexOf("<!DOCTYPE") < 0) {
                builder.insert(0, DOCTYPE_DECL);
            }
            return builder.toString();
        } catch (final UnsupportedEncodingException e) {
            // Cannot occur
            LOG.error("Unsupported encoding: " + e.getMessage(), e);
            return "";
        } catch (final IOException e) {
            // Cannot occur
            LOG.error("I/O error: " + e.getMessage(), e);
            return "";
        } catch (final RuntimeException rte) {
            /*
             * HtmlCleaner failed horribly...
             */
            LOG.warn("HtmlCleaner library failed to pretty-print HTML content with: " + rte.getMessage(), rte);
            return "";
        }
    }

    private Tidy createNewTidyInstance() {
        final Tidy tidy = new Tidy();
        /*
         * Set desired configuration options using tidy setters
         */
        tidy.setXHTML(true);
        tidy.setConfigurationFromProps(tidyConfiguration);
        tidy.setMakeClean(false);
        tidy.setForceOutput(true);
        tidy.setOutputEncoding(CHARSET_US_ASCII);
        tidy.setTidyMark(false);
        tidy.setXmlOut(true);
        tidy.setNumEntities(true);
        tidy.setDropEmptyParas(false);
        tidy.setDropFontTags(false);
        tidy.setDropProprietaryAttributes(false);
        tidy.setTrimEmptyElements(false);
        /*
         * Suppress tidy outputs
         */
        tidy.setShowErrors(0);
        tidy.setShowWarnings(false);
        tidy.setErrout(TIDY_DUMMY_PRINT_WRITER);
        return tidy;
    }

    private static final PrintWriter TIDY_DUMMY_PRINT_WRITER = new PrintWriter(new Writer() {

        @Override
        public void close() throws IOException {
            // Nothing to do
        }

        @Override
        public void flush() throws IOException {
            // Nothing to do
        }

        @Override
        public void write(final int c) throws IOException {
            // Nothing to do
        }

        @Override
        public void write(final char cbuf[]) throws IOException {
            // Nothing to do
        }

        @Override
        public void write(final String str) throws IOException {
            // Nothing to do
        }

        @Override
        public void write(final String str, final int off, final int len) throws IOException {
            // Nothing to do
        }

        @Override
        public Writer append(final CharSequence csq) throws IOException {
            return this;
        }

        @Override
        public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
            return this;
        }

        @Override
        public Writer append(final char c) throws IOException {
            return this;
        }

        @Override
        public void write(final char[] cbuf, final int off, final int len) throws IOException {
            // Nothing to do
        }
    });

    private static String urlEncode(final String s) {
        try {
            return URLEncoder.encode(s, "ISO-8859-1");
        } catch (final UnsupportedEncodingException e) {
            return s;
        }
    }

    private static String urlDecode(final String s) {
        try {
            return URLDecoder.decode(s, "ISO-8859-1");
        } catch (final UnsupportedEncodingException e) {
            return s;
        }
    }
}
