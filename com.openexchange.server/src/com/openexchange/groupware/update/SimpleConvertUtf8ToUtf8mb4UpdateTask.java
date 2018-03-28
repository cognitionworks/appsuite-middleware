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

package com.openexchange.groupware.update;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * {@link SimpleConvertUtf8ToUtf8mb4UpdateTask}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class SimpleConvertUtf8ToUtf8mb4UpdateTask extends AbstractConvertUtf8ToUtf8mb4Task {

    private List<String> tableNames;
    private String[] dependencies;

    /**
     * Initialises a new {@link SimpleConvertUtf8ToUtf8mb4UpdateTask}.
     * 
     * @param tableNames A {@link List} with table names to convert
     * @param dependencies An optional array of dependency update tasks
     * @param throws {@link IllegalArgumentException} if the tableNames
     *            {@link List} is either <code>null</code> or empty.
     */
    public SimpleConvertUtf8ToUtf8mb4UpdateTask(List<String> tableNames, String... dependencies) {
        super();
        if (tableNames == null || tableNames.isEmpty()) {
            throw new IllegalArgumentException("The tableNames can neither be 'null' nor empty");
        }
        this.tableNames = tableNames;
        this.dependencies = dependencies == null ? NO_DEPENDENCIES : dependencies;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.groupware.update.UpdateTaskV2#getDependencies()
     */
    @Override
    public String[] getDependencies() {
        return dependencies;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.groupware.update.AbstractConvertUtf8ToUtf8mb4Task#tablesToConvert()
     */
    @Override
    protected List<String> tablesToConvert() {
        return tableNames;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.groupware.update.AbstractConvertUtf8ToUtf8mb4Task#before(com.openexchange.groupware.update.PerformParameters, java.sql.Connection)
     */
    @Override
    protected void before(PerformParameters params, Connection connection) throws SQLException {
        // no-op, override to implement
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.groupware.update.AbstractConvertUtf8ToUtf8mb4Task#after(com.openexchange.groupware.update.PerformParameters, java.sql.Connection)
     */
    @Override
    protected void after(PerformParameters params, Connection connection) throws SQLException {
        // no-op, override to implement
    }
}
