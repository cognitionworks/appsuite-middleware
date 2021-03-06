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

package com.openexchange.share.impl;

import com.openexchange.exception.OXException;
import com.openexchange.groupware.notify.hostname.HostData;
import com.openexchange.server.ServiceLookup;
import com.openexchange.share.GuestInfo;
import com.openexchange.share.ShareTarget;
import com.openexchange.share.ShareTargetPath;
import com.openexchange.share.core.tools.ShareLinks;
import com.openexchange.share.core.tools.ShareTool;
import com.openexchange.user.User;

/**
 * {@link DefaultShareInfo}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.0
 */
public class DefaultShareInfo extends AbstractShareInfo {

    private final DefaultGuestInfo guestInfo;
    private final ShareTargetPath targetPath;

    /**
     * Creates a list of extended share info objects for the supplied shares.
     *
     * @param services A service lookup reference
     * @param contextID The context ID
     * @param guestUser The guest user
     * @param srcTarget The share target from the sharing users point of view
     * @param dstTarget The share target from the recipients point of view
     * @param targetPath The target path
     * @param includeSubfolders Whether sub-folders should be included in case the target is a infostore folder
     */
    public DefaultShareInfo(ServiceLookup services, int contextID, User guestUser, ShareTarget srcTarget, ShareTarget dstTarget, ShareTargetPath targetPath, boolean includeSubfolders) throws OXException {
        super(srcTarget, dstTarget, includeSubfolders);
        if (ShareTool.isAnonymousGuest(guestUser)) {
            this.guestInfo = new DefaultGuestInfo(services, contextID, guestUser, srcTarget);
        } else {
            this.guestInfo = new DefaultGuestInfo(services, contextID, guestUser, null);
        }
        this.targetPath = targetPath;
    }

    @Override
    public GuestInfo getGuest() {
        return guestInfo;
    }

    @Override
    public String getShareURL(HostData hostData) {
        return ShareLinks.generateExternal(hostData, guestInfo.getBaseToken(), targetPath);
    }

}
