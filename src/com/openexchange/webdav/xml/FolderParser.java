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

import com.openexchange.api.OXConflictException;
import com.openexchange.api2.OXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.server.OCLPermission;
import com.openexchange.sessiond.SessionObject;
import com.openexchange.webdav.xml.fields.FolderFields;
import java.util.ArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * FolderParser
 *
 * @author <a href="mailto:sebastian.kauss@netline-is.de">Sebastian Kauss</a>
 */

public class FolderParser extends FolderChildParser {
	
	private static final Log LOG = LogFactory.getLog(FolderParser.class);
	
	public FolderParser(SessionObject sessionObj) {
		this.sessionObj = sessionObj;
	}
	
	public void parse(XmlPullParser parser, FolderObject folderobject) throws OXException, XmlPullParserException {
		try {
			while (true) {
				if (parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equals("prop")) {
					break;
				}
				
				parseElementFolder(folderobject, parser);
				parser.nextTag();
			}
		} catch (XmlPullParserException exc) {
			throw exc;
		} catch (Exception exc) {
			throw new OXException(exc);
		}
	}
	
	protected void parseElementFolder(FolderObject folderobject, XmlPullParser parser) throws Exception {
		if (!hasCorrectNamespace(parser)) {
			LOG.trace("unknown namespace in tag: " + parser.getName());
			parser.nextText();
			return ;
		}
		
		if (isTag(parser, FolderFields.TITLE)) {
			folderobject.setFolderName(getValue(parser));
			
			return ;
		} else if (isTag(parser, FolderFields.TYPE)) {
			String type = getValue(parser);
			if ("private".equals(type)) {
				folderobject.setType(FolderObject.PRIVATE);
			} else if ("public".equals(type)) {
				folderobject.setType(FolderObject.PUBLIC);
			} else {
				throw new OXConflictException("unknown value in " + FolderFields.TYPE + ": " + type);
			}
			
			return ;
		} else if (isTag(parser, FolderFields.MODULE)) {
			String module = getValue(parser);
			if ("calendar".equals(module)) {
				folderobject.setModule(FolderObject.CALENDAR);
			} else if ("contact".equals(module)) {
				folderobject.setModule(FolderObject.CONTACT);
			} else if ("task".equals(module)) {
				folderobject.setModule(FolderObject.TASK);
			} else if ("unbound".equals(module)) {
				folderobject.setModule(FolderObject.UNBOUND);
			} else {
				throw new OXConflictException("unknown value in " + FolderFields.MODULE + ": " + module);
			}
			
			return ;
		} else if (isTag(parser, FolderFields.PERMISSIONS)) {
			parseElementPermissions(folderobject, parser);
			
			return;
		} else {
			parseElementFolderChildObject(folderobject, parser);
		}
	}
	
	protected void parseElementPermissions(FolderObject folderobject, XmlPullParser parser) throws OXException {
		ArrayList permissions = new ArrayList();
		
		try {
			boolean isPermission = true;
			
			while (isPermission) {
				parser.nextTag();
				
				if (isEnd(parser)) {
					throw new OXConflictException("invalid xml in permission!");
				}
				
				if (parser.getName().equals(FolderFields.PERMISSIONS) && parser.getEventType() == XmlPullParser.END_TAG) {
					isPermission = false;
					break;
				}
				
				OCLPermission oclp = new OCLPermission();
				
				if (isTag(parser, "user")) {
					parseElementPermissionAttributes(oclp, parser);
					parseEntity(oclp, parser);
				} else if (isTag(parser, "group")) {
					parseElementPermissionAttributes(oclp, parser);
					parseEntity(oclp, parser);
					oclp.setGroupPermission(true);
				} else {
					throw new OXConflictException("unknown xml tag in permissions: " + parser.getName());
				}
				
				permissions.add(oclp);
			}
		} catch (Exception exc) {
			throw new OXException(exc);
		}
		
		folderobject.setPermissions(permissions);
	}
	
	protected void parseEntity(OCLPermission oclp, XmlPullParser parser) throws Exception {
		oclp.setEntity( getValueAsInt(parser));
	}
	
	protected void parseElementPermissionAttributes(OCLPermission oclp, XmlPullParser parser) throws Exception {
		int fp = getPermissionAttributeValue(parser, "folderpermission");
		int orp = getPermissionAttributeValue(parser, "objectreadpermission");
		int owp = getPermissionAttributeValue(parser, "objectwritepermission");
		int odp = getPermissionAttributeValue(parser, "objectdeletepermission");
		
		oclp.setAllPermission(fp, orp, owp, odp);
		oclp.setFolderAdmin(getPermissionAdminFlag(parser));
	}
	
	protected int getPermissionAttributeValue(XmlPullParser parser, String name) throws Exception {
		return Integer.parseInt(parser.getAttributeValue(XmlServlet.NAMESPACE, name));
	}
	
	protected boolean getPermissionAdminFlag(XmlPullParser parser) throws Exception {
		return Boolean.parseBoolean(parser.getAttributeValue(XmlServlet.NAMESPACE, "admin_flag"));
	}
}




