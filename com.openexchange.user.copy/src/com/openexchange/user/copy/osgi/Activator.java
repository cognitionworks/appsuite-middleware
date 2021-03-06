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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.user.copy.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import com.openexchange.osgi.CompositeBundleActivator;
import com.openexchange.user.copy.internal.additional.osgi.AdditionalCopyActivator;
import com.openexchange.user.copy.internal.attachment.osgi.AttachmentCopyActivator;
import com.openexchange.user.copy.internal.chronos.osgi.ChronosCopyActivator;
import com.openexchange.user.copy.internal.connection.osgi.ConnectionFetcherActivator;
import com.openexchange.user.copy.internal.contact.osgi.ContactCopyActivator;
import com.openexchange.user.copy.internal.context.osgi.ContextLoadActivator;
import com.openexchange.user.copy.internal.folder.osgi.FolderCopyActivator;
import com.openexchange.user.copy.internal.infostore.osgi.InfostoreCopyActivator;
import com.openexchange.user.copy.internal.mailaccount.osgi.MailAccountCopyActivator;
import com.openexchange.user.copy.internal.messaging.osgi.MessagingCopyActivator;
import com.openexchange.user.copy.internal.oauth.osgi.OAuthCopyActivator;
import com.openexchange.user.copy.internal.subscription.osgi.SubscriptionCopyActivator;
import com.openexchange.user.copy.internal.tasks.osgi.TaskCopyActivator;
import com.openexchange.user.copy.internal.usecount.osgi.UseCountCopyActivator;
import com.openexchange.user.copy.internal.user.osgi.UserCopyActivator;
import com.openexchange.user.copy.internal.usersettings.osgi.UserSettingsActivator;

public class Activator extends CompositeBundleActivator {

    public Activator() {
	    super();
	}

    @Override
    protected BundleActivator[] getActivators() {
        return new BundleActivator[] {
            new UserCopyServiceActivator(),
            new ContextLoadActivator(),
            new UserSettingsActivator(),
            new UserCopyActivator(),
            new FolderCopyActivator(),
            new ConnectionFetcherActivator(),
            new ChronosCopyActivator(),
            new ContactCopyActivator(),
            new AttachmentCopyActivator(),
            new TaskCopyActivator(),
            new SubscriptionCopyActivator(),
            new MessagingCopyActivator(),
            new OAuthCopyActivator(),
            new InfostoreCopyActivator(),
            new AdditionalCopyActivator(),
            new CommandActivator(),
            new MailAccountCopyActivator(),
            new UseCountCopyActivator()
        };
    }

    @Override
    public void start(final BundleContext context) throws Exception {
        final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Activator.class);
        super.start(context);
        log.info("Bundle started: com.openexchange.user.copy");
    }
}
