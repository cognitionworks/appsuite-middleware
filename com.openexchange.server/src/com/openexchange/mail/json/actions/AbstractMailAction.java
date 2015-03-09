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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.mail.json.actions;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.idn.IDNA;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.Mail;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.AJAXState;
import com.openexchange.annotation.NonNull;
import com.openexchange.contactcollector.ContactCollectorService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.json.Column;
import com.openexchange.mail.json.MailActionConstants;
import com.openexchange.mail.json.MailRequest;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.utils.AddressUtility;
import com.openexchange.mail.utils.DisplayMode;
import com.openexchange.mail.utils.MsisdnUtility;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.server.ServiceLookup;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link AbstractMailAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class AbstractMailAction implements AJAXActionService, MailActionConstants {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractMailAction.class);

    private static final AJAXRequestResult RESULT_JSON_NULL = new AJAXRequestResult(JSONObject.NULL, "json");

    private final ServiceLookup services;

    public static final @NonNull int[] FIELDS_ALL_ALIAS = new int[] { 600, 601 };

    public static final @NonNull int[] FIELDS_LIST_ALIAS = new int[] { 600, 601, 614, 602, 611, 603, 612, 607, 652, 610, 608, 102 };

    public static final @NonNull Column[] COLUMNS_ALL_ALIAS = new Column[] { Column.field(600), Column.field(601) };

    public static final @NonNull Column[] COLUMNS_LIST_ALIAS = new Column[] { Column.field(600), Column.field(601), Column.field(614), Column.field(602), Column.field(611), Column.field(603), Column.field(612), Column.field(607), Column.field(652), Column.field(610), Column.field(608), Column.field(102) };

    /**
     * Initializes a new {@link AbstractMailAction}.
     */
    protected AbstractMailAction(final ServiceLookup services) {
        super();
        this.services = services;
    }

    /**
     * Cachable formats: <code>"apiResponse"</code>, <code>"json"</code>.
     */
    protected static final Set<String> CACHABLE_FORMATS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "apiResponse",
        "json")));

    /**
     * Gets the service of specified type
     *
     * @param clazz The service's class
     * @return The service or <code>null</code> if absent
     */
    protected <S> S getService(final Class<? extends S> clazz) {
        return services.getService(clazz);
    }

    /**
     * Gets the mail interface.
     *
     * @param mailRequest The mail request
     * @return The mail interface
     * @throws OXException If mail interface cannot be initialized
     */
    protected MailServletInterface getMailInterface(final MailRequest mailRequest) throws OXException {
        /*
         * Get mail interface
         */
        final AJAXState state = mailRequest.getRequest().getState();
        MailServletInterface mailInterface = null;
        if (state == null) {
            return MailServletInterface.getInstance(mailRequest.getSession());
        }
        mailInterface = state.optProperty(PROPERTY_MAIL_IFACE);
        if (mailInterface == null) {
            final MailServletInterface newMailInterface = MailServletInterface.getInstance(mailRequest.getSession());
            mailInterface = state.putProperty(PROPERTY_MAIL_IFACE, newMailInterface);
            if (null == mailInterface) {
                mailInterface = newMailInterface;
            } else {
                newMailInterface.close(true);
            }
        }
        return mailInterface;
    }

    /**
     * Gets the closeables.
     *
     * @param mailRequest The mail request
     * @return The closeables or <code>null</code> if state is absent
     * @throws OXException If closebales cannot be returned
     */
    protected Collection<Closeable> getCloseables(final MailRequest mailRequest) throws OXException {
        final AJAXState state = mailRequest.getRequest().getState();
        if (state == null) {
            return null;
        }
        Collection<Closeable> closeables = state.optProperty(PROPERTY_CLOSEABLES);
        if (null == closeables) {
            final Collection<Closeable> newCloseables = new LinkedList<Closeable>();
            closeables = state.putProperty(PROPERTY_CLOSEABLES, newCloseables);
            if (null == closeables) {
                closeables = newCloseables;
            }
        }
        return closeables;
    }

    @Override
    public AJAXRequestResult perform(final AJAXRequestData requestData, final ServerSession session) throws OXException {
        if (!session.getUserPermissionBits().hasWebMail()) {
            throw AjaxExceptionCodes.NO_PERMISSION_FOR_MODULE.create("mail");
        }
        try {
            return perform(new MailRequest(requestData, session));
        } catch (final IllegalStateException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof OXException) {
                throw (OXException) cause;
            }
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (final JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        } finally {
            requestData.cleanUploads();
        }
    }

    /**
     * Performs specified mail request.
     *
     * @param req The mail request
     * @return The result
     * @throws OXException If an error occurs
     * @throws JSONException If a JSON error occurs
     */
    protected abstract AJAXRequestResult perform(MailRequest req) throws OXException, JSONException;

    /**
     * Checks whether specified mail should be discarded according to filter flags
     *
     * @param m The mail to check
     * @param ignoreSeen <code>true</code> to ignore \Seen ones; otherwise <code>false</code>
     * @param ignoreDeleted <code>true</code> to ignore \Deleted ones that are \Seen; otherwise <code>false</code>
     * @return <code>true</code> if mail should be discarded according to filter flags; otherwise <code>false</code>
     */
    protected static boolean discardMail(final MailMessage m, final boolean ignoreSeen, final boolean ignoreDeleted) {
        if (null == m) {
            return true;
        }
        if (ignoreSeen && m.isSeen()) {
            // Discard \Seen mails
            return true;
        }
        if (ignoreDeleted && m.isDeleted() && m.isSeen()) {
            // Discard \Seen mails that are \Deleted
            return true;
        }
        // Do not discard
        return false;
    }

    /**
     * Triggers the contact collector for specified mail's addresses.
     *
     * @param session The session
     * @param mail The mail
     * @throws OXException
     */
    public static void triggerContactCollector(final ServerSession session, final MailMessage mail) throws OXException {
        final ContactCollectorService ccs = ServerServiceRegistry.getInstance().getService(ContactCollectorService.class);
        if (null != ccs) {
            Set<InternetAddress> addrs = AddressUtility.getUnknownAddresses(mail, session);

            if (!addrs.isEmpty()) {
                ccs.memorizeAddresses(new ArrayList<InternetAddress>(addrs), session);
            }
        }
    }

    protected static final String VIEW_RAW = "raw";

    protected static final String VIEW_TEXT = "text";

    protected static final String VIEW_HTML = "html";

    protected static final String VIEW_HTML_BLOCKED_IMAGES = "noimg";

    protected static final String VIEW_DOCUMENT = "document";

    /**
     * Detects the display mode.
     *
     * @param modifyable whether modifiable.
     * @param view the view
     * @param usm The user mail settings
     * @return The display mode
     */
    public static DisplayMode detectDisplayMode(final boolean modifyable, final String view, final UserSettingMail usm) {
        final DisplayMode displayMode;
        if (null != view) {
            if (VIEW_RAW.equals(view)) {
                displayMode = DisplayMode.RAW;
            } else if (VIEW_TEXT.equals(view)) {
                usm.setDisplayHtmlInlineContent(false);
                displayMode = modifyable ? DisplayMode.MODIFYABLE : DisplayMode.DISPLAY;
            } else if (VIEW_HTML.equals(view)) {
                usm.setDisplayHtmlInlineContent(true);
                usm.setAllowHTMLImages(true);
                displayMode = modifyable ? DisplayMode.MODIFYABLE : DisplayMode.DISPLAY;
            } else if (VIEW_HTML_BLOCKED_IMAGES.equals(view)) {
                usm.setDisplayHtmlInlineContent(true);
                usm.setAllowHTMLImages(false);
                displayMode = modifyable ? DisplayMode.MODIFYABLE : DisplayMode.DISPLAY;
            } else if (VIEW_DOCUMENT.equals(view)) {
                displayMode = DisplayMode.DOCUMENT;
            } else {
                LOG.warn("Unknown value in parameter {}: {}. Using user's mail settings as fallback.", Mail.PARAMETER_VIEW, view);
                displayMode = modifyable ? DisplayMode.MODIFYABLE : DisplayMode.DISPLAY;
            }
        } else {
            displayMode = modifyable ? DisplayMode.MODIFYABLE : DisplayMode.DISPLAY;
        }
        return displayMode;
    }

    /**
     * Gets the result filled with JSON <code>NULL</code>.
     *
     * @return The result with JSON <code>NULL</code>.
     */
    protected static AJAXRequestResult getJSONNullResult() {
        return RESULT_JSON_NULL;
    }

    /**
     * Resolves specified "from" address to associated account identifier
     *
     * @param session The session
     * @param from The from address
     * @param checkTransportSupport <code>true</code> to check transport support
     * @param checkFrom <code>true</code> to check from validity
     * @return The account identifier
     * @throws OXException If address cannot be resolved
     */
    protected static int resolveFrom2Account(final ServerSession session, final InternetAddress from, final boolean checkTransportSupport, final boolean checkFrom) throws OXException {
        /*
         * Resolve "From" to proper mail account to select right transport server
         */
        int accountId;
        {
            final MailAccountStorageService storageService = ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class, true);
            final int user = session.getUserId();
            final int cid = session.getContextId();
            if (null == from) {
                accountId = MailAccount.DEFAULT_ID;
            } else {
                accountId = storageService.getByPrimaryAddress(from.getAddress(), user, cid);
                if (accountId != -1) {
                    // Retry with IDN representation
                    accountId = storageService.getByPrimaryAddress(IDNA.toIDN(from.getAddress()), user, cid);
                }
            }
            if (accountId != -1) {
                if (!session.getUserPermissionBits().isMultipleMailAccounts() && accountId != MailAccount.DEFAULT_ID) {
                    throw MailAccountExceptionCodes.NOT_ENABLED.create(Integer.valueOf(user), Integer.valueOf(cid));
                }
                if (checkTransportSupport) {
                    final MailAccount account = storageService.getMailAccount(accountId, user, cid);
                    // Check if determined account supports mail transport
                    if (null == account.getTransportServer()) {
                        // Account does not support mail transport
                        throw MailExceptionCode.NO_TRANSPORT_SUPPORT.create(account.getName(), Integer.valueOf(accountId));
                    }
                }
            }
        }
        if (accountId == -1) {
            if (checkFrom && null != from) {
                /*
                 * Check aliases
                 */
                try {
                    final Set<InternetAddress> validAddrs = new HashSet<InternetAddress>(4);
                    final User user = session.getUser();
                    final String[] aliases = user.getAliases();
                    for (final String alias : aliases) {
                        validAddrs.add(new QuotedInternetAddress(alias));
                    }
                    if (MailProperties.getInstance().isSupportMsisdnAddresses()) {
                        MsisdnUtility.addMsisdnAddress(validAddrs, session);
                        final String address = from.getAddress();
                        final int pos = address.indexOf('/');
                        if (pos > 0) {
                            from.setAddress(address.substring(0, pos));
                        }
                    }
                    if (!validAddrs.contains(from)) {
                        throw MailExceptionCode.INVALID_SENDER.create(from.toString());
                    }
                } catch (final AddressException e) {
                    throw MimeMailException.handleMessagingException(e);
                }
            }
            accountId = MailAccount.DEFAULT_ID;
        }
        return accountId;
    }

    protected static String getDefaultSendAddress(final ServerSession session) throws OXException {
        return session.getUserSettingMail().getSendAddr();
        /*-
         *
        final MailAccountStorageService storageService = ServerServiceRegistry.getInstance().getService(
            MailAccountStorageService.class,
            true);
        return storageService.getDefaultMailAccount(session.getUserId(), session.getContextId()).getPrimaryAddress();
         *
         */
    }

    protected static boolean isEmpty(final String string) {
        return com.openexchange.java.Strings.isEmpty(string);
    }

}
