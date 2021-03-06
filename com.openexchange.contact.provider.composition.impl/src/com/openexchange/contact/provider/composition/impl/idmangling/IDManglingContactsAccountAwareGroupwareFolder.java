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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.contact.provider.composition.impl.idmangling;

import java.util.Date;
import java.util.Map;
import com.openexchange.contact.common.ContactsAccount;
import com.openexchange.contact.common.GroupwareContactsFolder;
import com.openexchange.contact.common.GroupwareFolderType;

/**
 * {@link IDManglingContactsAccountAwareGroupwareFolder}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
public class IDManglingContactsAccountAwareGroupwareFolder extends IDManglingContactsAccountAwareFolder implements GroupwareContactsFolder {

    private final String newParentId;
    private final GroupwareContactsFolder delegate;

    /**
     * Initializes a new {@link IDManglingContactsAccountAwareGroupwareFolder}.
     *
     * @param delegate The contacts folder delegate
     * @param account The underlying contacts account
     * @param newId The new identifier to hide the delegate's one
     * @param newParentId The new parent identifier to hide the delegate's one
     */
    public IDManglingContactsAccountAwareGroupwareFolder(GroupwareContactsFolder delegate, ContactsAccount account, String newId, String newParentId) {
        super(delegate, account, newId);
        this.delegate = delegate;
        this.newParentId = newParentId;
    }

    @Override
    public String getParentId() {
        return newParentId;
    }

    @Override
    public boolean isDefaultFolder() {
        return delegate.isDefaultFolder();
    }

    @Override
    public int getModifiedBy() {
        return delegate.getModifiedBy();
    }

    @Override
    public int getCreatedBy() {
        return delegate.getCreatedBy();
    }

    @Override
    public Date getCreationDate() {
        return delegate.getCreationDate();
    }

    @Override
    public GroupwareFolderType getType() {
        return delegate.getType();
    }

    @Override
    public Map<String, Object> getMeta() {
        return delegate.getMeta();
    }

    @Override
    public String toString() {
        return "IDManglingContactsAccountAwareGroupwareFolder [newId=" + newId + ", newParentId=" + newParentId + ", delegate=" + delegate + "]";
    }
}
