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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.folderstorage.outlook.memory.impl;

import gnu.trove.ConcurrentTIntHashSet;
import gnu.trove.set.TIntSet;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.SortableId;
import com.openexchange.folderstorage.outlook.OutlookFolder;
import com.openexchange.folderstorage.outlook.OutlookFolderStorage;
import com.openexchange.folderstorage.outlook.memory.MemoryCRUD;
import com.openexchange.folderstorage.outlook.memory.MemoryFolder;
import com.openexchange.folderstorage.outlook.memory.MemoryTree;
import com.openexchange.i18n.tools.StringHelper;

/**
 * {@link MemoryTreeImpl}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MemoryTreeImpl implements MemoryTree {

    private final ConcurrentMap<String, MemoryFolder> folderMap;

    private final ConcurrentMap<String, Set<MemoryFolder>> parentMap;

    private final MemoryCRUD crud;

    private final int treeId;

    /**
     * Initializes a new {@link MemoryTreeImpl}.
     * @param treeId 
     */
    public MemoryTreeImpl(final int treeId) {
        super();
        this.treeId = treeId;
        folderMap = new ConcurrentHashMap<String, MemoryFolder>(128);
        parentMap = new ConcurrentHashMap<String, Set<MemoryFolder>>(128);
        crud = new MemoryCRUDImpl(folderMap, parentMap);
    }

    /**
     * Gets the name of the folder held in virtual tree for the folder denoted by given folder identifier.
     */
    @Override
    public String getFolderName(final String folderId) {
        final MemoryFolder memoryFolder = folderMap.get(folderId);
        return null == memoryFolder ? null : memoryFolder.getName();
    }

    @Override
    public boolean containsParent(final String parentId) {
        return parentMap.containsKey(parentId);
    }

    @Override
    public boolean containsFolder(final String folderId) {
        return folderMap.containsKey(folderId);
    }

    @Override
    public boolean[] containsFolders(final String[] folderIds) {
        final boolean[] ret = new boolean[folderIds.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = folderMap.containsKey(folderIds[i]);
        }
        return ret;
    }

    @Override
    public boolean[] containsFolders(final SortableId[] folderIds) {
        final boolean[] ret = new boolean[folderIds.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = folderMap.containsKey(folderIds[i].getId());
        }
        return ret;
    }

    @Override
    public List<String[]> getSubfolderIds(final String parentId) {
        final Set<MemoryFolder> set = parentMap.get(parentId);
        if (null == set) {
            return Collections.emptyList();
        }
        final List<String[]> l = new ArrayList<String[]>(set.size());
        for (final MemoryFolder subfolder : set) {
            l.add(new String[] { subfolder.getId(), subfolder.getName() });
        }
        return l;
    }

    @Override
    public List<String> getFolders() {
        return new ArrayList<String>(folderMap.keySet());
    }

    private static final TIntSet KNOWN_TREES = new ConcurrentTIntHashSet(new int[] {Integer.parseInt(FolderStorage.REAL_TREE_ID), Integer.parseInt(OutlookFolderStorage.OUTLOOK_TREE_ID)});

    @Override
    public String[] getSubfolderIds(final Locale locale, final String parentId, final List<String[]> realSubfolderIds) {
        final List<String[]> ids = getSubfolderIds(parentId);
        final List<String> subfolderIds;
        if (FolderStorage.ROOT_ID.equals(parentId) && KNOWN_TREES.contains(treeId)) {
            /*
             * Proper sort of top level folders 1. Private 2. Public 3. Shared . . . n Sorted external email accounts
             */
            Collections.sort(ids, new PrivateSubfolderIDComparator(locale));
            subfolderIds = new ArrayList<String>(ids.size());
            for (final String[] fn : ids) {
                subfolderIds.add(fn[0]);
            }
        } else {
            final TreeMap<String, List<String>> treeMap = new TreeMap<String, List<String>>(new FolderNameComparator(locale));
            final StringHelper stringHelper = StringHelper.valueOf(locale);
            for (final String[] realSubfolderId : realSubfolderIds) {
                final String localizedName = stringHelper.getString(realSubfolderId[1]);
                List<String> list = treeMap.get(localizedName);
                if (null == list) {
                    list = new ArrayList<String>(2);
                    treeMap.put(localizedName, list);
                }
                list.add(realSubfolderId[0]);
            }
            for (final String[] idAndName : ids) {
                /*
                 * Names loaded from DB have no locale-sensitive string
                 */
                // String localizedName = stringHelper.getString(rs.getString(2));
                final String name = idAndName[1];
                List<String> list = treeMap.get(name);
                if (null == list) {
                    list = new ArrayList<String>(2);
                    treeMap.put(name, list);
                }
                list.add(idAndName[0]);
            }
            subfolderIds = new ArrayList<String>(treeMap.size());
            for (final List<String> list : treeMap.values()) {
                for (final String name : list) {
                    subfolderIds.add(name);
                }
            }
        }
        return subfolderIds.toArray(new String[subfolderIds.size()]);
    }

    @Override
    public boolean fillFolder(final OutlookFolder outlookFolder) {
        final String folderId = outlookFolder.getID();
        final MemoryFolder memoryFolder = folderMap.get(folderId);
        if (null == memoryFolder) {
            return false;
        }
        outlookFolder.setParentID(memoryFolder.getParentId());
        // Set name
        {
            final String name = memoryFolder.getName();
            if (null != name) {
                outlookFolder.setName(name);
            }

        }
        // Set optional modified-by
        {
            final int modifiedBy = memoryFolder.getModifiedBy();
            if (modifiedBy > 0) {
                outlookFolder.setModifiedBy(-1);
            } else {
                outlookFolder.setModifiedBy(modifiedBy);
            }
        }
        // Set optional last-modified time stamp
        {
            final Date date = memoryFolder.getLastModified();
            if (null != date) {
                outlookFolder.setLastModified(null);
            } else {
                outlookFolder.setLastModified(date);
            }
        }
        // Set permissions if non-null
        {
            final Permission[] permissions = memoryFolder.getPermissions();
            if (null == permissions) {
                outlookFolder.setPermissions(null);
            } else {
                outlookFolder.setPermissions(permissions);
            }
        }
        // Set subscription
        {
            final Boolean subscribed = memoryFolder.getSubscribed();
            if (null != subscribed) {
                outlookFolder.setSubscribed(subscribed.booleanValue());
            } else {
                outlookFolder.setSubscribed(true);
            }
        }
        // Check for subscribed subfolder
        {
            boolean subscribedSubfolder = false;
            final Set<MemoryFolder> set = parentMap.get(folderId);
            if (null != set) {
                for (final MemoryFolder subfolder : set) {
                    final Boolean subscribed = subfolder.getSubscribed();
                    if (null != subscribed && subscribed.booleanValue()) {
                        subscribedSubfolder = true;
                        break;
                    }
                }
            }
            if (subscribedSubfolder) {
                outlookFolder.setSubscribedSubfolders(true);
            }
        }
        return true;
    }

    @Override
    public MemoryCRUD getCrud() {
        return crud;
    }

    @Override
    public int size() {
        return folderMap.size();
    }

    @Override
    public boolean isEmpty() {
        return folderMap.isEmpty();
    }

    @Override
    public void clear() {
        folderMap.clear();
    }

    @Override
    public String toString() {
        return folderMap.toString();
    }

    /**
     * A folder name comparator
     */
    private static final class FolderNameComparator implements Comparator<String> {

        private final Collator collator;

        public FolderNameComparator(final Locale locale) {
            super();
            collator = Collator.getInstance(locale);
            collator.setStrength(Collator.SECONDARY);
        }

        @Override
        public int compare(final String o1, final String o2) {
            return collator.compare(o1, o2);
        }

    } // End of FolderNameComparator

    private static final class PrivateSubfolderIDComparator implements Comparator<String[]> {

        private final Collator collator;

        public PrivateSubfolderIDComparator(final Locale locale) {
            super();
            collator = Collator.getInstance(locale);
            collator.setStrength(Collator.SECONDARY);
        }

        @Override
        public int compare(final String[] o1, final String[] o2) {
            {
                final String privateId = "1";
                final Integer privateComp = conditionalCompare(privateId.equals(o1[0]), privateId.equals(o2[0]));
                if (null != privateComp) {
                    return privateComp.intValue();
                }
            }
            {
                final String publicId = "2";
                final Integer publicComp = conditionalCompare(publicId.equals(o1[0]), publicId.equals(o2[0]));
                if (null != publicComp) {
                    return publicComp.intValue();
                }
            }
            {
                final String sharedId = "3";
                final Integer sharedComp = conditionalCompare(sharedId.equals(o1[0]), sharedId.equals(o2[0]));
                if (null != sharedComp) {
                    return sharedComp.intValue();
                }
            }
            {
                final String uiName = "Unified Inbox";
                final Integer unifiedInboxComp = conditionalCompare(uiName.equalsIgnoreCase(o1[1]), uiName.equalsIgnoreCase(o2[1]));
                if (null != unifiedInboxComp) {
                    return unifiedInboxComp.intValue();
                }
            }
            return collator.compare(o1[1], o2[1]);
        }

        private Integer conditionalCompare(final boolean b1, final boolean b2) {
            if (b1) {
                if (!b2) {
                    return Integer.valueOf(-1);
                }
                return Integer.valueOf(0);
            } else if (b2) {
                return Integer.valueOf(1);
            }
            return null;
        }

    } // End of FolderNameComparator

}
