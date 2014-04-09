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
import static com.openexchange.tools.sql.DBUtils.closeSQLStuff;
import static com.openexchange.tools.sql.DBUtils.rollback;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
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
import com.openexchange.jslob.DefaultJSlob;
import com.openexchange.jslob.JSlobId;
import com.openexchange.mail.mime.MimeMailExceptionCode;
import com.openexchange.mailaccount.Attribute;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountDescription;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.json.fields.MailAccountFields;
import com.openexchange.mailaccount.json.parser.MailAccountParser;
import com.openexchange.mailaccount.json.writer.MailAccountWriter;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link UpdateAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@Action(method = RequestMethod.PUT, name = "update", description = "Update a mail account", parameters = { @Parameter(name = "session", description = "A session ID previously obtained from the login module.") }, requestBody = "A JSON object identifiying (field ID is present) and describing the account to update. See mail account data.", responseDescription = "A JSON object representing the updated mail account. See mail account data.")
public final class UpdateAction extends AbstractMailAccountAction implements MailAccountFields {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(UpdateAction.class);

    public static final String ACTION = AJAXServlet.ACTION_UPDATE;

    /**
     * Initializes a new {@link UpdateAction}.
     */
    public UpdateAction() {
        super();
    }

    private static final EnumSet<Attribute> DEFAULT = EnumSet.of(Attribute.CONFIRMED_HAM_FULLNAME_LITERAL, Attribute.CONFIRMED_HAM_LITERAL, Attribute.CONFIRMED_SPAM_FULLNAME_LITERAL, Attribute.CONFIRMED_SPAM_LITERAL, Attribute.DRAFTS_FULLNAME_LITERAL, Attribute.DRAFTS_LITERAL, Attribute.SENT_FULLNAME_LITERAL, Attribute.SENT_LITERAL, Attribute.SPAM_FULLNAME_LITERAL, Attribute.SPAM_LITERAL, Attribute.TRASH_FULLNAME_LITERAL, Attribute.TRASH_LITERAL);

    private static final Set<Attribute> WEBMAIL_ALLOWED = EnumSet.of(Attribute.ID_LITERAL, Attribute.PERSONAL_LITERAL, Attribute.REPLY_TO_LITERAL, Attribute.UNIFIED_INBOX_ENABLED_LITERAL);

    @Override
    protected AJAXRequestResult innerPerform(final AJAXRequestData requestData, final ServerSession session, final JSONValue jData) throws OXException, JSONException {
        final MailAccountDescription accountDescription = new MailAccountDescription();
        final List<OXException> warnings = new LinkedList<OXException>();
        final Set<Attribute> fieldsToUpdate = MailAccountParser.getInstance().parse(accountDescription, jData.toObject(), warnings);

        final int id = accountDescription.getId();
        if (-1 == id) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create(MailAccountFields.ID);
        }

        if (fieldsToUpdate.contains(Attribute.LOGIN_LITERAL)) {
            final String login = accountDescription.getLogin();
            if (isEmpty(login)) {
                throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create(MailAccountFields.LOGIN, null == login ? "null" : login);
            }
        }
        if (fieldsToUpdate.contains(Attribute.PASSWORD_LITERAL)) {
            final String pw = accountDescription.getPassword();
            if (MailAccount.DEFAULT_ID != id && isEmpty(pw)) {
                throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create(MailAccountFields.PASSWORD, null == pw ? "null" : pw);
            }
        }
        if (fieldsToUpdate.contains(Attribute.MAIL_URL_LITERAL)) {
            final String server = accountDescription.getMailServer();
            if (MailAccount.DEFAULT_ID != id && isEmpty(server)) {
                throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create(MailAccountFields.MAIL_URL, null == server ? "null" : server);
            }
        }

        final int contextId = session.getContextId();

        // Check attributes to update
        {
            final Set<Attribute> notAllowed = EnumSet.copyOf(fieldsToUpdate);
            notAllowed.removeAll(WEBMAIL_ALLOWED);
            if (!session.getUserPermissionBits().isMultipleMailAccounts() && (!isDefaultMailAccount(accountDescription) || (!notAllowed.isEmpty()))) {
                throw MailAccountExceptionCodes.NOT_ENABLED.create(Integer.valueOf(session.getUserId()), Integer.valueOf(contextId));
            }
        }

