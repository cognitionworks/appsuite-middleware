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

package com.openexchange.html;

import java.util.List;

/**
 * {@link HTMLService} - The HTML service provides several methods concerning processing of HTML content.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface HTMLService {

    /**
     * Replaces image URLs with a proxy URI to ensure safe loading of image content.
     * 
     * @param content The HTML content
     * @param sessionId The identifier of the session needed to register appropriate proxy URI
     * @return The HTML content with image URLs replaced
     */
    String replaceImages(String content, String sessionId);

    /**
     * Converts found URLs inside specified content to valid links.
     * 
     * @param content The content
     * @return The content with URLs turned to links
     */
    String formatURLs(String content);

    /**
     * Searches for non-HTML links and convert them to valid HTML links.
     * <p>
     * Example: <code>http://www.somewhere.com</code> is converted to
     * <code>&lt;a&nbsp;href=&quot;http://www.somewhere.com&quot;&gt;http://www.somewhere.com&lt;/a&gt;</code>.
     * 
     * @param content The content to search in
     * @return The given content with all non-HTML links converted to valid HTML links
     */
    String formatHrefLinks(final String content);

    /**
     * Filters specified HTML content according to white-list filter.
     * <p>
     * <b>Note</b>: Specified HTML content needs to be validated as per {@link #getConformHTML(String, String)}
     * 
     * @param htmlContent The <b>validated</b> HTML content
     * @return The filtered HTML content
     * @see #getConformHTML(String, String)
     */
    String filterWhitelist(String htmlContent);

    /**
     * Filters specified HTML content according to white-list filter.
     * <p>
     * <b>Note</b>: Specified HTML content needs to be validated as per {@link #getConformHTML(String, String)}
     * 
     * @param htmlContent The <b>validated</b> HTML content
     * @param configName The name of the whitelist to use.
     * @return The filtered HTML content
     * @see #getConformHTML(String, String)
     */
    String filterWhitelist(String htmlContent, String configName);

    
    /**
     * Filters externally loaded images out of specified HTML content.
     * <p>
     * <b>Note</b>: Specified HTML content needs to be validated as per {@link #getConformHTML(String, String)}
     * 
     * @param htmlContent The <b>validated</b> HTML content
     * @param modified A <code>boolean</code> array with length <code>1</code> to store modified status
     * @return The HTML content stripped by external images
     * @see #getConformHTML(String, String)
     */
    String filterExternalImages(String htmlContent, boolean[] modified);

    /**
     * Converts specified HTML content to plain text.
     * <p>
     * <b>Note</b>: Specified HTML content needs to be validated as per {@link #getConformHTML(String, String)}
     * 
     * @param htmlContent The <b>validated</b> HTML content
     * @param appendHref <code>true</code> to append URLs contained in <i>href</i>s and <i>src</i>s; otherwise <code>false</code>.<br>
     *            Example: <code>&lt;a&nbsp;href=\"www.somewhere.com\"&gt;Link&lt;a&gt;</code> would be <code>Link&nbsp;[www.somewhere.com]</code>
     * @return The plain text representation of specified HTML content
     * @see #getConformHTML(String, String)
     */
    String html2text(String htmlContent, boolean appendHref);

    /**
     * Formats plain text to HTML by escaping HTML special characters e.g. <code>&quot;&lt;&quot;</code> is converted to
     * <code>&quot;&amp;lt;&quot;</code>.
     * 
     * @param plainText The plain text
     * @param withQuote Whether to escape quotes (<code>&quot;</code>) or not
     * @param ignoreRanges The ranges to ignore; leave to <code>null</code> to format whole text
     * @return properly escaped HTML content
     */
    String htmlFormat(String plainText, boolean withQuote, List<Range> ignoreRanges);

    /**
     * Formats plain text to HTML by escaping HTML special characters e.g. <code>&quot;&lt;&quot;</code> is converted to
     * <code>&quot;&amp;lt;&quot;</code>.
     * 
     * @param plainText The plain text
     * @param withQuote Whether to escape quotes (<code>&quot;</code>) or not
     * @return properly escaped HTML content
     */
    String htmlFormat(String plainText, boolean withQuote);

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
    String htmlFormat(String plainText);

    /**
     * Creates valid HTML from specified HTML content conform to W3C standards.
     * Non-ascii-URLs will be replaced with puny-code-encoded URLs.
     * 
     * @param htmlContent The HTML content
     * @param charset The charset parameter
     * @return The HTML content conform to W3C standards
     */
    String getConformHTML(String htmlContent, String charset);
    
    /**
     * Creates valid HTML from specified HTML content conform to W3C standards.
     * 
     * @param htmlContent The HTML content
     * @param charset The charset parameter
     * @param checkUrls Define if non-ascii-URLs shell be replaced with puny-code-encoded URLs
     * @return The HTML content conform to W3C standards
     */
    String getConformHTML(String htmlContent, String charset, boolean replaceUrls);

    /**
     * Pretty prints specified HTML content.
     * 
     * @param htmlContent The HTML content
     * @return Pretty printed HTML content
     */
    String prettyPrint(final String htmlContent);

    /**
     * Replaces all HTML entities occurring in specified HTML content.
     * 
     * @param content The content
     * @return The content with HTML entities replaced
     */
    String replaceHTMLEntities(String content);

    /**
     * Maps specified HTML entity - e.g. <code>&amp;uuml;</code> - to corresponding ASCII character.
     * 
     * @param entity The HTML entity
     * @return The corresponding ASCII character or <code>null</code>
     */
    Character getHTMLEntity(String entity);

}
