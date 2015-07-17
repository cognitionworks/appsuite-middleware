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

package com.openexchange.share.impl;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.I2i;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.notify.hostname.HostData;
import com.openexchange.server.ServiceLookup;
import com.openexchange.share.Share;
import com.openexchange.share.ShareExceptionCodes;
import com.openexchange.share.ShareInfo;
import com.openexchange.share.ShareTarget;
import com.openexchange.share.core.tools.ShareLinks;
import com.openexchange.share.core.tools.ShareTool;
import com.openexchange.user.UserService;

/**
 * {@link DefaultShareInfo}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.0
 */
public class DefaultShareInfo extends ResolvedGuestShare implements ShareInfo {

    /**
     * Creates a list of extended share info objects for the supplied shares. The underlying share targets are used in their original from
     * (i.e. not personalized for the guest user).
     *
     * @param services A service lookup reference
     * @param contextID The context ID
     * @param shares The shares
     * @return The share infos
     */
    public static List<ShareInfo> createShareInfos(ServiceLookup services, int contextID, List<Share> shares) throws OXException {
        return createShareInfos(services, contextID, shares, false);
    }

    /**
     * Creates a list of extended share info objects for the supplied shares.
     *
     * @param services A service lookup reference
     * @param contextID The context ID
     * @param shares The shares
     * @param adjustTargets <code>true</code> to adjust the share targets for the guest user, <code>false</code>, otherwise
     * @return The share infos
     */
    public static List<ShareInfo> createShareInfos(ServiceLookup services, int contextID, List<Share> shares, boolean adjustTargets) throws OXException {
        if (null == shares || 0 == shares.size()) {
            return Collections.emptyList();
        }
        /*
         * retrieve referenced guest users
         */
        Context context = services.getService(ContextService.class).getContext(contextID);
        Set<Integer> guestIDs = ShareTool.getGuestIDs(shares);
        User[] users = services.getService(UserService.class).getUser(context, I2i(guestIDs));
        Map<Integer, User> guestUsers = new HashMap<Integer, User>(users.length);
        for (User user : users) {
            if (false == user.isGuest()) {
                throw ShareExceptionCodes.UNKNOWN_GUEST.create(I(user.getId()));
            }
            guestUsers.put(Integer.valueOf(user.getId()), user);
        }
        /*
         * build & return share infos
         */
        List<ShareInfo> shareInfos = new ArrayList<ShareInfo>(shares.size());
        for (Share share : shares) {
            shareInfos.add(new DefaultShareInfo(services, contextID, guestUsers.get(I(share.getGuest())), share, adjustTargets));
        }
        return shareInfos;
    }

    private final Share share;

    /**
     * Initializes a new {@link DefaultShareInfo}.
     *
     * @param services A service lookup reference
     * @param contextID The context ID
     * @param guestUser The guest user
     * @param share The share
     * @param adjustTargets <code>true</code> to adjust the share targets for the guest user, <code>false</code>, otherwise
     * @throws OXException
     */
    public DefaultShareInfo(ServiceLookup services, int contextID, User guestUser, Share share, boolean adjustTargets) throws OXException {
        super(services, contextID, guestUser, Collections.singletonList(share), adjustTargets);
        this.share = share;
        if (adjustTargets) {
            // take over adjusted target
            share.setTarget(super.getSingleTarget());
        }
    }

    @Override
    public Share getShare() {
        return share;
    }

    @Override
    public String getToken() {
        return super.getToken(share.getTarget());
    }

    @Override
    public String getShareURL(HostData hostData) {
        ShareTarget target = getSingleTarget();
        if (target == null) {
            return ShareLinks.generateExternal(hostData, guestInfo.getBaseToken());
        }

        return ShareLinks.generateExternal(hostData, getToken());
    }

}
