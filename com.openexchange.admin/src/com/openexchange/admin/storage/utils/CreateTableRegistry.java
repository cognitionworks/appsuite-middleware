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

package com.openexchange.admin.storage.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.openexchange.database.CreateTableService;

/**
 * {@link CreateTableRegistry} - A registry for appearing instances of <code>CreateTableService</code>.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class CreateTableRegistry {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CreateTableRegistry.class);

    private static final CreateTableRegistry SINGLETON = new CreateTableRegistry();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static CreateTableRegistry getInstance() {
        return SINGLETON;
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private final ConcurrentMap<CreateTableService, Object> createTables;

    /**
     * Prevent instantiation.
     */
    private CreateTableRegistry() {
        super();
        createTables = new ConcurrentHashMap<CreateTableService, Object>(128, 0.9F, 1);
    }

    public void addCreateTable(CreateTableService service) {
        if (null != createTables.putIfAbsent(service, service)) {
            LOG.warn("CreateTableService {} found twice.", service.getClass().getName());
        }
    }

    public void removeCreateTable(CreateTableService service) {
        if (null == createTables.remove(service)) {
            LOG.warn("Unkown CreateTableService {} removed.", service.getClass().getName());
        }
    }

    public List<CreateTableService> getList() {
        return new ArrayList<CreateTableService>(createTables.keySet());
    }

    /**
     * Gets all known tables.
     *
     * @return A set containing the names of all known tables
     */
    public Set<String> getKnownTables() {
        int size = createTables.size();
        if (size <= 0) {
            return Collections.emptySet();
        }

        Set<String> knownTables = new HashSet<String>(size);
        for (CreateTableService service : createTables.keySet()) {
            String[] tables = service.tablesToCreate();
            if (null != tables) {
                for (String table : tables) {
                    knownTables.add(table);
                }
            }
        }
        return knownTables;
    }

}
