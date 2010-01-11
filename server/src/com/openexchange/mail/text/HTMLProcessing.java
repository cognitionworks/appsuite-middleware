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

package com.openexchange.mail.text;

import static com.openexchange.mail.utils.MailFolderUtility.prepareFullname;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.tidy.Tidy;
import com.openexchange.configuration.SystemConfig;
import com.openexchange.conversion.DataArguments;
import com.openexchange.image.ImageService;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.conversion.InlineImageDataSource;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.text.parser.HTMLParser;
import com.openexchange.mail.text.parser.handler.HTMLFilterHandler;
import com.openexchange.mail.text.parser.handler.HTMLImageFilterHandler;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.utils.DisplayMode;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.UnsynchronizedStringWriter;
import com.openexchange.tools.regex.MatcherReplacer;
import com.openexchange.tools.regex.RegexUtility;
import com.openexchange.tools.stream.UnsynchronizedByteArrayInputStream;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;

/**
 * {@link HTMLProcessing} - Various methods for HTML processing.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class HTMLProcessing {

    private static final String CHARSET_US_ASCII = "US-ASCII";

    /**
     * Performs all the formatting for text content for a proper display according to specified user's mail settings.
     * 
     * @param content The plain text content
     * @param usm The settings used for formatting content
     * @param mode The display mode
     * @see #formatContentForDisplay(String, String, boolean, Session, MailPath, UserSettingMail, boolean[], DisplayMode)
     * @return The formatted content
     */
    public static String formatTextForDisplay(final String content, final UserSettingMail usm, final DisplayMode mode) {
        return formatContentForDisplay(content, null, false, null, null, usm, null, mode);
    }

    /**
     * Performs all the formatting for HTML content for a proper display according to specified user's mail settings.
     * 
     * @param content The HTML content
     * @param charset The character encoding
     * @param session The session
     * @param mailPath The message's unique path in mailbox
     * @param usm The settings used for formatting content
     * @param modified A <code>boolean</code> array with length <code>1</code> to store modified status of external images filter
     * @param mode The display mode
     * @see #formatContentForDisplay(String, String, boolean, Session, MailPath, UserSettingMail, boolean[], DisplayMode)
     * @return The formatted content
     */
    public static String formatHTMLForDisplay(final String content, final String charset, final Session session, final MailPath mailPath, final UserSettingMail usm, final boolean[] modified, final DisplayMode mode) {
        return formatContentForDisplay(content, charset, true, session, mailPath, usm, modified, mode);
    }

    /**
     * Performs all the formatting for both text and HTML content for a proper display according to specified user's mail settings.
     * <p>
     * If content is <b>plain text</b>:<br>
     * <ol>
     * <li>Plain text content is converted to valid HTML if at least {@link DisplayMode#MODIFYABLE} is given</li>
     * <li>If enabled by settings simple quotes are turned to colored block quotes if {@link DisplayMode#DISPLAY} is given</li>
     * <li>HTML links and URLs found in content are going to be prepared for proper display if {@link DisplayMode#DISPLAY} is given</li>
     * </ol>
     * If content is <b>HTML</b>:<br>
     * <ol>
     * <li>Both inline and non-inline images found in HTML content are prepared according to settings if {@link DisplayMode#DISPLAY} is
     * given</li>
     * </ol>
     * 
     * @param content The content
     * @param charset The character encoding (only needed by HTML content; may be <code>null</code> on plain text)
     * @param isHtml <code>true</code> if content is of type <code>text/html</code>; otherwise <code>false</code>
     * @param session The session
     * @param mailPath The message's unique path in mailbox
     * @param usm The settings used for formatting content
     * @param modified A <code>boolean</code> array with length <code>1</code> to store modified status of external images filter (only
     *            needed by HTML content; may be <code>null</code> on plain text)
     * @param mode The display mode
     * @return The formatted content
     */
    public static String formatContentForDisplay(final String content, final String charset, final boolean isHtml, final Session session, final MailPath mailPath, final UserSettingMail usm, final boolean[] modified, final DisplayMode mode) {
        String retval = null;
        if (isHtml) {
            retval = getConformHTML(content, charset == null ? CHARSET_US_ASCII : charset);
            if (DisplayMode.MODIFYABLE.isIncluded(mode) && usm.isDisplayHtmlInlineContent()) {
                /*
                 * Filter according to white-list
                 */
                retval = filterWhitelist(retval);
                if (DisplayMode.DISPLAY.equals(mode) && !usm.isAllowHTMLImages()) {
                    retval = filterExternalImages(retval, modified);
                }
                /*
                 * Filter inlined images
                 */
                if (mailPath != null && session != null) {
                    retval = filterInlineImages(retval, session, mailPath);
                }
            }
            // if (DisplayMode.DISPLAY.equals(mode) && usm.isDisplayHtmlInlineContent()) {
            // if (!usm.isAllowHTMLImages()) {
            // retval = filterExternalImages(retval, modified);
            // }
            // if (mailPath != null && session != null) {
            // retval = filterInlineImages(retval, session, mailPath);
            // }
            // }
        } else {
            retval = content;
            if (DisplayMode.MODIFYABLE.isIncluded(mode)) {
                if (DisplayMode.DISPLAY.equals(mode)) {
                    retval = htmlFormat(retval, true, getHrefPositions(retval));
                    if (usm.isUseColorQuote()) {
                        retval = replaceHTMLSimpleQuotesForDisplay(retval);
                    }
                    retval = formatHrefLinks(retval);
                } else {
                    retval = htmlFormat(retval);
                }
            }
        }
        return retval;
    }

    /**
     * The regular expression to match links inside both plain text and HTML content.
     * <p>
     * <b>WARNING</b>: May throw a {@link StackOverflowError} if a matched link is too large. Usages should handle this case.
     */
    public static final Pattern PATTERN_HREF =
        Pattern.compile(
            "<a\\s+href[^>]+>.*?</a>|((?:https?://|ftp://|mailto:|news\\.|www\\.)(?:[-\\p{L}0-9+@#/%?=~_|!:,.;]|&amp;|&(?![\\p{L}_0-9]+;))*(?:[-\\p{L}0-9+@#/%=~_|]|&amp;|&(?![\\p{L}_0-9]+;)))",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static int[][] getHrefPositions(final String content) {
        try {
            final Matcher m = PATTERN_HREF.matcher(content);
            if (m.find()) {
                final List<int[]> positions = new ArrayList<int[]>();
                do {
                    final String nonHtmlLink = m.group(1);
                    if (null != nonHtmlLink) {
                        positions.add(new int[] { m.start(1), m.end(1) });
                    }
                } while (m.find());
                return positions.toArray(new int[positions.size()][]);
            }
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        } catch (final StackOverflowError error) {
            LOG.error(StackOverflowError.class.getName(), error);
        }
        return new int[0][];
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
        try {
            final Matcher m = PATTERN_HREF.matcher(content);
            final MatcherReplacer mr = new MatcherReplacer(m, content);
            final StringBuilder sb = new StringBuilder(content.length());
            final StringBuilder tmp = new StringBuilder(256);
            while (m.find()) {
                final String nonHtmlLink = m.group(1);
                if ((nonHtmlLink == null) || (isSrcAttr(content, m.start(1)))) {
                    mr.appendLiteralReplacement(sb, checkTarget(m.group()));
                } else {
                    tmp.setLength(0);
                    mr.appendReplacement(sb, tmp.append("<a href=\"").append(
                        (nonHtmlLink.startsWith("www") || nonHtmlLink.startsWith("news") ? "http://" : "")).append(
                        "$1\" target=\"_blank\">$1</a>").toString());
                }
            }
            mr.appendTail(sb);
            return sb.toString();
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        } catch (final StackOverflowError error) {
            LOG.error(StackOverflowError.class.getName(), error);
        }
        return content;
    }

    private static final Pattern PATTERN_TARGET = Pattern.compile("(<a[^>]*?target=\"?)([^\\s\">]+)(\"?.*</a>)", Pattern.CASE_INSENSITIVE);

    private static final String STR_BLANK = "_blank";

    private static String checkTarget(final String anchorTag) {
        final Matcher m = PATTERN_TARGET.matcher(anchorTag);
        if (m.matches()) {
            if (!STR_BLANK.equalsIgnoreCase(m.group(2))) {
                final StringBuilder sb = new StringBuilder(128);
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
        final StringBuilder sb = new StringBuilder(anchorTag.length() + 16);
        return sb.append(anchorTag.substring(0, pos)).append(" target=\"").append(STR_BLANK).append('"').append(anchorTag.substring(pos)).toString();
    }

    private static final String STR_IMG_SRC = "src=";

    private static boolean isSrcAttr(final String line, final int urlStart) {
        return (urlStart >= 5) && ((STR_IMG_SRC.equalsIgnoreCase(line.substring(urlStart - 5, urlStart - 1))) || (STR_IMG_SRC.equalsIgnoreCase(line.substring(
            urlStart - 4,
            urlStart))));
    }

    private static final String RPL_CT = "#CT#";

    private static final String HTML_META_TEMPLATE = "\r\n    <meta content=\"" + RPL_CT + "\" http-equiv=\"Content-Type\" />";

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(HTMLProcessing.class);

    private static final Pattern PAT_META_CT = Pattern.compile("<meta[^>]*?http-equiv=\"?content-type\"?[^>]*?>", Pattern.CASE_INSENSITIVE);

    private static final String RPL_CS = "#CS#";

    private static final String CT_TEXT_HTML = "text/html; charset=" + RPL_CS;

    private static final String TAG_E_HEAD = "</head>";

    private static final String TAG_S_HEAD = "<head>";

    /**
     * Creates valid HTML from specified HTML content conform to W3C standards.
     * 
     * @param htmlContent The HTML content
     * @param contentType The corresponding content type (including charset parameter)
     * @return The HTML content conform to W3C standards
     */
    public static String getConformHTML(final String htmlContent, final ContentType contentType) {
        return getConformHTML(htmlContent, contentType.getCharsetParameter());
    }

    /**
     * Creates valid HTML from specified HTML content conform to W3C standards.
     * 
     * @param htmlContent The HTML content
     * @param charset The charset parameter
     * @return The HTML content conform to W3C standards
     */
    public static String getConformHTML(final String htmlContent, final String charset) {
        if ((htmlContent == null)) {
            /*
             * Nothing to do...
             */
            return htmlContent;
        }
        /*
         * Validate with JTidy library
         */
        String html;
        String cs = charset;
        if (null == cs) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Missing charset. Using fallback \"US-ASCII\" instead.");
            }
            cs = CHARSET_US_ASCII;
        }
        html = validate(htmlContent);
        /*
         * Check for meta tag in validated html content which indicates documents content type. Add if missing.
         */
        final int headTagLen = TAG_S_HEAD.length();
        final int start = html.indexOf(TAG_S_HEAD) + headTagLen;
        if (start >= headTagLen) {
            final Matcher m = PAT_META_CT.matcher(html.substring(start, html.indexOf(TAG_E_HEAD)));
            if (!m.find()) {
                final StringBuilder sb = new StringBuilder(html);
                sb.insert(start, HTML_META_TEMPLATE.replaceFirst(RPL_CT, CT_TEXT_HTML.replaceFirst(RPL_CS, cs)));
                html = sb.toString();
            }
        }
        html = processDownlevelRevealedConditionalComments(html);
        return removeXHTMLCData(html);
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
                final String match = PATTERN_UNQUOTE2.matcher(PATTERN_UNQUOTE1.matcher(m.group(2)).replaceAll("<!--")).replaceAll("-->");
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
            sb.append(CC_START_IF).append(m.group(2)).append(CC_END_IF);
            sb.append(m.group(3));
            sb.append(CC_ENDIF);
            lastMatch = m.end();
        } while (m.find());
        sb.append(htmlContent.substring(lastMatch));
        return sb.toString();
    }

    /**
     * Gets the messages used by JTidy as an input stream.
     * 
     * @return The messages used by JTidy as an input stream
     * @throws IOException If input stream cannot be generated
     */
    public static InputStream getTidyMessages() throws IOException {
        final String tidyMessagesFilename = SystemConfig.getProperty(SystemConfig.Property.TidyMessages);
        if (null == tidyMessagesFilename) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Using default JTidy messages");
            }
            final StringBuilder tidyMsgs = new StringBuilder(4096);
            tidyMsgs.append("anchor_not_unique={0} Anchor \"{1}\" already defined\n");
            tidyMsgs.append("apos_undefined=Named Entity &apos; only defined in XML/XHTML\n");
            tidyMsgs.append("attr_value_not_lcase={0} attribute value \"{1}\" for \"{2}\" must be lower case for XHTML\n");
            tidyMsgs.append("# to be translated\n");
            tidyMsgs.append("backslash_in_uri={0} URI reference contains backslash. Typo?\n");
            tidyMsgs.append("bad_argument=Warning - missing or malformed argument \"{1}\" for option \"{0}\"\n");
            tidyMsgs.append("bad_attribute_value={0} attribute \"{1}\" has invalid value \"{2}\"\n");
            tidyMsgs.append("bad_cdata_content='<' + '/' + letter not allowed here\n");
            tidyMsgs.append("bad_comment_chars=expecting -- or >\n");
            tidyMsgs.append("bad_tree=Panic - tree has lost its integrity\n");
            tidyMsgs.append("bad_xml_comment=XML comments can't contain --\n");
            tidyMsgs.append("badaccess_frames=Pages designed using frames presents problems for\\u000apeople who are either blind or using a browser that\\u000adoesn't support frames. A frames-based page should always\\u000ainclude an alternative layout inside a NOFRAMES element.\n");
            tidyMsgs.append("badaccess_missing_image_alt=The alt attribute should be used to give a short description\\u000aof an image; longer descriptions should be given with the\\u000alongdesc attribute which takes a URL linked to the description.\\u000aThese measures are needed for people using non-graphical browsers.\n");
            tidyMsgs.append("badaccess_missing_image_map=Use client-side image maps in preference to server-side image\\u000amaps as the latter are inaccessible to people using non-\\u000agraphical browsers. In addition, client-side maps are easier\\u000ato set up and provide immediate feedback to users.\n");
            tidyMsgs.append("badaccess_missing_link_alt=For hypertext links defined using a client-side image map, you\\u000aneed to use the alt attribute to provide a textual description\\u000aof the link for people using non-graphical browsers.\n");
            tidyMsgs.append("badaccess_missing_summary=The table summary attribute should be used to describe\\u000athe table structure. It is very helpful for people using\\u000anon-visual browsers. The scope and headers attributes for\\u000atable cells are useful for specifying which headers apply\\u000ato each table cell, enabling non-visual browsers to provide\\u000aa meaningful context for each cell.\n");
            tidyMsgs.append("badaccess_summary=For further advice on how to make your pages accessible\\u000asee \"{0}\". You may also want to try\\u000a\"http://www.cast.org/bobby/\" which is a free Web-based\\u000aservice for checking URLs for accessibility.\n");
            tidyMsgs.append("badchars_summary=Characters codes for the Microsoft Windows fonts in the range\\u000a128 - 159 may not be recognized on other platforms. You are\\u000ainstead recommended to use named entities, e.g. &trade; rather\\u000athan Windows character code 153 (0x2122 in Unicode). Note that\\u000aas of February 1998 few browsers support the new entities.\"\n");
            tidyMsgs.append("badform_summary=You may need to move one or both of the <form> and </form>\\u000atags. HTML elements should be properly nested and form elements\\u000aare no exception. For instance you should not place the <form>\\u000ain one table cell and the </form> in another. If the <form> is\\u000aplaced before a table, the </form> cannot be placed inside the\\u000atable! Note that one form can't be nested inside another!\n");
            tidyMsgs.append("badlayout_using_body=You are recommended to use CSS to specify page and link colors\n");
            tidyMsgs.append("badlayout_using_font=You are recommended to use CSS to specify the font and\\u000aproperties such as its size and color. This will reduce\\u000athe size of HTML files and make them easier to maintain\\u000acompared with using <FONT> elements.\n");
            tidyMsgs.append("badlayout_using_layer=The Cascading Style Sheets (CSS) Positioning mechanism\\u000ais recommended in preference to the proprietary <LAYER>\\u000aelement due to limited vendor support for LAYER.\n");
            tidyMsgs.append("badlayout_using_nobr=You are recommended to use CSS to control line wrapping.\\u000aUse \"white-space: nowrap\" to inhibit wrapping in place\\u000aof inserting <NOBR>...</NOBR> into the markup.\n");
            tidyMsgs.append("badlayout_using_spacer=You are recommended to use CSS for controlling white\\u000aspace (e.g. for indentation, margins and line spacing).\\u000aThe proprietary <SPACER> element has limited vendor support.\n");
            tidyMsgs.append("cant_be_nested={0} can''t be nested\n");
            tidyMsgs.append("coerce_to_endtag=<{0}> is probably intended as </{0}>\n");
            tidyMsgs.append("content_after_body=content occurs after end of body\n");
            tidyMsgs.append("discarding_unexpected=discarding unexpected {0}\n");
            tidyMsgs.append("doctype_after_tags=<!DOCTYPE> isn't allowed after elements\n");
            tidyMsgs.append("doctype_given={0}: Doctype given is \"{1}\"\n");
            tidyMsgs.append("dtype_not_upper_case=SYSTEM, PUBLIC, W3C, DTD, EN must be upper case\n");
            tidyMsgs.append("duplicate_frameset=repeated FRAMESET element\n");
            tidyMsgs.append("element_not_empty={0} element not empty or not closed\n");
            tidyMsgs.append("emacs_format={0}:{1,number}:{2,number}:\n");
            tidyMsgs.append("encoding_mismatch=specified input encoding ({0}) does not match actual input encoding ({1})\n");
            tidyMsgs.append("entity_in_id=no entities allowed in id attribute, discarding \"&\"\n");
            tidyMsgs.append("error=Error: \n");
            tidyMsgs.append("escaped_illegal_uri={0} escaping malformed URI reference\n");
            tidyMsgs.append("expected_equalsign={0} unexpected '=', expected attribute name\n");
            tidyMsgs.append("fixed_backslash={0} converting backslash in URI to slash\n");
            tidyMsgs.append("forced_end_anchor=Warning: <a> is probably intended as </a>\n");
            tidyMsgs.append("general_info=To learn more about JTidy see http://jtidy.sourceforge.net\\u000aPlease report bugs at http://sourceforge.net/tracker/?group_id=13153&atid=113153\\u000aHTML & CSS specifications are available from http://www.w3.org/\\u000aLobby your company to join W3C, see http://www.w3.org/Consortium\n");
            tidyMsgs.append("hello_message=Tidy (vers {0, date}) Parsing \"{1}\"\n");
            tidyMsgs.append("help_text={0} [option...] [file...]\\u000aUtility to clean up and pretty print HTML/XHTML/XML\\u000asee http://www.w3.org/People/Raggett/tidy/\\u000a\\u000aOptions for JTidy released on {1, date}\\u000aProcessing directives\\u000a---------------------\\u000a  -indent  or -i    to indent element content\\u000a  -omit    or -o    to omit optional end tags\\u000a  -wrap <column>    to wrap text at the specified <column> (default is 68)\\u000a  -upper   or -u    to force tags to upper case (default is lower case)\\u000a  -clean   or -c    to replace FONT, NOBR and CENTER tags by CSS\\u000a  -bare    or -b    to strip out smart quotes and em dashes, etc.\\u000a  -numeric or -n    to output numeric rather than named entities\\u000a  -errors  or -e    to only show errors\\u000a  -quiet   or -q    to suppress nonessential output\\u000a  -xml              to specify the input is well formed XML\\u000a  -asxml            to convert HTML to well formed XHTML\\u000a  -asxhtml          to convert HTML to well formed XHTML\\u000a  -ashtml           to force XHTML to well formed HTML\\u000a  -slides           to burst into slides on H2 elements\\u000a\\u000aCharacter encodings\\u000a-------------------\\u000a  -raw              to output values above 127 without conversion to entities\\u000a  -ascii            to use US-ASCII for output, ISO-8859-1 for input\\u000a  -latin1           to use ISO-8859-1 for both input and output\\u000a  -iso2022          to use ISO-2022 for both input and output\\u000a  -utf8             to use UTF-8 for both input and output\\u000a  -mac              to use MacRoman for input, US-ASCII for output\\u000a  -utf16le          to use UTF-16LE for both input and output\\u000a  -utf16be          to use UTF-16BE for both input and output\\u000a  -utf16            to use UTF-16 for both input and output\\u000a  -win1252          to use Windows-1252 for input, US-ASCII for output\\u000a  -big5             to use Big5 for both input and output\\u000a  -shiftjis         to use Shift_JIS for both input and output\\u000a  -language <lang>  to set the two-letter language code <lang> (for future use)\\u000a\\u000aFile manipulation\\u000a-----------------\\u000a  -config <file>    to set configuration options from the specified <file>\\u000a  -f      <file>    to write errors to the specified <file>\\u000a  -modify or -m     to modify the original input files\\u000a\\u000aMiscellaneous\\u000a-------------\\u000a  -version  or -v   to show the version of Tidy\\u000a  -help, -h or -?   to list the command line options\\u000a  -help-config      to list all configuration options\\u000a  -show-config      to list the current configuration settings\\u000a\\u000aYou can also use --blah for any configuration option blah\\u000a\\u000aInput/Output default to stdin/stdout respectively\\u000aSingle letter options apart from -f may be combined\\u000aas in:  tidy -f errs.txt -imu foo.html\\u000aFor further info on HTML see http://www.w3.org/MarkUp\n");
            tidyMsgs.append("id_name_mismatch={0} id and name attribute value mismatch\n");
            tidyMsgs.append("illegal_char=Warning: replacing illegal character code {0,number}\n");
            tidyMsgs.append("illegal_nesting={0} shouldn''t be nested\n");
            tidyMsgs.append("illegal_uri_reference={0} improperly escaped URI reference\n");
            tidyMsgs.append("inconsistent_namespace=html namespace doesn't match content\n");
            tidyMsgs.append("inconsistent_version=html doctype doesn't match content\n");
            tidyMsgs.append("inserting_tag=inserting implicit <{0}>\n");
            tidyMsgs.append("invalid_char={0,choice,0#replacing|1#discarding} invalid character code {1}\n");
            tidyMsgs.append("invalid_ncr={0,choice,0#replacing|1#discarding} invalid numeric character reference {1}\n");
            tidyMsgs.append("invalid_sgml_chars_summary=Character codes 128 to 159 (U+0080 to U+009F) are not allowed in HTML;\\u000aeven if they were, they would likely be unprintable control characters.\\u000aTidy assumed you wanted to refer to a character with the same byte value in the \\u000a{0,choice,0#specified|1#Windows-1252|2#MacRoman} encoding and replaced that reference with the Unicode equivalent.\n");
            tidyMsgs.append("invalid_utf16={0,choice,0#replacing|1#discarding} invalid UTF-16 surrogate pair (char. code {1})\n");
            tidyMsgs.append("invalid_utf16_summary=Character codes for UTF-16 must be in the range: U+0000 to U+10FFFF.\\u000aThe definition of UTF-16 in Annex C of ISO/IEC 10646-1:2000 does not allow the\\u000amapping of unpaired surrogates. For more information please refer to\\u000ahttp://www.unicode.org/unicode and http://www.cl.cam.ac.uk/~mgk25/unicode.html\n");
            tidyMsgs.append("invalid_utf8={0,choice,0#replacing|1#discarding} invalid UTF-8 bytes (char. code {1})\n");
            tidyMsgs.append("invalid_utf8_summary=Character codes for UTF-8 must be in the range: U+0000 to U+10FFFF.\\u000aThe definition of UTF-8 in Annex D of ISO/IEC 10646-1:2000 also\\u000aallows for the use of five- and six-byte sequences to encode\\u000acharacters that are outside the range of the Unicode character set;\\u000athose five- and six-byte sequences are illegal for the use of\\u000aUTF-8 as a transformation of Unicode characters. ISO/IEC 10646\\u000adoes not allow mapping of unpaired surrogates, nor U+FFFE and U+FFFF\\u000a(but it does allow other noncharacters). For more information please refer to\\u000ahttp://www.unicode.org/unicode and http://www.cl.cam.ac.uk/~mgk25/unicode.html\n");
            tidyMsgs.append("invaliduri_summary=URIs must be properly escaped, they must not contain unescaped\\u000acharacters below U+0021 including the space character and not\\u000aabove U+007E. Tidy escapes the URI for you as recommended by\\u000aHTML 4.01 section B.2.1 and XML 1.0 section 4.2.2. Some user agents\\u000ause another algorithm to escape such URIs and some server-sided\\u000ascripts depend on that. If you want to depend on that, you must\\u000aescape the URI by your own. For more information please refer to\\u000ahttp://www.w3.org/International/O-URL-and-ident.html\n");
            tidyMsgs.append("joining_attribute={0} joining values of repeated attribute \"{1}\"\n");
            tidyMsgs.append("line_column=line {0,number} column {1,number} - \n");
            tidyMsgs.append("malformed_comment=adjacent hyphens within comment\n");
            tidyMsgs.append("malformed_doctype=expected \"html PUBLIC\" or \"html SYSTEM\"\n");
            tidyMsgs.append("missing_attr_value={0} attribute \"{1}\" lacks value\n");
            tidyMsgs.append("missing_attribute={0} lacks \"{1}\" attribute\n");
            tidyMsgs.append("missing_body=Can't create slides - document is missing a body element.\n");
            tidyMsgs.append("missing_doctype=missing <!DOCTYPE> declaration\n");
            tidyMsgs.append("missing_endtag_before=missing </{0}> before {1}\n");
            tidyMsgs.append("missing_endtag_for=missing </{0}>\n");
            tidyMsgs.append("missing_imagemap={0} should use client-side image map\n");
            tidyMsgs.append("missing_quotemark={0} attribute with missing trailing quote mark\n");
            tidyMsgs.append("missing_semicolon=Warning: entity \"{0}\" doesn''t end in '';''\n");
            tidyMsgs.append("missing_semicolon_ncr=numeric character reference \"{0}\" doesn't end in \";\"\n");
            tidyMsgs.append("missing_starttag=missing <{0}>\n");
            tidyMsgs.append("missing_title_element=inserting missing 'title' element\n");
            tidyMsgs.append("needs_author_intervention=This document has errors that must be fixed before\\u000ausing HTML Tidy to generate a tidied up version.\n");
            tidyMsgs.append("nested_emphasis=nested emphasis {0}\n");
            tidyMsgs.append("nested_quotation=nested q elements, possible typo\n");
            tidyMsgs.append("newline_in_uri={0} discarding newline in URI reference\n");
            tidyMsgs.append("no_warnings=no warnings or errors were found\n");
            tidyMsgs.append("noframes_content={0} not inside ''noframes'' element\n");
            tidyMsgs.append("non_matching_endtag=replacing unexpected {0} by </{1}>\n");
            tidyMsgs.append("num_warnings={0,choice,0#no warnings|1#1 warning|1<{0,number,integer} warnings}, {1,choice,0#no errors|1#1 error|2#{1,number,integer} errors} were found!\n");
            tidyMsgs.append("obsolete_element=replacing obsolete element {0} by {1}\n");
            tidyMsgs.append("proprietary_attr_value={0} proprietary attribute value \"{1}\"\n");
            tidyMsgs.append("proprietary_attribute={0} proprietary attribute \"{1}\"\n");
            tidyMsgs.append("proprietary_element={0} is not approved by W3C\n");
            tidyMsgs.append("repeated_attribute={0} dropping value \"{1}\" for repeated attribute \"{2}\"\n");
            tidyMsgs.append("replacing_element=replacing element {0} by {1}\n");
            tidyMsgs.append("report_version={0}: Document content looks like {1}\n");
            tidyMsgs.append("slides_found={0,number} Slides found\n");
            tidyMsgs.append("suspected_missing_quote=missing quotemark for attribute value\n");
            tidyMsgs.append("tag_not_allowed_in={0} isn''t allowed in <{1}> elements\n");
            tidyMsgs.append("too_many_elements=too many {0} elements\n");
            tidyMsgs.append("too_many_elements_in=too many {0} elements in <{1}>\n");
            tidyMsgs.append("trim_empty_element=trimming empty {0}\n");
            tidyMsgs.append("unescaped_ampersand=Warning: unescaped & which should be written as &amp;\n");
            tidyMsgs.append("unescaped_element=unescaped {0} in pre content\n");
            tidyMsgs.append("unexpected_end_of_file=end of file while parsing attributes {0}\n");
            tidyMsgs.append("unexpected_endtag=unexpected </{0}>\n");
            tidyMsgs.append("unexpected_endtag_in=unexpected </{0}> in <{1}>\n");
            tidyMsgs.append("unexpected_gt={0} missing ''>'' for end of tag\n");
            tidyMsgs.append("unexpected_quotemark={0} unexpected or duplicate quote mark\n");
            tidyMsgs.append("unknown_attribute=unknown attribute \"{0}\"\n");
            tidyMsgs.append("unknown_element={0} is not recognized!\n");
            tidyMsgs.append("unknown_entity=Warning: unescaped & or unknown entity \"{0}\"\n");
            tidyMsgs.append("unknown_file={0}: can''t open file \"{1}\"\n");
            tidyMsgs.append("unknown_option=Warning - unknown option: {0}\n");
            tidyMsgs.append("unrecognized_option=unrecognized option -{0} use -help to list options\n");
            tidyMsgs.append("using_br_inplace_of=using <br> in place of {0}\n");
            tidyMsgs.append("vendor_specific_chars_summary=It is unlikely that vendor-specific, system-dependent encodings\\u000awork widely enough on the World Wide Web; you should avoid using the \\u000a{0,choice,0#specified|1#Windows-1252|2#MacRoman} character encoding, instead you are recommended to\\u000ause named entities, e.g. &trade;.\n");
            tidyMsgs.append("warning=Warning: \n");
            tidyMsgs.append("xml_attribute_value={0} has XML attribute \"{1}\"\n");
            tidyMsgs.append("xml_id_sintax=ID \"{0}\" uses XML ID syntax\n");
            return new UnsynchronizedByteArrayInputStream(String.valueOf(tidyMsgs.toString()).getBytes());
        }
        return new BufferedInputStream(new FileInputStream(tidyMessagesFilename));
    }

    private static volatile Properties properties;

    /**
     * Gets the configuration for JTidy either read from file if <i>TidyConfiguration.properties</i> exists or created from default
     * configuration.
     * 
     * @return The configuration for JTidy
     */
    private static Properties getTidyConfiguration() {
        Properties tmp = properties;
        if (null == tmp) {
            synchronized (HTMLProcessing.class) {
                tmp = properties;
                if (null == tmp) {
                    final Properties properties = new Properties();
                    final String tidyConfigFilename = SystemConfig.getProperty(SystemConfig.Property.TidyConfiguration);
                    boolean useDefaultConfig = true;
                    if (null != tidyConfigFilename) {
                        try {
                            final InputStream in = new FileInputStream(tidyConfigFilename);
                            try {
                                properties.load(in);
                                useDefaultConfig = false;
                            } finally {
                                in.close();
                            }
                        } catch (final FileNotFoundException e) {
                            LOG.warn("Missing JTidy configuration file \"" + tidyConfigFilename + "\"");
                        } catch (final IOException e) {
                            LOG.warn("I/O error while reading JTidy configuration from file \"" + tidyConfigFilename + "\"");
                        }
                    }
                    if (useDefaultConfig) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Using default JTidy configuration");
                        }
                        try {
                            final StringBuilder defaultConfig = new StringBuilder(512);
                            defaultConfig.append("indent=auto\n");
                            defaultConfig.append("indent-spaces=2\n");
                            defaultConfig.append("wrap=0\n");
                            defaultConfig.append("markup=yes\n");
                            defaultConfig.append("clean=yes\n");
                            defaultConfig.append("output-xml=no\n");
                            defaultConfig.append("input-xml=no\n");
                            defaultConfig.append("show-warnings=yes\n");
                            defaultConfig.append("numeric-entities=yes\n");
                            defaultConfig.append("quote-marks=yes\n");
                            defaultConfig.append("quote-nbsp=yes\n");
                            defaultConfig.append("quote-ampersand=no\n");
                            defaultConfig.append("break-before-br=no\n");
                            defaultConfig.append("uppercase-tags=yes\n");
                            defaultConfig.append("uppercase-attributes=yes\n");
                            defaultConfig.append("#smart-indent=no\n");
                            defaultConfig.append("output-xhtml=yes\n");
                            defaultConfig.append("char-encoding=latin1");
                            properties.load(new UnsynchronizedByteArrayInputStream(String.valueOf(defaultConfig.toString()).getBytes()));
                        } catch (final UnsupportedEncodingException e) {
                            /*
                             * Cannot occur
                             */
                            LOG.error(e.getMessage(), e);
                        } catch (final IOException e) {
                            /*
                             * Cannot occur
                             */
                            LOG.error(e.getMessage(), e);
                        }
                    }
                    tmp = HTMLProcessing.properties = properties;
                }
            }
        }
        return tmp;
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

    /**
     * Validates specified HTML content with <a href="http://tidy.sourceforge.net/">tidy html</a> library.
     * 
     * @param htmlContent The HTML content
     * @return The validated HTML content
     */
    public static String validate(final String htmlContent) {
        /*
         * Obtain a new Tidy instance
         */
        final Tidy tidy = createNewTidyInstance();
        /*
         * Run tidy, providing a reader and writer
         */
        final Writer writer = new UnsynchronizedStringWriter(htmlContent.length());
        tidy.parse(new StringReader(htmlContent), writer);
        return writer.toString();
    }

    private static Tidy createNewTidyInstance() {
        final Tidy tidy = new Tidy();
        /*
         * Set desired configuration options using tidy setters
         */
        tidy.setXHTML(true);
        tidy.setConfigurationFromProps(getTidyConfiguration());
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

    /**
     * Pretty prints specified HTML content.
     * 
     * @param htmlContent The HTML content
     * @return Pretty printed HTML content
     */
    public static String prettyPrint(final String htmlContent) {
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

    private static final Pattern PATTERN_BLOCKQUOTE =
        Pattern.compile("(?:(<blockquote.*?>)|(</blockquote>))", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Converts given HTML content into plain text, but keeps <code>&lt;blockquote&gt;</code> tags if any present.<br>
     * <b>NOTE:</b> returned content is again HTML content.
     * 
     * @param htmlContent The HTML content
     * @param converter The instance of {@link Html2TextConverter}
     * @return The partially converted plain text version of given HTML content as HTML content
     * @throws IOException If an I/O error occurs
     */
    public static String convertAndKeepQuotes(final String htmlContent, final Html2TextConverter converter) throws IOException {

        final StringBuilder sb = new StringBuilder(htmlContent.length() + 128);
        final Matcher m = PATTERN_BLOCKQUOTE.matcher(htmlContent);
        int lastMatch = 0;
        while (m.find()) {
            sb.append(htmlFormat(converter.convert(htmlContent.substring(lastMatch, m.start()))));
            sb.append(m.group());
            lastMatch = m.end();
        }
        sb.append(htmlFormat(converter.convert(htmlContent.substring(lastMatch))));
        return sb.toString();
    }

    private static Map<Character, String> htmlCharMap;

    private static Map<String, Character> htmlEntityMap;

    private static Map<Character, String> defaultHtmlCharMap;

    private static Map<String, Character> defaultHtmlEntityMap;

    static void setMaps(final Map<Character, String> htmlCharMap, final Map<String, Character> htmlEntityMap) {
        HTMLProcessing.htmlCharMap = htmlCharMap;
        HTMLProcessing.htmlEntityMap = htmlEntityMap;
    }

    static {
        final Map<Character, String> htmlCharMap = new HashMap<Character, String>();
        final Map<String, Character> htmlEntityMap = new HashMap<String, Character>();
        final Properties htmlEntities = new Properties();
        try {
            final StringBuilder entityMap = new StringBuilder(1024);
            entityMap.append("# character map for HTML emails\n");
            entityMap.append("weierp=8472\n");
            entityMap.append("supe=8839\n");
            entityMap.append("image=8465\n");
            entityMap.append("ecirc=234\n");
            entityMap.append("Otilde=213\n");
            entityMap.append("uacute=250\n");
            entityMap.append("diams=9830\n");
            entityMap.append("ntilde=241\n");
            entityMap.append("dArr=8659\n");
            entityMap.append("Ecirc=202\n");
            entityMap.append("ograve=242\n");
            entityMap.append("yacute=253\n");
            entityMap.append("times=215\n");
            entityMap.append("iuml=239\n");
            entityMap.append("rArr=8658\n");
            entityMap.append("micro=181\n");
            entityMap.append("rceil=8969\n");
            entityMap.append("plusmn=177\n");
            entityMap.append("there4=8756\n");
            entityMap.append("nabla=8711\n");
            entityMap.append("lsaquo=8249\n");
            entityMap.append("rang=9002\n");
            entityMap.append("Iuml=207\n");
            entityMap.append("real=8476\n");
            entityMap.append("sup3=179\n");
            entityMap.append("sube=8838\n");
            entityMap.append("acirc=226\n");
            entityMap.append("sup2=178\n");
            entityMap.append("sup1=185\n");
            entityMap.append("lsquo=8216\n");
            entityMap.append("Acirc=194\n");
            entityMap.append("sect=167\n");
            entityMap.append("notin=8713\n");
            entityMap.append("radic=8730\n");
            entityMap.append("ocirc=244\n");
            entityMap.append("oplus=8853\n");
            entityMap.append("euml=235\n");
            entityMap.append("Oacute=211\n");
            entityMap.append("rfloor=8971\n");
            entityMap.append("rdquo=8221\n");
            entityMap.append("Ocirc=212\n");
            entityMap.append("Igrave=204\n");
            entityMap.append("minus=8722\n");
            entityMap.append("trade=8482\n");
            entityMap.append("szlig=223\n");
            entityMap.append("Agrave=192\n");
            entityMap.append("forall=8704\n");
            entityMap.append("laquo=171\n");
            entityMap.append("cedil=184\n");
            entityMap.append("Euml=203\n");
            entityMap.append("ensp=8194\n");
            entityMap.append("Egrave=200\n");
            entityMap.append("otilde=245\n");
            entityMap.append("lowast=8727\n");
            entityMap.append("uml=168\n");
            entityMap.append("perp=8869\n");
            entityMap.append("int=8747\n");
            entityMap.append("nbsp=160\n");
            entityMap.append("Oslash=216\n");
            entityMap.append("Ugrave=217\n");
            entityMap.append("auml=228\n");
            entityMap.append("part=8706\n");
            entityMap.append("gt=62\n");
            entityMap.append("ouml=246\n");
            entityMap.append("ge=8805\n");
            entityMap.append("para=182\n");
            entityMap.append("empty=8709\n");
            entityMap.append("Auml=196\n");
            entityMap.append("isin=8712\n");
            entityMap.append("ang=8736\n");
            entityMap.append("uarr=8593\n");
            entityMap.append("agrave=224\n");
            entityMap.append("Ouml=214\n");
            entityMap.append("and=8743\n");
            entityMap.append("cap=8745\n");
            entityMap.append("exist=8707\n");
            entityMap.append("oline=8254\n");
            entityMap.append("egrave=232\n");
            entityMap.append("rsquo=8217\n");
            entityMap.append("oacute=243\n");
            entityMap.append("frac34=190\n");
            entityMap.append("larr=8592\n");
            entityMap.append("amp=38\n");
            entityMap.append("lrm=8206\n");
            entityMap.append("Atilde=195\n");
            entityMap.append("iquest=191\n");
            entityMap.append("infin=8734\n");
            entityMap.append("reg=174\n");
            entityMap.append("igrave=236\n");
            entityMap.append("sbquo=8218\n");
            entityMap.append("ucirc=251\n");
            entityMap.append("Ucirc=219\n");
            entityMap.append("yuml=255\n");
            entityMap.append("copy=169\n");
            entityMap.append("nsub=8836\n");
            entityMap.append("prime=8242\n");
            entityMap.append("raquo=187\n");
            entityMap.append("Ccedil=199\n");
            entityMap.append("Prime=8243\n");
            entityMap.append("hearts=9829\n");
            entityMap.append("oslash=248\n");
            entityMap.append("ugrave=249\n");
            entityMap.append("harr=8596\n");
            entityMap.append("brvbar=166\n");
            entityMap.append("Dagger=8225\n");
            entityMap.append("equiv=8801\n");
            entityMap.append("quot=34\n");
            entityMap.append("ordm=186\n");
            entityMap.append("deg=176\n");
            entityMap.append("bull=8226\n");
            entityMap.append("alefsym=8501\n");
            entityMap.append("frac14=188\n");
            entityMap.append("frac12=189\n");
            entityMap.append("ordf=170\n");
            entityMap.append("Iacute=205\n");
            entityMap.append("sim=8764\n");
            entityMap.append("zwnj=8204\n");
            entityMap.append("lfloor=8970\n");
            entityMap.append("otimes=8855\n");
            entityMap.append("rsaquo=8250\n");
            entityMap.append("Aacute=193\n");
            entityMap.append("uuml=252\n");
            entityMap.append("ndash=8211\n");
            entityMap.append("clubs=9827\n");
            entityMap.append("sup=8835\n");
            entityMap.append("atilde=227\n");
            entityMap.append("spades=9824\n");
            entityMap.append("sum=8721\n");
            entityMap.append("not=172\n");
            entityMap.append("loz=9674\n");
            entityMap.append("curren=164\n");
            entityMap.append("shy=173\n");
            entityMap.append("Eacute=201\n");
            entityMap.append("or=8744\n");
            entityMap.append("thinsp=8201\n");
            entityMap.append("sdot=8901\n");
            entityMap.append("aring=229\n");
            entityMap.append("sub=8834\n");
            entityMap.append("uArr=8657\n");
            entityMap.append("pound=163\n");
            entityMap.append("bdquo=8222\n");
            entityMap.append("Aring=197\n");
            entityMap.append("Uuml=220\n");
            entityMap.append("darr=8595\n");
            entityMap.append("Uacute=218\n");
            entityMap.append("cong=8773\n");
            entityMap.append("Ntilde=209\n");
            entityMap.append("ccedil=231\n");
            entityMap.append("aelig=230\n");
            entityMap.append("lArr=8656\n");
            entityMap.append("emsp=8195\n");
            entityMap.append("rarr=8594\n");
            entityMap.append("Ograve=210\n");
            entityMap.append("lceil=8968\n");
            entityMap.append("thorn=254\n");
            entityMap.append("Yacute=221\n");
            entityMap.append("euro=8364\n");
            entityMap.append("permil=8240\n");
            entityMap.append("dagger=8224\n");
            entityMap.append("ni=8715\n");
            entityMap.append("cent=162\n");
            entityMap.append("ne=8800\n");
            entityMap.append("cup=8746\n");
            entityMap.append("lang=9001\n");
            entityMap.append("asymp=8776\n");
            entityMap.append("THORN=222\n");
            entityMap.append("aacute=225\n");
            entityMap.append("AElig=198\n");
            entityMap.append("crarr=8629\n");
            entityMap.append("acute=180\n");
            entityMap.append("ETH=208\n");
            entityMap.append("iexcl=161\n");
            entityMap.append("icirc=238\n");
            entityMap.append("eacute=233\n");
            entityMap.append("divide=247\n");
            entityMap.append("eth=240\n");
            entityMap.append("hArr=8660\n");
            entityMap.append("ldquo=8220\n");
            entityMap.append("Icirc=206\n");
            entityMap.append("macr=175\n");
            entityMap.append("rlm=8207\n");
            entityMap.append("yen=165\n");
            entityMap.append("iacute=237\n");
            entityMap.append("hellip=8230\n");
            entityMap.append("middot=183\n");
            entityMap.append("prop=8733\n");
            entityMap.append("lt=60\n");
            entityMap.append("frasl=8260\n");
            entityMap.append("mdash=8212\n");
            entityMap.append("zwj=8205\n");
            entityMap.append("prod=8719\n");
            entityMap.append("le=8804");
            htmlEntities.load(new UnsynchronizedByteArrayInputStream(String.valueOf(entityMap.toString()).getBytes()));
        } catch (final IOException e) {
            /*
             * Cannot occur
             */
            LOG.error(e.getMessage(), e);
        }
        /*
         * Build up map
         */
        final Iterator<Map.Entry<Object, Object>> iter = htmlEntities.entrySet().iterator();
        final int size = htmlEntities.size();
        for (int i = 0; i < size; i++) {
            final Map.Entry<Object, Object> entry = iter.next();
            final Character c = Character.valueOf((char) Integer.parseInt((String) entry.getValue()));
            htmlEntityMap.put((String) entry.getKey(), c);
            htmlCharMap.put(c, (String) entry.getKey());
        }
        defaultHtmlCharMap = htmlCharMap;
        defaultHtmlEntityMap = htmlEntityMap;
    }

    private static Map<Character, String> getHTMLChar2EntityMap() {
        if (htmlCharMap == null) {
            return defaultHtmlCharMap;
        }
        return htmlCharMap;
    }

    private static Map<String, Character> getHTMLEntity2CharMap() {
        if (htmlEntityMap == null) {
            return defaultHtmlEntityMap;
        }
        return htmlEntityMap;
    }

    private static final Pattern PAT_HTML_ENTITIES = Pattern.compile("&(?:#([0-9]+)|([a-zA-Z]+));");

    /**
     * Replaces all HTML entities occurring in specified HTML content.
     * 
     * @param content The content
     * @return The content with HTML entities replaced
     */
    public static String replaceHTMLEntities(final String content) {
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

    /**
     * Maps specified HTML entity - e.g. <code>&amp;uuml;</code> - to corresponding ASCII character.
     * 
     * @param entity The HTML entity
     * @return The corresponding ASCII character or <code>null</code>
     */
    public static Character getHTMLEntity(final String entity) {
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
        final Character tmp = getHTMLEntity2CharMap().get(key);
        if (tmp != null) {
            return tmp;
        }
        return null;
    }

    private static String escape(final String s, final boolean withQuote, final int[][] ignoreRanges) {
        final int len = s.length();
        final StringBuilder sb = new StringBuilder(len);
        /*
         * Escape
         */
        final char[] chars = s.toCharArray();
        if (withQuote) {
            for (int i = 0; i < chars.length; i++) {
                final char c = chars[i];
                if (ignore(i, ignoreRanges)) {
                    sb.append(c);
                } else {
                    final String entity = getHTMLChar2EntityMap().get(Character.valueOf(c));
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
                if (ignore(i, ignoreRanges) || ('"' == c)) {
                    sb.append(c);
                } else {
                    final String entity = getHTMLChar2EntityMap().get(Character.valueOf(c));
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

    private static boolean ignore(final int index, final int[][] ignoreRanges) {
        if (null == ignoreRanges) {
            return false;
        }
        boolean ignore = false;
        for (int i = 0; !ignore && i < ignoreRanges.length; i++) {
            final int[] ignoreRange = ignoreRanges[i];
            if (index < ignoreRange[0]) {
                return false;
            }
            ignore = index < ignoreRange[1];
        }
        return ignore;
    }

    private static final String HTML_BR = "<br />";

    private static final Pattern PATTERN_CRLF = Pattern.compile("\r?\n");

    /**
     * Formats plain text to HTML by escaping HTML special characters e.g. <code>&quot;&lt;&quot;</code> is converted to
     * <code>&quot;&amp;lt;&quot;</code>.
     * 
     * @param plainText The plain text
     * @param withQuote Whether to escape quotes (<code>&quot;</code>) or not
     * @param ignoreRanges The ranges to ignore; leave to <code>null</code> to format whole text
     * @return properly escaped HTML content
     */
    private static String htmlFormat(final String plainText, final boolean withQuote, final int[][] ignoreRanges) {
        return PATTERN_CRLF.matcher(escape(plainText, withQuote, ignoreRanges)).replaceAll(HTML_BR);
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
        return PATTERN_CRLF.matcher(escape(plainText, withQuote, null)).replaceAll(HTML_BR);
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
        return htmlFormat(plainText, true);
    }

    private static final String DEFAULT_COLOR = "#0026ff";

    private static final String BLOCKQUOTE_START_TEMPLATE =
        "<blockquote type=\"cite\" style=\"margin-left: 0px; margin-right: 0px;" + " padding-left: 10px; color:%s; border-left: solid 1px %s;\">";

    /**
     * Determines the quote color for given <code>quotelevel</code>.
     * 
     * @param quotelevel - the quote level
     * @return The color for given <code>quotelevel</code>
     */
    private static String getLevelColor(final int quotelevel) {
        final String[] colors = MailProperties.getInstance().getQuoteLineColors();
        return (colors != null) && (colors.length > 0) ? (quotelevel >= colors.length ? colors[colors.length - 1] : colors[quotelevel]) : DEFAULT_COLOR;
    }

    private static final String BLOCKQUOTE_END = "</blockquote>\n";

    private static final String STR_HTML_QUOTE = "&gt;";

    private static final String STR_SPLIT_BR = "<br[ \t]*/?>";

    private static final String HTML_BREAK = "<br>";

    /**
     * Turns all simple quotes "&amp;gt; " occurring in specified HTML text to colored "&lt;blockquote&gt;" tags according to configured
     * quote colors.
     * 
     * @param htmlText The HTML text
     * @return The HTML text with simple quotes replaced with block quotes
     */
    public static String replaceHTMLSimpleQuotesForDisplay(final String htmlText) {
        final StringBuilder sb = new StringBuilder(htmlText.length());
        final String[] lines = htmlText.split(STR_SPLIT_BR);
        int levelBefore = 0;
        final int llen = lines.length - 1;
        for (int i = 0; i <= llen; i++) {
            String line = lines[i];
            int currentLevel = 0;
            int offset = 0;
            if ((offset = startsWithQuote(line)) != -1) {
                currentLevel++;
                int pos = -1;
                boolean next = true;
                while (next && ((pos = line.indexOf(STR_HTML_QUOTE, offset)) > -1)) {
                    /*
                     * Continue only if next starting position is equal to offset or if just one whitespace character has been skipped
                     */
                    next = ((offset == pos) || ((pos - offset == 1) && Character.isWhitespace(line.charAt(offset))));
                    if (next) {
                        currentLevel++;
                        offset = (pos + 4);
                    }
                }
            }
            if (offset > 0) {
                try {
                    offset = (offset < line.length()) && Character.isWhitespace(line.charAt(offset)) ? offset + 1 : offset;
                } catch (final StringIndexOutOfBoundsException e) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace(e.getMessage(), e);
                    }
                }
                line = line.substring(offset);
            }
            if (levelBefore < currentLevel) {
                for (; levelBefore < currentLevel; levelBefore++) {
                    final String color = getLevelColor(levelBefore);
                    sb.append(String.format(BLOCKQUOTE_START_TEMPLATE, color, color));
                }
            } else if (levelBefore > currentLevel) {
                for (; levelBefore > currentLevel; levelBefore--) {
                    sb.append(BLOCKQUOTE_END);
                }
            }
            sb.append(line);
            if (i < llen) {
                sb.append(HTML_BREAK);
            }
        }
        return sb.toString();
    }

    private static final Pattern PAT_STARTS_WITH_QUOTE = Pattern.compile("\\s*&gt;\\s*", Pattern.CASE_INSENSITIVE);

    private static int startsWithQuote(final String str) {
        final Matcher m = PAT_STARTS_WITH_QUOTE.matcher(str);
        if (m.find() && (m.start() == 0)) {
            return m.end();
        }
        return -1;
    }

    /**
     * Filters specified HTML content according to white-list filter.
     * 
     * @param htmlContent The HTML content
     * @return The filtered HTML content
     */
    public static String filterWhitelist(final String htmlContent) {
        final HTMLFilterHandler handler = new HTMLFilterHandler(htmlContent.length());
        HTMLParser.parse(htmlContent, handler);
        return handler.getHTML();
    }

    /**
     * Filters externally loaded images out of specified HTML content.
     * 
     * @param htmlContent The HTML content
     * @param modified A <code>boolean</code> array with length <code>1</code> to store modified status
     * @return The HTML content stripped by external images
     */
    public static String filterExternalImages(final String htmlContent, final boolean[] modified) {
        final HTMLImageFilterHandler handler = new HTMLImageFilterHandler(htmlContent.length());
        HTMLParser.parse(htmlContent, handler);
        modified[0] |= handler.isImageURLFound();
        return handler.getHTML();
    }

    private static final Pattern IMG_PATTERN = Pattern.compile("<img[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern CID_PATTERN =
        Pattern.compile("(?:src=cid:([^\\s>]*))|(?:src=\"cid:([^\"]*)\")", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern FILENAME_PATTERN =
        Pattern.compile("src=\"?([0-9a-z&&[^.\\s>\"]]+\\.[0-9a-z&&[^.\\s>\"]]+)\"?", Pattern.CASE_INSENSITIVE);

    // private static final String STR_AJAX_MAIL = "\"/ajax/mail?";

    private static final String STR_SRC = "src=";

    // private static final String CHARSET_ISO8859 = "ISO-8859-1";

    /**
     * Filters inline images occurring in HTML content of a message:
     * <ul>
     * <li>Inline images<br>
     * The source of inline images is in the message itself. Thus loading the inline image is redirected to the appropriate message (image)
     * attachment identified through header <code>Content-Id</code>; e.g.: <code>&lt;img
     * src=&quot;cid:[cid-value]&quot; ... /&gt;</code>.</li>
     * </ul>
     * 
     * @param content The HTML content possibly containing images
     * @param session The session
     * @param msgUID The message's unique path in mailbox
     * @return The HTML content with all inline images replaced with valid links
     */
    public static String filterInlineImages(final String content, final Session session, final MailPath msgUID) {
        String reval = content;
        try {
            final Matcher imgMatcher = IMG_PATTERN.matcher(reval);
            final MatcherReplacer imgReplacer = new MatcherReplacer(imgMatcher, reval);
            final StringBuilder sb = new StringBuilder(reval.length());
            if (imgMatcher.find()) {
                final StringBuilder strBuffer = new StringBuilder(256);
                final MatcherReplacer mr = new MatcherReplacer();
                final StringBuilder linkBuilder = new StringBuilder(256);
                /*
                 * Replace inline images with Content-ID
                 */
                do {
                    final String imgTag = imgMatcher.group();
                    if (!(replaceImgSrc(session, msgUID, imgTag, strBuffer, linkBuilder))) {
                        /*
                         * No cid pattern found, try with filename
                         */
                        strBuffer.setLength(0);
                        final Matcher m = FILENAME_PATTERN.matcher(imgTag);
                        mr.resetTo(m, imgTag);
                        if (m.find()) {
                            final String filename = m.group(1);
                            /*
                             * Compose corresponding image data
                             */
                            final ImageService imageService = ServerServiceRegistry.getInstance().getService(ImageService.class);
                            final String imageURL;
                            if (null == imageService) {
                                imageURL = "";
                            } else {
                                final InlineImageDataSource imgSource = new InlineImageDataSource();
                                final DataArguments args = new DataArguments();
                                final String[] argsNames = imgSource.getRequiredArguments();
                                args.put(argsNames[0], prepareFullname(msgUID.getAccountId(), msgUID.getFolder()));
                                args.put(argsNames[1], String.valueOf(msgUID.getMailID()));
                                args.put(argsNames[2], filename);
                                imageURL = imageService.addImageData(session, imgSource, args, 60000).getImageURL();
                            }
                            linkBuilder.setLength(0);
                            linkBuilder.append(STR_SRC).append('"').append(imageURL).append('"');
                            mr.appendLiteralReplacement(strBuffer, linkBuilder.toString());
                        }
                        mr.appendTail(strBuffer);
                    }
                    imgReplacer.appendLiteralReplacement(sb, strBuffer.toString());
                    strBuffer.setLength(0);
                } while (imgMatcher.find());
            }
            imgReplacer.appendTail(sb);
            reval = sb.toString();
        } catch (final Exception e) {
            LOG.warn("Unable to filter cid Images: " + e.getMessage());
        }
        return reval;
    }

    private static boolean replaceImgSrc(final Session session, final MailPath msgUID, final String imgTag, final StringBuilder cidBuffer, final StringBuilder linkBuilder) {
        boolean retval = false;
        final Matcher cidMatcher = CID_PATTERN.matcher(imgTag);
        final MatcherReplacer cidReplacer = new MatcherReplacer(cidMatcher, imgTag);
        if (cidMatcher.find()) {
            retval = true;
            do {
                /*
                 * Extract Content-ID
                 */
                String cid = cidMatcher.group(1);
                if (cid == null) {
                    cid = cidMatcher.group(2);
                }
                /*
                 * Compose corresponding image data
                 */
                final ImageService imageService = ServerServiceRegistry.getInstance().getService(ImageService.class);
                final String imageURL;
                if (null == imageService) {
                    LOG.warn("Missing image service.", new Throwable());
                    imageURL = "";
                } else {
                    final InlineImageDataSource imgSource = new InlineImageDataSource();
                    final DataArguments args = new DataArguments();
                    final String[] argsNames = imgSource.getRequiredArguments();
                    args.put(argsNames[0], prepareFullname(msgUID.getAccountId(), msgUID.getFolder()));
                    args.put(argsNames[1], String.valueOf(msgUID.getMailID()));
                    args.put(argsNames[2], cid);
                    imageURL = imageService.addImageData(session, imgSource, args, 60000).getImageURL();
                }
                linkBuilder.setLength(0);
                linkBuilder.append(STR_SRC).append('"').append(imageURL).append('"');
                cidReplacer.appendLiteralReplacement(cidBuffer, linkBuilder.toString());
            } while (cidMatcher.find());
        }
        cidReplacer.appendTail(cidBuffer);
        return retval;
    }

    /**
     * Translates specified string into application/x-www-form-urlencoded format using a specific encoding scheme. This method uses the
     * supplied encoding scheme to obtain the bytes for unsafe characters.
     * 
     * @param text The string to be translated.
     * @param charset The character encoding to use; should be <code>UTF-8</code> according to W3C
     * @return The translated string or the string itself if any error occurred
     */
    public static String urlEncodeSafe(final String text, final String charset) {
        try {
            return URLEncoder.encode(text, charset);
        } catch (final UnsupportedEncodingException e) {
            LOG.error(e.getMessage(), e);
            return text;
        }
    }

    /**
     * Initializes a new {@link HTMLProcessing}.
     */
    private HTMLProcessing() {
        super();
    }
}