        // Acquire storage service
        final MailAccountStorageService storageService = ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class, true);

        // Get & check the account to update
        final MailAccount toUpdate = storageService.getMailAccount(id, session.getUserId(), contextId);
        if (isUnifiedINBOXAccount(toUpdate)) {
            // Treat as no hit
            throw MailAccountExceptionCodes.NOT_FOUND.create(Integer.valueOf(id), Integer.valueOf(session.getUserId()), Integer.valueOf(contextId));
        }

        // Check whether to clear POP3 account's time stamp
        boolean clearStamp = false;
        {
            // Don't check for POP3 account due to access restrictions (login only allowed every n minutes)
            final boolean pop3 = accountDescription.getMailProtocol().toLowerCase(Locale.ENGLISH).startsWith("pop3");
            if (fieldsToUpdate.contains(Attribute.MAIL_URL_LITERAL) && !toUpdate.generateMailServerURL().equals(accountDescription.generateMailServerURL())) {
                if (!pop3 && !ValidateAction.checkMailServerURL(accountDescription, session, warnings)) {
                    final OXException warning = MimeMailExceptionCode.CONNECT_ERROR.create(accountDescription.getMailServer(), accountDescription.getLogin());
                    warning.setCategory(Category.CATEGORY_WARNING);
                    warnings.add(0, warning);
                }
                clearStamp |= (pop3 && !toUpdate.getMailServer().equals(accountDescription.getMailServer()));
            }
            if (fieldsToUpdate.contains(Attribute.TRANSPORT_URL_LITERAL) && !toUpdate.generateTransportServerURL().equals(accountDescription.generateTransportServerURL())) {
                if (!pop3 && !ValidateAction.checkTransportServerURL(accountDescription, session, warnings)) {
                    final String transportLogin = accountDescription.getTransportLogin();
                    final OXException warning = MimeMailExceptionCode.CONNECT_ERROR.create(accountDescription.getTransportServer(), transportLogin == null ? accountDescription.getLogin() : transportLogin);
                    warning.setCategory(Category.CATEGORY_WARNING);
                    warnings.add(0, warning);
                }
                clearStamp |= (pop3 && !toUpdate.getTransportServer().equals(accountDescription.getTransportServer()));
            }
        }

        // Update
        MailAccount updatedAccount = null;
        {
            final Connection wcon = Database.get(contextId, true);
            boolean rollback = false;
            try {
                Databases.startTransaction(wcon);
                rollback = true;

                // Invoke update
                storageService.updateMailAccount(accountDescription, fieldsToUpdate, session.getUserId(), contextId, session, wcon, false);

                // Reload
                final MailAccount[] accounts = storageService.getUserMailAccounts(session.getUserId(), contextId, wcon);
                for (final MailAccount mailAccount : accounts) {
                    if (mailAccount.getId() == id) {
                        updatedAccount = mailAccount;
                        break;
                    }
                }

                // Any standard folders changed?
                if ((null != updatedAccount) && (fieldsToUpdate.removeAll(DEFAULT))) {
                    updatedAccount = checkFullNames(updatedAccount, storageService, session, wcon);
                }

                // Clear POP3 account's time stamp
                if (clearStamp) {
                    PreparedStatement stmt = null;
                    try {
                        // Delete possibly existing mapping
                        stmt = wcon.prepareStatement("DELETE FROM user_mail_account_properties WHERE cid = ? AND user = ? AND id = ? AND name = ?");
                        int pos = 1;
                        stmt.setInt(pos++, contextId);
                        stmt.setInt(pos++, session.getUserId());
                        stmt.setInt(pos++, id);
                        stmt.setString(pos++, "pop3.lastaccess");
                        stmt.executeUpdate();
                    } catch (final SQLException e) {
                        throw MailAccountExceptionCodes.SQL_ERROR.create(e, e.getMessage());
                    } finally {
                        closeSQLStuff(null, stmt);
                    }
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
                Database.back(contextId, true, wcon);
            }
        }

        // Check for possible meta information
        {
            final JSONObject jBody = jData.toObject();
            if (jBody.hasAndNotNull(META)) {
                final JSONObject jMeta = jBody.optJSONObject(META);
                getStorage().store(new JSlobId(JSLOB_SERVICE_ID, Integer.toString(id), session.getUserId(), session.getContextId()), new DefaultJSlob(jMeta));
            }
        }

        // Write to JSON structure
        final JSONObject jsonAccount;
        if (null == updatedAccount) {
            jsonAccount = MailAccountWriter.write(storageService.getMailAccount(id, session.getUserId(), contextId));
        } else {
            jsonAccount = MailAccountWriter.write(updatedAccount);
        }

        return new AJAXRequestResult(jsonAccount).addWarnings(warnings);
    }

}
