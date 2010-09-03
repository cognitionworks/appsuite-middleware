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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.messaging.twitter;

import static com.openexchange.messaging.twitter.FormStrings.FORM_LABEL_LOGIN;
import static com.openexchange.messaging.twitter.FormStrings.FORM_LABEL_PASSWORD;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.openexchange.datatypes.genericonf.DynamicFormDescription;
import com.openexchange.datatypes.genericonf.FormElement;
import com.openexchange.messaging.MessagingAccountAccess;
import com.openexchange.messaging.MessagingAccountManager;
import com.openexchange.messaging.MessagingAccountTransport;
import com.openexchange.messaging.MessagingAction;
import com.openexchange.messaging.MessagingException;
import com.openexchange.messaging.MessagingService;
import com.openexchange.messaging.generic.DefaultMessagingAccountManager;
import com.openexchange.messaging.generic.ReadOnlyDynamicFormDescription;
import com.openexchange.session.Session;

/**
 * {@link TwitterMessagingService}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class TwitterMessagingService implements MessagingService {

    private static final List<MessagingAction> ACTIONS = Collections.unmodifiableList(Arrays.asList(
        new MessagingAction(TwitterConstants.TYPE_RETWEET, MessagingAction.Type.STORAGE, TwitterConstants.TYPE_TWEET),
        new MessagingAction(TwitterConstants.TYPE_RETWEET_NEW, MessagingAction.Type.STORAGE),
        new MessagingAction(TwitterConstants.TYPE_DIRECT_MESSAGE, MessagingAction.Type.STORAGE, TwitterConstants.TYPE_TWEET),
        new MessagingAction(TwitterConstants.TYPE_TWEET, MessagingAction.Type.MESSAGE)));

    private static final String ID = "com.openexchange.messaging.twitter";

    private static final String DISPLAY_NAME = "Twitter";

    /**
     * Gets the service identifier for twitter messaging service.
     * 
     * @return The service identifier
     */
    public static String getServiceId() {
        return ID;
    }

    /*-
     * -------------------------------------- Member section --------------------------------------
     */

    private final MessagingAccountManager accountManager;

    private final DynamicFormDescription formDescription;

    private final Set<String> secretProperties;

    /**
     * Initializes a new {@link TwitterMessagingService}.
     */
    public TwitterMessagingService() {
        super();
        accountManager = new TwitterMessagingAccountManager(new DefaultMessagingAccountManager(ID));
        final DynamicFormDescription tmpDescription = new DynamicFormDescription();
        tmpDescription.add(FormElement.input(TwitterConstants.TWITTER_LOGIN, FORM_LABEL_LOGIN, true, ""));
        tmpDescription.add(FormElement.password(TwitterConstants.TWITTER_PASSWORD, FORM_LABEL_PASSWORD, true, ""));
        this.formDescription = new ReadOnlyDynamicFormDescription(tmpDescription);
        secretProperties =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                TwitterConstants.TWITTER_PASSWORD,
                TwitterConstants.TWITTER_TOKEN,
                TwitterConstants.TWITTER_TOKEN_SECRET)));
    }

    public Set<String> getSecretProperties() {
        return secretProperties;
    }

    public MessagingAccountAccess getAccountAccess(final int accountId, final Session session) throws MessagingException {
        return new TwitterMessagingAccountAccess(accountManager.getAccount(accountId, session), session);
    }

    public MessagingAccountManager getAccountManager() {
        return accountManager;
    }

    public MessagingAccountTransport getAccountTransport(final int accountId, final Session session) throws MessagingException {
        return new TwitterMessagingAccountTransport(accountManager.getAccount(accountId, session), session);
    }

    public List<MessagingAction> getMessageActions() {
        return ACTIONS;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public String getId() {
        return ID;
    }

    public DynamicFormDescription getFormDescription() {
        return formDescription;
    }
}
