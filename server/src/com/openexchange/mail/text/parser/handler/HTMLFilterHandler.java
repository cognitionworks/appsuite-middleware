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

package com.openexchange.mail.text.parser.handler;

import static com.openexchange.mail.text.CSSMatcher.checkCSS;
import static com.openexchange.mail.text.CSSMatcher.checkCSSElements;
import static com.openexchange.mail.text.CSSMatcher.containsCSSElement;
import static com.openexchange.mail.text.HTMLProcessing.htmlFormat;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.openexchange.config.ConfigurationService;
import com.openexchange.configuration.SystemConfig;
import com.openexchange.configuration.SystemConfig.Property;
import com.openexchange.mail.text.parser.HTMLHandler;
import com.openexchange.server.services.ServerServiceRegistry;

/**
 * {@link HTMLFilterHandler} - The HTML white-list filter
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public final class HTMLFilterHandler implements HTMLHandler {

	private static final String COMMENT_END = "-->";

	private static final String COMMENT_START = "<!--";

	private static final String WARN_USING_DEFAULT_WHITE_LIST = "Using default white list";

	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory
			.getLog(HTMLFilterHandler.class);

	private static final String CRLF = "\r\n";

	private static final String STYLE = "style";

	private static final String CLASS = "class";

	private static final String ID = "id";

	private static final String HEAD = "head";

	private static final String BODY = "body";

	private static final String META = "meta";

	private static final String SCRIPT = "script";

	private static final String HTTP_EQUIV = "http-equiv";

	private static final Set<String> NUM_ATTRIBS = new HashSet<String>(0);

	// A decimal digit: [0-9]
	private static final Pattern PAT_NUMERIC = Pattern.compile("\\p{Digit}+");

	private static Map<String, Map<String, Set<String>>> shtmlMap;

	private static Map<String, Set<String>> sstyleMap;

	private final Map<String, Map<String, Set<String>>> htmlMap;

	private final Map<String, Set<String>> styleMap;

	private final StringBuilder htmlBuilder;

	private final StringBuilder attrBuilder;

	/**
	 * Used to track all subsequent elements of a tag that ought to be removed
	 * completely
	 */
	private int skipLevel;

	private boolean body;

	/**
	 * Used to track all subsequent elements of a tag from which only its tag
	 * elements ought to be removed
	 */
	private int depth;

	private boolean[] depthInfo;

	private boolean isCss;

	private final StringBuffer cssBuffer;

	/**
	 * Initializes a new {@link HTMLFilterHandler}
	 * 
	 * @param capacity
	 *            The initial capacity
	 * @param htmlMap
	 *            The HTML map
	 * @param styleMap
	 *            The CSS style map
	 */
	public HTMLFilterHandler(final int capacity, final Map<String, Map<String, Set<String>>> htmlMap,
			final Map<String, Set<String>> styleMap) {
		super();
		cssBuffer = new StringBuffer(256);
		htmlBuilder = new StringBuilder(capacity);
		attrBuilder = new StringBuilder(128);
		this.htmlMap = htmlMap;
		this.styleMap = styleMap;
		checkHTMLMap();
	}

	/**
	 * Initializes a new {@link HTMLFilterHandler}
	 * 
	 * @param capacity
	 *            The initial capacity
	 * @param mapStr
	 *            The map as string representation
	 */
	public HTMLFilterHandler(final int capacity, final String mapStr) {
		super();
		cssBuffer = new StringBuffer(256);
		htmlBuilder = new StringBuilder(capacity);
		attrBuilder = new StringBuilder(128);
		htmlMap = parseHTMLMap(mapStr);
		styleMap = parseStyleMap(mapStr);
		checkHTMLMap();
	}

	/**
	 * Initializes a new {@link HTMLFilterHandler} with default white list given
	 * through property {@link Property#Whitelist}
	 * 
	 * @param capacity
	 *            The initial capacity
	 */
	public HTMLFilterHandler(final int capacity) {
		super();
		cssBuffer = new StringBuffer(256);
		htmlBuilder = new StringBuilder(capacity);
		attrBuilder = new StringBuilder(128);
		if (null == shtmlMap) {
			loadWhitelist();
		}
		htmlMap = shtmlMap;
		styleMap = sstyleMap;
		checkHTMLMap();
	}

	/**
	 * Marks current <code>depth</code> position as <code>true</code> and
	 * increments <code>depth</code> counter
	 */
	private void mark() {
		if (null == depthInfo) {
			depthInfo = new boolean[8];
		} else if (depthInfo.length <= depth) {
			final boolean[] tmp = depthInfo;
			depthInfo = new boolean[depthInfo.length * 2];
			System.arraycopy(tmp, 0, depthInfo, 0, tmp.length);
		}
		depthInfo[depth++] = true;
	}

	/**
	 * Decrements <code>depth</code> counter and then marks its position as
	 * <code>false</code>
	 * 
	 * @return <code>true</code> if position's previous mark was set; otherwise
	 *         <code>false</code>
	 */
	private boolean getAndUnmark() {
		final boolean retval = depthInfo[--depth];
		depthInfo[depth] = false;
		return retval;
	}

	/**
	 * Loads the white list
	 */
	public static void loadWhitelist() {
		synchronized (HTMLFilterHandler.class) {
			if (null == shtmlMap) {
				String mapStr = null;
				{
					String whitelist = SystemConfig.getProperty(SystemConfig.Property.Whitelist);
					if (null == whitelist) {
						final ConfigurationService cs = ServerServiceRegistry.getInstance().getService(
								ConfigurationService.class);
						if (null != cs) {
							whitelist = cs.getProperty(SystemConfig.Property.Whitelist.getPropertyName());
						}
					}
					if (null == whitelist) {
						if (LOG.isWarnEnabled()) {
							LOG.warn(WARN_USING_DEFAULT_WHITE_LIST);
						}
						mapStr = new String(DEFAULT_WHITELIST);
					} else {
						BufferedReader reader = null;
						try {
							reader = new BufferedReader(new InputStreamReader(new FileInputStream(whitelist),
									"US-ASCII"));
							final StringBuilder sb = new StringBuilder();
							String line = null;
							while ((line = reader.readLine()) != null) {
								sb.append(line).append(CRLF);
							}
							mapStr = sb.toString();
						} catch (final UnsupportedEncodingException e) {
							if (LOG.isWarnEnabled()) {
								LOG.warn(WARN_USING_DEFAULT_WHITE_LIST, e);
							}
							mapStr = new String(DEFAULT_WHITELIST);
						} catch (final FileNotFoundException e) {
							if (LOG.isWarnEnabled()) {
								LOG.warn(WARN_USING_DEFAULT_WHITE_LIST, e);
							}
							mapStr = new String(DEFAULT_WHITELIST);
						} catch (final IOException e) {
							if (LOG.isWarnEnabled()) {
								LOG.warn(WARN_USING_DEFAULT_WHITE_LIST, e);
							}
							mapStr = new String(DEFAULT_WHITELIST);
						} finally {
							if (null != reader) {
								try {
									reader.close();
								} catch (final IOException e) {
									LOG.error(e.getMessage(), e);
								}
							}
						}
					}
				}
				shtmlMap = parseHTMLMap(mapStr);
				if (!shtmlMap.containsKey(HEAD)) {
					shtmlMap.put(HEAD, null);
				}
				shtmlMap = Collections.unmodifiableMap(shtmlMap);
				sstyleMap = Collections.unmodifiableMap(parseStyleMap(mapStr));
			}
		}
	}

	/**
	 * Resets the white list
	 */
	public static void resetWhitelist() {
		synchronized (HTMLFilterHandler.class) {
			shtmlMap = null;
			sstyleMap = null;
		}
	}

	private void checkHTMLMap() {
		if (!htmlMap.containsKey(HEAD)) {
			htmlMap.put(HEAD, null);
		}
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
		if (skipLevel == 0) {
			if (body && BODY.equals(tag)) {
				body = false;
			} else if (isCss && STYLE.equals(tag)) {
				isCss = false;
			}
			if (depth == 0) {
				htmlBuilder.append("</").append(tag).append('>');
			} else if (!getAndUnmark()) {
				htmlBuilder.append("</").append(tag).append('>');
			}
		} else {
			skipLevel--;
		}
	}

	public void handleError(final String errorMsg) {
		LOG.error(errorMsg);
	}

	public void handleSimpleTag(final String tag, final Map<String, String> attributes) {
		if (skipLevel > 0) {
			return;
		}
		if (htmlMap.containsKey(tag)) {
			addStartTag(tag, attributes, true, htmlMap.get(tag));
		}
	}

	public void handleStartTag(final String tag, final Map<String, String> attributes) {
		if (skipLevel > 0) {
			skipLevel++;
			return;
		}
		if (htmlMap.containsKey(tag)) {
			if (depth > 0) {
				depth++;
			}
			if (BODY.equals(tag)) {
				body = true;
			} else if (STYLE.equals(tag)) {
				isCss = true;
			}
			addStartTag(tag, attributes, false, htmlMap.get(tag));
		} else {
			if (!body || SCRIPT.equals(tag)) {
				/*
				 * Remove whole tag incl. subsequent content and tags
				 */
				skipLevel++;
			} else {
				/*
				 * Just remove tag definition:
				 * "<tag>text<subtag>text</subtag></tag>" would be
				 * "text<subtag>text</subtag>"
				 */
				mark();
			}
		}
	}

	public void handleCDATA(final String text) {
		if (skipLevel == 0) {
			htmlBuilder.append("<![CDATA[");
			if (isCss) {
				/*
				 * Handle style attribute
				 */
				checkCSS(cssBuffer.append(text), styleMap, true, true);
				htmlBuilder.append(cssBuffer.toString());
				cssBuffer.setLength(0);
			} else {
				htmlBuilder.append(text);
			}
			htmlBuilder.append("]]>");
		}
	}

	public void handleText(final String text, final boolean ignorable) {
		if (skipLevel == 0) {
			if (isCss) {
				if (ignorable) {
					htmlBuilder.append(text);
				} else {
					/*
					 * Handle style attribute
					 */
					checkCSS(cssBuffer.append(text), styleMap, true, true);
					htmlBuilder.append(cssBuffer.toString());
					cssBuffer.setLength(0);
				}
			} else {
				htmlBuilder.append(text);
			}
		}
	}

	private static final String VAL_START = "=\"";

	/**
	 * Adds tag occurring in white list to HTML result
	 * 
	 * @param tag
	 *            The tag to add
	 * @param a
	 *            The tag's attribute set
	 * @param simple
	 *            <code>true</code> to write a simple tag; otherwise
	 *            <code>false</code>
	 * @param attribs
	 *            The allowed tag's attributes or <code>null</code> to allow all
	 */
	private void addStartTag(final String tag, final Map<String, String> a, final boolean simple,
			final Map<String, Set<String>> attribs) {
		attrBuilder.setLength(0);
		if (simple && META.equals(tag) && a.containsKey(HTTP_EQUIV) && attribs.containsKey(HTTP_EQUIV)) {
			/*
			 * Special handling for allowed meta tag which provides an allowed
			 * HTTP header indicated through 'http-equiv' attribute
			 */
			final int size = a.size();
			final Iterator<Entry<String, String>> iter = a.entrySet().iterator();
			for (int i = 0; i < size; i++) {
				final Entry<String, String> e = iter.next();
				attrBuilder.append(' ').append(e.getKey()).append(VAL_START).append(e.getValue()).append('"');
			}
			htmlBuilder.append('<').append(tag).append(attrBuilder.toString()).append('/').append('>');
			return;
		}
		final int size = a.size();
		final Iterator<Entry<String, String>> iter = a.entrySet().iterator();
		for (int i = 0; i < size; i++) {
			final Entry<String, String> e = iter.next();
			final String attr = e.getKey().toLowerCase(Locale.ENGLISH);
			if (STYLE.equals(attr)) {
				/*
				 * Handle style attribute
				 */
				checkCSSElements(cssBuffer.append(e.getValue()), styleMap, true);
				final String checkedCSS = cssBuffer.toString();
				cssBuffer.setLength(0);
				if (containsCSSElement(checkedCSS)) {
					if (checkedCSS.indexOf('"') == -1) {
						attrBuilder.append(' ').append(STYLE).append(VAL_START).append(checkedCSS).append('"');
					} else {
						attrBuilder.append(' ').append(STYLE).append("='").append(checkedCSS).append('\'');
					}
				}
			} else if (CLASS.equals(attr) || ID.equals(attr)) {
				/*
				 * TODO: Is it safe to allow "class"/"id" attribute in any case
				 */
				attrBuilder.append(' ').append(attr).append(VAL_START).append(htmlFormat(e.getValue(), false)).append(
						'"');
			} else {
				if (null == attribs) {
					attrBuilder.append(' ').append(attr).append(VAL_START).append(htmlFormat(e.getValue(), false))
							.append('"');
				} else {
					if (attribs.containsKey(attr)) {
						final Set<String> allowedValues = attribs.get(attr);
						if (null == allowedValues || allowedValues.contains(e.getValue().toLowerCase(Locale.ENGLISH))) {
							attrBuilder.append(' ').append(attr).append(VAL_START).append(
									htmlFormat(e.getValue(), false)).append('"');
						} else if (NUM_ATTRIBS == allowedValues) {
							/*
							 * Only numeric attribute value allowed
							 */
							if (PAT_NUMERIC.matcher(e.getValue().trim()).matches()) {
								attrBuilder.append(' ').append(attr).append(VAL_START).append(e.getValue()).append('"');
							}
						}
					}
				}
			}
		}
		if (simple) {
			if (attrBuilder.length() > 0 || size == 0) {
				htmlBuilder.append('<').append(tag).append(attrBuilder.toString()).append('/').append('>');
			}
		} else {
			htmlBuilder.append('<').append(tag).append(attrBuilder.toString()).append('>');
		}
	}

	/**
	 * Gets the filtered HTML content
	 * 
	 * @return The filtered HTML content
	 */
	public String getHTML() {
		return htmlBuilder.toString();
	}

	/*
	 * ######################### HELPERS ##############################
	 */

	private static final byte[] DEFAULT_WHITELIST = String
			.valueOf(
					"# HTML tags and attributes\n"
							+ "\n"
							+ "html.tag.a=\",href,name,tabindex,target,type,\"\n"
							+ "html.tag.area=\",alt,coords,href,nohref[nohref],shape[:rect:circle:poly:default:],tabindex,target,\"\n"
							+ "html.tag.b=\"\"\n"
							+ "html.tag.basefont=\",color,face,size,\"\n"
							+ "html.tag.bdo=\",dir[:ltr:rtl:]\"\n"
							+ "html.tag.blockquote=\",type,\"\n"
							+ "html.tag.body=\",alink,background,bgcolor,link,text,vlink,\"\n"
							+ "html.tag.br=\",clear[:left:right:all:none:]\"\n"
							+ "html.tag.button=\",disabled[disabled],name,tabindex,type[:button:submit:reset:],value,\"\n"
							+ "html.tag.caption=\",align[:top:bottom:left:right:]\"\n"
							+ "html.tag.col=\",align[:left:center:right:justify:char:],char,charoff,span[],valign[:top:middle:bottom:baseline:],width,\"\n"
							+ "html.tag.colgroup=\",align[:left:center:right:justify:char:],char,charoff,span[],valign[:top:middle:bottom:baseline:],width,\"\n"
							+ "html.tag.del=\",datetime,\"\n"
							+ "html.tag.dir=\",compact[compact]\"\n"
							+ "html.tag.div=\",align[:left:center:right:justify:]\"\n"
							+ "html.tag.dl=\",compact[compact]\"\n"
							+ "html.tag.em=\"\"\n"
							+ "html.tag.font=\",color,face,size,\"\n"
							+ "html.tag.form=\",action,accept,accept-charset,enctype,method[:get:post:],name,target,\"\n"
							+ "html.tag.h1=\",align[:left:center:right:justify:]\"\n"
							+ "html.tag.h2=\",align[:left:center:right:justify:]\"\n"
							+ "html.tag.h3=\",align[:left:center:right:justify:]\"\n"
							+ "html.tag.h4=\",align[:left:center:right:justify:]\"\n"
							+ "html.tag.h5=\",align[:left:center:right:justify:]\"\n"
							+ "html.tag.h6=\",align[:left:center:right:justify:]\"\n"
							+ "html.tag.hr=\",align[:left:center:right:],noshade[noshade],size,width,\"\n"
							+ "html.tag.html=\",version,xmlns,\"\n"
							+ "html.tag.img=\",align[:top:middle:bottom:left:right:],alt,border,height,hspace,ismap[ismap],name,src,usemap,vspace,width,\"\n"
							+ "html.tag.input=\",accept,align[:top:middle:bottom:left:right:center:],alt,checked[checked],disabled[disabled],maxlength[],name,readonly[readonly],size,src,tabindex,type[:text:checkbox:radio:submit:reset:hidden:image:button:password:],value,\"\n"
							+ "html.tag.ins=\",datetime,\"\n"
							+ "html.tag.label=\",for,\"\n"
							+ "html.tag.legend=\",align[:left:top:right:bottom:]\"\n"
							+ "html.tag.li=\",type[:disc:square:circle:1:a:A:i:I:],value[],\"\n"
							+ "html.tag.map=\",name,\"\n"
							+ "html.tag.meta=\",http-equiv[:content-type:],\"\n"
							+ "html.tag.ol=\",compact[compact],start[],type[:1:a:A:i:I:],\"\n"
							+ "html.tag.optgroup=\",disabled[disabled],label,\"\n"
							+ "html.tag.option=\",disabled[disabled],label,selected[selected],value,\"\n"
							+ "html.tag.p=\",align[:left:center:right:justify:]\"\n"
							+ "html.tag.pre=\",width[],\"\n"
							+ "html.tag.select=\",disabled[disabled],multiple[multiple],name,size,tabindex[],\"\n"
							+ "html.tag.span=\"\"\n"
							+ "html.tag.strong=\"\"\n"
							+ "html.tag.style=\",media,type,\"\n"
							+ "html.tag.table=\",align[:left:center:right:],background,border,bgcolor,cellpadding,cellspacing,frame[:void:above:below:hsides:ihs:rhs:vsides:box:border:],rules[:none:groups:rows:cols:all:],summary,width,\"\n"
							+ "html.tag.tbody=\",align[:left:center:right:justify:char:],char,charoff,valign[:top:middle:bottom:baseline:],\"\n"
							+ "html.tag.td=\",abbr,align[:left:center:right:justify:char:],axis,background,bgcolor,char,charoff,colspan[],headers,height,nowrap[nowrap],rowspan[],scope[:row:col:rowgroup:colgroup:],valign[:top:middle:bottom:baseline:],width,\"\n"
							+ "html.tag.textarea=\",cols[],disabled[disabled],name,readonly[readonly],rows[],tabindex[],\"\n"
							+ "html.tag.tfoot=\",align[:left:center:right:justify:char:],char,charoff,valign[:top:middle:bottom:baseline:],\"\n"
							+ "html.tag.th=\",abbr,align[:left:center:right:justify:char:],axis,bgcolor,char,charoff,colspan[],headers,height,nowrap[nowrap],rowspan[],scope[:row:col:rowgroup:colgroup:],valign[:top:middle:bottom:baseline:],width,\"\n"
							+ "html.tag.thead=\",align[:left:center:right:justify:char:],char,charoff,valign[:top:middle:bottom:baseline:],\"\n"
							+ "html.tag.tr=\",align[:left:center:right:justify:char:],bgcolor,char,charoff,valign[:top:middle:bottom:baseline:],height,\"\n"
							+ "html.tag.u=\"\"\n"
							+ "html.tag.ul=\",compact[compact],type[:disc:square:circle:],\"\n"
							+ "\n"
							+ "\n"
							+ "# CSS key-value-pairs.\n"
							+ "# An empty value indicates a reference to style's combi-map.\n"
							+ "# Placeholders:\n"
							+ "# c: Any CSS color value\n"
							+ "# u: An URL; e.g. url(http://www.somewhere.com/myimage.jpg);\n"
							+ "# n: Any CSS number value without '%'\n"
							+ "# N: Any CSS number value\n"
							+ "# *: Any value allowed\n"
							+ "# d: delete\n"
							+ "# t: time\n"
							+ "\n"
							+ "html.style.azimuth=\",left-side,left-side behind,far-left,far-left behind,left,left behind,center-left,center-left behind,center,center behind,center-right,center-right behind,right,right behind,far-right,far-right behind,right-side,right behind,\"\n"
							+ "html.style.background=\"\"\n"
							+ "html.style.background-attachment=\",scroll,fixed,\"\n"
							+ "html.style.background-color=\"c,transparent,\"\n"
							+ "html.style.background-image=\"u\"\n"
							+ "html.style.background-position=\",top,bottom,center,left,right,\"\n"
							+ "html.style.background-repeat=\",repeat,repeat-x,repeat-y,no-repeat,\"\n"
							+ "html.style.border=\"\"\n"
							+ "html.style.border-bottom=\"\"\n"
							+ "html.style.border-bottom-color=\"c,transparent,\"\n"
							+ "html.style.border-bottom-style=\",none,hidden,dotted,dashed,solid,double,groove,ridge,inset,outset,\"\n"
							+ "html.style.border-bottom-width=\"n\"\n"
							+ "html.style.border-collapse=\",separate,collapse,\"\n"
							+ "html.style.border-color=\"c,transparent,\"\n"
							+ "html.style.border-left=\"\"\n"
							+ "html.style.border-left-color=\"c,transparent,\"\n"
							+ "html.style.border-left-style=\",none,hidden,dotted,dashed,solid,double,groove,ridge,inset,outset,\"\n"
							+ "html.style.border-left-width=\"n\"\n"
							+ "html.style.border-right=\"\"\n"
							+ "html.style.border-right-color=\"c,transparent,\"\n"
							+ "html.style.border-right-style=\",none,hidden,dotted,dashed,solid,double,groove,ridge,inset,outset,\"\n"
							+ "html.style.border-right-width=\"n\"\n"
							+ "html.style.border-spacing=\"N\"\n"
							+ "html.style.border-style=\"\"\n"
							+ "html.style.border-top=\"\"\n"
							+ "html.style.border-top-color=\"c,transparent,\"\n"
							+ "html.style.border-top-style=\",none,hidden,dotted,dashed,solid,double,groove,ridge,inset,outset,\"\n"
							+ "html.style.border-top-width=\"n\"\n"
							+ "html.style.border-width=\"\"\n"
							+ "html.style.bottom=\"N,auto,\"\n"
							+ "html.style.caption-side=\",top,bottom,left,right,\"\n"
							+ "html.style.centerline=\"d\"\n"
							+ "html.style.clear=\",left,right,both,none,\"\n"
							+ "html.style.clip=\"d\"\n"
							+ "html.style.color=\"c,transparent,\"\n"
							+ "html.style.content=\"d\"\n"
							+ "html.style.counter-increment=\"d\"\n"
							+ "html.style.counter-reset=\"d\"\n"
							+ "html.style.counter=\"d\"\n"
							+ "html.style.cue=\"u\"\n"
							+ "html.style.cue-after=\"u\"\n"
							+ "html.style.cue-before=\"u\"\n"
							+ "html.style.cursor=\",auto,default,crosshair,pointer,move,n-resize,ne-resize,e-resize,se-resize,s-resize,sw-resize,w-resize,nw-resize,text,wait,help,\"\n"
							+ "html.style.definition-src=\"d\"\n"
							+ "html.style.direction=\",ltr,rtl,\"\n"
							+ "html.style.display=\",block,inline,list-item,marker,run-in,compact,none,table,inline-table,table-row,table-cell,table-row-group,table-header-group,table-footer-group,table-column,table-column-group,table-caption,\"\n"
							+ "html.style.empty-cells=\",show,hide,\"\n"
							+ "html.style.elevation=\",below,level,above,higher,lower,\"\n"
							+ "html.style.filter=\"d\" \n"
							+ "html.style.float=\",left,right,none,\"\n"
							+ "html.style.font=\"\"\n"
							+ "html.style.font-family=\"*\"\n"
							+ "html.style.font-color=\"c,transparent,\"\n"
							+ "html.style.font-size=\"N,xx-small,x-small,small,medium,large,x-large,xx-large,smaller,larger,\"\n"
							+ "html.style.font-stretch=\",wider,narrower,condensed,semi-condensed,extra-condensed,ultra-condensed,expanded,semi-expanded,extra-expanded,ultra-expanded,normal,\"\n"
							+ "html.style.font-style=\",italic,oblique,normal,\"\n"
							+ "html.style.font-variant=\",small-caps,normal,\"\n"
							+ "html.style.font-weight=\",bold,bolder,lighter,100,200,300,400,500,600,700,800,900,normal,\"\n"
							+ "html.style.height=\"N,auto,\"\n"
							+ "html.style.left=\"N,auto,\"\n"
							+ "html.style.letter-spacing=\"n\"\n"
							+ "html.style.line-height=\"N\"\n"
							+ "html.style.list-style=\"\"	\n"
							+ "html.style.list-style-image=\"u,none,\"\n"
							+ "html.style.list-style-position=\",inside,outside,\"\n"
							+ "html.style.list-style-type=\",decimal,lower-roman,upper-roman,lower-alpha,lower-latin,upper-alpha,upper-latin,disc,circle,square,none,lower-greek,hebrew,decimal-leading-zero,cjk-ideographic,hiragana,katakana,hiragana-iroha,katakana-iroha,\"\n"
							+ "html.style.margin=\"\"\n"
							+ "html.style.margin-bottom=\"N,auto,inherit,\"\n"
							+ "html.style.margin-left=\"N,auto,inherit,\"\n"
							+ "html.style.margin-right=\"N,auto,inherit,\"\n"
							+ "html.style.margin-top=\"N,auto,inherit,\"\n"
							+ "html.style.max-height=\"N\"\n"
							+ "html.style.max-width=\"N\"\n"
							+ "html.style.min-height=\"N\"\n"
							+ "html.style.min-width=\"N\"\n"
							+ "html.style.orphans=\"0\"\n"
							+ "html.style.overflow=\",visible,hidden,scroll,auto,\"\n"
							+ "html.style.padding=\"\"\n"
							+ "html.style.padding-bottom=\"N\"\n"
							+ "html.style.padding-left=\"N\"\n"
							+ "html.style.padding-right=\"N\"\n"
							+ "html.style.padding-top=\"N\"\n"
							+ "html.style.page-break-after=\",always,avoid,left,right,inherit,auto,\"\n"
							+ "html.style.page-break-before=\",always,avoid,left,right,inherit,auto,\"\n"
							+ "html.style.page-break-inside=\",avoid,auto,\"\n"
							+ "html.style.pause=\"t\"\n"
							+ "html.style.pause-after=\"t\"\n"
							+ "html.style.pause-before=\"t\"\n"
							+ "html.style.pitch=\",x-low,low,medium,high,x-high,\"\n"
							+ "html.style.pitch-range=\"0\"\n"
							+ "html.style.play-during=\"u,mix,repeat,auto,\"\n"
							+ "html.style.position=\",absolute,fixed,relative,static,\"\n"
							+ "html.style.quotes=\"d\"\n"
							+ "html.style.richness=\"0\"\n"
							+ "html.style.right=\"N,auto,\"\n"
							+ "html.style.scrollbar-3dlight-color=\"c\"\n"
							+ "html.style.scrollbar-arrow-color=\"c\"\n"
							+ "html.style.scrollbar-base-color=\"c\"\n"
							+ "html.style.scrollbar-darkshadow-color=\"c\"\n"
							+ "html.style.scrollbar-face-color=\"c\"\n"
							+ "html.style.scrollbar-highlight-color=\"c\"\n"
							+ "html.style.scrollbar-shadow-color=\"c\"\n"
							+ "html.style.scrollbar-track-color=\"c\"\n"
							+ "html.style.speak=\",none,normal,spell-out,\"\n"
							+ "html.style.speak-header=\",always,once,\"\n"
							+ "html.style.speak-numeral=\",digits,continuous,\"\n"
							+ "html.style.speak-punctuation=\",code,none,\"\n"
							+ "html.style.speech-rate=\"0,x-slow,slow,slower,medium,faster,fast,x-fase,\"\n"
							+ "html.style.stress=\"0\"\n"
							+ "html.style.table-layout=\",auto,fixed,\"\n"
							+ "html.style.text-align=\",left,center,right,justify,\"\n"
							+ "html.style.text-decoration=\",underline,overline,line-through,blink,none,\"\n"
							+ "html.style.text-indent=\"N\"\n"
							+ "html.style.text-shadow=\"nc,none,\" or color\n"
							+ "html.style.text-transform=\",capitalize,uppercase,lowercase,none,\"\n"
							+ "html.style.top=\"N,auto,\"\n"
							+ "html.style.vertical-align=\",top,middle,bottom,baseline,sub,super,text-top,text-bottom,\"\n"
							+ "html.style.visibility=\",hidden,visible,\"\n"
							+ "html.style.voice-family=\",male,female,old,young,child,\"\n"
							+ "html.style.volume=\"0,silent,x-soft,soft,medium,loud,x-loud,\"\n"
							+ "html.style.white-space=\",normal,pre,nowrap,\"\n"
							+ "html.style.widows=\"0\"\n"
							+ "html.style.width=\"N,auto,\"\n"
							+ "html.style.word-spacing=\"n\"\n"
							+ "html.style.z-index=\"0\"\n"
							+ "\n"
							+ "\n"
							+ "# CSS combi-map\n"
							+ "\n"
							+ "html.style.combimap.background=\"uNc,scroll,fixed,transparent,top,bottom,center,left,right,repeat,repeat-x,repeat-y,no-repeat,\"\n"
							+ "html.style.combimap.border=\"Nc,transparent,none,hidden,dotted,dashed,solid,double,groove,ridge,inset,outset,separate,collapse,\"\n"
							+ "html.style.combimap.border-bottom=\"nc,transparent,none,hidden,dotted,dashed,solid,double,groove,ridge,inset,outset,\"\n"
							+ "html.style.combimap.border-left=\"nc,transparent,none,hidden,dotted,dashed,solid,double,groove,ridge,inset,outset,\"\n"
							+ "html.style.combimap.border-right=\"nc,transparent,none,hidden,dotted,dashed,solid,double,groove,ridge,inset,outset,\"\n"
							+ "html.style.combimap.border-style=\",none,hidden,dotted,dashed,solid,double,groove,ridge,inset,outset,\"\n"
							+ "html.style.combimap.border-top=\"nc,transparent,none,hidden,dotted,dashed,solid,double,groove,ridge,inset,outset,\"\n"
							+ "html.style.combimap.border-width=\"n\"\n"
							+ "html.style.combimap.font=\"N*,xx-small,x-small,small,medium,large,x-large,xx-large,smaller,larger,wider,narrower,condensed,semi-condensed,extra-condensed,ultra-condensed,expanded,semi-expanded,extra-expanded,ultra-expanded,normal,italic,oblique,small-caps,bold,bolder,lighter,100,200,300,400,500,600,700,800,900,\"\n"
							+ "html.style.combimap.list-style=\"u,none,inside,outside,decimal,lower-roman,upper-roman,lower-alpha,lower-latin,upper-alpha,upper-latin,disc,circle,square,lower-greek,hebrew,decimal-leading-zero,cjk-ideographic,hiragana,katakana,hiragana-iroha,katakana-iroha,\"\n"
							+ "html.style.combimap.margin=\"N,auto,inherit,\"\n"
							+ "html.style.combimap.padding=\"N\"\n").getBytes();

	private static final Pattern PATTERN_TAG_LINE = Pattern.compile(
			"html\\.tag\\.(\\p{Alnum}+)\\s*=\\s*\"(\\p{Print}*)\"", Pattern.CASE_INSENSITIVE);

	private static final Pattern PATTERN_ATTRIBUTE = Pattern
			.compile("([\\p{Alnum}-_]+)(?:\\[([\\p{Print}&&[^\\]]]*)\\])?");

	/**
	 * Parses specified HTML map
	 * 
	 * @param htmlMapStr
	 *            The HTML map string
	 * @return The parsed map
	 */
	private static Map<String, Map<String, Set<String>>> parseHTMLMap(final String htmlMapStr) {
		final Matcher m = PATTERN_TAG_LINE.matcher(htmlMapStr);
		final Map<String, Map<String, Set<String>>> tagMap = new HashMap<String, Map<String, Set<String>>>();
		while (m.find()) {
			final Matcher attribMatcher = PATTERN_ATTRIBUTE.matcher(m.group(2));
			final Map<String, Set<String>> attribMap = new HashMap<String, Set<String>>();
			while (attribMatcher.find()) {
				final String values = attribMatcher.group(2);
				if (null == values) {
					attribMap.put(attribMatcher.group(1).toLowerCase(Locale.ENGLISH), null);
				} else if (values.length() == 0) {
					attribMap.put(attribMatcher.group(1).toLowerCase(Locale.ENGLISH), NUM_ATTRIBS);
				} else {
					final Set<String> valueSet = new HashSet<String>();
					final String[] valArr = values.charAt(0) == ':' ? values.substring(1).split("\\s*:\\s*") : values
							.split("\\s*:\\s*");
					for (final String value : valArr) {
						valueSet.add(value.toLowerCase(Locale.ENGLISH));
					}
					attribMap.put(attribMatcher.group(1).toLowerCase(Locale.ENGLISH), valueSet);
				}
			}
			tagMap.put(m.group(1).toLowerCase(Locale.ENGLISH), attribMap);
		}
		return tagMap;
	}

	private static final Pattern PATTERN_STYLE_LINE = Pattern.compile(
			"html\\.style\\.([\\p{Alnum}-_]+)\\s*=\\s*\"([\\p{Print}&&[^\"]]*)\"", Pattern.CASE_INSENSITIVE);

	private static final Pattern PATTERN_VALUE = Pattern.compile("([\\p{Alnum}*-_ &&[^,]]+)");

	private static Map<String, Set<String>> parseStyleMap(final String styleMapStr) {
		/*
		 * Parse the combination map
		 */
		final Map<String, Set<String>> combiMap = parseCombiMap(styleMapStr);
		/*
		 * Parse style map
		 */
		final Matcher m = PATTERN_STYLE_LINE.matcher(styleMapStr);
		final Map<String, Set<String>> styleMap = new HashMap<String, Set<String>>();
		while (m.find()) {
			final String values = m.group(2);
			if (values.length() == 0) {
				/*
				 * Fetch from combination map
				 */
				final String cssElement = m.group(1).toLowerCase(Locale.ENGLISH);
				styleMap.put(cssElement, combiMap.get(cssElement));
			} else {
				/*
				 * Parse values
				 */
				final Matcher valueMatcher = PATTERN_VALUE.matcher(m.group(2));
				final Set<String> valueSet = new HashSet<String>();
				while (valueMatcher.find()) {
					valueSet.add(valueMatcher.group());
				}
				styleMap.put(m.group(1).toLowerCase(Locale.ENGLISH), valueSet);
			}
		}
		return styleMap;
	}

	private static final Pattern PATTERN_COMBI_LINE = Pattern.compile(
			"html\\.style\\.combimap\\.([\\p{Alnum}-_]+)\\s*=\\s*\"([\\p{Print}&&[^\"]]+)\"", Pattern.CASE_INSENSITIVE);

	/**
	 * Parses specified combination map for CSS elements
	 * 
	 * @param combiMapStr
	 *            The string representation for combination map
	 * @return The parsed map
	 */
	private static Map<String, Set<String>> parseCombiMap(final String combiMapStr) {
		final Matcher m = PATTERN_COMBI_LINE.matcher(combiMapStr);
		final Map<String, Set<String>> combiMap = new HashMap<String, Set<String>>();
		while (m.find()) {
			final Matcher valueMatcher = PATTERN_VALUE.matcher(m.group(2));
			final Set<String> valueSet = new HashSet<String>();
			while (valueMatcher.find()) {
				valueSet.add(valueMatcher.group());
			}
			combiMap.put(m.group(1).toLowerCase(Locale.ENGLISH), valueSet);
		}
		return combiMap;
	}
}
