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

package com.openexchange.admin.storage.mysqlStorage;

import static com.openexchange.java.Autoboxing.I;
import java.util.HashMap;
import java.util.Map;

/**
 * Combines the context file store information for a specific database server and a certain file store. Collects across all schemas of that
 * database server the file store usage for the given file store.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
class FilestoreContextBlock {

    public final int writeDBPoolID, filestoreID;

    public final Map<Integer, FilestoreInfo> filestores = new HashMap<Integer, FilestoreInfo>();

    public FilestoreContextBlock(int writeDBPoolID, int filestoreID) {
        super();
        this.writeDBPoolID = writeDBPoolID;
        this.filestoreID = filestoreID;
    }

    public int size() {
        return filestores.size();
    }

    public void add(final FilestoreInfo newInfo) {
        filestores.put(I(newInfo.contextID), newInfo);
    }

    public void update(final int contextID, final long usage) {
        final FilestoreInfo info = filestores.get(I(contextID));
        if (info != null) {
            info.usage = usage;
        }
        // The schema may contain contexts having the files stored in another file store.
    }

    @Override
    public String toString(){
        return "["+filestoreID+"] Elements: " + size() + ", writepoolID: " + writeDBPoolID;
    }
}
