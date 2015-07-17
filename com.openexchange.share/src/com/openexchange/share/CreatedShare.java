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
 *     Copyright (C) 2004-2015 Open-Xchange, Inc.
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

package com.openexchange.share;

import com.openexchange.groupware.notify.hostname.HostData;
import com.openexchange.share.recipient.ShareRecipient;

/**
 * {@link CreatedShare}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public interface CreatedShare {

    /**
     * Gets the number of share targets.
     *
     * @return The number
     */
    int size();

    /**
     * Gets the guest information of the according recipient.
     *
     * @return The guest info
     */
    GuestInfo getGuestInfo();

    /**
     * Gets the {@link ShareRecipient}.
     *
     * @return The recipient
     */
    ShareRecipient getShareRecipient();

    /**
     * Gets whether this share has a single or multiple targets.
     *
     * @return <code>true</code> a single target is contained. The
     * result is equivalent to <code>createdShare.size() == 1</code>.
     */
    boolean hasSingleTarget();

    /**
     * Gets the first of all share targets. If this is a single-target
     * share, that target is returned.
     *
     * @return The first target.
     */
    ShareTarget getFirstTarget();

    /**
     * Gets an iterable of all contained targets.
     *
     * @return The iterable
     */
    Iterable<ShareTarget> getTargets();

    /**
     * Gets the share info of the first contained share. If this is a single-target
     * share, the single share info instance is returned.
     *
     * @return The first share info
     */
    ShareInfo getFirstInfo();

    /**
     * Gets an iterable of all contained share infos.
     *
     * @return The iterable
     */
    Iterable<ShareInfo> getInfos();

    /**
     * Gets the token for this share. If the only a single target is contained, the
     * absolute token addressing this target is returned. Otherwise only the guest
     * users base token is returned. If the share recipient is an internal entity
     * (i.e. a user or group), <code>null</code> is returned.
     *
     * @return The token
     */
    String getToken();

    /**
     * Gets the URL to this share. If the recipient is a guest and this share has a single target,
     * the URL points to that target using the absolute share token. If the recipient is a guest
     * and this share has multiple targets, the URL is constructed with the guest users base token.
     * If the recipient is an internal user or group, the URL points to the first target, ignoring
     * whether this share has multiple targets or not. The latter behavior is subject to change in
     * the future.
     *
     * @param hostData The host data
     * @return The URL
     */
    String getUrl(HostData hostData);

    /**
     * Gets whether this share is internal, meaning that the recipient is either a user or a group.
     *
     * @return <code>true</code> if the share is internal
     */
    boolean isInternal();

}
