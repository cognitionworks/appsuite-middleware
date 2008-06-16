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

package com.openexchange.webdav.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;
import com.openexchange.webdav.action.ifheader.IfHeader;
import com.openexchange.webdav.action.ifheader.IfHeaderParseException;
import com.openexchange.webdav.protocol.WebdavCollection;
import com.openexchange.webdav.protocol.WebdavException;
import com.openexchange.webdav.protocol.WebdavFactory;
import com.openexchange.webdav.protocol.WebdavPath;
import com.openexchange.webdav.protocol.WebdavResource;

public class ReplayWebdavRequest implements WebdavRequest{
	private final WebdavRequest delegate;
	private byte[] body;
	
	public ReplayWebdavRequest(final WebdavRequest req) {
		this.delegate = req;
	}

	public InputStream getBody() throws IOException {
		if(this.body != null) {
			return new ByteArrayInputStream(this.body);
		}
		
		final ByteArrayOutputStream bout = new UnsynchronizedByteArrayOutputStream();
		InputStream in = null;
		
		in = delegate.getBody();
		
		final byte[] buffer = new byte[200];
		int b = 0;
		
		while((b = in.read(buffer))!=-1) {
			bout.write(buffer,0,b);
		}
		this.body = bout.toByteArray();
		
		return new ByteArrayInputStream(this.body);
	}

	public Document getBodyAsDocument() throws JDOMException, IOException {
		return new SAXBuilder().build(getBody());
	}

	public WebdavCollection getCollection() throws WebdavException {
		return delegate.getCollection();
	}

	public WebdavResource getDestination() throws WebdavException {
		return delegate.getDestination();
	}

	public WebdavPath getDestinationUrl() {
		return delegate.getDestinationUrl();
	}

	public String getHeader(final String header) {
		return delegate.getHeader(header);
	}

	public List<String> getHeaderNames() {
		return delegate.getHeaderNames();
	}

	public IfHeader getIfHeader() throws IfHeaderParseException {
		return delegate.getIfHeader();
	}

	public WebdavResource getResource() throws WebdavException {
		return delegate.getResource();
	}

	public WebdavPath getUrl() {
		return delegate.getUrl();
	}

	public String getURLPrefix() {
		return delegate.getURLPrefix();
	}
	
	public int getDepth(final int depth) {
		return delegate.getDepth(depth);
	}

	public WebdavFactory getFactory() throws WebdavException {
		return delegate.getFactory();
	}

	public String getCharset() {
		return delegate.getCharset();
	}
}
