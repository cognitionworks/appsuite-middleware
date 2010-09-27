package com.openexchange.upsell.multiple.impl;

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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.openexchange.ajax.DataServlet;
import com.openexchange.ajax.container.Response;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.session.Session;
import com.openexchange.tools.servlet.OXJSONException;
import com.openexchange.tools.session.ServerSession;

/**
 * 
 * Servlet which returns needed Data for the Spamexperts Iframe Plugin to redirect
 * and authenticate to an external GUI.
 * 
 * Also does jobs for the other GUI Plugin
 * 
 * 
 * @author <a href="mailto:manuel.kraft@open-xchange.com">Manuel Kraft</a>
 * 
 */
public final class MyServlet extends DataServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8914926421736440078L;
	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(MyServlet.class);

	public MyServlet() {
		super();
	}

	@Override
	protected boolean hasModulePermission(ServerSession session) {
		return true;
	}

	protected void doGet(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {

		final Response response = new Response();
		
		try {
			
			final String action = parseMandatoryStringParameter(req,PARAMETER_ACTION);
			final Session session = getSessionObject(req);
			JSONObject jsonObj;

			try {
				jsonObj = convertParameter2JSONObject(req);
			} catch (final JSONException e) {
				LOG.error(e.getMessage(), e);
				response.setException(new OXJSONException(OXJSONException.Code.JSON_BUILD_ERROR, e));
				writeResponse(response, resp);
				return;
			}
			final Context ctx = ContextStorage.getInstance().getContext(session);
			final MyServletRequest proRequest = new MyServletRequest(session, ctx);
			final Object responseObj = proRequest.action(action, jsonObj,req);
			response.setData(responseObj);

		} catch (final AbstractOXException e) {
			LOG.error(e.getMessage(), e);
			response.setException(e);
		} catch (final JSONException e) {
			final OXJSONException oje = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e);
			LOG.error(oje.getMessage(), oje);
			response.setException(oje);
		}

		writeResponse(response, resp);

	}

}
