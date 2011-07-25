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

package com.openexchange.imap.config;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.imap.IMAPProtocol;
import com.openexchange.imap.entity2acl.Entity2ACL;
import com.openexchange.imap.services.IMAPServiceRegistry;
import com.openexchange.mail.api.AbstractProtocolProperties;
import com.openexchange.mail.api.IMailProperties;
import com.openexchange.mail.api.MailConfig.BoolCapVal;
import com.openexchange.mail.config.MailConfigException;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.spamhandler.SpamHandler;

/**
 * {@link IMAPProperties}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class IMAPProperties extends AbstractProtocolProperties implements IIMAPProperties {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(IMAPProperties.class));

    private static final IMAPProperties instance = new IMAPProperties();

    /**
     * Gets the singleton instance of {@link IMAPProperties}
     * 
     * @return The singleton instance of {@link IMAPProperties}
     */
    public static IMAPProperties getInstance() {
        return instance;
    }

    /*-
     * Fields for global properties
     */

    private final IMailProperties mailProperties;

    private boolean imapSort;

    private boolean imapSearch;

    private boolean fastFetch;

    private boolean notifyRecent;

    private int notifyFrequencySeconds;

    private String notifyFullNames;

    private BoolCapVal supportsACLs;

    private int imapTimeout;

    private int imapConnectionTimeout;

    private int imapConnectionIdleTime;

    private int imapTemporaryDown;

    private String imapAuthEnc;

    private String entity2AclImpl;

    private int blockSize;

    private final Map<String, Boolean> newACLExtMap;

    private String spamHandlerName;

    private boolean propagateClientIPAddress;

    private Set<String> propagateHostNames;

    /**
     * Initializes a new {@link IMAPProperties}
     */
    private IMAPProperties() {
        super();
        newACLExtMap = new ConcurrentHashMap<String, Boolean>();
        mailProperties = MailProperties.getInstance();
        propagateHostNames = Collections.emptySet();
    }

    @Override
    protected void loadProperties0() throws OXException {
        final StringBuilder logBuilder = new StringBuilder(1024);
        logBuilder.append("\nLoading global IMAP properties...\n");

        final ConfigurationService configuration = IMAPServiceRegistry.getService(ConfigurationService.class);
        {
            final String tmp = configuration.getProperty("com.openexchange.imap.notifyRecent", STR_FALSE).trim();
            notifyRecent = Boolean.parseBoolean(tmp);
            logBuilder.append("\tNotify Recent: ").append(notifyRecent).append('\n');
        }

        {
            final String tmp = configuration.getProperty("com.openexchange.imap.notifyFrequencySeconds", "300").trim();
            try {
                notifyFrequencySeconds = Integer.parseInt(tmp);
                logBuilder.append("\tNotify Frequency Seconds: ").append(notifyFrequencySeconds).append('\n');
            } catch (final NumberFormatException e) {
                notifyFrequencySeconds = 300;
                logBuilder.append("\tNotify Frequency Seconds: Invalid value \"").append(tmp).append("\". Setting to fallback: ").append(
                    notifyFrequencySeconds).append('\n');
            }
        }

        {
            final String tmp = configuration.getProperty("com.openexchange.imap.notifyFullNames", "INBOX").trim();
            notifyFullNames = tmp;
        }

        {
            final String imapSortStr = configuration.getProperty("com.openexchange.imap.imapSort", "application").trim();
            imapSort = "imap".equalsIgnoreCase(imapSortStr);
            logBuilder.append("\tIMAP-Sort: ").append(imapSort).append('\n');
        }

        {
            final String imapSearchStr = configuration.getProperty("com.openexchange.imap.imapSearch", "imap").trim();
            imapSearch = "imap".equalsIgnoreCase(imapSearchStr);
            logBuilder.append("\tIMAP-Search: ").append(imapSearch).append('\n');
        }

        {
            final String fastFetchStr = configuration.getProperty("com.openexchange.imap.imapFastFetch", STR_TRUE).trim();
            fastFetch = Boolean.parseBoolean(fastFetchStr);
            logBuilder.append("\tFast Fetch Enabled: ").append(fastFetch).append('\n');
        }

        {
            final String tmp = configuration.getProperty("com.openexchange.imap.propagateClientIPAddress", STR_FALSE).trim();
            propagateClientIPAddress = Boolean.parseBoolean(tmp);
            logBuilder.append("\tPropagate Client IP Address: ").append(propagateClientIPAddress).append('\n');
        }

        {
            final String tmp = configuration.getProperty("com.openexchange.imap.propagateHostNames", "").trim();
            if (tmp.length() > 0) {
                propagateHostNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(tmp.split(" *, *"))));
            } else {
                propagateHostNames = Collections.emptySet();
            }
            logBuilder.append("\tPropagate Host Names: ").append(propagateHostNames.isEmpty() ? "<none>" : propagateHostNames.toString()).append(
                '\n');
        }

        {
            final String supportsACLsStr = configuration.getProperty("com.openexchange.imap.imapSupportsACL", STR_FALSE).trim();
            supportsACLs = BoolCapVal.parseBoolCapVal(supportsACLsStr);
            logBuilder.append("\tSupport ACLs: ").append(supportsACLs).append('\n');
        }

        {
            final String imapTimeoutStr = configuration.getProperty("com.openexchange.imap.imapTimeout", "0").trim();
            try {
                imapTimeout = Integer.parseInt(imapTimeoutStr);
                logBuilder.append("\tIMAP Timeout: ").append(imapTimeout).append('\n');
            } catch (final NumberFormatException e) {
                imapTimeout = 0;
                logBuilder.append("\tIMAP Timeout: Invalid value \"").append(imapTimeoutStr).append("\". Setting to fallback: ").append(
                    imapTimeout).append('\n');
            }
        }

        {
            final String imapConTimeoutStr = configuration.getProperty("com.openexchange.imap.imapConnectionTimeout", "0").trim();
            try {
                imapConnectionTimeout = Integer.parseInt(imapConTimeoutStr);
                logBuilder.append("\tIMAP Connection Timeout: ").append(imapConnectionTimeout).append('\n');
            } catch (final NumberFormatException e) {
                imapConnectionTimeout = 0;
                logBuilder.append("\tIMAP Connection Timeout: Invalid value \"").append(imapConTimeoutStr).append(
                    "\". Setting to fallback: ").append(imapConnectionTimeout).append('\n');
            }
        }

        {
            final String imapTempDownStr = configuration.getProperty("com.openexchange.imap.imapTemporaryDown", "0").trim();
            try {
                imapTemporaryDown = Integer.parseInt(imapTempDownStr);
                logBuilder.append("\tIMAP Temporary Down: ").append(imapTemporaryDown).append('\n');
            } catch (final NumberFormatException e) {
                imapTemporaryDown = 0;
                logBuilder.append("\tIMAP Temporary Down: Invalid value \"").append(imapTempDownStr).append("\". Setting to fallback: ").append(
                    imapTemporaryDown).append('\n');
            }
        }

        {
            final String maxConIdleTime = configuration.getProperty("com.openexchange.imap.maxIMAPConnectionIdleTime", "60000").trim();
            try {
                imapConnectionIdleTime = Integer.parseInt(maxConIdleTime);
                logBuilder.append("\tMax IMAP Connection Idle Time: ").append(imapConnectionIdleTime).append('\n');
            } catch (final NumberFormatException e) {
                imapConnectionIdleTime = 60000;
                logBuilder.append("\tMax IMAP Connection Idle Time: Invalid value \"").append(maxConIdleTime).append(
                    "\". Setting to fallback: ").append(imapConnectionIdleTime).append('\n');
            }
        }

        {
            final String imapAuthEncStr = configuration.getProperty("com.openexchange.imap.imapAuthEnc", "UTF-8").trim();
            if (Charset.isSupported(imapAuthEncStr)) {
                imapAuthEnc = imapAuthEncStr;
                logBuilder.append("\tAuthentication Encoding: ").append(imapAuthEnc).append('\n');
            } else {
                imapAuthEnc = "UTF-8";
                logBuilder.append("\tAuthentication Encoding: Unsupported charset \"").append(imapAuthEncStr).append(
                    "\". Setting to fallback: ").append(imapAuthEnc).append('\n');
            }
        }

        {
            entity2AclImpl = configuration.getProperty("com.openexchange.imap.User2ACLImpl");
            if (null == entity2AclImpl) {
                throw MailConfigException.create("Missing IMAP property \"com.openexchange.imap.User2ACLImpl\"");
            }
            entity2AclImpl = entity2AclImpl.trim();
        }

        {
            final String blockSizeStr = configuration.getProperty("com.openexchange.imap.blockSize", "1000").trim();
            try {
                blockSize = Integer.parseInt(blockSizeStr);
                logBuilder.append("\tBlock Size: ").append(blockSize).append('\n');
            } catch (final NumberFormatException e) {
                blockSize = 1000;
                logBuilder.append("\tBlock Size: Invalid value \"").append(blockSizeStr).append("\". Setting to fallback: ").append(
                    blockSize).append('\n');
            }
        }

        {
            String tmp = configuration.getProperty("com.openexchange.imap.maxNumExternalConnections");
            if (null != tmp) {
                tmp = tmp.trim();
                if (0 == tmp.length()) {
                    IMAPProtocol.getInstance().setOverallMaxCount(-1);
                    logBuilder.append("\tMax. Number of External Connections: ").append("No restrictions").append('\n');
                } else if (tmp.indexOf(':') > 0) {
                    // Expect a comma-separated list
                    final String[] sa = tmp.split(" *, *");
                    if (sa.length > 0) {
                        try {
                            final IMAPProtocol imapProtocol = IMAPProtocol.getInstance();
                            imapProtocol.initMaxCountMap();
                            final StringBuilder sb = new StringBuilder(128).append("\tMax. Number of External Connections: ");
                            boolean first = true;
                            for (final String desc : sa) {
                                final int pos = desc.indexOf(':');
                                if (pos > 0) {
                                    try {
                                        imapProtocol.putIfAbsent(desc.substring(0, pos), Integer.parseInt(desc.substring(pos + 1).trim()));
                                        if (first) {
                                            first = false;
                                        } else {
                                            sb.append(", ");
                                        }
                                        sb.append(desc);
                                    } catch (final RuntimeException e) {
                                        LOG.warn("Max. Number of External Connections: Invalid entry: " + desc, e);
                                    }
                                }
                            }
                            logBuilder.append(sb).append('\n');
                        } catch (final NumberFormatException e) {
                            IMAPProtocol.getInstance().setOverallMaxCount(-1);
                            logBuilder.append("\tMax. Number of External Connections: Invalid value \"").append(tmp).append(
                                "\". Setting to fallback: No restrictions").append('\n');
                        } catch (final OXException e) {
                            IMAPProtocol.getInstance().setOverallMaxCount(-1);
                            logBuilder.append("\tMax. Number of External Connections: Invalid value \"").append(tmp).append(
                                "\". Setting to fallback: No restrictions").append('\n');
                        }
                    }
                } else {
                    // Expect a single integer value
                    try {
                        IMAPProtocol.getInstance().setOverallMaxCount(Integer.parseInt(tmp));
                        logBuilder.append("\tMax. Number of External Connections: ").append(tmp).append(
                            " (applied to all external IMAP accounts)").append('\n');
                    } catch (final NumberFormatException e) {
                        IMAPProtocol.getInstance().setOverallMaxCount(-1);
                        logBuilder.append("\tMax. Number of External Connections: Invalid value \"").append(tmp).append(
                            "\". Setting to fallback: No restrictions").append('\n');
                    }
                }
            }
        }

        spamHandlerName = configuration.getProperty("com.openexchange.imap.spamHandler", SpamHandler.SPAM_HANDLER_FALLBACK).trim();
        logBuilder.append("\tSpam Handler: ").append(spamHandlerName).append('\n');

        logBuilder.append("Global IMAP properties successfully loaded!");
        if (LOG.isInfoEnabled()) {
            LOG.info(logBuilder.toString());
        }
    }

    @Override
    protected void resetFields() {
        imapSort = false;
        imapSearch = false;
        fastFetch = true;
        propagateClientIPAddress = false;
        propagateHostNames = Collections.emptySet();
        supportsACLs = null;
        imapTimeout = 0;
        imapConnectionTimeout = 0;
        imapConnectionIdleTime = 0;
        imapTemporaryDown = 0;
        imapAuthEnc = null;
        entity2AclImpl = null;
        blockSize = 0;
        spamHandlerName = null;
        notifyRecent = false;
        notifyFrequencySeconds = 300;
        notifyFullNames = "INBOX";
    }

    public boolean isFastFetch() {
        return fastFetch;
    }

    public boolean notifyRecent() {
        return notifyRecent;
    }

    public int getNotifyFrequencySeconds() {
        return notifyFrequencySeconds;
    }

    public String getNotifyFullNames() {
        return notifyFullNames;
    }

    public boolean isPropagateClientIPAddress() {
        return propagateClientIPAddress;
    }

    public Set<String> getPropagateHostNames() {
        return propagateHostNames;
    }

    public String getImapAuthEnc() {
        return imapAuthEnc;
    }

    public int getImapConnectionIdleTime() {
        return imapConnectionIdleTime;
    }

    public int getImapConnectionTimeout() {
        return imapConnectionTimeout;
    }

    public int getImapTemporaryDown() {
        return imapTemporaryDown;
    }

    public boolean isImapSearch() {
        return imapSearch;
    }

    public boolean isImapSort() {
        return imapSort;
    }

    public int getImapTimeout() {
        return imapTimeout;
    }

    public BoolCapVal getSupportsACLs() {
        return supportsACLs;
    }

    /**
     * Gets the {@link Entity2ACL}.
     * 
     * @return The {@link Entity2ACL}
     */
    public String getEntity2AclImpl() {
        return entity2AclImpl;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public Map<String, Boolean> getNewACLExtMap() {
        return newACLExtMap;
    }

    /**
     * Gets the spam handler name.
     * 
     * @return The spam handler name
     */
    public String getSpamHandlerName() {
        return spamHandlerName;
    }

    public int getAttachDisplaySize() {
        return mailProperties.getAttachDisplaySize();
    }

    public char getDefaultSeparator() {
        return mailProperties.getDefaultSeparator();
    }

    public int getMailAccessCacheIdleSeconds() {
        return mailProperties.getMailAccessCacheIdleSeconds();
    }

    public int getMailAccessCacheShrinkerSeconds() {
        return mailProperties.getMailAccessCacheShrinkerSeconds();
    }

    public int getMailFetchLimit() {
        return mailProperties.getMailFetchLimit();
    }

    public int getWatcherFrequency() {
        return mailProperties.getWatcherFrequency();
    }

    public int getWatcherTime() {
        return mailProperties.getWatcherTime();
    }

    public boolean isAllowNestedDefaultFolderOnAltNamespace() {
        return mailProperties.isAllowNestedDefaultFolderOnAltNamespace();
    }

    public boolean isIgnoreSubscription() {
        return mailProperties.isIgnoreSubscription();
    }

    public boolean isSupportSubscription() {
        return mailProperties.isSupportSubscription();
    }

    public boolean isUserFlagsEnabled() {
        return mailProperties.isUserFlagsEnabled();
    }

    public boolean isWatcherEnabled() {
        return mailProperties.isWatcherEnabled();
    }

    public boolean isWatcherShallClose() {
        return mailProperties.isWatcherShallClose();
    }

}
