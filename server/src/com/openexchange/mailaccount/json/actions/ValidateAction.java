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

import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.mail.MailException;
import com.openexchange.mail.MailException.Code;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.transport.MailTransport;
import com.openexchange.mail.transport.TransportProvider;
import com.openexchange.mail.transport.TransportProviderRegistry;
import com.openexchange.mail.transport.config.TransportConfig;
import com.openexchange.mail.utils.MailPasswordUtil;
import com.openexchange.mailaccount.MailAccountDescription;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.json.parser.MailAccountParser;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.net.URIDefaults;
import com.openexchange.tools.net.URIParser;
import com.openexchange.tools.net.URITools;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link ValidateAction}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ValidateAction extends AbstractMailAccountTreeAction {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.exception.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(ValidateAction.class));

    private static final boolean DEBUG = LOG.isDebugEnabled();

    public static final String ACTION = AJAXServlet.ACTION_VALIDATE;

    /**
     * Initializes a new {@link ValidateAction}.
     */
    public ValidateAction() {
        super();
    }

    public AJAXRequestResult perform(final AJAXRequestData request, final ServerSession session) throws OXException {
        final JSONObject jData = (JSONObject) request.getData();

        try {
            if (!session.getUserConfiguration().isMultipleMailAccounts()) {
                throw 
                    MailAccountExceptionCodes.NOT_ENABLED.create(
                    Integer.valueOf(session.getUserId()),
                    Integer.valueOf(session.getContextId()));
            }

            final MailAccountDescription accountDescription = new MailAccountDescription();
            new MailAccountParser().parse(accountDescription, jData);

            if (accountDescription.getId() >= 0 && null == accountDescription.getPassword()) {
                /*
                 * ID is delivered, but password not set. Thus load from storage version.
                 */
                final MailAccountStorageService storageService = ServerServiceRegistry.getInstance().getService(
                    MailAccountStorageService.class,
                    true);

                final String encodedPassword = storageService.getMailAccount(
                    accountDescription.getId(),
                    session.getUserId(),
                    session.getContextId()).getPassword();
                accountDescription.setPassword(MailPasswordUtil.decrypt(encodedPassword, getSecret(session)));
            }

            checkNeededFields(accountDescription);
            if (isUnifiedINBOXAccount(accountDescription.getMailProtocol())) {
                // Deny validation of Unified INBOX account
                throw MailAccountExceptionCodes.VALIDATION_FAILED.create();
            }
            // Check for tree parameter
            final boolean tree;
            {
                final String tmp = request.getParameter("tree");
                tree = Boolean.parseBoolean(tmp);
            }
            // List for possible warnings
            final List<AbstractOXException> warnings = new ArrayList<AbstractOXException>(2);
            if (tree) {
                return new AJAXRequestResult(actionValidateTree(accountDescription, session, warnings)).addWarnings(warnings);
            }
            return new AJAXRequestResult(actionValidateBoolean(accountDescription, session, warnings)).addWarnings(warnings);
        } catch (final JSONException e) {
            throw AjaxExceptionCodes.JSONError.create( e, e.getMessage());
        } catch (final GeneralSecurityException e) {
            throw MailAccountExceptionCodes.UNEXPECTED_ERROR.create(
                e,
                e.getMessage());
        }
    }

    private static JSONObject actionValidateTree(final MailAccountDescription accountDescription, final ServerSession session, final List<AbstractOXException> warnings) throws JSONException, OXException {
        if (!actionValidateBoolean(accountDescription, session, warnings).booleanValue()) {
            // TODO: How to indicate error if folder tree requested?
            return null;
        }
        // Create a mail access instance
        final MailAccess<?, ?> mailAccess = getMailAccess(accountDescription, session);
        return actionValidateTree0(mailAccess, session);
    }

    private static Boolean actionValidateBoolean(final MailAccountDescription accountDescription, final ServerSession session, final List<AbstractOXException> warnings) throws OXException {
        try {
            // Validate mail server
            boolean validated = checkMailServerURL(accountDescription, session, warnings);
            // Failed?
            if (!validated) {
                return Boolean.FALSE;
            }
            // Now check transport server URL, if a transport server is present
            final String transportServer = accountDescription.getTransportServer();
            if (null != transportServer && transportServer.length() > 0) {
                validated = checkTransportServerURL(accountDescription, session, warnings);
            }
            return Boolean.valueOf(validated);
        } catch (final AbstractOXException e) {
            throw new OXException(e);
        }
    }

    private static boolean checkMailServerURL(final MailAccountDescription accountDescription, final ServerSession session, final List<AbstractOXException> warnings) throws OXException {
        // Create a mail access instance
        final MailAccess<?, ?> mailAccess = getMailAccess(accountDescription, session);
        if (null == mailAccess) {
            return false;
        }
        // Now try to connect
        return mailAccess.ping();
    }

    private static boolean checkTransportServerURL(final MailAccountDescription accountDescription, final ServerSession session, final List<AbstractOXException> warnings) throws OXException {
        final String transportServerURL = accountDescription.generateTransportServerURL();
        // Get the appropriate transport provider by transport server URL
        final TransportProvider transportProvider = TransportProviderRegistry.getTransportProviderByURL(transportServerURL);
        if (null == transportProvider) {
            if (DEBUG) {
                LOG.debug("Validating mail account failed. No transport provider found for URL: " + transportServerURL);
            }
            return false;
        }
        // Create a transport access instance
        final MailTransport mailTransport = transportProvider.createNewMailTransport(session);
        final TransportConfig transportConfig = mailTransport.getTransportConfig();
        // Set login and password
        String login = accountDescription.getTransportLogin();
        if (null == login) {
            login = accountDescription.getLogin();
        }
        transportConfig.setLogin(login);
        String password = accountDescription.getTransportPassword();
        if (null == password) {
            password = accountDescription.getPassword();
        }
        transportConfig.setPassword(password);
        // Set server and port
        final URI uri;
        try {
            uri = URIParser.parse(transportServerURL, URIDefaults.SMTP);
        } catch (final URISyntaxException e) {
            throw new MailException(Code.URI_PARSE_FAILED, e, transportServerURL);
        }
        transportConfig.setServer(URITools.getHost(uri));
        transportConfig.setPort(uri.getPort());
        transportConfig.setSecure(accountDescription.isTransportSecure());
        boolean validated = true;
        // Now try to connect
        boolean close = false;
        try {
            mailTransport.ping();
            close = true;
        } catch (final AbstractOXException e) {
            if (DEBUG) {
                LOG.debug("Validating transport account failed.", e);
            }
            e.setCategory(Category.WARNING);
            warnings.add(e);
            validated = false;
        } finally {
            if (close) {
                mailTransport.close();
            }
        }
        return validated;
    }

}
