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

package com.openexchange.mailaccount.json.actions;

import org.json.JSONException;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountExceptionFactory;
import com.openexchange.mailaccount.MailAccountExceptionMessages;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.servlet.AjaxException;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link GetTreeAction}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class GetTreeAction extends AbstractMailAccountTreeAction {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.exception.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(GetTreeAction.class));

    public static final String ACTION = "get_tree";

    /**
     * Initializes a new {@link GetTreeAction}.
     */
    public GetTreeAction() {
        super();
    }

    public AJAXRequestResult perform(final AJAXRequestData request, final ServerSession session) throws AbstractOXException {
        final int id = parseIntParameter(AJAXServlet.PARAMETER_ID, request);

        try {
            final MailAccountStorageService storageService =
                ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class, true);

            final MailAccount mailAccount = storageService.getMailAccount(id, session.getUserId(), session.getContextId());

            if (isUnifiedINBOXAccount(mailAccount)) {
                // Treat as no hit
                throw MailAccountExceptionMessages.NOT_FOUND.create(
                    Integer.valueOf(id),
                    Integer.valueOf(session.getUserId()),
                    Integer.valueOf(session.getContextId()));
            }

            if (!session.getUserConfiguration().isMultipleMailAccounts() && !isDefaultMailAccount(mailAccount)) {
                throw MailAccountExceptionFactory.getInstance().create(
                    MailAccountExceptionMessages.NOT_ENABLED,
                    Integer.valueOf(session.getUserId()),
                    Integer.valueOf(session.getContextId()));
            }

            // Create a mail access instance
            final MailAccess<?, ?> mailAccess = MailAccess.getInstance(session, mailAccount.getId());
            return new AJAXRequestResult(actionValidateTree0(mailAccess, session));
        } catch (final JSONException e) {
            throw new AjaxException(AjaxException.Code.JSONError, e, e.getMessage());
        }
    }

}
