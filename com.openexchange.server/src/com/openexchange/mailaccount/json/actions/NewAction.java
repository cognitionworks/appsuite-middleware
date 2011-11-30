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

import static com.openexchange.tools.sql.DBUtils.autocommit;
import static com.openexchange.tools.sql.DBUtils.rollback;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.databaseold.Database;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.mail.mime.MIMEMailExceptionCode;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountDescription;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.json.parser.MailAccountParser;
import com.openexchange.mailaccount.json.writer.MailAccountWriter;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link NewAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class NewAction extends AbstractMailAccountAction {

    public static final String ACTION = AJAXServlet.ACTION_NEW;

    /**
     * Initializes a new {@link NewAction}.
     */
    public NewAction() {
        super();
    }

    @Override
    public AJAXRequestResult perform(final AJAXRequestData requestData, final ServerSession session) throws OXException {
        final JSONObject jData = (JSONObject) requestData.getData();

        try {
            if (!session.getUserConfiguration().isMultipleMailAccounts()) {
                throw
                    MailAccountExceptionCodes.NOT_ENABLED.create(
                    Integer.valueOf(session.getUserId()),
                    Integer.valueOf(session.getContextId()));
            }

            final MailAccountDescription accountDescription = new MailAccountDescription();
            new MailAccountParser().parse(accountDescription, jData);

            checkNeededFields(accountDescription);

            // Check if account denotes a Unified INBOX account
            if (isUnifiedINBOXAccount(accountDescription.getMailProtocol())) {
                // Deny creation of Unified INBOX account
                throw MailAccountExceptionCodes.CREATION_FAILED.create();
            }

            final MailAccountStorageService storageService =
                ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class, true);

            // List for possible warnings
            final List<OXException> warnings = new ArrayList<OXException>(2);

            {
                // Don't check for POP3 account due to access restrictions (login only allowed every n minutes)
                final boolean pop3 = accountDescription.getMailProtocol().toLowerCase(Locale.ENGLISH).startsWith("pop3");
                if (!pop3) {
                    session.setParameter("mail-account.validate.type", "create");
                    try {
                        if (!ValidateAction.actionValidateBoolean(accountDescription, session, warnings).booleanValue()) {
                            final OXException warning = MIMEMailExceptionCode.CONNECT_ERROR.create(accountDescription.getMailServer(), accountDescription.getLogin());
                            warning.setCategory(Category.CATEGORY_WARNING);
                            warnings.add(0, warning);
                        }
                    } finally {
                        session.setParameter("mail-account.validate.type", null);
                    }
                }
            }

            final int cid = session.getContextId();
            Connection con = null;
            try {
                con = Database.get(cid, true);
            } catch (final OXException e) {
                throw e;
            }
            final int id;
            MailAccount newAccount = null;
            try {
                con.setAutoCommit(false);
                id = storageService.insertMailAccount(accountDescription, session.getUserId(), session.getContext(), getSecret(session), con);
                // Check full names after successful creation
                final MailAccount[] accounts = storageService.getUserMailAccounts(session.getUserId(), cid, con);
                for (final MailAccount mailAccount : accounts) {
                    if (mailAccount.getId() == id) {
                        newAccount = mailAccount;
                        break;
                    }
                }
                con.commit();
            } catch (final SQLException e) {
                rollback(con);
                throw MailAccountExceptionCodes.SQL_ERROR.create(e, e.getMessage());
            } catch (final OXException e) {
                rollback(con);
                throw e;
            } catch (final RuntimeException e) {
                rollback(con);
                throw MailAccountExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
            } finally {
                autocommit(con);
                Database.back(cid, true, con);
            }

            if (null != newAccount) {
                checkFullNames(newAccount, storageService, session, null);
            }

            final JSONObject jsonAccount =
                MailAccountWriter.write(checkFullNames(
                    storageService.getMailAccount(id, session.getUserId(), session.getContextId()),
                    storageService,
                    session));

            return new AJAXRequestResult(jsonAccount).addWarnings(warnings);
        } catch (final JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create( e, e.getMessage());
        }
    }

}
