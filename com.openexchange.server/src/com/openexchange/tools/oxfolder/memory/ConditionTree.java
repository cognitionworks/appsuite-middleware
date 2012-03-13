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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.tools.oxfolder.memory;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.tools.oxfolder.OXFolderProperties;

/**
 * {@link ConditionTree}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ConditionTree {

    private static final Condition CONDITION_ADMIN = new Condition() {

        public boolean fulfilled(final Permission p) {
            return p.admin;
        }
    };

    private static final Condition CONDITION_READ_FOLDER = new Condition() {

        public boolean fulfilled(final Permission p) {
            return p.readFolder;
        }
    };

    private static final List<Condition> CONDITIONS = new CopyOnWriteArrayList<Condition>(Arrays.asList(CONDITION_ADMIN, CONDITION_READ_FOLDER));

    private static final int LENGTH = CONDITIONS.size();

    private final TIntSet folderIds;

    private final List<Permission> permissions;

    private final boolean ignoreSharedAddressbook;

    /**
     * Initializes a new {@link ConditionTree}.
     */
    public ConditionTree() {
        super();
        folderIds = new TIntHashSet();
        permissions = new ArrayList<Permission>();
        ignoreSharedAddressbook = OXFolderProperties.isIgnoreSharedAddressbook();
    }

    /**
     * Inserts specified permission.
     * 
     * @param p The permission associated with a certain entity
     */
    public void insert(final Permission p) {
        if (ignoreSharedAddressbook && FolderObject.SYSTEM_GLOBAL_FOLDER_ID == p.fuid) {
            return;
        }
        insert(CONDITION_ADMIN, p, 0);
    }

    private void insert(final Condition condition, final Permission p, final int index) {
        if (index >= LENGTH) {
            // Failed every condition
            return;
        }
        if (condition.fulfilled(p)) {
            folderIds.add(p.fuid);
            permissions.add(p);
        } else {
            final int nextIndex = index + 1;
            if (nextIndex >= LENGTH) {
                // Failed every condition
                return;
            }
            insert(CONDITIONS.get(nextIndex), p, nextIndex);
        }
    }

    /**
     * Gets the identifiers of visible folders.
     * 
     * @return The visible folders' identifiers
     */
    public TIntSet getVisibleFolderIds() {
        return folderIds;
    }

    /**
     * Gets the identifiers of visible folders filtered by given condition.
     * 
     * @param condition The condition to apply to returned identifiers
     * @return The visible folders' identifiers
     */
    public TIntSet getVisibleFolderIds(final Condition condition) {
        if (null == condition) {
            return new TIntHashSet(folderIds);
        }
        final TIntSet retval = new TIntHashSet(folderIds);
        for (final Permission p : permissions) {
            if (!condition.fulfilled(p)) {
                retval.remove(p.fuid);
            }
        }
        return retval;
    }

}
