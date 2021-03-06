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

package com.openexchange.context.osgi;

import java.rmi.Remote;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import com.openexchange.context.rmi.ContextRMIServiceImpl;
import com.openexchange.database.CreateTableService;
import com.openexchange.database.DatabaseService;
import com.openexchange.groupware.contexts.impl.sql.ChangePrimaryKeyForContextAttribute;
import com.openexchange.groupware.contexts.impl.sql.ContextAttributeCreateTable;
import com.openexchange.groupware.contexts.impl.sql.ContextAttributeTableUpdateTask;
import com.openexchange.groupware.update.UpdateTaskProviderService;
import com.openexchange.osgi.HousekeepingActivator;

/**
 * {@link ContextActivator}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class ContextActivator extends HousekeepingActivator {

    public ContextActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { DatabaseService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        DatabaseService dbase = getService(DatabaseService.class);

        ContextAttributeCreateTable createTable = new ContextAttributeCreateTable();
        registerService(CreateTableService.class, createTable);

        ContextAttributeTableUpdateTask updateTask = new ContextAttributeTableUpdateTask(dbase);

        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>(1);
        serviceProperties.put("RMI_NAME", ContextRMIServiceImpl.RMI_NAME);
        registerService(Remote.class, new ContextRMIServiceImpl(), serviceProperties);
        ChangePrimaryKeyForContextAttribute changePrimaryKeyForContextAttribute = new ChangePrimaryKeyForContextAttribute();

        registerService(UpdateTaskProviderService.class, () -> Arrays.asList(updateTask, changePrimaryKeyForContextAttribute));

        openTrackers();
    }

    @Override
    protected void stopBundle() throws Exception {
        super.stopBundle();
    }
}
