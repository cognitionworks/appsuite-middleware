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

package com.openexchange.tools.mail;

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
public class Html2TextConverter {
	
	private static final String LINEBREAK = "\n";

	private boolean body_found;

	private boolean in_body;

	private boolean pre;

	private String href = "";

	private final void reset() {
		body_found = false;
		in_body = false;
		pre = false;
		href = "";
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
	public final String convertWithQuotes(final String htmlContent) throws IOException {
		final StringBuilder sb = new StringBuilder(htmlContent.length() + 100);
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
			final String head = convert(htmlContent.substring(0, start));
			sb.append(head);
			final String body = convertWithQuotes(addSimpleQuote(htmlContent.substring(bodyStart, bodyEnd)));
			sb.append(body);
			final String tail = convertWithQuotes(htmlContent.substring(end));
			sb.append(tail);
		} else {
			reset();
			sb.append(convert(htmlContent));
		}
		return sb.toString();
	}

	private static final String addSimpleQuote(final String htmlContent) {
		return addSimpleQuote(htmlContent, 1);
	}

	private static final String addSimpleQuote(final String htmlContent, final int quoteLevel) {
		final String[] lines = htmlContent.split("<br>");
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			for (int j = 0; j < quoteLevel; j++) {
				sb.append(HTML_GT);
			}
			sb.append(lines[i]).append(HTML_BREAK);
		}
		return sb.toString();
	}

	/**
	 * Converts given HTML content into plain text
	 * 
	 * @return plain text version of given HTML content
	 */
	public final String convert(final String htmlContent) throws IOException {
		reset();
		final StringBuilder result = new StringBuilder();
		final StringBuilder result2 = new StringBuilder();
		final StringReader input = new StringReader(htmlContent);
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
					final String CurrentTag = getTag(input);
					text = convertTag(CurrentTag, in_body ? getLastChar(result) : getLastChar(result2));
				} else if (c == '&') {
					final String special = getSpecial(input);
					if ("lt;".equals(special) || "#60".equals(special)) {
						text = "<";
					} else if ("gt;".equals(special) || "#62".equals(special)) {
						text = ">";
					} else if ("amp;".equals(special) || "#38".equals(special)) {
						text = "&";
					} else if ("nbsp;".equals(special)) {
						text = " ";
					} else if ("quot;".equals(special) || "#34".equals(special)) {
						text = "\"";
					} else if ("copy;".equals(special) || "#169".equals(special)) {
						text = "[Copyright]";
					} else if ("reg;".equals(special) || "#174".equals(special)) {
						text = "[Registered]";
					} else if ("trade;".equals(special) || "#153".equals(special)) {
						text = "[Trademark]";
					} else {
						text = '&' + special;
					}
				} else if (!pre && Character.isWhitespace((char) c)) {
					final StringBuilder s = in_body ? result : result2;
					if (s.length() > 0 && Character.isWhitespace(s.charAt(s.length() - 1))) {
						text = "";
					} else {
						text = " ";
					}
				} else {
					text = "" + (char) c;
				}
				final StringBuilder s = in_body ? result : result2;
				s.append(text == null ? "" : text);
				c = input.read();
			}
		} catch (IOException e) {
			input.close();
			throw e;
		}
		return body_found ? result.toString() : result2.toString();
	}
	
	private static final char getLastChar(final StringBuilder sb) {
		if (sb.length() == 0) {
			return ' ';
		}
		return sb.charAt(sb.length() - 1);
	}

	private static final String getTag(final Reader r) throws IOException {
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

	private static final String getSpecial(final Reader r) throws IOException {
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

	private static final boolean isTag(final String tag, final String pattern) {
		return tag.regionMatches(true, 0, '<' + pattern + '>', 0, pattern.length() + 2)
				|| tag.regionMatches(true, 0, '<' + pattern + ' ', 0, pattern.length() + 2);
	}

	private final String convertTag(final String t, final char prevChar) {
		String result = "";
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
			result = LINEBREAK;
			pre = true;
		} else if (isTag(t, "/pre")) {
			result = LINEBREAK;
			pre = false;
		} else if (isTag(t, "p")) {
			if (prevChar == '\n') {
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
		} else if (isTag(t, "b")) {
			result = "*";
		} else if (isTag(t, "/b")) {
			result = "*";
		} else if (isTag(t, "i")) {
			result = "\"";
		} else if (isTag(t, "/i")) {
			result = "\"";
		} else if (isTag(t, "img")) {
			int idx = t.indexOf("alt=\"");
			if (idx != -1) {
				idx += 5;
				final int idx2 = t.indexOf("\"", idx);
				result = t.substring(idx, idx2);
			}
		} else if (isTag(t, "a")) {
			int idx = t.indexOf("href=\"");
			if (idx == -1) {
				href = "";
			} else {
				idx += 6;
				final int idx2 = t.indexOf("\"", idx);
				href = t.substring(idx, idx2);
			}
		} else if (isTag(t, "/a")) {
			if (href.length() > 0) {
				result = new StringBuilder(100).append(" [ ").append(href).append(" ]").toString();
				href = "";
			}
		}
		return result;
	}
}
