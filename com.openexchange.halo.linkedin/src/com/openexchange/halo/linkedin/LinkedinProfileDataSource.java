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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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
package com.openexchange.halo.linkedin;

import java.util.List;

import org.json.JSONObject;

import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.halo.HaloContactDataSource;
import com.openexchange.halo.HaloContactQuery;
import com.openexchange.oauth.OAuthAccount;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;


public class LinkedinProfileDataSource extends AbstractLinkedinDataSource implements HaloContactDataSource {

	public LinkedinProfileDataSource(ServiceLookup serviceLookup) {
		this.serviceLookup = serviceLookup;
	}

	@Override
	public String getId() {
		return "com.openexchange.halo.linkedIn.fullProfile";
	}

	@Override
	public AJAXRequestResult investigate(HaloContactQuery query, AJAXRequestData req, ServerSession session) throws OXException {
		String password = session.getPassword();
		int uid = session.getUserId();
		int cid = session.getContextId();
		
		String email = query.getContact().getEmail1();

		List<OAuthAccount> accounts = getOauthService().getAccounts("com.openexchange.socialplugin.linkedin", password, uid, cid);
		if(accounts.size() == 0)
			throw new OXException(1).setPrefix("HAL-LI").setLogMessage("Need at least 1 LinkedIn account");
		
		OAuthAccount linkedinAccount = accounts.get(0);
		JSONObject json = getLinkedinService().getFullProfileByEMail(email, password, uid, cid, linkedinAccount.getId());
		AJAXRequestResult result = new AJAXRequestResult();
		result.setResultObject(json, "json");
		return result;
	}
}
