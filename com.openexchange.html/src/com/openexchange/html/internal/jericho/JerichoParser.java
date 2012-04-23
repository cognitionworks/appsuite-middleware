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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.html.internal.jericho;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.EndTag;
import net.htmlparser.jericho.EndTagType;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.StartTagType;
import net.htmlparser.jericho.StreamedSource;
import net.htmlparser.jericho.Tag;
import net.htmlparser.jericho.TagType;
import org.apache.commons.logging.Log;
import com.openexchange.log.LogFactory;
import com.openexchange.html.internal.parser.HtmlHandler;

/**
 * {@link JerichoParser} - Parses specified real-life HTML document.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class JerichoParser {

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(JerichoParser.class));

    private static final boolean DEBUG = LOG.isDebugEnabled();

    /**
     * Initializes a new {@link JerichoParser}.
     */
    private JerichoParser() {
        super();
    }

    private static final Pattern BODY_START = Pattern.compile("<body.*?>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    /**
     * Ensure given HTML content has a <code>&lt;body&gt;</code> tag.
     * 
     * @param html The HTML content to check
     * @return The checked HTML content possibly with surrounded with a <code>&lt;body&gt;</code> tag
     */
    private static String checkBody(final String html) {
        if (null == html) {
            return html;
        }
        if (BODY_START.matcher(html).find()) {
            return html;
        }
        // <body> tag missing
        String sep = System.getProperty("line.separator");
        if (null == sep) {
            sep = "\n";
        }
        return new StringBuilder(html.length() + 16).append("<body>").append(sep).append(html).append(sep).append("</body>").toString();
    }

    private static final Pattern NESTED_TAG = Pattern.compile("^(?:\r?\n *)?(<[^>]+>)");

    private static final Pattern INVALID_DELIM = Pattern.compile("\" *, *\"");
    
    /**
     * Parses specified real-life HTML document and delegates events to given instance of {@link HtmlHandler}
     * 
     * @param html The real-life HTML document
     * @param handler The HTML handler
     */
    public static void parse(final String html, final JerichoHandler handler) {
        final long st = DEBUG ? System.currentTimeMillis() : 0L;
        final StreamedSource streamedSource = new StreamedSource(checkBody(html));
        streamedSource.setLogger(null);
        int lastSegmentEnd = 0;
        for (final Segment segment : streamedSource) {
            if (segment.getEnd() <= lastSegmentEnd) {
                /*
                 * If this tag is inside the previous tag (e.g. a server tag) then ignore it as it was already output along with the
                 * previous tag.
                 */
                continue;
            }
            lastSegmentEnd = segment.getEnd();
            /*
             * Handle current segment
             */
            handleSegment(handler, segment, false);
        }
        if (DEBUG) {
            final long dur = System.currentTimeMillis() - st;
            LOG.debug("\tJerichoParser.parse() took " + dur + "msec.");
        }
    }

    private static void handleSegment(final JerichoHandler handler, final Segment segment, final boolean reparseSegment) {
        if (segment instanceof Tag) {
            final Tag tag = (Tag) segment;
            final TagType tagType = tag.getTagType();
            if (tagType == StartTagType.NORMAL) {
                handler.handleStartTag((StartTag) tag);
            } else if (tagType == EndTagType.NORMAL) {
                handler.handleEndTag((EndTag) tag);
            } else if (tagType == StartTagType.DOCTYPE_DECLARATION) {
                handler.handleDocDeclaration(segment.toString());
            } else if (tagType == StartTagType.CDATA_SECTION) {
                handler.handleCData(segment.toString());
            } else if (tagType == StartTagType.COMMENT) {
                handler.handleComment(segment.toString());
            } else {
                if (!segment.isWhiteSpace()) {
                    handler.handleUnknownTag(tag);
                }
            }
        } else if (segment instanceof CharacterReference) {
            final CharacterReference characterReference = (CharacterReference) segment;
            handler.handleCharacterReference(characterReference);
        } else {
            /*
             * Safety re-parse
             */
            if (reparseSegment && (segment.toString().indexOf('<') >= 0) && !segment.isWhiteSpace()) {
                final Matcher m = NESTED_TAG.matcher(segment);
                if (m.matches()) {
                    final int startTagPos = m.start(1);
                    /*
                     * Handle extracted whitespace segment
                     */
                    StreamedSource nestedSource = new StreamedSource(segment.subSequence(0, startTagPos));
                    for (final Segment nestedSegment : nestedSource) {
                        handler.handleSegment(nestedSegment);
                    }
                    /*
                     * Re-parse start tag
                     */
                    final String startTag = fixStyleAttribute(segment.subSequence(startTagPos, segment.length()).toString());
                    nestedSource = new StreamedSource(startTag);
                    for (final Segment nestedSegment : nestedSource) {
                        handleSegment(handler, nestedSegment, false);
                    }
                } else {
                    handler.handleSegment(segment);
                }
            } else {
                handler.handleSegment(segment);
            }
        }
    }

    private static String fixStyleAttribute(final String startTag) {
        if (startTag.indexOf("style=") <= 0) {
            return startTag;
        }
        return INVALID_DELIM.matcher(startTag).replaceAll("; ");
    }

}
