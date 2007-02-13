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



package com.openexchange.webdav.xml;

import com.openexchange.groupware.container.DataObject;
import com.openexchange.sessiond.SessionObject;
import com.openexchange.webdav.xml.fields.DataFields;
import java.io.IOException;
import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * DataParser
 *
 * @author <a href="mailto:sebastian.kauss@netline-is.de">Sebastian Kauss</a>
 */

public class DataParser {
	
	public static final int SAVE = 1;
	
	public static final int DELETE = 2;
	
	public static final int CONFIRM = 3;

	protected SessionObject sessionObj = null;
	
	protected String client_id = null;
	
	protected int method = SAVE;
	
	protected int inFolder = 0;
	
	private static final Log LOG = LogFactory.getLog(DataParser.class);
	
	protected void parseElement(DataObject dataobject, XmlPullParser parser) throws Exception {
		if (isTag(parser, DataFields.OBJECT_ID, XmlServlet.NAMESPACE)) {
			dataobject.setObjectID(getValueAsInt(parser));
		} else if (isTag(parser, DataFields.LAST_MODIFIED, XmlServlet.NAMESPACE)) {
			dataobject.setLastModified(getValueAsDate(parser));
		} else if (isTag(parser, "client_id", XmlServlet.NAMESPACE)) {
			client_id = getValue(parser);
		} else if (isTag(parser, "method", XmlServlet.NAMESPACE)) {
			String s = getValue(parser);
			if (s != null) {
				if (s.equalsIgnoreCase("save")) {
					method = SAVE;
				} else if (s.equalsIgnoreCase("delete")) {
					method = DELETE;
				} else if (s.equalsIgnoreCase("confirm")) {
					method = CONFIRM;
				}
			} 
		} else {
			LOG.trace("unknown xml tag: " + parser.getName());
			getValue(parser);
		}
	}
	
	protected boolean hasCorrectNamespace(XmlPullParser parser) throws Exception {
		if (parser.getEventType() == XmlPullParser.START_TAG) {
			if (parser.getNamespace().equals(XmlServlet.NAMESPACE)) {
				return true;
			}
		}
		return false;
	}
		
	public boolean isTag(XmlPullParser parser, String name) throws XmlPullParserException {
		return parser.getEventType() == XmlPullParser.START_TAG	&& (name == null || name.equals(parser.getName()));
	}
	
	public boolean isTag(XmlPullParser parser, String name, String namespace) throws XmlPullParserException {
		return parser.getEventType() == XmlPullParser.START_TAG	&& (name == null || name.equals(parser.getName()));
	}
	
	public String getClientID() {
		return client_id;
	}
	
	public int getMethod() {
		return method;
	}

	public int getFolder() {
		return inFolder;
	}
	
	public int getValueAsInt(XmlPullParser parser) throws XmlPullParserException, IOException {
		String s = null;
		
		if ((s = getValue(parser)) != null && s.length() > 0) {
			return Integer.parseInt(s);
		} else {
			return 0;
		}
	}
	
	public float getValueAsFloat(XmlPullParser parser) throws XmlPullParserException, IOException {
		String s = null;
		
		if ((s = getValue(parser)) != null && s.length() > 0) {
			return Float.parseFloat(s);
		} else {
			return 0;
		}
	}
	
	public long getValueAsLong(XmlPullParser parser) throws XmlPullParserException, IOException {
		String s = null;
		
		if ((s = getValue(parser)) != null && s.length() > 0) {
			return Long.parseLong(s);
		} else {
			return 0;
		}
	}
	
	public Date getValueAsDate(XmlPullParser parser) throws XmlPullParserException, IOException {
		String s = null;
		
		if ((s = getValue(parser)) != null && s.length() > 0) {
			return new Date(Long.parseLong(s));
		} else {
			return null;
		}
	}
	
	public boolean getValueAsBoolean(XmlPullParser parser) throws XmlPullParserException, IOException {
		String s = null;
		
		if ((s = getValue(parser)) != null && s.equalsIgnoreCase("true")) {
			return true;
		}
		return false;
	}
	
	public byte[] getValueAsByteArray(XmlPullParser parser) throws XmlPullParserException, IOException {
		String s = parser.nextText();
		
		if (s != null && s.length() == 0) {
			return null;
		} 
		return s.getBytes();
	}
	
	public String getValue(XmlPullParser parser) throws XmlPullParserException, IOException {
		String s = parser.nextText();
		
		if (s != null && s.length() == 0) {
			return null;
		} 
		return s;
	}
	
	public static boolean isEnd(XmlPullParser parser) throws XmlPullParserException {
		return (parser.getEventType() == XmlPullParser.END_DOCUMENT);
	}
}




