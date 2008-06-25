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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

import static com.openexchange.mail.text.HTMLProcessing.PATTERN_HREF;
import static com.openexchange.mail.text.HTMLProcessing.getHTMLEntity;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Html2TextConverter
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public final class Html2TextConverter {

	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory
			.getLog(Html2TextConverter.class);

	private static final String STR_EMPTY = "";

	private static final String LINEBREAK = "\n";

	private boolean body_found;

	private boolean in_body;

	private boolean pre;

	private String href = STR_EMPTY;

	private boolean gatherAnchor;

	private final StringBuilder anchorBuilder;

	private static String getEntity(final String special) {
		final Character c = getHTMLEntity(special);
		return null == c ? null : String.valueOf(c.charValue());
	}

	public Html2TextConverter() {
		super();
		anchorBuilder = new StringBuilder(100);
	}

	private void reset() {
		body_found = false;
		in_body = false;
		pre = false;
		href = STR_EMPTY;
		gatherAnchor = false;
	}

	private static final String HTML_BREAK = "<br>";

	private static final String HTML_GT = "&gt; ";

	private static final Pattern PATTERN_BLOCKQUOTE = Pattern.compile("(?:(<blockquote.*?>)|(</blockquote>))",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	/**
	 * Converts given HTML content into plain text and turns blockquotes into
	 * simple quotes
	 * 
	 * @return plain text version of given HTML content
	 */
	public String convertWithQuotes(final String htmlContent) throws IOException {
		final StringBuilder sb = new StringBuilder(htmlContent.length() + 1024);
		final Matcher m = PATTERN_BLOCKQUOTE.matcher(htmlContent);
		boolean found = false;
		int start = 0, end = 0, bodyStart = htmlContent.length(), bodyEnd = htmlContent.length();
		int openBlockquotes = 0;
		FindBlockquote: while (m.find()) {
			if (m.group(2) == null) {
				/*
				 * Starting tag
				 */
				found = true;
				if (0 == openBlockquotes++) {
					start = m.start();
					bodyStart = m.end();
				}
			} else {
				/*
				 * Ending tag
				 */
				if (0 == --openBlockquotes) {
					end = m.end();
					bodyEnd = m.start();
					break FindBlockquote;
				}
			}
		}
		if (openBlockquotes > 0) {
			/*
			 * Malformed html
			 */
			final String head = convert(htmlContent.substring(0, start));
			sb.append(head);
			reset();
			sb.append(convert(addSimpleQuote(htmlContent.substring(start), openBlockquotes)));
			return sb.toString();
		}
		if (found) {
			reset();
			/*
			 * Head
			 */
			sb.append(convert(htmlContent.substring(0, start)));
			/*
			 * Body
			 */
			sb.append(convertWithQuotes(addSimpleQuote(htmlContent.substring(bodyStart, bodyEnd))));
			/*
			 * Tail
			 */
			sb.append(convertWithQuotes(htmlContent.substring(end)));
		} else {
			reset();
			sb.append(convert(htmlContent));
		}
		return sb.toString();
	}

	private static String addSimpleQuote(final String htmlContent) {
		return addSimpleQuote(htmlContent, 1);
	}

	private static final String SPLIT_BREAK = "<br */?>";

	private static String addSimpleQuote(final String htmlContent, final int quoteLevel) {
		final String[] lines = htmlContent.split(SPLIT_BREAK);
		final StringBuilder sb = new StringBuilder(htmlContent.length() + 256);
		/*
		 * Compose quote prefix
		 */
		final String quotePrefix;
		if (quoteLevel == 1) {
			quotePrefix = HTML_GT;
		} else {
			for (int j = 0; j < quoteLevel; j++) {
				sb.append(HTML_GT);
			}
			quotePrefix = sb.toString();
			sb.setLength(0);
		}
		/*
		 * Prepend prefix to each line
		 */
		for (int i = 0; i < lines.length; i++) {
			sb.append(quotePrefix);
			sb.append(lines[i]).append(HTML_BREAK);
		}
		return sb.toString();
	}

	/**
	 * Converts given HTML content into plain text
	 * 
	 * @return plain text version of given HTML content
	 */
	public String convert(final String htmlContent) throws IOException {
		reset();
		final StringBuilder result = new StringBuilder(1024);
		final StringBuilder result2 = new StringBuilder(1024);
		final StringReader input = new StringReader(removeStyleSheets(htmlContent));
		try {
			String text = null;
			int c = input.read();
			/*
			 * Convert until '-1' (EOF) is reached
			 */
			while (c != -1) {
				if (c == '<') {
					/*
					 * A starting tag
					 */
					text = convertTag(getTag(input), in_body ? getLastChar(result) : getLastChar(result2));
				} else if (c == '&') {
					final String special = getSpecial(input);
					if (special.length() > 0) {
						final String tmp = getEntity(special);
						if (tmp == null) {
							text = '&' + special;
						} else {
							text = tmp;
						}
					} else {
						text = '&' + special;
					}
				} else if (!pre && Character.isWhitespace((char) c)) {
					final StringBuilder s = in_body ? result : result2;
					if ((s.length() > 0) && Character.isWhitespace(s.charAt(s.length() - 1))) {
						text = STR_EMPTY;
					} else {
						text = " ";
					}
				} else {
					text = String.valueOf((char) c);
				}
				final StringBuilder s = in_body ? result : result2;
				s.append(text);
				if (gatherAnchor) {
					anchorBuilder.append(text);
				}
				c = input.read();
			}
		} catch (final IOException e) {
			input.close();
			throw e;
		}
		return body_found ? result.toString() : result2.toString();
	}

	private static final int NONE = -99;

	private static int getLastChar(final StringBuilder sb) {
		if (sb.length() == 0) {
			return NONE;
		}
		return sb.charAt(sb.length() - 1);
	}

	private static final Pattern PAT_STYLE_RM = Pattern.compile("<style[^>]*>.*?</style[^>]*>",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private static String removeStyleSheets(final String htmlContent) {
		final Matcher m = PAT_STYLE_RM.matcher(htmlContent);
		final StringBuffer sb = new StringBuffer(htmlContent.length());
		while (m.find()) {
			m.appendReplacement(sb, "");
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private static String getTag(final Reader r) throws IOException {
		final StringBuilder result = new StringBuilder();
		int level = 1;
		result.append('<');
		while (level > 0) {
			final int c = r.read();
			if (c == -1) {
				break; // EOF
			}
			result.append((char) c);
			if (c == '<') {
				level++;
			} else if (c == '>') {
				level--;
			}
		}
		return result.toString();
	}

	private static String getSpecial(final Reader r) throws IOException {
		final StringBuilder result = new StringBuilder();
		/*
		 * Mark the present position in the stream
		 */
		r.mark(1);
		int c = r.read();
		while (Character.isLetter((char) c)) {
			result.append((char) c);
			r.mark(1);
			c = r.read();
		}
		if (c == ';') {
			result.append(';');
		} else {
			r.reset();
		}
		return result.toString();
	}

	private static boolean isTag(final String tag, final String pattern) {
		return Pattern.compile(new StringBuilder(16).append('<').append(pattern).append("[^>]*>").toString(),
				Pattern.CASE_INSENSITIVE).matcher(tag).matches();
	}

	private static final Pattern PATTERN_HREF_CONTENT = Pattern.compile("href=\"?([^\\s\">]+)\"?",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern PATTERN_ALT_CONTENT = Pattern.compile("alt=\"?([^\">]+)\"?", Pattern.CASE_INSENSITIVE);

	private static final Pattern PATTERN_SRC_CONTENT = Pattern.compile("src=\"?([^\\s\">]+)\"?",
			Pattern.CASE_INSENSITIVE);

	private String convertTag(final String t, final int prevChar) {
		String result = STR_EMPTY;
		if (isTag(t, "body")) {
			in_body = true;
			body_found = true;
		} else if (isTag(t, "/body")) {
			in_body = false;
			result = LINEBREAK;
		} else if (isTag(t, "center")) {
			result = LINEBREAK;
		} else if (isTag(t, "/center")) {
			result = LINEBREAK;
		} else if (isTag(t, "pre")) {
			// result = LINEBREAK;
			pre = true;
		} else if (isTag(t, "/pre")) {
			// result = LINEBREAK;
			pre = false;
		} else if (isTag(t, "p")) {
			if (prevChar == NONE) {
				result = STR_EMPTY;
			} else if (((char) prevChar) == '\n') {
				result = LINEBREAK;
			} else {
				result = "\n\n";
			}
		} else if (isTag(t, "br")) {
			result = LINEBREAK;
		} else if (isTag(t, "br/")) {
			result = LINEBREAK;
		} else if (isTag(t, "h1") || isTag(t, "h2") || isTag(t, "h3") || isTag(t, "h4") || isTag(t, "h5")
				|| isTag(t, "h6") || isTag(t, "h7")) {
			result = LINEBREAK;
		} else if (isTag(t, "/h1") || isTag(t, "/h2") || isTag(t, "/h3") || isTag(t, "/h4") || isTag(t, "/h5")
				|| isTag(t, "/h6") || isTag(t, "/h7")) {
			result = LINEBREAK;
		} else if (isTag(t, "/dl")) {
			result = LINEBREAK;
		} else if (isTag(t, "dd")) {
			result = "\n * ";
		} else if (isTag(t, "dt")) {
			result = "      ";
		} else if (isTag(t, "li")) {
			result = "\n * ";
		} else if (isTag(t, "/ul")) {
			result = LINEBREAK;
		} else if (isTag(t, "/ol")) {
			result = LINEBREAK;
		} else if (isTag(t, "hr")) {
			result = "_________________________________________\n";
		} else if (isTag(t, "table")) {
			result = LINEBREAK;
		} else if (isTag(t, "/table")) {
			result = LINEBREAK;
		} else if (isTag(t, "form")) {
			result = LINEBREAK;
		} else if (isTag(t, "/form")) {
			result = LINEBREAK;
		}
		/**
		 * <pre>
		 * else if (isTag(t, &quot;b&quot;)) {
		 * 		result = &quot;*&quot;;
		 * } else if (isTag(t, &quot;/b&quot;)) {
		 * 		result = &quot;*&quot;;
		 * } else if (isTag(t, &quot;i&quot;)) {
		 * 		result = &quot;\&quot;&quot;;
		 * } else if (isTag(t, &quot;/i&quot;)) {
		 * 		result = &quot;\&quot;&quot;;
		 * }
		 * </pre>
		 */
		else if (isTag(t, "img")) {
			final StringBuilder sb = new StringBuilder(100);
			Matcher matcher = PATTERN_ALT_CONTENT.matcher(t);
			if (matcher.find()) {
				sb.append(matcher.group(1));
			}
			matcher = PATTERN_SRC_CONTENT.matcher(t);
			try {
				if (matcher.find() && PATTERN_HREF.matcher(matcher.group(1)).matches()) {
					sb.append(' ').append('[').append(matcher.group(1)).append(']');
				}
			} catch (final StackOverflowError error) {
				/*
				 * Ignore on error
				 */
				LOG.error(StackOverflowError.class.getName(), error);
			}
			result = sb.toString();
		} else if (isTag(t, "a")) {
			final Matcher matcher = PATTERN_HREF_CONTENT.matcher(t);
			if (matcher.find()) {
				href = matcher.group(1);
				anchorBuilder.setLength(0);
				gatherAnchor = true;
			} else {
				href = STR_EMPTY;
			}
		} else if (isTag(t, "/a") && (href.length() > 0)) {
			final String anchorTitle = anchorBuilder.toString();
			if (!PATTERN_HREF.matcher(anchorTitle).matches() && !href.equalsIgnoreCase(anchorTitle)) {
				result = new StringBuilder(100).append(' ').append('[').append(href).append(']').toString();
			}
			href = STR_EMPTY;
			gatherAnchor = false;
		}
		return result;
	}
}
