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

package com.openexchange.mail.smal.impl;

import static com.openexchange.mail.utils.ProviderUtility.extractProtocol;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.openexchange.exception.OXException;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailSessionCache;
import com.openexchange.mail.Protocol;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.api.MailProvider;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.session.Session;

/**
 * {@link SmalMailProviderRegistry} - The {@link MailProvider} registry for SMAL bundle.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SmalMailProviderRegistry {

    /**
     * The parameter name for SMAL provider.
     */
    private static final String SMAL_PROVIDER = "smal.provider";

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(SmalMailProviderRegistry.class);

    /**
     * Concurrent map used as set for mail providers
     */
    private static final ConcurrentMap<Protocol, MailProvider> PROVIDERS = new ConcurrentHashMap<Protocol, MailProvider>();

    /**
     * Initializes a new {@link SmalMailProviderRegistry}
     */
    private SmalMailProviderRegistry() {
        super();
    }

    /**
     * Gets the mail provider appropriate for specified session.
     *
     * @param session The session
     * @param accountId The account ID
     * @return The appropriate mail provider
     * @throws OXException If no supporting mail provider can be found
     */
    public static MailProvider getMailProviderBySession(final Session session, final int accountId) throws OXException {
        final MailSessionCache mailSessionCache = MailSessionCache.getInstance(session);
        MailProvider provider;
        try {
            provider = mailSessionCache.getParameter(accountId, SMAL_PROVIDER);
        } catch (final ClassCastException e) {
            /*
             * Probably caused by bundle update(s)
             */
            provider = null;
        }
        final String mailServerURL = MailConfig.getMailServerURL(session, accountId);
        final String protocol;
        if (mailServerURL == null) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Missing mail server URL. Mail server URL not set in account {} for user {} in context {}. Using fallback protocol {}", accountId, session.getUserId(), session.getContextId(), MailProperties.getInstance().getDefaultMailProvider());
            }
            protocol = MailProperties.getInstance().getDefaultMailProvider();
        } else {
            protocol = extractProtocol(mailServerURL, MailProperties.getInstance().getDefaultMailProvider());
        }
        if ((null != provider) && !provider.isDeprecated() && provider.supportsProtocol(protocol)) {
            return provider;
        }
        provider = getMailProvider(protocol);
        if (null == provider || !provider.supportsProtocol(protocol)) {
            throw MailExceptionCode.UNKNOWN_PROTOCOL.create(mailServerURL);
        }
        mailSessionCache.putParameter(accountId, SMAL_PROVIDER, provider);
        return provider;
    }

    /**
     * Gets the mail provider appropriate for specified mail server URL.
     * <p>
     * The given URL should match pattern
     *
     * <pre>
     * &lt;protocol&gt;://&lt;host&gt;(:&lt;port&gt;)?
     * </pre>
     *
     * The protocol should be present. Otherwise the configured fallback is used as protocol.
     *
     * @param serverUrl The mail server URL
     * @return The appropriate mail provider
     */
    public static MailProvider getMailProviderByURL(final String serverUrl) {
        /*
         * Get appropriate provider
         */
        return getMailProvider(extractProtocol(serverUrl, MailProperties.getInstance().getDefaultMailProvider()));
    }

    /**
     * Gets the mail provider appropriate for specified protocol.
     *
     * @param protocolName The mail protocol; e.g. <code>"imap"</code>
     * @return The appropriate mail provider
     */
    public static MailProvider getMailProvider(final String protocolName) {
        if (null == protocolName) {
            return null;
        }
        /*
         * Return real provider
         */
        return getRealMailProvider(protocolName);
    }

    private static MailProvider getRealMailProvider(final String protocolName) {
        /*
         * Look-up
         */
        for (final Iterator<Map.Entry<Protocol, MailProvider>> iter = PROVIDERS.entrySet().iterator(); iter.hasNext();) {
            final Map.Entry<Protocol, MailProvider> entry = iter.next();
            if (entry.getKey().isSupported(protocolName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Registers a mail provider and performs its start-up actions
     *
     * @param protocol The mail protocol's string representation; e.g. <code>"imap_imaps"</code>
     * @param provider The mail provider to register
     * @return <code>true</code> if mail provider has been successfully registered and no other mail provider supports the same protocol;
     *         otherwise <code>false</code>
     * @throws OXException If provider's start-up fails
     */
    public static boolean registerMailProvider(final String protocol, final MailProvider provider) throws OXException {
        try {
            final Protocol p = Protocol.parseProtocol(protocol);
            if (Protocol.PROTOCOL_ALL.equals(p)) {
                /*
                 * All provider
                 */
                return false;
            }
            /*
             * Non-all provider
             */
            if (null != PROVIDERS.putIfAbsent(p, provider)) {
                return false;
            }
            /*
             * No startup
             */
            return true;
        } catch (final OXException e) {
            throw e;
        } catch (final RuntimeException t) {
            LOG.error("", t);
            return false;
        }
    }

    /**
     * Unregisters all mail providers
     */
    public static void unregisterAll() {
        /*
         * Clear registry
         */
        PROVIDERS.clear();
    }

    /**
     * Unregisters the mail provider
     *
     * @param provider The mail provider to unregister
     * @return The unregistered mail provider, or <code>null</code>
     */
    public static MailProvider unregisterMailProvider(final MailProvider provider) {
        final Protocol protocol = provider.getProtocol();
        if (Protocol.PROTOCOL_ALL.equals(protocol)) {
            return null;
        }
        /*
         * Unregister
         */
        return PROVIDERS.remove(protocol);
    }

    /**
     * Unregisters the mail provider supporting specified protocol
     *
     * @param protocol The protocol
     * @return The unregistered instance of {@link MailProvider}, or <code>null</code> if there was no provider supporting specified
     *         protocol
     */
    public static MailProvider unregisterMailProviderByProtocol(final String protocol) {
        if (Protocol.ALL.equals(protocol)) {
            return null;
        }
        /*
         * Non-all
         */
        for (final Iterator<Map.Entry<Protocol, MailProvider>> iter = PROVIDERS.entrySet().iterator(); iter.hasNext();) {
            final Map.Entry<Protocol, MailProvider> entry = iter.next();
            if (entry.getKey().isSupported(protocol)) {
                iter.remove();
                return entry.getValue();
            }
        }
        return null;
    }

}
