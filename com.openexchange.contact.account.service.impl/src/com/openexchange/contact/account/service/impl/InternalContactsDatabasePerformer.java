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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.contact.account.service.impl;

import static com.openexchange.contact.ContactSessionParameterNames.getParamReadOnlyConnection;
import static com.openexchange.contact.ContactSessionParameterNames.getParamWritableConnection;
import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.LoggerFactory;
import com.openexchange.contact.common.DefaultContactsAccount;
import com.openexchange.contact.storage.ContactStorages;
import com.openexchange.contact.storage.ContactsStorageFactory;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.database.provider.DBTransactionPolicy;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link InternalContactsDatabasePerformer} - Database performer for all
 * internal ({@link DefaultContactsAccount}) contacts accounts.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
public abstract class InternalContactsDatabasePerformer<T> extends AbstractContactsDatabasePerformer<T> {

    private final Session session;
    private final ServiceLookup services;

    /**
     * Initialises a new {@link InternalContactsDatabasePerformer}.
     * 
     * @param session the session
     * @throws OXException in case the {@link DatabaseService} is absent
     */
    public InternalContactsDatabasePerformer(ServiceLookup services, Session session) throws OXException {
        super(services.getServiceSafe(DatabaseService.class), session.getContextId());
        this.services = services;
        this.session = session;
    }

    @Override
    ContactStorages initStorage(DBProvider dbProvider, DBTransactionPolicy txPolicy) throws OXException {
        Context context = ServerSessionAdapter.valueOf(session).getContext();
        ContactsStorageFactory storageFactory = services.getServiceSafe(ContactsStorageFactory.class);
        return storageFactory.create(context, dbProvider, txPolicy);
    }

    @Override
    protected void onConnection(Connection connection) {
        try {
            session.setParameter(connection.isReadOnly() ? getParamReadOnlyConnection() : getParamWritableConnection(), connection);
        } catch (SQLException e) {
            LoggerFactory.getLogger(InternalContactsDatabasePerformer.class).debug("Unable to set the 'connection' parameter to session", e);
        }
    }
}
