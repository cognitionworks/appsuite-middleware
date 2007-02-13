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

import com.openexchange.groupware.attach.AttachmentMetadata;
import com.openexchange.groupware.container.CommonObject;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.webdav.xml.fields.CommonFields;
import java.net.URLEncoder;
import org.jdom.Element;

/**
 * CommonWriter
 *
 * @author <a href="mailto:sebastian.kauss@netline-is.de">Sebastian Kauss</a>
 */

public abstract class CommonWriter extends FolderChildWriter {
	
	protected void writeCommonElements(CommonObject commonobject, Element e_prop) throws Exception {
		
		if (commonobject.containsParentFolderID() && commonobject.getParentFolderID() == 0) {
			addElement("personal_folder_id", commonobject.getParentFolderID(), e_prop);
			commonobject.setParentFolderID(-1);
		}
		
		if (commonobject.getNumberOfAttachments() > 0) {
			writeElementAttachments(commonobject, e_prop);
		}
		
		writeFolderChildElements(commonobject, e_prop);
		
		addElement("categories", commonobject.getCategories(), e_prop);
		addElement("private_flag", commonobject.getPrivateFlag(), e_prop);
	}
	
	protected void writeElementAttachments(CommonObject commonobject, Element e_prop) throws Exception {
		Element e_attachments = new Element("attachments", XmlServlet.NS);
		SearchIterator it = null;
		try {
			XmlServlet.attachmentBase.startTransaction();
			TimedResult tResult = XmlServlet.attachmentBase.getAttachments(commonobject.getParentFolderID(), commonobject.getObjectID(), getModule(), sessionObj.getContext(), sessionObj.getUserObject(), sessionObj.getUserConfiguration());
			
			it = tResult.results();
			
			while (it.hasNext()) {
				AttachmentMetadata attachmentMeta = (AttachmentMetadata)it.next();
				
				Element e = new Element("attachment", XmlServlet.NS);
				
				String filename = attachmentMeta.getFilename();
				
				if (filename != null) {
					filename = URLEncoder.encode(filename, "UTF-8");
				}
				
				e.addContent(filename);
				e.setAttribute("id", String.valueOf(attachmentMeta.getId()), XmlServlet.NS);
				e.setAttribute("last_modified", String.valueOf(attachmentMeta.getCreationDate().getTime()), XmlServlet.NS);
				e.setAttribute("mimetype", attachmentMeta.getFileMIMEType(), XmlServlet.NS);
				e.setAttribute("rtf_flag", String.valueOf(attachmentMeta.getRtfFlag()), XmlServlet.NS);
				
				e_attachments.addContent(e);
			}
		} finally {
			if(it != null) {
				it.close();
			} 
			
			XmlServlet.attachmentBase.commit();
			XmlServlet.attachmentBase.finish();
		}
		
		e_prop.addContent(e_attachments);
	}
	
	protected abstract int getModule();
}




