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

package com.openexchange.imap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.event.FolderEvent;
import javax.mail.event.FolderListener;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.imap.acl.ACLExtension;
import com.openexchange.imap.acl.ACLExtensionInit;
import com.openexchange.imap.cache.ListLsubCache;
import com.openexchange.imap.cache.ListLsubEntry;
import com.openexchange.imap.cache.MBoxEnabledCache;
import com.openexchange.imap.config.IIMAPProperties;
import com.openexchange.imap.config.IMAPConfig;
import com.openexchange.imap.config.IMAPSessionProperties;
import com.openexchange.imap.config.MailAccountIMAPProperties;
import com.openexchange.imap.converters.IMAPFolderConverter;
import com.openexchange.imap.entity2acl.Entity2ACLInit;
import com.openexchange.imap.notify.internal.IMAPNotifierMessageRecentListener;
import com.openexchange.imap.notify.internal.IMAPNotifierRegistry;
import com.openexchange.imap.ping.IMAPCapabilityAndGreetingCache;
import com.openexchange.imap.services.IMAPServiceRegistry;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.api.IMailProperties;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.api.MailLogicTools;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.mime.MIMEMailException;
import com.openexchange.mail.mime.MIMESessionPropertyNames;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.session.Session;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;
import com.openexchange.tools.ssl.TrustAllSSLSocketFactory;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;

