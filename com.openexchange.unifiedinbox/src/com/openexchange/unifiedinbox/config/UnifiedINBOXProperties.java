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

package com.openexchange.unifiedinbox.config;

import com.openexchange.mail.api.AbstractProtocolProperties;
import com.openexchange.mail.api.IMailProperties;
import com.openexchange.mail.config.MailConfigException;
import com.openexchange.mail.config.MailProperties;

/**
 * {@link UnifiedINBOXProperties}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class UnifiedINBOXProperties extends AbstractProtocolProperties implements IUnifiedINBOXProperties {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(UnifiedINBOXProperties.class));

    private static final UnifiedINBOXProperties instance = new UnifiedINBOXProperties();

    /**
     * Gets the singleton instance of {@link UnifiedINBOXProperties}
     * 
     * @return The singleton instance of {@link UnifiedINBOXProperties}
     */
    public static UnifiedINBOXProperties getInstance() {
        return instance;
    }

    private final IMailProperties mailProperties;

    /**
     * Initializes a new {@link UnifiedINBOXProperties}
     */
    private UnifiedINBOXProperties() {
        super();
        mailProperties = MailProperties.getInstance();
    }

    @Override
    protected void loadProperties0() throws MailConfigException {
        final StringBuilder logBuilder = new StringBuilder(1024);
        logBuilder.append("\nLoading global Unified INBOX properties...\n");
        logBuilder.append("Global Unified INBOX properties successfully loaded!");
        if (LOG.isInfoEnabled()) {
            LOG.info(logBuilder.toString());
        }
    }

    @Override
    protected void resetFields() {
        // Nothing to do
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
