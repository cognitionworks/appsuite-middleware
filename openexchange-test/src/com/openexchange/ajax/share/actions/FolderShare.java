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

package com.openexchange.ajax.share.actions;

import java.util.List;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.fields.FolderFields;
import com.openexchange.ajax.folder.actions.Parser;
import com.openexchange.ajax.parser.FolderParser;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.FolderObject;

/**
 * {@link FolderShare}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class FolderShare extends FolderObject {

    /**
     * Parses a {@link FolderShare}.
     *
     * @param jsonFolder The json array from a <code>shares</code> response
     * @param columns The requested columns
     * @param timeZone The client timezone
     */
    public static FolderShare parse(JSONArray jsonFolder, int[] columns, TimeZone timeZone) throws JSONException, OXException {
        FolderShare folderShare = new FolderShare();
        for (int i = 0; i < columns.length; i++) {
            switch (columns[i]) {
                case 3060:
                    folderShare.extendedFolderPermissions = ExtendedPermissionEntity.parse(jsonFolder.optJSONArray(i), timeZone);
                    break;
                default:
                    Parser.parse(jsonFolder.get(i), columns[i], folderShare);
                    break;
            }
        }
        return folderShare;
    }

    /**
     * Parses a {@link FolderShare}.
     *
     * @param jsonObject The json object from a <code>folder/get</code> response
     * @param timeZone The client timezone
     */
    public static FolderShare parse(JSONObject jsonObject, TimeZone timeZone) throws JSONException, OXException {
        FolderShare folderShare = new FolderShare();
        if (jsonObject.has(FolderFields.FOLDER_ID)) {
            String tmp = jsonObject.getString(FolderFields.FOLDER_ID);
            if (tmp.startsWith(FolderObject.SHARED_PREFIX)) {
                jsonObject.put(FolderFields.FOLDER_ID, Integer.toString(FolderObject.SYSTEM_SHARED_FOLDER_ID));
            }
        }
        new FolderParser().parse(folderShare, jsonObject);
        folderShare.extendedFolderPermissions = ExtendedPermissionEntity.parse(jsonObject.optJSONArray("com.openexchange.share.extendedPermissions"), timeZone);
        return folderShare;
    }

    private static final long serialVersionUID = 4389215025150629747L;

    private List<ExtendedPermissionEntity> extendedFolderPermissions;

    /**
     * Initializes a new {@link FolderShare}.
     */
    public FolderShare() {
        super();
    }

    /**
     * Gets the extended permission entities.
     *
     * @return The extended permissions
     */
    public List<ExtendedPermissionEntity> getExtendedPermissions() {
        return extendedFolderPermissions;
    }

}
