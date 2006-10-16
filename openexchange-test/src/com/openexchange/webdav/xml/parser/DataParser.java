/*
 *
 *    OPEN-XCHANGE - "the communication and information enviroment"
 *
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    OPEN-XCHANGE is a trademark of Netline Internet Service GmbH and all
 *    other brand and product names are or may be trademarks of, and are
 *    used to identify products or services of, their respective owners.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original code will still remain
 *    copyrighted by the copyright holder(s) or original author(s).
 *
 *
 *     Copyright (C) 1998 - 2005 Netline Internet Service GmbH
 *     mail:	                 info@netline-is.de
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License as published by the Free
 *     Software Foundation; either version 2 of the License, or (at your option)
 *     any later version.
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
 *
 
 */

package com.openexchange.webdav.xml.parser;

import com.openexchange.groupware.container.DataObject;
import com.openexchange.webdav.xml.XmlServlet;
import com.openexchange.webdav.xml.fields.DataFields;
import java.io.IOException;
import java.util.Date;
import org.jdom.Element;
import org.xmlpull.v1.XmlPullParserException;

/**
 * DataParser
 *
 * @author <a href="mailto:sebastian.kauss@netline-is.de">Sebastian Kauss</a>
 */

public abstract class DataParser {
	
	protected void parseElement(DataObject dataobject, Element eProp) throws Exception {
		if (hasElement(eProp.getChild(DataFields.OBJECT_ID, XmlServlet.NS))) {
			dataobject.setObjectID(getValueAsInt(eProp.getChild(DataFields.OBJECT_ID, XmlServlet.NS)));
		} 

		if (hasElement(eProp.getChild(DataFields.CREATED_BY, XmlServlet.NS))) {
			dataobject.setCreatedBy(getValueAsInt(eProp.getChild(DataFields.CREATED_BY, XmlServlet.NS)));
		} 
		
		if (hasElement(eProp.getChild(DataFields.LAST_MODIFIED, XmlServlet.NS))) {
			dataobject.setLastModified(getValueAsDate(eProp.getChild(DataFields.LAST_MODIFIED, XmlServlet.NS)));
		} 
	}
	
	public static int getValueAsInt(Element e) throws XmlPullParserException, IOException {
		String s = null;
		
		if ((s = e.getValue()) != null && s.length() > 0) {
			return Integer.parseInt(s);
		} else {
			return 0;
		}
	}
	
	public static float getValueAsFloat(Element e) throws Exception {
		String s = null;
		
		if ((s = e.getValue()) != null && s.length() > 0) {
			return Float.parseFloat(s);
		} else {
			return 0;
		}
	}
	
	public static long getValueAsLong(Element e) throws Exception {
		String s = null;
		
		if ((s = e.getValue()) != null && s.length() > 0) {
			return Long.parseLong(s);
		} else {
			return 0;
		}
	}
	
	public static Date getValueAsDate(Element e) throws Exception {
		String s = null;
		
		if ((s = e.getValue()) != null && s.length() > 0) {
			return new Date(Long.parseLong(s));
		} else {
			return null;
		}
	}
	
	public static boolean getValueAsBoolean(Element e) throws Exception {
		String s = null;
		
		if ((s = e.getValue()) != null && s.equalsIgnoreCase("true")) {
			return true;
		}
		return false;
	}
	
	public static String getValue(Element e) throws Exception {
		String s = e.getValue();
		
		if (s != null && s.length() == 0) {
			return null;
		} 
		return s;
	}
	
	public static boolean hasElement(Element e) throws Exception {
		return (e != null);
	}
}




