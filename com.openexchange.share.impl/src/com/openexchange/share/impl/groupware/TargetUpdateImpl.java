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

package com.openexchange.share.impl.groupware;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.AbstractFolder;
import com.openexchange.folderstorage.FolderPermissionType;
import com.openexchange.folderstorage.FolderResponse;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.folderstorage.FolderServiceDecorator;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.SetterAwareFolder;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.modules.Module;
import com.openexchange.osgi.Tools;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.share.ShareExceptionCodes;
import com.openexchange.share.ShareTarget;
import com.openexchange.share.core.HandlerParameters;
import com.openexchange.share.core.ModuleHandler;
import com.openexchange.share.core.groupware.FolderTargetProxy;
import com.openexchange.share.groupware.TargetProxy;
import com.openexchange.user.UserService;

/**
 * {@link TargetUpdateImpl}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class TargetUpdateImpl extends AbstractTargetUpdate {

    private final HandlerParameters parameters;

    public TargetUpdateImpl(Session session, Connection writeCon, ServiceLookup services, ModuleExtensionRegistry<ModuleHandler> handlers) throws OXException {
        super(services, handlers);
        parameters = new HandlerParameters();
        parameters.setSession(session);
        Context context = getContextService().getContext(session.getContextId());
        parameters.setContext(context);
        parameters.setUser(getUserService().getUser(writeCon, session.getUserId(), context));
        parameters.setWriteCon(writeCon);
        FolderServiceDecorator folderServiceDecorator = new FolderServiceDecorator();
        folderServiceDecorator.put(Connection.class.getName(), writeCon);
        folderServiceDecorator.put(FolderServiceDecorator.PROPERTY_IGNORE_GUEST_PERMISSIONS, Boolean.TRUE.toString());
        parameters.setFolderServiceDecorator(folderServiceDecorator);
    }

    @Override
    protected HandlerParameters getHandlerParameters() {
        return parameters;
    }

    @Override
    protected Map<ShareTarget, TargetProxy> prepareProxies(List<ShareTarget> folderTargets, Map<Integer, List<ShareTarget>> objectsByModule) throws OXException {
        Map<ShareTarget, TargetProxy> proxies = new HashMap<ShareTarget, TargetProxy>(folderTargets.size() + objectsByModule.size(), 1.0F);
        Map<String, UserizedFolder> foldersById = loadFolderTargets(folderTargets, true, proxies);
        loadObjectTargets(objectsByModule, foldersById, true, proxies);
        return proxies;
    }

    @Override
    protected void updateFolders(List<TargetProxy> proxies) throws OXException {
        FolderService folderService = getFolderService();
        for (TargetProxy proxy : proxies) {
            FolderTargetProxy folderTargetProxy = ((FolderTargetProxy) proxy);
            UserizedFolder folder = folderTargetProxy.getFolder();
            FolderUpdate folderUpdate = new FolderUpdate();
            folderUpdate.setTreeID(folder.getTreeID());
            folderUpdate.setID(folder.getID());
            folderUpdate.setPermissions(folder.getPermissions());
            folderService.updateFolder(folderUpdate, folder.getLastModifiedUTC(), parameters.getSession(), parameters.getFolderServiceDecorator());

            if (folder.getContentType().getModule() == FolderObject.INFOSTORE) {
                // Add permission to sub folders
                FolderResponse<UserizedFolder[]> folderObjects = folderService.getSubfolders(folder.getTreeID(), folder.getID(), true, parameters.getSession(), parameters.getFolderServiceDecorator());

                List<Permission> appliedPermissions = folderTargetProxy.getAppliedPermissions();
                List<Permission> removedPermissions = folderTargetProxy.getRemovedPermissions();

                FolderServiceDecorator folderServiceDecorator;
                try {
                    folderServiceDecorator = parameters.getFolderServiceDecorator().clone();
                } catch (CloneNotSupportedException e) {
                    // should never occur
                    folderServiceDecorator = parameters.getFolderServiceDecorator();
                }
                folderServiceDecorator.put("permissions", "inherit");
                for (UserizedFolder fol : folderObjects.getResponse()) {
                    prepareInheritedPermissions(fol, appliedPermissions, removedPermissions);
                    folderService.updateFolder(fol, fol.getLastModifiedUTC(), parameters.getSession(), folderServiceDecorator);
                }
            }
        }
    }

    private static UserizedFolder prepareInheritedPermissions(UserizedFolder folder, List<Permission> added, List<Permission> removed) {
        Permission[] originalPermissions = folder.getPermissions();
        if (null == originalPermissions) {
            originalPermissions = new Permission[0];
        }

        List<Permission> filtered = new ArrayList<>(added.size());
        for (Permission add : added) {
            if(add.getType() == FolderPermissionType.LEGATOR) {
                Permission clone = (Permission) add.clone();
                clone.setPermissionLegator(folder.getParentID());
                clone.setType(FolderPermissionType.INHERITED);
                filtered.add(clone);
            }
        }

        for (Permission rem : removed) {
            if(rem.getType() == FolderPermissionType.LEGATOR) {
                rem.setPermissionLegator(String.valueOf(folder.getParentID()));
            }
            rem.setType(FolderPermissionType.INHERITED);
        }

        List<Permission> permissions = new ArrayList<>(originalPermissions.length + filtered.size());
        Collections.addAll(permissions, originalPermissions);
        permissions.addAll(filtered);
        permissions = removePermissions(permissions, removed);
        folder.setPermissions(permissions.toArray(new Permission[permissions.size()]));
        return folder;
    }

    protected static List<Permission> removePermissions(List<Permission> origPermissions, List<Permission> toRemove) {
        if (origPermissions == null || origPermissions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Permission> newPermissions = new ArrayList<Permission>(origPermissions);
        Iterator<Permission> it = newPermissions.iterator();
        while (it.hasNext()) {
            Permission permission = it.next();
            for (Permission removable : toRemove) {
                if (permission.isGroup() == removable.isGroup() && permission.getEntity() == removable.getEntity()) {
                    it.remove();
                    break;
                }
            }
        }

        return newPermissions;
    }

    @Override
    protected void touchFolders(List<TargetProxy> proxies) throws OXException {
        FolderService folderService = getFolderService();
        for (TargetProxy proxy : proxies) {
            UserizedFolder folder = ((FolderTargetProxy) proxy).getFolder();
            FolderUpdate folderUpdate = new FolderUpdate();
            folderUpdate.setTreeID(folder.getTreeID());
            folderUpdate.setID(folder.getID());
            folderService.updateFolder(folderUpdate, folder.getLastModifiedUTC(), parameters.getSession(), parameters.getFolderServiceDecorator());
        }
    }

    private void loadObjectTargets(Map<Integer, List<ShareTarget>> objectsByModule, Map<String, UserizedFolder> foldersById, boolean checkPermissions, Map<ShareTarget, TargetProxy> proxies) throws OXException {
        FolderService folderService = getFolderService();
        for (Entry<Integer, List<ShareTarget>> moduleEntry : objectsByModule.entrySet()) {
            int module = moduleEntry.getKey().intValue();
            ModuleHandler handler = handlers.get(module);
            List<ShareTarget> targetList = moduleEntry.getValue();
            for (ShareTarget target : targetList) {
                UserizedFolder folder = foldersById.get(target.getFolder());
                if (folder == null) {
                    folder = folderService.getFolder(FolderStorage.REAL_TREE_ID,
                        target.getFolder(),
                        parameters.getSession(),
                        parameters.getFolderServiceDecorator());
                    foldersById.put(folder.getID(), folder);
                }
            }

            List<TargetProxy> objects = handler.loadTargets(targetList, parameters);
            Iterator<ShareTarget> tlit = targetList.iterator();
            for (TargetProxy proxy : objects) {
                ShareTarget target = tlit.next();
                UserizedFolder parentFolder = foldersById.get(target.getFolder());
                if (checkPermissions && !canShareObject(parentFolder, proxy, handler)) {
                    throw ShareExceptionCodes.NO_SHARE_PERMISSIONS.create(
                        parameters.getUser().getId(),
                        proxy.getTitle(),
                        parameters.getContext().getContextId());
                }

                proxies.put(target, proxy);
            }
        }
    }

    private Map<String, UserizedFolder> loadFolderTargets(List<ShareTarget> folderTargets, boolean checkPermissions, Map<ShareTarget, TargetProxy> proxies) throws OXException {
        Map<String, UserizedFolder> foldersById = new HashMap<>();
        FolderService folderService = getFolderService();
        for (ShareTarget folderTarget : folderTargets) {
            if (null != Module.getForFolderConstant(folderTarget.getModule())) {
                UserizedFolder folder = folderService.getFolder(FolderStorage.REAL_TREE_ID,
                    folderTarget.getFolder(),
                    parameters.getSession(),
                    parameters.getFolderServiceDecorator());
                FolderTargetProxy proxy = new FolderTargetProxy(folderTarget, folder);
                if (checkPermissions && !canShareFolder(folder)) {
                    throw ShareExceptionCodes.NO_SHARE_PERMISSIONS.create(
                        parameters.getUser().getId(),
                        proxy.getTitle(),
                        parameters.getContext().getContextId());
                }

                foldersById.put(folder.getID(), folder);
                proxies.put(folderTarget, proxy);
            } else {
                proxies.put(folderTarget, new VirtualTargetProxy(folderTarget));
            }
        }

        return foldersById;
    }

    private boolean canShareFolder(UserizedFolder folder) {
        return folder.getOwnPermission().isAdmin();
    }

    private boolean canShareObject(UserizedFolder folder, TargetProxy proxy, ModuleHandler handler) {
        return canShareFolder(folder) ? true : handler.canShare(proxy, parameters);
    }

    private UserService getUserService() throws OXException {
        return getService(UserService.class);
    }

    private ContextService getContextService() throws OXException {
        return getService(ContextService.class);
    }

    private FolderService getFolderService() throws OXException {
        return getService(FolderService.class);
    }

    private <T> T getService(Class<T> clazz) throws OXException {
        return Tools.requireService(clazz, services);
    }

    private static final class FolderUpdate extends AbstractFolder implements SetterAwareFolder {

        private static final long serialVersionUID = -8615729293509593034L;

        private boolean containsSubscribed;

        /**
         * Initializes a new {@link FolderUpdate}.
         */
        public FolderUpdate() {
            super();
            subscribed = true;
        }

        @Override
        public boolean isGlobalID() {
            return false;
        }

        @Override
        public void setSubscribed(boolean subscribed) {
            super.setSubscribed(subscribed);
            containsSubscribed = true;
        }

        @Override
        public boolean containsSubscribed() {
            return containsSubscribed;
        }

    }

}
