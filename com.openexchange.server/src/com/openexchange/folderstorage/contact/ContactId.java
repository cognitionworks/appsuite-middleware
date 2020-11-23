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

package com.openexchange.folderstorage.contact;

import com.openexchange.folderstorage.SortableId;

/**
 * {@link ContactId}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
public class ContactId implements SortableId {

    private final String folderId;
    private final int ordinal;
    private final String name;
    private final int hashCode;

    /**
     * Initializes a new {@link ContactId}.
     *
     * @param folderId The folder identifier
     * @param ordinal The ordinal
     * @param name The name
     */
    public ContactId(final String folderId, final int ordinal, final String name) {
        super();
        this.folderId = folderId;
        this.ordinal = ordinal;
        this.name = name;

        final int prime = 31;
        int result = 1;
        result = prime * result + ordinal;
        hashCode = result;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return folderId;
    }

    @Override
    public Priority getPriority() {
        return Priority.NORMAL;
    }

    @Override
    public int compareTo(final SortableId o) {
        if (o instanceof ContactId) {
            final int thisVal = ordinal;
            final int anotherVal = ((ContactId) o).ordinal;
            return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
        }
        final int thisPrio = getPriority().ordinal();
        final int anotherPrio = (o).getPriority().ordinal();
        return (thisPrio < anotherPrio ? 1 : (thisPrio == anotherPrio ? 0 : -1));
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ContactId)) {
            return false;
        }
        final ContactId other = (ContactId) obj;
        if (ordinal != other.ordinal) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder(32).append("{folderId=").append(folderId).append(", ordinal=").append(ordinal).append('}').toString();
    }
}
