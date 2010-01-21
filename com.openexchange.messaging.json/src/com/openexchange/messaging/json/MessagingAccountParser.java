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

package com.openexchange.messaging.json;

import static com.openexchange.messaging.json.MessagingAccountConstants.ID;
import static com.openexchange.messaging.json.MessagingAccountConstants.MESSAGING_SERVICE;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.datatypes.genericonf.json.FormContentParser;
import com.openexchange.messaging.MessagingAccount;
import com.openexchange.messaging.MessagingException;
import com.openexchange.messaging.MessagingService;
import com.openexchange.messaging.generic.DefaultMessagingAccount;
import com.openexchange.messaging.registry.MessagingServiceRegistry;

/**
 * {@link MessagingAccountParser}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Firstname Lastname</a>
 */
public class MessagingAccountParser {

    private final MessagingServiceRegistry registry;

    public MessagingAccountParser(final MessagingServiceRegistry serviceRegistry) {
        this.registry = serviceRegistry;
    }

    public MessagingAccount parse(final JSONObject accountJSON) throws MessagingException, JSONException {
        DefaultMessagingAccount account = new DefaultMessagingAccount();
        
        account.setId(accountJSON.optInt(ID));
        if(accountJSON.has("displayName")) {
            account.setDisplayName(accountJSON.optString("displayName"));
        }
        MessagingService messagingService = registry.getMessagingService(accountJSON.getString(MESSAGING_SERVICE));
        account.setMessagingService(messagingService);
        if(accountJSON.has("configuration")) {
            account.setConfiguration( new FormContentParser().parse(
                accountJSON.getJSONObject("configuration"),
                messagingService.getFormDescription()));
        }
        
        return account;
    }
}
