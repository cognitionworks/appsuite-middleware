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

package com.openexchange.share.impl.groupware;

import java.util.List;
import java.util.Map;
import com.openexchange.share.ShareTarget;
import com.openexchange.share.groupware.TargetPermission;
import com.openexchange.share.groupware.TargetProxy;
import com.openexchange.share.groupware.TargetProxyType;
import com.openexchange.share.groupware.VirtualTargetProxyType;


/**
 * {@link VirtualTargetProxy} - A {@link TargetProxy} for non groupware modules aka. third party plugins like e.g. messenger. This
 * {@link TargetProxy} only contains the minimum set of infos.  
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class VirtualTargetProxy extends AbstractTargetProxy {

    private final String folderId;
    private final String item;
    private final String title;
    private final int ownedBy;

    public VirtualTargetProxy(int ownedBy, String folderId, String item, String title) {
        super();
        this.ownedBy = ownedBy;
        this.folderId = folderId;
        this.item = item;
        this.title = title;
    }

    /**
     * Initializes a new {@link VirtualTargetProxy}.
     *
     * @param target The target
     */
    public VirtualTargetProxy(ShareTarget target) {
        this(target.getOwnedBy(), target);
    }

    /**
     * Initializes a new {@link VirtualTargetProxy}.
     *
     * @param ownedBy The identifier of the user that is considered as owner of the target
     * @param target The target
     */
    public VirtualTargetProxy(int ownedBy, ShareTarget target) {
        this(ownedBy, target.getFolder(), target.getItem(), getTitle(target));
    }

    @Override
    public String getID() {
        return item;
    }

    @Override
    public String getFolderID() {
        return folderId;
    }

    @Override
    public int getOwner() {
        return ownedBy;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void applyPermissions(List<TargetPermission> permissions) {
        //
    }

    @Override
    public void removePermissions(List<TargetPermission> permissions) {
        //
    }
    
    @Override
    public TargetProxyType getProxyType() {
        return VirtualTargetProxyType.getInstance();
    }

    private static String getTitle(ShareTarget target) {
        Map<String, Object> meta = target.getMeta();
        if (null != meta && meta.containsKey("title")) {
            return String.valueOf(meta.get("title"));
        }
        return target.toString();
    }

}
