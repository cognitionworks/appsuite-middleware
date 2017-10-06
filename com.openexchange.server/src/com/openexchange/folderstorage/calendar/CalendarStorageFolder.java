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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.folderstorage.calendar;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.openexchange.folderstorage.AbstractFolder;
import com.openexchange.folderstorage.CalendarFolderField;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.FolderField;
import com.openexchange.folderstorage.FolderProperty;
import com.openexchange.folderstorage.ParameterizedFolder;

/**
 * {@link CalendarStorageFolder}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class CalendarStorageFolder extends AbstractFolder implements ParameterizedFolder {

    private static final long serialVersionUID = 4412370864213762652L;

    private final Map<FolderField, FolderProperty> properties;

    /**
     * Initializes a new calendar folder as used by the internal folder storage.
     *
     * @param treeId The identifier of the folder tree to take over
     * @param accountId The fully-qualified account identifier to take over
     * @param contentType The content type to take over
     */
    public CalendarStorageFolder(String treeId, String accountId, ContentType contentType) {
        super();
        this.properties = new HashMap<FolderField, FolderProperty>();
        setTreeID(treeId);
        setAccountID(accountId);
        setSubscribed(true);
        setContentType(contentType);
        setDefaultType(contentType.getModule());

        properties.put(CalendarFolderField.COLOR, new FolderProperty("color", "cyan"));
        properties.put(CalendarFolderField.USED_FOR_SYNC, new FolderProperty("synced", Boolean.FALSE));

    }

    @Override
    public boolean isGlobalID() {
        return false;
    }

    @Override
    public void setProperty(FolderField name, Object value) {
        if (null == value) {
            properties.remove(name);
        } else {
            properties.put(name, new FolderProperty(name.getName(), value));
        }
    }

    @Override
    public Map<FolderField, FolderProperty> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public String toString() {
        return "CalendarStorageFolder [account=" + accountId + ", id=" + id + ", name=" + name + "]";
    }

}
