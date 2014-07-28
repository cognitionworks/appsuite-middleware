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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.database.Databases;
import com.openexchange.databaseold.Database;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.jslob.DefaultJSlob;
import com.openexchange.jslob.JSlobId;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.mime.MimeMailExceptionCode;
import com.openexchange.mailaccount.Attribute;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountDescription;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.Tools;
import com.openexchange.mailaccount.json.fields.MailAccountFields;
import com.openexchange.mailaccount.json.parser.MailAccountParser;
import com.openexchange.mailaccount.json.writer.MailAccountWriter;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link NewAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@Action(method = RequestMethod.PUT, name = "new", description = "Create a new mail account", parameters = {
    @Parameter(name = "session", description = "A session ID previously obtained from the login module.")
}, requestBody = "A JSON object describing the new account to create. See mail account data.",
    responseDescription = "A JSON object representing the inserted mail account. See mail account data.")
public final class NewAction extends AbstractMailAccountAction implements MailAccountFields {

    public static final String ACTION = AJAXServlet.ACTION_NEW;

    /**
     * Initializes a new {@link NewAction}.
     */
    public NewAction() {
        super();
    }

    @Override
    protected AJAXRequestResult innerPerform(final AJAXRequestData requestData, final ServerSession session, final JSONValue jData) throws OXException, JSONException {
        if (!session.getUserPermissionBits().isMultipleMailAccounts()) {
            throw
            MailAccountExceptionCodes.NOT_ENABLED.create(
                Integer.valueOf(session.getUserId()),
                Integer.valueOf(session.getContextId()));
        }

        final MailAccountDescription accountDescription = new MailAccountDescription();
        final List<OXException> warnings = new LinkedList<OXException>();
        final Set<Attribute> fieldsToUpdate = MailAccountParser.getInstance().parse(accountDescription, jData.toObject(), warnings, true);

        checkNeededFields(accountDescription);

        // Check if account denotes a Unified Mail account
        if (isUnifiedINBOXAccount(accountDescription.getMailProtocol())) {
            // Deny creation of Unified Mail account
            throw MailAccountExceptionCodes.UNIFIED_INBOX_ACCOUNT_CREATION_FAILED.create(accountDescription.getId());
        }

        {
            final String name = accountDescription.getName();
            if (isEmpty(name) || "null".equalsIgnoreCase(name)) {
                accountDescription.setName(accountDescription.getPrimaryAddress());
            }
        }

        final MailAccountStorageService storageService =
            ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class, true);

        // Don't check for POP3 account due to access restrictions (login only allowed every n minutes)
        final boolean pop3 = Strings.toLowerCase(accountDescription.getMailProtocol()).startsWith("pop3");
        if (!pop3) {
            session.setParameter("mail-account.validate.type", "create");
            try {
                if (!ValidateAction.actionValidateBoolean(accountDescription, session, false, warnings).booleanValue()) {
                    final OXException warning = MimeMailExceptionCode.CONNECT_ERROR.create(accountDescription.getMailServer(), accountDescription.getLogin());
                    warning.setCategory(Category.CATEGORY_WARNING);
                    warnings.add(0, warning);
                }
            } finally {
                session.setParameter("mail-account.validate.type", null);
            }
        }

        // Check standard folder names against full names
        if (!pop3) {
            MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
            try {
                mailAccess = getMailAccess(accountDescription, session, warnings);
                mailAccess.connect(false);
                Tools.checkNames(accountDescription, fieldsToUpdate, mailAccess.getFolderStorage().getFolder("INBOX").getSeparator());
            } finally {
                if (null != mailAccess) {
                    mailAccess.close(false);
                }
            }
        }

        final int cid = session.getContextId();
        final int id;
        MailAccount newAccount = null;
        {
            final Connection wcon = Database.get(cid, true);
            boolean rollback = false;
            try {
                Databases.startTransaction(wcon);
                rollback = true;
                id = storageService.insertMailAccount(accountDescription, session.getUserId(), session.getContext(), session, wcon);
                // Check full names after successful creation
                final MailAccount[] accounts = storageService.getUserMailAccounts(session.getUserId(), cid, wcon);
                for (final MailAccount mailAccount : accounts) {
                    if (mailAccount.getId() == id) {
                        newAccount = mailAccount;
                        break;
                    }
                }

                if (null != newAccount) {
                    newAccount = checkFullNames(newAccount, storageService, session, wcon);
                }

                wcon.commit();
                rollback = false;
            } catch (final SQLException e) {
                throw MailAccountExceptionCodes.SQL_ERROR.create(e, e.getMessage());
            } catch (final RuntimeException e) {
                throw MailAccountExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
            } finally {
                if (rollback) {
                    rollback(wcon);
                }
                autocommit(wcon);
                Database.back(cid, true, wcon);
            }
        }

        {
            final JSONObject jBody = jData.toObject();
            if (jBody.hasAndNotNull(META)) {
                final JSONObject jMeta = jBody.optJSONObject(META);
                getStorage().store(new JSlobId(JSLOB_SERVICE_ID, Integer.toString(id), session.getUserId(), session.getContextId()), new DefaultJSlob(jMeta));
            }
        }

        final JSONObject jsonAccount;
        if (null == newAccount) {
            final MailAccount loadedMailAccount = storageService.getMailAccount(id, session.getUserId(), session.getContextId());
            if (null == loadedMailAccount) {
                throw MailAccountExceptionCodes.NOT_FOUND.create(id, session.getUserId(), session.getContextId());
            }
            jsonAccount = MailAccountWriter.write(checkFullNames(loadedMailAccount, storageService, session));
        } else {
            jsonAccount = MailAccountWriter.write(newAccount);
        }

        return new AJAXRequestResult(jsonAccount).addWarnings(warnings);
    }

}
