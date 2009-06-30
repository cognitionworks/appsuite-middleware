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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.groupware.search;

import static com.openexchange.java.Autoboxing.I;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SearchObject
 * @author <a href="mailto:sebastian.kauss@netline-is.de">Sebastian Kauss</a>
 */
public abstract class SearchObject {

    /**
     * Undefined integer value.
     */
    public static final int NO_FOLDER = -1;

    /**
     * No search pattern.
     */
    public static final String NO_PATTERN = null;

    /**
     * No category search.
     */
    public static final String NO_CATEGORIES = null;

    private int folder = NO_FOLDER;

    private Set<Integer> folders = new HashSet<Integer>();

    private String pattern = NO_PATTERN;

    private String catgories = NO_CATEGORIES;

    private boolean subfolderSearch;
    private boolean allfoldersSearch;

    protected SearchObject() {
        super();
    }

    public String getCatgories() {
        return catgories;
    }

    public void setCatgories(final String catgories) {
        this.catgories = catgories;
    }

    /**
     * @deprecated use {@link #getFolders()} to support search in multiple folders.
     */
    @Deprecated
    public int getFolder() {
        return folder;
    }

    /**
     * @deprecated use {@link #addFolder(int)} to support search in multiple folders.
     */
    @Deprecated
    public void setFolder(final int folder) {
        this.folder = folder;
    }

    public void addFolder(final int folder) {
        folders.add(I(folder));
    }
    
    public void setFolders(final int...folder) {
        folders.clear();
        for (int folderId : folder) {
            folders.add(I(folderId));
        }
    }
    
    public void setFolders(List<Integer> folder) {
        folders.clear();
        folders.addAll(folder);
    }
    
    public void clearFolders() {
        folders.clear();
    }


    public int[] getFolders() {
        final Integer[] tmp = folders.toArray(new Integer[folders.size()]);
        final int[] retval = new int[folders.size()];
        for (int i = 0; i < tmp.length; i++) {
            retval[i] = tmp[i].intValue();
        }
        return retval;
    }

    public boolean hasFolders() {
        return !folders.isEmpty();
    }

    public boolean isSubfolderSearch() {
        return subfolderSearch;
    }

    public void setSubfolderSearch(final boolean subfolderSearch) {
        this.subfolderSearch = subfolderSearch;
    }

    /**
     * @deprecated check if {@link #hasFolders()} is <code>false</code>.
     */
    @Deprecated
    public boolean isAllFolders() {
        return allfoldersSearch;
    }

    /**
     * @deprecated simply do not add any folder definition to get an all folder search.
     */
    @Deprecated
    public void setAllFolders(final boolean allfolderSearch) {
        this.allfoldersSearch = allfolderSearch;
    }

    public void setPattern(final String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }
}
