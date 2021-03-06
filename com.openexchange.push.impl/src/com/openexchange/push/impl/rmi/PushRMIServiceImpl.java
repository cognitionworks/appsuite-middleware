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

package com.openexchange.push.impl.rmi;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.management.NotCompliantMBeanException;
import org.slf4j.Logger;
import com.openexchange.push.PushUserClient;
import com.openexchange.push.PushUserInfo;
import com.openexchange.push.impl.PushManagerRegistry;
import com.openexchange.push.rmi.PushRMIService;

/**
 * {@link PushRMIServiceImpl} - The MBean implementation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.1
 */
public class PushRMIServiceImpl implements PushRMIService {

    /**
     * Initializes a new {@link PushRMIServiceImpl}.
     *
     * @throws NotCompliantMBeanException If initialization fails
     */
    public PushRMIServiceImpl() {
        super();
    }

    @Override
    public List<List<String>> listPushUsers() throws RemoteException {
        try {
            List<PushUserInfo> pushUsers = PushManagerRegistry.getInstance().listPushUsers();
            Collections.sort(pushUsers);

            int size = pushUsers.size();
            List<List<String>> list = new ArrayList<List<String>>(size);
            for (int i = 0; i < size; i++) {
                PushUserInfo pushUser = pushUsers.get(i);
                if (null != pushUser) {
                    list.add(Arrays.asList(Integer.toString(pushUser.getContextId()), Integer.toString(pushUser.getUserId()), Boolean.toString(pushUser.isPermanent())));
                }
            }

            return list;
        } catch (Exception e) {
            Logger logger = org.slf4j.LoggerFactory.getLogger(PushRMIServiceImpl.class);
            logger.error("", e);
            String message = e.getMessage();
            throw new RemoteException(message, new Exception(message));
        }
    }

    @Override
    public List<List<String>> listClientRegistrations() throws RemoteException {
        try {
            List<PushUserClient> pushClients = PushManagerRegistry.getInstance().listRegisteredPushUsers();

            int size = pushClients.size();
            List<List<String>> list = new ArrayList<List<String>>(size);
            for (int i = 0; i < size; i++) {
                PushUserClient pushClient = pushClients.get(i);
                if (null != pushClient) {
                    list.add(Arrays.asList(Integer.toString(pushClient.getContextId()), Integer.toString(pushClient.getUserId()), pushClient.getClient()));
                }
            }

            return list;
        } catch (Exception e) {
            Logger logger = org.slf4j.LoggerFactory.getLogger(PushRMIServiceImpl.class);
            logger.error("", e);
            String message = e.getMessage();
            throw new RemoteException(message, new Exception(message));
        }
    }

    @Override
    public boolean unregisterPermanentListenerFor(int userId, int contextId, String clientId) throws RemoteException {
        try {
            return PushManagerRegistry.getInstance().unregisterPermanentListenerFor(userId, contextId, clientId);
        } catch (Exception e) {
            Logger logger = org.slf4j.LoggerFactory.getLogger(PushRMIServiceImpl.class);
            logger.error("", e);
            String message = e.getMessage();
            throw new RemoteException(message, new Exception(message));
        }
    }
}
