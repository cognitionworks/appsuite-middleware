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

package com.openexchange.share.json.fields;

import static com.openexchange.java.Autoboxing.I;
import static org.slf4j.LoggerFactory.getLogger;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.AbstractFolder;
import com.openexchange.folderstorage.Folder;
import com.openexchange.folderstorage.FolderPermissionType;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.Permissions;
import com.openexchange.share.GuestInfo;
import com.openexchange.share.ShareExceptionCodes;
import com.openexchange.share.core.tools.PermissionResolver;
import com.openexchange.share.recipient.RecipientType;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;

/**
 * {@link ExtendedFolderPermission}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ExtendedFolderPermission extends ExtendedPermission {

    private final Folder folder;
    private final Permission permission;

    /**
     * Initializes a new {@link ExtendedFolderPermission}.
     *
     * @param permissionResolver The permission resolver
     * @param folder The folder
     * @param parentPermission The underlying permissions
     */
    public ExtendedFolderPermission(PermissionResolver permissionResolver, Folder folder, Permission parentPermission) {
        super(permissionResolver);
        this.permission = parentPermission;
        this.folder = folder;
    }

    /**
     * Serializes the extended permissions as JSON.
     *
     * @param requestData The underlying request data, or <code>null</code> if not available
     * @return The serialized extended permissions
     */
    public JSONObject toJSON(AJAXRequestData requestData) throws JSONException, OXException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("identifier", null != permission.getIdentifier() ? permission.getIdentifier() : String.valueOf(permission.getEntity()));
        if (0 < permission.getEntity() || 0 == permission.getEntity() && permission.isGroup()) {
            jsonObject.put("entity", permission.getEntity());
        }
        jsonObject.put("bits", createPermissionBits(permission));
        if (permission.isGroup()) {
            jsonObject.put("type", "group");
            if (null != permission.getEntityInfo()) {
                addEntityInfo(requestData, jsonObject, permission.getEntityInfo());
            } else {
                addGroupInfo(requestData, jsonObject, resolver.getGroup(permission.getEntity()));
            }
        } else {
            if (null != permission.getEntityInfo()) {
                /*
                 * add extended information based on provided entity info object
                 */
                addEntityInfo(requestData, jsonObject, permission.getEntityInfo());
            } else if (0 >= permission.getEntity()) {
                getLogger(ExtendedFolderPermission.class).debug("Can't resolve user permission entity {} for folder {}", I(permission.getEntity()), folder);
            } else {
                /*
                 * lookup and add extended information for internal user/guest
                 */
                User user = resolver.getUser(permission.getEntity());
                if (null == user) {
                    getLogger(ExtendedFolderPermission.class).debug("Can't resolve user permission entity {} for folder {}", I(permission.getEntity()), folder);
                } else if (user.isGuest()) {
                    GuestInfo guest = resolver.getGuest(user.getId());
                    if (guest == null) {
                        ServerSession session = requestData.getSession();
                        int contextId = session == null ? -1 : session.getContextId();
                        throw ShareExceptionCodes.UNEXPECTED_ERROR.create("Could not resolve guest info for ID " + user.getId() + " in context " + contextId + ". " + "It might have been deleted in the mean time or is in an inconsistent state.");
                    }

                    jsonObject.put("type", guest.getRecipientType().toString().toLowerCase());
                    if (RecipientType.ANONYMOUS.equals(guest.getRecipientType())) {
                        if (permission.getType() == FolderPermissionType.INHERITED) {
                            Folder legator = new AbstractFolder() {

                                private static final long serialVersionUID = 1L;

                                @Override
                                public boolean isGlobalID() {
                                    return false;
                                }
                            };
                            legator.setContentType(folder.getContentType());
                            legator.setID(permission.getPermissionLegator());
                            addShareInfo(requestData, jsonObject, resolver.getShare(legator, permission.getEntity()));
                        } else {
                            addShareInfo(requestData, jsonObject, resolver.getShare(folder, permission.getEntity()));
                        }
                    } else {
                        addUserInfo(requestData, jsonObject, user);
                    }
                } else {
                    jsonObject.put("type", "user");
                    addUserInfo(requestData, jsonObject, user);
                }
            }
        }
        if (permission.getType() == FolderPermissionType.INHERITED) {
            jsonObject.put("isInherited", true);
            jsonObject.put("isInheritedFrom", permission.getPermissionLegator());
        }
        return jsonObject;
    }

    private static int createPermissionBits(Permission permission) {
        return Permissions.createPermissionBits(permission.getFolderPermission(), permission.getReadPermission(), permission.getWritePermission(), permission.getDeletePermission(), permission.isAdmin());
    }

}
