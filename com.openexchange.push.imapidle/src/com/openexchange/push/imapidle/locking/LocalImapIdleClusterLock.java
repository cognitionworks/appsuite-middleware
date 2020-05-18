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

package com.openexchange.push.imapidle.locking;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.UserAndContext;


/**
 * {@link LocalImapIdleClusterLock}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class LocalImapIdleClusterLock extends AbstractImapIdleClusterLock {

    private final ConcurrentMap<UserAndContext, String> locks;

    /**
     * Initializes a new {@link LocalImapIdleClusterLock}.
     */
    public LocalImapIdleClusterLock(boolean validateSessionExistence, ServiceLookup services) {
        super(validateSessionExistence, services);
        locks = new ConcurrentHashMap<>(2048, 0.9F, 1);
    }

    private UserAndContext generateKey(SessionInfo sessionInfo) {
        return UserAndContext.newInstance(sessionInfo.getUserId(), sessionInfo.getContextId());
    }

    @Override
    public Type getType() {
        return Type.LOCAL;
    }

    @Override
    public AcquisitionResult acquireLock(SessionInfo sessionInfo) throws OXException {
        UserAndContext key = generateKey(sessionInfo);

        long now = System.currentTimeMillis();
        String previous = locks.putIfAbsent(key, generateValue(now, sessionInfo));

        if (null == previous) {
            // Not present before
            return AcquisitionResult.ACQUIRED_NEW;
        }

        // Check if valid
        Validity validity = validateValue(previous, now, getValidationArgs(sessionInfo, null));
        if (Validity.VALID == validity) {
            // Locked
            return AcquisitionResult.NOT_ACQUIRED;
        }

        // Invalid entry - try to replace it mutually exclusive
        boolean replaced = locks.replace(key, previous, generateValue(now, sessionInfo));
        if (false == replaced) {
            return AcquisitionResult.NOT_ACQUIRED;
        }

        switch (validity) {
            case NO_SUCH_SESSION:
                return AcquisitionResult.ACQUIRED_NO_SUCH_SESSION;
            case TIMED_OUT:
                return AcquisitionResult.ACQUIRED_TIMED_OUT;
            default:
                return AcquisitionResult.ACQUIRED_NEW;
        }
    }

    @Override
    public void refreshLock(SessionInfo sessionInfo) throws OXException {
        locks.put(generateKey(sessionInfo), generateValue(System.currentTimeMillis(), sessionInfo));
    }

    @Override
    public void releaseLock(SessionInfo sessionInfo) throws OXException {
        locks.remove(generateKey(sessionInfo));
    }

}
