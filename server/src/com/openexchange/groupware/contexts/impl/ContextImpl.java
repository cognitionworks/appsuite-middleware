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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.groupware.contexts.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link ContextImpl} - The implementation of {@link ContextExtended}.
 * 
 * @author <a href="mailto:sebastian.kauss@open-xchange.org">Sebastian Kauss</a>
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class ContextImpl implements ContextExtended {

    private static final long serialVersionUID = 8570995404471786200L;

    private final int contextId;
    private String name;
    private String[] loginInfo;
    private int mailadmin = -1;
    private int filestoreId = -1;
    private String filestoreName;
    private String[] filestorageAuth;
    private long fileStorageQuota;
    private boolean enabled = true;
    private boolean updating = false;
    private boolean readOnly = false;
    private Map<String, Set<String>> attributes = new HashMap<String, Set<String>>();

    public ContextImpl(final int contextId) {
        this.contextId = contextId;
    }

    public int getContextId() {
        return contextId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ContextImpl)) {
            return false;
        }
        return contextId == ((ContextImpl) obj).contextId;
    }

    @Override
    public int hashCode() {
        return contextId;
    }

    @Override
    public String toString() {
        return "ContextImpl cid: " + contextId;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getMailadmin() {
        return mailadmin;
    }

    public long getFileStorageQuota() {
        return fileStorageQuota;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setMailadmin(final int mailadmin) {
        this.mailadmin = mailadmin;
    }

    public void setFileStorageQuota(final long fileStorageQuota) {
        this.fileStorageQuota = fileStorageQuota;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public int getFilestoreId() {
        return filestoreId;
    }

    public void setFilestoreId(final int filestoreId) {
        this.filestoreId = filestoreId;
    }

    public String getFilestoreName() {
        return filestoreName;
    }

    public void setFilestoreName(final String filestoreName) {
        this.filestoreName = filestoreName;
    }

    public void setFilestoreAuth(final String[] filestoreAuth) {
        this.filestorageAuth = filestoreAuth;
    }

    public String[] getFileStorageAuth() {
        return filestorageAuth.clone();
    }

    public String[] getLoginInfo() {
        return loginInfo.clone();
    }

    public void setLoginInfo(final String[] loginInfo) {
        this.loginInfo = loginInfo.clone();
    }

    public void setUpdating(final boolean updating) {
        this.updating = updating;
    }

    public boolean isUpdating() {
        return updating;
    }

    public boolean isReadOnly() {
        return readOnly;
    }
    
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        
    }

    public Map<String, Set<String>> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
    
    public void addAttribute(String attrName, String value) {
        Set<String> set = attributes.get(attrName);
        if (set == null) {
            set = new HashSet<String>();
            attributes.put(attrName, set);
        }
        set.add(value);
    }
}
