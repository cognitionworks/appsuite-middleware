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

package com.openexchange.chat.json.account.action;

import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.chat.ChatAccount;
import com.openexchange.chat.ChatAccountManager;
import com.openexchange.chat.ChatService;
import com.openexchange.chat.ChatServiceRegistry;
import com.openexchange.chat.json.account.AccountParser;
import com.openexchange.chat.json.account.AccountWriter;
import com.openexchange.chat.json.account.ChatAccountAJAXRequest;
import com.openexchange.chat.json.account.ParsedAccount;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link NewAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class NewAction extends AbstractChatAccountAction {

    /**
     * Initializes a new {@link NewAction}.
     */
    public NewAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    public AJAXRequestResult perform(final ChatAccountAJAXRequest request) throws OXException, JSONException {
        /*
         * The meta data identifier
         */
        final String serviceId = request.checkParameter("serviceId");
        final ServerSession session = request.getSession();
        /*
         * Parse account
         */
        final ParsedAccount parsedAccount = AccountParser.parse(request.<JSONObject> getData());
        /*
         * Get service
         */
        final ChatServiceRegistry registry = getService(ChatServiceRegistry.class);
        final ChatService chatService = registry.getChatService(serviceId, session.getUserId(), session.getContextId());
        parsedAccount.setChatService(chatService);
        final ChatAccountManager accountManager = chatService.getAccountManager();
        final String newId = accountManager.addAccount(parsedAccount, session);
        /*
         * Load account
         */
        final ChatAccount newAccount = accountManager.getAccount(newId, session);
        /*
         * Write as JSON
         */
        final JSONObject jsonAccount = AccountWriter.write(newAccount);
        /*
         * Return appropriate result
         */
        return new AJAXRequestResult(jsonAccount);
    }

}
