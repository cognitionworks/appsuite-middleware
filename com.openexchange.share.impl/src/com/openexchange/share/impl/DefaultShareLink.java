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

import java.util.Date;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.notify.hostname.HostData;
import com.openexchange.share.GuestInfo;
import com.openexchange.share.ShareInfo;
import com.openexchange.share.ShareLink;
import com.openexchange.share.ShareTarget;
import com.openexchange.share.SubfolderAwareShareInfo;

/**
 * {@link DefaultShareLink}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.0
 */
public class DefaultShareLink implements ShareLink {

    private final ShareInfo delegate;
    private final Date timestamp;
    private final boolean isNew;

    /**
     * Initializes a new {@link DefaultShareLink}.
     *
     * @param delegate The underlying share link
     * @param timestamp The target timestamp
     * @param isNew <code>true</code> if the link was just created, <code>false</code>, otherwise
     */
    public DefaultShareLink(ShareInfo delegate, Date timestamp, boolean isNew) {
        super();
        this.delegate = delegate;
        this.timestamp = timestamp;
        this.isNew = isNew;
    }

    @Override
    public ShareTarget getTarget() {
        return delegate.getTarget();
    }

    @Override
    public ShareTarget getDestinationTarget() {
        return delegate.getDestinationTarget();
    }

    @Override
    public GuestInfo getGuest() {
        return delegate.getGuest();
    }

    @Override
    public String getShareURL(HostData hostData) throws OXException {
        return delegate.getShareURL(hostData);
    }

    @Override
    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public boolean isIncludeSubfolders() {
        return delegate instanceof SubfolderAwareShareInfo ? ((SubfolderAwareShareInfo) delegate).isIncludeSubfolders() : false;
    }

}