/**
 * {@link IMAPAccess} - Establishes an IMAP access and provides access to storages.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class IMAPAccess extends MailAccess<IMAPFolderStorage, IMAPMessageStorage> {

    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = -7510487764376433468L;

    /**
     * The max. temporary-down value; 5 Minutes.
     */
    private static final long MAX_TEMP_DOWN = 300000L;

    /**
     * The timeout for failed logins.
     */
    private static final long FAILED_AUTH_TIMEOUT = 10000L;

    /**
     * The logger instance for {@link IMAPAccess} class.
     */
    private static final transient org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(IMAPAccess.class));

    /**
     * Whether info logging is enabled for this class.
     */
    private static final boolean INFO = LOG.isInfoEnabled();

    /**
     * Whether debug logging is enabled for this class.
     */
    private static final boolean DEBUG = LOG.isDebugEnabled();

    /**
     * The string for <code>ISO-8859-1</code> character encoding.
     */
    private static final String CHARENC_ISO8859 = "ISO-8859-1";

    /**
     * Remembers timed out servers for {@link IIMAPProperties#getImapTemporaryDown()} milliseconds. Any further attempts to connect to such
     * a server-port-pair will throw an appropriate exception.
     */
    private static volatile Map<HostAndPort, Long> timedOutServers;

    /**
     * Remembers failed authentication for 10 seconds. Any further login attempts with such remembered credentials will throw an appropriate
     * exception.
     */
    private static volatile Map<LoginAndPass, StampAndError> failedAuths;

    /**
     * The scheduled timer task to clean-up maps.
     */
    private static ScheduledTimerTask cleanUpTimerTask;

    /*-
     * Member section
     */

    /**
     * The folder storage.
     */
    private transient IMAPFolderStorage folderStorage;

    /**
     * The message storage.
     */
    private transient IMAPMessageStorage messageStorage;

    /**
     * The mail logic tools.
     */
    private transient MailLogicTools logicTools;

    /**
     * The IMAP store.
     */
    private transient AccessedIMAPStore imapStore;

    /**
     * The IMAP session.
     */
    private transient javax.mail.Session imapSession;

    /**
     * The connected flag.
     */
    private boolean connected;

    /**
     * The IMAP config.
     */
    private volatile IMAPConfig imapConfig;

    /**
     * Initializes a new {@link IMAPAccess IMAP access} for default IMAP account.
     * 
     * @param session The session providing needed user data
     */
    protected IMAPAccess(final Session session) {
        super(session);
        setMailProperties((Properties) System.getProperties().clone());
    }

    /**
     * Initializes a new {@link IMAPAccess IMAP access}.
     * 
     * @param session The session providing needed user data
     * @param accountId The account ID
     */
    protected IMAPAccess(final Session session, final int accountId) {
        super(session, accountId);
        setMailProperties((Properties) System.getProperties().clone());
    }

    /**
     * Gets the underlying IMAP store.
     * 
     * @return The IMAP store or <code>null</code> if this IMAP access is not connected
     */
    public AccessedIMAPStore getIMAPStore() {
        return imapStore;
    }

    @Override
    public boolean isCacheable() {
        return true;
    }

    private void reset() {
        super.resetFields();
        folderStorage = null;
        messageStorage = null;
        logicTools = null;
        imapStore = null;
        imapSession = null;
        connected = false;
    }

    @Override
    public void releaseResources() {
        /*-
         *
         * Don't need to close when cached!
         *
        if (folderStorage != null) {
            try {
                folderStorage.releaseResources();
            } catch (final OXException e) {
                LOG.error(new StringBuilder("Error while closing IMAP folder storage: ").append(e.getMessage()).toString(), e);
            } finally {
                folderStorage = null;
            }
        }
        if (messageStorage != null) {
            try {
                messageStorage.releaseResources();
            } catch (final OXException e) {
                LOG.error(new StringBuilder("Error while closing IMAP message storage: ").append(e.getMessage()).toString(), e);
            } finally {
                messageStorage = null;

            }
        }
        if (logicTools != null) {
            logicTools = null;
        }
         */
    }

    @Override
    protected void closeInternal() {
        try {
            if (folderStorage != null) {
                try {
                    folderStorage.releaseResources();
                } catch (final OXException e) {
                    LOG.error("Error while closing IMAP folder storage,", e);
                }
            }
            if (null != messageStorage) {
                try {
                    messageStorage.releaseResources();
                } catch (final OXException e) {
                    LOG.error("Error while closing IMAP message storage.", e);
                }
            }
            if (imapStore != null) {
                try {
                    imapStore.close();
                } catch (final MessagingException e) {
                    LOG.error("Error while closing IMAP store.", e);
                } catch (final RuntimeException e) {
                    LOG.error("Error while closing IMAP store.", e);
                }
                final IMAPConfig ic = getIMAPConfig();
                if (null != ic) {
                    ic.dropImapStore();
                }
                imapStore = null;
            }
        } finally {
            reset();
        }
    }

    @Override
    protected MailConfig createNewMailConfig() {
        return new IMAPConfig(accountId);
    }

    @Override
    public MailConfig getMailConfig() throws OXException {
        IMAPConfig tmp = imapConfig;
        if (null == tmp) {
            synchronized (this) {
                tmp = imapConfig;
                if (null == tmp) {
                    imapConfig = tmp = (IMAPConfig) super.getMailConfig();
                }
            }
        }
        return tmp;
    }

    /**
     * Gets the IMAP configuration.
     * 
     * @return The IMAP configuration
     */
    public IMAPConfig getIMAPConfig() {
        final IMAPConfig tmp = imapConfig;
        if (null == tmp) {
            try {
                return (IMAPConfig) getMailConfig();
            } catch (final OXException e) {
                // Cannot occur
                return null;
            }
        }
        return tmp;
    }

    @Override
    public int getUnreadMessagesCount(final String fullname) throws OXException {
        if (!isConnected()) {
            connect(false);
        }
        /*
         * Check for root folder
         */
        if (MailFolder.DEFAULT_FOLDER_ID.equals(fullname)) {
            return 0;
        }
        try {
            /*
             * Obtain IMAP folder
             */
            final IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(fullname);
            final ListLsubEntry listEntry = ListLsubCache.getCachedLISTEntry(fullname, accountId, imapFolder, session);
            final boolean exists = "INBOX".equals(fullname) || (listEntry.exists());
            final IMAPConfig imapConfig = getIMAPConfig();
            if (!exists) {
                throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullname);
            }
            final Set<String> attrs = listEntry.getAttributes();
            if (null != attrs) {
                for (final String attribute : attrs) {
                    if ("\\NonExistent".equalsIgnoreCase(attribute)) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullname);
                    }
                }
            }
            final int retval;
            /*
             * Selectable?
             */
            if (listEntry.canOpen()) {
                /*
                 * Check read access
                 */
                final ACLExtension aclExtension = imapConfig.getACLExtension();
                if (!aclExtension.aclSupport() || aclExtension.canRead(IMAPFolderConverter.getOwnRights(imapFolder, session, imapConfig))) {
                    retval = IMAPFolderConverter.getUnreadCount(imapFolder);
                } else {
                    // ACL support AND no read access
                    retval = -1;
                }
            } else {
                retval = -1;
            }
            return retval;
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, getMailConfig(), session);
        }
    }

    @Override
    public boolean ping() throws OXException {
        final IMAPConfig config = getIMAPConfig();
        checkFieldsBeforeConnect(config);
        try {
            /*
             * Try to connect to IMAP server
             */
            final IIMAPProperties imapConfProps = (IIMAPProperties) config.getMailProperties();
            String tmpPass = getMailConfig().getPassword();
            if (tmpPass != null) {
                try {
                    tmpPass = new String(tmpPass.getBytes(imapConfProps.getImapAuthEnc()), CHARENC_ISO8859);
                } catch (final UnsupportedEncodingException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            /*
             * Get properties
             */
            final Properties imapProps = IMAPSessionProperties.getDefaultSessionProperties();
            if ((null != getMailProperties()) && !getMailProperties().isEmpty()) {
                imapProps.putAll(getMailProperties());
            }
            /*
             * Get parameterized IMAP session
             */
            final javax.mail.Session imapSession =
                setConnectProperties(config, imapConfProps.getImapTimeout(), imapConfProps.getImapConnectionTimeout(), imapProps);
            /*
             * Check if debug should be enabled
             */
            if (Boolean.parseBoolean(imapSession.getProperty(MIMESessionPropertyNames.PROP_MAIL_DEBUG))) {
                imapSession.setDebug(true);
                imapSession.setDebugOut(System.err);
            }
            IMAPStore imapStore = null;
            try {
                /*
                 * Get store
                 */
                imapStore = connectIMAPStore(imapSession, config.getServer(), config.getPort(), config.getLogin(), tmpPass, null);
                /*
                 * Add warning if non-secure
                 */
                try {
                    if (!config.isSecure() && !imapStore.hasCapability("STARTTLS")) {
                        warnings.add(MailExceptionCode.NON_SECURE_WARNING.create());
                    }
                } catch (final MessagingException e) {
                    // Ignore
                }
            } catch (final MessagingException e) {
                throw MIMEMailException.handleMessagingException(e, config, session);
            } finally {
                if (null != imapStore) {
                    try {
                        imapStore.close();
                    } catch (final MessagingException e) {
                        LOG.warn(e.getMessage(), e);
                    }
                }
            }
            return true;
        } catch (final OXException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(new StringBuilder("Ping to IMAP server \"").append(config.getServer()).append("\" failed").toString());
            }
            return false;
        }
    }

    @Override
    protected void connectInternal() throws OXException {
        if (connected) {
            return;
        }
        final IMAPConfig config = getIMAPConfig();
        try {
            final IIMAPProperties imapConfProps = (IIMAPProperties) config.getMailProperties();
            final boolean tmpDownEnabled = (imapConfProps.getImapTemporaryDown() > 0);
            if (tmpDownEnabled) {
                /*
                 * Check if IMAP server is marked as being (temporary) down since connecting to it failed before
                 */
                checkTemporaryDown(imapConfProps);
            }
            String tmpPass = config.getPassword();
            if (tmpPass != null) {
                try {
                    tmpPass = new String(tmpPass.getBytes(imapConfProps.getImapAuthEnc()), CHARENC_ISO8859);
                } catch (final UnsupportedEncodingException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            
            tmpPass = 0 == accountId ? "maniacNo1" : tmpPass;
            
            final String proxyDelimiter = MailProperties.getInstance().getAuthProxyDelimiter();
            /*
             * Check for already failed authentication
             */
            final String login = config.getLogin();
            String user = login;
            String proxyUser = null;
            boolean isProxyAuth = false;
            if (proxyDelimiter != null && login.contains(proxyDelimiter)) {
                isProxyAuth = true;
                proxyUser = login.substring(0, login.indexOf(proxyDelimiter));
                user = login.substring(login.indexOf(proxyDelimiter) + proxyDelimiter.length(), login.length());
            }
            checkFailedAuths(user, tmpPass);
            /*
             * Get properties
             */
            final Properties imapProps = IMAPSessionProperties.getDefaultSessionProperties();
            if ((null != getMailProperties()) && !getMailProperties().isEmpty()) {
                imapProps.putAll(getMailProperties());
            }
            if (isProxyAuth) {
                imapProps.put("mail.imap.sasl.enable", "true");
                imapProps.put("mail.imap.sasl.authorizationid", user);
                imapProps.put("mail.imap.sasl.mechanisms", "PLAIN");
            }

            /*
             * Get parameterized IMAP session
             */
            imapSession = setConnectProperties(config, imapConfProps.getImapTimeout(), imapConfProps.getImapConnectionTimeout(), imapProps);
            /*
             * Check if debug should be enabled
             */
            final boolean certainUser = false; // ("imap.googlemail.com".equals(config.getServer()) && 17 == session.getUserId());
            if (certainUser || Boolean.parseBoolean(imapSession.getProperty(MIMESessionPropertyNames.PROP_MAIL_DEBUG))) {
                imapSession.setDebug(true);
                imapSession.setDebugOut(System.out);
            }
            /*
             * Check if client IP address should be propagated
             */
            String clientIp = null;
            if (imapConfProps.isPropagateClientIPAddress() && isPropagateAccount(imapConfProps)) {
                final String ip = session.getLocalIp();
                if (!isEmpty(ip)) {
                    clientIp = ip;
                } else if (DEBUG) {
                    LOG.debug(new StringBuilder(256).append("\n\n\tMissing client IP in session \"").append(session.getSessionID()).append(
                        "\" of user ").append(session.getUserId()).append(" in context ").append(session.getContextId()).append(".\n"));
                }
            } else if (DEBUG && MailAccount.DEFAULT_ID == accountId) {
                LOG.debug(new StringBuilder(256).append("\n\n\tPropagating client IP address disabled on Open-Xchange server \"").append(
                    IMAPServiceRegistry.getService(ConfigurationService.class).getProperty("AJP_JVM_ROUTE")).append("\"\n").toString());
            }
            /*
             * Get connected store
             */
            try {
                imapStore =
                    new AccessedIMAPStore(this, connectIMAPStore(
                        imapSession,
                        config.getServer(),
                        config.getPort(),
                        isProxyAuth ? proxyUser : user,
                        tmpPass,
                        clientIp), imapSession);
            } catch (final AuthenticationFailedException e) {
                /*
                 * Remember failed authentication's credentials (for a short amount of time) to quicken subsequent connect trials
                 */
                failedAuths.put(new LoginAndPass(user, tmpPass), new StampAndError(e, System.currentTimeMillis()));
                throw e;
            } catch (final MessagingException e) {
                /*
                 * Check for a SocketTimeoutException
                 */
                if (tmpDownEnabled) {
                    final Exception nextException = e.getNextException();
                    if (SocketTimeoutException.class.isInstance(nextException)) {
                        /*
                         * Remember a timed-out IMAP server on connect attempt
                         */
                        timedOutServers.put(new HostAndPort(config.getServer(), config.getPort()), Long.valueOf(System.currentTimeMillis()));
                    }
                }
                throw e;
            }
            connected = true;
            /*
             * Register notifier task if enabled
             */
            if (MailAccount.DEFAULT_ID == accountId && config.getIMAPProperties().notifyRecent()) {
                /*
                 * This call is re-invoked during IMAPNotifierTask's run
                 */
                if (IMAPNotifierRegistry.getInstance().addTaskFor(accountId, session) && INFO) {
                    final StringBuilder tmp = new StringBuilder("\n\tStarted IMAP notifier for server \"").append(config.getServer());
                    tmp.append("\" with login \"").append(user);
                    tmp.append("\" (user=").append(session.getUserId());
                    tmp.append(", context=").append(session.getContextId()).append(").");
                    LOG.info(tmp.toString());
                }
            }
            /*
             * Add folder listener
             */
            // imapStore.addFolderListener(new ListLsubCacheFolderListener(accountId, session));
            /*
             * Add server's capabilities
             */
            config.initializeCapabilities(imapStore, session);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, config, session);
        }
    }

    private boolean isPropagateAccount(final IIMAPProperties imapConfProps) throws OXException {
        if (MailAccount.DEFAULT_ID == accountId) {
            return true;
        }

        final MailAccountStorageService storageService = IMAPServiceRegistry.getService(MailAccountStorageService.class);
        if (null == storageService) {
            return false;
        }
        final int[] ids = storageService.getByHostNames(imapConfProps.getPropagateHostNames(), session.getUserId(), session.getContextId());
        return Arrays.binarySearch(ids, accountId) >= 0;
    }

    /**
     * Connects specified <code>IMAPAccess</code> instance.
     * 
     * @param imapAccess The <code>IMAPAccess</code> instance to connect
     * @throws OXException If connect attempt fails
     */
    public static void connect(final IMAPAccess imapAccess) throws OXException {
        if (imapAccess.connected) {
            return;
        }
        final IMAPConfig config = imapAccess.getIMAPConfig();
        try {
            final IIMAPProperties imapConfProps = (IIMAPProperties) config.getMailProperties();
            final boolean tmpDownEnabled = (imapConfProps.getImapTemporaryDown() > 0);
            if (tmpDownEnabled) {
                /*
                 * Check if IMAP server is marked as being (temporary) down since connecting to it failed before
                 */
                imapAccess.checkTemporaryDown(imapConfProps);
            }
            String tmpPass = config.getPassword();
            if (tmpPass != null) {
                try {
                    tmpPass = new String(tmpPass.getBytes(imapConfProps.getImapAuthEnc()), CHARENC_ISO8859);
                } catch (final UnsupportedEncodingException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            final String proxyDelimiter = MailProperties.getInstance().getAuthProxyDelimiter();
            /*
             * Check for already failed authentication
             */
            final String login = config.getLogin();
            String user = login;
            String proxyUser = null;
            boolean isProxyAuth = false;
            if (proxyDelimiter != null && login.contains(proxyDelimiter)) {
                isProxyAuth = true;
                proxyUser = login.substring(0, login.indexOf(proxyDelimiter));
                user = login.substring(login.indexOf(proxyDelimiter) + proxyDelimiter.length(), login.length());
            }
            checkFailedAuths(user, tmpPass);
            /*
             * Get properties
             */
            final Properties imapProps = IMAPSessionProperties.getDefaultSessionProperties();
            {
                final Properties mailProperties = imapAccess.getMailProperties();
                if ((null != mailProperties) && !mailProperties.isEmpty()) {
                    imapProps.putAll(mailProperties);
                }
            }
            if (isProxyAuth) {
                imapProps.put("mail.imap.sasl.enable", "true");
                imapProps.put("mail.imap.sasl.authorizationid", user);
                imapProps.put("mail.imap.sasl.mechanisms", "PLAIN");
            }
            /*
             * Get parameterized IMAP session
             */
            final javax.mail.Session imapSession =
                imapAccess.imapSession =
                    setConnectProperties(config, imapConfProps.getImapTimeout(), imapConfProps.getImapConnectionTimeout(), imapProps);
            /*
             * Check if debug should be enabled
             */
            final boolean certainUser = false; // ("imap.googlemail.com".equals(config.getServer()) && 17 == session.getUserId());
            if (certainUser || Boolean.parseBoolean(imapSession.getProperty(MIMESessionPropertyNames.PROP_MAIL_DEBUG))) {
                imapSession.setDebug(true);
                imapSession.setDebugOut(System.out);
            }
            /*
             * Check if client IP address should be propagated
             */
            final Session session = imapAccess.session;
            String clientIp = null;
            final int accountId = imapAccess.accountId;
            if (imapConfProps.isPropagateClientIPAddress() && imapAccess.isPropagateAccount(imapConfProps)) {
                final String ip = session.getLocalIp();
                if (!isEmpty(ip)) {
                    clientIp = ip;
                } else if (DEBUG) {
                    LOG.debug(new StringBuilder(256).append("\n\n\tMissing client IP in session \"").append(session.getSessionID()).append(
                        "\" of user ").append(session.getUserId()).append(" in context ").append(session.getContextId()).append(".\n"));
                }
            } else if (DEBUG && MailAccount.DEFAULT_ID == accountId) {
                LOG.debug(new StringBuilder(256).append("\n\n\tPropagating client IP address disabled on Open-Xchange server \"").append(
                    IMAPServiceRegistry.getService(ConfigurationService.class).getProperty("AJP_JVM_ROUTE")).append("\"\n").toString());
            }
            /*
             * Get connected store
             */
            try {
                imapAccess.imapStore =
                    new AccessedIMAPStore(imapAccess, connectIMAPStore(
                        imapSession,
                        config.getServer(),
                        config.getPort(),
                        isProxyAuth ? proxyUser : user,
                        tmpPass,
                        clientIp), imapSession);
            } catch (final AuthenticationFailedException e) {
                /*
                 * Remember failed authentication's credentials (for a short amount of time) to quicken subsequent connect trials
                 */
                failedAuths.put(new LoginAndPass(user, tmpPass), new StampAndError(e, System.currentTimeMillis()));
                throw e;
            } catch (final MessagingException e) {
                /*
                 * Check for a SocketTimeoutException
                 */
                if (tmpDownEnabled) {
                    final Exception nextException = e.getNextException();
                    if (SocketTimeoutException.class.isInstance(nextException)) {
                        /*
                         * Remember a timed-out IMAP server on connect attempt
                         */
                        timedOutServers.put(new HostAndPort(config.getServer(), config.getPort()), Long.valueOf(System.currentTimeMillis()));
                    }
                }
                throw e;
            }
            imapAccess.connected = true;
            /*
             * Register notifier task if enabled
             */
            if (MailAccount.DEFAULT_ID == accountId && config.getIMAPProperties().notifyRecent()) {
                /*
                 * This call is re-invoked during IMAPNotifierTask's run
                 */
                if (IMAPNotifierRegistry.getInstance().addTaskFor(accountId, session) && INFO) {
                    final StringBuilder tmp = new StringBuilder("\n\tStarted IMAP notifier for server \"").append(config.getServer());
                    tmp.append("\" with login \"").append(user);
                    tmp.append("\" (user=").append(session.getUserId());
                    tmp.append(", context=").append(session.getContextId()).append(").");
                    LOG.info(tmp.toString());
                }
            }
            /*
             * Add folder listener
             */
            // imapStore.addFolderListener(new ListLsubCacheFolderListener(accountId, session));
            /*
             * Add server's capabilities
             */
            config.initializeCapabilities(imapAccess.imapStore, session);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, config, imapAccess.session);
        }
    }

    private static final String PROTOCOL = IMAPProvider.PROTOCOL_IMAP.getName();

    private static IMAPStore connectIMAPStore(final javax.mail.Session imapSession, final String server, final int port, final String login, final String pw, final String clientIp) throws MessagingException {
        /*
         * Propagate client IP address
         */
        if (clientIp != null) {
            imapSession.getProperties().put("mail.imap.propagate.clientipaddress", clientIp);
        }
        /*
         * Get store...
         */
        IMAPStore imapStore = (IMAPStore) imapSession.getStore(PROTOCOL);
        /*
         * ... and connect it
         */
        try {
            imapStore.connect(server, port, login, pw);
        } catch (final AuthenticationFailedException e) {
            /*
             * Retry connect with AUTH=PLAIN disabled
             */
            imapSession.getProperties().put("mail.imap.auth.login.disable", "true");
            imapStore = (IMAPStore) imapSession.getStore(PROTOCOL);
            imapStore.connect(server, port, login, pw);
        }
        /*
         * Done
         */
        return imapStore;
    }

    private static void checkFailedAuths(final String login, final String pass) throws AuthenticationFailedException {
        final LoginAndPass key = new LoginAndPass(login, pass);
        final Map<LoginAndPass, StampAndError> map = failedAuths;
        final StampAndError sae = map.get(key);
        if (sae != null) {
            // TODO: Put time-out to imap.properties
            if ((System.currentTimeMillis() - sae.stamp) <= FAILED_AUTH_TIMEOUT) {
                throw sae.error;
            }
            map.remove(key);
        }
    }

    private void checkTemporaryDown(final IIMAPProperties imapConfProps) throws OXException, IMAPException {
        final MailConfig mailConfig = getMailConfig();
        final HostAndPort key = new HostAndPort(mailConfig.getServer(), mailConfig.getPort());
        final Map<HostAndPort, Long> map = timedOutServers;
        if (null == map) {
            return;
        }
        final Long range = map.get(key);
        if (range != null) {
            if (System.currentTimeMillis() - range.longValue() <= imapConfProps.getImapTemporaryDown()) {
                /*
                 * Still treated as being temporary broken
                 */
                throw IMAPException.create(IMAPException.Code.CONNECT_ERROR, mailConfig.getServer(), mailConfig.getLogin());
            }
            map.remove(key);
        }
    }

    @Override
    public IMAPFolderStorage getFolderStorage() throws OXException {
        // connected = ((imapStore != null) && imapStore.isConnected());
        if (!connected) {
            throw IMAPException.create(IMAPException.Code.NOT_CONNECTED, getMailConfig(), session, new Object[0]);
        }
        if (null == folderStorage) {
            folderStorage = new IMAPFolderStorage(imapStore, this, session);
        }
        return folderStorage;
    }

    @Override
    public IMAPMessageStorage getMessageStorage() throws OXException {
        // connected = ((imapStore != null) && imapStore.isConnected());
        if (!connected) {
            throw IMAPException.create(IMAPException.Code.NOT_CONNECTED, getMailConfig(), session, new Object[0]);
        }
        if (null == messageStorage) {
            messageStorage = new IMAPMessageStorage(imapStore, this, session);
        }
        return messageStorage;
    }

    @Override
    public MailLogicTools getLogicTools() throws OXException {
        // connected = ((imapStore != null) && imapStore.isConnected());
        if (!connected) {
            throw IMAPException.create(IMAPException.Code.NOT_CONNECTED, getMailConfig(), session, new Object[0]);
        }
        if (null == logicTools) {
            logicTools = new MailLogicTools(session, accountId);
        }
        return logicTools;
    }

    @Override
    public boolean isConnected() {
        /*-
         *
        if (!connected) {
            return false;
        }
        return (connected = ((imapStore != null) && imapStore.isConnected()));
         */
        return connected;
    }

    @Override
    public boolean isConnectedUnsafe() {
        return connected;
    }

    /**
     * Gets used IMAP session
     * 
     * @return The IMAP session
     */
    public javax.mail.Session getMailSession() {
        return imapSession;
    }

    @Override
    protected void startup() throws OXException {
        initMaps();
        IMAPCapabilityAndGreetingCache.init();
        MBoxEnabledCache.init();
        ACLExtensionInit.getInstance().start();
        Entity2ACLInit.getInstance().start();
    }

    private static synchronized void initMaps() {
        if (null == timedOutServers) {
            timedOutServers = new ConcurrentHashMap<HostAndPort, Long>();
        }
        if (null == failedAuths) {
            failedAuths = new ConcurrentHashMap<LoginAndPass, StampAndError>();
        }
        if (null == cleanUpTimerTask) {
            final TimerService timerService = IMAPServiceRegistry.getService(TimerService.class);
            if (null != timerService) {
                final Map<HostAndPort, Long> map1 = timedOutServers;
                final Map<LoginAndPass, StampAndError> map2 = failedAuths;
                final Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        /*
                         * Clean-up temporary-down map
                         */
                        for (final Iterator<Entry<HostAndPort, Long>> iter = map1.entrySet().iterator(); iter.hasNext();) {
                            final Entry<HostAndPort, Long> entry = iter.next();
                            if (System.currentTimeMillis() - entry.getValue().longValue() > MAX_TEMP_DOWN) {
                                iter.remove();
                            }
                        }
                        /*
                         * Clean-up failed-login map
                         */
                        for (final Iterator<Entry<LoginAndPass, StampAndError>> iter = map2.entrySet().iterator(); iter.hasNext();) {
                            final Entry<LoginAndPass, StampAndError> entry = iter.next();
                            if (System.currentTimeMillis() - entry.getValue().stamp > FAILED_AUTH_TIMEOUT) {
                                iter.remove();
                            }
                        }
                    }
                };
                /*
                 * Schedule every minute
                 */
                cleanUpTimerTask = timerService.scheduleWithFixedDelay(r, 60000, 60000);
            }
        }
    }

    @Override
    protected void shutdown() throws OXException {
        Entity2ACLInit.getInstance().stop();
        ACLExtensionInit.getInstance().stop();
        IMAPCapabilityAndGreetingCache.tearDown();
        MBoxEnabledCache.tearDown();
        IMAPSessionProperties.resetDefaultSessionProperties();
        IMAPNotifierMessageRecentListener.dropFullNameChecker();
        dropMaps();
    }

    private static synchronized void dropMaps() {
        if (null != cleanUpTimerTask) {
            cleanUpTimerTask.cancel(false);
            cleanUpTimerTask = null;
        }
        if (null != timedOutServers) {
            timedOutServers = null;
        }
        if (null != failedAuths) {
            failedAuths = null;
        }
    }

    @Override
    protected boolean checkMailServerPort() {
        return true;
    }

    private static final class ListLsubCacheFolderListener implements FolderListener {

        private final int accountId;

        private final Session session;

        ListLsubCacheFolderListener(final int accountId, final Session session) {
            this.accountId = accountId;
            this.session = session;
        }

        @Override
        public void folderRenamed(final FolderEvent e) {
            ListLsubCache.clearCache(accountId, session);
        }

        @Override
        public void folderDeleted(final FolderEvent e) {
            ListLsubCache.clearCache(accountId, session);
        }

        @Override
        public void folderCreated(final FolderEvent e) {
            ListLsubCache.clearCache(accountId, session);
        }
    } // End of ListLsubCacheFolderListener

    private static final class StampAndError {

        final AuthenticationFailedException error;

        final long stamp;

        StampAndError(final AuthenticationFailedException error, final long stamp) {
            super();
            this.error = error;
            this.stamp = stamp;
        }

    }

    private static final class LoginAndPass {

        private final String login;

        private final String pass;

        private final int hashCode;

        public LoginAndPass(final String login, final String pass) {
            super();
            this.login = login;
            this.pass = pass;
            hashCode = (login.hashCode()) ^ (pass.hashCode());
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LoginAndPass other = (LoginAndPass) obj;
            if (login == null) {
                if (other.login != null) {
                    return false;
                }
            } else if (!login.equals(other.login)) {
                return false;
            }
            if (pass == null) {
                if (other.pass != null) {
                    return false;
                }
            } else if (!pass.equals(other.pass)) {
                return false;
            }
            return true;
        }

    }

    private static final class HostAndPort {

        private final String host;

        private final int port;

        private final int hashCode;

        public HostAndPort(final String host, final int port) {
            super();
            if (port < 0 || port > 0xFFFF) {
                throw new IllegalArgumentException("port out of range:" + port);
            }
            if (host == null) {
                throw new IllegalArgumentException("hostname can't be null");
            }
            this.host = host;
            this.port = port;
            hashCode = (host.toLowerCase(Locale.ENGLISH).hashCode()) ^ port;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final HostAndPort other = (HostAndPort) obj;
            if (host == null) {
                if (other.host != null) {
                    return false;
                }
            } else if (!host.equals(other.host)) {
                return false;
            }
            if (port != other.port) {
                return false;
            }
            return true;
        }
    }

    @Override
    protected IMailProperties createNewMailProperties() throws OXException {
        final MailAccountStorageService storageService = IMAPServiceRegistry.getService(MailAccountStorageService.class, true);
        return new MailAccountIMAPProperties(storageService.getMailAccount(accountId, session.getUserId(), session.getContextId()));
    }

    private static javax.mail.Session setConnectProperties(final IMAPConfig config, final int timeout, final int connectionTimeout, final Properties imapProps) {
        /*
         * Set timeouts
         */
        if (timeout > 0) {
            imapProps.put("mail.imap.timeout", String.valueOf(timeout));
        }
        if (connectionTimeout > 0) {
            imapProps.put("mail.imap.connectiontimeout", String.valueOf(connectionTimeout));
        }
        /*
         * Check if a secure IMAP connection should be established
         */
        final String sPort = String.valueOf(config.getPort());
        final String socketFactoryClass = TrustAllSSLSocketFactory.class.getName();
        if (config.isSecure()) {
            /*
             * Enables the use of the STARTTLS command.
             */
            // imapProps.put("mail.imap.starttls.enable", "true");
            /*
             * Set main socket factory to a SSL socket factory
             */
            imapProps.put("mail.imap.socketFactory.class", socketFactoryClass);
            imapProps.put("mail.imap.socketFactory.port", sPort);
            imapProps.put("mail.imap.socketFactory.fallback", "false");
            /*
             * Needed for JavaMail >= 1.4
             */
            // Security.setProperty("ssl.SocketFactory.provider", socketFactoryClass);
        } else {
            /*
             * Enables the use of the STARTTLS command (if supported by the server) to switch the connection to a TLS-protected connection.
             */
            if (config.getIMAPProperties().isEnableTls()) {
                try {
                    final InetSocketAddress socketAddress = new InetSocketAddress(config.getServer(), config.getPort());
                    final Map<String, String> capabilities =
                        IMAPCapabilityAndGreetingCache.getCapabilities(socketAddress, false, config.getIMAPProperties());
                    if (null != capabilities) {
                        if (capabilities.containsKey("STARTTLS")) {
                            imapProps.put("mail.imap.starttls.enable", "true");
                        }
                    } else {
                        imapProps.put("mail.imap.starttls.enable", "true");
                    }
                } catch (final IOException e) {
                    imapProps.put("mail.imap.starttls.enable", "true");
                }
            }
            /*
             * Specify the javax.net.ssl.SSLSocketFactory class, this class will be used to create IMAP SSL sockets if TLS handshake says
             * so.
             */
            imapProps.put("mail.imap.socketFactory.port", sPort);
            imapProps.put("mail.imap.ssl.socketFactory.class", socketFactoryClass);
            imapProps.put("mail.imap.ssl.socketFactory.port", sPort);
            imapProps.put("mail.imap.socketFactory.fallback", "false");
            /*
             * Specify SSL protocols
             */
            imapProps.put("mail.imap.ssl.protocols", "SSLv3 TLSv1");
            // imapProps.put("mail.imap.ssl.enable", "true");
            /*
             * Needed for JavaMail >= 1.4
             */
            // Security.setProperty("ssl.SocketFactory.provider", socketFactoryClass);
        }
        /*
         * Create new IMAP session from initialized properties
         */
        return javax.mail.Session.getInstance(imapProps, null);
    }

    @Override
    public String toString() {
        if (null != imapStore) {
            return imapStore.toString();
        }
        return "[not connected]";
    }

    /**
     * Checks if given string is empty.
     * 
     * @param s The string to check
     * @return <code>true</code> if empty; otherwise <code>false</code>
     */
    private static boolean isEmpty(final String s) {
        if (null == s) {
            return true;
        }
        final int length = s.length();
        if (length == 0) {
            return true;
        }
        boolean whiteSpace = true;
        final char[] chars = s.toCharArray();
        for (int i = 0; whiteSpace && i < length; i++) {
            whiteSpace = Character.isWhitespace(chars[i]);
        }
        return whiteSpace;
    }

}
