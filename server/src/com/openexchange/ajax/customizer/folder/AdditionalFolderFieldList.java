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

package com.openexchange.ajax.customizer.folder;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link AdditionalFolderFieldList}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class AdditionalFolderFieldList {

    // TODO: Track service ranking and allow fields to overwrite other fields.

    private static final Log LOG = LogFactory.getLog(AdditionalFolderFieldList.class);

    private final Map<Integer, AdditionalFolderField> byColId = new HashMap<Integer, AdditionalFolderField>();
    private final Map<String, AdditionalFolderField> byName = new HashMap<String, AdditionalFolderField>();

    /**
     * Adds an additional folder field to this list.
     * 
     * @param field The additional folder field
     */
    public synchronized void addField(final AdditionalFolderField field) {
        final Integer key = Integer.valueOf(field.getColumnID());
        if (byColId.containsKey(key) || byName.containsKey(field.getColumnName())) {
            warnAboutCollision(field);
            return;
        }
        byColId.put(key, field);
        byName.put(field.getColumnName(), field);
    }

    private void warnAboutCollision(final AdditionalFolderField field) {
        LOG.warn("Collision in folder fields. Field '" + field.getColumnName() + "' : " + field.getColumnID() + " has already been taken. Ignoring second service.");
    }

    /**
     * Gets the additional folder field associated with specified column number.
     * 
     * @param col The column number
     * @return The additional folder field associated with specified column number or a neutral <code>null</code> field
     */
    public AdditionalFolderField get(final int col) {
        final AdditionalFolderField additionalFolderField = byColId.get(Integer.valueOf(col));
        return null == additionalFolderField ? new NullField(col) : additionalFolderField;
    }

    /**
     * Gets the additional folder field associated with specified column name.
     * 
     * @param col The column name
     * @return The additional folder field associated with specified column name or a <code>null</code>
     */
    public AdditionalFolderField get(final String col) {
        return byName.get(col);
    }

    /**
     * Checks if an additional folder field is associated with specified column number.
     * 
     * @param col The column number
     * @return <code>true</code> if an additional folder field is associated with specified column number; otherwise <code>false</code>
     */
    public boolean knows(final int col) {
        return byColId.containsKey(Integer.valueOf(col));
    }

    /**
     * Checks if an additional folder field is associated with specified column name.
     * 
     * @param col The column name
     * @return <code>true</code> if an additional folder field is associated with specified column name; otherwise <code>false</code>
     */
    public boolean knows(final String col) {
        return byName.containsKey(col);
    }

    /**
     * Removes the additional folder field associated with specified column number.
     * 
     * @param colId The column number
     */
    public synchronized void remove(final int colId) {
        if (!knows(colId)) {
            return;
        }
        final AdditionalFolderField f = get(colId);
        byName.remove(f.getColumnName());
        byColId.remove(Integer.valueOf(colId));
    }

    /**
     * A neutral <code>null</code> field implementation.
     */
    private static final class NullField implements AdditionalFolderField {

        private final int columnId;

        NullField(final int columnId) {
            super();
            this.columnId = columnId;
        }

        public int getColumnID() {
            return columnId;
        }

        public String getColumnName() {
            return null;
        }

        public Object getValue(final FolderObject folder, final ServerSession session) {
            return null;
        }

        public Object renderJSON(final Object value) {
            return null;
        }

    }
}
