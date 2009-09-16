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

package com.openexchange.mailaccount.servlet.request;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.parser.DataParser;
import com.openexchange.api.OXMandatoryFieldException;
import com.openexchange.api2.OXException;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.mail.MailException;
import com.openexchange.mail.MailProviderRegistry;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.api.MailProvider;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.json.writer.FolderWriter;
import com.openexchange.mail.transport.MailTransport;
import com.openexchange.mail.transport.TransportProvider;
import com.openexchange.mail.transport.TransportProviderRegistry;
import com.openexchange.mail.transport.config.TransportConfig;
import com.openexchange.mail.utils.MailPasswordUtil;
import com.openexchange.mailaccount.Attribute;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountDescription;
import com.openexchange.mailaccount.MailAccountExceptionFactory;
import com.openexchange.mailaccount.MailAccountExceptionMessages;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.UnifiedINBOXManagement;
import com.openexchange.mailaccount.servlet.fields.MailAccountFields;
import com.openexchange.mailaccount.servlet.parser.MailAccountParser;
import com.openexchange.mailaccount.servlet.writer.MailAccountWriter;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.iterator.SearchIteratorException;
import com.openexchange.tools.servlet.AjaxException;
import com.openexchange.tools.servlet.OXJSONException;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link MailAccountRequest} - Handles request to mail account servlet.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailAccountRequest {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(MailAccountRequest.class);

    private static final boolean DEBUG = LOG.isDebugEnabled();

    private final ServerSession session;

    private Date timestamp;

    /**
     * Gets the time stamp.
     * 
     * @return The time stamp.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Initializes a new {@link MailAccountRequest}.
     * 
     * @param session The session
     */
    public MailAccountRequest(final ServerSession session) {
        super();
        this.session = session;
    }

    /**
     * Handles the request dependent on specified action string.
     * 
     * @param action The action string
     * @param jsonObject The JSON object containing request's data & parameters
     * @return A JSON result object dependent on triggered action method
     * @throws OXMandatoryFieldException If a mandatory field is missing in passed JSON request object
     * @throws OXException If a server-related error occurs
     * @throws JSONException If a JSON error occurs
     * @throws SearchIteratorException If a search-iterator error occurs
     * @throws AjaxException If an AJAX error occurs
     * @throws OXJSONException If a JSON error occurs
     */
    public Object action(final String action, final JSONObject jsonObject) throws OXMandatoryFieldException, OXException, JSONException, SearchIteratorException, AjaxException, OXJSONException {
        if (action.equalsIgnoreCase(AJAXServlet.ACTION_DELETE)) {
            return actionDelete(jsonObject);
        } else if (action.equalsIgnoreCase(AJAXServlet.ACTION_NEW)) {
            return actionNew(jsonObject);
        } else if (action.equalsIgnoreCase(AJAXServlet.ACTION_UPDATE)) {
            return actionUpate(jsonObject);
        } else if (action.equalsIgnoreCase(AJAXServlet.ACTION_GET)) {
            return actionGet(jsonObject);
        } else if (action.equalsIgnoreCase(AJAXServlet.ACTION_ALL)) {
            return actionAll(jsonObject);
        } else if (action.equalsIgnoreCase(AJAXServlet.ACTION_LIST)) {
            return actionList(jsonObject);
        } else if (action.equalsIgnoreCase(AJAXServlet.ACTION_VALIDATE)) {
            return actionValidate(jsonObject);
        } else {
            throw new AjaxException(AjaxException.Code.UnknownAction, action);
        }
    }

    private JSONObject actionGet(final JSONObject jsonObject) throws JSONException, OXException, OXJSONException, AjaxException {
        final int id = DataParser.checkInt(jsonObject, AJAXServlet.PARAMETER_ID);

        try {
            final MailAccountStorageService storageService = ServerServiceRegistry.getInstance().getService(
                MailAccountStorageService.class,
                true);

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

            final JSONObject jsonAccount = MailAccountWriter.write(mailAccount);
            return jsonAccount;
        } catch (final AbstractOXException exc) {
            throw new OXException(exc);
        }
    }

    private JSONArray actionDelete(final JSONObject jsonObject) throws JSONException, OXException, OXJSONException, AjaxException {
        final int[] ids = DataParser.checkJSONIntArray(jsonObject, AJAXServlet.PARAMETER_DATA);

        final JSONArray jsonArray = new JSONArray();
        try {
            if (!session.getUserConfiguration().isMultipleMailAccounts()) {
                for (int i = 0; i < ids.length; i++) {
                    if (MailAccount.DEFAULT_ID != ids[i]) {
                        throw MailAccountExceptionFactory.getInstance().create(
                            MailAccountExceptionMessages.NOT_ENABLED,
                            Integer.valueOf(session.getUserId()),
                            Integer.valueOf(session.getContextId()));
                    }
                }
            }

            final MailAccountStorageService storageService = ServerServiceRegistry.getInstance().getService(
                MailAccountStorageService.class,
                true);

            for (int i = 0; i < ids.length; i++) {
                final int id = ids[i];
                final MailAccount mailAccount = storageService.getMailAccount(id, session.getUserId(), session.getContextId());

                if (!isUnifiedINBOXAccount(mailAccount)) {
                    storageService.deleteMailAccount(id, session.getUserId(), session.getContextId());
                }

                jsonArray.put(id);
            }
        } catch (final AbstractOXException exc) {
            throw new OXException(exc);
        }
        return jsonArray;
    }

    private JSONObject actionNew(final JSONObject jsonObject) throws AjaxException, OXException, JSONException {
        final JSONObject jData = DataParser.checkJSONObject(jsonObject, AJAXServlet.PARAMETER_DATA);

        try {
            if (!session.getUserConfiguration().isMultipleMailAccounts()) {
                throw MailAccountExceptionFactory.getInstance().create(
                    MailAccountExceptionMessages.NOT_ENABLED,
                    Integer.valueOf(session.getUserId()),
                    Integer.valueOf(session.getContextId()));
            }

            final MailAccountDescription accountDescription = new MailAccountDescription();
            new MailAccountParser().parse(accountDescription, jData);

            checkNeededFields(accountDescription);

            // Check if account denotes a Unified INBOX account
            if (isUnifiedINBOXAccount(accountDescription.getMailProtocol())) {
                // Deny creation of Unified INBOX account
                throw MailAccountExceptionMessages.CREATION_FAILED.create();
            }

            final MailAccountStorageService storageService = ServerServiceRegistry.getInstance().getService(
                MailAccountStorageService.class,
                true);

            final int id = storageService.insertMailAccount(
                accountDescription,
                session.getUserId(),
                session.getContext(),
                session.getPassword());

            final JSONObject jsonAccount = MailAccountWriter.write(storageService.getMailAccount(
                id,
                session.getUserId(),
                session.getContextId()));

            return jsonAccount;
        } catch (final AbstractOXException e) {
            throw new OXException(e);
        }
    }

    private Object actionValidate(final JSONObject jsonObject) throws AjaxException, OXException, JSONException {
        final JSONObject jData = DataParser.checkJSONObject(jsonObject, AJAXServlet.PARAMETER_DATA);

        try {
            if (!session.getUserConfiguration().isMultipleMailAccounts()) {
                throw MailAccountExceptionFactory.getInstance().create(
                    MailAccountExceptionMessages.NOT_ENABLED,
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
                accountDescription.setPassword(MailPasswordUtil.decrypt(encodedPassword, session.getPassword()));
            }

            checkNeededFields(accountDescription);
            if (isUnifiedINBOXAccount(accountDescription.getMailProtocol())) {
                // Deny validation of Unified INBOX account
                throw MailAccountExceptionMessages.VALIDATION_FAILED.create();
            }
            // Check for tree parameter
            final boolean tree = jsonObject.hasAndNotNull("tree") ? jsonObject.getBoolean("tree") : false;
            if (tree) {
                return actionValidateTree(accountDescription);
            }
            return actionValidateBoolean(accountDescription);
        } catch (final AbstractOXException e) {
            throw new OXException(e);
        } catch (final GeneralSecurityException e) {
            throw new OXException(MailAccountExceptionFactory.getInstance().create(
                MailAccountExceptionMessages.UNEXPECTED_ERROR,
                e,
                e.getMessage()));
        }
    }

    private JSONObject actionValidateTree(final MailAccountDescription accountDescription) throws OXException, MailException, JSONException {
        if (!actionValidateBoolean(accountDescription).booleanValue()) {
            // TODO: How to indicate error if folder tree requested?
            return null;
        }
        // Create a mail access instance
        final MailAccess<?, ?> mailAccess = getMailAccess(accountDescription);
        // Now try to connect
        boolean close = false;
        try {
            mailAccess.connect();
            close = true;
            // Compose folder tree
            final JSONObject root = FolderWriter.writeMailFolder(-1, mailAccess.getRootFolder(), mailAccess.getMailConfig());
            // Recursive call
            addSubfolders(
                root,
                mailAccess.getFolderStorage().getSubfolders(MailFolder.DEFAULT_FOLDER_ID, true),
                mailAccess,
                mailAccess.getMailConfig());
            return root;
        } catch (final AbstractOXException e) {
            if (DEBUG) {
                LOG.debug("Composing mail account's folder tree failed.", e);
            }
            // TODO: How to indicate error if folder tree requested?
            return null;
        } finally {
            if (close) {
                mailAccess.close(false);
            }
        }
    }

    private void addSubfolders(final JSONObject parent, final MailFolder[] subfolders, final MailAccess<?, ?> mailAccess, final MailConfig mailConfig) throws JSONException, MailException {
        if (subfolders.length == 0) {
            return;
        }

        final JSONArray subfolderArray = new JSONArray();
        parent.put("subfolder_array", subfolderArray);

        for (final MailFolder subfolder : subfolders) {
            final JSONObject subfolderObject = FolderWriter.writeMailFolder(-1, subfolder, mailConfig);
            subfolderArray.put(subfolderObject);
            // Recursive call
            addSubfolders(
                subfolderObject,
                mailAccess.getFolderStorage().getSubfolders(subfolder.getFullname(), true),
                mailAccess,
                mailConfig);
        }
    }

    private Boolean actionValidateBoolean(final MailAccountDescription accountDescription) throws OXException {
        try {
            // Validate mail server
            boolean validated = checkMailServerURL(accountDescription);
            // Failed?
            if (!validated) {
                return Boolean.FALSE;
            }
            // Now check transport server URL, if a transport server is present
            final String transportServer = accountDescription.getTransportServer();
            if (null != transportServer && transportServer.length() > 0) {
                validated = checkTransportServerURL(accountDescription);
            }
            return Boolean.valueOf(validated);
        } catch (final AbstractOXException e) {
            throw new OXException(e);
        }
    }

    private MailAccess<?, ?> getMailAccess(final MailAccountDescription accountDescription) throws MailException {
        final String mailServerURL = accountDescription.generateMailServerURL();
        // Get the appropriate mail provider by mail server URL
        final MailProvider mailProvider = MailProviderRegistry.getMailProviderByURL(mailServerURL);
        if (null == mailProvider) {
            if (DEBUG) {
                LOG.debug("Validating mail account failed. No mail provider found for URL: " + mailServerURL);
            }
            return null;
        }
        // Create a mail access instance
        final MailAccess<?, ?> mailAccess = mailProvider.createNewMailAccess(session);
        final MailConfig mailConfig = mailAccess.getMailConfig();
        // Set login and password
        mailConfig.setLogin(accountDescription.getLogin());
        mailConfig.setPassword(accountDescription.getPassword());
        // Set server and port
        final String server;
        {
            final String[] tmp = MailConfig.parseProtocol(mailServerURL);
            server = tmp == null ? mailServerURL : tmp[1];
        }
        final int pos = server.indexOf(':');
        if (pos == -1) {
            mailConfig.setPort(143);
            mailConfig.setServer(server);
        } else {
            final String sPort = server.substring(pos + 1);
            try {
                mailConfig.setPort(Integer.parseInt(sPort));
            } catch (final NumberFormatException e) {
                LOG.warn(new StringBuilder().append("Cannot parse port out of string: \"").append(sPort).append(
                    "\". Using fallback 143 instead."), e);
                mailConfig.setPort(143);
            }
            mailConfig.setServer(server.substring(0, pos));
        }
        mailConfig.setSecure(accountDescription.isMailSecure());
        mailAccess.setCacheable(false);
        return mailAccess;
    }

    private boolean checkMailServerURL(final MailAccountDescription accountDescription) throws MailException {
        // Create a mail access instance
        final MailAccess<?, ?> mailAccess = getMailAccess(accountDescription);
        if (null == mailAccess) {
            return false;
        }
        // Now try to connect
        return mailAccess.ping();
    }

    private boolean checkTransportServerURL(final MailAccountDescription accountDescription) throws MailException {
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
        transportConfig.setLogin(accountDescription.getTransportLogin());
        transportConfig.setPassword(accountDescription.getTransportPassword());
        // Set server and port
        final String server;
        {
            final String[] tmp = TransportConfig.parseProtocol(transportServerURL);
            server = tmp == null ? transportServerURL : tmp[1];
        }
        final int pos = server.indexOf(':');
        if (pos == -1) {
            transportConfig.setPort(25);
            transportConfig.setServer(server);
        } else {
            final String sPort = server.substring(pos + 1);
            try {
                transportConfig.setPort(Integer.parseInt(sPort));
            } catch (final NumberFormatException e) {
                LOG.warn(new StringBuilder().append("Cannot parse port out of string: \"").append(sPort).append(
                    "\". Using fallback 25 instead."), e);
                transportConfig.setPort(25);
            }
            transportConfig.setServer(server.substring(0, pos));
        }
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
            validated = false;
        } finally {
            if (close) {
                mailTransport.close();
            }
        }
        return validated;
    }

    private JSONObject actionUpate(final JSONObject jsonObject) throws AjaxException, OXException, JSONException {
        final JSONObject jData = DataParser.checkJSONObject(jsonObject, AJAXServlet.PARAMETER_DATA);

        try {
            final MailAccountDescription accountDescription = new MailAccountDescription();
            final Set<Attribute> fieldsToUpdate = new MailAccountParser().parse(accountDescription, jData);

            if (!session.getUserConfiguration().isMultipleMailAccounts() && !isDefaultMailAccount(accountDescription)) {
                throw MailAccountExceptionFactory.getInstance().create(
                    MailAccountExceptionMessages.NOT_ENABLED,
                    Integer.valueOf(session.getUserId()),
                    Integer.valueOf(session.getContextId()));
            }

            final MailAccountStorageService storageService = ServerServiceRegistry.getInstance().getService(
                MailAccountStorageService.class,
                true);

            final int id = accountDescription.getId();
            if (-1 == id) {
                throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, MailAccountFields.ID);
            }

            final MailAccount toUpdate = storageService.getMailAccount(id, session.getUserId(), session.getContextId());
            if (isUnifiedINBOXAccount(toUpdate)) {
                // Treat as no hit
                throw MailAccountExceptionMessages.NOT_FOUND.create(
                    Integer.valueOf(id),
                    Integer.valueOf(session.getUserId()),
                    Integer.valueOf(session.getContextId()));
            }

            storageService.updateMailAccount(
                accountDescription,
                fieldsToUpdate,
                session.getUserId(),
                session.getContextId(),
                session.getPassword());

            final JSONObject jsonAccount = MailAccountWriter.write(storageService.getMailAccount(
                id,
                session.getUserId(),
                session.getContextId()));

            return jsonAccount;
        } catch (final AbstractOXException e) {
            throw new OXException(e);
        }
    }

    private JSONArray actionAll(final JSONObject request) throws OXException {
        final String colString = request.optString(AJAXServlet.PARAMETER_COLUMNS);

        final List<Attribute> attributes = getColumns(colString);
        try {
            final MailAccountStorageService storageService = ServerServiceRegistry.getInstance().getService(
                MailAccountStorageService.class,
                true);

            MailAccount[] userMailAccounts = storageService.getUserMailAccounts(session.getUserId(), session.getContextId());

            final boolean multipleEnabled = session.getUserConfiguration().isMultipleMailAccounts();
            final List<MailAccount> tmp = new ArrayList<MailAccount>(userMailAccounts.length);

            for (int i = 0; i < userMailAccounts.length; i++) {
                final MailAccount mailAccount = userMailAccounts[i];
                if (!isUnifiedINBOXAccount(mailAccount) && (multipleEnabled || isDefaultMailAccount(mailAccount))) {
                    tmp.add(mailAccount);
                }
            }
            userMailAccounts = tmp.toArray(new MailAccount[tmp.size()]);

            return MailAccountWriter.writeArray(userMailAccounts, attributes);
        } catch (final AbstractOXException e) {
            throw new OXException(e);
        }
    }

    private List<Attribute> getColumns(final String colString) {
        List<Attribute> attributes = null;
        if (colString != null && !"".equals(colString.trim())) {
            attributes = new LinkedList<Attribute>();
            for (final String col : colString.split("\\s*,\\s*")) {
                if ("".equals(col)) {
                    continue;
                }
                attributes.add(Attribute.getById(Integer.parseInt(col)));
            }
            return attributes;
        }
        // All columns
        return Arrays.asList(Attribute.values());
    }

    private JSONArray actionList(final JSONObject request) throws JSONException, OXException {
        final String colString = request.optString(AJAXServlet.PARAMETER_COLUMNS);

        final List<Attribute> attributes = getColumns(colString);
        try {
            final MailAccountStorageService storageService = ServerServiceRegistry.getInstance().getService(
                MailAccountStorageService.class,
                true);

            final JSONArray ids = request.getJSONArray(AJAXServlet.PARAMETER_DATA);

            final boolean multipleEnabled = session.getUserConfiguration().isMultipleMailAccounts();
            final List<MailAccount> accounts = new ArrayList<MailAccount>();

            for (int i = 0, size = ids.length(); i < size; i++) {
                final int id = ids.getInt(i);
                final MailAccount account = storageService.getMailAccount(id, session.getUserId(), session.getContextId());
                if (!isUnifiedINBOXAccount(account) && (multipleEnabled || isDefaultMailAccount(account))) {
                    accounts.add(account);
                }
            }

            return MailAccountWriter.writeArray(accounts.toArray(new MailAccount[accounts.size()]), attributes);
        } catch (final AbstractOXException e) {
            throw new OXException(e);
        }
    }

    private static boolean isUnifiedINBOXAccount(final MailAccount mailAccount) {
        return isUnifiedINBOXAccount(mailAccount.getMailProtocol());
    }

    private static boolean isUnifiedINBOXAccount(final String mailProtocol) {
        return UnifiedINBOXManagement.PROTOCOL_UNIFIED_INBOX.equals(mailProtocol);
    }

    private static boolean isDefaultMailAccount(final MailAccount mailAccount) {
        return mailAccount.isDefaultAccount() || MailAccount.DEFAULT_ID == mailAccount.getId();
    }

    private static boolean isDefaultMailAccount(final MailAccountDescription mailAccount) {
        return MailAccount.DEFAULT_ID == mailAccount.getId();
    }

    private static void checkNeededFields(final MailAccountDescription accountDescription) throws AjaxException {
        // Check needed fields
        if (null == accountDescription.getMailServer()) {
            throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, MailAccountFields.MAIL_URL);
        }
        if (null == accountDescription.getLogin()) {
            throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, MailAccountFields.LOGIN);
        }
        if (null == accountDescription.getPassword()) {
            throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, MailAccountFields.PASSWORD);
        }
    }
}
