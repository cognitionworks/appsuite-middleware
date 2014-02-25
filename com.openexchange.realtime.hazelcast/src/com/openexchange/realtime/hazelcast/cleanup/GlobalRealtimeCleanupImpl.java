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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.realtime.hazelcast.cleanup;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.openexchange.exception.OXException;
import com.openexchange.realtime.cleanup.GlobalRealtimeCleanup;
import com.openexchange.realtime.cleanup.LocalRealtimeCleanup;
import com.openexchange.realtime.hazelcast.Services;
import com.openexchange.realtime.hazelcast.channel.HazelcastAccess;
import com.openexchange.realtime.hazelcast.directory.HazelcastResourceDirectory;
import com.openexchange.realtime.packet.ID;

/**
 * {@link GlobalRealtimeCleanupImpl}
 *
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
public class GlobalRealtimeCleanupImpl implements GlobalRealtimeCleanup {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalRealtimeCleanupImpl.class);
    private final HazelcastResourceDirectory hazelcastResourceDirectory;

    /**
     * Initializes a new {@link GlobalRealtimeCleanupImpl}.
     * @param hazelcastResourceDirectory
     */
    public GlobalRealtimeCleanupImpl(HazelcastResourceDirectory hazelcastResourceDirectory) {
        super();
        this.hazelcastResourceDirectory = hazelcastResourceDirectory;
    }

    @Override
    public void cleanForId(ID id) {
        LOG.debug("Starting global realtime cleanup for ID: {}", id);
        try {
            Collection<ID> removeFromResourceDirectory = removeFromResourceDirectory(id);
            if(removeFromResourceDirectory.isEmpty()) {
                LOG.debug("Unable to remove {} from ResourceDirectory.", id);
            }
        } catch (OXException oxe) {
            LOG.error("Unable to remove {} from ResourceDirectory.", id, oxe);
        }

        //Do the local cleanup via a simple service call
        LocalRealtimeCleanup localRealtimeCleanup = Services.getService(LocalRealtimeCleanup.class);
        localRealtimeCleanup.cleanForId(id);

        //Remote cleanup via distributed MultiTask to remaining members of the cluster
        HazelcastInstance hazelcastInstance;
        try {
            hazelcastInstance = HazelcastAccess.getHazelcastInstance();
            Member localMember = HazelcastAccess.getLocalMember();
            Set<Member> clusterMembers = new HashSet<Member>(hazelcastInstance.getCluster().getMembers());
            if(!clusterMembers.remove(localMember)) {
                LOG.warn("Couldn't remove local member from cluster members.");
            }
            if(!clusterMembers.isEmpty()) {
                hazelcastInstance.getExecutorService("default").submitToMembers(new CleanupDispatcher(id), clusterMembers);
            } else {
                LOG.debug("No other cluster members besides the local member. No further clean up necessary.");
            }
        } catch (Exception e) {
            LOG.error("Failed to issue remote cleanup for {}.", id, e);
        }
    }

    @Override
    public Collection<ID> removeFromResourceDirectory(ID id) throws OXException {
        return hazelcastResourceDirectory.remove(id).keySet();
    }

    @Override
    public Collection<ID> removeFromResourceDirectory(Collection<ID> ids) throws OXException {
        return hazelcastResourceDirectory.remove(ids).keySet();
    }

}
